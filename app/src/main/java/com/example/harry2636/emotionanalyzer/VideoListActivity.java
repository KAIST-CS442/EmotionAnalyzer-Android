package com.example.harry2636.emotionanalyzer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by harrykim on 2017. 5. 23..
 */

public class VideoListActivity extends AppCompatActivity {
  private EditText et;
  AsyncTask<?, ?, ?> searchTask;
  ArrayList<SearchData> sdata = new ArrayList<SearchData>();
  String videoId = "";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.video_list);

    et = (EditText) findViewById(R.id.eturl);

    Button search = (Button) findViewById(R.id.search);
    search.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        searchTask = new searchTask().execute();

      }
    });

  }

  /* Referred from http://ondestroy.tistory.com/entry/안드로이드-유튜브youtube-v3-동영상-리스트-검색하기 */
  private class searchTask extends AsyncTask<Void, Void, Void> {
    @Override
    protected void onPreExecute() {
      super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... params) {
      try {
        parsingJsonData(searchFromYouTube());
      } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      return null;
    }

    @Override
    protected void onPostExecute(Void result) {

      ListView searchlist = (ListView) findViewById(R.id.searchlist);

      StoreListAdapter mAdapter = new StoreListAdapter(
          VideoListActivity.this, R.layout.listview_start, sdata); //Json파싱해서 가져온 유튜브 데이터를 이용해서 리스트를 만들어줍니다.

      searchlist.setAdapter(mAdapter);

    }
  }

  public JSONObject searchFromYouTube() {

    String queryUrl =
        "https://www.googleapis.com/youtube/v3/search?"
            + "part=snippet&q=" + et.getText().toString()
            + "&key="+ Configuration.API_KEY+"&maxResults=50";

    Log.e("queryUrl", queryUrl);
    JSONObject searchJson = getJsonFromUrl(queryUrl);

    return searchJson;
  }

  //파싱을 하면 여러가지 값을 얻을 수 있는데 필요한 값들을 세팅하셔서 사용하시면 됩니다.
  private void parsingJsonData(JSONObject jsonObject) throws JSONException {
    sdata.clear();

    Log.e("search_result", jsonObject.toString());

    JSONArray contacts = jsonObject.getJSONArray("items");

    for (int i = 0; i < contacts.length(); i++) {
      JSONObject c = contacts.getJSONObject(i);
      String kind =  c.getJSONObject("id").getString("kind"); // 종류를 체크하여 playlist도 저장
      if(kind.equals("youtube#video")){
        videoId = c.getJSONObject("id").getString("videoId"); // 유튜브
        // 동영상
        // 아이디
        // 값입니다.
        // 재생시
        // 필요합니다.
      }else{
        continue;
        //videoId = c.getJSONObject("id").getString("playlistId"); // 유튜브
      }

      String title = c.getJSONObject("snippet").getString("title"); //유튜브 제목을 받아옵니다
      String changString = "";
      try {
        //TODO: check and erase
        changString = new String(title.getBytes("8859_1"), "utf-8"); //한글이 깨져서 인코딩 해주었습니다
      } catch (UnsupportedEncodingException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      changString = title;

      String date = c.getJSONObject("snippet").getString("publishedAt") //등록날짜
          .substring(0, 10);
      String imgUrl = c.getJSONObject("snippet").getJSONObject("thumbnails")
          .getJSONObject("default").getString("url");  //썸내일 이미지 URL값
      sdata.add(new SearchData(videoId, changString, imgUrl, date));
    }

  }

  public class StoreListAdapter extends ArrayAdapter<SearchData> {
    private ArrayList<SearchData> items;
    SearchData fInfo;

    public StoreListAdapter(Context context, int textViewResourseId,
                            ArrayList<SearchData> items) {
      super(context, textViewResourseId, items);
      this.items = items;
    }

    public View getView(int position, View convertView, ViewGroup parent) {// listview

      // 출력
      View v = convertView;
      fInfo = items.get(position);

      LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

      v = vi.inflate(R.layout.listview_start, null);
      ImageView img = (ImageView) v.findViewById(R.id.img);

      String url = fInfo.getUrl();

      String sUrl = "";
      String eUrl = "";
      sUrl = url.substring(0, url.lastIndexOf("/") + 1);
      eUrl = url.substring(url.lastIndexOf("/") + 1, url.length());
      try {
        eUrl = URLEncoder.encode(eUrl, "EUC-KR").replace("+", "%20");
      } catch (UnsupportedEncodingException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      String new_url = sUrl + eUrl;
      new DownloadImageTask(img)
          .execute(new_url);
      /*
      URL full_url = null;
      try {
        full_url = new URL(new_url);
        Bitmap bmp = BitmapFactory.decodeStream(full_url.openConnection().getInputStream());
        img.setImageBitmap(bmp);
      } catch (MalformedURLException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
      */

      v.setTag(position);
      v.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          int pos = (Integer) v.getTag();

          Intent intent = new Intent(VideoListActivity.this,
              VideoWatchActivity.class);
          intent.putExtra("id", items.get(pos).getVideoId());
          startActivity(intent); //리스트 터치시 재생하는 엑티비티로 이동합니다. 동영상 아이디를 넘겨줍니다..
        }
      });

      ((TextView) v.findViewById(R.id.title)).setText(fInfo.getTitle());
      ((TextView) v.findViewById(R.id.date)).setText(fInfo
          .getPublishedAt());

      return v;
    }
  }

  private JSONObject getJsonFromUrl (String requestString) {
    JSONObject result = null;
    try {
      URL requestUrl = new URL(requestString.replace(" ", "%20"));
      HttpURLConnection urlConnection = (HttpURLConnection) requestUrl.openConnection();
      InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
      result = getJsonFromStream(inputStream);
      Log.e("getJsonFromUrl", result.toString());
    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  private JSONObject getJsonFromStream(InputStream inputStream) {
    JSONObject resultObject = null;
    try {
      BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
      StringBuilder responseStrBuilder = new StringBuilder();

      String inputStr;
      while ((inputStr = streamReader.readLine()) != null)
        responseStrBuilder.append(inputStr);
      resultObject = new JSONObject(responseStrBuilder.toString());
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    }

    return resultObject;
  }

  private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    ImageView bmImage;

    public DownloadImageTask(ImageView bmImage) {
      this.bmImage = bmImage;
    }

    protected Bitmap doInBackground(String... urls) {
      String urldisplay = urls[0];
      Bitmap mIcon11 = null;
      try {
        InputStream in = new java.net.URL(urldisplay).openStream();
        mIcon11 = BitmapFactory.decodeStream(in);
      } catch (Exception e) {
        Log.e("Error", e.getMessage());
        e.printStackTrace();
      }
      return mIcon11;
    }

    protected void onPostExecute(Bitmap result) {
      bmImage.setImageBitmap(result);
    }
  }
}
