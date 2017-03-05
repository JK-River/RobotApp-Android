package com.kobot.lib.video.multi.control;

import android.content.Context;
import android.view.View;
import com.kobot.lib.utils.Log;
import com.kobot.lib.video.BaseQavControl;
import com.kobot.lib.video.multi.MultiMemberInfo;
import com.kobot.lib.video.multi.VideoConstants;
import com.tencent.av.opengl.ui.GLRootView;
import com.tencent.av.sdk.AVContext;
import com.tencent.av.sdk.AVRoomMulti;
import com.tencent.av.sdk.AVView;

import java.util.ArrayList;
import java.util.List;

public class MultiQavsdkControl extends BaseQavControl {
	private static final String TAG = "MultiQavsdkControl";
	private MultiAVContextControl mAVContextControl = null;
	private MultiAVRoomControl mAVRoomControl = null;
	private MultiAVEndpointControl mAVEndpointControl = null;
	private MultiAVUIControl mAVUIControl = null;
	private MultiAVVideoControl mAVVideoControl = null;
	private MultiAVAudioControl mAVAudioControl = null;

	private int roomId = 0;
	private boolean isCapturing = false;

	public MultiQavsdkControl(Context context) {
		mAVContextControl = new MultiAVContextControl(context);
		mAVRoomControl = new MultiAVRoomControl(context);
		mAVEndpointControl = new MultiAVEndpointControl(context);
		mAVVideoControl = new MultiAVVideoControl(context);
		mAVAudioControl = new MultiAVAudioControl(context);
		Log.d(TAG, "WL_DEBUG MultiQavsdkControl");
	}

	public void setIsSupportMultiView(boolean isSupport) {
		if (null != mAVEndpointControl) {
			mAVEndpointControl.setIsSupportMultiView(isSupport);
		}
	}

	public boolean getIsSupportMultiView() {
		if (null != mAVEndpointControl) {
			return mAVEndpointControl.getIsSupportMultiView();
		}

		return false;
	}

	public void deleteRequestView(String identifier, int videoSrcType) {
		if (null != mAVEndpointControl) {
			mAVEndpointControl.deleteRequestView(identifier, videoSrcType);
		}
	}

	/**
	 * 启动SDK系统
	 * 
	 * @param identifier
	 *            用户身份的唯一标识
	 * @param usersig
	 *            用户身份的校验信息
	 */
	public int startContext(String identifier, String usersig) {
		if (mAVContextControl == null)
			return VideoConstants.DEMO_ERROR_NULL_POINTER;
		return mAVContextControl.startContext(identifier, usersig);
	}

	/**
	 * 关闭SDK系统
	 */
	public void stopContext() {
		if (mAVContextControl != null) {
			mAVContextControl.stopContext();	
		}
	}

	public boolean hasAVContext() {
		if (mAVContextControl == null)
			return false;
		return mAVContextControl.hasAVContext();
	}

	public String getSelfIdentifier() {
		if (mAVContextControl == null)
			return null;
		return mAVContextControl.getSelfIdentifier();
	}

	/**
	 * 创建房间
	 * 
	 * @param relationId
	 *            讨论组号
	 */
	public void enterRoom(int relationId, String roomRole) {
		if (mAVRoomControl != null) {
			mAVRoomControl.enterRoom(relationId, roomRole);
			roomId = relationId;
		}
	}

	public int getRoomId() {
		return roomId;
	}
	
	public void setAudioCat(int audioCat) {
		if (mAVRoomControl != null) {
			mAVRoomControl.setAudioCat(audioCat);		
		}
	}

	/** 关闭房间 */
	public int exitRoom() {
		if (mAVRoomControl == null)
			return VideoConstants.DEMO_ERROR_NULL_POINTER;
		return mAVRoomControl.exitRoom();
	}

	/**
	 * 获取成员列表
	 * 
	 * @return 成员列表
	 */
	public ArrayList<MultiMemberInfo> getMemberList() {
		if (mAVRoomControl == null) {
			return null;
		} 
		return mAVRoomControl.getMemberList();	
	}

	public ArrayList<MultiMemberInfo> getAudioAndCameraMemberList() {
		if (mAVRoomControl == null) {
			return null;
		}
		return mAVRoomControl.getAudioAndCameraMemberList();
	}

	public ArrayList<MultiMemberInfo> getScreenMemberList() {
		if (mAVRoomControl == null) {
			return null;
		}
		return mAVRoomControl.getScreenMemberList();
	}

	public AVRoomMulti getRoom() {
		AVContext avContext = getAVContext();

		return avContext != null ? avContext.getRoom() : null;
	}

	public boolean getIsInStartContext() {
		if (mAVContextControl == null)
			return false;
		
		return mAVContextControl.getIsInStartContext();
	}

	public boolean getIsInStopContext() {
		if (mAVContextControl == null)
			return false;
		
		return mAVContextControl.getIsInStopContext();
	}
	
	public void setTestEnvStatus(boolean status) {
		if (mAVContextControl != null)
			mAVContextControl.setTestEnvStatus(status);
	}
	
	
	
	
	public boolean setIsInStopContext(boolean isInStopContext) {
		if (mAVContextControl == null)
			return false;
		
		return mAVContextControl.setIsInStopContext(isInStopContext);
	}

	public boolean getIsInEnterRoom() {
		if (mAVRoomControl == null)
			return false;
		return mAVRoomControl.getIsInEnterRoom();
	}

	public boolean getIsInCloseRoom() {
		if (mAVRoomControl == null)
			return false;
		return mAVRoomControl.getIsInCloseRoom();
	}

	public AVContext getAVContext() {
		if (mAVContextControl == null)
			return null;		
		return mAVContextControl.getAVContext();
	}

	public boolean isInRequestList(String identifier, int videoSrcType) {
		if (null != mAVEndpointControl) {
			return mAVEndpointControl.isInRequestList(identifier, videoSrcType);
		}

		return false;
	}


	public void onCreate(MultiAVUIControl avuiControl) {
		mAVUIControl = avuiControl;
		mAVVideoControl.initAVVideo();
		mAVAudioControl.initAVAudio();
		mAVEndpointControl.initMembersUI();
	}

	public void onContinue(MultiAVUIControl avuiControl) {
		mAVUIControl = avuiControl;
		List<MultiMemberInfo> memberInfoList = mAVRoomControl
				.getAudioAndCameraMemberList();
		for (int j = 0; j < memberInfoList.size(); j++)
		{
			if (memberInfoList.get(j).hasCameraVideo && memberInfoList.get(j)
					.identifier.equals(getPeerId()))
			{
//				openRemoteVideo();
				setRemoteHasVideo(true, memberInfoList.get(j).identifier,
						AVView.VIDEO_SRC_TYPE_CAMERA);
			}
		}
	}

	public void onResume() {
//		mAVContextControl.getAVContext().onResume();
		if (mAVUIControl != null) {
			mAVUIControl.onResume();		
		}
	}

	public void onPause() {
//		mAVContextControl.getAVContext().onPause();
		if (null != mAVUIControl) {	
			mAVUIControl.onPause();
		}
	}

	public void onDestroy() {
		if (null != mAVAudioControl) {
			mAVAudioControl.resetAudio();
		}
		closeRemoteVideo();
		if (null != mAVUIControl && mAVUIControl.getGLRootView() != null) {
			mAVUIControl.getGLRootView().setVisibility(View.INVISIBLE);
//			mAVUIControl.onDestroy();
//			mAVUIControl = null;
		}

		if (null != mAVEndpointControl) {
			mAVEndpointControl.clearRequestList();
		}
	}

	public void closeRemoteVideo() {
		mAVEndpointControl.closeRemoteVideo();
	}

	public void setLocalHasVideo(boolean isLocalHasVideo, String selfIdentifier) {
		if (null != mAVUIControl) {
			mAVUIControl.setLocalHasVideo(isLocalHasVideo, false, selfIdentifier);
		}
	}
	public void setRemoteHasVideo(boolean isRemoteHasVideo, String identifier, int videoSrcType) {
		if (null != mAVUIControl) {
			mAVUIControl.setSmallVideoViewLayout(isRemoteHasVideo, identifier, videoSrcType);
		}
	}

	public GLRootView getRemoteView() {
		if (null != mAVUIControl) {
			return mAVUIControl.getRemoteView();
		}
		return null;
	}

	public void setSelfId(String key) {
		if (null != mAVUIControl) {		
			mAVUIControl.setSelfId(key);
		}
	}

	public int toggleEnableCamera() {
		return mAVVideoControl.toggleEnableCamera();
	}

	public int toggleSwitchCamera() {
		return mAVVideoControl.toggleSwitchCamera();
	}

	public void setIsOpenBackCameraFirst(boolean _isOpenBackCameraFirst) {
		mAVVideoControl.setIsOpenBackCameraFirst(_isOpenBackCameraFirst);
	}

	public boolean needCapture() {
		return isCapturing;
	}

	public void setNeedCapture(boolean flag) {
		isCapturing = flag;
	}

	public boolean getIsInOnOffCamera() {
		return mAVVideoControl.getIsInOnOffCamera();
	}
	
	public boolean getIsInOnOffExternalCapture() {
		return mAVVideoControl.getIsInOnOffExternalCapture();
	}
	

	public boolean getIsInSwitchCamera() {
		return mAVVideoControl.getIsInSwitchCamera();
	}
	
	public void setIsInSwitchCamera(boolean isInSwitchCamera) {
		mAVVideoControl.setIsInSwitchCamera(isInSwitchCamera);
	}

	public boolean getIsEnableCamera() {
		return mAVVideoControl.getIsEnableCamera();
	}
	
	public void setIsInOnOffCamera(boolean isInOnOffCamera) {
		mAVVideoControl.setIsInOnOffCamera(isInOnOffCamera);
	}

	public void setIsOnOffExternalCapture(boolean isOnOffExternalCapture) {
		mAVVideoControl.setIsOnOffExternalCapture(isOnOffExternalCapture);
	}

	public boolean getIsFrontCamera() {
		return mAVVideoControl.getIsFrontCamera();
	}
	
	public boolean getIsEnableExternalCapture() {
		return mAVVideoControl.getIsEnableExternalCapture();
	}

	public void onMemberChange() {
		if (mAVUIControl != null) {
			mAVUIControl.onMemberChange();		
		}
	}

	public boolean getHandfreeChecked() {
		return mAVAudioControl.getHandfreeChecked();
	}
	
	
	public MultiAVVideoControl getAVVideoControl() {
		return mAVVideoControl;
	}
	public MultiAVAudioControl getAVAudioControl() {
		return mAVAudioControl;
	}
	public void setRotation(int rotation) {
		if (mAVUIControl != null) {
			mAVUIControl.setRotation(rotation);	
		}
	}
	
	public String getQualityTips( ) {
		if (null != mAVUIControl) {
			return mAVUIControl.getQualityTips();
		} else {
			return null;
		}
	}
	public void setCreateRoomStatus(boolean status) {
		if (mAVRoomControl != null) {
			mAVRoomControl.setCreateRoomStatus(status);
		}
	}
	public void setCloseRoomStatus(boolean status) {
		if (mAVRoomControl != null) {
			mAVRoomControl.setCloseRoomStatus(status);
		}
	}	
		
	public int enableExternalCapture(boolean isEnable) {
		return mAVVideoControl.enableExternalCapture(isEnable);
	}
	
	public void setNetType(int netType) {
		if (mAVRoomControl == null)return ;
		mAVRoomControl.setNetType(netType);
	}

	public boolean changeAuthority(byte[] auth_buffer) {
		return mAVRoomControl.changeAuthority(auth_buffer);
	}

	public void openRemoteVideo(String identifier){
		mAVEndpointControl.openRemoteVideo(identifier, AVView.VIDEO_SRC_TYPE_CAMERA);
	}

	public void reOpenRemoteVideo(){
		mAVEndpointControl.reOpenRemoteVideo(getPeerId(), AVView
				.VIDEO_SRC_TYPE_CAMERA);
	}
}