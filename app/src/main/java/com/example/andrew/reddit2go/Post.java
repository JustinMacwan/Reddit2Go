package com.example.andrew.reddit2go;

import org.json.JSONArray;

/**
 * Created by Andrew on 2015-02-05.
 * Models the attributes and behaviours of a Reddit post.
 */
public class Post {

    //subreddit this post belongs to
    String subreddit;
    //title of the post
    String title;
    //author of the post
    String author;
    //total likes of this post
    int score;
    //comments on this post
    int numOfComment;
    //not including the host, link of this post
    String permalink;
    //has this post been cached
    private boolean hasCached;
    //has this post been read by the user
    private boolean isRead;
    //url of the post
    String url;
    //domain of the post
    String domain;
    //id of the post
    String id;

    //default constructor initializes this post, assumes has not been read
    public Post(){
        hasCached = false;
    }

    //return string details of this post
    String getDetail() {
        String detail = author + "\n" + numOfComment + " comments | " + subreddit;
        return detail;
    }

    //return the title of this post
    String getTitle() {
        return title;
    }

    //return the score of this post
    String getScore() {
        return Integer.toString(score);
    }

    //has this page been cached
    boolean getIsCached() { return hasCached; }

    //this page has cached, notify
    void setHasCached(boolean status) { hasCached=status;}

    //return the permalink of this post
    String getPermalink() { return permalink; }

    //this page has been read, notify
    void readPost() { isRead = true; }

    //return has this post been read
    boolean wasRead() { return isRead; }
}
