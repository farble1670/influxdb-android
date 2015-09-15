package com.clover.influxdb.android;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class InfluxDb {
  static final String TAG = "influxdb-android";

  private final Context context;
  private final Uri uri;

  public InfluxDb(Context context) {
    this.context = context;
    this.uri = InfluxDbContract.Points.getUri(context);
  }

  /**
   * Queue a write.
   * <p/>
   * Should be called asynchronously as this performs a content provider insert.
   */
  public void write(Point... points) {
    ContentValues[] values = new ContentValues[points.length];
    for (int i = 0; i < points.length; i++) {
      ContentValues v = new ContentValues();
      v.put(InfluxDbContract.Points.POINT, points[i].lineProtocol());
      values[i] = v;
    }

    context.getContentResolver().bulkInsert(uri, values);
    context.startService(new Intent(context, WriteService.class));
  }
}
