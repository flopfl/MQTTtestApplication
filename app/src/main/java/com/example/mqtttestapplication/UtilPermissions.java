package com.example.mqtttestapplication;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationManagerCompat;

public class UtilPermissions {

    /**
     * for autostart capability on some devices the user is required to give the app permissions in a security layer inbetween.
     * This is a List of a few intents for this layer inbetween of devices that might require this step.
     */
    private static final Intent[] POWERMANAGER_INTENTS = {
            new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            new Intent().setComponent(new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")),
            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
            new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")),
            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
            new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
            new Intent().setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
            new Intent().setComponent(new ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
            new Intent().setComponent(new ComponentName("com.htc.pitroad", "com.htc.pitroad.landingpage.activity.LandingPageActivity")),
            new Intent().setComponent(new ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.MainActivity")),
            new Intent().setComponent(new ComponentName("com.transsion.phonemanager", "com.itel.autobootmanager.activity.AutoBootMgrActivity"))
    };

    /**
     * requesting permission for NotificationListenerServices
     * @param context
     */
    public static void getNotificationListenerPermission(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (!NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.getPackageName())) {
                showExplanation("Notification Permissions", "This Permission is required for participation in Surveys. it gives the App the Permission to monitor your Notifications. Please enable it in the next screen for this App (BackgroundListenerTest)", new AlarmCallback() {
                    @Override
                    public void onSuccess() {
                        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                        context.startActivity(intent);
                    }
                    @Override
                    public void onFailure() {
                        Toast.makeText(context, "The app is disabled until you enable this option.", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onOptional() {

                    }
                },context);
            }
        }
    }

    /**
     * Interface used for Callbacks with 3 options
     */
    public interface AlarmCallback{
        void onSuccess();
        void onFailure();
        void onOptional();

    }

    /**
     * gets called on startup of App and will request autostartpermissions if first time after install
     * @param openedByMenu used to be able to start this manually from settings
     * @param context
     */
    public static void getAutostartPermission(Boolean openedByMenu,Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Log.v("NOL_first_installation",""+prefs.getBoolean("firstTime2", false));
        if (openedByMenu || !prefs.getBoolean("firstTime2", false)) {
            requestAutoStartPermission(context);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("firstTime2", true);
            editor.commit();
        }
    }

    /**
     * matches the intent supported by the device if contained in POWERMANAGER_INTENTS.
     * The Device specific message will be shown telling the user how to give permissions on his device
     * @param context
     */
    private static void  requestAutoStartPermission(Context context){
        for (int i=0;i<POWERMANAGER_INTENTS.length;i++){
            Intent intent= POWERMANAGER_INTENTS[i];
            if (context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                String message="to enable Autostart make sure the backgoundListenerTest has the according permissions. For some phones this is set per default";
                String title="Please enabel Autostart";
                switch(i){
                    case 0:
                        break;
                    case 2:
                        Log.v("NOL_powermanager","huawei");
                        title="Please enable Autostart";
                        message="to enable Autostart make sure the backgoundListenerTest tag is set to manual and not auto";
                        break;
                    case 11:
                        Log.v("NOL_powermanager","samsung");
                        title="Please enable Autostart";
                        message="to enable Autostart make sure the backgoundListenerTest has according permissions turned on. For most Samsung devices this is enabled by default.";
                        break;
                }
                Log.v("NOL_powermanager","in Intent "+i);
                AlarmCallback callback =new AlarmCallback() {
                    @Override
                    public void onSuccess() {
                        context.startActivity(intent);
                    }
                    @Override
                    public void onFailure() {}
                    @Override
                    public void onOptional() {}
                };
                showExplanation(title,message,callback,context);
                break;
            }
        }
    }

    public static void showExplanation(String title, String message, final AlarmCallback callback, Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        callback.onSuccess();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        callback.onFailure();
                    }
                });
        builder.create().show();
    }

    public static void showDeleteExplanation(String title,String message,final AlarmCallback callback,Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        callback.onOptional();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        callback.onFailure();
                    }
                }).setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                callback.onSuccess();
            }
        });
        builder.create().show();
    }
}

