package com.example.mqtttestapplication;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;


import androidx.core.app.NotificationCompat;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.lifecycle.MqttClientAutoReconnect;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static java.lang.Thread.interrupted;


public class MqttService extends Service {
    //Thread thread;
    Mqtt3AsyncClient client;
    int starts=0;
    int wakeLockCounter=0;
    String manufacturer = Build.MANUFACTURER;
    String model = Build.MODEL;
    PowerManager.WakeLock wakeLock;
    PowerManager.WakeLock startUpWakeLock;
    PowerManager.WakeLock pongWakeLock;
    final String wakelockTag="com.example.mqtttestapplication::mqttWakeLockTag";
    final String pongWakelockTag="com.example.mqtttestapplication::mqttPongWakeLockTag";
    final String startUpWakelockTag="com.example.mqtttestapplication::mqttStartUpWakeLockTag";
    boolean waiting=false;
    BroadcastReceiver bc;
    int receivedPongs=0;


    Messenger mMessenger;
    static final int MSG_RECONNECT = 1;
   // MyIdleReceiver br;
    //PowerManager powerManager;
   // Handler handler=new Handler();


    @Override
    public void onCreate() {
        Log.v("mqttLog","oncreate");
        if(wakeLock!=null && wakeLock.isHeld()){
            Log.v("mqttLog","create Released wakelock");
            wakeLock.release();
        }
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.v("mqttLog","onDestroy");
        if(wakeLock!=null && wakeLock.isHeld()){
            Log.v("mqttLog","destroy Released wakelock");
            wakeLock.release();
        }
        if(startUpWakeLock!=null && startUpWakeLock.isHeld()){
            Log.v("mqttLog","destroy Released wakelock");
            startUpWakeLock.release();
        }
        super.onDestroy();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {


        if(startUpWakeLock==null ){
            Log.v("mqttLog","start");
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            Log.v("mqttLog","powermanager");
            startUpWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    startUpWakelockTag);
            Log.v("mqttLog","wakelock");

        }
        if( !startUpWakeLock.isHeld()){
            startUpWakeLock.acquire();
            Log.v("mqttLog","acquired");
        }

        if(bc==null){
            bc= new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int resultCode = getResultCode();
                    Log.v("mqttLog","resultcode "+resultCode);
                    if(wakeLockCounter==1){
                        wakeLockCounter=wakeLockCounter-1;
                        wakeLock.release();
                        Log.v("mqttLog","released");
                    }else{
                        wakeLockCounter=wakeLockCounter-1;
                    }
                    Log.v("mqttLog","release counter="+wakeLockCounter);
                    waiting=false;

                }
            };
        }



        starts++;
        Log.v("mqttLog", "current onstartCommand " + starts);

        //############## added since working
        /*
        if(client!=null){
            try {
                client.disconnect().get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/
        //################## added since working
        client=null;

        buildClient();
        subscribe();
        client.connectWith().cleanSession(false)
                //  .keepAlive(120)
                // .noKeepAlive()
                .keepAlive(600)//10min
                .send()
                .whenComplete((connAck, throwable) -> {
                    if (throwable != null) {

                        Log.v("mqttLog", "connect failure -> release");
                        //startUpWakeLock.release();
                        Log.v("mqttLog", throwable.getMessage());
                       // wakeLock.release();
                        // handle failure
                    } else {
                        Log.v("mqttLog", "connect");

                    }
                });


        rescheduleAlarm(15);
      /*  if(br==null){
            Log.v("mqttLog", "starting receiver");
            br=new MyIdleReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
            this.registerReceiver(br, filter);
        }*/

        return START_STICKY;
    }

    public void buildClient(){
        client = Mqtt3Client.builder()
                .identifier(manufacturer + model)
              //  .automaticReconnectWithDefaultConfig()
                .addConnectedListener(new MqttClientConnectedListener() {
                    int id=starts;
                    @Override
                    public void onConnected(MqttClientConnectedContext context) {
                        Log.v("mqttLog", "connectedListener  "+ id);

                        if(startUpWakeLock.isHeld()&&id==starts){
                            startUpWakeLock.release();
                            Log.v("mqttLog", "startUpWakeLock released in connect");
                        }

                    }
                }).addDisconnectedListener(new MqttClientDisconnectedListener() {
                    int id=starts;
                    long maxDelay=4*60;
                    @Override
                    public void onDisconnected(MqttClientDisconnectedContext context) {

                        Log.v("mqttLog", "disconnectedListener  "+ id+" attempt "+context.getReconnector().getAttempts());
                        Log.v("mqttLog", context.getCause().getMessage());
                        Log.v("mqttLog", context.getSource().name());

                        if(starts==id){
                            if(startUpWakeLock.isHeld() && context.getReconnector().getAttempts()==2){
                                startUpWakeLock.release();
                                Log.v("mqttLog", "startUpWakeLock released in disconnect");
                            }
                            long delay=5 * context.getReconnector().getAttempts();
                            if(delay>maxDelay)
                                delay=maxDelay;
                            context.getReconnector()
                                    .reconnect(true)
                                    .delay(delay, TimeUnit.SECONDS);
                        }

                    }
                })
                .serverHost("192.168.0.89")//gögglingen
                //.serverHost("192.168.178.36")
                .serverPort(1883)

                .buildAsync();

    }

    public void subscribe(){

        client.subscribeWith()
                .topicFilter("testTopic2")
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback(msg -> {
                   // Log.v("mqttLog","null");
                    wakeLockCounter++;
                    if(wakeLockCounter==1 && (wakeLock==null || !wakeLock.isHeld())){
                        //Log.v("mqttLog","start");
                        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                       // Log.v("mqttLog","powermanager");
                        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                wakelockTag);
                      //  Log.v("mqttLog","wakelock");
                        wakeLock.acquire();
                        Log.v("mqttLog","acquired");
                    }
                    Log.v("mqttLog","acquire counter="+wakeLockCounter);
                    waiting=true;

                    //String message= new String(msg.getPayload().get().array());
                  //  Log.v("mqttLog", "message");
                    String message = "message from phone: " + manufacturer + "  " + model;
                    if (msg.getPayload().isPresent()) {
                      //  Log.v("mqttLog", "is present");
                        String s = StandardCharsets.UTF_8.decode(msg.getPayload().get()).toString();
                        message = message + s;
                        Log.v("mqttLog", message);
                        sendBroadcast(Integer.parseInt(s));

                    } else {
                        Log.v("mqttLog", "Message not present");
                    }
                    //#######
                   /* boolean idle=powerManager.isDeviceIdleMode();
                    if(idle){
                        message="in idle";
                    }else{
                        message="not idle";
                    }*/
                    //#######
                    client.toAsync().publishWith().topic("android").payload(message.getBytes()).send();
                })
                .send();
        client.toAsync().subscribeWith()
                .topicFilter("pingRequestAdress")
                .qos(MqttQos.AT_MOST_ONCE)
                .callback(msg -> {
                    //String message= new String(msg.getPayload().get().array());
                    if(pongWakeLock==null || !pongWakeLock.isHeld()){
                        //Log.v("mqttLog","start");
                        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                        // Log.v("mqttLog","powermanager");
                        pongWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                pongWakelockTag);
                        //  Log.v("mqttLog","wakelock");
                        pongWakeLock.acquire();
                        Log.v("mqttLog","acquired");

                    }
                    Log.v("mqttLog", "message");
                    String message = "Pong from phone: " + manufacturer + "  " + model;
                    Log.v("mqttLog", "pingRequest");
                    client.toAsync().publishWith().topic("PongResponse").payload(message.getBytes()).send();
                    receivedPongs++;
                    //##################################################### changed since working
                    if(receivedPongs==3)
                        rescheduleAlarm(15);
                    pongWakeLock.release();
                    Log.v("mqttLog","release");

                })
                .send();

    }

    public void rescheduleAlarm(int min){
        receivedPongs=0;
        Intent intent2 = new Intent(getApplicationContext(), MqttService.class);
        PendingIntent pendingIntent=PendingIntent.getService(getApplicationContext(),243,intent2,0);
        AlarmManager alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarm.cancel(pendingIntent);
        alarm.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + min*60*1000,pendingIntent);
    }



    public void sendBroadcast(int m){
        Intent intent = new Intent();
        intent.setAction("com.example.NOTIFICATIONBROADCSTACTION");
        putIntentExtras(intent,m);
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> infos = packageManager.queryBroadcastReceivers(intent, 0);
        Log.v("mqttLog", "receiverNumber:"+infos.size());
        for (ResolveInfo info : infos) {
            ComponentName cn = new ComponentName(info.activityInfo.packageName,
                    info.activityInfo.name);
            Log.v("mqttLog","activityInfo.packageName: "+ info.activityInfo.packageName+ "   info.activityInfo.name "+info.activityInfo.name);
            intent.setComponent(cn);
            //sendBroadcast(intent);


            sendOrderedBroadcast(intent, null, bc,null, MainActivity.RESULT_CANCELED,null,null);

        }
    }

    public void sendBroadcast(int m,String s){
        Intent intent = new Intent();
        intent.setAction("com.example.NOTIFICATIONBROADCSTACTION");
        putIntentExtras(intent,m);
        intent.putExtra("content",s);
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> infos = packageManager.queryBroadcastReceivers(intent, 0);
        for (ResolveInfo info : infos) {
            ComponentName cn = new ComponentName(info.activityInfo.packageName,
                    info.activityInfo.name);
            intent.setComponent(cn);
            sendBroadcast(intent);
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


    class IncomingHandler extends Handler {
        private Context applicationContext;

        IncomingHandler(Context context) {
            applicationContext = context.getApplicationContext();
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RECONNECT:
                    Log.v("mqttLogIPC","working");

                default:
                    super.handleMessage(msg);
            }
        }
    }



    @Override
    public IBinder onBind(Intent intent) {
        mMessenger = new Messenger(new IncomingHandler(this));
        return mMessenger.getBinder();
    }


}