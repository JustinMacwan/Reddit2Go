package com.example.andrew.reddit2go;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Dan on 2015-02-08.
 * Activity called by the MainActivity when a Post in the PostFragment list is clicked, contains the webview and loads only from cache
 */
public class ViewPostActivity extends Activity {
    //swipe left/right handler
    private OnSwipeTouchListener onSwipeTouchListener;
    //the url of this post
    private String url;
    //WebView on this activity
    private WebView webView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_post);

        //get the url from the extras in the Intent.
        Bundle extras = getIntent().getExtras();
        //If there is no URL, trigger sending Activity again.
        if(extras != null){
            url = extras.getString(RedditList.POST_PERMALINK_JSON_NAME);
            //If Available, navigate to the the url passed to this activity
            if(url!=null && url!=""){
                //Get a reference to the WebView on the page, and direct it to the selected Post's content url
                webView = (WebView)findViewById(R.id.postContentView);

                // Initialize the WebView
                webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
                webView.getSettings().setSupportZoom(true);
                webView.getSettings().setBuiltInZoomControls(true);
                webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
                webView.setScrollbarFadingEnabled(true);
                webView.getSettings().setLoadsImagesAutomatically(true);
                webView.getSettings().setAppCachePath(getCacheDir().getAbsolutePath());
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setAllowFileAccess(true);
                webView.getSettings().setAppCacheEnabled(true);

                // Load the URLs inside the WebView, not in the external web browser
                webView.setWebViewClient(new WebViewClient(){
                    @Override
                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                        view.loadData("<html>"+failingUrl+" Failed because of " + errorCode + ".  Which means: " + description + "</html>", "html", null);

                        //Log.e("Reddit2Go", "Error loading " + failingUrl + ": " + errorCode);
                        super.onReceivedError(view, errorCode, description, failingUrl);
                    }
                });

                //add swipe left/right functionality to the WebView
                onSwipeTouchListener = new OnSwipeTouchListener() {
                    public void onSwipeRight() {
                        //Toast.makeText(ViewPostActivity.this, "Right swipe, navigate left", Toast.LENGTH_SHORT).show();
                        navigateEarlierPost(webView.getUrl());
                    }
                    public void onSwipeLeft() {
                        //Toast.makeText(ViewPostActivity.this, "Left swipe, navigate right", Toast.LENGTH_SHORT).show();
                        navigateLaterPost(webView.getUrl());
                    }
                };

                webView.setOnTouchListener(onSwipeTouchListener);

                //finally, now that the WebView is all set up, load and cache the url
                webView.loadUrl(url);
            }
        }
        else{//trigger sending Activity because the required Permalink was not included in the starting intent
            Intent returnToSender = new Intent( this, MainActivity.class);
            startActivity(returnToSender);
        }
    }

    //user has paginated right, so update the WebView with the appropriate post url
    private void navigateLaterPost(String currentPostUrl){
        Reddit2GoService service = Reddit2GoService.getRunningInstance();
        if(null!=service){
            //Log.d("Reddit2Go SwipeRight", "Paginate right");
            Post nextPost = service.nextPost(service.postFromURL(currentPostUrl));
            String nextPage = nextPost.getPermalink();
            if(!service.isFirstOrLastPost(service.postFromURL(currentPostUrl))){
                webView.loadUrl(PostFragment.BASE_REDDIT_URL+nextPage);
                nextPost.readPost();
            }
            else{ //load the list instead of the same page again
                goBackToMainActivity();
            }
        }
        else{
            //Log.d("Reddit2Go SwipeRight", "Service is null");

        }
    }

    //user has paginated left, so update the WebView with the appropriate post url
    private void navigateEarlierPost(String currentPostUrl){
        Reddit2GoService service = Reddit2GoService.getRunningInstance();
        if(null!=service){
            //Log.d("Reddit2Go - onSwipeLeft", "Paginate left");
            Post nextPost = service.previousPost(service.postFromURL(currentPostUrl));
            String nextPage = nextPost.getPermalink();
            if(!service.isFirstOrLastPost(service.postFromURL(currentPostUrl))){
                webView.loadUrl(PostFragment.BASE_REDDIT_URL+nextPage);
                nextPost.readPost();
            }
            else{ //load the list instead of the same page again
                goBackToMainActivity();
            }
        }
        else{
            //Log.d("Reddit2Go - onSwipeLeft", "Service is null");
        }
    }

    private void goBackToMainActivity(){
        Intent startMainActivity = new Intent(this, MainActivity.class);
        startActivity(startMainActivity);
        //Toast.makeText(this,"Should move to main activity",Toast.LENGTH_LONG);
    }
}
