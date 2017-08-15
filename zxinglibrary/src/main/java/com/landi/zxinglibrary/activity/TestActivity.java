package com.landi.zxinglibrary.activity;

import android.content.Intent;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.landi.zxinglibrary.R;
import com.landi.zxinglibrary.camera.RxDecodeHelper;
import com.landi.zxinglibrary.camera.ScanRectInfo;

public class TestActivity extends AppCompatActivity {

	RxDecodeHelper mDecodeHelper;
	//相机预览所处的线程
	Handler mCameraHandler;
	//解码器所处的线程
	Handler mDecorderHandler;

	TextureView mTextureView;
	ImageView mCaptureScanLine;
	RelativeLayout mCaptureCropView;
	RelativeLayout mCaptureContainer;

	public static final int MSG_DECODE_SUCCESS = 0X01;
	public static final int MSG_DECODE_FAILED = 0X02;

	public static final int REQUEST_CODE = 99;

	private static final String TAG = "TestActivity";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//保持常亮
		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_test);

		mTextureView = (TextureView)findViewById(R.id.capture_preview);
		mCaptureScanLine = (ImageView)findViewById(R.id.capture_scan_line);
		mCaptureCropView = (RelativeLayout)findViewById(R.id.capture_crop_view);
		mCaptureContainer = (RelativeLayout)findViewById(R.id.capture_container);

		// TODO: 2017/8/7 待加入计时关闭功能 + 蜂鸣器提示

		TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation
				.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT,
				0.9f);
		animation.setDuration(4500);
		animation.setRepeatCount(-1);
		animation.setRepeatMode(Animation.RESTART);
		mCaptureScanLine.startAnimation(animation);

		initHandler();
		initTextureView();

	}

	private void initHandler() {
		HandlerThread cameraThread = new HandlerThread("Camera2");
		cameraThread.start();
		mCameraHandler = new Handler(cameraThread.getLooper());

		HandlerThread decoderThread = new HandlerThread("Decoder");
		decoderThread.start();
		mDecorderHandler = new Handler(decoderThread.getLooper()){
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				switch (msg.what){
					case MSG_DECODE_SUCCESS:
						Log.d(TAG, "handleMessage: decode success! result:"+msg.obj.toString());
						break;
					case MSG_DECODE_FAILED:
						Log.d(TAG, "handleMessage: decode failed!");
						break;
				}
			}
		};

	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	private void initTextureView() {
		mTextureView.setSurfaceTextureListener(new MySurfaceTextureListener());
		mDecodeHelper = new RxDecodeHelper(this,mCameraHandler,mDecorderHandler);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDecodeHelper.close();
	}

	class MySurfaceTextureListener implements TextureView.SurfaceTextureListener{
		@Override
		public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
			Log.d(TAG, "onSurfaceTextureAvailable: ");
			Rect decodingViewRect = new Rect(mCaptureCropView.getLeft(),mCaptureCropView.getTop(),mCaptureCropView.getRight(),mCaptureCropView.getBottom());
			//宽与高需要倒转
			float xRatio = decodingViewRect.left/(float)width;
			float yRatio = decodingViewRect.top/(float)height;
			float widthRatio = decodingViewRect.width()/(float)width;
			float heightRatio = decodingViewRect.height()/(float)height;

			ScanRectInfo scanRectInfo = new ScanRectInfo(xRatio,yRatio,widthRatio,heightRatio);
			mDecodeHelper.setScanningRectInfo(scanRectInfo);
			Log.d(TAG, "set decoding rect is : "+decodingViewRect);
			mDecodeHelper.preview(surface, new RxDecodeHelper.OnAchieveDecodedResult() {
				@Override
				public void achieveResult(String result) {
					Log.d(TAG, "achieveResult: "+result);
					Intent intent = new Intent();
					intent.putExtra("result",result);
					setResult(REQUEST_CODE,intent);
					finish();
				}
			});
		}

		@Override
		public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

		}

		@Override
		public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
			return false;
		}

		@Override
		public void onSurfaceTextureUpdated(SurfaceTexture surface) {
		}
	}


}
