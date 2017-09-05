package com.landi.zxinglibrary.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;

import com.landi.zxinglibrary.R;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Django .
 * description :  监测
 */

public    class PermissionUtil {

	public static final int PERMISSION_REQUEST_CODE = 99;
	public static final int SETTINGS_REQUEST_CODE = 101;


	/**
	 * 判断是否获取了全部需要的权限
	 * @param activity
	 * @param permission
	 * @return
	 */
	public static boolean isAllPermissionGranted(Activity activity, String... permission){
		List<String> denyPermissions = findDeniedPermissions(activity,permission);
		if (denyPermissions==null||denyPermissions.size()==0){
			return true;
		}else
			return false;
	}

	/**
	 * 找出未获取的权限
	 * @param activity
	 * @param permission
	 * @return
	 */
	public static List<String> findDeniedPermissions(Activity activity, String... permission) {
		if (activity == null) {
			return null;
		}

		List<String> denyPermissions = new ArrayList<>();

		if (Build.VERSION.SDK_INT >= 23) {
			for (String value : permission) {
				if (activity.checkSelfPermission(value) != PackageManager.PERMISSION_GRANTED) {
					denyPermissions.add(value);
				}
			}
		}

		return denyPermissions;
	}

	public static void checkPermissionAndGotoSetup(Activity activity, PermissionRequestCallback callback, String... permission){

		if (Build.VERSION.SDK_INT<23){
			callback.onPermissionGranted();
			return;
		}
		requestPermission(activity,callback,permission);
	}

	/**
	 * 判断是否要动态申请权限
	 * @param activity
	 * @param permission
	 */
	@TargetApi(23)
	private static void requestPermission(final Activity activity, final PermissionRequestCallback callback, String... permission){

		final List<String> denyPermissions = findDeniedPermissions(activity,permission);
		if (denyPermissions==null||denyPermissions.size()==0){
			return;
		}
		//默认请求打开权限
		boolean shouldShowRationale = true;
		for (String perm : denyPermissions) {
			//shouldShowRequestPermissionRationale 判断对于该项权限是否还提醒用户开启
			shouldShowRationale = shouldShowRationale || ActivityCompat.shouldShowRequestPermissionRationale(activity, perm);
		}

		if(shouldShowRationale){
			new AlertDialog.Builder(activity).setMessage(R.string.ask_for_permission)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
//							Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//							Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
//							intent.setData(uri);
//							activity.startActivityForResult(intent,SETTINGS_REQUEST_CODE);
							activity.requestPermissions(denyPermissions.toArray(new String[denyPermissions.size()]),PERMISSION_REQUEST_CODE);
						}
					})
					.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							callback.onPermissionDenied();
							dialog.dismiss();
						}
					})
					.create()
					.show();
		}
	}


	public interface PermissionRequestCallback{
		void onPermissionGranted();
		void onPermissionDenied();
	}
}
