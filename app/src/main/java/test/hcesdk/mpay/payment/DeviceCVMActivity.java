package test.hcesdk.mpay.payment;

import android.app.KeyguardManager;
import android.content.Context;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.gemalto.mfs.mwsdk.cdcvm.DeviceCVMCancellationSignal;
import com.gemalto.mfs.mwsdk.cdcvm.DeviceCVMVerifier;
import com.gemalto.mfs.mwsdk.cdcvm.DeviceCVMVerifierInput;
import com.gemalto.mfs.mwsdk.cdcvm.DeviceCVMVerifyListener;
import com.gemalto.mfs.mwsdk.payment.CHVerificationMethod;
import com.gemalto.mfs.mwsdk.payment.PaymentBusinessManager;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKError;
import com.gemalto.mfs.mwsdk.utils.chcodeverifier.CHCodeVerifier;
import com.gemalto.mfs.mwsdk.utils.chcodeverifier.CHCodeVerifierErrorCode;
import com.gemalto.mfs.mwsdk.utils.chcodeverifier.CHCodeVerifierListener;

import test.hcesdk.mpay.R;
import test.hcesdk.mpay.app.AppConstants;
import test.hcesdk.mpay.util.AppLogger;
import test.hcesdk.mpay.util.Constants;

public class DeviceCVMActivity extends AppCompatActivity {

    public static final String EXTRA_CVM = "DeviceCVMActivity.EXTRA_CVM";
    public static final String EXTRA_AMOUNT = "DeviceCVMActivity.EXTRA_AMOUNT";
    private static final int MAX_ATTEMPT_COUNT = 5;
    private static final int RESET_DELAY_TIME = 1500;
    private static String TAG = DeviceCVMActivity.class.getSimpleName();

    private CHVerificationMethod cvm;
    private DeviceCVMVerifier deviceCVMVerifier;
    private DeviceCVMCancellationSignal cancellationSignal;

    private boolean tooManyAttempt;
    private boolean switchedToKeyguard;
    private int fingerprintErrorCount = 0;
    private StringBuilder keyguardDescription;
    private CharSequence title;
    private CharSequence subTitle;
    private CharSequence negativeButtonText;

    private TextView fingerprintText;
    private View btnKeyguardVer;
    private View progressLayout;
    private View mainLayout;
    private TextView tvAmount;

    private PowerManager.WakeLock mWakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        AppLogger.d(TAG, "Auth Screen Display " + AppConstants.STARTED);
        unlockAndWake();

        setContentView(R.layout.activity_cvm);
        Bundle extras = getIntent().getExtras();
        cvm = (CHVerificationMethod) extras.getSerializable(EXTRA_CVM);
        fingerprintText = findViewById(R.id.message);
        btnKeyguardVer = findViewById(R.id.btn_keyguard_verification);
        progressLayout = findViewById(R.id.progress_layout);
        mainLayout = findViewById(R.id.mainLayout);
        tvAmount = findViewById(R.id.tv_amount);

        final String amount = extras.getString(EXTRA_AMOUNT);
        keyguardDescription = new StringBuilder();

        if (amount != null) {
            tvAmount.setText(amount);
            keyguardDescription.append("Transaction amount: ");
            keyguardDescription.append(amount);
            keyguardDescription.append("\n");
        } else {
            tvAmount.setVisibility(View.GONE);
            findViewById(R.id.tv_amount_title).setVisibility(View.GONE);
        }


        if(cvm == CHVerificationMethod.WALLET_PIN){

            View btnAuthByWalletPIN = findViewById(R.id.btn_auth_by_wallet_pin);
            btnAuthByWalletPIN.setVisibility(View.VISIBLE);
            btnAuthByWalletPIN.setOnClickListener(v -> authByWalletPIN());

        } else {

            keyguardDescription.append(getString(R.string.verify_by_keyguard_msg));

            title = getString(R.string.verify_using_fingerprint);
            subTitle = "Subtitle";
            negativeButtonText = "Cancel";

            deviceCVMVerifier = (DeviceCVMVerifier) PaymentBusinessManager.getPaymentBusinessService()
                    .getActivatedPaymentService().getCHVerifier(cvm);

            deviceCVMVerifier.setDeviceCVMVerifyListener(deviceCVMVerifyListener);
            deviceCVMVerifier.setKeyguardActivity(this, DeviceKeyguardActivity.class);

            if (cvm == CHVerificationMethod.BIOMETRICS) {
                AppLogger.d(AppConstants.APP_TAG, ".Starting fp authentication prompt for user " + AppConstants.STARTED);

                DeviceCVMVerifierInput deviceCVMVerifierInput = new DeviceCVMVerifierInput(title, subTitle, keyguardDescription.toString(), negativeButtonText);
                cancellationSignal = deviceCVMVerifierInput.getDeviceCVMCancellationSignal();
                deviceCVMVerifier.startAuthentication(deviceCVMVerifierInput);

                AppLogger.d(AppConstants.APP_TAG, ".Starting fp authentication prompt for user " + AppConstants.ENDED);

                //If on top of lock screen and in Android9 device,
                // show keyguard button for user to authenticate
                if (isRestrictModeEnabled() && Build.VERSION.SDK_INT == 28) {
                    btnKeyguardVer.setVisibility(View.VISIBLE);
                }

            } else if (cvm == CHVerificationMethod.DEVICE_KEYGUARD) {
                AppLogger.d(TAG, "Verify by Keyguard");
                progressLayout.setVisibility(View.VISIBLE);
                mainLayout.setVisibility(View.GONE);


                new Handler().postDelayed(() -> {
                    AppLogger.i(AppConstants.APP_TAG, ".Starting keyguard authentication");
                    DeviceCVMVerifierInput deviceCVMVerifierInput = new DeviceCVMVerifierInput(title, subTitle, keyguardDescription.toString(), negativeButtonText);
                    cancellationSignal = deviceCVMVerifierInput.getDeviceCVMCancellationSignal();
                    deviceCVMVerifier.startAuthentication(deviceCVMVerifierInput);
                }, 100);

            }


            btnKeyguardVer.setOnClickListener(view -> {
                switchedToKeyguard = true;

                deviceCVMVerifier.confirmCredential(title, keyguardDescription.toString());
            });
        }

        AppLogger.d(TAG, "Auth Screen Display " + AppConstants.ENDED);
    }

    @Override
    protected void onDestroy() {
        AppLogger.d(TAG, ".onDestroy()");
        super.onDestroy();
        releaseWakeLockAndClearFlag();
        if (cancellationSignal != null) {
            cancellationSignal.cancel();
        }
    }

    private void authByWalletPIN() {
        CHCodeVerifier chVerifier = (CHCodeVerifier) PaymentBusinessManager.getPaymentBusinessService()
                .getActivatedPaymentService().getCHVerifier(CHVerificationMethod.WALLET_PIN);

        chVerifier.setCHCodeVerifierListener(new CHCodeVerifierListener() {
            @Override
            public void onVerificationError(int i, SDKError<CHCodeVerifierErrorCode> sdkError) {

            }

            @Override
            public void onVerificationSuccess() {

            }

            @Override
            public void maxRetryReached() {

            }
        });
        chVerifier.inputCode(Constants.WALLET_PIN);
    }

    final DeviceCVMVerifyListener deviceCVMVerifyListener = new DeviceCVMVerifyListener() {
        @Override
        public void onVerifySuccess() {
            AppLogger.i(AppConstants.APP_TAG, ".onVerifySuccess");

            setResult(RESULT_OK);
            overridePendingTransition(0, 0);
            finish();
        }

        @Override
        public void onVerifyError(SDKError<Integer> sdkErrorInteger) {
            //Only Called for Fingerprint
            AppLogger.e(TAG, ".onVerifyError(): " + sdkErrorInteger.getErrorCode() + " : " + sdkErrorInteger.getErrorMessage() + " fingerprintErrorCount " + fingerprintErrorCount);
            fingerprintErrorCount++;

            if (sdkErrorInteger.getErrorCode() == FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
                // When user hits button to switch to keyguard after a FP verification fails
                // then this error handler is triggered with FINGERPRINT_ERROR_CANCELED
                // There is an error situation when OS sends this error in loop during Fingerprint authentication setup
                // Application is to break the loop, don't ask authentication and enable device keyguard button
                cancellationSignal.cancel();

                if (fingerprintErrorCount < MAX_ATTEMPT_COUNT) {
                    DeviceCVMVerifierInput deviceCVMVerifierInput = new DeviceCVMVerifierInput(title,
                            subTitle, keyguardDescription.toString(), negativeButtonText);
                    cancellationSignal = deviceCVMVerifierInput.getDeviceCVMCancellationSignal();
                    deviceCVMVerifier.startAuthentication(deviceCVMVerifierInput);
                    enableFallBackMechanism(sdkErrorInteger.getErrorMessage(), fingerprintErrorCount);

                } else {
                    if (Build.VERSION.SDK_INT >= 29) {
                        /*
                         * rsriniva: For Android Q or up, confirmCredential has no impact.
                         * When It reaches here, it also means the fallback was not chosen by user or was not available for Android Q.
                         * Therefore, we need to cancel the transaction.
                         */
                        cancelAction();
                    } else {
                        deviceCVMVerifier.confirmCredential(title, keyguardDescription.toString());
                    }
                }

            } else if (sdkErrorInteger.getErrorCode() == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED) {
                //User cancelled action
                if (Build.VERSION.SDK_INT >= 29) {
                    cancelAction();
                } else {
                    enableFallBackMechanism(sdkErrorInteger.getErrorMessage(), MAX_ATTEMPT_COUNT);
                }

            } else {
                enableFallBackMechanism(sdkErrorInteger.getErrorMessage(), MAX_ATTEMPT_COUNT);
            }
        }

        @Override
        public void onVerifyFailed() {
            AppLogger.e(TAG, ".onVerifyFailed()");
            setKeyguardFallbackButtonVisible();
            resetTextMessage(getString(R.string.unable_to_recognise_fingerprint));
            if (cvm == CHVerificationMethod.DEVICE_KEYGUARD || switchedToKeyguard) {
                //cancel transaction
                cancelAction();
            }
        }

        @Override
        public void onVerifyHelp(int i, CharSequence charSequence) {
            AppLogger.e(TAG, ".onVerifyHelp(): " + i + " : " + charSequence);
            setKeyguardFallbackButtonVisible();
            resetTextMessage(charSequence + getString(R.string.please_try_again));
        }
    };

    private void resetTextMessage(String message) {
        AppLogger.e(TAG, "resetTextMessage(): " + message);

        fingerprintText.setText(message);
        fingerprintText.setTextColor(getResources().getColor(R.color.text_color_5));
        new Handler().postDelayed(() -> {
            if (!tooManyAttempt) {
                fingerprintText.setText(getString(R.string.tap_finger_print));
                fingerprintText.setTextColor(getResources().getColor(R.color.text_color_1));
            }
        }, RESET_DELAY_TIME);
    }

    private void enableFallBackMechanism(String message, int attemptCount) {
        if (attemptCount >= MAX_ATTEMPT_COUNT) {
            tooManyAttempt = true;
        }
        resetTextMessage(message);
        setKeyguardFallbackButtonVisible();
    }

    private void cancelAction() {
        if (cancellationSignal != null) {
            cancellationSignal.cancel();
        }
        setResult(RESULT_FIRST_USER);
        finish();
    }

    private void setKeyguardFallbackButtonVisible() {

        if (btnKeyguardVer != null) {
            if (Build.VERSION.SDK_INT >= 29) {
                /*
                 * rsriniva:
                 * For Android Q, fallback happens within biometric prompt itself. There is no need have fallback mechanism in the UI.
                 * More importantly, it will not work in Android Q devices anyways. So , this button can be an issue for UX.
                 */
                btnKeyguardVer.setVisibility(View.GONE);
            } else {
                btnKeyguardVer.setVisibility(View.VISIBLE);

            }
        }
    }

    private void unlockAndWake() {
        AppLogger.d(TAG, ".unlockAndWake()");

        //Return if mWakelock is non null as it's already initialized and acquired
        if (mWakeLock != null && mWakeLock.isHeld())
            return;

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        if (pm != null && !pm.isInteractive()) {
            mWakeLock = pm.newWakeLock(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP, getResources().getString(R.string.app_name) + "CA:wakelock");
            if (mWakeLock != null)
                mWakeLock.acquire();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    }

    private void releaseWakeLockAndClearFlag() {
        AppLogger.d(TAG, ".releaseWakeLockAndClearFlag()");

        if (mWakeLock != null && mWakeLock.isHeld()) {
            try {
                mWakeLock.release();
            } catch (Exception e) {
                // Ignoring this exception, probably wakeLock was already released
                AppLogger.e(TAG, "Exception observed in releasing wakeLock");
            } finally {
                mWakeLock = null;
            }
        }

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    }

    /**
     *
     * @return true means on top of lock screen
     */
    private boolean isRestrictModeEnabled() {
        KeyguardManager keyguardManager = (KeyguardManager) getApplication().getSystemService(Context.KEYGUARD_SERVICE);
        //Check if in case the MPA is used for payment without unlocking device
        return keyguardManager != null && keyguardManager.inKeyguardRestrictedInputMode();
    }

}
