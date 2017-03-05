package com.kobot.lib.video.multi;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MultiMyDate {
	public static String getFileName() {
		SimpleDateFormat format = new SimpleDateFormat("yy.MM.dd.HH");
		String date = format.format(new Date(System.currentTimeMillis()));
		return date;// 2012年10月03日 23:41:31
	}

	public static String getDateEN() {
		SimpleDateFormat format1 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
		String date1 = format1.format(new Date(System.currentTimeMillis()));
		return date1;// 2012-10-03 23:41:31
	}

}