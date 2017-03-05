package com.kobot.lib.activity;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kobot.lib.BuildConfig;
import com.kobot.lib.R;
import com.kobot.lib.utils.Log;
import com.kobot.lib.utils.Methods;
import com.kobot.lib.video.multi.MultiUtil;
import com.kobot.lib.video.multi.activity.MultiAvActivity;
import com.kobot.lib.video.multi.service.MultiQavService;

import java.util.Timer;
import java.util.TimerTask;

public class CallingActivity extends BaseActivity
    implements View.OnClickListener {
  private static final String TAG = "CallingActivity";
  /**
   * 视频通话主叫
   */
  public static final int VIDEO_CALL = 0;

  /**
   * 视频通话被叫
   */
  public static final int VIDEO_BE_CALLED = 1;

  public static final String EXTRA_TYPE_TALK_WITH_PERSON = "type_talk_with_person";

  //  private RoundedImageView mHeadView;
  //  private TextView mNameView;
  private ImageView bigRing;
  private ImageView smallRing;
  private LinearLayout hangupBtn;
  private LinearLayout acceptBtn;
  private ImageView headView;
  private TextView hintTV;

  private String hint;
  private int mCallType;

  private ObjectAnimator bigRingAnimator;
  private ObjectAnimator smallRingAnimator;

  private MediaPlayer mMediaPlayer;

  private Timer timer = new Timer();
  private final int[] time = { 0 };

  private TimerTask timerTask = new TimerTask() {
    @Override public void run() {
      ++time[0];
      runOnUiThread(new Runnable() {
        @Override public void run() {
          if (time[0] > 60) {
            Methods.showToast(getString(R.string.call_time_out));
            cancel();
            if (myBinder != null) {
              myBinder.cancelCall();
            }
            finish();
          }
        }
      });

    }
  };

  private MultiQavService.MyBinder myBinder;

  private ServiceConnection connection = new ServiceConnection() {

    @Override public void onServiceDisconnected(ComponentName name) {
    }

    @Override public void onServiceConnected(ComponentName name,
        IBinder service) {
      myBinder = (MultiQavService.MyBinder) service;
      if (mCallType == VIDEO_CALL) {
        myBinder.startCalling(MultiQavService.mReceiveIdentifier,
                getIntent().getBooleanExtra("isMonitor", false));
      } else {
        if(myBinder.getState() instanceof MultiQavService.IdleState) {
          finish();
        }
      }
    }
  };

  private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

    @Override public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      Log.e(TAG, "WL_DEBUG onReceive action = " + action);
      if (action.equals(MultiUtil.ACTION_PERSON_TALK_FINISH)) {
        finish();
      } else if (action.equals(MultiUtil.ACTION_CREATE_ROOM_SUCCESS)) {
        startActivity(new Intent(context, MultiAvActivity.class)
            .putExtra(MultiUtil.EXTRA_RELATION_ID, intent.getLongExtra
                (MultiUtil.EXTRA_RELATION_ID, 0))
            .putExtra(MultiUtil.EXTRA_SELF_IDENTIFIER, intent.getStringExtra(
                MultiUtil.EXTRA_SELF_IDENTIFIER))
            .putExtra(MultiUtil.EXTRA_IS_MONITOR, intent.getBooleanExtra
                (MultiUtil.EXTRA_IS_MONITOR, false))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
      }
    }
  };

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_calling);
    getData();
    initView();
    Intent bindIntent = new Intent(this, MultiQavService.class);
    bindService(bindIntent, connection, BIND_AUTO_CREATE);
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(MultiUtil.ACTION_PERSON_TALK_FINISH);
    intentFilter.addAction(MultiUtil.ACTION_CREATE_ROOM_SUCCESS);
    registerReceiver(mBroadcastReceiver, intentFilter);
    if (mCallType == VIDEO_BE_CALLED) {
      Window win = getWindow();
      win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
          | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
          | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
          | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

      mMediaPlayer = MediaPlayer.create(context,
          getDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE));
      playRingTone();
    } else {
      timer.schedule(timerTask, 1000, 1000);
    }
  }

  @Override public void onBackPressed() {
    if (mCallType == VIDEO_CALL) {
      if (myBinder != null) {
        myBinder.cancelCall();
      }
      finish();
    } else {
      if (myBinder != null) {
        myBinder.denyInvite();
      }
    }
  }

  @Override protected void onStop() {
    super.onStop();
    Log.d("TalkWithPerson", "WL_DEBUG onStop");
    timerTask.cancel();
  }

  @Override protected void onPause() {
    super.onPause();
  }

  private void getData() {
    Intent intent = getIntent();
    mCallType = intent.getIntExtra(EXTRA_TYPE_TALK_WITH_PERSON, 0);
    if (mCallType == VIDEO_CALL) {
      hint = "正在呼叫";
    } else {
      String caller = getIntent().getStringExtra("caller");
      hint = (TextUtils.isEmpty(caller) ? "" : caller) + "来电";
    }
  }

  private void initView() {
    bigRing = (ImageView) findViewById(R.id.calling_ring_big);
    smallRing = (ImageView) findViewById(R.id.calling_ring_small);
    hangupBtn = (LinearLayout) findViewById(R.id.calling_hangup);
    acceptBtn = (LinearLayout) findViewById(R.id.calling_accept);
    headView = (ImageView) findViewById(R.id.calling_head);
    hintTV = (TextView) findViewById(R.id.calling_status);
    hangupBtn.setOnClickListener(this);
    acceptBtn.setOnClickListener(this);
    if (mCallType == VIDEO_CALL) {
      acceptBtn.setVisibility(View.GONE);
    } else {
      acceptBtn.setVisibility(View.VISIBLE);
    }
    updateHeadAndName();
    bigRingAnimator = ObjectAnimator
        .ofFloat(bigRing, "alpha", 0, 1, 0).setDuration(1000);
    smallRingAnimator = ObjectAnimator
        .ofFloat(smallRing, "alpha", 0, 1, 0).setDuration(1000);
    bigRingAnimator.setStartDelay(500);
    bigRingAnimator.setRepeatCount(ValueAnimator.INFINITE);
    smallRingAnimator.setRepeatCount(ValueAnimator.INFINITE);
    bigRingAnimator.start();
    smallRingAnimator.start();
  }

  /**
   * 获取的是铃声的Uri
   *
   * @param ctx
   * @param type
   * @return
   */
  private Uri getDefaultRingtoneUri(Context ctx, int type) {

    return RingtoneManager.getActualDefaultRingtoneUri(ctx, type);

  }

  /**
   * 播放铃声
   */

  private void playRingTone() {
    if(mMediaPlayer == null) {
      return;
    }
    mMediaPlayer.setLooping(true);
    mMediaPlayer.start();
  }

  private void updateHeadAndName() {
    headView.setImageResource(R.drawable.robot_logo);
    if(TextUtils.isEmpty(hint)) {
      hintTV.setText("视频通话呼叫中");
    } else {
      hintTV.setText(hint);
    }
  }

  @Override public void onClick(View v) {
    switch (v.getId()) {
    case R.id.calling_hangup:
      if (myBinder != null) {
        if (mCallType == VIDEO_CALL) {
          myBinder.cancelCall();
        } else {
          myBinder.denyInvite();
        }
      }
      finish();
      break;
    case R.id.calling_accept:
      if (myBinder != null) {
        myBinder.acceptInvite();
      }
      break;
    default:
      break;
    }
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    smallRingAnimator.cancel();
    bigRingAnimator.cancel();
    unbindService(connection);
    unregisterReceiver(mBroadcastReceiver);
    if (mCallType == VIDEO_BE_CALLED) {
      mMediaPlayer.stop();
    }
  }
}
