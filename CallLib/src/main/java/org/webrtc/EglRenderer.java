/*
 *  Copyright 2016 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.Surface;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Implements org.webrtc.VideoRenderer.Callbacks by displaying the video stream on an EGL Surface.
 * This class is intended to be used as a helper class for rendering on SurfaceViews and
 * TextureViews.
 */
public class EglRenderer implements VideoRenderer.Callbacks {
  protected static final String TAG = "EglRenderer";
  protected static final long LOG_INTERVAL_SEC = 4;
  protected static final int MAX_SURFACE_CLEAR_COUNT = 3;

  protected class EglSurfaceCreation implements Runnable {
    private Object surface;

    public synchronized void setSurface(Object surface) {
      this.surface = surface;
    }

    @Override
    public synchronized void run() {
      if (surface != null && eglBase != null && !eglBase.hasSurface()) {
        if (surface instanceof Surface) {
          eglBase.createSurface((Surface) surface);
        } else if (surface instanceof SurfaceTexture) {
          eglBase.createSurface((SurfaceTexture) surface);
        } else {
          throw new IllegalStateException("Invalid surface: " + surface);
        }
        eglBase.makeCurrent();
        // Necessary for YUV frames with odd width.
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
      }
    }
  }

  protected String name;

  // |renderThreadHandler| is a handler for communicating with |renderThread|, and is synchronized
  // on |handlerLock|.
  protected final Object handlerLock = new Object();
  protected Handler renderThreadHandler;

  // Variables for fps reduction.
  protected final Object fpsReductionLock = new Object();
  // Time for when next frame should be rendered.
  protected long nextFrameTimeNs;
  // Minimum duration between frames when fps reduction is active, or -1 if video is completely
  // paused.
  protected long minRenderPeriodNs;

  // EGL and GL resources for drawing YUV/OES textures. After initilization, these are only accessed
  // from the render thread.
  protected EglBase eglBase;
  protected final RendererCommon.YuvUploader yuvUploader = new RendererCommon.YuvUploader();
  protected RendererCommon.GlDrawer drawer;
  // Texture ids for YUV frames. Allocated on first arrival of a YUV frame.
  protected int[] yuvTextures = null;

  // Pending frame to render. Serves as a queue with size 1. Synchronized on |frameLock|.
  protected final Object frameLock = new Object();
  protected VideoRenderer.I420Frame pendingFrame;

  // These variables are synchronized on |layoutLock|.
  protected final Object layoutLock = new Object();
  protected int surfaceWidth;
  protected int surfaceHeight;
  protected float layoutAspectRatio;
  // If true, mirrors the video stream horizontally.
  protected boolean mirror;

  // These variables are synchronized on |statisticsLock|.
  protected final Object statisticsLock = new Object();
  // Total number of video frames received in renderFrame() call.
  protected int framesReceived;
  // Number of video frames dropped by renderFrame() because previous frame has not been rendered
  // yet.
  protected int framesDropped;
  // Number of rendered video frames.
  protected int framesRendered;
  // Start time for counting these statistics, or 0 if we haven't started measuring yet.
  protected long statisticsStartTimeNs;
  // Time in ns spent in renderFrameOnRenderThread() function.
  protected long renderTimeNs;
  // Time in ns spent by the render thread in the swapBuffers() function.
  protected long renderSwapBufferTimeNs;

  // Runnable for posting frames to render thread.
  protected final Runnable renderFrameRunnable = new Runnable() {
    @Override
    public void run() {
      renderFrameOnRenderThread();
    }
  };

  protected final Runnable logStatisticsRunnable = new Runnable() {
    @Override
    public void run() {
      logStatistics();
      synchronized (handlerLock) {
        if (renderThreadHandler != null) {
          renderThreadHandler.removeCallbacks(logStatisticsRunnable);
          renderThreadHandler.postDelayed(
              logStatisticsRunnable, TimeUnit.SECONDS.toMillis(LOG_INTERVAL_SEC));
        }
      }
    }
  };

  protected final EglSurfaceCreation eglSurfaceCreationRunnable = new EglSurfaceCreation();

  /**
   * Standard constructor. The name will be used for the render thread name and included when
   * logging. In order to render something, you must first call init() and createEglSurface.
   */
  public EglRenderer(String name) {
    this.name = name;
  }

  public EglRenderer() {

  }

  public void SetResourceName( final String name ) {
    this.name = name;
  }

  /**
   * Initialize this class, sharing resources with |sharedContext|. The custom |drawer| will be used
   * for drawing frames on the EGLSurface. This class is responsible for calling release() on
   * |drawer|. It is allowed to call init() to reinitialize the renderer after a previous
   * init()/release() cycle.
   */
  public void init(final EglBase.Context sharedContext, final int[] configAttributes,
      RendererCommon.GlDrawer drawer) {
    synchronized (handlerLock) {
      if (renderThreadHandler != null) {
        throw new IllegalStateException(name + "Already initialized");
      }
      logD("Initializing EglRenderer");
      this.drawer = drawer;

      final HandlerThread renderThread = new HandlerThread(name + "EglRenderer");
      renderThread.start();
      renderThreadHandler = new Handler(renderThread.getLooper());
      // Create EGL context on the newly created render thread. It should be possibly to create the
      // context on this thread and make it current on the render thread, but this causes failure on
      // some Marvel based JB devices. https://bugs.chromium.org/p/webrtc/issues/detail?id=6350.
      ThreadUtils.invokeAtFrontUninterruptibly(renderThreadHandler, new Runnable() {
        @Override
        public void run() {
          // If sharedContext is null, then texture frames are disabled. This is typically for old
          // devices that might not be fully spec compliant, so force EGL 1.0 since EGL 1.4 has
          // caused trouble on some weird devices.
          if (sharedContext == null) {
            logD("EglBase10.create context");
            eglBase = new EglBase10(null /* sharedContext */, configAttributes);
          } else {
            logD("EglBase.create shared context");
            eglBase = EglBase.create(sharedContext, configAttributes);
          }
        }
      });
      renderThreadHandler.post(eglSurfaceCreationRunnable);
      final long currentTimeNs = System.nanoTime();
      resetStatistics(currentTimeNs);
      renderThreadHandler.postDelayed(
          logStatisticsRunnable, TimeUnit.SECONDS.toMillis(LOG_INTERVAL_SEC));
    }
  }

  public void createEglSurface(Surface surface) {
    createEglSurfaceInternal(surface);
  }

  public void createEglSurface(SurfaceTexture surfaceTexture) {
    createEglSurfaceInternal(surfaceTexture);
  }

  protected void createEglSurfaceInternal(Object surface) {
    eglSurfaceCreationRunnable.setSurface(surface);
    postToRenderThread(eglSurfaceCreationRunnable);
  }

  /**
   * Block until any pending frame is returned and all GL resources released, even if an interrupt
   * occurs. If an interrupt occurs during release(), the interrupt flag will be set. This function
   * should be called before the Activity is destroyed and the EGLContext is still valid. If you
   * don't call this function, the GL resources might leak.
   */
  public void release() {
    logD("Releasing.");
    final CountDownLatch eglCleanupBarrier = new CountDownLatch(1);
    synchronized (handlerLock) {
      if (renderThreadHandler == null) {
        logD("Already released");
        return;
      }
      renderThreadHandler.removeCallbacks(logStatisticsRunnable);
      // Release EGL and GL resources on render thread.
      renderThreadHandler.postAtFrontOfQueue(new Runnable() {
        @Override
        public void run() {
          if (drawer != null) {
            drawer.release();
            drawer = null;
          }
          if (yuvTextures != null) {
            GLES20.glDeleteTextures(3, yuvTextures, 0);
            yuvTextures = null;
          }
          if (eglBase != null) {
            logD("eglBase detach and release.");
            eglBase.detachCurrent();
            eglBase.release();
            eglBase = null;
          }
          eglCleanupBarrier.countDown();
        }
      });
      final Looper renderLooper = renderThreadHandler.getLooper();
      // TODO(magjed): Replace this post() with renderLooper.quitSafely() when API support >= 18.
      renderThreadHandler.post(new Runnable() {
        @Override
        public void run() {
          logD("Quitting render thread.");
          renderLooper.quit();
        }
      });
      // Don't accept any more frames or messages to the render thread.
      renderThreadHandler = null;
    }
    // Make sure the EGL/GL cleanup posted above is executed.
    ThreadUtils.awaitUninterruptibly(eglCleanupBarrier);
    synchronized (frameLock) {
      if (pendingFrame != null) {
        VideoRenderer.renderFrameDone(pendingFrame);
        pendingFrame = null;
      }
    }
    logD("Releasing done.");
  }

  /**
   * Reset the statistics logged in logStatistics().
   */
  protected void resetStatistics(long currentTimeNs) {
    synchronized (statisticsLock) {
      statisticsStartTimeNs = currentTimeNs;
      framesReceived = 0;
      framesDropped = 0;
      framesRendered = 0;
      renderTimeNs = 0;
      renderSwapBufferTimeNs = 0;
    }
  }

  public void printStackTrace() {
    synchronized (handlerLock) {
      final Thread renderThread =
          (renderThreadHandler == null) ? null : renderThreadHandler.getLooper().getThread();
      if (renderThread != null) {
        final StackTraceElement[] renderStackTrace = renderThread.getStackTrace();
        if (renderStackTrace.length > 0) {
          logD("EglRenderer stack trace:");
          for (StackTraceElement traceElem : renderStackTrace) {
            logD(traceElem.toString());
          }
        }
      }
    }
  }

  /**
   * Set if the video stream should be mirrored or not.
   */
  public void setMirror(final boolean mirror) {
    logD("setMirror: " + mirror);
    synchronized (layoutLock) {
      this.mirror = mirror;
    }
  }

  /**
   * Set layout aspect ratio. This is used to crop frames when rendering to avoid stretched video.
   * Set this to 0 to disable cropping.
   */
  public void setLayoutAspectRatio(float layoutAspectRatio) {
    logD("setLayoutAspectRatio: " + layoutAspectRatio);
    synchronized (layoutLock) {
      this.layoutAspectRatio = layoutAspectRatio;
    }
  }

  /**
   * Limit render framerate.
   *
   * @param fps Limit render framerate to this value, or use Float.POSITIVE_INFINITY to disable fps
   *            reduction.
   */
  public void setFpsReduction(float fps) {
    logD("setFpsReduction: " + fps);
    synchronized (fpsReductionLock) {
      final long previousRenderPeriodNs = minRenderPeriodNs;
      if (fps <= 0) {
        minRenderPeriodNs = Long.MAX_VALUE;
      } else {
        minRenderPeriodNs = (long) (TimeUnit.SECONDS.toNanos(1) / fps);
      }
      if (minRenderPeriodNs != previousRenderPeriodNs) {
        // Fps reduction changed - reset frame time.
        nextFrameTimeNs = System.nanoTime();
      }
    }
  }

  public void disableFpsReduction() {
    setFpsReduction(Float.POSITIVE_INFINITY /* fps */);
  }

  public void pauseVideo() {
    setFpsReduction(0 /* fps */);
  }

  // VideoRenderer.Callbacks interface.
  @Override
  public void renderFrame(VideoRenderer.I420Frame frame) {
    synchronized (statisticsLock) {
      ++framesReceived;
    }
    final boolean dropOldFrame;
    synchronized (handlerLock) {
      if (renderThreadHandler == null) {
        logD("Dropping frame - Not initialized or already released.");
        VideoRenderer.renderFrameDone(frame);
        return;
      }
      // Check if fps reduction is active.
      synchronized (fpsReductionLock) {
        if (minRenderPeriodNs > 0) {
          final long currentTimeNs = System.nanoTime();
          if (currentTimeNs < nextFrameTimeNs) {
            logD("Dropping frame - fps reduction is active.");
            VideoRenderer.renderFrameDone(frame);
            return;
          }
          nextFrameTimeNs += minRenderPeriodNs;
          // The time for the next frame should always be in the future.
          nextFrameTimeNs = Math.max(nextFrameTimeNs, currentTimeNs);
        }
      }
      synchronized (frameLock) {
        dropOldFrame = (pendingFrame != null);
        if (dropOldFrame) {
          VideoRenderer.renderFrameDone(pendingFrame);
        }
        pendingFrame = frame;
        renderThreadHandler.post(renderFrameRunnable);
      }
    }
    if (dropOldFrame) {
      synchronized (statisticsLock) {
        ++framesDropped;
      }
    }
  }

  /**
   * Release EGL surface. This function will block until the EGL surface is released.
   */
  public void releaseEglSurface() {
    // Ensure that the render thread is no longer touching the Surface before returning from this
    // function.
    eglSurfaceCreationRunnable.setSurface(null /* surface */);
    synchronized (handlerLock) {
      if (renderThreadHandler != null) {
        renderThreadHandler.removeCallbacks(eglSurfaceCreationRunnable);
        ThreadUtils.invokeAtFrontUninterruptibly(renderThreadHandler, new Runnable() {
          @Override
          public void run() {
            if (eglBase != null) {
              eglBase.detachCurrent();
              eglBase.releaseSurface();
            }
          }
        });
      }
    }
  }

  /**
   * Notify that the surface size has changed.
   */
  public void surfaceSizeChanged(int surfaceWidth, int surfaceHeight) {
    logD("Surface size changed: " + surfaceWidth + "x" + surfaceHeight);
    synchronized (layoutLock) {
      this.surfaceWidth = surfaceWidth;
      this.surfaceHeight = surfaceHeight;
    }
  }

  /**
   * Private helper function to post tasks safely.
   */
  protected void postToRenderThread(Runnable runnable) {
    synchronized (handlerLock) {
      if (renderThreadHandler != null) {
        renderThreadHandler.post(runnable);
      }
    }
  }

  protected void clearSurfaceOnRenderThread() {
    if (eglBase != null && eglBase.hasSurface()) {
      logD("clearSurface");
      GLES20.glClearColor(0 /* red */, 0 /* green */, 0 /* blue */, 0 /* alpha */);
      GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
      eglBase.swapBuffers();
    }
  }

  /**
   * Post a task to clear the TextureView to a transparent uniform color.
   */
  public void clearImage() {
    synchronized (handlerLock) {
      if (renderThreadHandler == null) {
        return;
      }
      renderThreadHandler.postAtFrontOfQueue(new Runnable() {
        @Override
        public void run() {
          clearSurfaceOnRenderThread();
        }
      });
    }
  }

  /**
   * Renders and releases |pendingFrame|.
   */
  protected void renderFrameOnRenderThread() {
    // Fetch and render |pendingFrame|.
    final VideoRenderer.I420Frame frame;
    synchronized (frameLock) {
      if (pendingFrame == null) {
        return;
      }
      frame = pendingFrame;
      pendingFrame = null;
    }
    if (eglBase == null || !eglBase.hasSurface()) {
//      logD("Dropping frame - No surface");
      VideoRenderer.renderFrameDone(frame);
      return;
    }

    final long startTimeNs = System.nanoTime();
    float[] texMatrix =
        RendererCommon.rotateTextureMatrix(frame.samplingMatrix, frame.rotationDegree);

    // After a surface size change, the EGLSurface might still have a buffer of the old size in the
    // pipeline. Querying the EGLSurface will show if the underlying buffer dimensions haven't yet
    // changed. Such a buffer will be rendered incorrectly, so flush it with a black frame.
    synchronized (layoutLock) {
      int surfaceClearCount = 0;
      while (eglBase.surfaceWidth() != surfaceWidth || eglBase.surfaceHeight() != surfaceHeight) {
        ++surfaceClearCount;
        if (surfaceClearCount > MAX_SURFACE_CLEAR_COUNT) {
          logD("Failed to get surface of expected size - dropping frame.");
          VideoRenderer.renderFrameDone(frame);
          return;
        }
        logD("Surface size mismatch - clearing surface.");
        clearSurfaceOnRenderThread();
      }
      final float[] layoutMatrix;
      if (layoutAspectRatio > 0) {
        layoutMatrix = RendererCommon.getLayoutMatrix(
            mirror, frame.rotatedWidth() / (float) frame.rotatedHeight(), layoutAspectRatio);
      } else {
        layoutMatrix =
            mirror ? RendererCommon.horizontalFlipMatrix() : RendererCommon.identityMatrix();
      }
      texMatrix = RendererCommon.multiplyMatrices(texMatrix, layoutMatrix);
    }

    GLES20.glClearColor(0 /* red */, 0 /* green */, 0 /* blue */, 0 /* alpha */);
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

    if (frame.yuvFrame) {
      // Make sure YUV textures are allocated.
      if (yuvTextures == null) {
        yuvTextures = new int[3];
        for (int i = 0; i < 3; i++) {
          yuvTextures[i] = GlUtil.generateTexture(GLES20.GL_TEXTURE_2D);
        }
      }
      yuvUploader.uploadYuvData(
          yuvTextures, frame.width, frame.height, frame.yuvStrides, frame.yuvPlanes);
      drawer.drawYuv(yuvTextures, texMatrix, frame.rotatedWidth(), frame.rotatedHeight(), 0, 0,
          surfaceWidth, surfaceHeight);
    } else {
      drawer.drawOes(frame.textureId, texMatrix, frame.rotatedWidth(), frame.rotatedHeight(), 0, 0,
          surfaceWidth, surfaceHeight);
    }

    final long swapBuffersStartTimeNs = System.nanoTime();
    eglBase.swapBuffers();

    VideoRenderer.renderFrameDone(frame);

    final long currentTimeNs = System.nanoTime();
    synchronized (statisticsLock) {
      ++framesRendered;
      renderTimeNs += (currentTimeNs - startTimeNs);
      renderSwapBufferTimeNs += (currentTimeNs - swapBuffersStartTimeNs);
    }
  }

  protected String averageTimeAsString(long sumTimeNs, int count) {
    return (count <= 0) ? "NA" : TimeUnit.NANOSECONDS.toMicros(sumTimeNs / count) + " μs";
  }

  protected void logStatistics() {
    final long currentTimeNs = System.nanoTime();
    synchronized (statisticsLock) {
      final long elapsedTimeNs = currentTimeNs - statisticsStartTimeNs;
      if (elapsedTimeNs <= 0) {
        return;
      }
      final float renderFps = framesRendered * TimeUnit.SECONDS.toNanos(1) / (float) elapsedTimeNs;
      logD("Duration: " + TimeUnit.NANOSECONDS.toMillis(elapsedTimeNs) + " ms."
          + " Frames received: " + framesReceived + "."
          + " Dropped: " + framesDropped + "."
          + " Rendered: " + framesRendered + "."
          + " Render fps: " + String.format("%.1f", renderFps) + "."
          + " Average render time: " + averageTimeAsString(renderTimeNs, framesRendered) + "."
          + " Average swapBuffer time: "
          + averageTimeAsString(renderSwapBufferTimeNs, framesRendered) + ".");
      resetStatistics(currentTimeNs);
    }
  }

  protected void logD(String string) {
    Logging.d(TAG, name + string);
  }
}
