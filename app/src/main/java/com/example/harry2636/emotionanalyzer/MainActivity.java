package com.example.harry2636.emotionanalyzer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.View;

/**
 * Created by harrykim on 2017. 5. 23..
 */

public class MainActivity extends AppCompatActivity {
  public final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
  public final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;
  public final int ASK_MULTIPLE_PERMISSION = 3;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    if ( (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) ||
        (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
      ActivityCompat.requestPermissions(this,
          new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
          ASK_MULTIPLE_PERMISSION);
    }

    CardView videoWatchPageButton = (CardView) findViewById(R.id.videoListPageButton);
    videoWatchPageButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        moveToVideoListPage();
      }
    });

    CardView highlightWatchPageButton = (CardView) findViewById(R.id.highlightListPageButton);
    highlightWatchPageButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        moveToHighlightListPage();
      }
    });

  }

  private void moveToVideoListPage() {
    Intent intent = new Intent(this, VideoListActivity.class);
    startActivity(intent);
  }

  private void moveToHighlightListPage() {
    Intent intent = new Intent(this, HighlightListActivity.class);
    startActivity(intent);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String permissions[], int[] grantResults) {
    switch (requestCode) {
      case ASK_MULTIPLE_PERMISSION: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0) {
          boolean cameraPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
          boolean writeExternalFile = grantResults[1] == PackageManager.PERMISSION_GRANTED;

          if (cameraPermission && writeExternalFile) {

          } else {
            System.exit(0);
          }

          // permission was granted, yay! Do the
          // contacts-related task you need to do.

        } else {
          System.exit(0);

          // permission denied, boo! Disable the
          // functionality that depends on this permission.
        }
        return;
      }

      // other 'case' lines to check for other
      // permissions this app might request
    }
  }

}
