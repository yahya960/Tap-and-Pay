package test.hcesdk.mpay.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.gemalto.mfs.mwsdk.payment.cdcvm.DeviceCVMPreEntryReceiver;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKController;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKServiceState;

import test.hcesdk.mpay.MainActivity;
import test.hcesdk.mpay.R;
import test.hcesdk.mpay.util.AppLogger;
import test.hcesdk.mpay.util.SDKHelper;

public class SDKService extends Service {


    private int FOREGROUND_NOTIFICATION_ID = 7;

    private DeviceCVMPreEntryReceiver mPreEntryReceiver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AppLogger.d("Service", "onStartCommand");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(FOREGROUND_NOTIFICATION_ID, buildNotification());
        }
        SDKHelper.InitCPSSDKCallback initCPSSDKCallback = new SDKHelper.InitCPSSDKCallback() {
            @Override
            public void doAction() {
                //This is very important to do if you want one-tap behaviour
                //Firebase API has limitation when there are multiple sender ID, the onNewToken is triggered only for default SENDER_ID.
                // So it is prudent to check for updatePushToken regularly after SDK initialization as well.
                //And it is prudent to check for updatePushToken just before card enrollment process begin as well.
                registerPreFpEntry();
                SDKHelper.updateFirebaseToken(SDKService.this);
                SDKHelper.initMGSDKCall(SDKService.this);
                SDKHelper.performWalletSecureEnrollmentFlow(SDKService.this);
            }
        };
        SDKHelper.initCPSSDK(SDKService.this,initCPSSDKCallback,true);
        return START_STICKY;
    }

    private  void registerPreFpEntry() {
        AppLogger.d("Service", "registerPreFpEntry");
        if (mPreEntryReceiver != null) {
            unregisterReceiver(mPreEntryReceiver);
            mPreEntryReceiver = null;
        }
        IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
        mPreEntryReceiver = new DeviceCVMPreEntryReceiver();
        mPreEntryReceiver.init();
        registerReceiver(mPreEntryReceiver, filter);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPreEntryReceiver != null) {
            unregisterReceiver(mPreEntryReceiver);
        }
        mPreEntryReceiver = null;
    }

    private Notification buildNotification() {
        AppLogger.d("Service", "Notification is initialized in foreground");
        String CHANNEL_ID = "Payment Service";
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.notification_service_channel),
                    NotificationManager.IMPORTANCE_HIGH);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(this);
        }
        builder.setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_stat_card)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.foreground_service_message))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
        return builder.build();
    }

}
