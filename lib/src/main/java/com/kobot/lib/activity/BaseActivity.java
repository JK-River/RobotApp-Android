package com.kobot.lib.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

/**
 * Created by machao on 2015/6/14.
 */
public abstract class BaseActivity extends Activity {
  protected Context context;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    context = this;
  }
}
