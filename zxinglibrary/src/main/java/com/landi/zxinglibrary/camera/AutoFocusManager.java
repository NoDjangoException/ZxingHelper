package com.landi.zxinglibrary.camera;

import android.hardware.Camera;
import android.os.HandlerThread;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;

/**
 * Created by Django .
 * description :  负责完成相机自动对焦的功能
 */

public    class AutoFocusManager {

	private static final String TAG = "AutoFocusManager";
	private Camera mCamera;
	private boolean useAutoFocus;
	private Disposable autoFoucusDisposable;
	private HandlerThread autoFocusHandler;

	private static final long AUTO_FOCUS_INTERVAL_MS = 2000L;
	private static final Collection<String> FOCUS_MODES_CALLING_AF;

	static {
		FOCUS_MODES_CALLING_AF = new ArrayList<>(2);
		FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_AUTO);
		FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_MACRO);
	}

	public AutoFocusManager(Camera camera) {
		mCamera = camera;
	}

	public void start(){
		String currentFocusMode = mCamera.getParameters().getFocusMode();
		useAutoFocus = FOCUS_MODES_CALLING_AF.contains(currentFocusMode);
		Log.i(TAG, "Current focus mode '" + currentFocusMode + "'; use auto focus? " + useAutoFocus);

		if (!useAutoFocus){
			return;
		}

		autoFocusHandler = new HandlerThread("AUTO-FOCUS");
		autoFocusHandler.setDaemon(true);
		autoFocusHandler.start();
		//设为守护进程，在程序关闭后，会自动关闭
		autoFoucusDisposable = Flowable.interval(AUTO_FOCUS_INTERVAL_MS, TimeUnit.MILLISECONDS)
				.subscribeOn(AndroidSchedulers.from(autoFocusHandler.getLooper()))
				.map(new Function<Long, Object>() {
					@Override
					public Object apply(@NonNull Long aLong) throws Exception {
						mCamera.autoFocus(new MyAutoFocusCallback());
						return aLong;
					}
				})
				.subscribe();
	}

	public void stop(){
		if (autoFoucusDisposable!=null&&!autoFoucusDisposable.isDisposed()){
			autoFoucusDisposable.dispose();
		}
	}

	class MyAutoFocusCallback implements Camera.AutoFocusCallback{
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			Log.d(TAG, "is onAutoFocus succeed?:"+success);
		}
	}
}
