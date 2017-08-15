package com.landi.zxinglibrary.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.lang.ref.WeakReference;
import java.util.Arrays;

/**
 * Created by Django .
 * description : 
 */

public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
	private static final String TAG = "CameraSurfaceView";
	SurfaceHolder mSurfaceHolder;
	CameraManager mCameraManager;
	CameraDevice mCamera;
	Context mContext;
	WeakReference<Handler> mHandler;
//	Handler mHandler;
	CaptureRequest.Builder builder;

	public CameraSurfaceView(Context context) {
		this(context, null);
	}

	public CameraSurfaceView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		HandlerThread handlerThread = new HandlerThread("CAMERA2");
		handlerThread.start();
		Handler handler = new Handler(handlerThread.getLooper()){
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				Log.d(TAG, "handleMessage: ");
			}
		};
		mHandler = new WeakReference<>(handler);
		mContext = context;
		mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
		mSurfaceHolder = getHolder();
		mSurfaceHolder.addCallback(this);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (mCamera != null){
			return;
		}
		try {
			String[] cameraIdList = mCameraManager.getCameraIdList();
//			//获取可用相机设备列表
//			CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraIdList[0]);
//			for (CameraCharacteristics.Key key : characteristics.getKeys() ){
//				Log.d(TAG, "first camera info : "+characteristics.get(key));
//			}
//			//在这里可以通过CameraCharacteristics设置相机的功能,当然必须检查是否支持
//			characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
			if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
				return;
			}
			mCameraManager.openCamera(cameraIdList[0], new CameraStateCallback(), mHandler.get());
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mCamera==null){
			return;
		}
		mCamera.close();
		mCamera = null;
	}


	private void startPreview() {
		try {
			builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
			builder.addTarget(mSurfaceHolder.getSurface());
			mCamera.createCaptureSession(Arrays.asList(mSurfaceHolder.getSurface()),new CaptureSessionStateCallBack(),mHandler.get());
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
	}

	private class MyCaptureCallBack extends CameraCaptureSession.CaptureCallback{

	}

	private class CaptureSessionStateCallBack extends CameraCaptureSession.StateCallback{
		@Override
		public void onConfigured(@NonNull CameraCaptureSession session) {
			try {
				session.setRepeatingRequest(builder.build(),new MyCaptureCallBack(),mHandler.get());
			} catch (CameraAccessException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onConfigureFailed(@NonNull CameraCaptureSession session) {
			session.close();
			Log.e(TAG, "onConfigureFailed ");
		}
	}

	private class CameraStateCallback extends CameraDevice.StateCallback{

		@Override
		public void onOpened(@NonNull CameraDevice camera) {
			mCamera = camera;
			Log.d(TAG, "onOpened: "+mCamera.getId());
			startPreview();
		}

		@Override
		public void onDisconnected(@NonNull CameraDevice camera) {
			mCamera = null;
			Log.i(TAG, "onDisconnected: ");
		}

		@Override
		public void onError(@NonNull CameraDevice camera, int error) {
			mCamera = null;
			Log.e(TAG, "onError: "+error);
		}
	}
}
