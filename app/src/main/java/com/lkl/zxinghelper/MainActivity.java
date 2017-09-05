package com.lkl.zxinghelper;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.landi.zxinglibrary.activity.TestActivity;
import com.landi.zxinglibrary.util.PermissionUtil;

public class MainActivity extends AppCompatActivity {

	private TextView mTvResult;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		findViewById(R.id.btn_scan).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(PermissionUtil.isAllPermissionGranted(MainActivity.this,new String[]{Manifest.permission.CAMERA})){
					startActivityForResult(new Intent(MainActivity.this,TestActivity.class),TestActivity.REQUEST_CODE);
					return;
				}
				PermissionUtil.checkPermissionAndGotoSetup(MainActivity.this, new PermissionUtil.PermissionRequestCallback() {
					@Override
					public void onPermissionGranted() {
						startActivityForResult(new Intent(MainActivity.this,TestActivity.class),TestActivity.REQUEST_CODE);
					}

					@Override
					public void onPermissionDenied() {
					}
				},new String[]{Manifest.permission.CAMERA});
			}
		});
		mTvResult = (TextView)findViewById(R.id.tv_result);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == TestActivity.REQUEST_CODE){
			if (data == null){
				return;
			}
			String result = data.getStringExtra("result");
			if (result!=null){
				mTvResult.setText("scan result："+result);
			}else {
				mTvResult.setText("can`t receive result");
			}
		}
	}
}
