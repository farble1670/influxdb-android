package com.clover.influxdb.android;

import android.content.Context;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class InfluxDbConfig extends XmlResourceParser {
  private static InfluxDbConfig instance = null;

  static synchronized InfluxDbConfig instance(Context context) throws IOException, XmlPullParserException {
    if (instance == null) {
      instance = new InfluxDbConfig(context);
      instance.parse();
    }
    return instance;
  }

  private InfluxDbConfig(Context context) {
    super(context);
  }

  public void parse() throws XmlPullParserException, IOException {
    int id = context.getResources().getIdentifier("influxdb", "xml", context.getPackageName());
    if (id == -1) {
      throw new IllegalArgumentException("influxdb config not found");
    }
    super.parse(id);
  }

  String getDbName() {
    return strings.get("db_name");
  }

  String getUrl() {
    return strings.get("url");
  }

  String getUser() {
    return strings.get("user");
  }

  String getPassword() {
    return strings.get("password");
  }

  int getWriteBatchSize() {
    if (integers.containsKey("write_batch_size")) {
      return integers.get("write_batch_size");
    }
    return context.getResources().getInteger(R.integer.default_write_batch_size);
  }

}
