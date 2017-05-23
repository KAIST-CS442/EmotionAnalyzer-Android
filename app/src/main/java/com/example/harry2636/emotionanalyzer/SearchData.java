package com.example.harry2636.emotionanalyzer;

/**
 * Created by harrykim on 2017. 5. 23..
 */

/* Referred from http://ondestroy.tistory.com/entry/안드로이드-유튜브youtube-v3-동영상-리스트-검색하기 */
public class SearchData {
  String videoId;
  String title;
  String url;
  String publishedAt;

  public SearchData(String videoId, String title, String url,
                    String publishedAt) {
    super();
    this.videoId = videoId;
    this.title = title;
    this.url = url;
    this.publishedAt = publishedAt;
  }

  public String getVideoId() {
    return videoId;
  }

  public void setVideoId(String videoId) {
    this.videoId = videoId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getPublishedAt() {
    return publishedAt;
  }

  public void setPublishedAt(String publishedAt) {
    this.publishedAt = publishedAt;
  }
}
