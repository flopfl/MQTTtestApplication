package com.example.mqtttestapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class alarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        Log.v("mqttLog","in alarm");
        Intent intent2 = new Intent(context, MqttService.class);
        context.startService(intent2);
    }
}