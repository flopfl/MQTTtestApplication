package com.example.mqtttestapplication;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Date;
import java.util.List;

public class MyIdleReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        Log.v("mqttLog","snooze");
       // sendBroadcast(22,"snoozeMode",context);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        Date dt = new Date();
        editor.putLong("snoozeDate", dt.getTime());
        int counter= preferences.getInt("dozeCount",0);
        counter++;
        editor.putInt("dozeCount",counter);
       editor.apply();


    }

    public void sendBroadcast(int m,String s,Context context){
        Intent intent = new Intent();
        intent.setAction("com.example.NOTIFICATIONBROADCSTACTION");
        putIntentExtras(intent,m);
        intent.putExtra("content",s);
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> infos = packageManager.queryBroadcastReceivers(intent, 0);
        for (ResolveInfo info : infos) {
            ComponentName cn = new ComponentName(info.activityInfo.packageName,
                    info.activityInfo.name);
            intent.setComponent(cn);
            context.sendBroadcast(intent);
        }
        //sendBroadcast(intent);

    }

    int notID=2223;
    public void putIntentExtras(Intent intent,int m){
        int notificationID=notID+m;
        intent.putExtra("contentIntent","com.example.notificatontestapp.notifyActivity");
        intent.putExtra("title","titleTest");
        intent.putExtra("content","itWorks "+m);
        intent.putExtra("priority", NotificationCompat.PRIORITY_HIGH);
        intent.putExtra("NotificationID",notificationID);
        intent.putExtra("autoCancel",true);
    }
}