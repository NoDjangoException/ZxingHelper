package com.landi.zxinglibrary.camera;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;

import com.google.zxing.Result;
import com.landi.zxinglibrary.exception.DecodeFailedException;
import com.landi.zxinglibrary.util.Decoder;

import org.reactivestreams.Publisher;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

/**
 * Created by Django .
 * description :
 */

public class RxDecodeHelper {
	private static final String TAG = "RxDecodeHelper";
	private Context mContext;
	private Camera mCamera1;
	private Decoder mDecoder;
	private CameraConfigManager mCameraConfigManager;
	private Handler mCameraHandler;
	private Handler mDecodeHandler;
	private ScanRectInfo mScanningRectInfo;
	private Rect mScanRect;
	private Disposable mDisposable;
	private AutoFocusManager mAutoFocusManager;

	public RxDecodeHelper(Context context, Handler cameraHandler, Handler decodeHandler) {
		mContext = context;
		mCameraHandler = cameraHandler;
		mDecodeHandler = decodeHandler;
		mDecoder = new Decoder(Decoder.QR_CODE_MODE,mDecodeHandler);
	}

	/**
	 * 从屏幕中获取扫描的区域
	 * @param scanningRectInfo 被扫区域的信息
	 */
	public void setScanningRectInfo(ScanRectInfo scanningRectInfo) {
		mScanningRectInfo = scanningRectInfo;
	}

	/**
	 * 选择合适的相机
	 */
	private void initCamera() {

		int numCameras = Camera.getNumberOfCameras();
		if (numCameras == 0) {
			Log.w(TAG, "No cameras!");
			return;
		}

		// Select a camera if no explicit camera requested
		int index = 0;
		while (index < numCameras) {
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			Camera.getCameraInfo(index, cameraInfo);
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
				break;
			}
			index++;
		}
		if (index < numCameras) {
			Log.i(TAG, "Opening camera #" + index);
			mCamera1 = Camera.open(index);
		} else {
			Log.i(TAG, "No camera facing back; returning camera #0");
			mCamera1 = Camera.open(0);
		}
	}

	public void preview(SurfaceTexture surfaceTexture, final OnAchieveDecodedResult resultListener){
		if (mCamera1==null){
			initCamera();
		}
		mCameraConfigManager = new CameraConfigManager(mCamera1,mContext);
		mCameraConfigManager.setPreviewParameters();
		initDecodeRect();
		try {
			mCamera1.setPreviewTexture(surfaceTexture);
			mCamera1.startPreview();
			mCameraHandler.post(new Runnable() {
				@Override
				public void run() {
					startCaptureAndDecode(resultListener);
				}
			});
			mAutoFocusManager = new AutoFocusManager(mCamera1);
			mAutoFocusManager.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initDecodeRect() {

		if (mScanningRectInfo==null){
			return;
		}

		//因为相机默认是横屏，而矩形信息是以竖屏获得的，故需要转换。
		mScanningRectInfo.changeToHorizontal();

		int cameraWidth = mCameraConfigManager.getCameraResolution().x;
		int cameraHeight = mCameraConfigManager.getCameraResolution().y;

		/** 计算最终截取的矩形的左上角顶点x坐标 */
		int x = (int)(cameraWidth*mScanningRectInfo.xRatio);
		/** 计算最终截取的矩形的左上角顶点y坐标 */
		int y = (int)(cameraHeight*mScanningRectInfo.yRatio);

		/** 计算最终截取的矩形的宽度 */
		int width = (int)(cameraWidth*mScanningRectInfo.widthRatio);
		/** 计算最终截取的矩形的高度 */
		int height = (int)(cameraHeight*mScanningRectInfo.heightRatio);

		/** 生成最终的截取的矩形 */
		mScanRect = new Rect(x, y, width + x, height + y);
	}

	private void startCaptureAndDecode(final OnAchieveDecodedResult resultListener) {
		Log.d(TAG,Thread.currentThread().getName()+ " startCaptureAndDecode: ");

		mDisposable = Flowable.just(1)
				.observeOn(AndroidSchedulers.from(mDecodeHandler.getLooper()))
				.map(new CaptureFunc())
				.map(new DecodeFunc())
				.retryWhen(retryDecode(0))
				.subscribe(
						new Consumer<String>() {
							@Override
							public void accept(@NonNull String s) throws Exception {
								Log.d(TAG, "accept: achieve result!!--" + s);
								resultListener.achieveResult(s);
							}
						},
						new Consumer<Throwable>() {
							@Override
							public void accept(@NonNull Throwable throwable) throws Exception {
								Log.e(TAG, "accept error :",throwable );
							}
						}
				);
	}



	public Function<? super Flowable<Throwable>, ? extends Publisher<?>> retryDecode(final int delayMills) {
		return new Function<Flowable<Throwable>, Publisher<?>>() {
			@Override
			public Publisher<?> apply(@NonNull Flowable<Throwable> throwableFlowable) throws Exception {
				return throwableFlowable.flatMap(new Function<Throwable, Publisher<?>>() {
					@Override
					public Publisher<?> apply(@NonNull Throwable throwable) throws Exception {
						if (throwable instanceof DecodeFailedException){
							Log.e(TAG, "apply DecodeFailedException and retry");
							return Flowable.timer(delayMills, TimeUnit.MILLISECONDS);
						}
						Log.e(TAG, "apply: ", throwable);
						return Flowable.error(throwable);
					}
				});
			}
		};
	}


	public void close() {
		if (mCamera1 != null) {
			mCamera1.release();
			mCamera1 = null;
		}
		if (!mDisposable.isDisposed()){
			mDisposable.dispose();
		}
		if (mAutoFocusManager!=null){
			mAutoFocusManager.stop();
		}
	}

	class CaptureFunc implements Function<Object,byte[]> {

		CountDownLatch captureLatch;
		byte[] imageData;

		public CaptureFunc() {
			captureLatch = new CountDownLatch(1);
		}

		@Override
		public byte[] apply(@NonNull Object o) throws Exception {
			Log.d(TAG, "CaptureFunc apply: ");
			mCamera1.setOneShotPreviewCallback(new Camera.PreviewCallback() {
				@Override
				public void onPreviewFrame(byte[] data, Camera camera) {
					imageData = data;
					captureLatch.countDown();
				}
			});
			Log.d(TAG, "apply: wait for image");
			captureLatch.await();
			Log.d(TAG, "apply: achieve image!");
			return imageData;
		}
	}

	class DecodeFunc implements Function<byte[],String>{
		@Override
		public String apply(@NonNull byte[] imageData) throws Exception {
			Log.d(TAG, "DecodeFunc apply: ");
			int width = mCameraConfigManager.getCameraResolution().x;
			int height = mCameraConfigManager.getCameraResolution().y;
			Log.d(TAG, "apply: width is "+width+",height is :"+height);
			Result zxingResult = mDecoder.decode(imageData,width,height,mScanRect);
			if (zxingResult==null){
				throw new DecodeFailedException("decode failed!");
			}
			return zxingResult.getText();
		}
	}


	public interface OnAchieveDecodedResult{
		void achieveResult(String result);
	}

}


