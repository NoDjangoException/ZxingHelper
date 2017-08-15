package com.landi.zxinglibrary.camera;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Django .
 * description : 该类主要负责设置相机的参数信息，获取最佳的预览界面
 */

public    class CameraConfigManager   {
	private static final String TAG = "CameraConfigManager";

	private static final int MIN_PREVIEW_PIXELS = 480 * 320;
	private static final double MAX_ASPECT_DISTORTION = 0.15;




	// 屏幕分辨率
	private Point screenResolution;
	// 相机分辨率
	private Point cameraResolution;

	private Camera mCamera;
	private Context mContex;


	public CameraConfigManager(Camera camera, Context contex) {
		mCamera = camera;
		mContex = contex;
		init();
	}

	/**
	 * 从相机参数中获取可用于预览的尺寸
	 * @return 可用于预览的尺寸
	 */
	private List<Camera.Size> getPreviewSizeList(){
		Camera.Parameters parameters = mCamera.getParameters();
		List<Camera.Size> previewSizeList = parameters.getSupportedPreviewSizes();
		if (previewSizeList.size()==0){
			previewSizeList.add(parameters.getPreviewSize());
		}
		return previewSizeList;
	}


	private Point findBestPreviewSizeValue(Point screenResolutionForCamera) {
		Point tempCameraResolution = new Point();
		List<Camera.Size> rawSupportedSizes = getPreviewSizeList();
		if (rawSupportedSizes.size()==1) {
			Log.w(TAG, "Device returned no supported preview sizes; using default");
			Camera.Size defaultSize = rawSupportedSizes.get(0);
			tempCameraResolution.set(defaultSize.width, defaultSize.height);
			return tempCameraResolution;
		}
		// Sort by size, increase
		List<Camera.Size> supportedPreviewSizes = new ArrayList<>(rawSupportedSizes);
		Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
			@Override
			public int compare(Camera.Size a, Camera.Size b) {
				int aPixels = a.height * a.width;
				int bPixels = b.height * b.width;
				if (bPixels < aPixels) {
					return 1;
				}
				if (bPixels > aPixels) {
					return -1;
				}
				return 0;
			}
		});
		if (Log.isLoggable(TAG, Log.INFO)) {
			StringBuilder previewSizesString = new StringBuilder();
			for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
				previewSizesString.append(supportedPreviewSize.width).append('x').append
						(supportedPreviewSize.height).append(' ');
			}
			Log.i(TAG, "Supported preview sizes: " + previewSizesString);
		}

		double screenAspectRatio = (double) screenResolutionForCamera.x / (double) screenResolutionForCamera.y;

		//筛选出适用的预览尺寸
		List<Camera.Size> enablePreviewSize = new ArrayList<>();
		for (Camera.Size itemSize : supportedPreviewSizes){
			int realWidth = itemSize.width;
			int realHeight = itemSize.height;
			if (realWidth * realHeight < MIN_PREVIEW_PIXELS) {
				continue;
			}

			boolean isCandidatePortrait = realWidth > realHeight;//true 表示为竖屏

			int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
			int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;

			double aspectRatio = (double) maybeFlippedWidth / (double) maybeFlippedHeight;
			double distortion = Math.abs(aspectRatio - screenAspectRatio);
			if (distortion > MAX_ASPECT_DISTORTION) {
				continue;
			}

			if (maybeFlippedWidth == screenResolutionForCamera.x && maybeFlippedHeight == screenResolutionForCamera.y) {
				Point exactPoint = new Point(realWidth, realHeight);
				Log.i(TAG, "Found preview size exactly matching screen size: " + exactPoint);
				return exactPoint;
			}
			enablePreviewSize.add(itemSize);
		}

		// If no exact match, use largest preview size. This was not a great
		// idea on older devices because
		// of the additional computation needed. We're likely to get here on
		// newer Android 4+ devices, where
		// the CPU is much more powerful.
		if (!enablePreviewSize.isEmpty()) {
			Camera.Size smllestPreview = enablePreviewSize.get(0);
			Point smallestSize = new Point(smllestPreview.width, smllestPreview.height);
			Log.i(TAG, "Using smallest suitable preview size: " + smallestSize);
			return smallestSize;
		}

		// If there is nothing at all suitable, return current preview size
		Camera.Size defaultPreview = mCamera.getParameters().getPreviewSize();
		Point defaultSize = new Point(defaultPreview.width, defaultPreview.height);
		Log.i(TAG, "No suitable preview sizes, using default: " + defaultSize);

		return defaultSize;
	}

	/**
	 * 进行初始化，获取适合的相机分辨率
	 */
	private void init(){
		if (screenResolution!=null&&cameraResolution!=null){
			return;
		}
		screenResolution = new Point();
		WindowManager manager = (WindowManager) mContex.getSystemService(Context.WINDOW_SERVICE);
		Display display = manager.getDefaultDisplay();
		display.getSize(screenResolution);
		Log.i(TAG, "Screen resolution: " + screenResolution);

		/** 因为换成了竖屏显示，所以不替换屏幕宽高得出的预览图是变形的 */
		Point screenResolutionForCamera = new Point(screenResolution.x,screenResolution.y);

		if (screenResolution.x < screenResolution.y) {
			screenResolutionForCamera.x = screenResolution.y;
			screenResolutionForCamera.y = screenResolution.x;
		}

		cameraResolution = findBestPreviewSizeValue(screenResolution);
		Log.i(TAG, "Camera resolution x: " + cameraResolution.x);
		Log.i(TAG, "Camera resolution y: " + cameraResolution.y);
	}

	Point getCameraResolution() {
		return cameraResolution;
	}

	public Point getScreenResolution() {
		return screenResolution;
	}

	/**
	 * 根据计算的结果设置预览参数
	 */
	void setPreviewParameters(){
		Camera.Parameters parameters = mCamera.getParameters();
		parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);
		mCamera.setParameters(parameters);

		Camera.Parameters afterParameters = mCamera.getParameters();
		Camera.Size afterSize = afterParameters.getPreviewSize();
		if (afterSize != null && (cameraResolution.x != afterSize.width || cameraResolution.y != afterSize
				.height)) {
			Log.w(TAG, "Camera said it supported preview size " + cameraResolution.x + 'x' +
					cameraResolution.y + ", but after setting it, preview size is " + afterSize.width + 'x'
					+ afterSize.height);
			cameraResolution.x = afterSize.width;
			cameraResolution.y = afterSize.height;
		}

		/** 设置相机预览为竖屏 */
		mCamera.setDisplayOrientation(90);
	}
}
