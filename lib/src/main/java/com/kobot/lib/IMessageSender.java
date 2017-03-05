package com.kobot.lib;

import org.json.JSONObject;

/**
 * Created by machao on 16/7/31.
 */
public interface IMessageSender {
  public void sendMessage(JSONObject object, long robotId);
}
