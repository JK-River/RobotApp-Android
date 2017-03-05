package com.kobot.lib.video.multi;

import com.tencent.av.sdk.AVRoomMulti;

public class VideoConstants {

	public static final int DEMO_ERROR_BASE = -99999999;

	public static final int AUDIO_VOICE_CHAT_MODE = 0;
	/**
	 * 空指针
	 */
	public static final int DEMO_ERROR_NULL_POINTER = DEMO_ERROR_BASE + 1;


	public static final long HOST_AUTH = AVRoomMulti.AUTH_BITS_DEFAULT;
	//权限位；TODO：默认值是拥有所有权限。
	public static final long VIDEO_MEMBER_AUTH = AVRoomMulti.AUTH_BITS_DEFAULT;//权限位；TODO：默认值是拥有所有权限。
	public static final long NORMAL_MEMBER_AUTH = AVRoomMulti.AUTH_BITS_JOIN_ROOM | AVRoomMulti.AUTH_BITS_RECV_AUDIO | AVRoomMulti.AUTH_BITS_RECV_CAMERA_VIDEO | AVRoomMulti.AUTH_BITS_RECV_SCREEN_VIDEO;


	public static final String HOST_ROLE = "Host";

	public static final String sdkAppId = "";
	public static final String accountType = "";
}
