package com.kobot.lib.common;

import android.os.Handler;
import android.os.Looper;
import android.support.multidex.MultiDexApplication;
import com.kobot.lib.video.multi.control.MultiQavsdkControl;

public class RobotApplication extends MultiDexApplication {
  private static RobotApplication sContext;

  private MultiQavsdkControl multiQavsdkControl = null;

  private static Handler sApplicationHandler = null;

  public static Handler getApplicationHandler() {
    return sApplicationHandler;
  }

  public static RobotApplication getContext() {
    return sContext;
  }

  public RobotApplication() {
    sContext = this;
  }

  /**
   * Called when the activity is first created.
   */
  @SuppressWarnings({ "unchecked",
      "unused" }) @Override public void onCreate() {
    super.onCreate();
    sContext = this;

    if (sApplicationHandler == null) {
      sApplicationHandler = new Handler(Looper.getMainLooper());
    }
    AppInfo.initApp();
    multiQavsdkControl = new MultiQavsdkControl(this);
  }

  public MultiQavsdkControl getMultiQavsdkControl() {
    if (multiQavsdkControl == null) {
      multiQavsdkControl = new MultiQavsdkControl(this);
    }
    return multiQavsdkControl;
  }
}
