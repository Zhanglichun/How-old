package com.example.face;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.face.utils.FaceDetect;
import com.example.face.utils.FaceDetect.CallBack;
import com.facepp.error.FaceppParseException;

import android.R.integer;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener{

	private ImageView photo;
	private Button get;
	private Button detect;
	private TextView tip;
	private FrameLayout waiting;
	
	private String currentPhotoString;
	private Bitmap mPhotoImage;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		
		initViews();
		initEvent();
		paint = new Paint();
	}
	
	private void initEvent() {
		// TODO Auto-generated method stub
		get.setOnClickListener(this);
		detect.setOnClickListener(this);
	}
	
	private void initViews() {
		
		photo = (ImageView) findViewById(R.id.image);
		get = (Button) findViewById(R.id.get);
		detect = (Button) findViewById(R.id.detect);
		tip = (TextView) findViewById(R.id.tip);	
		waiting = (FrameLayout)findViewById(R.id.waiting);
	}
	
	private static final int MSG_SUCC = 0;
	private static final int MSG_FAIL = 1;
	
	private Handler handler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_SUCC:
				waiting.setVisibility(View.GONE);
				
				JSONObject object = (JSONObject) msg.obj;
				
				prepareResult(object);

				photo.setImageBitmap(mPhotoImage);
				break;
				
			case MSG_FAIL:
				waiting.setVisibility(View.GONE);
				String errorString = (String) msg.obj;
				if (TextUtils.isEmpty(errorString)) {
					tip.setText("Error");
				}else{
					tip.setText(errorString);
				}
				break;

			default:
				break;
			}
		}
	};
	private Paint paint;
	
	private void prepareResult(JSONObject object) {
		
		Bitmap bitmap = Bitmap.createBitmap(mPhotoImage.getWidth(), mPhotoImage.getHeight(), mPhotoImage.getConfig());
		Canvas canvas = new Canvas(bitmap);
		
		canvas.drawBitmap(mPhotoImage, 0, 0, null);
		
		try {
			JSONArray faces = object.getJSONArray("face");
			
			int faceCount = faces.length();
			tip.setText("Find:"+faceCount);
			
			for (int i = 0; i < faceCount; i++) {
				
				JSONObject face = faces.getJSONObject(i);
				JSONObject position = face.getJSONObject("position");
				
				
				float x = (float) position.getJSONObject("center").getDouble("x");
				float y = (float) position.getJSONObject("center").getDouble("y");
				
				float w = (float) position.getDouble("width");
				float h = (float) position.getDouble("height");
				
				Log.e("DEBUG", x + "--" + y + "--" + w + "--" + h);
				
				x = x/100 * bitmap.getWidth();
				y = y/100 * bitmap.getHeight();
				
				w = w/100 * bitmap.getWidth();
				h = h/100 * bitmap.getHeight();
				
				paint.setColor(Color.WHITE);
				paint.setStrokeWidth(3);
				
				canvas.drawLine(x - w/2, y - h/2, x - w/2, y + h/2, paint);
				canvas.drawLine(x - w/2, y - h/2, x + w/2, y - h/2, paint);	
				canvas.drawLine(x + w/2, y - h/2, x + w/2, y + h/2, paint);
				canvas.drawLine(x - w/2, y + h/2, x + w/2, y + h/2, paint);
				
				int age = face.getJSONObject("attribute").getJSONObject("age").getInt("value");
				String gender = face.getJSONObject("attribute").getJSONObject("gender").getString("value");
				
				Bitmap ageBitmap = buildAgeBitmap(age, "Male".equals(gender));
				
				int ageWidth = ageBitmap.getWidth();
				int ageHeight = ageBitmap.getHeight();
				
				if (bitmap.getWidth() < photo.getWidth() && bitmap.getHeight() < photo.getHeight()) {
					float ratio = Math.max(bitmap.getWidth()*1.0f/photo.getWidth(), bitmap.getHeight()*1.0f/photo.getHeight());
					ageBitmap = Bitmap.createScaledBitmap(ageBitmap, (int)(ageWidth*ratio), (int)(ageHeight*ratio), false);
				}
				
				canvas.drawBitmap(ageBitmap, x - ageBitmap.getWidth()/2, y - h/2 - ageBitmap.getHeight()/2-50, null);
				
				mPhotoImage = bitmap;

			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	};
	
	private Bitmap buildAgeBitmap(int age, boolean isMale) {
		
		TextView tv = (TextView) waiting.findViewById(R.id.ageandgender);
		tv.setText(age+"");
		if (isMale) {
			//tv.setCompoundDrawables(getResources().getDrawable(R.drawable.male), null, null, null);
			tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.male, 0, 0, 0);
		}else{ 
			//tv.setCompoundDrawables(getResources().getDrawable(R.drawable.female), null, null, null);
			tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.female, 0, 0, 0);
		}
		
		tv.setDrawingCacheEnabled(true);
		Bitmap bitmap = Bitmap.createBitmap(tv.getDrawingCache());
		tv.destroyDrawingCache();
		
		return bitmap;
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		switch (arg0.getId()) {
		case R.id.get:
			Intent intent = new Intent(Intent.ACTION_PICK);
			intent.setType("image/*");
			startActivityForResult(intent, 0);
			break;

		case R.id.detect:
			
			waiting.setVisibility(View.VISIBLE);
			
			if (currentPhotoString != null && !currentPhotoString.trim().equals("")) {
				reSizePhoto();
			}else{
				mPhotoImage = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
			}
			
			FaceDetect.detect(mPhotoImage, new CallBack() {
					
				@Override
				public void success(JSONObject result) {
					// TODO Auto-generated method stub
					Message msg = Message.obtain();
					msg.what = MSG_SUCC;
					msg.obj = result;
					handler.sendMessage(msg);
				}
				
				@Override
				public void fail(FaceppParseException exception) {
					// TODO Auto-generated method stub
					Message msg = Message.obtain();
					msg.what = MSG_FAIL;
					msg.obj = exception.getErrorMessage();
					handler.sendMessage(msg);
				}
			});
			
			break;

		default:
			break;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if (requestCode == 0) {
			if (data != null) {
				Uri uri = data.getData();
				Cursor cursor = getContentResolver().query(uri, null, null, null, null);
				cursor.moveToFirst();
				int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
				currentPhotoString = cursor.getString(index);
				cursor.close();
				
				reSizePhoto();
				
				photo.setImageBitmap(mPhotoImage);
				tip.setText("Click Detect -->");
			}
			super.onActivityResult(requestCode, resultCode, data);
		}else if(requestCode == 1){
			
			mPhotoImage = (Bitmap) data.getExtras().get("data");
			photo.setImageBitmap(mPhotoImage);
			tip.setText("Click Detect -->");
			
		}
	}

	private void reSizePhoto() {
		// TODO Auto-generated method stub
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		
		BitmapFactory.decodeFile(currentPhotoString, options);
		
		double ratio = Math.max(options.outWidth * 1.0d /1024f, options.outHeight * 1.0d /1024f);
		options.inSampleSize = (int) Math.ceil(ratio);
		options.inJustDecodeBounds = false;
		
		mPhotoImage = BitmapFactory.decodeFile(currentPhotoString, options);
	}
}
