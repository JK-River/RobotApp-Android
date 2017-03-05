package com.kobot.lib.video.multi.control;

import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.util.Log;
import com.kobot.lib.common.RobotApplication;
import com.kobot.lib.utils.Methods;
import com.kobot.lib.video.multi.MultiUtil;
import com.tencent.av.sdk.AVError;
import com.tencent.av.sdk.AVVideoCtrl;
import com.tencent.av.sdk.AVVideoCtrl.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Vector;

public class MultiAVVideoControl {
	private static final String TAG = "MultiAVVideoControl";
	private Context mContext = null;
	private boolean mIsEnableCamera = false;
	private boolean mIsFrontCamera = true;
	private boolean mIsInOnOffCamera = false;
	private boolean mIsInSwitchCamera = false;
	private static final int CAMERA_NONE = -1;
	private static final int FRONT_CAMERA = 0;
	private static final int BACK_CAMERA = 1;
	private boolean mIsOnOffExternalCapture = false;
	private boolean mIsEnableExternalCapture = false;

	Vector<String> hasFileOpenIDList;

	private boolean isOpenBackCameraFirst = false;
	public void setIsOpenBackCameraFirst(boolean _isOpenBackCameraFirst) {
		isOpenBackCameraFirst = _isOpenBackCameraFirst;
	}

	private EnableCameraCompleteCallback mEnableCameraCompleteCallback = new EnableCameraCompleteCallback() {
		protected void onComplete(boolean enable, int result) {
			super.onComplete(enable, result);
			Log.d(TAG, "WL_DEBUG mEnableCameraCompleteCallback.onComplete enable = " + enable);
			Log.d(TAG, "WL_DEBUG mEnableCameraCompleteCallback.onComplete result = " + result);
			mIsInOnOffCamera = false;

			if (result == AVError.AV_OK) {
				mIsEnableCamera = enable;
			}

			mContext.sendBroadcast(new Intent(MultiUtil.ACTION_ENABLE_CAMERA_COMPLETE).putExtra(
					MultiUtil.EXTRA_AV_ERROR_RESULT, result).putExtra(MultiUtil.EXTRA_IS_ENABLE, enable));
		}
	};
	
	private EnableExternalCaptureCompleteCallback mEnableExternalCaptureCompleteCallback = new EnableExternalCaptureCompleteCallback() {
			@Override
			protected void onComplete(boolean enable, int result) {
				super.onComplete(enable, result);
				Log.d(TAG, "WL_DEBUG mEnableExternalCaptureCompleteCallback.onComplete enable = " + enable);
				Log.d(TAG, "WL_DEBUG mEnableExternalCaptureCompleteCallback.onComplete result = " + result);
				mIsOnOffExternalCapture = false;
				
				if (result == AVError.AV_OK) {
					mIsEnableExternalCapture = enable;
				}
				
				mContext.sendBroadcast(new Intent(MultiUtil.ACTION_ENABLE_EXTERNAL_CAPTURE_COMPLETE).putExtra(
						MultiUtil.EXTRA_AV_ERROR_RESULT, result).putExtra(MultiUtil.EXTRA_IS_ENABLE, enable));
				
			}
	};

	private SwitchCameraCompleteCallback mSwitchCameraCompleteCallback = new SwitchCameraCompleteCallback() {
		protected void onComplete(int cameraId, int result) {
			super.onComplete(cameraId, result);
			Log.d(TAG, "WL_DEBUG mSwitchCameraCompleteCallback.onComplete cameraId = " + cameraId);
			Log.d(TAG, "WL_DEBUG mSwitchCameraCompleteCallback.onComplete result = " + result);
			mIsInSwitchCamera = false;
			boolean isFront = cameraId == FRONT_CAMERA;

			if (result == AVError.AV_OK) {
				mIsFrontCamera = isFront;
			}

			mContext.sendBroadcast(new Intent(MultiUtil.ACTION_SWITCH_CAMERA_COMPLETE).putExtra(
					MultiUtil.EXTRA_AV_ERROR_RESULT, result).putExtra(MultiUtil.EXTRA_IS_FRONT, isFront));
		}
	};

	public MultiAVVideoControl(Context context) {
		mContext = context;
	}

	int enableCamera(boolean isEnable) {
		int result = AVError.AV_OK;

		if (mIsEnableCamera != isEnable) {
			MultiQavsdkControl qavsdk = ((RobotApplication) mContext).getMultiQavsdkControl();
			AVVideoCtrl avVideoCtrl = qavsdk.getAVContext().getVideoCtrl();
			mIsInOnOffCamera = true;
			if (!isOpenBackCameraFirst) {
				mIsFrontCamera = true;
				if(AVVideoCtrl.isEnableBeauty()) {
					avVideoCtrl.inputBeautyParam(5);
				}
				result = avVideoCtrl.enableCamera(FRONT_CAMERA, isEnable, mEnableCameraCompleteCallback);
			} else {
				mIsFrontCamera = false;
				result = avVideoCtrl.enableCamera(BACK_CAMERA, isEnable, mEnableCameraCompleteCallback);
			}
		}
		Log.d(TAG, "WL_DEBUG enableCamera isEnable = " + isEnable);
		Log.d(TAG, "WL_DEBUG enableCamera result = " + result);
		return result;
	}
	
	int enableExternalCapture(boolean isEnable) {
		int result = AVError.AV_OK;
		
		if (mIsEnableExternalCapture != isEnable) {
			MultiQavsdkControl qavsdk = ((RobotApplication) mContext).getMultiQavsdkControl();
			AVVideoCtrl avVideoCtrl = qavsdk.getAVContext().getVideoCtrl();
			mIsOnOffExternalCapture = true;
			result = avVideoCtrl.enableExternalCapture(isEnable, mEnableExternalCaptureCompleteCallback);
		}
		
		Log.d(TAG, "WL_DEBUG enableExternalCapture isEnable = " + isEnable);
		Log.d(TAG, "WL_DEBUG enableExternalCapture result = " + result);
		return result;
	}

	int switchCamera(boolean isFront) {
		int result = AVError.AV_OK;

		if (mIsFrontCamera != isFront) {
			MultiQavsdkControl qavsdk = ((RobotApplication) mContext).getMultiQavsdkControl();
			AVVideoCtrl avVideoCtrl = qavsdk.getAVContext().getVideoCtrl();
			mIsInSwitchCamera = true;
			result = avVideoCtrl.switchCamera(isFront ? FRONT_CAMERA : BACK_CAMERA, mSwitchCameraCompleteCallback);
		}
		Log.d(TAG, "WL_DEBUG switchCamera isFront = " + isFront);
		Log.d(TAG, "WL_DEBUG switchCamera result = " + result);
		return result;
	}
	
	void setRotation(int rotation) {
		MultiQavsdkControl qavsdk = ((RobotApplication) mContext).getMultiQavsdkControl();
		AVVideoCtrl avVideoCtrl = qavsdk.getAVContext().getVideoCtrl();
		avVideoCtrl.setRotation(rotation);
		Log.e(TAG, "WL_DEBUG setRotation rotation = " + rotation);

	}
	String getQualityTips() {
		MultiQavsdkControl qavsdk = ((RobotApplication) mContext).getMultiQavsdkControl();
		AVVideoCtrl avVideoCtrl = qavsdk.getAVContext().getVideoCtrl();
		return avVideoCtrl.getQualityTips();
	}	

	int toggleEnableCamera() {
		return enableCamera(!mIsEnableCamera);
	}

	int toggleSwitchCamera() {
		return switchCamera(!mIsFrontCamera);
	}

	boolean getIsInOnOffCamera() {
		return mIsInOnOffCamera;
	}

	boolean getIsInSwitchCamera() {
		return mIsInSwitchCamera;
	}
	
	public void setIsInSwitchCamera(boolean isInSwitchCamera) {
		this.mIsInSwitchCamera = isInSwitchCamera;
	}

	boolean getIsEnableCamera() {
		return mIsEnableCamera;
	}
	
	boolean getIsInOnOffExternalCapture() {
		return mIsOnOffExternalCapture;
	}
	
	boolean getIsEnableExternalCapture() {
		return mIsEnableExternalCapture;
	}
	
	public void setIsInOnOffCamera(boolean isInOnOffCamera) {
		this.mIsInOnOffCamera = isInOnOffCamera;
	}
	
	public void setIsOnOffExternalCapture(boolean isOnOffExternalCapture) {
		this.mIsOnOffExternalCapture = isOnOffExternalCapture;
	}

	boolean getIsFrontCamera() {
		return mIsFrontCamera;
	}

	void initAVVideo() {
		mIsEnableCamera = false;
		mIsFrontCamera = true;
		mIsInOnOffCamera = false;
		mIsInSwitchCamera = false;
		mIsEnableExternalCapture = false;
		mIsOnOffExternalCapture = false;
	}
}