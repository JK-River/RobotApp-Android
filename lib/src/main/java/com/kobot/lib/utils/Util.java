package com.kobot.lib.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import com.kobot.lib.common.RobotApplication;

/**
 * Created by machao on 16/5/1.
 */
public class Util {

  public static String getCurVersion() {
    PackageInfo pi = null;
    Context context = RobotApplication.getContext().getApplicationContext();
    try {
      pi = context.getPackageManager().getPackageInfo(context.getPackageName(),
          0);
      String version = pi.versionName;
      return version;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
  }
}
