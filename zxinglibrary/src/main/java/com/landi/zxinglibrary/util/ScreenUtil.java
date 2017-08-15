package com.landi.zxinglibrary.util;

import android.content.Context;

import java.lang.reflect.Field;

/**
 * Created by Django .
 * description : 
 */

public    class ScreenUtil   {
	public static int getStatusBarHeight(Context context) {
		try {
			Class<?> c = Class.forName("com.android.internal.R$dimen");
			Object obj = c.newInstance();
			Field field = c.getField("status_bar_height");
			int x = Integer.parseInt(field.get(obj).toString());
			return context.getApplicationContext().getResources().getDimensionPixelSize(x);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
}
