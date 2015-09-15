package com.clover.influxdb.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.provider.BaseColumns;

public abstract class InfluxDbContract {
  static String getAuthority(Context context) {
    ComponentName cn = new ComponentName(context, InfluxDbProvider.class);
    try {
      ProviderInfo pi = context.getPackageManager().getProviderInfo(cn, 0);
      if (pi == null) {
        throw new IllegalArgumentException("provider not found!");
      }
      if (pi.authority == null) {
        throw new IllegalArgumentException("authority not found");
      }
      return pi.authority;
    } catch (PackageManager.NameNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static final class Points implements BaseColumns {
    public static final String CONTENT_DIRECTORY = "points";
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/points";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/point";

    static Uri getUri(Context context) {
      return Uri.withAppendedPath(Uri.parse("content://" + getAuthority(context)), InfluxDbContract.Points.CONTENT_DIRECTORY);
    }

    public static final String POINT = "point";
    public static final String CREATED_TIME = "created_time";
  }
}
