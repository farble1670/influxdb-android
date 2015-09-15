package com.clover.influxdb.android;

import android.content.Context;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class XmlResourceParser {
  protected final Context context;

  protected final Map<String,String> strings = new HashMap<String, String>();
  protected final Map<String,Integer> integers = new HashMap<String, Integer>();

  public XmlResourceParser(Context context) {
    this.context = context;
  }

  public void parse(int id) throws XmlPullParserException, IOException {
    XmlPullParser xpp = context.getResources().getXml(id);
    int eventType = xpp.getEventType();

    while (eventType != XmlPullParser.END_DOCUMENT) {
      if (eventType == XmlPullParser.START_DOCUMENT) {
      }
      else if (eventType == XmlPullParser.END_DOCUMENT) {
      }
      else if (eventType == XmlPullParser.START_TAG) {
        String tag = xpp.getName();
        if ("string".equals(tag)) {
          strings.put(xpp.getAttributeValue(null, "name"), xpp.nextText());
        } else if ("integer".equals(tag)) {
          integers.put(xpp.getAttributeValue(null, "name"), Integer.valueOf(xpp.nextText()));
        }
      }
      else if (eventType == XmlPullParser.END_TAG) {
      }
      else if (eventType == XmlPullParser.TEXT) {
      }
      eventType = xpp.next();
    }

  }
}
