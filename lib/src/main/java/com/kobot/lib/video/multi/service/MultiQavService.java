package com.kobot.lib.video.multi.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.kobot.lib.R;
import com.kobot.lib.activity.CallingActivity;
import com.kobot.lib.common.Constant;
import com.kobot.lib.common.RobotApplication;
import com.kobot.lib.model.RecordResponse;
import com.kobot.lib.utils.Methods;
import com.kobot.lib.video.IVideoBinder;
import com.kobot.lib.video.multi.MultiLogcatHelper;
import com.kobot.lib.video.multi.MultiUtil;
import com.kobot.lib.video.multi.VideoConstants;
import com.kobot.lib.video.multi.control.MultiQavsdkControl;
import com.tencent.TIMConversation;
import com.tencent.TIMConversationType;
import com.tencent.TIMCustomElem;
import com.tencent.TIMElem;
import com.tencent.TIMElemType;
import com.tencent.TIMManager;
import com.tencent.TIMMessage;
import com.tencent.TIMMessageListener;
import com.tencent.TIMUserStatusListener;
import com.tencent.TIMValueCallBack;
import com.tencent.av.sdk.AVError;
import com.tencent.qalsdk.QALSDKManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by ma_chao on 2015/7/9.
 */
public class MultiQavService extends Service {

  private static final String TAG = "MultiQavService";

  private static final int TYPE_CALL = 1900;
  private static final int TYPE_CANCEL = 1901;
  private static final int TYPE_ACCEPT = 1902;
  private static final int TYPE_DENY = 1903;
  private static final int TYPE_BUSY = 1904;
  private static final int TYPE_BYE = 1905;

  private static IdleState mIdleState;// = new IdleState();
  private static CallingState mCallingState;// = new CallingState();
  private static BeCalledState mBeCalledState;// = new BeCalledState();
  private static BeCalledWaitState mBeCalledWaitState;// = new BeCalledWaitState();
  private static VideoingState mVideoingState;// = new VideoingState();
  private static State mState;// = mIdleState;

  private MyBinder mBinder = new MyBinder();

  private MultiQavsdkControl mQavsdkControl;
  private int mLoginErrorCode = AVError.AV_OK;

  public static String mReceiveIdentifier = "";
  public static String mSelfIdentifier = "";
  public static String mSelfSig = "";

  private Context ctx = null;
  private TIMConversation conversation;

  private String mRoomId;
  private boolean isMonitor;
  private boolean isStarting = false;

  private int mCreateRoomErrorCode = AVError.AV_OK;
  private int mCloseRoomErrorCode = AVError.AV_OK;
  private static final int MAX_TIMEOUT = 30 * 1000;
  private static final int MSG_CREATEROOM_TIMEOUT = 1;
  private static final int MSG_START_CONTEXT = 4;
  private ArrayList<String> robotIds = new ArrayList<String>();

  private Handler handler = new Handler() {
    @Override public void handleMessage(Message msg) {
      switch (msg.what) {
      case MSG_CREATEROOM_TIMEOUT:
        if (mQavsdkControl != null) {
          mQavsdkControl.setCreateRoomStatus(false);
          mQavsdkControl.setCloseRoomStatus(false);
          Toast.makeText(ctx, R.string.notify_network_error, Toast.LENGTH_SHORT)
              .show();
        }
        break;
      case MSG_START_CONTEXT:
        removeMessages(MSG_START_CONTEXT);
        startVideoContext();
        break;
      default:
        break;
      }
    }
  };

  @Override public IBinder onBind(Intent intent) {
    return mBinder;
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    flags = START_STICKY;
    return super.onStartCommand(intent, flags, startId);
  }

  @Override public void onDestroy() {
    super.onDestroy();
    MultiLogcatHelper.getInstance(this).stop();
    handler.removeCallbacksAndMessages(null);
    try {
      unregisterReceiver(mBroadcastReceiver);
      mQavsdkControl.onDestroy();
      mQavsdkControl.stopContext();
    } catch (Exception e) {
      com.kobot.lib.utils.Log.d(TAG, "service stop error maybe not login");
    }
  }

  public class MyBinder extends Binder implements IVideoBinder {

    public void onAvFinish() {
      if(mState == mIdleState) {
        return;
      }
      Log.e(TAG, "WL_DEBUG onActivityResult");
      sendByeMsg(mReceiveIdentifier);
      closeRoom();
      mState = mIdleState;
    }

    @Override public void startCalling(String receiveIdentifier,
        boolean monitor) {
      if(!mReceiveIdentifier.equals(receiveIdentifier)) {
        if(mState == mCallingState) {
          cancelCall();
        } else if (mState == mBeCalledState) {
          denyInvite();
        } else if (mState == mVideoingState) {
          onAvFinish();
        }
      }
      if(mState == mBeCalledState) {
        sendAcceptMsg(mReceiveIdentifier);
        return;
      }
      if(mState == mVideoingState) {
        return;
      }
      mReceiveIdentifier = String.valueOf(receiveIdentifier);
      mQavsdkControl.setPeerId(mReceiveIdentifier);
      mState = mCallingState;
      sendCallMsg(mReceiveIdentifier, monitor);
      isMonitor = monitor;
    }

    //取消呼叫
    @Override public void cancelCall() {
      Log.e(TAG, "WL_DEBUG onCancel");
      mQavsdkControl.setPeerId("");
      mState = mIdleState;
      sendCancelMsg(mReceiveIdentifier);
    }

    @Override public void acceptInvite() {
      isMonitor = false;
      mState = mBeCalledWaitState;
      sendAcceptMsg(mReceiveIdentifier);
    }

    @Override public void denyInvite() {
      mQavsdkControl.setPeerId("");
      mState = mIdleState;
      sendDenyMsg(mReceiveIdentifier);
    }

    @Override public void sendControlMsg(JSONObject object) {
      sendMsg(object, mReceiveIdentifier);
    }

    @Override public void sendMessage(JSONObject object, String id) {
      sendMsg(object, id);
    }

    public State getState() {
      return mState;
    }
  }

  private void createRoom() {
    String roomRoleValue = "";
    MultiUtil.auth_bits = VideoConstants.HOST_AUTH;

    mQavsdkControl.enterRoom(Integer.parseInt(mRoomId), roomRoleValue);
    handler.sendEmptyMessageDelayed(MSG_CREATEROOM_TIMEOUT, MAX_TIMEOUT);
  }

  private void sendCallMsg(String receiver, boolean isMonitor) {
    JSONObject jsonObject = new JSONObject();
    try {
      int randomInt = new Random(System.currentTimeMillis()).nextInt();
      if(randomInt < 0) {
        randomInt = -randomInt;
      } else if (randomInt == 0) {
        randomInt = 91283471;
      }
      mRoomId = String.valueOf(randomInt);
      jsonObject.put("type", TYPE_CALL);
      jsonObject.put("roomId", mRoomId);
      jsonObject.put("isMonitor", isMonitor);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    sendMsg(jsonObject, receiver);
  }

  private void sendCancelMsg(String receiver) {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("type", TYPE_CANCEL);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    sendMsg(jsonObject, receiver);
  }

  private void sendDenyMsg(String receiver) {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("type", TYPE_DENY);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    sendMsg(jsonObject, receiver);
  }

  private void sendAcceptMsg(String receiver) {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("type", TYPE_ACCEPT);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    sendMsg(jsonObject, receiver);
  }

  private void sendByeMsg(String receiver) {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("type", TYPE_BYE);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    sendMsg(jsonObject, receiver);
  }

  private void sendBusyMsg(String receiver) {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("type", TYPE_BUSY);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    sendMsg(jsonObject, receiver);
  }

  private void sendMsg(final JSONObject msgText, final String receiver) {
    if(!mQavsdkControl.hasAVContext()) {
      handler.sendMessage(handler.obtainMessage(MSG_START_CONTEXT));
      mState = mIdleState;
      return;
    }
    conversation = TIMManager.getInstance()
        .getConversation(TIMConversationType.C2C, receiver);

    com.kobot.lib.utils.Log.d("WL_DEBUG", "msgText:" + msgText);
    if (conversation == null) {
      startVideoContext();
      //            Methods.showToast(getString(R.string.init_fail));
      com.kobot.lib.utils.Log.d("WL_DEBUG", "conversation == null");
      return;
    }
    final TIMMessage msg = new TIMMessage();
    //	msg.addTextElement(str);
    TIMCustomElem elem = new TIMCustomElem();
    try {
      if(msgText.getInt("type") == TYPE_CALL)
      elem.setDesc("机器人正在向你发起呼叫请求");
    } catch (JSONException e) {
      e.printStackTrace();
    }
    try {
      elem.setData(msgText.toString().getBytes("UTF-8"));
      int iRet = msg.addElement(elem);
      if (iRet != 0) {
        com.kobot.lib.utils.Log.d(TAG, "add element error:" + iRet);
        return;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    //    TIMTextElem elem = new TIMTextElem();
//    elem.setText(msgText.toString());
    com.kobot.lib.utils.Log.d(TAG, "ready send text msg = " + msgText);
    conversation.sendOnlineMessage(msg, new TIMValueCallBack<TIMMessage>() {
      @Override public void onError(int code, String desc) {//发送消息失败
        //服务器返回了错误码code和错误描述desc，可用于定位请求失败原因
        //错误码code列表请参见附录
        if (code == Constant.TEXT_MSG_FAILED_FOR_TOO_LOGNG) {
          desc = getString(R.string.send_msg_too_long);
        } else if (code == Constant.SEND_MSG_FAILED_FOR_PEER_NOT_LOGIN) {
          desc = getString(R.string.send_msg_fail_for_peer_not_login);
        }
        Log.e(TAG, "send message failed. code: " + code + " errmsg: " + desc);
//        Methods.showToast(getString(R.string.send_msg_fail) +
//            ". code: " + code + " errmsg: " + desc);
        mState.onSendFail(msgText, receiver);
      }

      @Override public void onSuccess(TIMMessage arg0) {
        Log.e(TAG, "Send text Msg ok");
        handler.post(new Runnable() {
          @Override public void run() {
            mState.onSendSuccess(msgText, receiver);
          }
        });
      }
    });
  }

  private void closeRoom() {
    if ((mQavsdkControl != null) && (mQavsdkControl.getAVContext() != null) && (mQavsdkControl.getAVContext().getAudioCtrl() != null)) {
      mQavsdkControl.getAVContext().getAudioCtrl().stopTRAEService();
    }

    mCloseRoomErrorCode = mQavsdkControl.exitRoom();
    
    if (mCloseRoomErrorCode != AVError.AV_OK) {
//      showDialog(DIALOG_CLOSE_ROOM_ERROR);
      if (mQavsdkControl != null) {
        mQavsdkControl.setCloseRoomStatus(false);
      }
      return;
    }
  }

  private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
    @Override public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (action.equals(MultiUtil.ACTION_START_CONTEXT_COMPLETE)) {
        mLoginErrorCode = intent
            .getIntExtra(MultiUtil.EXTRA_AV_ERROR_RESULT, AVError.AV_OK);

        if (mLoginErrorCode == AVError.AV_OK) {
          registerMsgListener();
          handler.sendMessageDelayed(
              handler.obtainMessage(MSG_START_CONTEXT), 60*60*1000);
        } else {
          Methods.showToast(R.string.init_fail_retry);
          handler.sendMessageDelayed(
              handler.obtainMessage(MSG_START_CONTEXT), 5*1000);
        }
      } else if (action.equals(MultiUtil.ACTION_CLOSE_CONTEXT_COMPLETE)) {
        mQavsdkControl.setIsInStopContext(false);
        //                refreshWaitingDialog();
      } else if (action.equals(MultiUtil.ACTION_ROOM_CREATE_COMPLETE)) {
        handler.removeMessages(MSG_CREATEROOM_TIMEOUT);
        //                refreshWaitingDialog();

        mCreateRoomErrorCode = intent
            .getIntExtra(MultiUtil.EXTRA_AV_ERROR_RESULT, AVError.AV_OK);

        if (mCreateRoomErrorCode == AVError.AV_OK) {
          sendBroadcast(new Intent(MultiUtil.ACTION_CREATE_ROOM_SUCCESS)
              .putExtra(MultiUtil.EXTRA_RELATION_ID, Long.parseLong(mRoomId))
              .putExtra(MultiUtil.EXTRA_SELF_IDENTIFIER, mSelfIdentifier)
              .putExtra(MultiUtil.EXTRA_IS_MONITOR, isMonitor));
          mState = mVideoingState;
        } else {
          if ((mQavsdkControl != null) && (mQavsdkControl.getAVContext() != null) && (mQavsdkControl.getAVContext().getAudioCtrl() != null)) {
            mQavsdkControl.getAVContext().getAudioCtrl().stopTRAEService();
          }
          //                    showDialog(DIALOG_CREATE_ROOM_ERROR);
        }
      } else if (action.equals(MultiUtil.ACTION_CLOSE_ROOM_COMPLETE)) {
        mState = mIdleState;
      } else if (action.equals(MultiUtil.ACTION_AV_ACTIVITY_FINISH)) {
        //                refreshWaitingDialog();
      }
      Log.e(TAG,
          "WL_DEBUG ANR StartContextActivity onReceive action = " + action
              + " Out");
    }
  };

  private void registerMsgListener() {
    //设置消息监听器，收到新消息时，通过此监听器回调
    TIMManager.getInstance().addMessageListener(new TIMMessageListener() {
      @Override public boolean onNewMessages(List<TIMMessage> list) {
        for (TIMMessage msg : list) {
          long timeStamp = QALSDKManager.getInstance().getServetTimeSecondInterv() + System.currentTimeMillis()/1000;
          if (timeStamp - msg.timestamp() > 10) {
            com.kobot.lib.utils.Log.d("machao", "skip msg" + (timeStamp - msg.timestamp()));
            continue;
          }
          for (int i = 0; i < msg.getElementCount(); i++) {
            TIMElem elem = msg.getElement(i);
            //获取当前元素的类型
            TIMElemType elemType = elem.getType();
            com.kobot.lib.utils.Log.d("machao", "elem type: " + elemType.name());
            if (elemType == TIMElemType.Custom) {
              //文本元素, 获取文本内容
              TIMCustomElem e = (TIMCustomElem) elem;
              try {
                Log.d(TAG,
                    "msg: " + new String(e.getData(), "UTF-8") + "|sender:" + msg
                        .getSender());
                JSONObject object = new JSONObject(new String(e.getData(), "UTF-8"));
                int type = object.getInt("type");
                if(type == Constant.MSG_RECORD_FAIL) {
                  RecordResponse response = new RecordResponse();
                  try {
                    response.errorCode = object.getInt("errorCode");
                  } catch (JSONException e1) {
                    e1.printStackTrace();
                  }

                  try {
                    response.errorMsg = object.getString("errorMsg");
                  } catch (JSONException e1) {
                    e1.printStackTrace();
                  }

                  if(response.errorCode != 0) {
                    sendBroadcast(new Intent(Constant
                        .ACTION_RECORD_FAIL).putExtra(Constant
                        .EXTRA_RECORD_FAIL_RESPONSE, response));
                  }
                } else {
                  mState.onReceive(object, msg.getSender());
                }
              } catch (JSONException e1) {
                e1.printStackTrace();
              } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
              }
            }
          }
        }
        return true;
      }
    });
    TIMManager.getInstance().setUserStatusListener(new TIMUserStatusListener() {
      @Override public void onForceOffline() {
        Log.e(TAG, "onForceOffline");
        sendBroadcast(new Intent(Constant.ACTION_BE_KICKED_OUT));
        stopSelf();
        sendByeMsg(mReceiveIdentifier);
        closeRoom();
      }

      @Override public void onUserSigExpired() {
        if(mQavsdkControl != null) {
          mQavsdkControl.stopContext();
        }
        startVideoContext();
      }
    });
  }

  @Override public void onCreate() {
    super.onCreate();
    ctx = this;
    
    mIdleState = new IdleState();
    mCallingState = new CallingState();
    mBeCalledState = new BeCalledState();
    mBeCalledWaitState = new BeCalledWaitState();
    mVideoingState = new VideoingState();
    mState = mIdleState;
    
    MultiLogcatHelper.getInstance(this).start();
    startVideoContext();

    if(mQavsdkControl == null) {
      return;
    }
    mQavsdkControl.setIsSupportMultiView(false);
    mQavsdkControl.setIsOpenBackCameraFirst(false);

    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(MultiUtil.ACTION_START_CONTEXT_COMPLETE);
    intentFilter.addAction(MultiUtil.ACTION_CLOSE_CONTEXT_COMPLETE);
    intentFilter.addAction(MultiUtil.ACTION_ROOM_CREATE_COMPLETE);
    intentFilter.addAction(MultiUtil.ACTION_CLOSE_ROOM_COMPLETE);
    registerReceiver(mBroadcastReceiver, intentFilter);
    Log.e(TAG, "WL_DEBUG onCreate");
  }

  private synchronized void startVideoContext() {
    if(isStarting) {
      return;
    }
    isStarting = true;

    mQavsdkControl = ((RobotApplication) getApplication()).getMultiQavsdkControl();
    mLoginErrorCode = mQavsdkControl
            .startContext(mSelfIdentifier, mSelfSig);

    isStarting = false;
  }

  abstract class State {

    protected abstract void handleMessage(int type, String sender,
        JSONObject object);

    protected abstract void onSendFail(JSONObject object, String peerId);

    protected abstract void onSendSuccess(JSONObject object, String peerId);

    public void onReceive(JSONObject object, String sender) {
      int type = getType(object);
      handleMessage(type, sender, object);
    }

    protected int getType(JSONObject object) {
      try {
        return object.getInt("type");
      } catch (JSONException e) {
        e.printStackTrace();
      }
      return 0;
    }

  }

  public class IdleState extends State {

    @Override protected void handleMessage(int type, final String sender,
        JSONObject object) {
      switch (type) {
      case TYPE_CALL:
        try {
          mRoomId = object.getString("roomId");
        } catch (JSONException e) {
          e.printStackTrace();
        }
        mReceiveIdentifier = sender;
        mQavsdkControl.setPeerId(sender);

        startActivity(
            new Intent(MultiQavService.this, CallingActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(CallingActivity.EXTRA_TYPE_TALK_WITH_PERSON,
                    CallingActivity.VIDEO_BE_CALLED)
                .putExtra("caller", sender));
        mState = mBeCalledState;
        break;
      }
    }

    @Override protected void onSendFail(JSONObject object, String peerId) {

    }

    @Override protected void onSendSuccess(JSONObject object, String peerId) {
      sendBroadcast(new Intent(MultiUtil.ACTION_PERSON_TALK_FINISH));
    }
  }

  public class CallingState extends State {

    @Override protected void handleMessage(int type, final String sender,
        JSONObject object) {
      switch (type) {
      case TYPE_BUSY:
        if(sender.equals(mQavsdkControl.getPeerId())) {
          if (!isMonitor)
            Methods.showToast(R.string.call_peer_is_calling);
          sendBroadcast(new Intent(MultiUtil.ACTION_PERSON_TALK_FINISH));
          mState = mIdleState;
        }
        break;
      case TYPE_DENY:
        if (sender.equals(mQavsdkControl.getPeerId())) {
          Methods.showToast(R.string.call_peer_deny);
          sendBroadcast(new Intent(MultiUtil.ACTION_PERSON_TALK_FINISH));
          mState = mIdleState;
        }
        break;
      case TYPE_CALL:
        if (sender.equals(mReceiveIdentifier)) {
          mState = mBeCalledWaitState;
          sendAcceptMsg(sender);
        } else {
          sendBusyMsg(sender);
        }
        break;
      case TYPE_ACCEPT:
        if (sender.equals(mQavsdkControl.getPeerId())) {
          createRoom();
        }
        break;
      }
    }

    @Override protected void onSendFail(JSONObject object, String peerId) {
      int type = getType(object);
      switch (type) {
      case TYPE_CALL:
        if (peerId.equals(mQavsdkControl.getPeerId())) {
//          Methods.showToast(R.string.call_peer_offline);
          sendBroadcast(new Intent(MultiUtil.ACTION_PERSON_TALK_FINISH));
          mState = mIdleState;
        }
        break;
      }
    }

    @Override protected void onSendSuccess(JSONObject object, String peerId) {
    }
  }

  public class BeCalledState extends State {

    @Override protected void handleMessage(int type, final String sender,
        JSONObject object) {
      switch (type) {
      case TYPE_CALL:
        if (!sender.equals(mReceiveIdentifier)) {
          sendBusyMsg(sender);
        }
        break;
      case TYPE_CANCEL:
        if (sender.equals(mReceiveIdentifier)) {
          Methods.showToast(R.string.call_peer_cancel);
          sendBroadcast(new Intent(MultiUtil.ACTION_PERSON_TALK_FINISH));
          mState = mIdleState;
        }
        break;
      }
    }

    @Override protected void onSendFail(JSONObject object, String peerId) {

    }

    @Override protected void onSendSuccess(JSONObject object, String peerId) {

    }
  }

  class BeCalledWaitState extends State {

    @Override protected void handleMessage(int type, final String sender,
        JSONObject object) {
      switch (type) {
      case TYPE_CALL:
        if (!sender.equals(mReceiveIdentifier)) {
          sendBusyMsg(sender);
        }
        break;
      case TYPE_CANCEL:
        if (sender.equals(mReceiveIdentifier)) {
          Methods.showToast(R.string.call_peer_cancel);
          sendBroadcast(new Intent(MultiUtil.ACTION_PERSON_TALK_FINISH));
          mState = mIdleState;
        }
        break;
      }
    }

    @Override protected void onSendFail(JSONObject object, String peerId) {

    }

    @Override protected void onSendSuccess(JSONObject object, String peerId) {
      int type = getType(object);
      switch (type) {
      case TYPE_ACCEPT:
        createRoom();
        break;
      }
    }
  }

  class VideoingState extends State {

    @Override protected void handleMessage(int type, final String sender,
        JSONObject object) {
      switch (type) {
      case TYPE_CALL:
        try {
          String roomId = object.getString("roomId");
          if(mRoomId.equals(roomId)) {
            sendAcceptMsg(sender);
            sendBroadcast(new Intent(MultiUtil.ACTION_JUMP_TO_VIDEOING)
                .putExtra("peerId", sender));
            break;
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }
        if (!sender.equals(mReceiveIdentifier)) {
          sendBusyMsg(sender);
        }
        break;
      case TYPE_BYE:
        if (sender.equals(mReceiveIdentifier)) {
          sendBroadcast(new Intent(MultiUtil.ACTION_AV_ACTIVITY_FINISH));
        }
        break;
      }
    }

    @Override protected void onSendFail(JSONObject object, String peerId) {

    }

    @Override protected void onSendSuccess(JSONObject object, String peerId) {

    }
  }

}
