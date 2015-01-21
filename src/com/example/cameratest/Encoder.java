/*
 * Copyright (c) 2010, Texas Instruments Incorporated
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * *  Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * *  Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * *  Neither the name of Texas Instruments Incorporated nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.example.cameratest;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class Encoder {
	public enum VIDEO_AVCPROFILETYPE {
    	VIDEO_AVCProfileBaseline(0x01),   /**< Baseline profile */
    	VIDEO_AVCProfileMain(0x02),   /**< Main profile */
    	VIDEO_AVCProfileExtended(0x04),   /**< Extended profile */
    	VIDEO_AVCProfileHigh(0x08),   /**< High profile */
    	VIDEO_AVCProfileHigh10(0x10),   /**< High 10 profile */
    	VIDEO_AVCProfileHigh422(0x20),   /**< High 4:2:2 profile */
    	VIDEO_AVCProfileHigh444(0x40),   /**< High 4:4:4 profile */
    	VIDEO_AVCProfileKhronosExtensions(0x6F000000), /**< Reserved region for introducing Khronos Standard Extensions */ 
    	VIDEO_AVCProfileVendorStartUnused(0x7F000000), /**< Reserved region for introducing Vendor Extensions */
    	VIDEO_AVCProfileMax(0x7FFFFFFF);  
		
		private int value;
		private VIDEO_AVCPROFILETYPE (int value) {
			this.value = value;
		}
		public int value() {
			return this.value;
		}
	}
	
	public enum VIDEO_AVCLEVELTYPE {
    	VIDEO_AVCLevel1(0x01),     /**< Level 1 */
    	VIDEO_AVCLevel1b(0x02),     /**< Level 1b */
    	VIDEO_AVCLevel11(0x04),     /**< Level 1.1 */
    	VIDEO_AVCLevel12(0x08),     /**< Level 1.2 */
     	VIDEO_AVCLevel13(0x10),     /**< Level 1.3 */
    	VIDEO_AVCLevel2(0x20),     /**< Level 2 */
    	VIDEO_AVCLevel21(0x40),     /**< Level 2.1 */
    	VIDEO_AVCLevel22(0x80),     /**< Level 2.2 */
    	VIDEO_AVCLevel3(0x100),    /**< Level 3 */
    	VIDEO_AVCLevel31(0x200),    /**< Level 3.1 */
    	VIDEO_AVCLevel32(0x400),    /**< Level 3.2 */
    	VIDEO_AVCLevel4(0x800),    /**< Level 4 */
    	VIDEO_AVCLevel41(0x1000),   /**< Level 4.1 */
    	VIDEO_AVCLevel42(0x2000),   /**< Level 4.2 */
    	VIDEO_AVCLevel5(0x4000),   /**< Level 5 */
    	VIDEO_AVCLevel51(0x8000),   /**< Level 5.1 */
    	VIDEO_AVCLevelKhronosExtensions(0x6F000000), /**< Reserved region for introducing Khronos Standard Extensions */ 
    	VIDEO_AVCLevelVendorStartUnused(0x7F000000), /**< Reserved region for introducing Vendor Extensions */
    	VIDEO_AVCLevelMax(0x7FFFFFFF); 
		
		private int value;
		private VIDEO_AVCLEVELTYPE (int value) {
			this.value = value;
		}
		public int value() {
			return this.value;
		}
	} 

	static {
        /*
         * Load the library.  If it's already loaded, this does nothing.
         */
        System.loadLibrary("videotest_jni");
    }

    private static final String TAG = "EncoderTest";

    private int mEncoderContext; // accessed by native methods
    private EventHandler mEventHandler;
	private static EncoderCallback mEncoderCallback;
	private static ArrayList<String> mPayloadQueue;
	private static ArrayList<Long> mTimeQueue;
	
    Encoder(boolean isdecoder) {
        Looper looper;
        if ((looper = Looper.myLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else {
            mEventHandler = null;
        }
        mEncoderCallback = null;

        native_setup(new WeakReference<Encoder>(this), isdecoder);
    }
	
    protected void finalize() {
        release();
    }

    public final void release() {
        native_release();
    }

	public final void setEncoderCallback(EncoderCallback cb) {
		   mEncoderCallback = cb;
	}
	
	private static void postEventFromNative(Object encoder_native, byte[] buffer, int len, long timestamp) 
	{
		Encoder ec = (Encoder)((WeakReference)encoder_native).get();
		if (ec == null) {
			return;
		}
		EncoderCallback eCb = mEncoderCallback;
		eCb.onEncoderFrame((byte[])buffer, len, timestamp);
	}
	
	private native final void native_setup(Object encode_this, boolean isdecoder);
    private native final void native_release();
	public native final void setParament(int width, int height, int framerate, int bitrate, int sliceHeight);
	public native final void config(Object profile, Object level, int refFrames);
	public native final void setBuffer(byte[] buffer, long timestamp);
    public native final void unlock();
    public native final void lock();
    public native final void start();
    public native final void stop();

    private class EventHandler extends Handler
    {
        private Encoder mEncoderTest;

        public EventHandler(Encoder c, Looper looper) {
            super(looper);
            mEncoderTest = c;
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
              default:
                Log.e(TAG, "Unknown message type " + msg.what);
                return;
            }
        }
    }
	
	public interface EncoderCallback
	{
		 void onEncoderFrame(byte[] data, int length, long timestamp);
	}	
}
