package com.example.mqtttestapplication;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    Button button;
    Button button2;


    PowerManager.WakeLock wakeLock;
    PowerManager.WakeLock startUpWakeLock;
    final String wakelockTag="com.example.mqtttestapplication::mqttWakeLockTag";
    final String startUpWakelockTag="com.example.mqtttestapplication::mqttStartUpWakeLockTag";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UtilPermissions.getAutostartPermission(false, this);


        PowerManager mgr = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(mgr.isIgnoringBatteryOptimizations(getPackageName())){
                Log.v("mqttLog","ignoring batterysaving");

            }else{
                Log.v("mqttLog","not ignoring batterysaving");
                startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:"+getPackageName())));
            }
        }else{
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:"+getPackageName())));
        }




        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //startService(intent);
                Intent intent = new Intent(getApplicationContext(), MqttService.class);
               // intent.putExtra("test",new String[] {"ssss","fff"});
                startService(intent);
             /*   PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                Log.v("mqttLog","powermanager");
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                        wakelockTag);
                Log.v("mqttLog","wakelock");
                wakeLock.acquire();
                Log.v("mqttLog","powermanager");
                startUpWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                        startUpWakelockTag);
                Log.v("mqttLog","wakelock");
                startUpWakeLock.acquire();*/
            }
        });
        button2 = findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               // wakeLock.release();
               // startUpWakeLock.release();
            }
        });

    }


}