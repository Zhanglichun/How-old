package com.example.face.utils;

import java.io.ByteArrayOutputStream;

import org.apache.http.HttpRequest;
import org.json.JSONObject;

import com.example.face.constant.Constant;
import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import android.graphics.Bitmap;
import android.util.Log;

public class FaceDetect {

	public interface CallBack{
		void success(JSONObject result);
		
		void fail(FaceppParseException exception);
	}
	
	public static void detect(final Bitmap bm, final CallBack callBack){
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				try {
					//¥¥Ω®«Î«Û
					HttpRequests requests = new HttpRequests(Constant.KEY, Constant.SECRET, true, true);
					
					//—πÀı
					Bitmap bmSmall = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight());
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					bmSmall.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
					
					byte[] array = outputStream.toByteArray();
					
					PostParameters parameters = new PostParameters();
					parameters.setImg(array);
					
					JSONObject object = requests.detectionDetect(parameters);
					
					Log.e("Tag", object.toString());
					
					if (callBack != null) {
						callBack.success(object);
					}
					
				} catch (FaceppParseException e) {
					
					if (callBack!=null) {
						callBack.fail(e);
					}
				}
			}
		}).start();
	}
}
