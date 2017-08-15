package com.landi.zxinglibrary.util;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by Django .
 * description : 
 */

public    class Decoder {

	private static final String TAG = "Decoder";

	public static final int BARCODE_MODE = 0X100;
	public static final int QR_CODE_MODE = 0X200;
	public static final int ALL_MODE = 0X300;

	private final MultiFormatReader multiFormatReader;
	private Handler mHandler;

	public Decoder(int decodeMode,Handler handler){
		Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);

		Collection<BarcodeFormat> decodeFormats = new ArrayList<>();
		decodeFormats.addAll(EnumSet.of(BarcodeFormat.AZTEC));
		decodeFormats.addAll(EnumSet.of(BarcodeFormat.PDF_417));

		switch (decodeMode) {
			case BARCODE_MODE:
				decodeFormats.addAll(DecodeFormatManager.getBarCodeFormats());
				break;

			case QR_CODE_MODE:
				decodeFormats.addAll(DecodeFormatManager.getQrCodeFormats());
				break;

			case ALL_MODE:
				decodeFormats.addAll(DecodeFormatManager.getBarCodeFormats());
				decodeFormats.addAll(DecodeFormatManager.getQrCodeFormats());
				break;

			default:
				break;
		}

		hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
		multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(hints);

		mHandler = handler;
	}


	/**
	 *  Decode the data within the viewfinder rectangle, and time how long it
	 * took. For efficiency, reuse the same reader objects from one decode to
	 * the next.
	 *
	 * @param data 图片数据
	 * @param width 原图片宽度
	 * @param height 原图片高度
	 * @param rect 扫描的区域 先默认扫描中间位置的图片
	 * @return
	 */
	public Result decode(byte[] data,int width,int height, Rect rect) {
		Log.d(TAG, "decode: data length is "+data.length+",width is "+width+",height is "+height);
		if (rect == null){
			int scanWidth = width/2;
			int scanHeight = height/2;
			int x = width/4;
			int y = height/3;
			rect = new Rect(x,y,x+scanWidth,y+scanHeight);
		}
		Log.d(TAG, "decoding rect is"+rect);
//		rect = new Rect(0,0,width,height);

		Result rawResult = null;
		PlanarYUVLuminanceSource source =
				new PlanarYUVLuminanceSource(data, width, height,rect.left,rect.top,rect.width(),rect.height(), false);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		try {
			rawResult = multiFormatReader.decodeWithState(bitmap);
		} catch (ReaderException re) {
			// continue
		} finally {
			multiFormatReader.reset();
		}
		return rawResult;
	}


	public Result decode(Bitmap bitmap) {
		//解析二维码
		MyLuminanceSource myLuminanceSource=new MyLuminanceSource(bitmap);
		Hashtable<DecodeHintType,String> settingMap=new Hashtable<>();
		settingMap.put(DecodeHintType.CHARACTER_SET,"UTF-8");
		BinaryBitmap binaryBitmap=new BinaryBitmap(new HybridBinarizer(myLuminanceSource));
		QRCodeReader qrCodeReader=new QRCodeReader();
		Result result= null;
		try {
			result = qrCodeReader.decode(binaryBitmap,settingMap);
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (ChecksumException e) {
			e.printStackTrace();
		} catch (FormatException e) {
			e.printStackTrace();
		}
		return result;
	}

	class MyLuminanceSource extends LuminanceSource{

		private byte bitmapPixels[];

		protected MyLuminanceSource(Bitmap bitmap) {
			super(bitmap.getWidth(), bitmap.getHeight());

			// 首先，要取得该图片的像素数组内容
			int[] data = new int[bitmap.getWidth() * bitmap.getHeight()];
			this.bitmapPixels = new byte[bitmap.getWidth() * bitmap.getHeight()];
			bitmap.getPixels(data, 0, getWidth(), 0, 0, getWidth(), getHeight());

			// 将int数组转换为byte数组，也就是取像素值中蓝色值部分作为辨析内容
			for (int i = 0; i < data.length; i++) {
				this.bitmapPixels[i] = (byte) data[i];
			}
		}

		@Override
		public byte[] getRow(int i, byte[] bytes) {
			// 这里要得到指定行的像素数据
			System.arraycopy(bitmapPixels, i * getWidth(), bytes, 0, getWidth());
			return bytes;
		}

		@Override
		public byte[] getMatrix() {
			// 返回我们生成好的像素数据
			return bitmapPixels;
		}
	}
}
