package com.kobot.lib.common;

public class Constant {

  public static final String PACKAGE_NAME = "com.robot.lib";

  public static final int TEXT_MSG_FAILED_FOR_TOO_LOGNG = 85;
  public static final int SEND_MSG_FAILED_FOR_PEER_NOT_LOGIN = 6011;

  public static final int MSG_CONTROL_LEFT = 1001;
  public static final int MSG_CONTROL_RIGHT = 1003;
  public static final int MSG_CONTROL_UP = 1005;
  public static final int MSG_CONTROL_DOWN = 1007;

  public static final int MSG_START_RECORD = 1911;
  public static final int MSG_STOP_RECORD = 1912;
  public static final int MSG_RECORD_FAIL = 1913;
  public static final int MSG_FULL_SCREEN = 1914;


  public static final String ACTION_BE_KICKED_OUT = PACKAGE_NAME + "action_be_kicked_out";

  public static final String ACTION_RECORD_FAIL =
          PACKAGE_NAME + "action_record_fail";
  public static final String EXTRA_RECORD_FAIL_RESPONSE =
      "extra_record_fail_response";

}
