package com.example.mqtttestapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RebootReceiver extends BroadcastReceiver {
    private static final String TAG = "mqttLog";
    @Override
    public void onReceive(Context context, Intent intent) {
        StringBuilder sb = new StringBuilder();
        sb.append("Action: " + intent.getAction() + "\n");
        sb.append("URI: " + intent.toUri(Intent.URI_INTENT_SCHEME).toString() + "\n");
        String log = sb.toString();
        Log.d(TAG, log);
        Intent serviceIntent = new Intent(context, MqttService.class);
        intent.putExtra("test",new String[] {"ssss","fff"});
        context.startService(serviceIntent);

    }
}