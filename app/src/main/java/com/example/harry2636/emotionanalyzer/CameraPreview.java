package com.example.harry2636.emotionanalyzer;

/**
 * Created by harrykim on 2017. 4. 8..
 */

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
  private SurfaceHolder mHolder;
  private Camera mCamera;
  private List<Camera.Size> mSupportedPreviewSizes;
  private Camera.Size mPreviewSize;


  public CameraPreview(Context context, Camera camera) {
    super(context);
    mCamera = camera;

    // supported preview sizes
    mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
    // Install a SurfaceHolder.Callback so we get notified when the
    // underlying surface is created and destroyed.
    mHolder = getHolder();
    mHolder.addCallback(this);
    // deprecated setting, but required on Android versions prior to 3.0
    mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
  }


  public void surfaceCreated(SurfaceHolder holder) {
    // The Surface has been created, now tell the camera where to draw the preview.
    try {
      mCamera.setPreviewDisplay(mHolder);

    } catch (IOException e) {
      Log.d("CameraPreview", "Error setting camera preview: " + e.getMessage());
    }
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
    //Log.d("surfacedestroy", "surface destroys");
    // empty. Take care of releasing the Camera preview in your activity.
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    //Log.d("surfacechanged", "sufracechanged starts");
    // If your preview can change or rotate, take care of those events here.
    // Make sure to stop the preview before resizing or reformatting it.

    if (mHolder.getSurface() == null){
      // preview surface does not exist
      return;
    }

    // stop preview before making changes
    try {
      mCamera.stopPreview();
    } catch (Exception e){
      // ignore: tried to stop a non-existent preview
    }

    // set preview size and make any resize, rotate or
    // reformatting changes here

    // start preview with new settings
    try {
      setCameraDisplayOrientation((Activity)getContext(), MainActivity.cameraId, mCamera);
      mCamera.setPreviewDisplay(mHolder);

      Camera.Parameters parameters = mCamera.getParameters();
      parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
      Log.d("preview wh", "w: "+mPreviewSize.width+" h: "+mPreviewSize.height);
      mCamera.setParameters(parameters);

      mCamera.startPreview();

    } catch (Exception e){
      Log.d("CameraPreveiw", "Error starting camera preview: " + e.getMessage());
    }
  }

  public static void setCameraDisplayOrientation(Activity activity,
                                                 int cameraId, Camera camera) {

    Camera.CameraInfo info =
        new Camera.CameraInfo();
    Camera.getCameraInfo(cameraId, info);
    int rotation = activity.getWindowManager().getDefaultDisplay()
        .getRotation();
    int degrees = 0;

    switch (rotation) {
      case Surface.ROTATION_0: degrees = 0; break; //Natural orientation
      case Surface.ROTATION_90: degrees = 90; break; //Landscape left
      case Surface.ROTATION_180: degrees = 180; break; //Upside down
      case Surface.ROTATION_270: degrees = 270; break; //Landscape right
    }




    int result;
    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
      result = (info.orientation + degrees) % 360;
      result = (360 - result) % 360;  // compensate the mirror
    } else {  // back-facing
      result = (info.orientation - degrees + 360) % 360;
    }
    camera.setDisplayOrientation(result);

  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
    final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
    setMeasuredDimension(width, height);

    if (mSupportedPreviewSizes != null) {
      mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
    }
  }


  private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
    final double ASPECT_TOLERANCE = 0.1;
    double targetRatio=(double)h / w;

    if (sizes == null) return null;

    Camera.Size optimalSize = null;
    double minDiff = Double.MAX_VALUE;

    int targetHeight = h;

    for (Camera.Size size : sizes) {
      double ratio = (double) size.width / size.height;
      if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
      if (Math.abs(size.height - targetHeight) < minDiff) {
        optimalSize = size;
        minDiff = Math.abs(size.height - targetHeight);
      }
    }

    if (optimalSize == null) {
      minDiff = Double.MAX_VALUE;
      for (Camera.Size size : sizes) {
        if (Math.abs(size.height - targetHeight) < minDiff) {
          optimalSize = size;
          minDiff = Math.abs(size.height - targetHeight);
        }
      }
    }
    return optimalSize;
  }


}