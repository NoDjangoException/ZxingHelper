package com.landi.zxinglibrary.exception;
/**
 * Created by Django .
 * description : 二维码解读失败
 */

public    class DecodeFailedException extends Exception   {
	public DecodeFailedException() {
	}

	public DecodeFailedException(String message) {
		super(message);
	}
}
