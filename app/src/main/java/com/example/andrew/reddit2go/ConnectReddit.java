package com.example.andrew.reddit2go;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Andrew on 2015-02-05.
 */
public class ConnectReddit {

    public static HttpURLConnection getConnection(String url) {
        System.out.println("URL: " + url);
        HttpURLConnection hcon = null;
        try {
            hcon = (HttpURLConnection) new URL(url).openConnection();
            hcon.setReadTimeout(30000); // set to 30 seconds for timeout
            hcon.setRequestProperty("User-Agent", "Andrew");
        } catch (MalformedURLException e) {
            Log.e("getConnection()", "Invalid URL: " + e.toString());
        } catch (IOException e) {
            Log.e("getConnection()", "Could not connect: " + e.toString());
        }
        return hcon;
    }

    /**
     * Pulled from http://www.whycouch.com/2012/12/how-to-create-android-client-for-reddit_25.html
     *
     * A very handy utility method that reads the contents of a URL
     * and returns them as a String.
     *
     * @param url
     * @return
     */
    public static String readContents(String url){

        //Check if the cache contains data for this URL

        byte[] t=MyCache.read(url);
        String cached=null;
        if(t!=null) {
            cached=new String(t);
            t=null;
        }
        if(cached!=null) {
            Log.d("MSG","Using cache for "+url);
            return cached;
        }

        //The following will be executed only if the
        //cache did not contain data for this URL

        HttpURLConnection hcon=getConnection(url);
        if(hcon==null) return null;
        try{
            StringBuffer sb=new StringBuffer(8192);
            String tmp="";
            BufferedReader br=new BufferedReader(
                    new InputStreamReader(
                            hcon.getInputStream()
                    )
            );
            while((tmp=br.readLine())!=null)
                sb.append(tmp).append("\n");
            br.close();

            // We now add this data to the cache
            MyCache.write(url, sb.toString());
            return sb.toString();
        }catch(IOException e){
            Log.d("READ FAILED", e.toString());
            return null;
        }
    }

/*    public static String getContents(String url) {
        HttpURLConnection hcon = getConncection(url);
        if (hcon == null) return null;
        try {
            StringBuffer sb = new StringBuffer(8192);
            String tmp = "";
            BufferedReader br = new BufferedReader(new InputStreamReader(hcon.getInputStream()));
            while ((tmp = br.readLine()) != null)
                sb.append(tmp).append("\n");
            br.close();
            return sb.toString();
        } catch (IOException e) {
            Log.d("READ FAILED", e.toString());
            return null;
        }
    }
    */
}
