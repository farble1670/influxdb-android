package com.clover.influxdb.android;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class InfluxDbProvider extends ContentProvider {
  // @SuppressWarnings("unused")
  private static final String TAG = InfluxDbProvider.class.getSimpleName();

  static boolean notify = true;

  private static final int DATABASE_VERSION = 1;
  private static final String DATABASE_NAME = "influxdb.db";
  
  private static final int POINTS = 1;
  private static final int POINT = 2;
  
  private DatabaseHelper helper = null;

  private synchronized DatabaseHelper getDbHelper(Uri uri) {
    if (helper == null) {
      helper = new DatabaseHelper(getContext(), DATABASE_NAME);
    }

    return helper;
  }

  private static class DatabaseHelper extends SQLiteOpenHelper {

    DatabaseHelper(Context context, String name) {
      super(context, name, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      String cmd = "CREATE TABLE IF NOT EXISTS " + InfluxDbContract.Points.CONTENT_DIRECTORY + " ("
          + InfluxDbContract.Points._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
          + InfluxDbContract.Points.POINT + " TEXT NOT NULL,"
          + InfluxDbContract.Points.CREATED_TIME + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL"
          + ");";
      db.execSQL(cmd);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.i(TAG, "upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
      db.execSQL("DROP TABLE IF EXISTS " + InfluxDbContract.Points.CONTENT_DIRECTORY);
      onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      onUpgrade(db, oldVersion, newVersion);
    }
  }
  
  private final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

  private String authority;

  private static final Map<String, String> paymentsProjection = new HashMap<String, String>();

  @Override
  public boolean onCreate() {
    ComponentName cn = new ComponentName(getContext(), getClass());
    try {
      ProviderInfo pi = getContext().getPackageManager().getProviderInfo(cn, 0);
      if (pi == null) {
        throw new IllegalArgumentException("provider not found!");
      }
      authority = pi.authority;
      if (authority == null) {
        throw new IllegalArgumentException("authority not found");
      }
    } catch (PackageManager.NameNotFoundException e) {
      throw new RuntimeException(e);
    }

    uriMatcher.addURI(authority, InfluxDbContract.Points.CONTENT_DIRECTORY, POINTS);
    uriMatcher.addURI(authority, InfluxDbContract.Points.CONTENT_DIRECTORY + "/#", POINT);

    paymentsProjection.put(InfluxDbContract.Points._ID, InfluxDbContract.Points._ID);
    paymentsProjection.put(InfluxDbContract.Points.POINT, InfluxDbContract.Points.POINT);
    paymentsProjection.put(InfluxDbContract.Points.CREATED_TIME, InfluxDbContract.Points.CREATED_TIME);
    
    return true;
  }

  @Override
  public int delete(Uri uri, String whereClause, String[] whereArgs) {
    DatabaseHelper dbHelper = getDbHelper(uri);
    if (dbHelper == null) {
      return 0;
    }

    SQLiteDatabase db = dbHelper.getWritableDatabase();

    int count = 0;
    long id = -1;

    switch (uriMatcher.match(uri)) {
      case POINTS:
        db.beginTransaction();
        try {
          count = db.delete(InfluxDbContract.Points.CONTENT_DIRECTORY, whereClause, whereArgs);
          db.setTransactionSuccessful();
        } finally {
          db.endTransaction();
        }
        break;

      case POINT:
        id = ContentUris.parseId(uri);
        db.beginTransaction();
        try {
          count = db.delete(InfluxDbContract.Points.CONTENT_DIRECTORY, InfluxDbContract.Points._ID + "=" + id + (!TextUtils.isEmpty(whereClause) ? " AND (" + whereClause + ")" : ""), whereArgs);
          db.setTransactionSuccessful();
        } finally {
          db.endTransaction();
        }
        break;
    }

    if (notify && count > 0) {
      getContext().getContentResolver().notifyChange(uri, null);
    }

    return count;
  }

  @Override
  public String getType(Uri uri) {
    switch (uriMatcher.match(uri)) {
      case POINTS:
        return InfluxDbContract.Points.CONTENT_TYPE;
      case POINT:
        return InfluxDbContract.Points.CONTENT_ITEM_TYPE;
      default:
        throw new IllegalArgumentException("unhandled URI: " + uri);
    }
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    DatabaseHelper dbHelper = getDbHelper(uri);
    if (dbHelper == null) {
      return null;
    }

    SQLiteDatabase db = dbHelper.getWritableDatabase();

    long rowId = 0;

    switch (uriMatcher.match(uri)) {
      case POINTS:
        db.beginTransaction();
        try {
          rowId = db.insertWithOnConflict(InfluxDbContract.Points.CONTENT_DIRECTORY, null, values, SQLiteDatabase.CONFLICT_FAIL);
          db.setTransactionSuccessful();
        } finally {
          db.endTransaction();
        }

        if (rowId > 0) {
          Uri rowUri = ContentUris.withAppendedId(uri, rowId);
          if (notify) {
            getContext().getContentResolver().notifyChange(rowUri, null);
          }
          trim(uri);
          return rowUri;
        }
        break;

      case POINT:
        throw new IllegalArgumentException("cannot insert URI: " + uri);

      default:
        throw new IllegalArgumentException("unhandled URI: " + uri);
    }

    throw new SQLException("insert failed, URI: " + uri);
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
    long id = -1;

    DatabaseHelper dbHelper = getDbHelper(uri);
    if (dbHelper == null) {
      return null;
    }

    switch (uriMatcher.match(uri)) {
      case POINTS:
        qb.setTables(InfluxDbContract.Points.CONTENT_DIRECTORY);
        qb.setProjectionMap(paymentsProjection);
        break;

      case POINT:
        qb.setTables(InfluxDbContract.Points.CONTENT_DIRECTORY);
        qb.setProjectionMap(paymentsProjection);

        id = ContentUris.parseId(uri);
        if (id >= 0) {
          qb.appendWhere(InfluxDbContract.Points._ID + "=" + id);
        }
        break;

      default:
        throw new IllegalArgumentException("unhandled URI: " + uri);
    }

    SQLiteDatabase db = dbHelper.getWritableDatabase();
    Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
    c.setNotificationUri(getContext().getContentResolver(), uri);

    return c;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void shutdown() {
    super.shutdown();
    synchronized (this) {
      if (helper != null) {
        helper.close();
      }
    }
  }

  @Override
  public int bulkInsert(Uri uri, ContentValues[] values){
    int numInserted = 0;

    int uriType = uriMatcher.match(uri);

    switch (uriType) {
      case POINTS:
        break;
      case POINT:
        throw new IllegalArgumentException("unhandled URI: " + uri);

    }
    SQLiteDatabase db = getDbHelper(uri).getWritableDatabase();
    db.beginTransaction();
    try {
      for (ContentValues cv : values) {
        long newId = db.insertWithOnConflict(InfluxDbContract.Points.CONTENT_DIRECTORY, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        if (newId <= 0) {
          throw new SQLException("failed to insert row into URI: " + uri);
        }
      }
      db.setTransactionSuccessful();
      getContext().getContentResolver().notifyChange(uri, null);
      numInserted = values.length;
    } finally {
      db.endTransaction();
    }

    trim(uri);

    return numInserted;
  }

  static final int TRIM_SIZE = 10000;

  public boolean trim(Uri uri) {
    SQLiteDatabase db = getDbHelper(uri).getWritableDatabase();

    try {
      String sql = String.format(
          "DELETE FROM %s WHERE %s IN (SELECT %s FROM %s ORDER BY %s DESC LIMIT -1 OFFSET %d)",
          InfluxDbContract.Points.CONTENT_DIRECTORY,
          InfluxDbContract.Points._ID,
          InfluxDbContract.Points._ID,
          InfluxDbContract.Points.CONTENT_DIRECTORY,
          InfluxDbContract.Points._ID,
          TRIM_SIZE
      );
      db.execSQL(sql);
      return true;
    } catch (Exception e) {
      Log.e(TAG, "error trimming database", e);
    }

    return false;
  }
}
