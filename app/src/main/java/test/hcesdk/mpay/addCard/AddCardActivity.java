package test.hcesdk.mpay.addCard;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;

import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.gemalto.mfs.mwsdk.mobilegateway.MobileGatewayError;
import com.gemalto.mfs.mwsdk.mobilegateway.MobileGatewayManager;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.IDVMethod;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.IDVMethodSelector;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.PendingCardActivation;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.PendingCardActivationState;
import com.gemalto.mfs.mwsdk.mobilegateway.listener.MGDigitizationListener;
import com.gemalto.mfs.mwsdk.provisioning.ProvisioningServiceManager;
import com.gemalto.mfs.mwsdk.provisioning.listener.EnrollingServiceListener;
import com.gemalto.mfs.mwsdk.provisioning.model.EnrollmentStatus;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServiceError;
import com.gemalto.mfs.mwsdk.provisioning.sdkconfig.EnrollingBusinessService;
import com.gemalto.mfs.mwsdk.provisioning.sdkconfig.ProvisioningBusinessService;
import com.gemalto.mfs.mwsdk.utils.chcodeverifier.CHCodeVerifier;
import com.gemalto.mfs.mwsdk.utils.chcodeverifier.SecureCodeInputer;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.io.IOException;

import test.hcesdk.mpay.App;
import test.hcesdk.mpay.R;
import test.hcesdk.mpay.service.MyFirebaseMessagingService;
import test.hcesdk.mpay.util.AppExecutors;
import test.hcesdk.mpay.util.AppLogger;
import test.hcesdk.mpay.util.Constants;
import test.hcesdk.mpay.util.SharedPreferenceUtils;

public class AddCardActivity extends AppCompatActivity implements MGDigitizationListener, EnrollingServiceListener {

    private static final String TAG = "AddCardActivity";
    View progressLayout;
    byte[] activationCode;

    AppExecutors appExecutors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_card);
        progressLayout = findViewById(R.id.progress_layout);
        appExecutors = ((App)getApplication()).getAppExecutors();

        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().setDisplayShowHomeEnabled(true);

        if (savedInstanceState == null) {
            //add fragment
            AddCardFragment addForm = new AddCardFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, addForm).commit();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        appExecutors.mainThread().cancelAllPendingOps();
    }

    public void toggleProgress(final boolean show) {
        appExecutors.mainThread().execute(new Runnable() {
            @Override
            public void run() {
                progressLayout.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStackImmediate();
        } else {
            finish();
        }
    }

    public void switchFragment(Fragment fragment, boolean replace) {
        hideKeyboard(this);
        if (replace) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
        } else {
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    public static void hideKeyboard(AppCompatActivity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    /**
     * Digitize callback
     */
    @Override
    public void onCPSActivationCodeAcquired(String id, byte[] code) {
        String firebaseToken = "";
        //TODO: Trigger CPS Enrollment
        if(!TextUtils.isEmpty(SharedPreferenceUtils.getFirebaseId(this))){
            firebaseToken = SharedPreferenceUtils.getFirebaseId(this);
        }else{
            firebaseToken = FirebaseInstanceId.getInstance().getToken();
        }

        if (TextUtils.isEmpty(firebaseToken)) {
            Log.i(TAG,"FireBaseToken is null");
            throw new RuntimeException("Firebase token is null ");
        }
        EnrollingBusinessService enrollingService = ProvisioningServiceManager.getEnrollingBusinessService();
        ProvisioningBusinessService provisioningBusinessService = ProvisioningServiceManager.getProvisioningBusinessService();

        this.activationCode = new byte[code.length];
        for (int i = 0; i < code.length; i++) {
            activationCode[i] = code[i];
        }

        //WalletID of MG SDK is userID of CPS SDK Enrollment process
        String userId = MobileGatewayManager.INSTANCE.getCardEnrollmentService().getWalletId();
        Log.i(TAG,"FireBaseToken is  "+ firebaseToken);

        EnrollmentStatus status = enrollingService.isEnrolled();
        switch (status) {
            case ENROLLMENT_NEEDED:
                enrollingService.enroll(userId, firebaseToken, "en", this);
                break;
            case ENROLLMENT_IN_PROGRESS:
                enrollingService.continueEnrollment("en", this);
                break;
            case ENROLLMENT_COMPLETE:
                provisioningBusinessService.sendActivationCode(this);
                break;
        }


    }

    @Override
    public void onSelectIDVMethod(IDVMethodSelector idvMethodSelector) {
        //TODO: Show idv method list

        //For demo purpose, we skip and select the first one
        IDVMethod firstMethod = idvMethodSelector.getIdvMethodList()[0];
        idvMethodSelector.select(firstMethod.getId());
        toggleProgress(true);
    }

    @Override
    public void onActivationRequired(PendingCardActivation pendingCardActivation) {
        toggleProgress(false);
        Toast.makeText(this, ".onActivationRequired() :", Toast.LENGTH_SHORT).show();
        if (pendingCardActivation.getState() == PendingCardActivationState.WEB_3DS_NEEDED) {
            ThreeDSFragment frag = new ThreeDSFragment();
            frag.pendingCardActivation = pendingCardActivation;
            switchFragment(frag, true);
        } else if(pendingCardActivation.getState() == PendingCardActivationState.OTP_NEEDED){
            //OTP is hardcoded one.
            pendingCardActivation.activate(Constants.OTP_YELLOW_FLOW.getBytes(), this);
        }
    }

    @Override
    public void onComplete(final String s) {
        appExecutors.mainThread().execute(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(AddCardActivity.this, "Digitize Successful : " + s, Toast.LENGTH_SHORT).show();
                toggleProgress(false);
                finish();
            }
        });
    }

    @Override
    public void onError(String s, final MobileGatewayError mobileGatewayError) {
        appExecutors.mainThread().execute(new Runnable() {
            @Override
            public void run() {
                toggleProgress(false);
                Toast.makeText(AddCardActivity.this, mobileGatewayError.getMessage(), Toast.LENGTH_SHORT).show();
                onBackPressed();
            }
        });

    }


    /**********************************************************/
    /*           Enrolling Service listener                   */
    /**********************************************************/
    @Override
    public void onCodeRequired(CHCodeVerifier chCodeVerifier) {
        AppLogger.d(TAG, ".onCodeRequired called. Providing activation code");

        SecureCodeInputer inputer = chCodeVerifier.getSecureCodeInputer();
        for (byte i : activationCode) {
            inputer.input(i);
        }
        inputer.finish();

        //wipe after use
        for (int i = 0; i < activationCode.length; i++) {
            activationCode[i] = 0;
        }
    }

    @Override
    public void onStarted() {
        AppLogger.d(TAG, ".onStarted()");

    }

    @Override
    public void onError(ProvisioningServiceError provisioningServiceError) {
        AppLogger.e(TAG, ".onError() - " + provisioningServiceError.getErrorMessage());

        Toast.makeText(this, "EnrollmentListener - onError:" + provisioningServiceError.getSdkErrorCode() ,
                Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onComplete() {
        AppLogger.d(TAG, ".onComplete()");

    }
}
