package com.example.andrew.reddit2go;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Reddit2GoService extends Service {

    public static final String NUMBER_POSTS = "num_posts";
    //keep track of the running service instance
    private static Reddit2GoService instance = null;
    // NOTE: the content of this path will be deleted
    //       when the application is uninstalled (Android 2.2 and higher)
    protected File extStorageAppBasePath;

    protected File extStorageAppCachePath;

    //Until settings are implemented, use this subreddit as the default to cache on WiFi
    private final String DEFAULT_SUBREDDIT = "askreddit";
    //posts gathered from the loaded subreddit
    private ArrayList<Post> allPosts;
    private ArrayList<Post> visiblePosts;
    private ArrayList<Post> awaitingReferenceAdapter;

    private FloatService floatingInstance = null;

    RedditList redditList;
    ArrayAdapter postFragAdapter;

    private WebView webView;

    //while the user is logged in, keep track of their credentials
    // String to store a user name
    public String username = null;
    // The Reddit cookie string, used by other methods after a successful login.
    private String redditCookie = "";
    //keep track of the existence of an instance of this service
    //private static boolean isRunning = false;
    //Receiver to respond to changes in the list of available WiFi networks
    WiFiNetworksAvailableReceiver networkListReceiver;

    //timer to stagger the network traffic
    Timer cacheTimer;

    public Reddit2GoService() {
        //Log.d("Reddit2Go", "Service instance created");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        //Get a reference to the system WiFi manager so can detect WiFi state change
        WifiManager mainWifiObj;
        mainWifiObj = (WifiManager)getSystemService(Context.WIFI_SERVICE);

        //register custom Broadcast receiver for CONNECTIVITY_ACTION
        WifiStateReceiver wifiReciever = new WifiStateReceiver();
        registerReceiver(wifiReciever, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        //register custom Broadcast receiver for SCAN_RESULTS_AVAILABLE_ACTION
        networkListReceiver = new WiFiNetworksAvailableReceiver();
        registerReceiver(networkListReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        //to avoid NetworkOnMainThreadException brute force
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        super.onCreate();

        //complete list of all known posts
        allPosts = new ArrayList<Post>();
        //curently cached posts
        visiblePosts = new ArrayList<Post>();

        updateList();

        // Check if the external storage is writeable
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
        {
            // Retrieve the base path for the application in the external storage
            File externalStorageDir = Environment.getExternalStorageDirectory();

            if (externalStorageDir != null)
            {
                // {SD_PATH}/Android/data/com.devahead.androidwebviewcacheonsd
                extStorageAppBasePath = new File(externalStorageDir.getAbsolutePath() +
                        File.separator + "Android" + File.separator + "data" +
                        File.separator + getPackageName());
            }

            if (extStorageAppBasePath != null)
            {
                // {SD_PATH}/Android/data/com.devahead.androidwebviewcacheonsd/cache
                extStorageAppCachePath = new File(extStorageAppBasePath.getAbsolutePath() +
                        File.separator + "cache");

                boolean isCachePathAvailable = true;

                if (!extStorageAppCachePath.exists())
                {
                    // Create the cache path on the external storage
                    isCachePathAvailable = extStorageAppCachePath.mkdirs();
                }

                if (!isCachePathAvailable)
                {
                    // Unable to create the cache path
                    extStorageAppCachePath = null;
                }
            }
        }

        //make sure connected to WiFi before caching the detail pages
        if(isWiFiConnected()){
            cacheDetailPages();
        }
    }

    //this method is used in Android 2.2 and higher to get the cache directory
    @Override
    public File getCacheDir()
    {
        if (extStorageAppCachePath != null)
        {
            // Use the external storage for the cache
            return extStorageAppCachePath;
        }
        else
        {
            return super.getCacheDir();
        }

    }

    //go to Reddit.com for the full list of posts and update allPosts
    private void updateList(){

        //DEFAULT_SUBREDDIT contains the name of the default subreddit
        redditList = new RedditList(DEFAULT_SUBREDDIT);

        List<Post> redditPostList = redditList.fetchPost();
        List<Post> redundantPosts = new ArrayList<Post>();
        //get the list of posts not yet contained in allPosts
        for( Post p : redditPostList ){

            for(Post existingtPost : allPosts){
                if(existingtPost.getTitle().compareToIgnoreCase(p.getTitle())==0){
                    redundantPosts.add(p);
                }
            }
        }

        //remove the redundant posts from the list
        for(Post p : redundantPosts){
            redditPostList.remove(p);
        }

        //add the new unique posts to the list of allPosts
        allPosts.addAll(redditPostList);
    }

    /*
    * Cache the detail view pages, and update the detailItemsList with all the currently cached pages
    * To be triggered on WiFi state changes
    * */
    public void cacheDetailPages(){

        //get the updated list of posts from Reddit.com and update allPosts
        updateList();
        //allPosts.addAll(redditList.fetchPost());
        if(null!=webView && isWiFiConnected()){
            loadNextLink();
            return;
        }

        webView = new WebView(getApplicationContext()); //WebView)findViewById(R.id.postContentView);

        // Initialize the WebView
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.setScrollbarFadingEnabled(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
//        webView.getSettings().setAppCachePath(getCacheDir().getAbsolutePath());

        // Load the URLs inside the WebView, not in the external web browser
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageStarted (WebView view, String url, Bitmap favicon){
                //url has started loading
            }
            //If the page has finished, consider it cached as long as adequate time is given before loading the next
            @Override
            public void onPageFinished (WebView view, String url){
                //if not still connected, page has probably not fully loaded, despite calling of this function.
                if(!isWiFiConnected()){
                    //return and wait for reconnection
                    return;
                }
                //add the post to the list now that it is cached
                Post postToAdd = postFromURL(url);
                final String urlOfPost = url;
                if(null!=postToAdd){

                    //the post has cached
                    postToAdd.setHasCached(true);
                    makePostVisible(postToAdd);
                }
                //To give the page time to load and cache, start a timer and give 2 seconds before loading next link
                try {
                    //don't sleep the thread, set up a timer for this
                    //Thread.sleep(2000);
 /*                   cacheTimer = new Timer();
                    cacheTimer.schedule(new TimerTask() {
                        //load the next link on schedule to lessen the impact on the Main UI
                        @Override
                        public void run() {
                            cacheTimerMethod();
                        }

                    }, 0, 2000);
     */
                    (new Handler()).postDelayed(Timer_Tick, 2000);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //If there was an error loading this one, then remove it from the list to avoid and endless loop of failed attempts
            @Override
            public void onReceivedError (WebView view, int errorCode, String description, String failingUrl){
                if(!isWiFiConnected()){
                    return;
                }
                //discard this page that will not load and continue on loading the others.  It will not be visible
                Post postToRemove = postFromURL(failingUrl);
                allPosts.remove(postToRemove);
            }
  /*          @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);

            } */
        });

        //load the next link if still connected
        loadNextLink();
    }

    private void cacheTimerMethod()
    {
        //This method is called directly by the timer
        //and runs in the same thread as the timer.

        //We call the method that will work with the UI
        //through the runOnUiThread method.
        //this.runOnUiThread(Timer_Tick);
        //(new Activity()).runOnUiThread(Timer_Tick); //gives Looper.Prepare exception
        //Timer_Tick.run();
        Intent startMainActivity = new Intent(getBaseContext(), MainActivity.class);
        startMainActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplication().startActivity(startMainActivity);

        //get a reference to the started MainActivity
        while(null==MainActivity.getInstance()){
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //now that reference to MainActivity instance is available, run the cache update on it
        MainActivity.getInstance().runOnUiThread(Timer_Tick);
    }

    private Runnable Timer_Tick = new Runnable() {
        public void run() {
            //Ready to go get the next link
            loadNextLink();
        }
    };

    //Go through the full list of posts and cache the next page
    public void loadNextLink(){

        if(!isWiFiConnected()){
            Log.d("Reddig2Go", "No WiFi, cancel loading cache.");
            return;
        }
        //WiFi is connected, so load the next post permalink
        for(Post p : allPosts) {
            if (!isLoaded(p)) {
                webView.loadUrl(PostFragment.BASE_REDDIT_URL + p.getPermalink());
                return;
            }
        }
    }

    //signal that a post has been cached and add it to visiblePosts, add it to the adapter, and update the ChatHead
    private void makePostVisible(Post loadedPost){
        //now that it is cached, make it visible
        visiblePosts.add(loadedPost);
        //try to update the adapter
        updateFragmentPosts();
        //update the number on the Chat Head
        updateNumUnread(null);
    }

    //use the current post as a starting point and get the next one in the list.  Return this if it is the last one.
    public Post nextPost(Post currentPost){
        int currentIndex=visiblePosts.indexOf(currentPost);
        Post nextPost;
        if(currentIndex < visiblePosts.size()-1){
            nextPost = visiblePosts.get(currentIndex+1);
            nextPost.readPost();
            updateFloatingNumber();
            return nextPost;
        }
        else{
            //Log.d("Reddit2Go", "End of the list");
            return currentPost;
        }
    }

    //use the current post as a starting point and get the previous one in the list.  Return this if it is the first one.
    public Post previousPost(Post currentPost){
        int currentIndex=visiblePosts.indexOf(currentPost);
        if(currentIndex > 0){
            return visiblePosts.get(currentIndex-1);
        }
        else{
            //Log.d("Reddit2Go", "First Post, can't go back any further");
            return currentPost;
        }
    }

    //get the post that has this url as its permalink
    public Post postFromURL(String url){
        for(Post p : allPosts ){
            if(url.contains(p.getPermalink())){
                return p;
            }

        }
        return null;
    }

    //Has this post been loaded successfully in the webview?
    private boolean isLoaded(Post checkPost){
        if( visiblePosts.contains(checkPost)){
            return true;
        }
        return false;
    }

    /*
    Get the posts that have already been loaded, so they can load from the cached page
     */
    public List<Post> getVisiblePosts(){
        return visiblePosts;
    }

    //PostFragment calls to update the service with it's ListView adapter and update that adapter with the outstanding cached posts
    public void adapterFromPostFrag(ArrayAdapter postAdapter){
        postFragAdapter = postAdapter;

        for(Post nextPost : visiblePosts ){

            postFragAdapter.add(nextPost);
        }
    }

    //add the loaded posts to the adapter if not null
    private void updateFragmentPosts(){
        //remove redundant elements
        for(int i = 0 ; i < visiblePosts.size() ; i++){
            for(int j = i+1 ; j < visiblePosts.size() ; j++){
                if(visiblePosts.get(i).getTitle().compareToIgnoreCase(visiblePosts.get(j).getTitle())==0){
                    visiblePosts.remove(j--);
                }
            }
        }
        if(null!=postFragAdapter){
            //Log.d("Reddit2Go", "Adapter available, update with post");

            //add each visible post to the postFragAdapter
            for(Post p : visiblePosts ){
                boolean isContained = false;
                for(int i = 0 ; i < postFragAdapter.getCount() ; i++) {
                    if (((Post)postFragAdapter.getItem(i)).getTitle().compareToIgnoreCase(p.getTitle()) == 0){
                        isContained = true;
                        break;
                    }
                }
                //if the adapter does not currently contain this post, then add it to the adapter
                if(!isContained){
                    postFragAdapter.add(p);
                }
            }
        }
    }

    //get a copy of the current ListView adapter to keep consistent
    public ArrayAdapter getListViewAdapter(){
        return postFragAdapter;
    }

    //called by WifiStateReceiver when there is a CONNECTION_ACTION to initiate caching if connected to WiFi
    private void wifiStatusChange(int currentState, ArrayList<String> availableNetworks){
        //Log.d("Reddit2Go", "WiFi state change to " + currentState);
        switch (currentState){
            case WifiManager.WIFI_STATE_ENABLED:
                ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                if (mWifi.isConnected()) {
                    WifiInfo airInfo = ((WifiManager)getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();

                    //Log.d("String", "WiFi is connected");
                    //connected to WiFi so start to cache the pages
                    cacheDetailPages();
                }
                else{
                    for(String network : availableNetworks){
                        //Log.d("Reddit2Go", "Available network: " + network);
                    }
                }
                break;
            case  WifiManager.WIFI_STATE_DISABLED:
            case WifiManager.WIFI_STATE_DISABLING:
            case WifiManager.WIFI_STATE_ENABLING:
            case WifiManager.WIFI_STATE_UNKNOWN:
            default:
                //do nothing in any of these states
        }
    }

    //force update of number on ChatHead
    public void updateFloatingNumber(){
        updateNumUnread(null);
    }

    //Update the number of unread posts in the FloatingView
    private void updateNumUnread(String networkList){

        if(null==networkList&&!isWiFiConnected()){
            networkList=getNetworkList();
        }

        //establish the current number of unread posts
        int unreadPosts = 0;
        for(Post p : visiblePosts ){
            //Log.d("Reddit2Go", p.wasRead() + ", " + p.getTitle());
            if(!p.wasRead())
                unreadPosts++;
        }

        //if a list of networks is supplied, then this is called by the NetworksAvailalbleReceiver so show list available networks too
        if(null!=networkList){
            if(null==FloatService.getFloatingInstance()){
                Intent startFloat = new Intent(this ,FloatService.class);
                //startFloat.putExtra(NUMBER_POSTS, "" + unreadPosts );
                startService(startFloat);
                floatingInstance = FloatService.getFloatingInstance();
                if(null!=floatingInstance){
                    floatingInstance.updateUnreadNum(unreadPosts, networkList);
                }
            }
            else{
                floatingInstance = FloatService.getFloatingInstance();
                floatingInstance.updateUnreadNum(unreadPosts, networkList);
            }
        }
        //currently connected and there are posts to show but there is no ChatHead instance
        else if(null == floatingInstance && unreadPosts>0){
            Intent startFloat = new Intent(this ,FloatService.class);
            //startFloat.putExtra(NUMBER_POSTS, "" + unreadPosts );
            startService(startFloat);
            floatingInstance = FloatService.getFloatingInstance();
            if(null!=floatingInstance){
                floatingInstance.updateUnreadNum(unreadPosts);
            }
        }
        //update the existing ChatHead with the number of unread posts
        else if(null!=floatingInstance){
            floatingInstance.updateUnreadNum(unreadPosts);
        }

        //to stop the ChatHead: stopService(new Intent(HelloWorldActivity.this, FloatService.class));
    }

    //check the current WiFi state and return true if connected
    private boolean isWiFiConnected(){
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi.isConnected())
            return true;
        else
            return false;
    }

    //Class to manage the WiFi broadcasts and initiate post retrieval and update triggers on ConnectivityManager.CONNECTION_ACTION
    class WifiStateReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            Bundle extras = intent.getExtras();
            ConnectivityManager conMan = (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
            ArrayList<String> availableNetworks = new ArrayList<String>();
            wifiStatusChange(((WifiManager)getSystemService(Context.WIFI_SERVICE)).getWifiState(),availableNetworks);
        }
    }

    //Class to manage WiFi networks available notification
    class WiFiNetworksAvailableReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent){

            if(isWiFiConnected()){
                updateNumUnread(null);
                return;
            }

            String availableNetworks = getNetworkList();

            //update the chathead with the names of the available networks
            updateNumUnread(availableNetworks );
            //Log.d("Reddit2Go", "Network List: " + networkList.toString());
        }
    }

    //get the list of networks available for connection if 0 return null
    private String getNetworkList() {
        WifiManager mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        List<ScanResult> wifiList;
        StringBuilder networkList = new StringBuilder();
        wifiList = mainWifi.getScanResults();
        //number of networks discovered, if 0 in the end return null
        int numAvailable = 0;
        //for each of the elements in the List of networks returned from the scan, append to list for ChatHead
        for(int i = 0; i < wifiList.size(); i++){

            numAvailable++;
            if(networkList.length()==0){
                networkList.append("Would you like to connect to:\n");
            }
            else{
                networkList.append("\n");
            }
            networkList.append(new Integer(i+1).toString() + ". ");
            networkList.append((wifiList.get(i)).SSID);
        }

        if(numAvailable==0)
            return null;
        //return the String version of this network list discovered
        return networkList.toString();
    }

    //after successful login, set the username of the logged in client
    public void setUsername(String name){
        username = name;
    }

    //get the username of the currently logged in user, or null if none exists
    public String getCurrentUser(){
        return username;
    }

    //set the cookie upon successful login, for use in getting this user's reddits
    public void setRedditCookie(String cookie){
        redditCookie = cookie;
    }

    //returns the cookie set when the user logged in
    public String getRedditCookie(){
        return redditCookie;
    }

    //called when an activity binds to this service for communication purposes
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    //public static boolean isRunning(){ return isRunning; }
    public static Reddit2GoService getRunningInstance(){
        if(null!=instance){
            return instance;
        }
        else{
            //Log.d("Reddit2Go", "Instance invalid");
            return null;
        }
    }

    //is this post either the first or the last post in the list
    public boolean isFirstOrLastPost(Post post){
        if(post.getTitle().compareToIgnoreCase(visiblePosts.get(0).getTitle())==0)
            return true;
        else if(post.getTitle().compareToIgnoreCase(visiblePosts.get(visiblePosts.size()-1).getTitle())==0)
            return true;

        return false;
    }

    //destroy the instance if this service is stopped
    public void onDestroy(){
        instance = null;
    }
}
