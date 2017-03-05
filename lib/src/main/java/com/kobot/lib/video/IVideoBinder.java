package com.kobot.lib.video;

import org.json.JSONObject;

/**
 * Created by machao on 15/12/12.
 */
public interface IVideoBinder {
  public void startCalling(String receiveIdentifier, boolean isVideo);

  //取消呼叫
  public void cancelCall();

  public void acceptInvite();

  public void denyInvite();

  public void sendControlMsg(JSONObject object);

  public void sendMessage(JSONObject object, String id);

  public void onAvFinish();
}
