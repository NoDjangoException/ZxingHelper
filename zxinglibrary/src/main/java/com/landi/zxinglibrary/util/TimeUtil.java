package com.landi.zxinglibrary.util;

import android.util.Log;

import java.util.Date;

/**
 * Created by Django .
 * description : 
 */

public    class TimeUtil {
	public static void showUpTime(String tag, Date...date){
		for (int i=1;i<date.length;i++){
			long timeGap = date[i].getTime()-date[i-1].getTime();
			Log.d(tag, String.format("第 %d 次事件间隔为：%d",i,timeGap));
		}
	}
}
