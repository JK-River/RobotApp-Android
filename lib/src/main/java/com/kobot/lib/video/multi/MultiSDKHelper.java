package com.kobot.lib.video.multi;

import android.content.Context;
import android.util.Log;
import com.tencent.TIMConnListener;
import com.tencent.TIMManager;

public class MultiSDKHelper {
	private static final String TAG = MultiSDKHelper.class.getSimpleName();
	boolean bSDKInited = false;
	boolean isClientStart = true;
	Context context;

	
    public synchronized boolean init(Context context){
        if(bSDKInited){
            return true;
        }
        this.context=context;
        InitTIMSDK();
      //  imdata	= new IMData(context);
        
        bSDKInited = true;
        return true;
    }
    
    public boolean getClientStarted(){
    	return isClientStart;    
    	}
    
    
	private void InitTIMSDK()
	{
	    //设置接入环境(可选)  调用init初始化前, 可使用此接口指定接入环境:
        //参数: 1 - 测试环境  /  0或不调用此接口  - 正式环境
 //     TIMManager.getInstance().setEnv(1);

        TIMManager.getInstance().init(context);
        SetConnCallBack();     

	}

	private void SetConnCallBack(){

        //设置网络连接监听器，连接建立／断开时回调wsd 
        TIMManager.getInstance().setConnectionListener(new TIMConnListener() {
            @Override
            public void onConnected() {
                Log.e(TAG, "connected");
                isClientStart = true;
            }

            @Override
            public void onDisconnected(int code, String desc) {                 
                //接口返回了错误码code和错误描述desc，可用于定位连接断开原因
            	Log.e(TAG, "disconnected");
            }

            @Override
            public void onWifiNeedAuth(String s) {

            }
        });
	}

		
}
