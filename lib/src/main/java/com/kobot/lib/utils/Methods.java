package com.kobot.lib.utils;

import android.annotation.SuppressLint;
import android.widget.Toast;

import com.kobot.lib.common.RobotApplication;

@SuppressLint("SimpleDateFormat") public class Methods {

  /**
   * 显示短时间的Toast
   *
   * @param resId
   */
  public static void showToast(final int resId) {
    showToast(RobotApplication.getContext().getString(resId), false);
  }

  /**
   * 显示短时间的Toast
   *
   * @param text
   */
  public static void showToast(final CharSequence text) {
    showToast(text, false);
  }

  /**
   * 显示Toast
   *
   * @param text
   * @param lengthLong
   */

  public static void showToast(final CharSequence text,
      final boolean lengthLong) {
    Runnable update = new Runnable() {
      public void run() {
        Toast.makeText(RobotApplication.getContext(), text,
            lengthLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
      }
    };
    RobotApplication.getApplicationHandler().post(update);
  }

}
