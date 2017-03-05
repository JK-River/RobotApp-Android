package com.kobot.lib.video.multi;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import tencent.tls.platform.TLSAccountHelper;
import tencent.tls.platform.TLSLoginHelper;
import tencent.tls.platform.TLSUserInfo;
import tencent.tls.report.QLog;

import java.util.List;

public class MultiTLSHelper {
  private static final String TAG = MultiTLSHelper.class.getSimpleName();

  Context context;

  public static String userID = null;
  public static TLSLoginHelper loginHelper;
  public static TLSAccountHelper accountHelper;
  static int LANG = 2052;
  static String appVer = "1.0";
  static int country = 86;
  static boolean NoPwdReg = false;
  public final static String PREFIX = "86-";

  public synchronized boolean init(Context context) {

    this.context = context;
    InitTLSSDK();
    return true;
  }

  private void InitTLSSDK() {
    clearHost();
    //	TLSLoginHelper.setLogcat(true);
    loginHelper = TLSLoginHelper.getInstance()
        .init(context, Long.valueOf(VideoConstants.sdkAppId),
            Integer.valueOf(VideoConstants.accountType), appVer);
    loginHelper.setTimeOut(3000);
    loginHelper.setLocalId(LANG);
    //	loginHelper.setTestHost("113.108.76.104", false); // todo 设置wtlogin测试环境, 注释之，则是走SSO测试环境

    accountHelper = TLSAccountHelper.getInstance()
        .init(context, Long.valueOf(VideoConstants.sdkAppId),
            Integer.valueOf(VideoConstants.accountType), appVer);
    accountHelper.setCountry(country);
    accountHelper.setTimeOut(3000);
    accountHelper.setLocalId(LANG);

    List<TLSUserInfo> allInfos = loginHelper.getAllUserInfo();
    for (int i = 0, len = allInfos.size(); i < len; i++) {
      TLSUserInfo info = allInfos.get(i);
      QLog.i("userID: " + info.identifier + ", sdkAppid: " + info.accountType);
    }

    TLSUserInfo userInfo = loginHelper.getLastUserInfo();
    if (userInfo != null) {
      userID = userInfo.identifier;
      Log.d(TAG, "userID:" + userID);
    }
  }

  private void clearHost() {
    String host = "";
    SharedPreferences settings = context
        .getSharedPreferences("WLOGIN_DEVICE_INFO", 0);
    SharedPreferences.Editor editor = settings.edit();
    editor.putString("host1", host);
    editor.putString("host2", host);
    editor.putString("wap-host1", host);
    editor.putString("wap-host2", host);
    editor.commit();
  }

}
