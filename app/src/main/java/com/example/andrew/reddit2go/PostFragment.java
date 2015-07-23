package com.example.andrew.reddit2go;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Created by Andrew on 2015-02-06.
 * Fragment that contains the Post list in the main activity.
 */
public class PostFragment extends Fragment {

    //prefix for permalink url
    public static final String BASE_REDDIT_URL = "http://www.reddit.com";
    //keep a reference to the last created instance
    private static PostFragment instance = null;

    ListView postList;
    ArrayAdapter<Post> adapter;
    Handler handler;

    //String subreddit;
    List<Post> posts;
    //RedditList redditList;

    //constructor instantiates the posts ArrayList
    public PostFragment() {

        posts = new ArrayList<Post>();
        initialize();
    }

    //get an intstance of this fragment, using the defaut Reddit list for testing
    public static Fragment newInstance (String subreddit) {
        if(null != instance){ return instance; }
        PostFragment pf = new PostFragment();
        //pf.subreddit = subreddit;
        //pf.redditList = new RedditList(pf.subreddit);
        instance=pf;
        return pf;
    }

    //when this PostFragment is built and shown on the screen set its onClick listeners for Post clicks
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.post, container, false);
        postList = (ListView)v.findViewById(R.id.posts_list);
        //Dan Feb 8 handle onClick to ListView items
        postList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?>adapter,View v, int position, long id){

                //get reference to the Post that was clicked
                Post item = (Post)adapter.getItemAtPosition(position);
                //update Post with "read" status
                item.readPost();
                //force the service to update the ChatHead
                Reddit2GoService service = Reddit2GoService.getRunningInstance();
                if(null!=service){
                    service.updateFloatingNumber();
                }
                else{
                    //Log.d("Reddit2Go", "Service is null, not updating ChatHead");
                }
                //Build an Intent we'll use to launch the ViewPostActivity, passing along the URL of the Post content
                Intent intent = new Intent(getActivity(), ViewPostActivity.class).putExtra(RedditList.POST_PERMALINK_JSON_NAME, BASE_REDDIT_URL + item.permalink);
                //launch the ViewPostActivity
                startActivity(intent);
            }
        });

        return v;
    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    //initialize this fragment, and force the service to update its views
    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //initialize();
    }

    //initialize this PostFragment
    private void initialize() {

        //Update the ListView ArrayAdapter
        createAdapter();

    }

    //Reference, or create an adapter for the ListView
    private void createAdapter() {
        //must have an Activity to be running this Fragment
        if (getActivity()==null)
            return;
        //get a reference to the Reddit2GoService
        Reddit2GoService service = Reddit2GoService.getRunningInstance();
        //does the adapter already match the Application version
        if (null!=adapter && null!=service && service.getListViewAdapter().equals(adapter)) {
            //Log.d("Reddit2Go", "Adapter is up to date");
            return;
        }
        //check the application to see if there is an existing adapter, and use that one if so
        else if (null!=service && null!=service.getListViewAdapter()){
            adapter = service.getListViewAdapter();
            return;
        }
        else if (null == service ){
            //Log.d ("Reddit2Go createAdapter", "Service is null");
        }
        //set the adapter for the post list view
        adapter = new ArrayAdapter<Post>(getActivity(), R.layout.post_item, posts) {
            //called by the service when a Post becomes cached
            @Override
            public void add (Post newPost){
                if(getPosition(newPost)==-1){
                    super.add(newPost);
                    notifyDataSetChanged();
                    postList.invalidateViews();
                }
                else{
                    //item in the array already
                }
            }
            //ArrayAdapter function called by Android to get the individual views that make up its conainer object
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getActivity().getLayoutInflater().inflate(R.layout.post_item, null);
                }

                TextView postTitle;
                postTitle = (TextView) convertView.findViewById(R.id.post_title);

                TextView postDetail;
                postDetail = (TextView) convertView.findViewById(R.id.post_detail);

                TextView postScore;
                postScore = (TextView) convertView.findViewById(R.id.post_score);

                postTitle.setText(posts.get(position).title);
                postDetail.setText(posts.get(position).getDetail());
                postScore.setText(posts.get(position).getScore());

                return convertView;
            }
        };
        //set this newly generated adapter to the PostList
        postList.setAdapter(adapter);

        if(null!=service){
            //give the application a reference to this newly generated adapter, so it can update the view with cached posts
            service.adapterFromPostFrag((ArrayAdapter)postList.getAdapter());
        }
        else{
            //Log.d("Reddit2Go", "Service is null, no path for adapter");
        }

    }

    //get a Post that links to this url in its Permalink
    private Post getPost(String url) {
        Reddit2GoService service = Reddit2GoService.getRunningInstance();
        if(null!=service)
            return service.postFromURL(url);
        else {
            //Log.d("Reddit2Go - getPost()", "service is null");
            return null;
        }
    }

    //not used right now
    public ArrayAdapter<Post> getAdapter(){
        if(null!=adapter){
            return adapter;
        }
        createAdapter();
        return adapter;
    }

    //get an instance of the running PostFragment
    public static Fragment getInstance(){
        return instance;
    }

    //when this fragment becomes visible, update its views
    @Override
    public void onStart (){
        super.onStart();
        Reddit2GoService service = Reddit2GoService.getRunningInstance();
        if(null!=service){
            createAdapter();

            service.adapterFromPostFrag(adapter);
        }
        else{
            //Log.d("Reddit2Go", "Service is null, not updating the list onStart()");
        }
    }

    //when the fragement becomes visible to the user and is running, reset its adapter
    @Override
    public void onResume (){
        super.onStart();
        Reddit2GoService service = Reddit2GoService.getRunningInstance();
        if(null!=service){
            createAdapter();

            service.adapterFromPostFrag(adapter);
        }
        else{
            //Log.d("Reddit2Go", "Service is null, not updating the list onResume()");
        }
    }

 /*  //check to see if the service is running, and if so bind to it from http://stackoverflow.com/questions/4300291/example-communication-between-activity-and-service-using-messaging
    private void CheckIfServiceIsRunning() {
        //If the service is running when the activity starts, we want to automatically bind to it.
        if (Reddit2GoService.isRunning()) {
            doBindService();
        }
    }
    */
}
