package com.example.andrew.reddit2go;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends FragmentActivity {


    private static MainActivity instance = null;
    // The login API URL
    private final String REDDIT_LOGIN_URL = "https://ssl.reddit.com/api/login";
    private Menu menu;

    //used when inserting the PostFragment into this Activity layout
    FragmentTransaction transaction;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //to avoid NetworkOnMainThreadException brute force
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        super.onCreate(savedInstanceState);

        Reddit2GoService service = Reddit2GoService.getRunningInstance();
        if(null==service){
            Context appContext = getApplicationContext();
            Intent startIntent = new Intent(appContext , Reddit2GoService.class);
            appContext.startService(startIntent);
        }
        //inflate the layout
        setContentView(R.layout.activity_main);
        //generate the ListView
        addFragment();
    }

    public static MainActivity getInstance(){
        return instance;
    }

    //generate the ListView PostFragment and insert it into this activity
    void addFragment() {
        transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragments_holder, PostFragment.newInstance("askreddit")).commit();
    }

    //When the user selects the options menu, inflate it
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        return true;
    }

    //Handle options item selection for login, and settings
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_login) {
            login();
            return true;
        }
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method allows a user to login to reddit via the login dialog
     */
    private void login() {

        //keep track of the current user
        String currentUsername = null;
        Reddit2GoService service = Reddit2GoService.getRunningInstance();
        if(null!=service){
            //get the currently logged in user from the service and null if no user is logged in
            currentUsername = service.getCurrentUser();
        }
        else{
            Log.d("Reddit2Go login", "Service is null, and there is no user logged in");
        }

        // proceed login if a user is not logged in
        if ( null == currentUsername ) {
            LayoutInflater inflater = this.getLayoutInflater();
            final View viewDialog = inflater.inflate(R.layout.activity_login, null);
            final AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(viewDialog)
                    .setTitle(R.string.sign_in)
                    .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();

            // Event listener of the login dialog
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {

                /**
                 * This override method shows the login dialog & listens an event from the dialog
                 * @param dialog
                 */
                @Override
                public void onShow(final DialogInterface dialog) {

                    Button btnOK = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                    btnOK.setOnClickListener(new View.OnClickListener() {

                        /**
                         * This override method checks whether username/password a user entered are correct
                         * if they are let the user logs in to reddit and closes the login dialog
                         * @param view
                         */
                        @Override
                        public void onClick(View view) {

                            // check username is not empty
                            if (((EditText) viewDialog.findViewById(R.id.username)).getText().toString().trim().length() > 0) {
                                if (login(((EditText) viewDialog.findViewById(R.id.username)).getText().toString().trim(),
                                        ((EditText) viewDialog.findViewById(R.id.password)).getText().toString().trim())) {
                                    //successful login, set the user name in the service
                                    Reddit2GoService service = Reddit2GoService.getRunningInstance();
                                    if(null!=service){
                                        service.setUsername(((EditText) viewDialog.findViewById(R.id.username)).getText().toString().trim());
                                    }
                                    else{
                                        Log.d("Reddit2Go onClick()", "Service is null");
                                    }
                                    dialog.dismiss();

                                    //change the menu option to logout
                                    MenuItem item = menu.findItem(R.id.action_login);
                                    item.setTitle(R.string.logout);
                                    //toast the user to feedback the login success
                                    Toast.makeText(getApplicationContext(), R.string.msg_success_login, Toast.LENGTH_SHORT).show();
                                }
                                else{
                                    //toast the user to feedback login failure
                                    Toast.makeText(getApplicationContext(), R.string.msg_failed_login, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });

                }
            });

            dialog.show();
        }
        else {logout();}
    }

    // This method creates a connection that allows you to POST data
    private HttpURLConnection getConnection(String url){
        URL u = null;
        try{
            u = new URL(url);
        }catch(MalformedURLException e){
            Log.d("Invalid URL", url);
            return null;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection)u.openConnection();
        } catch (IOException e) {
            Log.d("Unable to connect", url);
            return null;
        }
        // Timeout after 30 seconds
        connection.setReadTimeout(30000);
        // Allow POST data
        connection.setDoOutput(true);
        return connection;
    }

    // This method lets you POST data to the URL.
    private boolean writeToConnection(HttpURLConnection con, String data){
        try{
            PrintWriter pw=new PrintWriter(
                    new OutputStreamWriter(
                            con.getOutputStream()
                    )
            );
            pw.write(data);
            pw.close();
            return true;
        }catch(IOException e){
            Log.d("Unable to write", e.toString());
            return false;
        }
    }

    //log in to Reddit, fetch the cookie which can be used in subsequent calls to the Reddit API.
    private boolean login(String username, String password){
        HttpURLConnection connection = getConnection(REDDIT_LOGIN_URL);

        if(connection == null)
            return false;

        //Parameters that the API needs
        String data="user="+username+"&passwd="+password;

        if(!writeToConnection(connection, data))
            return false;

        String cookie=connection.getHeaderField("set-cookie");

        if(cookie==null)
            return false;

        cookie=cookie.split(";")[0];
        if(cookie.startsWith("reddit_first")){
            // Login failed
            Log.d("Error", "Unable to login.");
            return false;
        }else if(cookie.startsWith("reddit_session")){
            // Login success
            Log.d("Success", cookie);
            Reddit2GoService service = Reddit2GoService.getRunningInstance();
            if(null!=service){
                service.setRedditCookie(cookie);
            }
            else{
                Log.d("Reddit2Go - login()", "Service is null");
            }
            return true;
        }
        return false;
    }

    // Validate username if it is empty, notify a user to enter username
    private boolean ValidateUsername(EditText username) {
        //check if title is empty
        if (!hasContent(username)) {
            Toast.makeText(this, R.string.error_invalid_username, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    //does the EditText supplied as username contain text
    private boolean hasContent(EditText username) {
        boolean bHasContent = false;

        if (username.getText().toString().trim().length() > 0) {
            // Got content
            bHasContent = true;
        }
        return bHasContent;
    }

    //log the current user out
    public void logout(){
        Toast.makeText(getApplicationContext(), R.string.msg_success_logout, Toast.LENGTH_SHORT).show();
        Reddit2GoService service = Reddit2GoService.getRunningInstance();
        if(null != service) {
            service.setUsername(null);
        }
        else{
            Log.d("Reddit2Go - logout", "Service is null");
        }
        MenuItem item = menu.findItem(R.id.action_login);
        item.setTitle(R.string.sign_in);

//        SharedPreferences sharedpreferences = getSharedPreferences
//                (MainActivity.MyPREFERENCES, Context.MODE_PRIVATE);
//        Editor editor = sharedpreferences.edit();
//        editor.clear();
//        editor.commit();
//        moveTaskToBack(true);
//        Welcome.this.finish();
    }
}
