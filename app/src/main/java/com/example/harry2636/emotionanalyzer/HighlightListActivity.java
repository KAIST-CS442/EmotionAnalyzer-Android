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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by harrykim on 2017. 5. 23..
 */

public class HighlightListActivity extends AppCompatActivity {
  AsyncTask<?, ?, ?> highlightTask;
  ArrayList<SearchData> sdata = new ArrayList<SearchData>();
  String videoId = "";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.highlight_list);

    highlightTask = new GetHighlightTask().execute();
  }

  /* Referred from http://ondestroy.tistory.com/entry/안드로이드-유튜브youtube-v3-동영상-리스트-검색하기 */
  private class GetHighlightTask extends AsyncTask<Void, Void, Void> {
    @Override
    protected void onPreExecute() {
      super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... params) {
      try {
        parsingJsonData(getListFromServer());
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
          HighlightListActivity.this, R.layout.listview_start, sdata); //Json파싱해서 가져온 유튜브 데이터를 이용해서 리스트를 만들어줍니다.

      searchlist.setAdapter(mAdapter);

    }
  }

  public JSONObject getListFromServer() {
    String queryUrl = Configuration.SERVER_ADDRESS + "/highlight/list";
    JSONObject searchJson = getJsonFromUrl(queryUrl);
    return searchJson;
  }

  //파싱을 하면 여러가지 값을 얻을 수 있는데 필요한 값들을 세팅하셔서 사용하시면 됩니다.
  private void parsingJsonData(JSONObject jsonObject) throws JSONException {
    sdata.clear();

    Log.e("search_result", jsonObject.toString());

    JSONArray contacts = jsonObject.getJSONArray("items");

    for (int i = 0; i <contacts.length(); i++) {
      JSONObject highlight = contacts.getJSONObject(i);
      String video_name = highlight.getString("video_name");
    }

    for (int i = 0; i < contacts.length(); i++) {
      /* returns [{ video_id: 'video_id', video_name: 'video_name',
       * highlight_url: 'highlight_url', thumbnail_url: 'thumbnail_url'}]
       */
      JSONObject highlight = contacts.getJSONObject(i);
      String video_id = highlight.getString("video_id");
      String video_name = highlight.getString("video_name");

      int name_counter = 0;
      for (int j = 0; j < i; j++) {
        JSONObject previous_highlight = contacts.getJSONObject(i);
        String previous_video_name = previous_highlight.getString("video_name");
        if (video_name == previous_video_name) {
          name_counter++;
        }
      }

      if (name_counter != 0) {
        video_name = video_name + "-" + name_counter;
      }

      String highlight_url = highlight.getString("highlight_url");
      highlight_url = Configuration.SERVER_ADDRESS + "/" + highlight_url;
      String imgUrl = highlight.getString("thumbnail_url");
      imgUrl = Configuration.SERVER_ADDRESS + "/" + imgUrl;

      /* Add highlight_url instead of video_id in the first argument */
      sdata.add(new SearchData(highlight_url, video_name, imgUrl, ""));
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
      Log.e("fInfo url", url);

      String new_url = url;
      new DownloadImageTask(img)
          .execute(new_url);

      v.setTag(position);
      v.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          int pos = (Integer) v.getTag();

          Intent intent = new Intent(HighlightListActivity.this,
              HighlightWatchActivity.class);
          intent.putExtra("highlight_url", items.get(pos).getVideoId());
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
