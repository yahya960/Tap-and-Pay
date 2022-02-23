package test.hcesdk.mpay;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.multidex.MultiDexApplication;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.gemalto.mfs.mwsdk.mobilegateway.MGConfigurationChangeReceiver;
import com.gemalto.mfs.mwsdk.mobilegateway.MobileGatewayError;
import com.gemalto.mfs.mwsdk.mobilegateway.MobileGatewayManager;
import com.gemalto.mfs.mwsdk.mobilegateway.listener.MGConfigurationResetListener;
import com.gemalto.mfs.mwsdk.payment.cdcvm.DeviceCVMPreEntryReceiver;
import com.gemalto.mfs.mwsdk.payment.sdkconfig.SDKDataController;
import com.gemalto.mfs.mwsdk.provisioning.ProvisioningServiceManager;
import com.gemalto.mfs.mwsdk.provisioning.exception.ExistingRetrySessionException;
import com.gemalto.mfs.mwsdk.provisioning.exception.NoSessionException;
import com.gemalto.mfs.mwsdk.provisioning.listener.PushServiceListener;
import com.gemalto.mfs.mwsdk.provisioning.model.KnownMessageCode;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServiceError;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServiceErrorCodes;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServiceMessage;
import com.gemalto.mfs.mwsdk.provisioning.sdkconfig.ProvisioningBusinessService;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;

import test.hcesdk.mpay.app.AppBuildConfigurations;
import test.hcesdk.mpay.app.AppConstants;
import test.hcesdk.mpay.payment.contactless.ContactlessPayListener;
import test.hcesdk.mpay.service.SDKService;
import test.hcesdk.mpay.util.AppExecutors;
import test.hcesdk.mpay.util.AppLogger;
import test.hcesdk.mpay.util.SDKHelper;
import test.hcesdk.mpay.util.SharedPreferenceUtils;
import com.gemalto.mfs.mwsdkservices.SDKLoaderService

public class App extends MultiDexApplication implements PushServiceListener {

    private static final String TAG = "App";
    private AppExecutors appExecutors;
    private ContactlessPayListener contactlessPayListener;
    private MGConfigurationChangeReceiver configurationChangeReceiver;
    private final int MAX_RETRY = 5;
    private int provisionRetryCounter = 0;
    private long paymentStartTime =0;
    private DeviceCVMPreEntryReceiver mPreEntryReceiver;

    @Override
    public void onCreate() {
        AppLogger.d(AppConstants.APP_TAG, "T0_Activity " + AppConstants.STARTED);
        AppLogger.d(AppConstants.APP_TAG, "T0_TILL_T2_Activity " + AppConstants.STARTED);
        AppLogger.d(AppConstants.APP_TAG, "App.onCreate " + AppConstants.STARTED);
        paymentStartTime =System.currentTimeMillis();
        super.onCreate();
        //init variables
        appExecutors = new AppExecutors();
        contactlessPayListener = new ContactlessPayListener(App.this);
        initFirebase();

        boolean isWipeNeeded = SharedPreferenceUtils.getNeedWipeAll(this);
        AppLogger.d(TAG, "WipeALL Triggered " + isWipeNeeded);
        if (isWipeNeeded) {
            try {
                SDKDataController.INSTANCE.wipeAll(this);
                MobileGatewayManager.INSTANCE.resetSDKStorage(this, new MGConfigurationResetListener() {
                    @Override
                    public void onSuccess() {
                        SharedPreferenceUtils.setNeedWipeAll(App.this, false);
                        Log.i(TAG," Wipe is success");
                        new Handler().postDelayed(App.this::initForNormal, 2500);
                        AppLogger.d(TAG, ".wipeAll2 - Trigger in onSuccess");
                    }

                    @Override

                    public void onError(MobileGatewayError mobileGatewayError) {
                        Log.i(TAG,"Wipe Got error "+mobileGatewayError.getMessage());
                    }
                });

            } catch (Exception e) {
                AppLogger.d(TAG, ".wipeAll2 - Trigger got some error " + e.getMessage());
            }
        } else {
            initForNormal();
        }
        AppLogger.d(AppConstants.APP_TAG, "App.onCreate " + AppConstants.ENDED);

    }

    private void initFirebase() {
        AppLogger.d(AppConstants.APP_TAG, "FirebaseApp.initializeApp " + AppConstants.STARTED);
        FirebaseApp.initializeApp(App.this);
        AppLogger.d(AppConstants.APP_TAG, "FirebaseApp.initializeApp " + AppConstants.ENDED);

    }

    private void initForNormal() {
       AppLogger.d(TAG, "initForNormal");
        //Initialize CPS SDK and MG SDK in App.onCreate() for Non-PFP case only
        if (AppBuildConfigurations.IS_PFP_ENABLED) {
            return;
        }

        if(AppBuildConfigurations.IS_ONETAP_PAYMENT){

            Thread initThread=new Thread(() -> {
                SDKHelper.InitCPSSDKCallback initCPSSDKCallback = new SDKHelper.InitCPSSDKCallback() {
                    @Override
                    public void doAction() {
                        //init MG SDK
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //Firebase API has limitation when there are multiple sender ID, the onNewToken is triggered only for default SENDER_ID.
                                // So it is prudent to check for updatePushToken regularly after SDK initialization as well.
                                //And it is prudent to check for updatePushToken just before card enrollment process begin as well.
                                SDKHelper.updateFirebaseToken(App.this);
                                SDKHelper.initMGSDKCall(getApplicationContext());
                                SDKHelper.performWalletSecureEnrollmentFlow(App.this);
                                registerPreFpEntry();
                                FirebaseInstanceId.getInstance().getToken();
                            }
                        },AppBuildConfigurations.INIT_MG_SDK_DELAY);
                    }
                };
                SDKHelper.initCPSSDK(App.this,initCPSSDKCallback,true);
            });
            initThread.start();

        }else {
            initCPSSDKFromService();
            AppLogger.d(TAG, "getToken");
            //trigger FCM token fetching to be able to get token readily
            FirebaseInstanceId.getInstance().getToken();
            //init MG SDK
            SDKHelper.initMGSDKCall(getApplicationContext());
        }
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

    private void initCPSSDKFromService() {
        AppLogger.d(TAG, "SDKLoaderReceiver start service");
        Intent service = new Intent(App.this, SDKService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            AppLogger.d(TAG, "SDKLoaderReceiver start foreground Service for OS Android 8 and above");
            //For Android O and above.
            startForegroundService(service);
        } else {
            AppLogger.d(TAG, "SDKLoaderReceiver start background Service for OS below Android 8");
            startService(service);
        }

    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(configurationChangeReceiver);
    }

    /**********************************************************/
    /*                Other utilities                         */

    /**********************************************************/
    public AppExecutors getAppExecutors() {
        return appExecutors;
    }

    public ContactlessPayListener getContactlessPayListener() {
        return contactlessPayListener;
    }


    /**********************************************************/
    /*                Push Service listener                   */

    /**********************************************************/
    @Override
    public void onError(ProvisioningServiceError provisioningServiceError) {
        Toast.makeText(this, provisioningServiceError.getErrorMessage(), Toast.LENGTH_SHORT).show();

        ProvisioningServiceErrorCodes errCode = provisioningServiceError.getSdkErrorCode();
        switch (errCode) {
            case COMMON_COMM_ERROR:
            case COMMON_NO_INTERNET:
                if (provisionRetryCounter == MAX_RETRY) {
                    provisionRetryCounter = 0;
                    AppLogger.e(TAG, "Error during provisioning session :" + provisioningServiceError.getErrorMessage());
                } else {
                    retryProvisioning();
                    provisionRetryCounter++;
                }
                break;
            default:
                provisionRetryCounter = 0;
                AppLogger.e(TAG, "Error during provisioning session :" + provisioningServiceError.getErrorMessage());
                break;
        }
    }

    @Override
    public void onUnsupportedPushContent(Bundle bundle) {
        Toast.makeText(this, "Impossible .onUnsupportedPushContent()", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onServerMessage(String s, ProvisioningServiceMessage provisioningServiceMessage) {
        String messageCode = provisioningServiceMessage.getMsgCode();
        if (messageCode == null) {
            AppLogger.e(TAG, "messageCode is null for some reason");
            return;
        }
        Log.e("ServerMessageCode",provisioningServiceMessage.getMsgCode().toString());
        Toast.makeText(this, provisioningServiceMessage.getMsgCode(), Toast.LENGTH_SHORT).show();
        switch (messageCode) {
            case KnownMessageCode.REQUEST_INSTALL_CARD:
                // 1st push notification for installing card
            case KnownMessageCode.REQUEST_REPLENISH_KEYS:
                // 2nd push notification for installing payment keys and subsequent replenishments
            case KnownMessageCode.REQUEST_RESUME_CARD:
                // card resumed
            case KnownMessageCode.REQUEST_SUSPEND_CARD:
                // card suspended
            case KnownMessageCode.REQUEST_RENEW_CARD:
                //token renewed (profile update)
            case KnownMessageCode.REQUEST_DELETE_CARD:
                //card deleted.
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(AppConstants.ACTION_RELOAD_CARDS));
                break;
            default:
                AppLogger.e(TAG, "Other events: Not handling");
        }
    }

    @Override
    public void onComplete() {
        Toast.makeText(this, "Completed processing message", Toast.LENGTH_SHORT).show();
    }

    private void retryProvisioning() {
        AppLogger.d(TAG, "AppPushServiceListener ->  attempting to retry job ");
        ProvisioningBusinessService provBs = ProvisioningServiceManager.getProvisioningBusinessService();
        if (provBs != null) {
            try {
                provBs.retrySession(this);
            } catch (ExistingRetrySessionException e) {
                e.printStackTrace();
            } catch (NoSessionException e) {
                e.printStackTrace();
            }
        }
    }

    public MGConfigurationChangeReceiver getConfigurationChangeReceiver() {
        return configurationChangeReceiver;
    }

    public void setConfigurationChangeReceiver(MGConfigurationChangeReceiver configurationChangeReceiver) {
        this.configurationChangeReceiver = configurationChangeReceiver;
    }

    public long getPaymentStartTime() {
        return paymentStartTime;
    }
}
