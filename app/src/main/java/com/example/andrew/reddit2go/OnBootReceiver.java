package com.example.andrew.reddit2go;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if ((intent.getAction() != null) && (intent.getAction().equals("android.intent.action.BOOT_COMPLETED") ||
                intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON" )))
        {
            Intent startIntent = new Intent(context, Reddit2GoService.class);
            context.startService(startIntent);
        }
    }
}