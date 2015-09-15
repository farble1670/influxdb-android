package com.clover.influxdb.android;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class WriteService extends IntentService {
  private static final String PREF_LAST_WRITE = WriteService.class.getName() + ".lastWrite";

  private SharedPreferences prefs;

  public WriteService() {
    super(WriteService.class.getName());
  }

  @Override
  public void onCreate() {
    super.onCreate();

    this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    if (!isNetworkConnected(this)) {
      Log.i(InfluxDb.TAG, "network not connected, aborting");
      return;
    }

    Uri uri = InfluxDbContract.Points.getUri(this);
    InfluxDbService service = new InfluxDbService(this);
    InfluxDbConfig config = null;
    try {
      config = InfluxDbConfig.instance(this);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    long now = System.currentTimeMillis();
    long lastWrite = prefs.getLong(PREF_LAST_WRITE, System.currentTimeMillis());
    long maxWriteDelay = config.getMaxWriteDelay() * 1000;

    boolean maxWriteDelayExceeded = lastWrite + maxWriteDelay <= now;

    // proceeding with an attempted write, so cancel any scheduled operations
    WriteReceiver.cancel(this);

    while (true) {
      Cursor c = null;
      try {
        c = getContentResolver().query(uri, null, null, null, "_id ASC LIMIT " + config.getWriteBatchSize());
        if (c == null || !c.moveToFirst()) {
          Log.i(InfluxDb.TAG, "no (more) points, aborting");
          break;
        }
        Log.i(InfluxDb.TAG, "processing points, count: " + c.getCount());

        if (maxWriteDelayExceeded) {
          Log.i(InfluxDb.TAG, "max write delay exceeded, ignoring batch size");
        } else {
          Log.i(InfluxDb.TAG, "max write delay not exceeded, checking batch size ...");
          if (c.getCount() < config.getWriteBatchSize()) {
            Log.i(InfluxDb.TAG, "count < batch size, aborting");

            // schedule a retry when max write delay is expected to be expired
            long delay = (lastWrite + maxWriteDelay) - now;
            Log.i(InfluxDb.TAG, String.format("scheduling retry in ~%dms", delay));
            WriteReceiver.schedule(this, delay);

            break;
          }
        }

        final List<String> lineProtocols = new ArrayList<String>();
        long maxId = 0;
        final int idColumn = c.getColumnIndex(InfluxDbContract.Points._ID);
        final int pointColumn = c.getColumnIndex(InfluxDbContract.Points.POINT);

        while (!c.isAfterLast()) {
          maxId = c.getInt(idColumn);
          lineProtocols.add(c.getString(pointColumn));

          c.moveToNext();
        }

        Log.i(InfluxDb.TAG, "writing line protocols ...");
        if (service.write(lineProtocols)) {
          getContentResolver().delete(uri, InfluxDbContract.Points._ID + "<=" + maxId, null);
          prefs.edit().putLong(PREF_LAST_WRITE, now).apply();
          WriteReceiver.cancel(this);
          Log.i(InfluxDb.TAG, "done writing line protocols");
        } else {
          Log.w(InfluxDb.TAG, "failed writing line protocols");
          break;
        }
      } finally {
        if (c != null) {
          c.close();
        }
      }
    }
  }

  private int getCount() {
    Cursor c = null;
    try {
      c = getContentResolver().query(InfluxDbContract.Points.getUri(this), new String[]{"count(*) AS count"}, null, null, null);
      if (c == null || !c.moveToFirst()) {
        return 0;
      }
      return c.getInt(0);
    } finally {
      if (c != null) {
        c.close();
      }
    }
  }

  static boolean isNetworkConnected(Context context) {
    ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo active = connManager.getActiveNetworkInfo();

    return active != null && active.isConnected();
  }
}
