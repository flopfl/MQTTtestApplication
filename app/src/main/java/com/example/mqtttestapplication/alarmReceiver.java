package com.example.mqtttestapplication;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import static android.content.Context.POWER_SERVICE;

public class alarmReceiver extends BroadcastReceiver {
    final String startUpWakelockTag="com.example.mqtttestapplication::mqttStartUpWakeLockTag";
    PowerManager.WakeLock startUpWakeLock;
    Context context2;
    @Override
    public void onReceive(Context context, Intent intent) {
        context2=context;
       /* if(startUpWakeLock==null || !startUpWakeLock.isHeld()){
            Log.v("mqttLog","start");
            PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
            Log.v("mqttLog","powermanager");
            startUpWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    startUpWakelockTag);
            Log.v("mqttLog","wakelock");
            startUpWakeLock.acquire();
            Log.v("mqttLog","acquired");
        }*/
        Log.v("mqttLog","acquired");
        Log.v("mqttLogAlarm","in alarm");
        Log.v("mqttLogAlarm",context.getPackageName());
        Intent intent2 = new Intent(context, MqttService.class);
        context.startService(intent2);

    }

    boolean bound=false;
    public void initIPCService(){
        if(!bound){
            Log.v("libraryApproch","trying to bind");
            bindToService();
        }else{
            Log.v("libraryApproch","connected");
            //sayHello();
            sendNot();
        }
    }

    /** Messenger for communicating with the service. */
    Messenger mService = null;


    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            Log.v("libraryApproch","connected");
            mService = new Messenger(service);
            bound = true;
            sendNot();
            // sayHello();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            Log.v("libraryApproch","disconnected");
            mService = null;
            bound = false;
        }
    };

    public void sendNot(){
        if (!bound) return;
        // Create and send a message to the service, using a supported 'what' value
        //NotiObject notiObject=new NotiObject("com.example.notificatontestapp.notifyActivity","titleTest","it works",NotificationCompat.PRIORITY_HIGH,true);
        Message msg = Message.obtain(null, 1);

        msg.setData(createBundle());
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public Bundle createBundle(){
        Bundle bundle=new Bundle();
        bundle.putString("contentIntent","com.example.notificatontestapp.notifyActivity");
        bundle.putString("title","titleTest");
        bundle.putString("content","itWorks");
        bundle.putInt("priority", NotificationCompat.PRIORITY_HIGH);
        bundle.putBoolean("autoCancel",true);
        return bundle;
    }

    public void bindToService(){
        Intent intent = new Intent();
        intent.setClassName("com.example.mqtttestapplication", "com.example.mqtttestapplication.MqttService");
        // intent.setClassName("com.example.notificatontestapp", "com.example.notificatontestapp.messangerService");
        // Bind to the service
        try{
            context2.bindService(intent, mConnection,
                    Context.BIND_AUTO_CREATE);
        }catch (Exception e){
            Log.v("library approach","securuity exception");
        }

    }
}