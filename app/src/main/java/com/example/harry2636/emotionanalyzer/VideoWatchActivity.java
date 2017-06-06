package com.example.harry2636.emotionanalyzer;

import static android.content.ContentValues.TAG;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

import android.content.Intent;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/* Referred http://android-coffee.com/tutorial-play-youtube-video/ for Youtube player API*/
public class VideoWatchActivity extends YouTubeBaseActivity implements  YouTubePlayer.OnInitializedListener{
  private static YouTubePlayer player;
  private Camera mCamera;
  private CameraPreview mPreview;
  public static int cameraId = 0;

  private static boolean sendFlag = false;
  private static final Object sendLock = new Object();
  private int randomId;

  private String selectedVideoId;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.video_watch);

    Intent listIntent =getIntent();
    selectedVideoId = listIntent.getStringExtra("id");

    YouTubePlayerView youTubePlayerView = (YouTubePlayerView) findViewById(R.id.youtube_player);
    youTubePlayerView.initialize(Configuration.API_KEY, this);

    /*
    // Add a listener to the Capture button
    Button captureButton = (Button) findViewById(R.id.button_capture);
    captureButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            // get an image from the camera
            mCamera.takePicture(null, null, mPicture);
          }
        }
    );
    */

    Random r = new Random();
    randomId = r.nextInt();
  }
  private void initializeCamera() {
    // Create an instance of Camera
    mCamera = getFrontCamera();

    // Create our Preview view and set it as the content of our activity.
    mPreview = new CameraPreview(this, mCamera);
    FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
    preview.addView(mPreview);
  }

  @Override
  protected void onResume() {
    super.onResume();
    initializeCamera();              // release the camera immediately on pause event
  }

  @Override
  protected void onPause() {
    super.onPause();
    releaseCamera();              // release the camera immediately on pause event
  }

  private void releaseCamera(){
    if (mCamera != null){
      mCamera.stopPreview();
      FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
      preview.removeAllViews();
      mPreview = null;
      mCamera.release();        // release the camera for other applications
      mCamera = null;
    }
  }


  /** Primary camera is usually back camera*/
  public Camera getPrimaryCamera(){
    Camera camera = null;
    try {
      camera = Camera.open(); // attempt to get a Camera instance
    }
    catch (Exception e){
      Log.e(TAG, "Camera failed to open: " + e.getLocalizedMessage());
      // Camera is not available (in use or does not exist)
    }
    return camera; // returns null if camera is unavailable
  }

  public Camera getFrontCamera() {
    int cameraCount = 0;
    Camera camera = null;
    Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
    cameraCount = Camera.getNumberOfCameras();
    for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
      Camera.getCameraInfo(camIdx, cameraInfo);
      if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
        try {
          cameraId = camIdx;
          camera = Camera.open(camIdx);
        } catch (RuntimeException e) {
          Log.e(TAG, "Camera failed to open: " + e.getLocalizedMessage());
        }
      }
    }

    return camera;
  }

  private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
      Log.d("picture", "picture sent to server");
      PostImageTask postImageTask = new PostImageTask(data,
          VideoWatchActivity.player.getCurrentTimeMillis(), selectedVideoId);
      postImageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      mCamera.startPreview();
    }
  };

  /** Create a File for saving an image or video */
  private static File getOutputMediaFile(int type){
    // To be safe, you should check that the SDCard is mounted
    // using Environment.getExternalStorageState() before doing this.

    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_PICTURES), "MyCameraApp");
    // This location works best if you want the created images to be shared
    // between applications and persist after your app has been uninstalled.

    // Create the storage directory if it does not exist
    if (! mediaStorageDir.exists()){
      if (! mediaStorageDir.mkdirs()){
        Log.d("MyCameraApp", "failed to create directory");
        return null;
      }
    }

    // Create a media file name
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    File mediaFile;
    if (type == MEDIA_TYPE_IMAGE){
      mediaFile = new File(mediaStorageDir.getPath() + File.separator +
          "IMG_"+ timeStamp + ".jpg");
    } else if(type == MEDIA_TYPE_VIDEO) {
      mediaFile = new File(mediaStorageDir.getPath() + File.separator +
          "VID_"+ timeStamp + ".mp4");
    } else {
      return null;
    }
    return mediaFile;
  }



  /* Youtube related functions */

  @Override
  public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult result) {
    Toast.makeText(this, "Failured to Initialize!", Toast.LENGTH_LONG).show();
  }
  @Override
  public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
/** add listeners to YouTubePlayer instance **/
    player.setPlayerStateChangeListener(playerStateChangeListener);
    player.setPlaybackEventListener(playbackEventListener);
/** Start buffering **/
    if (!wasRestored) {
      player.cueVideo(selectedVideoId);
    }

    VideoWatchActivity.player = player;
  }
  private YouTubePlayer.PlaybackEventListener playbackEventListener = new YouTubePlayer.PlaybackEventListener() {
    @Override
    public void onBuffering(boolean arg0) {
    }

    @Override
    public void onPaused() {
      synchronized (sendLock) {
        sendFlag = false;
      }
      //Toast.makeText(VideoWatchActivity.this, "Video is paused", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPlaying() {
      synchronized (sendLock) {
        sendFlag = true;
      }
      //Toast.makeText(VideoWatchActivity.this, "Video is playing", Toast.LENGTH_LONG).show();

      PictureLoopTask pictureLoopTask = new PictureLoopTask();
      pictureLoopTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onSeekTo(int arg0) {
    }

    @Override
    public void onStopped() {
      synchronized (sendLock) {
        sendFlag = false;
      }
      //Toast.makeText(VideoWatchActivity.this, "Video is stopped", Toast.LENGTH_LONG).show();
    }
  };
  private YouTubePlayer.PlayerStateChangeListener playerStateChangeListener = new YouTubePlayer.PlayerStateChangeListener() {
    @Override
    public void onAdStarted() {
    }
    @Override
    public void onError(YouTubePlayer.ErrorReason arg0) {
    }
    @Override
    public void onLoaded(String arg0) {
      //Toast.makeText(VideoWatchActivity.this, "Video is loading", Toast.LENGTH_LONG).show();
    }
    @Override
    public void onLoading() {
    }
    @Override
    public void onVideoEnded() {
    }
    @Override
    public void onVideoStarted() {
      //Toast.makeText(VideoWatchActivity.this, "Video started", Toast.LENGTH_LONG).show();
    }
  };

  private class PostImageTask extends AsyncTask<String, Void, String> {
    byte[] data;
    int time;
    String videoId;

    public PostImageTask(byte[] data, int time, String videoId) {
      this.data = data;
      this.time = time;
      this.videoId = videoId;
    }

    protected String doInBackground(String... urls) {
      String result = postImageToServer(data);
      return result;
    }

    protected void onPostExecute(String result) {
      super.onPostExecute(result);
      Log.d("result", result);
    }

    private String postImageToServer(byte[] data) {

      try {
        URL url = new URL(Configuration.SERVER_ADDRESS + "/face");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        String encodedImage = Base64.encodeToString(data, Base64.DEFAULT);
        Log.d("imageLength", encodedImage.length() +"");
        JSONObject object = new JSONObject();
        object.put("userId", randomId);
        object.put("videoId", videoId);
        object.put("time", this.time);
        object.put("image", encodedImage);

        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(object.toString());
        writer.flush();
        writer.close();

        InputStream in = new BufferedInputStream(connection.getInputStream());
        BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder responseStrBuilder = new StringBuilder();

        String inputStr;
        while ((inputStr = streamReader.readLine()) != null)
          responseStrBuilder.append(inputStr);
        JSONObject result = new JSONObject(responseStrBuilder.toString());

        in.close();
        connection.disconnect();
        return result.get("ok").toString();
      } catch (IOException e) {
        e.printStackTrace();
      } catch (JSONException e) {
        e.printStackTrace();
      }
      return "error";
    }
  }

  private class PictureLoopTask extends AsyncTask<String, Void, String> {

    public PictureLoopTask() {
    }

    protected String doInBackground(String... urls) {
      while (sendFlag) {
        mCamera.takePicture(null, null, mPicture);
        Log.d("position", VideoWatchActivity.player.getCurrentTimeMillis() + "");
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      return "done";
    }

    protected void onPostExecute(String result) {
      super.onPostExecute(result);
      Log.d("loop", result);
    }
  }
}
