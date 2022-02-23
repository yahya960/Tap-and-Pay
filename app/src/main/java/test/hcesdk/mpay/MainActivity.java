package test.hcesdk.mpay;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.cardemulation.CardEmulation;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.gemalto.mfs.mwsdk.cdcvm.BiometricsSupport;
import com.gemalto.mfs.mwsdk.cdcvm.DeviceCVMEligibilityChecker;
import com.gemalto.mfs.mwsdk.cdcvm.DeviceCVMEligibilityResult;
import com.gemalto.mfs.mwsdk.cdcvm.DeviceKeyguardSupport;
import com.gemalto.mfs.mwsdk.dcm.AbstractWalletPinService;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCard;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardDetails;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardErrorCodes;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardManager;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardState;
import com.gemalto.mfs.mwsdk.dcm.PaymentType;
import com.gemalto.mfs.mwsdk.dcm.TokenSyncError;
import com.gemalto.mfs.mwsdk.dcm.TokenSyncListener;
import com.gemalto.mfs.mwsdk.dcm.TokenSyncStatus;
import com.gemalto.mfs.mwsdk.dcm.WalletPinErrorCode;
import com.gemalto.mfs.mwsdk.dcm.WalletPinEventListener;
import com.gemalto.mfs.mwsdk.dcm.WalletPinManager;
import com.gemalto.mfs.mwsdk.dcm.cdcvm.DeviceCVMManager;
import com.gemalto.mfs.mwsdk.dcm.exception.WalletPinException;
import com.gemalto.mfs.mwsdk.exception.DeviceCVMException;
import com.gemalto.mfs.mwsdk.mobilegateway.MobileGatewayError;
import com.gemalto.mfs.mwsdk.mobilegateway.MobileGatewayManager;
import com.gemalto.mfs.mwsdk.mobilegateway.listener.MGCardLifecycleEventListener;
import com.gemalto.mfs.mwsdk.payment.CHVerificationMethod;
import com.gemalto.mfs.mwsdk.payment.PaymentBusinessManager;
import com.gemalto.mfs.mwsdk.payment.PaymentBusinessService;
import com.gemalto.mfs.mwsdk.payment.experience.PaymentExperience;
import com.gemalto.mfs.mwsdk.payment.experience.PaymentExperienceSettings;
import com.gemalto.mfs.mwsdk.provisioning.ProvisioningServiceManager;
import com.gemalto.mfs.mwsdk.provisioning.listener.AccessTokenListener;
import com.gemalto.mfs.mwsdk.provisioning.model.GetAccessTokenMode;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServiceError;
import com.gemalto.mfs.mwsdk.provisioning.model.WalletSecureEnrollmentState;
import com.gemalto.mfs.mwsdk.provisioning.sdkconfig.ProvisioningBusinessService;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKController;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKServiceState;
import com.gemalto.mfs.mwsdk.utils.async.AbstractAsyncHandler;
import com.gemalto.mfs.mwsdk.utils.async.AsyncResult;
import com.gemalto.mfs.mwsdk.utils.async.AsyncToken;
import com.gemalto.mfs.mwsdk.utils.chcodeverifier.CHCodeVerifier;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.noowenz.customdatetimepicker.CustomDateTimePicker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import test.hcesdk.mpay.about.AboutActivity;
import test.hcesdk.mpay.addCard.AddCardActivity;
import test.hcesdk.mpay.app.AppBuildConfigurations;
import test.hcesdk.mpay.app.AppConstants;
import test.hcesdk.mpay.dsrp.DsrpActivity;
import test.hcesdk.mpay.enrollment.EnrollmentActivity;
import test.hcesdk.mpay.history.HistoryActivity;
import test.hcesdk.mpay.model.MyDigitalCard;
import test.hcesdk.mpay.payment.contactless.ContactlessPayListener;
import test.hcesdk.mpay.payment.contactless.cardsettings.CardSettingsActivity;
import test.hcesdk.mpay.payment.contactless.pfp.PFPHelper;
import test.hcesdk.mpay.payment.qr.PaymentQRActivity;
import test.hcesdk.mpay.paymentexperience.PaymentExperienceSettingActivity;
import test.hcesdk.mpay.util.AppExecutors;
import test.hcesdk.mpay.util.AppLogger;
import test.hcesdk.mpay.util.CardListAdapter;
import test.hcesdk.mpay.util.Constants;
import test.hcesdk.mpay.util.DialogHelper;
import test.hcesdk.mpay.util.SDKHelper;
import test.hcesdk.mpay.util.SecureInputType;
import test.hcesdk.mpay.util.SharedPreferenceUtils;
import test.hcesdk.mpay.util.TokenReplenishmentRequestor;

import static android.nfc.cardemulation.CardEmulation.ACTION_CHANGE_DEFAULT;
import static android.nfc.cardemulation.CardEmulation.EXTRA_CATEGORY;
import static android.nfc.cardemulation.CardEmulation.EXTRA_SERVICE_COMPONENT;

public class MainActivity extends AppCompatActivity implements CardListAdapter.CardListAdapterCallback{

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int STORAGE_COMPONENT_ERROR = 1012;
    public static final String STORAGE_COMPONENT_EXCEPTION_KEY ="SECURE_STORAGE_ERROR";

    private FloatingActionButton fabAdd;
    private AppExecutors appExecutors;
    private View progress_layout;
    private View key_pad_fragment;
    private Button btnTokenSync, btnmockTimeStamp, btnmockTimeSlot;
    private Button btnRemoveTimeStamp, btnRemoveTimeslot;
    private TextView lastTimeStamp, nextTimeSlot;

    private static final int DIALOG_INPUT_TIMESTAMP = 0;
    private static final int DIALOG_INPUT_SLOT = 1;

    private RecyclerView recyclerView;
    private MySDKInitReceiver receiver = new MySDKInitReceiver();
    private CoordinatorLayout coordinatorLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ContactlessPayListener payListener;
    private String selectedItem;
    private List<String> tokenSyncCardID;
    // This is the token ID of the default card set in the list view.
    // Use cases such as pre-auth(Wallet mode) and card change between payments may change the default card used for payment
    // The application will set it back to this one.


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appExecutors = ((App) getApplication()).getAppExecutors();

        fabAdd = findViewById(R.id.fab);
        recyclerView = findViewById(R.id.recyclerView);
        progress_layout = findViewById(R.id.progress_layout);
        key_pad_fragment = findViewById(R.id.key_pad_fragment);
        coordinatorLayout = findViewById(R.id.mainLayout);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        lastTimeStamp = findViewById(R.id.lastSyncTime);
        nextTimeSlot = findViewById(R.id.nextSlot);
        btnTokenSync = findViewById(R.id.btn_tokenSync);
        btnmockTimeSlot = findViewById(R.id.btn_mocktimeslot);
        btnmockTimeStamp = findViewById(R.id.btn_mocktimestamp);
        btnRemoveTimeStamp = findViewById(R.id.btn_removemocktimestamp);
        btnRemoveTimeslot = findViewById(R.id.btn_removemocktimeslot);
        payListener = ((App) getApplication()).getContactlessPayListener();
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        fabAdd.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, AddCardActivity.class)));
        fabAdd.hide(); //This is to avoid showing add button when Initialization failed before it comes here
        if (SDKController.getInstance().getSDKServiceState() == SDKServiceState.STATE_INITIALIZING_IN_PROGRESS) {
            toggleProgress(true);
            fabAdd.show();
            AppLogger.d(TAG, "SDK not initialized");
        } else if (ProvisioningServiceManager.getWalletSecureEnrollmentBusinessService().getState() == WalletSecureEnrollmentState.WSE_REQUIRED) {
            AppLogger.d(TAG, "WSE REQUIRED");
        }

        if (SDKController.getInstance().getSDKServiceState() == SDKServiceState.STATE_INITIALIZED){
            fabAdd.show();
        }

        //Set the custom toolbar to the App
        Toolbar myToolbar = findViewById(R.id.topToolBar);
        setToolbarProperties(myToolbar);


        //Need to check why setting this, makes the menu disappear
        //and because of this test.hcesdk.mpay.addCard.AddCardActivity.onCreate line 50,51 needs to be commented.
        // setSupportActionBar(myToolbar);
        swipeRefreshLayout.setOnRefreshListener(this::loadCards);
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_dark, android.R.color.holo_green_dark, android.R.color.holo_orange_dark, android.R.color.holo_red_dark);

        //check tap and pay
        checkTapAndPay();
        //init for PFP or for normal contactless wallet
        if ((PaymentExperienceSettings.getPaymentExperience(MainActivity.this) == PaymentExperience.TWO_TAP_ALWAYS) && AppBuildConfigurations.IS_PFP_ENABLED) {
            AppLogger.d(TAG, "PFPHelper init");
            PFPHelper.INSTANCE.initSDKs(MainActivity.this, true);

            //check the benchmark flavour from shared prefs
            if (SharedPreferenceUtils.getBenchmark(getApplicationContext()) != null) {
                Snackbar snackbar = Snackbar.make(coordinatorLayout, "Benchmark Selected" +
                                SharedPreferenceUtils.getBenchmark(getApplicationContext()),
                        Snackbar.LENGTH_LONG);
                snackbar.show();
            } else {
                AppLogger.d(TAG, "SDKHelper init");
                initCPSSDK();
            }
        }
        btnTokenSync.setOnClickListener(v ->{
            triggerTokenSync();
        });

        btnmockTimeStamp.setOnClickListener(v ->{
            buildDateTimePicker(DIALOG_INPUT_TIMESTAMP);
        });

        btnmockTimeSlot.setOnClickListener(v ->{
            buildDateTimePicker(DIALOG_INPUT_SLOT);
        });

        btnRemoveTimeStamp.setOnClickListener(v ->{
            removeTimeStamp();
        });

        btnRemoveTimeslot.setOnClickListener(v ->{
            removeTimeSlot();
        });
    }

    private void initCPSSDK(){
        SDKHelper.InitCPSSDKCallback initCPSSDKCallback = () -> {
            //Firebase API has limitation when there are multiple sender ID, the onNewToken is triggered only for default SENDER_ID.
            // So it is prudent to check for updatePushToken regularly after SDK initialization as well.
            //And it is prudent to check for updatePushToken just before card enrollment process begin as well.

            new Thread(() -> {
                Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
                SDKHelper.updateFirebaseToken(MainActivity.this);
                SDKHelper.initMGSDKCall(MainActivity.this);
                SDKHelper.performWalletSecureEnrollmentFlow(MainActivity.this);
            }).start();

        };SDKHelper.initCPSSDK(MainActivity.this, initCPSSDKCallback, true);
    }

    private void triggerTokenSync(){
        if(DigitalizedCardManager.isTokenSyncAllowed()){

            DigitalizedCardManager.invokeTokenSync(new TokenSyncListener() {
                @Override
                public void onStarted() {
                    Log.d(TAG, "TokenSync OnStarted!");
                    showToastMessage("TokenSync OnStarted!");
                    tokenSyncCardID = new ArrayList<>();
                }

                @Override
                public void onCompleted() {
                    Log.d(TAG, "TokenSync OnCompleted!");
                    showToastMessage("TokenSync OnCompleted!");
                    //SharedPreferenceUtils.clearDisablePushNoti(getApplicationContext());
                    loadCards();
                    updateSchedulingDisplay();
                }

                @Override
                public void onProgressUpdate(String s, TokenSyncStatus tokenSyncStatus, DigitalizedCardState digitalizedCardState) {
                    Log.d(TAG, "TokenSync OnProgressUpdate!: TokenID :" + s );
                    Log.d(TAG, "TokenSync OnProgressUpdate!: TokenSyncStatus " + tokenSyncStatus);
                    Log.d(TAG, "TokenSync OnProgressUpdate!: DigitalCardState: " + digitalizedCardState);
                    showToastMessage("TokenSync onProgressUpdate!: TokenID: " + s +  "TokenSyncStatus " + tokenSyncStatus + "DigitalCardState " + digitalizedCardState);
                }

                @Override
                public void onCardDeleted(String s) {
                    Log.d(TAG, "TokenSync onCardDeleted!: TokenID: " + s);
                    showToastMessage("TokenSync onCardDeleted!: TokenID: " + s);
                }

                @Override
                public void onError(TokenSyncError tokenSyncError) {
                    Log.d(TAG, "TokenSyncError! " + tokenSyncError.getMessage() + tokenSyncError.getErrorCode());
                    showToastMessage("TokenSyncError! " + tokenSyncError.getMessage() + tokenSyncError.getErrorCode());
                    updateSchedulingDisplay();
                }
            });
        }else{
            Log.d(TAG,"TokenSync is not Allowed!");
            showToastMessage("TokenSync is not Allowed!");
            updateSchedulingDisplay();
        }
    }

    //Helper method to set Toolbar menu and listener
    private void setToolbarProperties (Toolbar myToolbar) {

        myToolbar.setTitle(R.string.app_name);
        myToolbar.inflateMenu(R.menu.toptoolbar);
        MenuItem menuItem_webUI = myToolbar.getMenu().findItem(R.id.action_enroll_by_webui); // show and hide webui depend on AppBuildConfiguration
        if(AppBuildConfigurations.WEB_UI_CONFIG)
        {
            menuItem_webUI.setVisible(true);
            Log.e("webui menuitem","visible ");
        }
        else {
            menuItem_webUI.setVisible(false);
            Log.e("webui menuitem","invisible ");
        }
        MenuItem delegatedMenuItem = myToolbar.getMenu().findItem(R.id.action_delegated_cdcvm);
        boolean status = SharedPreferenceUtils.getDelegatedCDCVMStatus(this);
        delegatedMenuItem.setTitle(status ? getString(R.string.action_disable_delegated_cdcvm)
                : getString(R.string.action_enable_delegated_cdcvm));

        myToolbar.setOnMenuItemClickListener(menuItem -> {

            if (menuItem.getItemId() == R.id.action_setting) {
                showSettingsDialog();
            } else if (menuItem.getItemId() == R.id.action_about) {
                Intent settingsIntent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(settingsIntent);
            }
            else if(menuItem.getItemId() == R.id.action_enroll_by_webui) {
                Intent enrollbyWebUiIntent = new Intent(MainActivity.this, EnrollmentActivity.class);
                startActivity(enrollbyWebUiIntent);
            }
            else if(menuItem.getItemId() == R.id.action_disablePushNoti) {
                SharedPreferenceUtils.disablePushNoti(this.getApplicationContext());
            }
            else if(menuItem.getItemId() == R.id.action_enablePushNoti){
                SharedPreferenceUtils.enablePushNoti(this.getApplicationContext());
            } else if (menuItem.getItemId() == R.id.action_delegated_cdcvm) {
                boolean currStatus = SharedPreferenceUtils.getDelegatedCDCVMStatus(this);
                SharedPreferenceUtils.setDelegatedCDCVMStatus(this, !currStatus);
                delegatedMenuItem.setTitle(
                        !currStatus ? getString(R.string.action_disable_delegated_cdcvm) : getString(R.string.action_enable_delegated_cdcvm)
                );
            }
            return false;
        });
    }


    @Override
    protected void onResume() {
        super.onResume();


        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppConstants.ACTION_INIT_DONE);
        intentFilter.addAction(AppConstants.ACTION_RELOAD_CARDS);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);

        if (SDKController.getInstance().getSDKServiceState() == SDKServiceState.STATE_INITIALIZED) {
            toggleProgress(true);
            fabAdd.show();
            loadCards();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        appExecutors.mainThread().cancelAllPendingOps();
    }


    public void toggleProgress(final boolean show) {
        appExecutors.mainThread().execute(new Runnable() {
            @Override
            public void run() {
                progress_layout.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void loadCardsSync(){
        AsyncToken<String[]> allCardsToken =
                DigitalizedCardManager.getAllCards(null);

        AsyncResult<String[]> asyncResult=allCardsToken.waitToComplete();
        if(!asyncResult.isSuccessful()){
            int errorCode = asyncResult.getErrorCode();

            if (STORAGE_COMPONENT_ERROR == errorCode) {

                HashMap<String, Object> additionalInformation = asyncResult.getAdditionalInformation();

                if (additionalInformation != null && additionalInformation.size() > 0) {
                    Object additionalObject = additionalInformation.get(STORAGE_COMPONENT_EXCEPTION_KEY);
                    if (additionalObject != null && additionalObject instanceof Exception) {
                        Exception exception = (Exception) additionalObject;
                        AppLogger.e(TAG, "Get All cards failed because " + exception.getMessage());
                        exception.printStackTrace();
                        //In production app, this event to be sent to Analytics server. Exception stack trace can be sent to analytics, if available.
                        //If exception stack trace is not possible, please send atleast the exception message. (e.getMessage() to analytics
                    }
                }
                // the production MPA can retry again instead.
                AppLogger.e(TAG, "Failed to reload the card list due to secure storage: " + asyncResult.getErrorMessage());
                // if issue, persists even after certain number of retries. Recommend to do the following
                // 1. Send a specific error event that retry failed
                // 2. SDK APIs cannot be used in this user session anymore. so block all SDK usage from this point onward

            } else if (errorCode == DigitalizedCardErrorCodes.CD_CVM_REQUIRED) {

                // CDCVM is required to be able to use cards.
                AppLogger.d(TAG, "CD_CVM_REQUIRED");
                DeviceCVMEligibilityResult result = DeviceCVMEligibilityChecker.checkDeviceEligibility(MainActivity.this);

                if (result.getBiometricsSupport() == BiometricsSupport.SUPPORTED) {
                    //to use fingerprint. Be sure to check for device support
                    try {
                        DeviceCVMManager.INSTANCE.initialize(CHVerificationMethod.BIOMETRICS);
                    } catch (DeviceCVMException e) {
                        Toast.makeText(this, "DeviceCVMException - onError:" + e.getMessage() ,
                                Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                } else if (result.getDeviceKeyguardSupport() == DeviceKeyguardSupport.SUPPORTED) {
                    //to use device key guard
                    try {
                        DeviceCVMManager.INSTANCE.initialize(CHVerificationMethod.DEVICE_KEYGUARD);
                    } catch (DeviceCVMException e) {
                        Toast.makeText(this, "DeviceCVMException - onError:" + e.getMessage() ,
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    //Force To use Wallet PIN
                }
            }else{
                //Generic error handling
            }
        }else {

            String[] allCards = allCardsToken.waitToComplete().getResult();

            for (String tokenID : allCards) {
                //process cards
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private void loadCards() {
        DigitalizedCardManager.getAllCards(new AbstractAsyncHandler<String[]>() {
            @Override
            public void onComplete(AsyncResult<String[]> asyncResult) {
                Log.i(TAG,"Load Cards Complete");
                swipeRefreshLayout.setRefreshing(false);
                toggleProgress(false);
                if (asyncResult.isSuccessful()) {
                    List<DigitalizedCard> allCards = new ArrayList<>();
                    for (String token : asyncResult.getResult()) {
                        allCards.add(DigitalizedCardManager.getDigitalizedCard(token));
                    }

                    List<MyDigitalCard> mCards = new ArrayList<>();
                    for (DigitalizedCard card : allCards) {
                        MyDigitalCard mCard = new MyDigitalCard(card);
                        mCard.setDigitalizedCardId(DigitalizedCardManager.getDigitalCardId(card.getTokenizedCardID()));

                        //check default
                        mCard.setDefaultCardFlag(card.isDefault(PaymentType.CONTACTLESS, null).waitToComplete().getResult());
                        DigitalizedCardDetails digitalizedCardDetails=card.getCardDetails(null).waitToComplete().getResult();
                        mCard.setRemotePaymentSupported(digitalizedCardDetails.isPaymentTypeSupported(PaymentType.DSRP));
                        //get card status
                        mCard.setCardStatus(card.getCardState(null).waitToComplete().getResult());

                        //Check if the card needs replenishment of tokens
                        TokenReplenishmentRequestor.replenish(mCard.getCardStatus(), card.getTokenizedCardID());

                        mCards.add(mCard);
                    }

                    findViewById(R.id.no_cards).setVisibility((mCards.size() > 0) ? View.GONE : View.VISIBLE);

                    recyclerView.setAdapter(new CardListAdapter(
                            getApplicationContext(),
                            MainActivity.this,
                            mCards,
                            CardListAdapter.DisplayType.CARD_LIST_MAIN));



                } else {
                    int errorCode = asyncResult.getErrorCode();

                    if (STORAGE_COMPONENT_ERROR == errorCode) {

                        HashMap<String, Object> additionalInformation = asyncResult.getAdditionalInformation();

                        if (additionalInformation != null && additionalInformation.size() > 0) {
                            Object additionalObject = additionalInformation.get(STORAGE_COMPONENT_EXCEPTION_KEY);
                            if (additionalObject != null && additionalObject instanceof Exception) {
                                Exception exception = (Exception) additionalObject;
                                AppLogger.e(TAG, "Get All cards failed because" + exception.getMessage());
                                exception.printStackTrace();
                                //In production app, this event to be sent to Analytics server. Exception stack trace can be sent to analytics, if available.
                                //If exception stack trace is not possible, please send atleast the exception message. (e.getMessage() to analytics
                            }
                        }
                        // the production MPA can retry again instead.
                        AppLogger.e(TAG, "Failed to reload the card list due to secure storage: " + asyncResult.getErrorMessage());
                        // if issue, persists even after certain number of retries. Recommend to do the following
                        // 1. Send a specific error event that retry failed
                        // 2. SDK APIs cannot be used in this user session anymore. so block all SDK usage from this point onward
                    }

                    else if (errorCode == DigitalizedCardErrorCodes.CD_CVM_REQUIRED) {

                        AppLogger.d(TAG, "CD_CVM_REQUIRED");


                        DeviceCVMEligibilityResult result =
                                DeviceCVMEligibilityChecker.checkDeviceEligibility(MainActivity.this);

                        if (result.getBiometricsSupport() == BiometricsSupport.SUPPORTED) {
                            //to use fingerprint. Be sure to check for device support
                            try {
                                DeviceCVMManager.INSTANCE.initialize(CHVerificationMethod.BIOMETRICS);
                            } catch (DeviceCVMException e) {
                                e.printStackTrace();
                            }
                        } else if (result.getDeviceKeyguardSupport() == DeviceKeyguardSupport.SUPPORTED) {
                            //to use device key guard
                            try {
                                DeviceCVMManager.INSTANCE.initialize(CHVerificationMethod.DEVICE_KEYGUARD);
                            } catch (DeviceCVMException e) {
                                e.printStackTrace();
                            }
                        } else {
                            //throw new RuntimeException("Device not suitable for demo");
                            //Force To use Wallet PIN
                            enablePin();
                        }
                    }
                }
            }
        });

    }

    private void enablePin() {
        WalletPinManager.getInstance().bindAbstractWalletPinService(new AbstractWalletPinService() {
            @Override
            public void onSetWalletPin(CHCodeVerifier chCodeVerifier) {
                chCodeVerifier.inputCode(Constants.WALLET_PIN);
            }

            @Override
            public void onVerifyWalletPin(CHCodeVerifier chCodeVerifier) {

            }

            @Override
            public WalletPinEventListener setupListener() {
                return walletPinEventListener;
            }
        });
        try {
            WalletPinManager.getInstance().invokeSetWalletPin();
        } catch (WalletPinException e) {
            e.printStackTrace();
        }
    }


    WalletPinEventListener walletPinEventListener = new WalletPinEventListener() {
        @Override
        public void onPinSet() {
            toggleProgress(true);
            loadCards();
            AppLogger.d(TAG, "Pin set successfully");
            Toast.makeText(getApplicationContext(), "Pin set successfully", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPinVerified() {
            AppLogger.d(TAG, "Pin verified successfully");
            Toast.makeText(getApplicationContext(), "Pin verified successfully", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPinChanged() {
            AppLogger.d(TAG, "Pin changed successfully");
            Toast.makeText(getApplicationContext(), "Pin changed successfully", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(WalletPinErrorCode walletPinErrorCode, String s) {
            AppLogger.e(TAG, "Wallet pin error " + s + " errorCode :" + walletPinErrorCode);
            Toast.makeText(getApplicationContext(), "Wallet pin error " + s + " errorCode :" + walletPinErrorCode, Toast.LENGTH_SHORT).show();
        }
    };


    //Adapter callback
    @Override
    public void onBtnDelete(final MyDigitalCard card) {
        toggleProgress(true);

        // Initializing a new alert dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        // Set the alert dialog title
        builder.setTitle(R.string.choose_action);

        // Initializing an array of actions
        final String[] actionsWithSuspend = new String[]{
                getString(R.string.suspend),
                getString(R.string.delete)
        };

        // Initializing an array of actions
        final String[] actionsWithResume = new String[]{
                getString(R.string.resume),
                getString(R.string.delete)
        };

        final String[] actions;

        if(card.getCardStatus().getState()==DigitalizedCardState.ACTIVE){
            actions= actionsWithSuspend;
        }else{
            actions= actionsWithResume;
        }

        selectedItem=null;
        // Set a single choice items list for alert dialog
        builder.setSingleChoiceItems(
                actions, // Items list
                -1, // Index of checked item (-1 = no selection)
                new DialogInterface.OnClickListener() // Item click listener
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int index) {
                        selectedItem = Arrays.asList(actions).get(index);
                    }
                });

        // Set the alert dialog positive button
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                AppLogger.i(TAG,"action OK::index"+index);
                if(selectedItem!=null && !selectedItem.isEmpty()) {
                    // Get the alert dialog selected item's text
                    AppLogger.i(TAG,"selectedItem"+selectedItem);
                    if (selectedItem.equalsIgnoreCase(getString(R.string.suspend))) {
                        manageCard(card,false);
                    }else if (selectedItem.equalsIgnoreCase(getString(R.string.resume))) {
                        manageCard(card,true);
                    } else {
                        deleteCard(card);
                    }
                }else {
                    AppLogger.i(TAG,"selectedItem"+selectedItem);
                    toggleProgress(false);
                }
            }
        });

        // Create the alert dialog
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(dialogInterface -> toggleProgress(false));

        // Finally, display the alert dialog
        dialog.show();
    }

    @SuppressLint("StaticFieldLeak")
    private void manageCard(MyDigitalCard card,final  boolean isResume) {

        AppLogger.i(TAG,"manageCard");
        new AsyncTask<Object, Object, Object>(){

            @Override
            protected Object doInBackground(Object[] objects) {
                AppLogger.i(TAG,"doInBackground");
                ProvisioningBusinessService provisioningBusinessService=ProvisioningServiceManager.getProvisioningBusinessService();
                if (provisioningBusinessService == null) {
                    AppLogger.i(TAG, "provisioningBusinessService is null");
                    Snackbar snackbar = Snackbar
                            .make(coordinatorLayout, "Error(1), cannot suspend card", Snackbar.LENGTH_LONG);
                    snackbar.show();
                    toggleProgress(false);
                } else {
                    AppLogger.i(TAG, "getAccessToken");
                    provisioningBusinessService.getAccessToken(card.getDigitalizedCardId(),
                            GetAccessTokenMode.NO_REFRESH, new AccessTokenListener() {
                                @Override
                                public void onSuccess(String digitalCardID, String accessToken) {
                                    AppLogger.i(TAG,"getAccessToken::onSuccess");

                                    if(!isResume){
                                        MobileGatewayManager.INSTANCE.getCardLifeCycleManager().suspendCard(
                                                card.getDigitalizedCardId(),
                                                new MGCardLifecycleEventListener() {
                                                    @Override
                                                    public void onSuccess(String s) {

                                                        AppLogger.i(TAG,"suspendcard ::onSuccess");
                                                        appExecutors.mainThread().execute(new Runnable() {


                                                            @Override
                                                            public void run() {
                                                                toggleProgress(false);
                                                                Snackbar snackbar = Snackbar
                                                                        .make(coordinatorLayout, "Request sent to suspend card", Snackbar.LENGTH_LONG);
                                                                snackbar.show();
                                                            }
                                                        });

                                                    }

                                                    @Override
                                                    public void onError(String s, final MobileGatewayError mobileGatewayError) {
                                                        AppLogger.i(TAG,"suspendcard ::onError");
                                                        appExecutors.mainThread().execute(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Snackbar snackbar = Snackbar
                                                                        .make(coordinatorLayout, mobileGatewayError.getMessage(), Snackbar.LENGTH_LONG);
                                                                snackbar.show();
                                                                toggleProgress(false);
                                                            }
                                                        });
                                                    }
                                                },null,null,accessToken);
                                    }else{
                                        MobileGatewayManager.INSTANCE.getCardLifeCycleManager().resumeCard(
                                                card.getDigitalizedCardId(),
                                                new MGCardLifecycleEventListener() {
                                                    @Override
                                                    public void onSuccess(String s) {

                                                        AppLogger.i(TAG,"resumeCard ::onSuccess");
                                                        appExecutors.mainThread().execute(new Runnable() {


                                                            @Override
                                                            public void run() {
                                                                toggleProgress(false);
                                                                Snackbar snackbar = Snackbar
                                                                        .make(coordinatorLayout, "Request sent to resume Card ", Snackbar.LENGTH_LONG);
                                                                snackbar.show();
                                                            }
                                                        });

                                                    }

                                                    @Override
                                                    public void onError(String s, final MobileGatewayError mobileGatewayError) {
                                                        AppLogger.i(TAG,"resumeCard ::onError");
                                                        appExecutors.mainThread().execute(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Snackbar snackbar = Snackbar
                                                                        .make(coordinatorLayout, mobileGatewayError.getMessage(), Snackbar.LENGTH_LONG);
                                                                snackbar.show();
                                                                toggleProgress(false);
                                                            }
                                                        });
                                                    }
                                                },null,null,accessToken);
                                    }


                                }

                                @Override
                                public void onError(String s, ProvisioningServiceError provisioningServiceError) {
                                    AppLogger.i(TAG,"access Token ::onError");
                                    Snackbar snackbar = Snackbar
                                            .make(coordinatorLayout, "Error(2), cannot manage Card", Snackbar.LENGTH_LONG);
                                    snackbar.show();
                                    toggleProgress(false);
                                }
                            });
                }
                return null;

            }
        }.execute();

    }


    public void deleteCard(final MyDigitalCard card) {
        //starting the delete request.
        MobileGatewayManager.INSTANCE.getCardLifeCycleManager().deleteCard(card.getDigitalizedCardId(), new MGCardLifecycleEventListener() {
            @Override
            public void onSuccess(String s) {
                appExecutors.mainThread().execute(new Runnable() {
                    @Override
                    public void run() {
                        toggleProgress(false);
                        Snackbar snackbar = Snackbar
                                .make(coordinatorLayout, "Request sent to delete card", Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }
                });

            }

            @Override
            public void onError(String s, final MobileGatewayError mobileGatewayError) {
                appExecutors.mainThread().execute(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), mobileGatewayError.getMessage(), Toast.LENGTH_SHORT).show();
                        toggleProgress(false);
                    }
                });
            }
        });
    }

    @Override
    public void onPaymentClicked(MyDigitalCard card) {
        //just to be sure. deactivate before any payment
        PaymentBusinessService paymentBS = PaymentBusinessManager.getPaymentBusinessService();
        if (paymentBS != null) {
            paymentBS.deactivate();
        } else {
            AppLogger.e(TAG, "Failed to get PaymentBusinessService to deactivate");
        }
        //show dialog for condensed payment / standard payment type

        showQRTypeDialog(card);
    }

    @Override
    public void onBtnHistoryClicked(MyDigitalCard card) {
        AppLogger.i(TAG, "[onBtnHistoryClicked]");

        Intent history = new Intent(this, HistoryActivity.class);
        history.putExtra(HistoryActivity.EXTRA_DIGITAL_CARD_ID, card.getDigitalizedCardId());
        startActivity(history);
    }

    @Override
    public void onLongClickCard(MyDigitalCard card) {
        AppLogger.i(TAG, "[onLongClick]");

        String defaultCardTokenID = DigitalizedCardManager
                .getDefault(PaymentType.CONTACTLESS, null).waitToComplete().getResult();

        AppLogger.i(TAG, "defaultCardTokenID fetched");

        if (defaultCardTokenID != null && !defaultCardTokenID.isEmpty()) {
            AppLogger.i(TAG, "defaultCardTokenID fetched is Not null or empty");
            SharedPreferenceUtils.saveDefaultCard(getApplicationContext(), defaultCardTokenID);
        } else {
            AppLogger.i(TAG, "defaultCardTokenID fetched is null or empty");
        }

        if (card.getCardStatus().getState() != DigitalizedCardState.ACTIVE) {
            AppLogger.i(TAG, "Card is not Active and cannot be used for payment");
            Snackbar snackbar = Snackbar.make(coordinatorLayout, "Card is not Active and cannot be used for payment", Snackbar.LENGTH_SHORT);
            snackbar.show();

        } else {
            toggleProgress(true);

            AppLogger.i(TAG, "setDefault called from onLongclick");
            DigitalizedCardManager.getDigitalizedCard(card.getTokenId()).setDefault(PaymentType.CONTACTLESS, new AbstractAsyncHandler<Void>() {
                @Override
                public void onComplete(AsyncResult<Void> asyncResult) {
                    if(asyncResult.isSuccessful()) {
                        AppLogger.i(TAG, "setDefault called from onLongclick :: Listener");
                        toggleProgress(false);
                        AppLogger.i(TAG, "paymentBS");
                        final PaymentBusinessService paymentBS = PaymentBusinessManager.getPaymentBusinessService();
                        AppLogger.i(TAG, "getAuthenticationFlowPriorToPayment started");
                        paymentBS.getAuthenticationFlowPriorToPayment(((App) getApplication()).getContactlessPayListener(), PaymentType.CONTACTLESS);
                        AppLogger.i(TAG, "getAuthenticationFlowPriorToPayment ended");
                    }else{
                        Snackbar snackbar = Snackbar.make(coordinatorLayout, "Card could not be set as default card:\n Error message::"+asyncResult.getErrorMessage(), Snackbar.LENGTH_SHORT);
                        snackbar.show();
                        toggleProgress(false);

                    }
                }
            });
        }
    }

    @Override
    public void onCheckboxClicked(MyDigitalCard card) {

    }

    @Override
    public void onDsrpClicked(MyDigitalCard card) {
        toggleProgress(true);
        AppLogger.i(TAG, "onDsrpClicked called");
            Intent dsrp = new Intent(this, DsrpActivity.class);
            dsrp.putExtra(DsrpActivity.EXTRA_TOKEN_ID,card.getTokenId());
            dsrp.putExtra(DsrpActivity.EXTRA_DIGITALIZED_CARD_ID,card.getDigitalizedCardId());
            startActivity(dsrp);

    }

    @Override
    public void onSettingsClicked(MyDigitalCard card) {
        toggleProgress(true);

        AppLogger.i(TAG, "onSettingsClicked called");

        DigitalizedCardDetails digitalizedCardDetails
                = DigitalizedCardManager.getDigitalizedCard(card.getTokenId()).getCardDetails(null).waitToComplete().getResult();
        if (digitalizedCardDetails.getScheme().equalsIgnoreCase("VISA")) {
            Intent cardSettingsActivity = new Intent(this, CardSettingsActivity.class);
            cardSettingsActivity.putExtra("selected_token_id", card.getTokenId());
            startActivity(cardSettingsActivity);
        } else {
            toggleProgress(false);
            Toast.makeText(this, "Only support VISA for AID settings.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void setDefaultCardAction(final MyDigitalCard card) {
        AppLogger.d(TAG, "[set Default Card]-" + card.getTokenId());
        toggleProgress(true);
        DigitalizedCardState status = card.getCardStatus().getState();
        if (status == DigitalizedCardState.SUSPENDED) {
            Snackbar snackbar = Snackbar.make(coordinatorLayout, "The card is Suspended.You cannot use the card to make payment", Snackbar.LENGTH_LONG);
            snackbar.show();
        } else {
            DigitalizedCardManager.getDigitalizedCard(card.getTokenId()).setDefault(PaymentType.CONTACTLESS, new AbstractAsyncHandler<Void>() {
                @Override
                public void onComplete(AsyncResult<Void> asyncResult) {
                    toggleProgress(false);
                    String defaultCardTokenID = card.getTokenId();
                    SharedPreferenceUtils.saveDefaultCard(getApplicationContext(), defaultCardTokenID);
                    Snackbar snackbar = Snackbar.make(coordinatorLayout, "Card (" + card.getTokenId() + ") set to default for payment", Snackbar.LENGTH_LONG);
                    snackbar.show();
                    loadCards();
                }
            });
        }
    }

    private class MySDKInitReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equalsIgnoreCase(AppConstants.ACTION_RELOAD_CARDS)){
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        loadCards();
                    }
                });
            }else if(action.equalsIgnoreCase(AppConstants.ACTION_INIT_DONE)){
                if (intent.getBooleanExtra(AppConstants.INIT_FAILED_EXTRA, false)) {
                    Snackbar snackbar = Snackbar.make(coordinatorLayout, "Closing the app as SDK initialization failed", Snackbar.LENGTH_LONG);
                    snackbar.show();
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    });

                } else {
                    AppLogger.d(TAG, "*** onReceive - loadCards");

                    if (intent.getBooleanExtra(AppConstants.INIT_UI_UPDATE_NEEDED, false)) {
                        Snackbar snackbar = Snackbar.make(coordinatorLayout, "Initialization completed", Snackbar.LENGTH_LONG);
                        snackbar.show();
                        final Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                loadCards();
                            }
                        });
                    }
                    updateSchedulingDisplay();
                }
            }

        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void checkTapAndPay() {
        if (!isEmulator()) {
            Intent activate = new Intent();
            activate.setAction(ACTION_CHANGE_DEFAULT);
            activate.putExtra(EXTRA_SERVICE_COMPONENT, new ComponentName(this,
                    AppConstants.CANONICAL_PAYMENT_SERVICENAME));
            activate.putExtra(EXTRA_CATEGORY, CardEmulation.CATEGORY_PAYMENT);
            startActivity(activate);
        }
    }

    private static boolean isEmulator() {
        String model = Build.MODEL;
        AppLogger.d(TAG, "model=" + model);
        String product = Build.PRODUCT;
        AppLogger.d(TAG, "product=" + product);
        boolean isEmulator = false;
        if (product != null) {
            isEmulator = product.equals("sdk") || product.contains("_sdk") || product.contains("sdk_");
        }
        AppLogger.d(TAG, "isEmulator=" + isEmulator);
        return isEmulator;
    }

    private void showSettingsDialog() {
        String[] settingsList = {getString(R.string.action_wipe_data), getString(R.string.change_benchmarking),getString(R.string.payment_experience)};
        int selected = 0;

        DialogHelper.createBenchmarkingDialog(MainActivity.this, getString(R.string.action_settings), settingsList, selected, (dialog, i) -> {
            dialog.dismiss();
            switch (i) {
                case 0:
                    DialogHelper.createAlertDialog(MainActivity.this,
                            getString(R.string.app_name),
                            getString(R.string.delete_alert_message), (dialog1, which) -> {
                                AppLogger.d(TAG, "setNeedWipeAll true");
                                SharedPreferenceUtils.setNeedWipeAll(getApplicationContext(), true);
                                Toast.makeText(getApplicationContext(), getString(R.string.wipe_triggered_toast), Toast.LENGTH_SHORT).show();
                                new Handler().postDelayed(() -> {
                                    finishAndRemoveTask();
                                    System.exit(0);
                                }, 1000);
                            },
                            (dialog1, which) ->{
                                AppLogger.d(TAG, "Wipe Cancelled");
                                Toast.makeText(getApplicationContext(), "Wipe Cancelled", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                    ).show();
                    break;

                case 1:
                    String[] benchamarkList = {AppConstants.Schemes.VISA.name(), AppConstants.Schemes.MASTERCARD.name()};
                    int selectedBenchmark1 = 0;
                    if (SharedPreferenceUtils.getBenchmark(getApplicationContext()) != null) {
                        if (SharedPreferenceUtils.getBenchmark(getApplicationContext())
                                .equals(AppConstants.Schemes.MASTERCARD.name())) {
                            selectedBenchmark1 = 1;
                        }
                    }

                    DialogHelper.createBenchmarkingDialog(MainActivity.this, getString(R.string.change_benchmarking),
                            benchamarkList, selectedBenchmark1, (dialog2, i1) -> {
                                switch (i1) {
                                    case 0:
                                        AppConstants.BENCHMARKING_SCHEME_SELECTED = AppConstants.Schemes.VISA;
                                        SharedPreferenceUtils.saveBenchmark(getApplicationContext(),
                                                AppConstants.BENCHMARKING_SCHEME_SELECTED.name());
                                        dialog2.dismiss();
                                        break;

                                    case 1:
                                        AppConstants.BENCHMARKING_SCHEME_SELECTED = AppConstants.Schemes.MASTERCARD;
                                        SharedPreferenceUtils.saveBenchmark(getApplicationContext(),
                                                AppConstants.BENCHMARKING_SCHEME_SELECTED.name());
                                        dialog2.dismiss();
                                        break;
                                }
                                Snackbar snackbar = Snackbar.make(coordinatorLayout, "Benchmark changed to " + SharedPreferenceUtils.getBenchmark(getApplicationContext()),
                                        Snackbar.LENGTH_LONG);
                                snackbar.show();
                            }).show();
                    break;
                case 2:
                    Intent intent = new Intent(MainActivity.this, PaymentExperienceSettingActivity.class);
                    startActivity(intent);
                    break;

            }
        }).show();
    }

    private void showQRTypeDialog(MyDigitalCard myDigitalCard) {
        String[] settingsList = {getString(R.string.standard_qr), getString(R.string.condensed_qr)};
        int selected = 0;
        Intent intent = new Intent(this, PaymentQRActivity.class);
        DialogHelper.createBenchmarkingDialog(MainActivity.this,
                getString(R.string.select_qr_type), settingsList, selected, (dialog, i) -> {
                    dialog.dismiss();
                    switch (i) {
                        case 0:
                            intent.putExtra("QR_TYPE", "Standard");
                            intent.putExtra(PaymentQRActivity.EXTRA_QR_TOKEN_ID,myDigitalCard.getTokenId());
                            startActivity(intent);
                            break;
                        case 1:
                            intent.putExtra("QR_TYPE", "Condensed");
                            intent.putExtra(PaymentQRActivity.EXTRA_QR_TOKEN_ID,myDigitalCard.getTokenId());
                            startActivity(intent);
                            break;
                    }
                }).show();
    }

    private void buildDateTimePicker(int dialogAction){
        CustomDateTimePicker picker = new CustomDateTimePicker(this, new CustomDateTimePicker.ICustomDateTimeListener() {
            @Override
            public void onSet(Dialog dialog,
                              Calendar calendar,
                              Date date,
                              int year,
                              String monthFull,
                              String monthShort,
                              int monthNum,
                              int day,
                              String weekDayFull,
                              String weekDayShort,
                              int hour24,
                              int hour12,
                              int min,
                              int sec,
                              String s4) {
                if(dialogAction == DIALOG_INPUT_SLOT){
                    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                    int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
                    int minOfHour = calendar.get(Calendar.MINUTE);
                    int secOfMin =  calendar.get(Calendar.SECOND);     // 0 to 59 Sec
                    int milliSec = calendar.get(Calendar.MILLISECOND);
                    String slot = dayOfWeek + "," + hourOfDay + "," + minOfHour+ "," + secOfMin+ "," + milliSec;
                    Log.e("###", "######### SLOT " + slot);
                    Toast.makeText(MainActivity.this, "Set Slot " + weekDayShort +" "+hour12+s4 + " " + min+"M " +sec+"S", Toast.LENGTH_LONG).show();
                    SharedPreferenceUtils.mockTimeSlot(MainActivity.this.getApplicationContext(), slot);
                }else{
                    long currentTime = calendar.getTimeInMillis();
                    Toast.makeText(MainActivity.this, "Set lastSync " + weekDayShort +" "+hour12+s4 + " " + min+"M " +sec+"S", Toast.LENGTH_LONG).show();
                    SharedPreferenceUtils.mockTimeStamp(MainActivity.this.getApplicationContext(), currentTime+"");
                }
                updateSchedulingDisplay();
            }

            @Override
            public void onCancel() {
            }
        });
        picker.setDate(Calendar.getInstance());
        picker.showDialog();
    }

    private void updateSchedulingDisplay(){
        Calendar currentTime = Calendar.getInstance();
        String nextSlot = SharedPreferenceUtils.getTimeSlot(getApplicationContext());
        if(nextSlot!=null) {
            currentTime.setTimeInMillis(DigitalizedCardManager.getTokenSyncSchedule());
            nextTimeSlot.setText("Next Slot: " + currentTime.getTime().toString());
        }else{
            nextTimeSlot.setText("Next Slot: ----");
        }

        Calendar lastSyncCalendar = Calendar.getInstance();
        String lastSync = SharedPreferenceUtils.getTimeStamp(getApplicationContext());
        if(lastSync!=null) {
            lastSyncCalendar.setTimeInMillis(Long.parseLong(lastSync));
            lastTimeStamp.setText("Last Sync: " + lastSyncCalendar.getTime().toString());
        }else{
            lastTimeStamp.setText("Last Sync: ----");
        }
    }

    private void removeTimeStamp(){
        SharedPreferenceUtils.removeTimeStamp(getApplicationContext());
        updateSchedulingDisplay();
    }

    private void removeTimeSlot(){
        SharedPreferenceUtils.removeTimeSlot(getApplicationContext());
        updateSchedulingDisplay();
    }

    private void showToastMessage(final String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

}
