package com.clover.influxdb.android;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

public class InfluxDb {
  static final String TAG = "influxdb-android";

  private final Context context;
  private final InfluxDbDatabase database;

  public InfluxDb(Context context) {
    this.context = context;
    this.database = new InfluxDbDatabase(context);
  }

  /**
   * Queue a write.
   * <p/>
   * Should be called asynchronously as this performs a database insert.
   */
  public void write(Point... points) {
    ContentValues[] values = new ContentValues[points.length];
    for (int i = 0; i < points.length; i++) {
      ContentValues v = new ContentValues();
      v.put(InfluxDbContract.Points.POINT, points[i].lineProtocol());
      values[i] = v;
    }

    database.bulkInsert(values);
    context.startService(new Intent(context, WriteService.class));
  }

  public void shutdown() {
    database.shutdown();
  }
}
