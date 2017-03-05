package com.kobot.lib.common;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

/**
 * application信息保存类, 需要在第一时间初始化该类, 公共库的所有功能都
 */
public class AppInfo {

    private static AppInfo instance = null;

    public static String IMEI = "";             // 设备标示IMEI
    public static String mac = "";              // mac地址
    public static String operator = "";         // 移动网络操作码
    public static String versionName = "";      // 版本号
    public static String screen = "";           // 屏幕信息
    public static float density;                // 屏幕密度
    public static float scaledDensity;
    public static int screenResolution;         // 屏幕分辨率
    public static int screenWidthForPortrait;   // 屏幕宽度
    public static int screenHeightForPortrait;  // 屏幕高度
    public static int screenStatusBarHeight;    //屏幕通知栏高度

    private Context mContext;



    /**
     * 初始化屏幕信息, 必须在activity.onCreate()第一时间进行初始化
     * 此函数真实只会执行一次, 所有在每个Activity.onCreate() 都调用此函数不会有多大开销
     */
    private AppInfo() {
        mContext = RobotApplication.getContext();
    }


    /**
     * 初始化函数, 必须在application.onCreate()的第一时间进行初始化操作
     */
    public synchronized static void initApp() {
        if (instance == null) {
            instance = new AppInfo();
            instance.initDeviceInfo();
            instance.initScreenInfo();
        }
    }


    /**
     * 初始化一些设备信息
     */
    private void initDeviceInfo() {
        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        IMEI = tm.getDeviceId();
        mac = getLocalMacAddress();
        if (TextUtils.isEmpty(IMEI)) {
            IMEI = mac;
        }
        operator = tm.getSimOperator();
        try {
            PackageManager packageManager = mContext.getPackageManager();
            PackageInfo packInfo = packageManager.getPackageInfo(mContext.getPackageName(), 0);
            versionName = packInfo.versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static int dp2px(double dp) {
        return (int) (dp * density + 0.5);
    }


    /**
     * 初始化屏幕信息, 必须在activity.onCreate()第一时间进行初始化
     * 此函数真实只会执行一次, 所有在每个Activity.onCreate()都调用此函数不会有多大开销
     * @param
     */
    private void initScreenInfo() {
        if (density != 0) {
            return;
        }
        WindowManager manager = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        DisplayMetrics metric = new DisplayMetrics();
        display.getMetrics(metric);
        density = metric.density;
        scaledDensity = metric.scaledDensity;
        screen = "" + metric.widthPixels + "*" + metric.heightPixels;
        screenResolution = metric.widthPixels * metric.heightPixels;
        if (metric.heightPixels >= metric.widthPixels) {
            screenWidthForPortrait = metric.widthPixels;
            screenHeightForPortrait = metric.heightPixels;
        } else {
            screenWidthForPortrait = metric.heightPixels;
            screenHeightForPortrait = metric.widthPixels;
        }
        screenStatusBarHeight = Resources.getSystem().getDimensionPixelSize(Resources.getSystem().getIdentifier("status_bar_height", "dimen", "android"));
    }

    /**
     * 获取MAC地址
     *
     * @return
     */
    private String getLocalMacAddress() {
        String mac = "000000";
        WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            WifiInfo info = wifi.getConnectionInfo();
            if (info != null) {
                mac = info.getMacAddress();
            }
        }
        return mac;
    }
}
