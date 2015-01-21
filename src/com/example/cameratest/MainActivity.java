package com.example.cameratest;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import com.example.cameratest.Encoder.EncoderCallback;
import com.example.cameratest.Encoder.VIDEO_AVCLEVELTYPE;
import com.example.cameratest.Encoder.VIDEO_AVCPROFILETYPE;

public class MainActivity extends Activity implements Callback {

	private static final String TAG = "MainActivity";
	private SurfaceView mSurfaceView = null; //surfaceview对象,视频显示
	private SurfaceHolder mSurfaceHolder = null; //surfaceholder对象,surfaceview支持类
	private Camera mCamera;  //Camera对象
	private boolean isPreview;
	private PreviewCallback mJpegPreviewCallback;
	private Encoder encoder = new Encoder(false);
	private int level = 0x800;
	private int refFrames = 1;
	private int bitrate = 5000000; // 比特率
	private int sliceHeight = 0;
	private EncoderCallback encoderCallback;
	private SurfaceHolder holder;
	private boolean isStart;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		// 设置像素格式透明
		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		setContentView(R.layout.activity_main);
		initSurfaceView();
		init();
	}

	private void init() {
		encoder.setParament(1280, 720, 30, bitrate, sliceHeight);
		Encoder.VIDEO_AVCPROFILETYPE fileType = VIDEO_AVCPROFILETYPE.VIDEO_AVCProfileBaseline;
		Encoder.VIDEO_AVCLEVELTYPE levType = VIDEO_AVCLEVELTYPE.VIDEO_AVCLevel4;
		encoder.config(fileType, levType, refFrames);

		
		// decoder.start();
		encoder.setEncoderCallback(encoderCallback);
	}

	private void initSurfaceView() {
		mSurfaceView = (SurfaceView) findViewById(R.id.Surfaceview);
		mSurfaceHolder = mSurfaceView.getHolder();  //绑定surfaceview,取得surfaceview对象
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mCamera = Camera.open();
		try {
			mCamera.setPreviewDisplay(mSurfaceHolder);
		} catch (Exception e) {
			if(null != mCamera){
				mCamera.release();
				mCamera = null;
			}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.i(TAG, "SurfaceHolder.Callback：Surface Changed");
		initCamera();
	}

	

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i(TAG, "SurfaceHolder.Callback：Surface Destroyed");
		if(null != mCamera){
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			isPreview = false;
			mCamera.release();
			mCamera = null;
		}
	}

	private void initCamera() {
		Log.i(TAG, "going into initCamera");
		if(isPreview){
			mCamera.stopPreview();
		}
		
		if(null != mCamera){
			try {
				Camera.Parameters parameters = mCamera.getParameters();
				parameters.setPictureFormat(PixelFormat.JPEG);
				parameters.setPreviewFormat(PixelFormat.YCbCr_420_SP);
				parameters.setPreviewSize(1920, 720);
				
				if(this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE){
					parameters.set("orientation", "portrait");
					parameters.set("rotation", 90);
					mCamera.setDisplayOrientation(90);
					
				}else {
					parameters.set("orientation", "landscape");
					mCamera.setDisplayOrientation(0);
					
				}
				mCamera.setPreviewCallback(mJpegPreviewCallback);
				mCamera.setParameters(parameters);
				mCamera.startPreview();
				isPreview = true;
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		mJpegPreviewCallback = new Camera.PreviewCallback() {
			
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				try {
					long timestamp = System.currentTimeMillis();
					encoder.setBuffer(data, timestamp);
					if(!isStart){
						encoder.start();
						isStart = true;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
	}

}
