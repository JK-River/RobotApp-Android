package com.kobot.lib.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.kobot.lib.R;

/**
 * Created by machao on 16/1/2.
 */
public class LaunchActivity extends BaseActivity {

  private Handler handler = new Handler();

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_launch);
    handler.postDelayed(new Runnable() {
      @Override public void run() {
        startActivity(new Intent(context, MainActivity.class));
        finish();
      }
    }, 1000);
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    handler.removeCallbacksAndMessages(null);
  }
}
