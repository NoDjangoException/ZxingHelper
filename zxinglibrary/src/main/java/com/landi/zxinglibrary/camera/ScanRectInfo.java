package com.landi.zxinglibrary.camera;
/**
 * Created by Django .
 * description : 以竖屏的形式保存被扫描区域所在预览区域的信息，包括左上角所处的位置，和宽度比例，长度比例
 */

public    class ScanRectInfo   {

	float xRatio;
	float yRatio;
	float widthRatio;
	float heightRatio;

	public ScanRectInfo(float xRatio, float yRatio, float widthRatio, float heightRatio) {
		this.xRatio = xRatio;
		this.yRatio = yRatio;
		this.widthRatio = widthRatio;
		this.heightRatio = heightRatio;
	}

	public void changeToHorizontal(){
		float temp;
		temp = xRatio;
		xRatio = yRatio;
		yRatio = temp;

		temp = widthRatio;
		widthRatio = heightRatio;
		heightRatio =temp;
	}


	@Override
	public String toString() {
		return "ScanRectInfo{" +
				"xRatio=" + xRatio +
				", yRatio=" + yRatio +
				", widthRatio=" + widthRatio +
				", heightRatio=" + heightRatio +
				'}';
	}
}
