package com.kobot.lib.utils;

import com.kobot.lib.BuildConfig;

/**
 * Created by machao on 15/10/31.
 */
public class Log {

    public static void i(String tag, String msg) {
        if(!BuildConfig.DEBUG) {
            return;
        }
        android.util.Log.i(tag, msg);
    }

    public static void d(String tag, String msg) {
        if(!BuildConfig.DEBUG) {
            return;
        }
        android.util.Log.d(tag, msg);
    }

    public static void w(String tag, String msg) {
        if(!BuildConfig.DEBUG) {
            return;
        }
        android.util.Log.w(tag, msg);
    }

    public static void e(String tag, String msg) {
        if(!BuildConfig.DEBUG) {
            return;
        }
        android.util.Log.e(tag, msg);
    }
}
