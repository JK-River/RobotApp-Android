package com.kobot.lib.video.multi.control;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.kobot.lib.common.RobotApplication;
import com.kobot.lib.video.multi.MultiMemberInfo;
import com.kobot.lib.video.multi.MultiUtil;
import com.kobot.lib.video.multi.VideoConstants;
import com.tencent.av.sdk.*;

import java.util.ArrayList;

class MultiAVRoomControl {
  private static final int TYPE_MEMBER_CHANGE_IN = 1;//进入房间事件。
  private static final int TYPE_MEMBER_CHANGE_OUT = 2;//退出房间事件。
  private static final int TYPE_MEMBER_CHANGE_HAS_CAMERA_VIDEO = 3;//有发摄像头视频事件。
  private static final int TYPE_MEMBER_CHANGE_NO_CAMERA_VIDEO = 4;//无发摄像头视频事件。
  private static final int TYPE_MEMBER_CHANGE_HAS_AUDIO = 5;//有发语音事件。
  private static final int TYPE_MEMBER_CHANGE_NO_AUDIO = 6;//无发语音事件。
  private static final int TYPE_MEMBER_CHANGE_HAS_SCREEN_VIDEO = 7;//有发屏幕视频事件。
  private static final int TYPE_MEMBER_CHANGE_NO_SCREEN_VIDEO = 8;//无发屏幕视频事件。

  private static final String TAG = "MultiAVRoomControl";
  private boolean mIsInCreateRoom = false;
  private boolean mIsInCloseRoom = false;
  private Context mContext;
  private ArrayList<MultiMemberInfo> mAudioAndCameraMemberList = new ArrayList<MultiMemberInfo>();
  private ArrayList<MultiMemberInfo> mScreenMemberList = new ArrayList<MultiMemberInfo>();

  private int audioCat = 0;

  public void setAudioCat(int audioCat) {
    this.audioCat = audioCat;
  }

  /**
   * 房间回调
   */
  private AVRoomMulti.EventListener mEventListener = new AVRoomMulti.EventListener() {
    // 创建房间成功回调
    public void onEnterRoomComplete(int result) {
      if (result == 0) {
        initAudioService();
      }

      Log.d(TAG,
          "WL_DEBUG mRoomDelegate.onEnterRoomComplete result = " + result);
      mIsInCreateRoom = false;
      mContext.sendBroadcast(new Intent(MultiUtil.ACTION_ROOM_CREATE_COMPLETE)
          .putExtra(MultiUtil.EXTRA_AV_ERROR_RESULT, result));

    }

    @Override
    public void onExitRoomComplete() {
      uninitAudioService();
      mIsInCloseRoom = false;
      mAudioAndCameraMemberList.clear();
      mScreenMemberList.clear();
      mContext.sendBroadcast(new Intent(MultiUtil.ACTION_CLOSE_ROOM_COMPLETE));
    }

    @Override
    public void onRoomDisconnect(int i) {
      uninitAudioService();

      mIsInCloseRoom = false;
      mAudioAndCameraMemberList.clear();
      mScreenMemberList.clear();
      mContext.sendBroadcast(new Intent(MultiUtil.ACTION_CLOSE_ROOM_COMPLETE));
    }

    //房间成员变化回调
    public void onEndpointsUpdateInfo(int eventid, String[] updateList) {
      Log.d(TAG, "WL_DEBUG onEndpointsUpdateInfo. eventid = " + eventid);
      onMemberChange(eventid, updateList);

    }

    @Override
    public void onPrivilegeDiffNotify(int i) {

    }

    @Override
    public void onSemiAutoRecvCameraVideo(String[] strings) {

    }

    @Override
    public void onCameraSettingNotify(int i, int i1, int i2) {

    }

    @Override
    public void onRoomEvent(int i, int i1, Object o) {

    }


  };

  MultiAVRoomControl(Context context) {
    mContext = context;
  }

  private void initAudioService() {
    MultiQavsdkControl qavsdk = ((RobotApplication) mContext).getMultiQavsdkControl();
    if ((qavsdk != null) && (qavsdk.getAVContext() != null) && (qavsdk.getAVContext().getAudioCtrl() != null)) {
      qavsdk.getAVContext().getAudioCtrl().startTRAEService();
    }
  }

  private void uninitAudioService() {
    MultiQavsdkControl qavsdk = ((RobotApplication) mContext).getMultiQavsdkControl();
    if ((qavsdk != null) && (qavsdk.getAVContext() != null) && (qavsdk.getAVContext().getAudioCtrl() != null)) {
      qavsdk.getAVContext().getAudioCtrl().stopTRAEService();
    }
  }

  /**
   * 成员列表变化
   */
  private void onMemberChange(int eventid, String[] updateList) {
    Log.d(TAG, "WL_DEBUG onMemberChange type = " + eventid);
    Log.d(TAG, "WL_DEBUG onMemberChange endpointCount = " + updateList.length);
    int endpointCount = updateList.length;
    MultiQavsdkControl qavsdk = ((RobotApplication) mContext)
        .getMultiQavsdkControl();
    AVRoomMulti avRoomMulti = ((AVRoomMulti) qavsdk.getRoom());
    int memberCount = mScreenMemberList.size();

    for (int i = 0; i < endpointCount; i++)
    {
      AVEndpoint endpoint = avRoomMulti.getEndpointById(updateList[i]);
      if(endpoint == null)//endpoint is not exist at all
      {
        for (int j = 0; j < mAudioAndCameraMemberList.size(); j++)
        {
          if (mAudioAndCameraMemberList.get(j).identifier.equals(updateList[i]))
          {
            qavsdk.deleteRequestView(mAudioAndCameraMemberList.get(j).identifier, AVView.VIDEO_SRC_TYPE_CAMERA);
            mAudioAndCameraMemberList.remove(j);
            break;
          }
        }

        for (int j = 0; j < mScreenMemberList.size(); j++)
        {
          if (mScreenMemberList.get(j).identifier.equals(updateList[i]))
          {
            qavsdk.deleteRequestView(mScreenMemberList.get(j).identifier, AVView.VIDEO_SRC_TYPE_SCREEN);
            mScreenMemberList.remove(j);
            break;
          }
        }

        continue;
      }

      AVEndpoint.Info userInfo = endpoint.getInfo();
      String identifier = userInfo.openId;

      //audio and camera
      boolean bAudioAndCameraMemberDelete = !endpoint.hasAudio() && !endpoint.hasCameraVideo();
      boolean bAudioAndCameraMemberExist = false;

      for (int j = 0; j < mAudioAndCameraMemberList.size(); j++)
      {
        if (mAudioAndCameraMemberList.get(j).identifier.equals(identifier))
        {
          if (!endpoint.hasCameraVideo())
          {
            qavsdk.deleteRequestView(mAudioAndCameraMemberList.get(j).identifier, AVView.VIDEO_SRC_TYPE_CAMERA);
          }

          if(bAudioAndCameraMemberDelete)//delete
          {
            mAudioAndCameraMemberList.remove(j);
          } else//modify info
          {
            MultiMemberInfo info = new MultiMemberInfo();
            info.identifier = userInfo.openId;
            info.name = userInfo.openId;
            info.hasCameraVideo = endpoint.hasCameraVideo();
            info.hasAudio = endpoint.hasAudio();
            info.hasScreenVideo = false;
            MultiMemberInfo oldInfo = mAudioAndCameraMemberList.get(j);
            mAudioAndCameraMemberList.set(j, info);
            bAudioAndCameraMemberExist = true;
            if (info.hasCameraVideo && !oldInfo.hasCameraVideo && info
                .identifier.equals(qavsdk.getPeerId())) {
              Log.d("machao", "video update has video");
              qavsdk.openRemoteVideo(identifier);
            }
          }

          break;
        }
      }

      if (!bAudioAndCameraMemberDelete && !bAudioAndCameraMemberExist)
      {
        MultiMemberInfo info = new MultiMemberInfo();
        info.identifier = userInfo.openId;
        info.name = userInfo.openId;
        info.hasCameraVideo = endpoint.hasCameraVideo();
        info.hasAudio = endpoint.hasAudio();
        info.hasScreenVideo = false;
        mAudioAndCameraMemberList.add(info);

        if(info.hasCameraVideo && info
            .identifier.equals(qavsdk.getPeerId())) {
          Log.d("machao", "video new has video");
          qavsdk.openRemoteVideo(identifier);
        }
      }

      //screen
      boolean bScreenMemberDelete = !endpoint.hasScreenVideo();
      boolean bScreenMemberExist = false;

      for (int j = 0; j < mScreenMemberList.size(); j++)
      {
        if (mScreenMemberList.get(j).identifier.equals(identifier))
        {
          if (!endpoint.hasScreenVideo())
          {
            qavsdk.deleteRequestView(mScreenMemberList.get(j).identifier, AVView.VIDEO_SRC_TYPE_SCREEN);
          }

          if(bScreenMemberDelete)//delete
          {
            mScreenMemberList.remove(j);
          }
          else//modify info
          {
            MultiMemberInfo info = new MultiMemberInfo();
            info.identifier = userInfo.openId;
            info.name = userInfo.openId;
            info.hasCameraVideo = false;
            info.hasAudio = false;
            info.hasScreenVideo = endpoint.hasScreenVideo();
            mScreenMemberList.set(j, info);
            bScreenMemberExist = true;
          }

          break;
        }
      }

      if (!bScreenMemberDelete && !bScreenMemberExist)
      {
        MultiMemberInfo info = new MultiMemberInfo();
        info.identifier = userInfo.openId;
        info.name = userInfo.openId;
        info.hasCameraVideo = false;
        info.hasAudio = false;
        info.hasScreenVideo = endpoint.hasScreenVideo();
        mScreenMemberList.add(info);

//        if(identifier.equals(qavsdk.getPeerId())) {
//          qavsdk.openRemoteVideo();
//        }
      }
    }

    if(memberCount > 1 && mScreenMemberList.size() <= 1) {
      mContext.sendBroadcast(new Intent(MultiUtil.ACTION_AV_ACTIVITY_FINISH));
    }

    mContext.sendBroadcast(new Intent(MultiUtil.ACTION_MEMBER_CHANGE));
  }

  /**
   * 创建房间
   *
   * @param relationId 讨论组号
   */
  void enterRoom(int relationId, String roomRole) {
    Log.d(TAG, "WL_DEBUG enterRoom relationId = " + relationId);
    MultiQavsdkControl qavsdk = ((RobotApplication) mContext)
        .getMultiQavsdkControl();
    AVContext avContext = qavsdk.getAVContext();
    byte[] authBuffer = null;//权限位加密串；TODO：请业务侧填上自己的加密串�

    AVRoomMulti.EnterParam.Builder enterRoomParam = new AVRoomMulti.EnterParam.Builder(relationId);

    enterRoomParam.auth(VideoConstants.HOST_AUTH, authBuffer).avControlRole
        (VideoConstants.HOST_ROLE).autoCreateRoom(true).isEnableMic(true)
        .isEnableSpeaker(true);
    enterRoomParam.audioCategory(VideoConstants.AUDIO_VOICE_CHAT_MODE).videoRecvMode(AVRoomMulti.VIDEO_RECV_MODE_SEMI_AUTO_RECV_CAMERA_VIDEO);
    // create room
    if (avContext != null) {
      int ret = avContext.enterRoom(mEventListener, enterRoomParam.build());
    }
    mIsInCreateRoom = true;

  }

  /**
   * 关闭房间
   */
  int exitRoom() {
    Log.d(TAG, "WL_DEBUG exitRoom");
    MultiQavsdkControl qavsdk = ((RobotApplication) mContext)
        .getMultiQavsdkControl();
    AVContext avContext = qavsdk.getAVContext();
    if(avContext == null) {
      return VideoConstants.DEMO_ERROR_NULL_POINTER;
    }
    int result = avContext.exitRoom();
    mIsInCloseRoom = true;

    return result;
  }

  boolean changeAuthority(byte[] auth_buffer)
  {
    Log.d(TAG, "WL_DEBUG changeAuthority");
    MultiQavsdkControl qavsdk = ((RobotApplication) mContext).getMultiQavsdkControl();
    AVContext avContext = qavsdk.getAVContext();
    AVRoomMulti room = (AVRoomMulti)avContext.getRoom();
    return room.changeAuthority(0, auth_buffer, auth_buffer.length,new AVRoomMulti.ChangeAuthorityCallback(){});
  }

  /**
   * 获取成员列表
   *
   * @return 成员列表
   */
  ArrayList<MultiMemberInfo> getMemberList() {
    ArrayList<MultiMemberInfo> memberList = (ArrayList<MultiMemberInfo>)mAudioAndCameraMemberList.clone();
    for (int j = 0; j < mScreenMemberList.size(); j++)
    {
      memberList.add(mScreenMemberList.get(j));
    }
    return memberList;
  }

  ArrayList<MultiMemberInfo> getAudioAndCameraMemberList() {
    return mAudioAndCameraMemberList;
  }

  ArrayList<MultiMemberInfo> getScreenMemberList() {
    return mScreenMemberList;
  }

  boolean getIsInEnterRoom() {
    return mIsInCreateRoom;
  }

  boolean getIsInCloseRoom() {
    return mIsInCloseRoom;
  }

  public void setCreateRoomStatus(boolean status) {
    mIsInCreateRoom = status;
  }

  public void setCloseRoomStatus(boolean status) {
    mIsInCloseRoom = status;
  }

  public void setNetType(int netType) {
    MultiQavsdkControl qavsdk = ((RobotApplication) mContext)
        .getMultiQavsdkControl();
    AVContext avContext = qavsdk.getAVContext();
    AVRoomMulti room = (AVRoomMulti) avContext.getRoom();
    if (null != room) {
      room.setNetType(netType);
    }
  }
}