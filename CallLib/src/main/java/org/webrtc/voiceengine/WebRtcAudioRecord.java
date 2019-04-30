/*
 *  Copyright (c) 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc.voiceengine;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.hv.imlib.HoloMessage;

import org.greenrobot.eventbus.EventBus;
import org.webrtc.Logging;

import java.nio.ByteBuffer;


public class WebRtcAudioRecord {
  private static final boolean DEBUG = false;

  private static final String TAG = "WebRtcAudioRecord";

  // Default audio data format is PCM 16 bit per sample.
  // Guaranteed to be supported by all devices.
  private static final int BITS_PER_SAMPLE = 16;

  // Requested size of each recorded buffer provided to the client.
  private static final int CALLBACK_BUFFER_SIZE_MS = 10;

  // Average number of callbacks per second.
  private static final int BUFFERS_PER_SECOND = 1000 / CALLBACK_BUFFER_SIZE_MS;

  // We ask for a native buffer size of BUFFER_SIZE_FACTOR * (minimum required
  // buffer size). The extra space is allocated to guard against glitches under
  // high load.
  private static final int BUFFER_SIZE_FACTOR = 2;

  // The AudioRecordJavaThread is allowed to wait for successful call to join()
  // but the wait times out afther this amount of time.
  private static final long AUDIO_RECORD_THREAD_JOIN_TIMEOUT_MS = 2000;

  private final long nativeAudioRecord;
  private final Context context;

  private WebRtcAudioEffects effects = null;

  private ByteBuffer byteBuffer;

  //private AudioRecord audioRecord = null;
  //private AudioRecordThread audioThread = null;
  //private XAudioRecord mAudioRecord = null;

  private static volatile boolean microphoneMute = false;
  private byte[] emptyBytes;
  private static WebRtcAudioRecord mWebRtcAudioRecord;

  WebRtcAudioRecord(Context context, long nativeAudioRecord) {
    Logging.d(TAG, "ctor" + WebRtcAudioUtils.getThreadInfo());
    this.context = context;
    this.nativeAudioRecord = nativeAudioRecord;
    if (DEBUG) {
      WebRtcAudioUtils.logDeviceInfo(TAG);
    }
    mWebRtcAudioRecord = this;
//        effects = WebRtcAudioEffects.create();

//        mAudioMgr = XAudioRecordMgr.newInstance(context);
  }

  public static WebRtcAudioRecord getInstance() {
    return mWebRtcAudioRecord;
  }

  private boolean enableBuiltInAEC(boolean enable) {
    return false;
  }

  private boolean enableBuiltInNS(boolean enable) {
    Logging.d(TAG, "enableBuiltInNS(" + enable + ')');

    return false;
  }

  private int initRecording(int sampleRate, int channels) {

    Logging.d(TAG, "initRecording(sampleRate=" + sampleRate + ", channels=" + channels + ")");

    //if (mAudioRecord != null) {
    //  Logging.e(TAG, "InitRecording() called twice without StopRecording()");
    //  return -1;
    //}
    final int bytesPerFrame = channels * (BITS_PER_SAMPLE / 8);
    final int framesPerBuffer = sampleRate / BUFFERS_PER_SECOND;
    byteBuffer = ByteBuffer.allocateDirect(bytesPerFrame * framesPerBuffer);
    Logging.d(TAG, "byteBuffer.capacity: " + byteBuffer.capacity());
    emptyBytes = new byte[byteBuffer.capacity()];
    // Rather than passing the ByteBuffer with every callback (requiring
    // the potentially expensive GetDirectBufferAddress) we simply have the
    // the native class cache the address to the memory once.
    nativeCacheDirectBufferAddress(byteBuffer, nativeAudioRecord);

    // Get the minimum buffer size required for the successful creation of
    // an AudioRecord object, in byte units.
    // Note that this size doesn't guarantee a smooth recording under load.
    /*
    int minBufferSize = mAudioMgr.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
      Logging.e(TAG, "AudioRecord.getMinBufferSize failed: " + minBufferSize);
      return -1;
    }
    Logging.d(TAG, "AudioRecord.getMinBufferSize: " + minBufferSize);
    */
    // Use a larger buffer size than the minimum required when creating the
    // AudioRecord instance to ensure smooth recording under load. It has been
    // verified that it does not increase the actual recording latency.
    //int bufferSizeInBytes = Math.max(BUFFER_SIZE_FACTOR * minBufferSize, byteBuffer.capacity());
    //Logging.d(TAG, "bufferSizeInBytes: " + bufferSizeInBytes);
    /*
    try {
      mAudioRecord = new XAudioRecord(AudioSource.VOICE_COMMUNICATION, sampleRate,
              AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);
    } catch (IllegalArgumentException e) {
      Logging.e(TAG, e.getMessage());
      return -1;
    }

    if (mAudioRecord == null || mAudioRecord.getState() != mAudioRecord.STATE_INITIALIZED) {
      Logging.e(TAG, "Failed to create a new AudioRecord instance");
      return -1;
    }

    Logging.d(TAG, "AudioRecord "
            + "session ID: " + mAudioRecord.getAudioSessionId() + ", "
            + "audio format: " + mAudioRecord.getAudioFormat() + ", "
            + "channels: " + mAudioRecord.getChannelCount() + ", "
            + "sample rate: " + mAudioRecord.getSampleRate());
    */
    // TODO(phoglund): put back audioRecord.getBufferSizeInFrames when
    // all known downstream users supports M.
    // if (WebRtcAudioUtils.runningOnMOrHigher()) {
    // Returns the frame count of the native AudioRecord buffer. This is
    // greater than or equal to the bufferSizeInBytes converted to frame
    // units. The native frame count may be enlarged to accommodate the
    // requirements of the source on creation or if the AudioRecord is
    // subsequently rerouted.

    // Logging.d(TAG, "bufferSizeInFrames: "
    //     + audioRecord.getBufferSizeInFrames());
    //}
    return framesPerBuffer;
  }

  private boolean startRecording() {
    Logging.d(TAG, "startRecording");
//        assertTrue(mAudioMgr != null);
//        //assertTrue(mAudioRecord != null);
//        if (mAudioMgr.isAudioRecorded() == false) {
//            mAudioMgr.Subscribe(this);
//            mAudioMgr.startRecording();
//        } else {
//            mAudioMgr.Subscribe(this);
//        }


    HandlerThread audioThread = new HandlerThread("AudioThread");
    audioThread.start();
    handler = new Handler(audioThread.getLooper()) {
      @Override
      public void handleMessage(Message msg) {
        super.handleMessage(msg);
        byte[] audioData = (byte[]) msg.obj;
        if (microphoneMute) {
          byteBuffer.clear();
          byteBuffer.put(emptyBytes);
        } else {
          byteBuffer.clear();
          byteBuffer.put(audioData);
        }
        nativeDataIsRecorded(byteBuffer.capacity(), nativeAudioRecord);
      }
    };


    HoloMessage message = new HoloMessage();
    message.setAction("api.audio.subscribe");
    EventBus.getDefault().post(message);
    return true;
  }

  Handler handler;

  private boolean stopRecording() {
    Logging.d(TAG, "stopRecording");

//        assertTrue(mAudioMgr != null);
//        //assertTrue(mAudioRecord != null);
//        if (mAudioMgr.isAudioRecorded() == true) {
//            //mAudioMgr.stopRecording();
//            mAudioMgr.UnSubscribe(this);
//        }

    HoloMessage message = new HoloMessage();
    message.setAction("api.audio.unsubscribe");
    EventBus.getDefault().post(message);
    return true;
  }

  // Helper method which throws an exception  when an assertion has failed.
  private static void assertTrue(boolean condition) {
    if (!condition) {
      throw new AssertionError("Expected condition to be true");
    }
  }

  private native void nativeCacheDirectBufferAddress(ByteBuffer byteBuffer, long nativeAudioRecord);

  private native void nativeDataIsRecorded(int bytes, long nativeAudioRecord);

  // Sets all recorded samples to zero if |mute| is true, i.e., ensures that
  // the microphone is muted.
  public static void setMicrophoneMute(boolean mute) {
    Logging.w(TAG, "setMicrophoneMute(" + mute + ")");
    microphoneMute = mute;
  }

//    @Override
//    public void onAudioData(byte[] audioData) {
//        Logging.d(TAG, "onAudioData");
//
//        //byteBuffer.put(audioData);
//        if (microphoneMute) {
//            byteBuffer.clear();
//            byteBuffer.put(emptyBytes);
//        } else {
//            byteBuffer.clear();
//            byteBuffer.put(audioData);
//        }
//
//        //int cnt = byteBuffer.capacity();
//        //byte[] bytes = new byte[cnt];
//        //byteBuffer.get(bytes);
//        //byteBuffer.get(bytes,0,10);
//        nativeDataIsRecorded(byteBuffer.capacity(), nativeAudioRecord);
//    }

  public void onAudioData(byte[] audioData) {
//    Logging.d(TAG, "onAudioData");
//        int bytesRead = audioRecord.read(byteBuffer, byteBuffer.capacity());
    Message message = new Message();
    message.obj = audioData;

    handler.sendMessage(message);

    //byteBuffer.put(audioData);

  }


}
