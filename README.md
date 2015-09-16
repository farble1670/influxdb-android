# influxdb-android
InfluxDB SDK for Android OS. This is a WIP. This library saves points and sends them efficiently in batches, considering network connectivity.

This is an Android AAR library. It is not currently published to Maven Central or any other repository. To use it, install it locally using gradle (`gradle install`), and and reference it from your dependencies. 

```
dependencies {
  compile 'com.clover.influxdb.android:influxdb-android-lib:1-SNAPSHOT'
}
```

This library is for data collection only, and hence only supports /write.\

You must provide a config file named `influxdb.xml` in `res/xml`. E.g.,

```
<?xml version="1.0" encoding="utf-8"?>
<resources>
  <!--
  REQUIRED: Influx DB name.
  -->
  <string name="db_name">metrics</string>
  <!--
  REQUIRED: Influx DB URL.
  -->
  <string name="url">http://influxdb.test.com</string>
  <!--
  REQUIRED: Influx DB user name.
  -->
  <string name="user">testuser</string>
  <!--
  REQUIRED: Influx DB password.
  -->
  <string name="password">testpassword</string>
  <!--
  OPTIONAL: Will wait until this many measurements are queued before sending,
  except when max write delay is exceeded (see below). Default value is 100.
  -->
  <integer name="write_batch_size">10</integer>
  <!--
  OPTIONAL: Maximum delay between writes, in seconds, irrespective of batch size.
  Default value is 300 (5 minutes).
  -->
  <integer name="max_write_delay">30</integer>
</resources>
```

Then simply use the SDK class `InfluxDb`,

```
InfluxDb db = new InfluxDb(context);
Point p = Point.measurement("test2")
  .tag("serial", Build.SERIAL)
  .tag("model", Build.MODEL)
  .field("sine", Math.sin(value++))
  .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
  .build();
db.write(p);
```

Since `InfluxDb.write()` performs a database operation, it should not be called on the UI thread (run it in an `AsyncTask`).

## Why not influxdb-java?

influxdb-java is a pure Java SDK for InfluxDB and can of course be used on Android.
https://github.com/influxdb/influxdb-java

It didn't work for me, since it does not ensure reliable delivery, and has no way to integrate network connectivity awareness into the SDK. On mobile, that's expecially important as you don't want code banging on the network wasting battery when you know it's going to fail.

It also has extremely large transitive dependencies, including Guava and OkHttp. This is a real problem on Android where we are limited to 64k methods (unless we use multi-DEX, which is a problem in itself). 
