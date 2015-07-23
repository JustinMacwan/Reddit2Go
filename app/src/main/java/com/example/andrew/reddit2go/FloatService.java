package com.example.andrew.reddit2go;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Created by Justeen on 2015-02-26.
 */
public class FloatService extends Service {

    private FloatingActionMenu FloatingMenu;
    private FloatingActionButton FloatingButton;
    private SubActionButton SubButton;
    private TextView textView;
    private final IBinder mBinder = new LocalBinder();

    private FloatingActionButton ActionButton;

    private FloatingActionMenu circleMenu;

    final Animation animation = new AlphaAnimation(1, 0); // Change alpha from fully visible to invisible

    private boolean serviceWillBeDismissed;
    //keep track of an instance to communicate with
    private static FloatService floatingInstance;

    public class LocalBinder extends Binder {
        FloatService getService() {
            // Return this instance of LocalService so clients can call public methods
            return FloatService.this;
        }
    }

    @Override public IBinder onBind(Intent intent) {
        // Not used
        return mBinder;
    }

    @Override public void onCreate() {
        super.onCreate();
        serviceWillBeDismissed = false;
        floatingInstance = FloatService.this;

        textView = new TextView(this);
        textView.setText("0");
        textView.setTypeface(null, Typeface.BOLD);
        textView.setTextColor(Color.GRAY);
        WindowManager.LayoutParams params = FloatingActionButton.Builder.getDefaultSystemWindowParams(this);
        FrameLayout.LayoutParams acParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);

        ActionButton = new FloatingActionButton.Builder(this)
                .setSystemOverlay(true)
                .setContentView(textView)
                .setLayoutParams(params)
                .setPosition(FloatingButton.POSITION_TOP_CENTER)
                .build();

        SubActionButton.Builder rLSubBuilder = new SubActionButton.Builder(this);
        FrameLayout.LayoutParams tvParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);

        TextView rlIcon1 = new TextView(this);
        TextView rlIcon2 = new TextView(this);
        TextView rlIcon3 = new TextView(this);

        rlIcon1.setText("Open App");
        rlIcon1.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        rlIcon1.setTextSize(10);
        rlIcon2.setText("Stop Cache");
        rlIcon2.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        rlIcon2.setTextSize(10);
        rlIcon3.setText("Hide");
        rlIcon3.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        rlIcon3.setTextSize(10);

        SubActionButton r1 = rLSubBuilder.setContentView(rlIcon1, tvParams).build();
        SubActionButton r2 = rLSubBuilder.setContentView(rlIcon2, tvParams).build();
        SubActionButton r3 = rLSubBuilder.setContentView(rlIcon3, tvParams).build();


        circleMenu = new FloatingActionMenu.Builder(this, true)
                .addSubActionView(r1, r1.getLayoutParams().width, r1.getLayoutParams().height)
                .addSubActionView(r2, r2.getLayoutParams().width, r2.getLayoutParams().height)
                .addSubActionView(r3, r3.getLayoutParams().width, r3.getLayoutParams().height)
                .setStartAngle(0)
                .setEndAngle(180)
                .attachTo(ActionButton)
                .build();

        r1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent activityStart = new Intent(getApplicationContext(), MainActivity.class);
                activityStart.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(activityStart);
            }
        });

        r2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Dan : add your code here to stop the caching
            }
        });

        r3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serviceWillBeDismissed = true;
                circleMenu.close(true);
            }
        });



        circleMenu.setStateChangeListener(new FloatingActionMenu.MenuStateChangeListener() {
            @Override
            public void onMenuOpened(FloatingActionMenu menu) {

            }

            @Override
            public void onMenuClosed(FloatingActionMenu menu) {
                if (serviceWillBeDismissed) {
                    FloatService.this.stopSelf();
                    serviceWillBeDismissed = false;
                }
            }

        });
    }

    public void toogleFlashOn(){
        animation.setDuration(500); // duration - half a second
        animation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
        animation.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
        animation.setRepeatMode(Animation.REVERSE); // Reverse animation at the end so the button will fade back in
        textView.startAnimation(animation);
    }

    public void toogleFlashOff(){
        textView.clearAnimation();
    }

            /**  try {
            textView.setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;
                private final int MAX_CLICK_DURATION = 200;
                private long startClickTime;


                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            startClickTime = Calendar.getInstance().getTimeInMillis();
                            return true;
                        case MotionEvent.ACTION_UP:
                            long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                            //if this is not a move, consider it a click and start the activity to see the posts
                            if(clickDuration < MAX_CLICK_DURATION) {
                                Intent activityStart = new Intent(getApplicationContext(), MainActivity.class);
                                activityStart.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(activityStart);
                            }
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            ActionButton.updateViewLayout(textView, params);
                            return true;
                    }
                    return false;
                }
            });
        } catch (Exception e) {
            // TODO: handle exception
        }*/

    @Override
    public int onStartCommand( Intent intent , int flags , int startId ) {
        super.onStartCommand(intent, flags, startId);

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (textView != null)
        floatingInstance = null;
        if(circleMenu != null && circleMenu.isOpen()) circleMenu.close(false);
        if(ActionButton != null) ActionButton.detach();
    }

    //called to update the number on this ChatHead
    public void updateUnreadNum(int unreadPosts){
        if (null != textView) {
            Log.d("Reddit2Go", "number of posts is: " + unreadPosts);
            textView.setText(""+unreadPosts);
        }
    }

    //called to update the list of available networks and the number on this ChatHead
    public void updateUnreadNum(int unreadPosts, String networkList){
        if (null != textView) {
            Log.d("Reddit2Go", "number of posts is: " + unreadPosts);
            //textView.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            textView.setGravity(Gravity.CENTER);
            textView.setText(""+unreadPosts+" Cached\n" + networkList);
        }
    }

    //get a reference to a running instance of this service
    public static FloatService getFloatingInstance(){
        return floatingInstance;
    }
}