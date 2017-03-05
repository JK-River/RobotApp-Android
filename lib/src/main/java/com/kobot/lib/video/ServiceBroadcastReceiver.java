package com.kobot.lib.video;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.kobot.lib.utils.Log;
import com.kobot.lib.video.multi.service.MultiQavService;

/**
 * Created by machao on 16/1/30.
 */
public class ServiceBroadcastReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context context, Intent intent) {
    Log.d("receiver", "receive broadcast");
    Intent service = new Intent(context, MultiQavService.class);
    context.startService(service);
  }
}
