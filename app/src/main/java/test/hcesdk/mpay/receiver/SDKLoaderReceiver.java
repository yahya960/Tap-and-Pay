package test.hcesdk.mpay.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import test.hcesdk.mpay.app.AppBuildConfigurations;
import test.hcesdk.mpay.service.SDKService;
import test.hcesdk.mpay.util.AppLogger;

public class SDKLoaderReceiver extends BroadcastReceiver {

    private static final String TAG = SDKLoaderReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        AppLogger.d(TAG, "SDKLoaderReceiver start service");
        Intent service = new Intent(context, SDKService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            AppLogger.d(TAG, "SDKLoaderReceiver start foregorund Service for OS Android 8 and above");
            //For Android O and above.
            context.startForegroundService(service);
        } else {
            AppLogger.d(TAG, "SDKLoaderReceiver start background Service for OS below Android 8");
            context.startService(service);
        }


    }
}