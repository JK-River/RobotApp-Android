package com.kobot.lib.video.multi.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.*;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.os.*;
import android.text.TextUtils;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import com.kobot.lib.R;
import com.kobot.lib.activity.BaseActivity;
import com.kobot.lib.common.Constant;
import com.kobot.lib.common.RobotApplication;
import com.kobot.lib.model.RecordResponse;
import com.kobot.lib.utils.Log;
import com.kobot.lib.utils.Methods;
import com.kobot.lib.video.multi.MultiExternalCaptureThread;
import com.kobot.lib.video.multi.MultiMyCheckable;
import com.kobot.lib.video.multi.MultiUtil;
import com.kobot.lib.video.multi.control.MultiAVUIControl;
import com.kobot.lib.video.multi.control.MultiQavsdkControl;
import com.kobot.lib.video.multi.service.MultiQavService;
import com.tencent.av.sdk.AVAudioCtrl;
import com.tencent.av.sdk.AVConstants;
import com.tencent.av.sdk.AVError;
import com.tencent.av.sdk.AVView;
import com.tencent.av.utils.PhoneStatusTools;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class MultiAvActivity extends BaseActivity implements OnClickListener {
  private static final String TAG = "MultiAvActivity";
  private static final int MSG_REQUEST_VIEW = 101;
  private static final int MSG_RECORD_ESCAPE_TIME = 102;
  private static final int DIALOG_INIT = 0;
  private static final int DIALOG_AT_ON_CAMERA = DIALOG_INIT + 1;
  private static final int DIALOG_ON_CAMERA_FAILED = DIALOG_AT_ON_CAMERA + 1;
  private static final int DIALOG_AT_OFF_CAMERA = DIALOG_ON_CAMERA_FAILED + 1;
  private static final int DIALOG_OFF_CAMERA_FAILED = DIALOG_AT_OFF_CAMERA + 1;
  private static final int DIALOG_AT_SWITCH_FRONT_CAMERA =
      DIALOG_OFF_CAMERA_FAILED + 1;
  private static final int DIALOG_SWITCH_FRONT_CAMERA_FAILED =
      DIALOG_AT_SWITCH_FRONT_CAMERA + 1;
  private static final int DIALOG_AT_SWITCH_BACK_CAMERA =
      DIALOG_SWITCH_FRONT_CAMERA_FAILED + 1;
  private static final int DIALOG_SWITCH_BACK_CAMERA_FAILED =
      DIALOG_AT_SWITCH_BACK_CAMERA + 1;

  private static final int DIALOG_AT_ON_EXTERNAL_CAPTURE =
      DIALOG_SWITCH_BACK_CAMERA_FAILED + 1;
  private static final int DIALOG_AT_ON_EXTERNAL_CAPTURE_FAILED =
      DIALOG_AT_ON_EXTERNAL_CAPTURE + 1;
  private static final int DIALOG_AT_OFF_EXTERNAL_CAPTURE =
      DIALOG_AT_ON_EXTERNAL_CAPTURE_FAILED + 1;
  private static final int DIALOG_AT_OFF_EXTERNAL_CAPTURE_FAILED =
      DIALOG_AT_OFF_EXTERNAL_CAPTURE + 1;
  private static final int DIALOG_CHANGE_AUTHRITY_OK = DIALOG_AT_OFF_EXTERNAL_CAPTURE_FAILED + 1;
  private static final int DIALOG_CHANGE_AUTHRITY_FAILED = DIALOG_CHANGE_AUTHRITY_OK + 1;

  private boolean mIsPaused = false;
  private int mOnOffCameraErrorCode = AVError.AV_OK;
  private int mSwitchCameraErrorCode = AVError.AV_OK;
  private int mEnableExternalCaptureErrorCode = AVError.AV_OK;
  private ProgressDialog mDialogInit = null;
  private ProgressDialog mDialogAtOnCamera = null;
  private ProgressDialog mDialogAtOffCamera = null;

  private ProgressDialog mDialogAtOnExternalCapture = null;
  private ProgressDialog mDialogAtOffExternalCapture = null;

  private ProgressDialog mDialogAtSwitchFrontCamera = null;
  private ProgressDialog mDialogAtSwitchBackCamera = null;
  private MultiQavsdkControl mQavsdkControl;
  private String mRecvIdentifier = "";
  private String mSelfIdentifier = "";
  OrientationEventListener mOrientationEventListener = null;
  int mRotationAngle = 0;
  private static final int TIMER_INTERVAL = 2000; //2s检查一次
  private TextView tvTipsMsg;
  private boolean showTips = false;
  private TextView tvShowTips;
  private ImageView switchCameraBtn;
  private ImageView closeBtn;
  private Context ctx;
  private View rootView;

  private MultiExternalCaptureThread inputStreamThread;
  private boolean isUserRendEnable = true;
  private ImageView recordButton;

  private ImageView leftBtn;
  private ImageView topBtn;
  private ImageView rightBtn;
  private ImageView bottomBtn;
  private TextView tv_record_time_escape;

  private TextView monitorToVideo;

  private RelativeLayout bottomBar;
  private LinearLayout directionControlView;
  private RelativeLayout videoView;
  private long mLastMsgTime = 0;
  private boolean isWithRobot = true;

  private boolean isFirstLoad = true;
  private boolean isMonitor = false;
  private boolean toContinue = false;

  private MultiAVUIControl mAVUIControl;

  private MultiQavService.MyBinder myBinder;

  private int timeRetry = 0;
  private Handler handler = new Handler() {
    @Override public void handleMessage(Message msg) {
      super.handleMessage(msg);
      if (msg.what == MSG_REQUEST_VIEW) {
        mQavsdkControl.reOpenRemoteVideo();
        ++timeRetry;
        if (timeRetry < 6) {
          sendMessageDelayed(Message.obtain(msg), 3000);
        }
      }
    }
  };

  private Handler recordEscapeTimeHandler = new Handler() {
    public void handleMessage(Message msg) {
      if (msg.what == MSG_RECORD_ESCAPE_TIME) {
        if (tv_record_time_escape != null) {
          tv_record_time_escape.setVisibility(View.VISIBLE);
          int time_escape = Integer
              .parseInt(tv_record_time_escape.getTag().toString());
          if (time_escape >= 20) {
            stopRecord();
          } else {
            time_escape++;
            int h = time_escape / (60 * 60);
            int m = (time_escape % (60 * 60)) / 60;
            int s = ((time_escape % (60 * 60)) % 60);
            String time = String.format("%02d:%02d:%02d", h, m, s);
            tv_record_time_escape.setText(time);
            tv_record_time_escape.setTag(time_escape);
            sendEmptyMessageDelayed(MSG_RECORD_ESCAPE_TIME, 1000);
          }
        }
      }
    }
  };

  private ServiceConnection connection = new ServiceConnection() {

    @Override public void onServiceDisconnected(ComponentName name) {
    }

    @Override public void onServiceConnected(ComponentName name,
        IBinder service) {
      myBinder = (MultiQavService.MyBinder) service;
      if(isMonitor) {
        sendFullScreenMsg(true);
      }
    }
  };

  private BroadcastReceiver connectionReceiver = new BroadcastReceiver() {
    @Override public void onReceive(Context context, Intent intent) {
      int netType = MultiUtil.getNetWorkType(ctx);
      Log.e(TAG, "WL_DEBUG connectionReceiver getNetWorkType = " + netType);
      mQavsdkControl.setNetType(netType);
    }
  };
  private MultiMyCheckable mMuteCheckable = new MultiMyCheckable(true) {
    @Override protected void onCheckedChanged(boolean checked) {
      ImageView button = (ImageView) findViewById(R.id.qav_bottombar_mute);
      AVAudioCtrl avAudioCtrl = mQavsdkControl.getAVContext().getAudioCtrl();

      if (checked) {
        button.setSelected(false);
        avAudioCtrl.enableMic(true);
      } else {
        button.setSelected(true);
        avAudioCtrl.enableMic(false);
      }
    }
  };
  Timer timer = new Timer();
  TimerTask task = new TimerTask() {
    public void run() {

      runOnUiThread(new Runnable() {
        public void run() {
          if (showTips) {
            if (tvTipsMsg != null) {
              String strTips = mQavsdkControl.getQualityTips();
              if (!TextUtils.isEmpty(strTips)) {
                tvTipsMsg.setText(strTips);
              }
            }
          } else {
            tvTipsMsg.setText("");
          }
        }
      });
    }
  };
  private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

    @Override public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      Log.d(TAG, "WL_DEBUG onReceive action = " + action);
      if (action.equals(MultiUtil.ACTION_SURFACE_CREATED)) {
        if (!isMonitor) {
          locateCameraPreview();

          boolean isEnable = mQavsdkControl.getIsEnableCamera();
          mOnOffCameraErrorCode = mQavsdkControl.toggleEnableCamera();
          if (mOnOffCameraErrorCode != AVError.AV_OK) {
            showDialog(
                isEnable ? DIALOG_OFF_CAMERA_FAILED : DIALOG_ON_CAMERA_FAILED);
            mQavsdkControl.setIsInOnOffCamera(false);
          }
        }

        refreshCameraUI();

        AVAudioCtrl avAudioCtrl = mQavsdkControl.getAVContext().getAudioCtrl();
//        if (isMonitor) {
//          avAudioCtrl.enableMic(false);
//        } else {
//          avAudioCtrl.enableMic(true);
//        }
        avAudioCtrl.enableMic(false);

      } else if (action.equals(MultiUtil.ACTION_VIDEO_CLOSE)) {
        String identifier = intent.getStringExtra(MultiUtil.EXTRA_IDENTIFIER);
        int videoSrcType = intent.getIntExtra(MultiUtil.EXTRA_VIDEO_SRC_TYPE,
            AVView.VIDEO_SRC_TYPE_NONE);
        mRecvIdentifier = identifier;
        if (!TextUtils.isEmpty(mRecvIdentifier)
            && videoSrcType != AVView.VIDEO_SRC_TYPE_NONE) {
          mQavsdkControl
              .setRemoteHasVideo(false, mRecvIdentifier, videoSrcType);
        }
      } else if (action.equals(MultiUtil.ACTION_VIDEO_SHOW)) {
        String identifier = intent.getStringExtra(MultiUtil.EXTRA_IDENTIFIER);
        int videoSrcType = intent.getIntExtra(MultiUtil.EXTRA_VIDEO_SRC_TYPE,
            AVView.VIDEO_SRC_TYPE_NONE);
        mRecvIdentifier = identifier;
        if (!TextUtils.isEmpty(mRecvIdentifier)
            && videoSrcType != AVView.VIDEO_SRC_TYPE_NONE) {
          mQavsdkControl.setRemoteHasVideo(true, mRecvIdentifier, videoSrcType);
          // handler.sendMessageDelayed(Message.obtain(handler,
          // MSG_REQUEST_VIEW), 5000);
        }
      } else if (action.equals(MultiUtil.ACTION_ENABLE_CAMERA_COMPLETE)) {
        refreshCameraUI();

        mOnOffCameraErrorCode = intent
            .getIntExtra(MultiUtil.EXTRA_AV_ERROR_RESULT, AVError.AV_OK);
        boolean isEnable = intent
            .getBooleanExtra(MultiUtil.EXTRA_IS_ENABLE, false);

        if (mOnOffCameraErrorCode == AVError.AV_OK) {
          if (!mIsPaused) {
            mQavsdkControl.setSelfId(mSelfIdentifier);
            mQavsdkControl.setLocalHasVideo(isEnable, mSelfIdentifier);
          }
        } else {
          showDialog(
              isEnable ? DIALOG_ON_CAMERA_FAILED : DIALOG_OFF_CAMERA_FAILED);
        }
        // 开启渲染回调的接口
        // mQavsdkControl.setRenderCallback();
      } else if (action
          .equals(MultiUtil.ACTION_ENABLE_EXTERNAL_CAPTURE_COMPLETE)) {
        refreshCameraUI();
        mOnOffCameraErrorCode = intent
            .getIntExtra(MultiUtil.EXTRA_AV_ERROR_RESULT, AVError.AV_OK);
        boolean isEnable = intent
            .getBooleanExtra(MultiUtil.EXTRA_IS_ENABLE, false);

        if (mOnOffCameraErrorCode == AVError.AV_OK) {
          // 打开外部摄像头之后就开始传输，用户可以实现自己的逻辑
          // test

          if (isEnable) {
            inputStreamThread = new MultiExternalCaptureThread(
                getApplicationContext());
            inputStreamThread.start();
          } else {
            if (inputStreamThread != null) {
              inputStreamThread.canRun = false;
              inputStreamThread = null;
            }
          }
        } else {
          showDialog(isEnable ?
              DIALOG_AT_ON_EXTERNAL_CAPTURE_FAILED :
              DIALOG_AT_OFF_EXTERNAL_CAPTURE_FAILED);
        }
      } else if (action.equals(MultiUtil.ACTION_SWITCH_CAMERA_COMPLETE)) {
        refreshCameraUI();

        mSwitchCameraErrorCode = intent
            .getIntExtra(MultiUtil.EXTRA_AV_ERROR_RESULT, AVError.AV_OK);
        boolean isFront = intent
            .getBooleanExtra(MultiUtil.EXTRA_IS_FRONT, false);
        if (mSwitchCameraErrorCode != AVError.AV_OK) {
          showDialog(isFront ?
              DIALOG_SWITCH_FRONT_CAMERA_FAILED :
              DIALOG_SWITCH_BACK_CAMERA_FAILED);
        }
      } else if (action.equals(MultiUtil.ACTION_MEMBER_CHANGE)) {
        mQavsdkControl.onMemberChange();
      } else if (action.equals(MultiUtil.ACTION_OUTPUT_MODE_CHANGE)) {
        updateHandfreeButton();
      } else if (action.equals(MultiUtil.ACTION_AV_ACTIVITY_FINISH)) {
        mQavsdkControl.getAVContext().getAudioCtrl().enableMic(false);
        finish();
      } else if (action.equals(MultiUtil.ACTION_TOGGLE_CONTROL_BARS)) {
        toggleShowBars();
      } else if (action.equals(MultiUtil.ACTION_CLOSE_ROOM_COMPLETE)) {
        if (isFinishing()) {

        } else {
          finish();
        }
      } else if (action.equals(MultiUtil.ACTION_CHANGE_AUTHRITY)) {
        int result = intent
            .getIntExtra(MultiUtil.EXTRA_AV_ERROR_RESULT, AVError.AV_OK);
        if (result == AVError.AV_OK) {
          showDialog(DIALOG_CHANGE_AUTHRITY_OK);
        } else {
          showDialog(DIALOG_CHANGE_AUTHRITY_FAILED);
        }
      } else if(action.equals(Constant.ACTION_RECORD_FAIL)) {
        RecordResponse response = (RecordResponse) intent.getSerializableExtra(Constant
            .EXTRA_RECORD_FAIL_RESPONSE);
        if(response == null || response.errorCode == 0) {
          return;
        }
        String errMsg = response.errorMsg;
        if(TextUtils.isEmpty(errMsg) && response.errorCode == 201) {
          errMsg = "机器人正在录制过程中";
        }
        Methods.showToast(errMsg);
        resetRecordUi();
        isUserRendEnable = true;
      } else if(action.equals(MultiUtil.ACTION_JUMP_TO_VIDEOING)) {
        String peerId = intent.getStringExtra("peerId");
        if (!TextUtils.isEmpty(peerId) && isMonitor) {
          toVideo();
        }
      }
    }
  };

  @Override protected void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "WL_DEBUG onCreate start");
    super.onCreate(savedInstanceState);
    ctx = this;
    rootView = LayoutInflater.from(this)
        .inflate(R.layout.activity_av_multi, null);
    setContentView(rootView);
    isMonitor = getIntent().getBooleanExtra(MultiUtil.EXTRA_IS_MONITOR, false);
    if (isMonitor) {
      findViewById(R.id.qav_bottombar_mute).setVisibility(View.INVISIBLE);
      findViewById(R.id.qav_bottom_bar_switch_camera).setVisibility(View.INVISIBLE);
    }
    // findViewById(R.id.qav_bottom_bar_hand_free).setOnClickListener(this);
    findViewById(R.id.qav_bottombar_mute).setOnClickListener(this);
    findViewById(R.id.qav_bottom_bar_capture).setOnClickListener(this);
    findViewById(R.id.qav_bottom_bar_record).setOnClickListener(this);
    switchCameraBtn = (ImageView) findViewById(R.id.qav_bottom_bar_switch_camera);
    switchCameraBtn.setOnClickListener(this);
    findViewById(R.id.av_controller_left).setOnClickListener(this);
    findViewById(R.id.av_controller_top).setOnClickListener(this);
    findViewById(R.id.av_controller_right).setOnClickListener(this);
    findViewById(R.id.av_controller_bottom).setOnClickListener(this);
    // findViewById(R.id.qav_bottombar_camera).setOnClickListener(this);
    closeBtn = (ImageView) findViewById(R.id.av_video_close);
    tv_record_time_escape = (TextView) findViewById(R.id.tv_record_time_escape);
    monitorToVideo = (TextView) findViewById(R.id.av_monitor_to_video);
    if(isMonitor) {
      monitorToVideo.setVisibility(View.VISIBLE);
    }
    monitorToVideo.setOnClickListener(this);
    closeBtn.setOnClickListener(this);
    // findViewById(R.id.qav_bottombar_enable_ex).setOnClickListener(this);

    recordButton = (ImageView) findViewById(R.id.qav_bottom_bar_record);
    // recordButton.setOnClickListener(this);

    tvTipsMsg = (TextView) findViewById(R.id.qav_tips_msg);
    tvTipsMsg.setTextColor(Color.RED);
    tvShowTips = (TextView) findViewById(R.id.qav_show_tips);
    tvShowTips.setTextColor(Color.GREEN);
    tvShowTips.setText(R.string.tips_show);
    tvShowTips.setOnClickListener(this);
    timer.schedule(task, TIMER_INTERVAL, TIMER_INTERVAL);

    bottomBar = (RelativeLayout) findViewById(R.id.qav_bottom_bar);
    directionControlView = (LinearLayout) findViewById(R.id.av_controller);

    // 注册广播
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(MultiUtil.ACTION_SURFACE_CREATED);
    intentFilter.addAction(MultiUtil.ACTION_VIDEO_SHOW);
    intentFilter.addAction(MultiUtil.ACTION_VIDEO_CLOSE);
    intentFilter.addAction(MultiUtil.ACTION_ENABLE_CAMERA_COMPLETE);
    intentFilter.addAction(MultiUtil.ACTION_ENABLE_EXTERNAL_CAPTURE_COMPLETE);
    intentFilter.addAction(MultiUtil.ACTION_SWITCH_CAMERA_COMPLETE);
    intentFilter.addAction(MultiUtil.ACTION_MEMBER_CHANGE);
    intentFilter.addAction(MultiUtil.ACTION_OUTPUT_MODE_CHANGE);
    intentFilter.addAction(MultiUtil.ACTION_TOGGLE_CONTROL_BARS);
    intentFilter.addAction(MultiUtil.ACTION_AV_ACTIVITY_FINISH);
    intentFilter.addAction(Constant.ACTION_RECORD_FAIL);
    intentFilter.addAction(MultiUtil.ACTION_JUMP_TO_VIDEOING);
    registerReceiver(mBroadcastReceiver, intentFilter);

    IntentFilter netIntentFilter = new IntentFilter();
    netIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
    registerReceiver(connectionReceiver, netIntentFilter);

    // showDialog(DIALOG_INIT);

    Intent bindIntent = new Intent(this, MultiQavService.class);
    bindService(bindIntent, connection, BIND_AUTO_CREATE);

    mQavsdkControl = ((RobotApplication) getApplication())
        .getMultiQavsdkControl();

    int netType = MultiUtil.getNetWorkType(ctx);
    Log.e(TAG, "WL_DEBUG connectionReceiver onCreate = " + netType);
    if (netType != AVConstants.NETTYPE_NONE) {
      mQavsdkControl.setNetType(MultiUtil.getNetWorkType(ctx));
    }
    mRecvIdentifier = getIntent().getExtras()
        .getString(MultiUtil.EXTRA_IDENTIFIER);
    mSelfIdentifier = getIntent().getExtras()
        .getString(MultiUtil.EXTRA_SELF_IDENTIFIER);
    toContinue = getIntent().getBooleanExtra(MultiUtil.EXTRA_TO_CONTINUE,
        false);
    if (mQavsdkControl.getAVContext() != null) {
      mAVUIControl = new MultiAVUIControl(getApplication(),
          rootView.findViewById(R.id.av_video_layer_ui));
      mAVUIControl.getGLRootView().setVisibility(View.VISIBLE);
      if(toContinue) {
        mQavsdkControl.onContinue(mAVUIControl);
      } else {
        mQavsdkControl.onCreate(mAVUIControl);
      }
      updateHandfreeButton();
    } else {
      finish();
    }
//    registerOrientationListener();
    if (mQavsdkControl != null) {
      mQavsdkControl.setRotation(180);
    }
    Window win = getWindow();
    win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
  }

  @Override public void onResume() {
    super.onResume();
    mIsPaused = false;
//    mQavsdkControl.onResume();
    mAVUIControl.onResume();
    refreshCameraUI();
    if (mOnOffCameraErrorCode != AVError.AV_OK) {
      showDialog(DIALOG_ON_CAMERA_FAILED);
    }
    startOrientationListener();
    // if (getRequestedOrientation()
    // != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
    // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    // }
    showBars();
  }

  private int pauseTime = 0;
  @Override protected void onPause() {
    super.onPause();
    ++pauseTime;
    KeyguardManager mKeyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
    if (mKeyguardManager.inKeyguardRestrictedInputMode() && pauseTime < 2 &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return;
    }
    mIsPaused = true;
//    mQavsdkControl.onPause();
    mAVUIControl.onPause();
    refreshCameraUI();
    if (mOnOffCameraErrorCode != AVError.AV_OK) {
      showDialog(DIALOG_OFF_CAMERA_FAILED);
    }
    finish();
    stopOrientationListener();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    Log.e("memoryLeak", "memoryLeak avactivity onDestroy");
    handler.removeCallbacksAndMessages(null);
    handler = null;
    if(!toContinue) {
      if (myBinder != null) {
        myBinder.onAvFinish();
      }
      mQavsdkControl.onDestroy();
      mAVUIControl.onDestroy();
    } else {
      if (!isMonitor) {
        boolean isEnable = mQavsdkControl.getIsEnableCamera();
        if(isEnable) {
          mOnOffCameraErrorCode = mQavsdkControl.toggleEnableCamera();
          if (mOnOffCameraErrorCode != AVError.AV_OK) {
            showDialog(
                isEnable ? DIALOG_OFF_CAMERA_FAILED : DIALOG_ON_CAMERA_FAILED);
            mQavsdkControl.setIsInOnOffCamera(false);
          }
        }
      }
      sendFullScreenMsg(false);
    }

    unbindService(connection);

    // 注销广播
    if (mBroadcastReceiver != null) {
      unregisterReceiver(mBroadcastReceiver);
    }
    if (connectionReceiver != null) {
      unregisterReceiver(connectionReceiver);
    }
    if (timer != null) {
      task.cancel();
      timer.cancel();
      task = null;
      timer = null;
    }
    Log.e("memoryLeak", "memoryLeak avactivity onDestroy end");
    Log.d(TAG, "WL_DEBUG onDestroy");

    if (inputStreamThread != null) {
      inputStreamThread.canRun = false;
      inputStreamThread = null;
    }
  }

  private void locateCameraPreview() {
    // SurfaceView localVideo = (SurfaceView)
    // findViewById(R.id.av_video_surfaceView);
    // MarginLayoutParams params = (MarginLayoutParams)
    // localVideo.getLayoutParams();
    // params.leftMargin = -3000;
    // localVideo.setLayoutParams(params);

    if (mDialogInit != null && mDialogInit.isShowing()) {
      mDialogInit.dismiss();
    }
  }

  @Override public void onClick(View v) {
    showBars();
    switch (v.getId()) {
    // case R.id.qav_bottom_bar_hand_free:
    // mQavsdkControl.getAVContext().getAudioCtrl().setAudioOutputMode(
    // mQavsdkControl.getHandfreeChecked() ?
    // AVAudioCtrl.OUTPUT_MODE_SPEAKER :
    // AVAudioCtrl.OUTPUT_MODE_HEADSET);
    // break;
    case R.id.av_monitor_to_video:
      toVideo();
      break;
    case R.id.qav_bottombar_mute:
      mMuteCheckable.toggle();
      break;
    case R.id.qav_bottom_bar_capture:
//      startCapture();
      break;
    // case R.id.qav_bottombar_camera:
    // boolean isEnable = mQavsdkControl.getIsEnableCamera();
    // mOnOffCameraErrorCode = mQavsdkControl.toggleEnableCamera();
    // refreshCameraUI();
    // if (mOnOffCameraErrorCode != AVError.AV_OK) {
    // showDialog(isEnable ? DIALOG_OFF_CAMERA_FAILED :
    // DIALOG_ON_CAMERA_FAILED);
    // mQavsdkControl.setIsInOnOffCamera(false);
    // refreshCameraUI();
    // }
    // break;
    case R.id.av_video_close:
      mQavsdkControl.getAVContext().getAudioCtrl().enableMic(false);
      finish();
      break;
    case R.id.qav_bottom_bar_switch_camera:
      boolean isFront = mQavsdkControl.getIsFrontCamera();
      mSwitchCameraErrorCode = mQavsdkControl.toggleSwitchCamera();
      refreshCameraUI();
      if (mSwitchCameraErrorCode != AVError.AV_OK) {
        showDialog(isFront ?
            DIALOG_SWITCH_BACK_CAMERA_FAILED :
            DIALOG_SWITCH_FRONT_CAMERA_FAILED);
        mQavsdkControl.setIsInSwitchCamera(false);
        refreshCameraUI();
      }
      break;
    case R.id.qav_show_tips:
      showTips = !showTips;
      if (showTips) {
        tvShowTips.setText(R.string.tips_close);
      } else {
        tvShowTips.setText(R.string.tips_show);
      }
      break;
    case R.id.av_controller_left:
      sendControlMsg(Constant.MSG_CONTROL_LEFT);
      break;
    case R.id.av_controller_right:
      sendControlMsg(Constant.MSG_CONTROL_RIGHT);
      break;
    case R.id.av_controller_top:
      sendControlMsg(Constant.MSG_CONTROL_UP);
      break;
    case R.id.av_controller_bottom:
      sendControlMsg(Constant.MSG_CONTROL_DOWN);
      break;

    // case R.id.qav_bottombar_enable_ex:
    // boolean isEnableExternalCapture =
    // mQavsdkControl.getIsEnableExternalCapture();
    // mEnableExternalCaptureErrorCode =
    // mQavsdkControl.enableExternalCapture(!isEnableExternalCapture);
    // refreshCameraUI();
    // if (mEnableExternalCaptureErrorCode != AVError.AV_OK) {
    // showDialog(isEnableExternalCapture ?
    // DIALOG_AT_OFF_EXTERNAL_CAPTURE_FAILED :
    // DIALOG_AT_ON_EXTERNAL_CAPTURE_FAILED);
    // mQavsdkControl.setIsOnOffExternalCapture(false);
    // refreshCameraUI();
    // }
    // break;

    case R.id.qav_bottom_bar_record:
      if (isUserRendEnable) {
        startRecord();
        // recordButton.setText(R.string.start_recording_video);
      } else {
        stopRecord();
        // recordButton.setText(R.string.stop_recording_video);
      }
      isUserRendEnable = !isUserRendEnable;
      break;

    default:
      break;
    }
  }

  private void toVideo() {
    monitorToVideo.setVisibility(View.GONE);
    isMonitor = false;
    findViewById(R.id.qav_bottombar_mute).setVisibility(View.VISIBLE);
    findViewById(R.id.qav_bottom_bar_switch_camera).setVisibility(View.VISIBLE);
    locateCameraPreview();

    boolean isEnable = mQavsdkControl.getIsEnableCamera();
    if(!isEnable) {
      mOnOffCameraErrorCode = mQavsdkControl.toggleEnableCamera();
      if (mOnOffCameraErrorCode != AVError.AV_OK) {
        showDialog(
            isEnable ? DIALOG_OFF_CAMERA_FAILED : DIALOG_ON_CAMERA_FAILED);
        mQavsdkControl.setIsInOnOffCamera(false);
      }
    }
    refreshCameraUI();

    AVAudioCtrl avAudioCtrl = mQavsdkControl.getAVContext().getAudioCtrl();
    avAudioCtrl.enableMic(true);
  }

  private void stopRecord() {
    /*int roomid = (int) mQavsdkControl.getAVContext().getRoom().getRoomId();

    TIMAvManager.RoomInfo roomInfo = TIMAvManager.getInstance().new RoomInfo();
    roomInfo.setRoomId(roomid);
    roomInfo.setRelationId(mQavsdkControl.getRoomId());
    TIMAvManager.getInstance().requestMultiVideoRecorderStop(roomInfo,
        new TIMValueCallBack<List<String>>() {
          @Override public void onError(int i, String s) {
            Log.e(TAG, "stop record error " + i + " : " + s);
            Toast.makeText(getApplicationContext(),
                "stop record error,try again", Toast.LENGTH_LONG).show();
          }

          @Override public void onSuccess(List<String> files) {
            for (String file : files) {
              Log.d(TAG, "stopRecord onSuccess file  " + file);
            }
            ((Button) findViewById(R.id.qav_bottom_bar_record))
                .setTextColor(getResources().getColor(R.color.white));
            ((Button) findViewById(R.id.qav_bottom_bar_record)).setText("录制");
            Toast.makeText(getApplicationContext(), "stop record success",
                Toast.LENGTH_SHORT).show();

          }
        });
    Log.d(TAG, "success");*/
    sendControlMsg(Constant.MSG_STOP_RECORD);
    resetRecordUi();
  }

  private void resetRecordUi() {
    recordButton.setImageResource(R.drawable.video_call_record_icon);
    recordEscapeTimeHandler.removeMessages(MSG_RECORD_ESCAPE_TIME);
    this.tv_record_time_escape.setVisibility(View.GONE);
    this.tv_record_time_escape.setTag(0);
  }

  public void startRecord() {
    /*int roomid = (int) mQavsdkControl.getAVContext().getRoom().getRoomId();
    Log.i(TAG, "roomid: " + roomid);

    TIMAvManager.RoomInfo roomInfo = TIMAvManager.getInstance().new RoomInfo();
    roomInfo.setRoomId(roomid);
    roomInfo.setRelationId(mQavsdkControl.getRoomId());

    TIMAvManager.RecordParam mRecordParam = TIMAvManager
        .getInstance().new RecordParam();
    mRecordParam.setFilename(
        String.valueOf(mQavsdkControl.getAVContext().getRoom().getRoomId()));
    mRecordParam.setClassId(5760);
    mRecordParam.setTransCode(true);
    mRecordParam.setSreenShot(true);
    mRecordParam.setWaterMark(false);
    mRecordParam.addTag(String.valueOf(MyInfo.sUserInfo.getCurRobotId()));
    mRecordParam.setFilename(mQavsdkControl.getRoomId() + "_" +
        QALSDKManager.getInstance().getServetTimeSecondInterv() + System.currentTimeMillis()/1000);

    TIMAvManager.getInstance()
        .requestMultiVideoRecorderStart(roomInfo, mRecordParam,
            new TIMCallBack() {
              @Override public void onError(int i, String s) {
                Log.e(TAG, "Record error" + i + " : " + s);
                Toast.makeText(getApplicationContext(),
                    "start record error,try again", Toast.LENGTH_LONG).show();
              }

              @Override public void onSuccess() {
                ((Button) findViewById(R.id.qav_bottom_bar_record))
                    .setTextColor(getResources().getColor(R.color.red));
                ((Button) findViewById(R.id.qav_bottom_bar_record))
                    .setText("停录");
                Log.i(TAG, "begin to record");
                Toast.makeText(getApplicationContext(), "start record now ",
                    Toast.LENGTH_SHORT).show();
              }
            });*/
    recordButton.setImageResource(R.drawable.video_call_recording_icon);
    sendControlMsg(Constant.MSG_START_RECORD);
    this.tv_record_time_escape.setVisibility(View.VISIBLE);
    this.tv_record_time_escape.setText("00:00:00");
    recordEscapeTimeHandler
        .sendEmptyMessageDelayed(MSG_RECORD_ESCAPE_TIME, 1000);
  }

  @Override protected Dialog onCreateDialog(int id) {
    Dialog dialog = null;
    showBars();
    switch (id) {
    case DIALOG_INIT:
      dialog = mDialogInit = MultiUtil
          .newProgressDialog(this, R.string.interface_initialization);
      break;
    case DIALOG_AT_ON_CAMERA:
      dialog = mDialogAtOnCamera = MultiUtil
          .newProgressDialog(this, R.string.at_on_camera);
      break;
    case DIALOG_ON_CAMERA_FAILED:
      dialog = MultiUtil.newErrorDialog(this, R.string.on_camera_failed);
      break;
    case DIALOG_AT_OFF_CAMERA:
      dialog = mDialogAtOffCamera = MultiUtil
          .newProgressDialog(this, R.string.at_off_camera);
      break;
    case DIALOG_OFF_CAMERA_FAILED:
      dialog = MultiUtil.newErrorDialog(this, R.string.off_camera_failed);
      break;

    // case DIALOG_AT_ON_EXTERNAL_CAPTURE:
    // dialog = mDialogAtOnExternalCapture =
    // MultiUtil.newProgressDialog(this, R.string.at_on_external_capture);
    // break;
    // case DIALOG_AT_ON_EXTERNAL_CAPTURE_FAILED:
    // dialog = MultiUtil.newErrorDialog(this,
    // R.string.on_external_capture_failed);
    // break;
    // case DIALOG_AT_OFF_EXTERNAL_CAPTURE:
    // dialog = mDialogAtOffExternalCapture =
    // MultiUtil.newProgressDialog(this, R.string.at_off_external_capture);
    // break;
    // case DIALOG_AT_OFF_EXTERNAL_CAPTURE_FAILED:
    // dialog = MultiUtil.newErrorDialog(this,
    // R.string.off_external_capture_failed);
    // break;

    case DIALOG_AT_SWITCH_FRONT_CAMERA:
      dialog = mDialogAtSwitchFrontCamera = MultiUtil
          .newProgressDialog(this, R.string.at_switch_front_camera);
      break;
    case DIALOG_SWITCH_FRONT_CAMERA_FAILED:
      dialog = MultiUtil
          .newErrorDialog(this, R.string.switch_front_camera_failed);
      break;
    case DIALOG_AT_SWITCH_BACK_CAMERA:
      dialog = mDialogAtSwitchBackCamera = MultiUtil
          .newProgressDialog(this, R.string.at_switch_back_camera);
      break;
    case DIALOG_SWITCH_BACK_CAMERA_FAILED:
      dialog = MultiUtil
          .newErrorDialog(this, R.string.switch_back_camera_failed);
      break;

    default:
      break;
    }
    return dialog;
  }

  @Override protected void onPrepareDialog(int id, Dialog dialog) {
    switch (id) {
    case DIALOG_ON_CAMERA_FAILED:
    case DIALOG_OFF_CAMERA_FAILED:
      ((AlertDialog) dialog).setMessage(
          getString(R.string.error_code_prefix) + mOnOffCameraErrorCode);
      break;
    case DIALOG_SWITCH_FRONT_CAMERA_FAILED:
    case DIALOG_SWITCH_BACK_CAMERA_FAILED:
      ((AlertDialog) dialog).setMessage(
          getString(R.string.error_code_prefix) + mSwitchCameraErrorCode);
      break;

    case DIALOG_AT_ON_EXTERNAL_CAPTURE_FAILED:
    case DIALOG_AT_OFF_EXTERNAL_CAPTURE_FAILED:
      ((AlertDialog) dialog).setMessage(getString(R.string.error_code_prefix)
          + mEnableExternalCaptureErrorCode);
      break;

    default:
      break;
    }
  }

  private void refreshCameraUI() {
    boolean isEnable = mQavsdkControl.getIsEnableCamera();
    boolean isFront = mQavsdkControl.getIsFrontCamera();
    boolean isInOnOffCamera = mQavsdkControl.getIsInOnOffCamera();
    boolean isInSwitchCamera = mQavsdkControl.getIsInSwitchCamera();
    // Button buttonEnableCamera = (Button)
    // findViewById(R.id.qav_bottombar_camera);
    Button buttonSwitchCamera = (Button) findViewById(
        R.id.qav_bottombar_switchcamera);
    // Button external_capture_status =
    // (Button)findViewById(R.id.qav_bottombar_enable_ex);
    boolean isExternalCaptureEnable = mQavsdkControl
        .getIsEnableExternalCapture();
    boolean isOnOffExternalCapture = mQavsdkControl
        .getIsInOnOffExternalCapture();
    // if (isExternalCaptureEnable) {
    // external_capture_status.setSelected(true);
    // external_capture_status.setText(R.string.video_close_external_acc_txt);
    // } else {
    // external_capture_status.setSelected(false);
    // external_capture_status.setText(R.string.video_open_external_acc_txt);
    // }
    //
    // if (isEnable) {
    // buttonEnableCamera.setSelected(true);
    // buttonEnableCamera.setText(R.string.audio_close_camera_acc_txt);
    // buttonSwitchCamera.setVisibility(View.VISIBLE);
    // } else {
    // buttonEnableCamera.setSelected(false);
    // buttonEnableCamera.setText(R.string.audio_open_camera_acc_txt);
    // buttonSwitchCamera.setVisibility(View.GONE);
    // }

		/*
		 * if (isFront) {
		 * buttonSwitchCamera.setText(R.string.gaudio_switch_camera_front_acc_txt
		 * ); } else {
		 * buttonSwitchCamera.setText(R.string.gaudio_switch_camera_back_acc_txt
		 * ); }
		 */

    if (isInOnOffCamera) {
      if (isEnable) {
        MultiUtil
            .switchWaitingDialog(this, mDialogAtOffCamera, DIALOG_AT_OFF_CAMERA,
                true);
        MultiUtil
            .switchWaitingDialog(this, mDialogAtOnCamera, DIALOG_AT_ON_CAMERA,
                false);
      } else {
        MultiUtil
            .switchWaitingDialog(this, mDialogAtOffCamera, DIALOG_AT_OFF_CAMERA,
                false);
        MultiUtil
            .switchWaitingDialog(this, mDialogAtOnCamera, DIALOG_AT_ON_CAMERA,
                true);
      }
    } else {
      MultiUtil
          .switchWaitingDialog(this, mDialogAtOffCamera, DIALOG_AT_OFF_CAMERA,
              false);
      MultiUtil
          .switchWaitingDialog(this, mDialogAtOnCamera, DIALOG_AT_ON_CAMERA,
              false);
    }

    if (isInSwitchCamera) {
      if (isFront) {
        MultiUtil.switchWaitingDialog(this, mDialogAtSwitchBackCamera,
            DIALOG_AT_SWITCH_BACK_CAMERA, true);
        MultiUtil.switchWaitingDialog(this, mDialogAtSwitchFrontCamera,
            DIALOG_AT_SWITCH_FRONT_CAMERA, false);
      } else {
        MultiUtil.switchWaitingDialog(this, mDialogAtSwitchBackCamera,
            DIALOG_AT_SWITCH_BACK_CAMERA, false);
        MultiUtil.switchWaitingDialog(this, mDialogAtSwitchFrontCamera,
            DIALOG_AT_SWITCH_FRONT_CAMERA, true);
      }
    } else {
      MultiUtil.switchWaitingDialog(this, mDialogAtSwitchBackCamera,
          DIALOG_AT_SWITCH_BACK_CAMERA, false);
      MultiUtil.switchWaitingDialog(this, mDialogAtSwitchFrontCamera,
          DIALOG_AT_SWITCH_FRONT_CAMERA, false);
    }

    if (isOnOffExternalCapture) {
      if (isEnable) {
        MultiUtil.switchWaitingDialog(this, mDialogAtOnExternalCapture,
            DIALOG_AT_OFF_EXTERNAL_CAPTURE, true);
        MultiUtil.switchWaitingDialog(this, mDialogAtOffExternalCapture,
            DIALOG_AT_ON_EXTERNAL_CAPTURE, false);
      } else {
        MultiUtil.switchWaitingDialog(this, mDialogAtOnExternalCapture,
            DIALOG_AT_OFF_EXTERNAL_CAPTURE, false);
        MultiUtil.switchWaitingDialog(this, mDialogAtOffExternalCapture,
            DIALOG_AT_ON_EXTERNAL_CAPTURE, true);
      }
    } else {
      MultiUtil.switchWaitingDialog(this, mDialogAtOnExternalCapture,
          DIALOG_AT_OFF_EXTERNAL_CAPTURE, false);
      MultiUtil.switchWaitingDialog(this, mDialogAtOffExternalCapture,
          DIALOG_AT_ON_EXTERNAL_CAPTURE, false);
    }
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    // getMenuInflater().inflate(R.menu.stream_set, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    boolean result = false;
    // Intent intent = new Intent(this, StreamSetActivity.class);
    // switch (item.getItemId()) {
    // case R.id.action_input_set:
    // intent.putExtra("isInputSet", true);
    // startActivity(intent);
    // break;
    // case R.id.action_output_set:
    // intent.putExtra("isInputSet", false);
    // startActivity(intent);
    // break;
    //
    // default:
    // break;
    // }

    return result;
  }

  private void updateHandfreeButton() {
		/*
		 * Button button = (Button) findViewById(R.id.qav_bottombar_handfree);
		 * 
		 * if (mQavsdkControl.getHandfreeChecked()) { button.setSelected(true);
		 * button.setText(R.string.audio_switch_to_speaker_mode_acc_txt); } else
		 * { button.setSelected(false);
		 * button.setText(R.string.audio_switch_to_headset_mode_acc_txt); }
		 */
  }

  class VideoOrientationEventListener extends OrientationEventListener {

    boolean mbIsTablet = false;

    public VideoOrientationEventListener(Context context, int rate) {
      super(context, rate);
      mbIsTablet = PhoneStatusTools.isTablet(context);
    }

    int mLastOrientation = -25;

    @Override public void onOrientationChanged(int orientation) {
      if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
        if (mLastOrientation != orientation) {

        }
        mLastOrientation = orientation;
        return;
      }

      if (mLastOrientation < 0) {
        mLastOrientation = 0;
      }

      if (((orientation - mLastOrientation) < 20) && (
          (orientation - mLastOrientation) > -20)) {
        return;
      }

      if (mbIsTablet) {
        orientation -= 90;
        if (orientation < 0) {
          orientation += 360;
        }
      }

      mLastOrientation = orientation;

      if (orientation > 314 || orientation < 45) {
        if (mQavsdkControl != null) {
          mQavsdkControl.setRotation(270);

        }

        mRotationAngle = 0;
      } else if (orientation > 44 && orientation < 135) {
        if (mQavsdkControl != null) {
          mQavsdkControl.setRotation(0);
        }
        mRotationAngle = 90;
      } else if (orientation > 134 && orientation < 225) {
        if (mQavsdkControl != null) {
          mQavsdkControl.setRotation(90);
        }
        mRotationAngle = 180;
      } else {
        if (mQavsdkControl != null) {
          mQavsdkControl.setRotation(180);
        }
        mRotationAngle = 270;
      }
    }
  }

  private void sendControlMsg(int type) {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("type", type);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    if (myBinder != null) {
      myBinder.sendControlMsg(jsonObject);
    }
  }

  private void sendFullScreenMsg(boolean isFullScreen) {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("type", Constant.MSG_FULL_SCREEN);
      jsonObject.put("isFullScreen", isFullScreen);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    if (myBinder != null) {
      myBinder.sendControlMsg(jsonObject);
    }
  }

  void registerOrientationListener() {
    if (mOrientationEventListener == null) {
      mOrientationEventListener = new VideoOrientationEventListener(
          super.getApplicationContext(), SensorManager.SENSOR_DELAY_UI);
    }
  }

  void startOrientationListener() {
    if (mOrientationEventListener != null) {
      mOrientationEventListener.enable();
    }
  }

  void stopOrientationListener() {
    if (mOrientationEventListener != null) {
      mOrientationEventListener.disable();
    }
  }

  private void toggleShowBars() {
    if (bottomBar.getVisibility() == View.VISIBLE) {
      handler.removeCallbacksAndMessages(null);
      dismissBars();
    } else {
      showBars();
    }
  }

  private void showBars() {
    if(handler == null) {
      return;
    }
    handler.removeCallbacksAndMessages(null);
    if (isWithRobot) {
      directionControlView.setVisibility(View.VISIBLE);
    }
    bottomBar.setVisibility(View.VISIBLE);
    closeBtn.setVisibility(View.VISIBLE);

    dismissBarsDelay();
  }

  private void dismissBarsDelay() {
    if(handler == null) {
      return;
    }
    handler.postDelayed(new Runnable() {
      @Override public void run() {
        dismissBars();
      }
    }, 5000);
  }

  private void dismissBars() {
    directionControlView.setVisibility(View.GONE);
    bottomBar.setVisibility(View.GONE);
    closeBtn.setVisibility(View.GONE);
  }
}