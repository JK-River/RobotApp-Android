package com.kobot.lib.video.multi.control;

import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import com.kobot.lib.R;
import com.kobot.lib.common.RobotApplication;
import com.kobot.lib.video.multi.MultiMemberInfo;
import com.kobot.lib.video.multi.MultiUtil;
import com.tencent.av.opengl.GraphicRendererMgr;
import com.tencent.av.opengl.gesturedetectors.MoveGestureDetector;
import com.tencent.av.opengl.gesturedetectors.MoveGestureDetector.OnMoveGestureListener;
import com.tencent.av.opengl.ui.GLRootView;
import com.tencent.av.opengl.ui.GLView;
import com.tencent.av.opengl.ui.GLViewGroup;
import com.tencent.av.opengl.utils.Utils;
import com.tencent.av.sdk.AVView;
import com.tencent.av.utils.QLog;

import java.util.ArrayList;

public class MultiAVUIControl extends GLViewGroup {
	static final String TAG = "VideoLayerUI";

	boolean mIsLocalHasVideo = false;// 自己是否有视频画面

	Context mContext = null;
	GraphicRendererMgr mGraphicRenderMgr = null;

	View mRootView = null;
	int mTopOffset = 0;
	int mBottomOffset = 0;

	public GLRootView mGlRootView = null;
	MultiGLVideoView mGlVideoView[] = null;

	int mClickTimes = 0;
	int mTargetIndex = -1;
	OnTouchListener mTouchListener = null;
	GestureDetector mGestureDetector = null;
	MoveGestureDetector mMoveDetector = null;
	ScaleGestureDetector mScaleGestureDetector = null;
	private MultiQavsdkControl mQavsdkControl;
	
	private int localViewIndex = -1;
	private int remoteViewIndex = -1;
	private String mRemoteIdentifier = "";
	private boolean isSupportMultiVideo = false;

	private SurfaceView mSurfaceView = null;
	private SurfaceHolder.Callback mSurfaceHolderListener = new SurfaceHolder.Callback() {
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			mContext.sendBroadcast(new Intent(MultiUtil.ACTION_SURFACE_CREATED));
			mCameraSurfaceCreated = true;

			MultiQavsdkControl qavsdk = ((RobotApplication) mContext).getMultiQavsdkControl();
			qavsdk.getAVContext().setRenderMgrAndHolder(mGraphicRenderMgr, holder);
			Log.e("memoryLeak", "memoryLeak surfaceCreated");
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			if (holder.getSurface() == null) {
				return;
			}
			holder.setFixedSize(width, height);
			Log.e("memoryLeak", "memoryLeak surfaceChanged");				
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			Log.e("memoryLeak", "memoryLeak surfaceDestroyed");			
		}
	};

	public MultiAVUIControl(Context context, View rootView) {
		if(context == null) {
			mContext = RobotApplication.getContext();
		} else {
			mContext = context;
		}
		mRootView = rootView;
		mGraphicRenderMgr = GraphicRendererMgr.getInstance();
		initQQGlView();
		initCameraPreview();
		initVideoParam();
		setBackground(R.drawable.call_video_demo);
	}

	private void initVideoParam() {
		MultiQavsdkControl qavsdkControl = ((RobotApplication) mContext).getMultiQavsdkControl();
		if (null != qavsdkControl && qavsdkControl.getIsSupportMultiView()) {
			isSupportMultiVideo = true;
		}

		if (QLog.isColorLevel()) {
			QLog.d(TAG, QLog.CLR, "isSupportMultiVideo: " + isSupportMultiVideo);
		}
	}

	@Override
	protected void onLayout(boolean flag, int left, int top, int right, int bottom) {
		if (QLog.isColorLevel()) {
			QLog.d(TAG, QLog.CLR, "onLayout|left: " + left + ", top: " + top + ", right: " + right + ", bottom: " + bottom);
		}
		layoutVideoView(false);
	}

	public void showGlView() {
		if (mGlRootView != null) {
			mGlRootView.setVisibility(View.VISIBLE);
		}
	}

	public void hideGlView() {
		if (mGlRootView != null) {
			mGlRootView.setVisibility(View.GONE);
		}
	}

	public void onResume() {
		if (mGlRootView != null) {
			mGlRootView.onResume();
		}

		setRotation(mCacheRotation);
	}

	public void onPause() {
		if (mGlRootView != null) {
			mGlRootView.onPause();
		}
	}

	public void onDestroy() {
		Log.e("memoryLeak", "memoryLeak MultiAVUIControl onDestroy");
		unInitCameraaPreview();
		mContext = null;
		mRootView = null;

		removeAllView();
		for (int i = 0; i < mGlVideoView.length; i++) {
			mGlVideoView[i].flush();
			mGlVideoView[i].clearRender();
			mGlVideoView[i] = null;
		}
		mGlRootView.setOnTouchListener(null);
		mGlRootView.setContentPane(null);

		mTouchListener = null;
		mGestureDetector = null;
		mMoveDetector = null;
		mScaleGestureDetector = null;

		mGraphicRenderMgr = null;

		mGlRootView = null;
		mGlVideoView = null;
	}
	
	public boolean setLocalHasVideo(boolean isLocalHasVideo, boolean forceToBigView, String identifier) {
		if (mContext == null)
			return false;

		if (Utils.getGLVersion(mContext) == 1) {
			return false;
		}
		
				
		if (isLocalHasVideo) {// 打开摄像头
			MultiGLVideoView view = null;
			int index = getViewIndexById(identifier, AVView.VIDEO_SRC_TYPE_CAMERA);
			if (index < 0) {
				index = getIdleViewIndex(1);
				if (index >= 0) {
					view = mGlVideoView[index];
					view.setRender(identifier, AVView.VIDEO_SRC_TYPE_CAMERA);
					localViewIndex = index;
				}
			} else {
				view = mGlVideoView[index];
			}
			if (view != null) {
				view.setIsPC(false);
				view.enableLoading(false);
				// if (isFrontCamera()) {
				// view.setMirror(true);
				// } else {
				// view.setMirror(false);
				// }
				view.setVisibility(GLView.VISIBLE);
			}
			if (forceToBigView && index > 0) {
				switchVideo(0, index);				
			}
		} else if (!isLocalHasVideo) {// 关闭摄像头
			int index = getViewIndexById(identifier, AVView.VIDEO_SRC_TYPE_CAMERA);
			if (index >= 0) {
				closeVideoView(index);
				localViewIndex = -1;
			}
		}
		mIsLocalHasVideo = isLocalHasVideo;

		return true;
	}

	int mRotation = 0;
	int mCacheRotation = 180;

	public void setRotation(int rotation) {
		if (mContext == null) {
			return;
		}

		if ((rotation % 90) != (mRotation % 90)) {
			mClickTimes = 0;
		}

		mRotation = rotation;
		mCacheRotation = rotation;
		
		// layoutVideoView(true);
		MultiQavsdkControl qavsdk = ((RobotApplication) mContext).getMultiQavsdkControl();
		if ((qavsdk != null) && (qavsdk.getAVVideoControl() != null)) {		
			qavsdk.getAVVideoControl().setRotation(rotation);
		}
		switch (rotation) {
		case 0:
			for (int i = 0; i < getChildCount(); i++) {
				GLView view = getChild(i);
				if(view != null)
					view.setRotation(0);
			}
			break;
		case 90:
			for (int i = 0; i < getChildCount(); i++) {
				GLView view = getChild(i);
				if(view != null)				
					view.setRotation(90);
			}
			break;
		case 180:
			for (int i = 0; i < getChildCount(); i++) {
				GLView view = getChild(i);
				if(view != null)				
					view.setRotation(180);
			}
			break;
		case 270:
			for (int i = 0; i < getChildCount(); i++) {
				GLView view = getChild(i);
				if(view != null)				
					view.setRotation(270);
			}
			break;
		default:
			break;
		}
	}
	public String getQualityTips() {
		MultiQavsdkControl qavsdk = ((RobotApplication) mContext).getMultiQavsdkControl();
		String tipsAudio = "";
		String tipsVideo = "";
		String tipsRoom = "";

		if (qavsdk != null) {
			if (qavsdk.getAVAudioControl() != null) {
				tipsAudio = qavsdk.getAVAudioControl().getQualityTips();
			}
			if (qavsdk.getAVVideoControl() != null) {
				tipsVideo = qavsdk.getAVVideoControl().getQualityTips();
			}

			if (qavsdk.getRoom() != null) {
				tipsRoom = qavsdk.getRoom().getQualityTips();
			}
		}

		String tipsAll = "";

		if(tipsRoom != null && tipsRoom.length() > 0)
		{
			tipsAll += tipsRoom + "\n";
		}

		if(tipsAudio != null && tipsAudio.length() > 0)
		{
			tipsAll += tipsAudio + "\n";
		}

		if(tipsVideo != null && tipsVideo.length() > 0)
		{
			tipsAll += tipsVideo;
		}

		return tipsAll;
	}
	public void setOffset(int topOffset, int bottomOffset) {
		if (QLog.isColorLevel()) {
			QLog.d(TAG, QLog.CLR, "setOffset topOffset: " + topOffset + ", bottomOffset: " + bottomOffset);
		}
		mTopOffset = topOffset;
		mBottomOffset = bottomOffset;
		// refreshUI();
		layoutVideoView(true);
	}

	public void setText(String identifier, int videoSrcType, String text, float textSize, int color) {
		int index = getViewIndexById(identifier, videoSrcType);
		if (index < 0) {
			index = getIdleViewIndex(0);
			if (index >= 0) {
				MultiGLVideoView view = mGlVideoView[index];
				view.setRender(identifier, videoSrcType);
			}
		}
		if (index >= 0) {
			MultiGLVideoView view = mGlVideoView[index];
			view.setVisibility(GLView.VISIBLE);
			view.setText(text, textSize, color);
		}
		if (QLog.isColorLevel()) {
			QLog.d(TAG, QLog.CLR, "setText identifier: " + identifier + ", videoSrcType: " + videoSrcType + ", text: " + text + ", textSize: " + textSize + ", color: " + color + ", index: " + index);
		}
	}

	public void setBackground(String identifier, int videoSrcType, Bitmap bitmap, boolean needRenderVideo) {
		int index = getViewIndexById(identifier, videoSrcType);
		if (index < 0) {
			index = getIdleViewIndex(0);
			if (index >= 0) {
				MultiGLVideoView view = mGlVideoView[index];
				view.setVisibility(GLView.VISIBLE);
				view.setRender(identifier, videoSrcType);
			}
		}
		if (index >= 0) {
			MultiGLVideoView view = mGlVideoView[index];
			view.setBackground(bitmap);
			view.setNeedRenderVideo(needRenderVideo);
			if (!needRenderVideo) {
				view.enableLoading(false);
			}
		}
		if (QLog.isColorLevel()) {
			QLog.d(TAG, QLog.CLR, "setBackground identifier: " + identifier + ", videoSrcType: " + videoSrcType + ", index: " + index + ", needRenderVideo: " + needRenderVideo);
		}
	}

	boolean isLocalFront() {
		boolean isLocalFront = true;
		String selfIdentifier = "";
		MultiGLVideoView view = mGlVideoView[0];
		if (view.getVisibility() == GLView.VISIBLE && selfIdentifier.equals(view.getIdentifier())) {
			isLocalFront = false;
		}
		return isLocalFront;
	}

	int getViewCount() {
		int count = 0;
		for (int i = 0; i < mGlVideoView.length; i++) {
			MultiGLVideoView view = mGlVideoView[i];
			if (view.getVisibility() == GLView.VISIBLE && null != view.getIdentifier()) {
				count++;
			}
		}
		return count;
	}

	int getIdleViewIndex(int start) {
		int index = -1;
		for (int i = start; i < mGlVideoView.length; i++) {
			MultiGLVideoView view = mGlVideoView[i];
			if (null == view.getIdentifier() || view.getVisibility() == GLView.INVISIBLE) {
				index = i;
				break;
			}
		}
		return index;
	}

	int getViewIndexById(String identifier, int videoSrcType) {
		int index = -1;
		if (null == identifier) {
			return index;
		}
		for (int i = 0; i < mGlVideoView.length; i++) {
			MultiGLVideoView view = mGlVideoView[i];
			if ((identifier.equals(view.getIdentifier()) && view.getVideoSrcType() == videoSrcType) && view.getVisibility() == GLView.VISIBLE) {
				index = i;
				break;
			}
		}
		return index;
	}

	void layoutVideoView(boolean virtical) {
		if (QLog.isColorLevel()) {
			QLog.d(TAG, QLog.CLR, "layoutVideoView virtical: " + virtical);
		}
		if (mContext == null)
			return;

		int width = getWidth();
		int height = getHeight();
		mGlVideoView[0].layout(0, 0, width, height);
//		mGlVideoView[0].setBackgroundColor(Color.BLACK);
		mGlVideoView[0].setBackground(R.drawable.call_video_demo);
		//
		int edgeX = mContext.getResources().getDimensionPixelSize(R.dimen.video_small_view_offsetX);
		int edgeY = edgeX;
		if (mBottomOffset != 0) {
			edgeY = mContext.getResources().getDimensionPixelSize(R.dimen.video_small_view_offsetY);
		}
		final int w = (width - edgeX * 5) / 4;
		final int h = w*3/4;
		//
		int left = 0;
		int right = 0;
		int top = height - h - edgeY - mBottomOffset;
		int bottom = height - edgeY - mBottomOffset;
		if (isSupportMultiVideo) {
			if (QLog.isColorLevel()) {
				QLog.d(TAG, QLog.CLR, "SupportMultiVideo");
			}

			int wRemote = mContext.getResources().getDimensionPixelSize(R.dimen.video_small_view_width_landscape);
			int hRemote = mContext.getResources().getDimensionPixelSize(R.dimen.video_small_view_height_landscape);
			int edgeXRemote = mContext.getResources().getDimensionPixelSize(R.dimen.video_small_view_offsetX);
			int edgeYRemote = mContext.getResources().getDimensionPixelSize(R.dimen.video_small_view_offsetY);
			left = edgeXRemote;
			right = left + wRemote;
			top = edgeYRemote + mTopOffset;
//			bottom = top + hRemote;
			bottom = top + h;

//			mGlVideoView[1].layout(left, top, right, bottom);
//			mGlVideoView[1].setBackgroundColor(Color.WHITE);

			//多人画面的位置为了不与下面的关闭免提，打开麦克风等按钮重复，需要重新设计其位置，暂时置于视图中间
//			top = (height - h) / 2;
//			bottom = (height + h) / 2;

			if (virtical) {
				left = mGlVideoView[4].getBounds().left;
				right = mGlVideoView[4].getBounds().right;
			} else {
				left = width - w - edgeX;
				right = width - edgeX;
			}
			mGlVideoView[4].layout(left, top, right, bottom);
			if (virtical) {
				left = mGlVideoView[3].getBounds().left;
				right = mGlVideoView[3].getBounds().right;
			} else {
				right = left-edgeX;
				left = right - w;
			}
			mGlVideoView[3].layout(left, top, right, bottom);
			if (virtical) {
				left = mGlVideoView[2].getBounds().left;
				right = mGlVideoView[2].getBounds().right;
			} else {
				right = left-edgeX;
				left = right - w;
			}
			mGlVideoView[2].layout(left, top, right, bottom);
			if (virtical) {
				left = mGlVideoView[1].getBounds().left;
				right = mGlVideoView[1].getBounds().right;
			} else {
				right = left-edgeX;
				left = right - w;
			}
			mGlVideoView[1].layout(left, top, right, bottom);

			mGlVideoView[1].setBackgroundColor(Color.WHITE);
			mGlVideoView[2].setBackgroundColor(Color.WHITE);
			mGlVideoView[3].setBackgroundColor(Color.WHITE);
			mGlVideoView[4].setBackgroundColor(Color.WHITE);

			mGlVideoView[1].setPaddings(2, 3, 3, 3);
			mGlVideoView[2].setPaddings(2, 3, 2, 3);
			mGlVideoView[3].setPaddings(2, 3, 2, 3);
			mGlVideoView[4].setPaddings(3, 3, 2, 3);
		} else {
			int wRemote = mContext.getResources().getDimensionPixelSize(R.dimen.video_small_view_width_landscape);
			int hRemote = mContext.getResources().getDimensionPixelSize(R.dimen.video_small_view_height_landscape);
			int edgeXRemote = mContext.getResources().getDimensionPixelSize(R.dimen.video_small_view_offsetX);
			int edgeYRemote = mContext.getResources().getDimensionPixelSize(R.dimen.video_small_view_offsetY);
			left = edgeXRemote;
			right = left + wRemote;
			top = edgeYRemote + mTopOffset;
			bottom = top + hRemote;

			mGlVideoView[1].layout(left, top, right, bottom);
			mGlVideoView[1].setBackgroundColor(Color.WHITE);
			mGlVideoView[2].setBackgroundColor(Color.WHITE);
			mGlVideoView[3].setBackgroundColor(Color.WHITE);
			mGlVideoView[4].setBackgroundColor(Color.WHITE);

			mGlVideoView[1].setPaddings(2, 3, 3, 3);
			mGlVideoView[2].setPaddings(2, 3, 2, 3);
			mGlVideoView[3].setPaddings(2, 3, 2, 3);
			mGlVideoView[4].setPaddings(3, 3, 2, 3);
		}
		invalidate();
	}

	void closeVideoView(int index) {
		if (QLog.isColorLevel()) {
			QLog.d(TAG, QLog.CLR, "closeVideoView index: " + index);
		}

	
		MultiGLVideoView view = mGlVideoView[index];
		view.setVisibility(GLView.INVISIBLE);
		view.setNeedRenderVideo(true);
		view.enableLoading(false);
		view.setIsPC(false);
		view.clearRender();

//		for (int i = 0; i < mGlVideoView.length - 1; i++) {
//			MultiGLVideoView view1 = mGlVideoView[i];
//			for (int j = i + 1; j < mGlVideoView.length; j++) {
//				MultiGLVideoView view2 = mGlVideoView[j];
//				if (view1.getVisibility() == GLView.INVISIBLE && view2.getVisibility() == GLView.VISIBLE) {
//					String openId = view2.getOpenId();
//					int videoSrcType = view2.getVideoSrcType();
//					boolean isPC = view2.isPC();
//					boolean isMirror = view2.isMirror();
//					boolean isLoading = view2.isLoading();
//					view1.setRender(openId, videoSrcType);
//					view1.setIsPC(isPC);
//					view1.setMirror(isMirror);
//					view1.enableLoading(isLoading);
//					view1.setVisibility(GLView.VISIBLE);
//					view2.setVisibility(GLView.INVISIBLE);
//				}
//			}
//		}

		layoutVideoView(false);
	}

	void initQQGlView() {
		if (QLog.isColorLevel()) {
			QLog.d(TAG, QLog.CLR, "initQQGlView");
		}
		mGlRootView = (GLRootView) mRootView.findViewById(R.id.av_video_glview);
		mGlVideoView = new MultiGLVideoView[AVView.MAX_VIEW_COUNT];
		// for (int i = 0; i < mGlVideoView.length; i++) {
		// mGlVideoView[i] = new MultiGLVideoView(mVideoController, mContext.getApplicationContext());
		// mGlVideoView[i].setVisibility(GLView.INVISIBLE);
		// addView(mGlVideoView[i]);
		// }
		mGlVideoView[0] = new MultiGLVideoView(mContext.getApplicationContext(), mGraphicRenderMgr);
		mGlVideoView[0].setVisibility(GLView.INVISIBLE);
		addView(mGlVideoView[0]);
		for (int i = AVView.MAX_VIEW_COUNT-1; i >= 1; i--) {
			mGlVideoView[i] = new MultiGLVideoView(mContext.getApplicationContext(), mGraphicRenderMgr);
			mGlVideoView[i].setVisibility(GLView.INVISIBLE);
			addView(mGlVideoView[i]);
		}
		mGlRootView.setContentPane(this);
		// set bitmap ,reuse the backgroud BitmapDrawable,mlzhong
		mGlVideoView[0].setBackground(R.drawable.call_video_demo);

		mScaleGestureDetector = new ScaleGestureDetector(mContext, new ScaleGestureListener());
		mGestureDetector = new GestureDetector(mContext, new GestureListener());
		mMoveDetector = new MoveGestureDetector(mContext, new MoveListener());
		mTouchListener = new TouchListener();
		setOnTouchListener(mTouchListener);
	}

	boolean mCameraSurfaceCreated = false;

	void initCameraPreview() {
		
//		SurfaceView localVideo = (SurfaceView) mRootView.findViewById(R.id.av_video_surfaceView);
//		SurfaceHolder holder = localVideo.getHolder();
//		holder.addCallback(mSurfaceHolderListener);
//		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);// 3.0以下必须在初始化时调用，否则不能启动预览
//		localVideo.setZOrderMediaOverlay(true);
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.width = 1;
        layoutParams.height = 1;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        // layoutParams.flags |= LayoutParams.FLAG_NOT_TOUCHABLE;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.windowAnimations = 0;// android.R.style.Animation_Toast;
        layoutParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        //layoutParams.setTitle("Toast");
        try {
        	mSurfaceView = new SurfaceView(mContext);
            SurfaceHolder holder = mSurfaceView.getHolder();
            holder.addCallback(mSurfaceHolderListener);
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);// 3.0以下必须在初始化时调用，否则不能启动预览
            mSurfaceView.setZOrderMediaOverlay(true);
            windowManager.addView(mSurfaceView, layoutParams);
        } catch (IllegalStateException e) {
            windowManager.updateViewLayout(mSurfaceView, layoutParams);
            if (QLog.isColorLevel()) {
                QLog.d(TAG, QLog.CLR, "add camera surface view fail: IllegalStateException." + e);
            }
        } catch (Exception e) {
            if (QLog.isColorLevel()) {
                QLog.d(TAG, QLog.CLR, "add camera surface view fail." + e);
            }
        }
		Log.e("memoryLeak", "memoryLeak initCameraPreview");
	}
	
	void unInitCameraaPreview() {
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        try {
            windowManager.removeView(mSurfaceView);
            mSurfaceView = null;
        } catch(Exception e) {
            if (QLog.isColorLevel()) {
                QLog.e(TAG, QLog.CLR, "remove camera view fail.", e);
            }
        }
	}

	void switchVideo(int index1, int index2) {
		if (QLog.isColorLevel()) {
			QLog.d(TAG, QLog.CLR, "switchVideo index1: " + index1 + ", index2: " + index2);
		}
		if (index1 == index2 || index1 < 0 || index1 >= mGlVideoView.length || index2 < 0 || index2 >= mGlVideoView.length) {
			return;
		}

		if (GLView.INVISIBLE == mGlVideoView[index1].getVisibility() || GLView.INVISIBLE == mGlVideoView[index2].getVisibility()) {
			Log.d("switchVideo", "can not switchVideo");
			return;
		}

		String identifier1 = mGlVideoView[index1].getIdentifier();
		int videoSrcType1 = mGlVideoView[index1].getVideoSrcType();
		boolean isPC1 = mGlVideoView[index1].isPC();
		boolean isMirror1 = mGlVideoView[index1].isMirror();
		boolean isLoading1 = mGlVideoView[index1].isLoading();
		String identifier2 = mGlVideoView[index2].getIdentifier();
		int videoSrcType2 = mGlVideoView[index2].getVideoSrcType();
		boolean isPC2 = mGlVideoView[index2].isPC();
		boolean isMirror2 = mGlVideoView[index2].isMirror();
		boolean isLoading2 = mGlVideoView[index2].isLoading();

		mGlVideoView[index1].setRender(identifier2, videoSrcType2);
		mGlVideoView[index1].setIsPC(isPC2);
		mGlVideoView[index1].setMirror(isMirror2);
		mGlVideoView[index1].enableLoading(isLoading2);
		mGlVideoView[index2].setRender(identifier1, videoSrcType1);
		mGlVideoView[index2].setIsPC(isPC1);
		mGlVideoView[index2].setMirror(isMirror1);
		mGlVideoView[index2].enableLoading(isLoading1);
		
		int temp = localViewIndex;
		localViewIndex = remoteViewIndex;
		remoteViewIndex = temp;
	}

	class Position {
		final static int CENTER = 0;
		final static int LEFT_TOP = 1;
		final static int RIGHT_TOP = 2;
		final static int RIGHT_BOTTOM = 3;
		final static int LEFT_BOTTOM = 4;
	}

	public void setSmallVideoViewLayout(boolean isRemoteHasVideo, String remoteIdentifier, int videoSrcType) {
		if (QLog.isColorLevel()) {
			QLog.d(TAG, QLog.CLR, "setSmallVideoViewLayout position: " + mPosition);
		}
		if (mContext == null) {
			return;
		}
		
		int left = 0;
		int top = 0;
		int right = 0;
		int bottom = 0;
		int width = getWidth();
		int height = getHeight();
		int w = mContext.getResources().getDimensionPixelSize(R.dimen.video_small_view_width_landscape);
		int h = mContext.getResources().getDimensionPixelSize(R.dimen.video_small_view_height_landscape);
		int edgeX = mContext.getResources().getDimensionPixelSize(R.dimen.video_small_view_offsetX);
		int edgeY = mContext.getResources().getDimensionPixelSize(R.dimen.video_small_view_offsetY);
		if (mBottomOffset == 0) {
			edgeY = edgeX;
		}

		switch (mPosition) {
		case Position.LEFT_TOP:
			left = edgeX;
			right = left + w;
			// if (mBottomOffset != 0) {
			// top = height - h - edgeY - mBottomOffset;
			// bottom = top + h;
			// } else {
			top = edgeY + mTopOffset;
			bottom = top + h;
			// }
			break;
		case Position.RIGHT_TOP:
			left = width - w - edgeX;
			right = left + w;
			// if (mBottomOffset != 0) {
			// top = height - h - edgeY - mBottomOffset;
			// bottom = top + h;
			// } else {
			top = edgeY + mTopOffset;
			bottom = top + h;
			// }
			break;
		case Position.LEFT_BOTTOM:
			left = edgeX;
			right = left + w;
			top = height - h - edgeY - mBottomOffset;
			bottom = top + h;
			break;
		case Position.RIGHT_BOTTOM:
			left = width - w - edgeX;
			top = height - h - edgeY - mBottomOffset;
			right = left + w;
			bottom = top + h;
			break;
		}
		
		
		if (isRemoteHasVideo) {// 打开摄像头
			MultiGLVideoView view = null;
			mRemoteIdentifier = remoteIdentifier;
			int index = getViewIndexById(remoteIdentifier, videoSrcType);
			
			//请求多路画面用这个测试
//			if (remoteViewIndex != -1 && !mRemoteIdentifier.equals("") && !mRemoteIdentifier.equals(remoteIdentifier)) {
//				closeVideoView(remoteViewIndex);
//			}

			if(!isSupportMultiVideo) {
				if (remoteViewIndex != -1) {
					closeVideoView(remoteViewIndex);
				}
			}
//			if (remoteViewIndex != -1 && index != remoteViewIndex) {
//				closeVideoView(remoteViewIndex);
//			}
			if (index < 0) {
				index = getIdleViewIndex(0);					
				if (index >= 0) {
					view = mGlVideoView[index];
					view.setRender(remoteIdentifier, videoSrcType);
					remoteViewIndex = index;
				}
			} else {
				view = mGlVideoView[index];
				view.setRender(remoteIdentifier, videoSrcType);
			}
			if (view != null) {
				view.setIsPC(false);
				view.enableLoading(false);
				view.setVisibility(GLView.VISIBLE);
			}

		} else {// 关闭摄像头
			int index = getViewIndexById(remoteIdentifier, videoSrcType);
			if (index >= 0) {
				closeVideoView(index);
				remoteViewIndex = -1;
			}
		}		
		
		
//		if (null != mGlVideoView[1].getOpenId()) {
//			mGlVideoView[1].clearRender();
//		}
//		
//				
//		mGlVideoView[1].layout(left, top, right, bottom);
//		mGlVideoView[1].setRender(remoteOpenid, videoSrcType);
//		mGlVideoView[1].setIsPC(false);
//		mGlVideoView[1].enableLoading(false);	
//		mGlVideoView[1].setVisibility(View.VISIBLE);
	}

	public GLRootView getRemoteView() {
		if(remoteViewIndex != -1) {
			return mGlVideoView[remoteViewIndex].getGLRootView();
		}
		return null;
//		return mGlRootView;
	}

	int mPosition = Position.LEFT_TOP;
	boolean mDragMoving = false;

	public int getPosition() {
		return mPosition;
	}

	void checkAndChangeMargin(int deltaX, int deltaY) {
		if (mContext == null) {
			return;
		}
		int width = mContext.getResources().getDimensionPixelSize(R.dimen.video_small_view_width_landscape);
		int height = mContext.getResources().getDimensionPixelSize(R.dimen.video_small_view_height_landscape);

		Rect outRect = getBounds();
		int minOffsetX = 0;
		int minOffsetY = 0;
		int maxOffsetX = outRect.width() - width;
		int maxOffsetY = outRect.height() - height;

		int left = mGlVideoView[1].getBounds().left + deltaX;
		int top = mGlVideoView[1].getBounds().top + deltaY;
		if (left < minOffsetX) {
			left = minOffsetX;
		} else if (left > maxOffsetX) {
			left = maxOffsetX;
		}
		if (top < minOffsetY) {
			top = minOffsetY;
		} else if (top > maxOffsetY) {
			top = maxOffsetY;
		}
		int right = left + width;
		int bottom = top + height;
		mGlVideoView[1].layout(left, top, right, bottom);
	}

	int getSmallViewPosition() {
		int position = Position.CENTER;
		Rect visableRect = getBounds();
		int screenCenterX = visableRect.centerX();
		int screenCenterY = visableRect.centerY();
		int viewCenterX = mGlVideoView[1].getBounds().centerX();
		int viewCenterY = mGlVideoView[1].getBounds().centerY();
		if (viewCenterX < screenCenterX && viewCenterY < screenCenterY) {
			position = Position.LEFT_TOP;
		} else if (viewCenterX < screenCenterX && viewCenterY > screenCenterY) {
			position = Position.LEFT_BOTTOM;
		} else if (viewCenterX > screenCenterX && viewCenterY < screenCenterY) {
			position = Position.RIGHT_TOP;
		} else if (viewCenterX > screenCenterX && viewCenterY > screenCenterY) {
			position = Position.RIGHT_BOTTOM;
		}

		return position;
	}

	class TouchListener implements OnTouchListener {
		@Override
		public boolean onTouch(GLView view, MotionEvent event) {
			if (view == mGlVideoView[0]) {
				mTargetIndex = 0;
			} else if (view == mGlVideoView[1]) {
				mTargetIndex = 1;
			} else if (view == mGlVideoView[2]) {
				mTargetIndex = 2;
			} else if (view == mGlVideoView[3]) {
				mTargetIndex = 3;
			} else if (view == mGlVideoView[4]) {
				mTargetIndex = 4;
			} else {
				mTargetIndex = -1;
			}
			if (mGestureDetector != null) {
				mGestureDetector.onTouchEvent(event);
			}
			if (mTargetIndex == 1 && mMoveDetector != null) {
				mMoveDetector.onTouchEvent(event);
			} else if (mTargetIndex == 0 && mGlVideoView[0].getVideoSrcType() == AVView.VIDEO_SRC_TYPE_SCREEN) {
				if (mScaleGestureDetector != null) {
					mScaleGestureDetector.onTouchEvent(event);
				}
				if (mMoveDetector != null) {
					mMoveDetector.onTouchEvent(event);
				}
			}
			return true;
		}
	};

	class GestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onSingleTapConfirmed(MotionEvent event) {
			if (QLog.isColorLevel())
				QLog.d(TAG, QLog.CLR, "GestureListener-->mTargetIndex=" + mTargetIndex);
			if (mTargetIndex <= 0) {
				// 显示控制层
				mContext.sendBroadcast(new Intent(MultiUtil
						.ACTION_TOGGLE_CONTROL_BARS));
				Log.e(TAG, "opengl : GestureListener mTargetIndex = " + mTargetIndex);
			} else {
				Log.e(TAG, "opengl : GestureListener switchVideo mTargetIndex = " + mTargetIndex);
			}
			return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if (mTargetIndex == 0 && mGlVideoView[0].getVideoSrcType() == AVView.VIDEO_SRC_TYPE_SCREEN) {
				mClickTimes++;
				if (mClickTimes % 2 == 1) {
					mGlVideoView[0].setScale(MultiGLVideoView.MAX_SCALE + 1, 0, 0, true);
				} else {
					mGlVideoView[0].setScale(MultiGLVideoView.MIN_SCALE, 0, 0, true);
				}
				return true;
			}
			return super.onDoubleTap(e);
		}
	};

	class MoveListener implements OnMoveGestureListener {
		int startX = 0;
		int startY = 0;
		int endX = 0;
		int endY = 0;
		int startPosition = 0;

		@Override
		public boolean onMoveBegin(MoveGestureDetector detector) {
			if (mTargetIndex == 0) {

			} else if (mTargetIndex == 1) {
				startX = (int) detector.getFocusX();
				startY = (int) detector.getFocusY();
				startPosition = getSmallViewPosition();
			}
			return true;
		}

		@Override
		public boolean onMove(MoveGestureDetector detector) {
			PointF delta = detector.getFocusDelta();
			int deltaX = (int) delta.x;
			int deltaY = (int) delta.y;
			if (mTargetIndex == 0) {
				mGlVideoView[0].setOffset(deltaX, deltaY, false);
			} else if (mTargetIndex == 1) {
				if (Math.abs(deltaX) > AVView.MAX_VIEW_COUNT || Math.abs(deltaY) > AVView.MAX_VIEW_COUNT) {
					mDragMoving = true;
				}
				// 修改拖动窗口的位置
				checkAndChangeMargin(deltaX, deltaY);
			}
			return true;
		}

		@Override
		public void onMoveEnd(MoveGestureDetector detector) {
			PointF delta = detector.getFocusDelta();
			int deltaX = (int) delta.x;
			int deltaY = (int) delta.y;
			if (mTargetIndex == 0) {
				mGlVideoView[0].setOffset(deltaX, deltaY, true);
			} else if (mTargetIndex == 1) {
				// 修改拖动窗口的位置
				checkAndChangeMargin(deltaX, deltaY);
				endX = (int) detector.getFocusX();
				endY = (int) detector.getFocusY();
				mPosition = getSmallViewDstPosition(startPosition, startX, startY, endX, endY);
				afterDrag(mPosition);
			}
		}
	};

	class ScaleGestureListener implements ScaleGestureDetector.OnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			float x = detector.getFocusX();
			float y = detector.getFocusY();
			float scale = detector.getScaleFactor();
			float curScale = mGlVideoView[0].getScale();
			mGlVideoView[0].setScale(curScale * scale, (int) x, (int) y, false);
			return true;
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			return true;
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			float x = detector.getFocusX();
			float y = detector.getFocusY();
			float scale = detector.getScaleFactor();
			float curScale = mGlVideoView[0].getScale();
			mGlVideoView[0].setScale(curScale * scale, (int) x, (int) y, true);
		}

	}

	enum MoveDistanceLevel {
		e_MoveDistance_Min, e_MoveDistance_Positive, e_MoveDistance_Negative
	};

	int getSmallViewDstPosition(int startPosition, int nStartX, int nStartY, int nEndX, int nEndY) {
		int thresholdX = mContext.getApplicationContext().getResources().getDimensionPixelSize(
				R.dimen.video_smallview_move_thresholdX);
		int thresholdY = mContext.getApplicationContext().getResources().getDimensionPixelSize(
				R.dimen.video_smallview_move_thresholdY);
		int xMoveDistanceLevelStandard = thresholdX;
		int yMoveDistanceLevelStandard = thresholdY;

		MoveDistanceLevel eXMoveDistanceLevel = MoveDistanceLevel.e_MoveDistance_Min;
		MoveDistanceLevel eYMoveDistanceLevel = MoveDistanceLevel.e_MoveDistance_Min;

		if (nEndX - nStartX > xMoveDistanceLevelStandard) {
			eXMoveDistanceLevel = MoveDistanceLevel.e_MoveDistance_Positive;
		} else if (nEndX - nStartX < -xMoveDistanceLevelStandard) {
			eXMoveDistanceLevel = MoveDistanceLevel.e_MoveDistance_Negative;
		} else {
			eXMoveDistanceLevel = MoveDistanceLevel.e_MoveDistance_Min;
		}

		if (nEndY - nStartY > yMoveDistanceLevelStandard) {
			eYMoveDistanceLevel = MoveDistanceLevel.e_MoveDistance_Positive;
		} else if (nEndY - nStartY < -yMoveDistanceLevelStandard) {
			eYMoveDistanceLevel = MoveDistanceLevel.e_MoveDistance_Negative;
		} else {
			eYMoveDistanceLevel = MoveDistanceLevel.e_MoveDistance_Min;
		}

		int eBeginPosition = startPosition;
		int eEndPosition = Position.LEFT_TOP;
		int eDstPosition = Position.LEFT_TOP;
		eEndPosition = getSmallViewPosition();

		if (eEndPosition == Position.RIGHT_BOTTOM) {
			if (eBeginPosition == Position.LEFT_TOP) {
				eDstPosition = Position.RIGHT_BOTTOM;
			} else if (eBeginPosition == Position.RIGHT_TOP) {
				eDstPosition = Position.RIGHT_BOTTOM;
			} else if (eBeginPosition == Position.LEFT_BOTTOM) {
				eDstPosition = Position.RIGHT_BOTTOM;
			} else if (eBeginPosition == Position.RIGHT_BOTTOM) {
				if (eXMoveDistanceLevel == MoveDistanceLevel.e_MoveDistance_Negative) {
					if (eYMoveDistanceLevel == MoveDistanceLevel.e_MoveDistance_Negative) {
						eDstPosition = Position.LEFT_TOP;
					} else {
						eDstPosition = Position.LEFT_BOTTOM;
					}
				} else {
					if (eYMoveDistanceLevel == MoveDistanceLevel.e_MoveDistance_Negative) {
						eDstPosition = Position.RIGHT_TOP;
					} else {
						eDstPosition = Position.RIGHT_BOTTOM;
					}
				}
			}
		} else if (eEndPosition == Position.RIGHT_TOP) {
			if (eBeginPosition == Position.LEFT_TOP) {
				eDstPosition = Position.RIGHT_TOP;
			} else if (eBeginPosition == Position.RIGHT_BOTTOM) {
				eDstPosition = Position.RIGHT_TOP;
			} else if (eBeginPosition == Position.LEFT_BOTTOM) {
				eDstPosition = Position.RIGHT_TOP;
			} else if (eBeginPosition == Position.RIGHT_TOP) {
				if (eXMoveDistanceLevel == MoveDistanceLevel.e_MoveDistance_Negative) {
					if (eYMoveDistanceLevel == MoveDistanceLevel.e_MoveDistance_Positive) {
						eDstPosition = Position.LEFT_BOTTOM;
					} else {
						eDstPosition = Position.LEFT_TOP;
					}
				} else {
					if (eYMoveDistanceLevel == MoveDistanceLevel.e_MoveDistance_Positive) {
						eDstPosition = Position.RIGHT_BOTTOM;
					} else {
						eDstPosition = Position.RIGHT_TOP;
					}
				}
			}
		} else if (eEndPosition == Position.LEFT_TOP) {
			if (eBeginPosition == Position.RIGHT_TOP) {
				eDstPosition = Position.LEFT_TOP;
			} else if (eBeginPosition == Position.RIGHT_BOTTOM) {
				eDstPosition = Position.LEFT_TOP;
			} else if (eBeginPosition == Position.LEFT_BOTTOM) {
				eDstPosition = Position.LEFT_TOP;
			} else if (eBeginPosition == Position.LEFT_TOP) {
				if (eXMoveDistanceLevel == MoveDistanceLevel.e_MoveDistance_Positive) {
					if (eYMoveDistanceLevel == MoveDistanceLevel.e_MoveDistance_Positive) {
						eDstPosition = Position.RIGHT_BOTTOM;
					} else {
						eDstPosition = Position.RIGHT_TOP;
					}
				} else {
					if (eYMoveDistanceLevel == MoveDistanceLevel.e_MoveDistance_Positive) {
						eDstPosition = Position.LEFT_BOTTOM;
					} else {
						eDstPosition = Position.LEFT_TOP;
					}
				}
			}
		} else if (eEndPosition == Position.LEFT_BOTTOM) {
			if (eBeginPosition == Position.LEFT_TOP) {
				eDstPosition = Position.LEFT_BOTTOM;
			} else if (eBeginPosition == Position.RIGHT_TOP) {
				eDstPosition = Position.LEFT_BOTTOM;
			} else if (eBeginPosition == Position.RIGHT_BOTTOM) {
				eDstPosition = Position.LEFT_BOTTOM;
			} else if (eBeginPosition == Position.LEFT_BOTTOM) {
				if (eXMoveDistanceLevel == MoveDistanceLevel.e_MoveDistance_Positive) {
					if (eYMoveDistanceLevel == MoveDistanceLevel.e_MoveDistance_Negative) {
						eDstPosition = Position.RIGHT_TOP;
					} else {
						eDstPosition = Position.RIGHT_BOTTOM;
					}
				} else {
					if (eYMoveDistanceLevel == MoveDistanceLevel.e_MoveDistance_Negative) {
						eDstPosition = Position.LEFT_TOP;
					} else {
						eDstPosition = Position.LEFT_BOTTOM;
					}
				}
			}
		}
		return eDstPosition;
	}

	void afterDrag(int position) {
		int width = mContext.getResources().getDimensionPixelSize(R.dimen.video_small_view_width_landscape);
		int height = mContext.getResources().getDimensionPixelSize(R.dimen.video_small_view_height_landscape);
		int edgeX = mContext.getResources().getDimensionPixelSize(R.dimen.video_small_view_offsetX);
		int edgeY = mContext.getResources().getDimensionPixelSize(R.dimen.video_small_view_offsetY);
		if (mBottomOffset == 0) {
			edgeY = edgeX;
		}
		Rect visableRect = getBounds();

		int fromX = mGlVideoView[1].getBounds().left;
		int fromY = mGlVideoView[1].getBounds().top;
		int toX = 0;
		int toY = 0;

		switch (position) {
		case Position.LEFT_TOP:
			toX = edgeX;
			toY = edgeY;
			break;
		case Position.RIGHT_TOP:
			toX = visableRect.width() - edgeX - width;
			toY = edgeY;
			break;
		case Position.RIGHT_BOTTOM:
			toX = visableRect.width() - edgeX - width;
			toY = visableRect.height() - edgeY - height;
			break;
		case Position.LEFT_BOTTOM:
			toX = edgeX;
			toY = visableRect.height() - edgeY - height;
			break;
		default:
			break;
		}
	}
	public void setSelfId(String key) {
		if (mGraphicRenderMgr != null) {
			mGraphicRenderMgr.setSelfId(key + "_" + AVView.VIDEO_SRC_TYPE_CAMERA);
		}
	}
	void onMemberChange() {
		Log.d(TAG, "WL_DEBUG onMemberChange start");
		if(mContext == null) {
			mContext = RobotApplication.getContext();
		}
		MultiQavsdkControl qavsdk = ((RobotApplication) mContext).getMultiQavsdkControl();
		ArrayList<MultiMemberInfo> audioAndCameraMemberList = qavsdk.getAudioAndCameraMemberList();

		for (MultiMemberInfo memberInfo : audioAndCameraMemberList) {
			int index = getViewIndexById(memberInfo.identifier, AVView.VIDEO_SRC_TYPE_CAMERA);
			if (index >= 0) {
				Log.d(TAG, "WL_DEBUG onMemberChange memberInfo.hasCameraVideo = " + memberInfo.hasCameraVideo);

				if (!memberInfo.hasCameraVideo && !memberInfo.hasAudio) {
					closeVideoView(index);
				}
			}
		}

		ArrayList<MultiMemberInfo> screenMemberList = qavsdk.getScreenMemberList();

		for (MultiMemberInfo memberInfo : screenMemberList) {
			int index = getViewIndexById(memberInfo.identifier, AVView.VIDEO_SRC_TYPE_SCREEN);
			if (index >= 0) {
				Log.d(TAG, "WL_DEBUG onMemberChange memberInfo.hasScreenVideo = " + memberInfo.hasScreenVideo);

				if (!memberInfo.hasScreenVideo) {
					closeVideoView(index);
				}
			}
		}

		ArrayList<MultiMemberInfo> memberList = qavsdk.getMemberList();
		// 去掉已经不再memberlist中的view
		if (!memberList.isEmpty()) {
			for (int i = 0; i < mGlVideoView.length; i++) {
				MultiGLVideoView view = mGlVideoView[i];
				if (view == null)
					continue;
				String viewIdentifier = view.getIdentifier();
				int viewVideoSrcType = view.getVideoSrcType();

				if (TextUtils.isEmpty(viewIdentifier) || viewVideoSrcType == AVView.VIDEO_SRC_TYPE_NONE)
					continue;
				
				
				boolean memberExist = false;
				for (int j=0; j<memberList.size(); j++) {
					if (!TextUtils.isEmpty(memberList.get(j).identifier)) {
						int videoSrcType = AVView.VIDEO_SRC_TYPE_NONE;
						if(memberList.get(j).hasCameraVideo)videoSrcType = AVView.VIDEO_SRC_TYPE_CAMERA;
						else if(memberList.get(j).hasScreenVideo)videoSrcType = AVView.VIDEO_SRC_TYPE_SCREEN;
						else videoSrcType = AVView.VIDEO_SRC_TYPE_NONE;

						if (viewIdentifier.equals(memberList.get(j).identifier) && viewVideoSrcType == videoSrcType) {
							memberExist = true;
							break;
						}
					}
				}

				if (!memberExist) {
					mQavsdkControl = ((RobotApplication) mContext.getApplicationContext()).getMultiQavsdkControl();
					if (null != mQavsdkControl) {
						String selfIdentifier = mQavsdkControl.getSelfIdentifier();
						Log.d(TAG, "self identifier : " + selfIdentifier);
						if (selfIdentifier != null && selfIdentifier.equals(viewIdentifier)) {
							return;
						}
					}

					closeVideoView(i);
				}
			}	
		} else {
			for (int i = 0; i < mGlVideoView.length; i++) {
				closeVideoView(i);
			}
		}

		Log.d(TAG, "WL_DEBUG onMemberChange end");
	}
}
