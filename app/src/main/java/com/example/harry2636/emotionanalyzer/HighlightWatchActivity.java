package com.example.harry2636.emotionanalyzer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by harrykim on 2017. 5. 23..
 */



public class HighlightWatchActivity extends AppCompatActivity {
  private int videoNum = 0;
  private VideoView videoView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.highlight_watch);
    Intent listIntent = getIntent();
    String highlight_url = listIntent.getStringExtra("highlight_url");

    DownloadVideoTask videoTask = new DownloadVideoTask();
    videoTask.execute(highlight_url);
    // videoTask.execute("http://143.248.198.101:3000/6dZWm1DhQeQ_highlight_0.mp4");
    //videoTask.execute("http://www.sample-videos.com/video/mp4/720/big_buck_bunny_720p_1mb.mp4");
  }

  /* Referred http://stackoverflow.com/questions/17699679/passing-input-stream-to-video-view */
  private class DownloadVideoTask extends AsyncTask<String, String, String> {
    private ProgressDialog progressDialog;
    private int total = 0;
    public DownloadVideoTask() {
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      progressDialog = new ProgressDialog(HighlightWatchActivity.this);
      progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      progressDialog.setMessage("Start");
      progressDialog.setCancelable(false);
      progressDialog.show();
    }

    @Override
    protected String doInBackground(String... urls) {
      String url = urls[0];
      String result = url;
      if (result == null) {
        try {
          HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();

          int fileLength = urlConnection.getContentLength();
          Log.d("donwloadVideo", "" + fileLength);
          publishProgress("max", Integer.toString(fileLength));

          InputStream in = new BufferedInputStream(urlConnection.getInputStream());
          result = getVideoFromStream(in);
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        Log.d("cache", "No url");
      }

      //result = url;
      return result;
    }

    @Override
    protected void onProgressUpdate(String... progress) {
      if (progress[0].equals("progress")) {
        progressDialog.setProgress(Integer.parseInt(progress[1]));
        progressDialog.setMessage(progress[2]);
      } else if (progress[0].equals("max")) {
        progressDialog.setMax(Integer.parseInt(progress[1]));
      }
    }

    @Override
    protected void onPostExecute(String result) {
      //Toast.makeText(MapsActivity.this, "Video download done", Toast.LENGTH_LONG).show();
      videoView = null;
      LinearLayout linearLayout = (LinearLayout)findViewById(R.id.content);
      linearLayout.removeAllViews();

      videoView = new VideoView(HighlightWatchActivity.this);


      //videoView.setVideoURI(Uri.parse(result));
      videoView.setVideoPath(result);
      final MediaController mediaController =
          new MediaController(HighlightWatchActivity.this);
      videoView.setMediaController(mediaController);

      Log.d("videoPath", result);
      videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

        public void onPrepared(MediaPlayer mp) {
          videoView.requestFocus();
          videoView.start();
          Log.d("videoPlaying", videoView.isPlaying() + "");
          mediaController.show();
        }
      });

      linearLayout.addView(videoView);
      progressDialog.dismiss();
    }

    private String getVideoFromStream(InputStream inputStream) {
      String result = "";
      try {
        File temp = new File(getCacheDir(), "video"+videoNum);
        videoNum++;
        String tempPath = temp.getAbsolutePath();
        FileOutputStream out = new FileOutputStream(temp);
        byte buf[] = new byte[128];
        do {
          int numread = inputStream.read(buf);
          if (numread <= 0)
            break;
          out.write(buf, 0, numread);
          total += numread;
          publishProgress("progress", Integer.toString(total), "Downloading now");
        } while (true);
        try {
          inputStream.close();
          out.close();
        } catch (IOException ex) {
          Log.e("error", ex.getMessage(), ex);
        }
        result = tempPath;
      } catch (Exception e) {
        Log.e("Error", e.getMessage());
        e.printStackTrace();
      }

      return result;
    }
  }
}
