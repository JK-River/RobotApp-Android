package com.kobot.lib.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.kobot.lib.R;
import com.kobot.lib.video.multi.service.MultiQavService;

/**
 * Created by machao on 15/11/17.
 */
public class MainActivity extends BaseActivity {

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Button callBtn = (Button) findViewById(R.id.main_call);
    callBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivity(new Intent(MainActivity.this, CallingActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      }
    });
    startService(new Intent(this, MultiQavService.class));
  }


}
