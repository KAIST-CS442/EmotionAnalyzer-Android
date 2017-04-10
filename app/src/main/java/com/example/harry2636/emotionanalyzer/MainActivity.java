package com.example.harry2636.emotionanalyzer;

import static android.content.ContentValues.TAG;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

/* Referred http://android-coffee.com/tutorial-play-youtube-video/ for Youtube player API*/
public class MainActivity extends YouTubeBaseActivity implements  YouTubePlayer.OnInitializedListener{
  public static final String API_KEY = "AIzaSyDKYGldszOioPAiSFTKrto200NbSEg1aVI";
  public static final String VIDEO_ID = "8A2t_tAjMz8"; // Twice Knock Knock video id

  private Camera mCamera;
  private CameraPreview mPreview;
  public static int cameraId = 0;

  //TODO: fix ip address
  public static final String SERVER_ADDRESS = "http://target ip address"; //This must not be localhost!!!

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);


    YouTubePlayerView youTubePlayerView = (YouTubePlayerView) findViewById(R.id.youtube_player);
    youTubePlayerView.initialize(API_KEY, this);

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
      PostImageTask postImageTask = new PostImageTask(data);
      postImageTask.execute();

      File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
      if (pictureFile == null){
        Log.d(TAG, "Error creating media file, check storage permissions:");
        return;
      }

      try {
        FileOutputStream fos = new FileOutputStream(pictureFile);
        fos.write(data);
        fos.close();
      } catch (FileNotFoundException e) {
        Log.d(TAG, "File not found: " + e.getMessage());
      } catch (IOException e) {
        Log.d(TAG, "Error accessing file: " + e.getMessage());
      }
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
      player.cueVideo(VIDEO_ID);
    }
  }
  private YouTubePlayer.PlaybackEventListener playbackEventListener = new YouTubePlayer.PlaybackEventListener() {
    @Override
    public void onBuffering(boolean arg0) {
    }
    @Override
    public void onPaused() {
    }
    @Override
    public void onPlaying() {
    }
    @Override
    public void onSeekTo(int arg0) {
    }
    @Override
    public void onStopped() {
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
    }
    @Override
    public void onLoading() {
    }
    @Override
    public void onVideoEnded() {
    }
    @Override
    public void onVideoStarted() {
    }
  };

  private class PostImageTask extends AsyncTask<String, Void, String> {
    byte[] data;

    public PostImageTask(byte[] data) {
      this.data = data;
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
        URL url = new URL(SERVER_ADDRESS);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        String encodedImage = Base64.encodeToString(data, Base64.DEFAULT);
        Log.d("imageLength", encodedImage.length() +"");
        JSONObject object = new JSONObject();
        object.put("userId", 1);
        object.put("videoId", VIDEO_ID);
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
}
