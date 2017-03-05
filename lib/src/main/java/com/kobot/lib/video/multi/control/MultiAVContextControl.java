package com.kobot.lib.video.multi.control;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.kobot.lib.video.multi.MultiUtil;
import com.kobot.lib.video.multi.VideoConstants;
import com.tencent.TIMCallBack;
import com.tencent.TIMManager;
import com.tencent.TIMUser;
import com.tencent.av.sdk.AVCallback;
import com.tencent.av.sdk.AVContext;
import com.tencent.av.sdk.AVError;
import com.tencent.openqq.IMSdkInt;

class MultiAVContextControl {
	private static final String TAG = "AvContextControl";
	private static int SDK_APP_ID = 1400001242;
	private static String ACCOUNT_TYPE = "658";
	private boolean mIsInStartContext = false;
	private boolean mIsInStopContext = false;
	private Context mContext;
	private AVContext mAVContext = null;
	private String mSelfIdentifier = "";
	private String mPeerIdentifier = "";
	private AVContext.StartParam mConfig = null;
	private String mUserSig = "";
	/**
	 * 启动SDK系统的回调函数
	 */
	
	private boolean testEnvStatus = false;
	public void setTestEnvStatus(boolean status) {
		testEnvStatus = status;
	}
	
	private AVCallback mStartContextCompleteCallback = new AVCallback() {
		@Override public void onComplete(int result, String s) {
			mIsInStartContext = false;
			Log.d(TAG,
					"WL_DEBUG mStartContextCompleteCallback.OnComplete result = "
							+ result);
			mContext.sendBroadcast(new Intent(
					MultiUtil.ACTION_START_CONTEXT_COMPLETE).putExtra(
					MultiUtil.EXTRA_AV_ERROR_RESULT, result));

			if (result != AVError.AV_OK) {
				mAVContext = null;
				Log.d(TAG, "WL_DEBUG mStartContextCompleteCallback mAVContext is null");
			}
		}
	};

	MultiAVContextControl(Context context) {
		mContext = context;
	}
	
	/**
	 * 启动SDK系统
	 * 
	 * @param identifier
	 *            用户身份的唯一标识
	 * @param usersig
	 *            用户身份的校验信息
	 */
	int startContext(String identifier, String usersig) {
		int result = AVError.AV_OK;
		if (!hasAVContext()) {
			Log.d(TAG, "WL_DEBUG startContext identifier = " + identifier);
			Log.d(TAG, "WL_DEBUG startContext usersig = " + usersig);

			try {
				mConfig = new AVContext.StartParam();
				mConfig.sdkAppId = Integer.valueOf(VideoConstants.sdkAppId);
				mConfig.accountType = VideoConstants.accountType;
				mConfig.appIdAt3rd = VideoConstants.sdkAppId;
				mConfig.identifier = identifier;

				mUserSig = usersig;
				login();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		
		return result;
	}

	/**
	 * 关闭SDK系统
	 */
	void stopContext() {
		if (hasAVContext()) {
			Log.d(TAG, "WL_DEBUG stopContext");
			mAVContext.stop();
			mIsInStopContext = true;
			logout();
		}
	}
	
	boolean getIsInStartContext() {
		return mIsInStartContext;
	}

	boolean getIsInStopContext() {
		return mIsInStopContext;
	}
	
	boolean setIsInStopContext(boolean isInStopContext) {
		return this.mIsInStopContext = isInStopContext;
	}
	
	boolean hasAVContext() {
		return mAVContext != null;
	}
	
	AVContext getAVContext() {
		return mAVContext;
	}
	
	public String getSelfIdentifier() {
		return mSelfIdentifier;
	}
	
	String getPeerIdentifier() {
		return mPeerIdentifier;
	}

	void setPeerIdentifier(String peerIdentifier) {
		mPeerIdentifier = peerIdentifier;
	}
	private void login()
	{
		Log.d("login", "testEnvStatus: " + testEnvStatus);

		//请确保TIMManager.getInstance().init()一定执行在主线程		
		TIMManager.getInstance().init(mContext, mConfig.sdkAppId);
				
		TIMUser userId = new TIMUser();
		userId.setAccountType(VideoConstants.accountType);
		userId.setAppIdAt3rd(mConfig.appIdAt3rd);
		userId.setIdentifier(mConfig.identifier);     
		
		/**
		 * 登陆所需信息
		 * 1.sdkAppId ： 创建应用时页面上分配的 sdkappid
		 * 2.uid ： 创建应用账号集成配置页面上分配的 accounttype
		 * 3.app_id_at3rd ： 第三方开放平台账号 appid，如果是自有的账号，那么直接填 sdkappid 的字符串形式
		 * 4.identifier ：用户标示符，也就是我们常说的用户 id
		 * 5.user_sig ：使用 tls 后台 api tls_gen_signature_ex 或者工具生成的 user_sig
		 * 
		*/
		TIMManager.getInstance().login(
		    mConfig.sdkAppId  ,
		    userId,
		    mUserSig,
		    new TIMCallBack() {
		      @Override
		      public void onSuccess() {
		        Log.i(TAG, "init successfully. tiny id = " + IMSdkInt.get().getTinyId());
		        onLogin(true, IMSdkInt.get().getTinyId());
		        }
		      
		      @Override
		      public void onError(int code, String desc) {
		        Log.e(TAG, "init failed, imsdk error code  = " + code + ", desc = " + desc);
		        onLogin(false, 0);
		        }
		      });

	}
	
	private void onLogin(boolean result, long tinyId)
	{
		if(result)
		{
			mSelfIdentifier = mConfig.identifier;
			mAVContext = AVContext.createInstance(mContext,false);
			int ret = mAVContext.start(mConfig,mStartContextCompleteCallback);
			mIsInStartContext = true;
		}
		else
		{
			mStartContextCompleteCallback.onComplete(AVError.AV_ERR_FAILED, "");
		}
	}
	
	private void logout()
	{
		
		TIMManager.getInstance().logout();
		onLogout(true);			
	}
	
	private void onLogout(boolean result)
	{
		Log.d(TAG, "WL_DEBUG mStopContextCompleteCallback.OnComplete");
		mAVContext.destroy();
		mAVContext = null;
		Log.d(TAG, "WL_DEBUG mStopContextCompleteCallback mAVContext is null");
		mIsInStopContext = false;
		mContext.sendBroadcast(new Intent(
		MultiUtil.ACTION_CLOSE_CONTEXT_COMPLETE));
	}
}