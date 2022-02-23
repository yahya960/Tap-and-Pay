package test.hcesdk.mpay.service;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardManager;
import com.gemalto.mfs.mwsdk.provisioning.ProvisioningServiceManager;
import com.gemalto.mfs.mwsdk.provisioning.sdkconfig.ProvisioningBusinessService;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.util.Map;

import androidx.annotation.NonNull;
import test.hcesdk.mpay.App;
import test.hcesdk.mpay.payment.contactless.pfp.PFPHelper;
import test.hcesdk.mpay.util.AppLogger;
import test.hcesdk.mpay.util.Constants;
import test.hcesdk.mpay.util.SDKHelper;
import test.hcesdk.mpay.util.SharedPreferenceUtils;
import test.hcesdk.mpay.util.TokenReplenishmentRequestor;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = MyFirebaseMessagingService.class.getSimpleName();
    private static final String FIREBASE_ID = "firebase_id";


    @Override
    public void onNewToken(@NonNull String s) {

        super.onNewToken(s);
        Log.i(TAG,"Token Refresh is "+s);
       SharedPreferenceUtils.setFirebaseId(this,s);

        //Firebase API has limitation when there are multiple sender ID, the onNewToken is triggered only for default SENDER_ID.
        // So it is prudent to check for updatePushToken regularly after SDK initialization as well.
        //And it is prudent to check for updatePushToken just before card enrollment process begin as well.
        SDKHelper.updateFirebaseToken(this.getApplicationContext());
    }



    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        AppLogger.d(TAG, remoteMessage.getData().toString());

        PFPHelper.INSTANCE.initSDKs(MyFirebaseMessagingService.
                this.getApplicationContext(),false);


        super.onMessageReceived(remoteMessage);

        Bundle bundle = new Bundle();

        Map<String, String> data = remoteMessage.getData();
        if (data == null) {
            //We are only handling data messages from FCM.
            //Not interested in other types of messages.
            return;
        }

        String sender = "";
        String action = "";
        String digitalCardID = "";
        if (!data.isEmpty()) {
            for (String key : data.keySet()) {
                AppLogger.d(TAG, key + " ---|--- " + data.get(key));

                if (null != data.get(key)) {
                    bundle.putString(key, data.get(key));
                    if (key.equalsIgnoreCase("sender")) {
                        sender = data.get(key);
                    }
                    if (key.equalsIgnoreCase("action")) {
                        action = data.get(key);
                    }
                    if (key.equalsIgnoreCase("digitalCardID")) {
                        digitalCardID = data.get(key);
                    }

                }

            }
        }
        if (sender.equalsIgnoreCase("CPS")) {
            final String isPushNotiDisabled = SharedPreferenceUtils.isPushNotiDisabled(this.getApplicationContext());
            Log.i(TAG, "action "+ action + "ddigitalCardID "+ digitalCardID);
            if (isPushNotiDisabled.equals("") || isPushNotiDisabled.isEmpty()){
                Log.i(TAG, "action "+ action + "ddigitalCardID "+ digitalCardID);
                ProvisioningBusinessService provService = ProvisioningServiceManager.getProvisioningBusinessService();
                provService.processIncomingMessage(bundle, (App) getApplication());
            }

        } else if ("MG".equalsIgnoreCase(sender)) {
            if (action != null && action.equalsIgnoreCase("MG:ReplenishmentNeededNotification")) {
                if (digitalCardID != null && !digitalCardID.isEmpty()) {
                    final String tokenizedCardId = DigitalizedCardManager.getTokenizedCardId(digitalCardID);
                    if (tokenizedCardId != null && !tokenizedCardId.isEmpty()) {
                        TokenReplenishmentRequestor.forceReplenish(tokenizedCardId);
                    }
                }
            }
        } else if (sender.equalsIgnoreCase("TNS")) {

        }

    }
}
