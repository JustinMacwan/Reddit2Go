package com.example.andrew.reddit2go;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Andrew on 2015-02-06.
 */
public class RedditList {

    //Constants, added by Dan Morocz Feb 8, 20015 @8:21 PM
    private final String URL_TEMPLATE = "http://www.reddit.com/r/SUBREDDIT_NAME/" + ".json" + "?after=AFTER";
    private final String POST_TITLE_JSON_NAME = "title";
    private final String POST_URL_JSON_NAME = "url";
    private final String POST_COMMENTS_JSON_NAME = "num_comments";
    private final String POST_SCORE_JSON_NAME = "score";
    private final String POST_AUTHOR_JSON_NAME = "author";
    private final String POST_SUBREDDIT_JSON_NAME = "subreddit";
    public static final String POST_PERMALINK_JSON_NAME = "permalink";
    private final String POST_DOMAIN_JSON_NAME = "domain";
    private final String POST_ID_JSON_NAME = "id";

    String subreddit;
    String url;
    String after;

    RedditList (String str) {
        subreddit=str;
        after="";
        generateURL();
    }

    private void generateURL() {
        url = URL_TEMPLATE.replace("SUBREDDIT_NAME", subreddit);
        url = url.replace("AFTER", after);
    }

    List<Post> fetchPost() {
        String raw = ConnectReddit.readContents(url);
        List<Post> list = new ArrayList<Post>();

        try {
            JSONObject data = new JSONObject(raw).getJSONObject("data");
            JSONArray children = data.getJSONArray("children");

            after = data.getString("after");

            for (int i=0; i < children.length(); i++) {
                JSONObject cur=children.getJSONObject(i).getJSONObject("data");
                Post p=new Post();
                p.title=cur.optString(POST_TITLE_JSON_NAME);
                p.url=cur.optString(POST_URL_JSON_NAME);
                p.numOfComment=cur.optInt(POST_COMMENTS_JSON_NAME);
                p.score =cur.optInt(POST_SCORE_JSON_NAME);
                p.author=cur.optString(POST_AUTHOR_JSON_NAME);
                p.subreddit=cur.optString(POST_SUBREDDIT_JSON_NAME);
                p.permalink=cur.optString(POST_PERMALINK_JSON_NAME);
                p.domain=cur.optString(POST_DOMAIN_JSON_NAME);
                p.id=cur.optString(POST_ID_JSON_NAME);
                if(p.title!=null)
                    list.add(p);
            }

        } catch (Exception e) {
            Log.e("fetchPost()",e.toString());
        }
        return list;
    }

    List<Post> fetchMorePost(){
        generateURL();
        return fetchPost();
    }
}
