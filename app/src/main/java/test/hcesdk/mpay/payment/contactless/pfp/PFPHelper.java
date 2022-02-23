package test.hcesdk.mpay.payment.contactless.pfp;

import android.content.Context;
import android.os.Bundle;
import android.os.Process;
import android.widget.Toast;

import com.gemalto.mfs.mwsdk.provisioning.ProvisioningServiceManager;
import com.gemalto.mfs.mwsdk.provisioning.listener.PushServiceListener;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServiceError;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServiceMessage;
import com.gemalto.mfs.mwsdk.provisioning.sdkconfig.ProvisioningBusinessService;
import com.gemalto.mfs.mwsdk.sdkconfig.AndroidContextResolver;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKController;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKServiceState;
import com.google.firebase.iid.FirebaseInstanceId;

import test.hcesdk.mpay.R;
import test.hcesdk.mpay.app.AppBuildConfigurations;
import test.hcesdk.mpay.app.AppConstants;
import test.hcesdk.mpay.util.AppLogger;
import test.hcesdk.mpay.util.SDKHelper;

public enum PFPHelper {
    //  ## PFP Based Payment flow ####
    //  Major changes are:
    //  * No foreground service => first tap is handled by the PFP
    //  * There appears prompt for authentication WITH transaction info
    //  * User can change card for the second tap AFTER the authentication

    INSTANCE;

    private static final String TAG = PFPHelper.class.getSimpleName();

    public void initSDKs(final Context context, final boolean isUIUpdateNeeded) {
        AppLogger.d(AppConstants.APP_TAG, "initSDKsForPFP");
        SDKHelper.InitCPSSDKCallback initCPSSDKCallback = new SDKHelper.InitCPSSDKCallback() {
            @Override
            public void doAction() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
                        //Firebase API has limitation when there are multiple sender ID, the onNewToken is triggered only for default SENDER_ID.
                        // So it is prudent to check for updatePushToken regularly after SDK initialization as well.
                        //And it is prudent to check for updatePushToken just before card enrollment process begin as well.
                        SDKHelper.updateFirebaseToken(context);
                        //init MG SDK
                        SDKHelper.initMGSDKCall(context);

                        //perform Wallet Secure Enrollment
                        SDKHelper.performWalletSecureEnrollmentFlow(context);
                    }
                }).start();
            }
        };
        SDKHelper.initCPSSDK(context, initCPSSDKCallback,isUIUpdateNeeded);

    }

}


