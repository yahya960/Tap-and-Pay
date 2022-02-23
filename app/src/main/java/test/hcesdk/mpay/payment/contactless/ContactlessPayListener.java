package test.hcesdk.mpay.payment.contactless;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gemalto.mfs.mwsdk.payment.CHVerificationMethod;
import com.gemalto.mfs.mwsdk.payment.PaymentServiceErrorCode;
import com.gemalto.mfs.mwsdk.payment.engine.ContactlessPaymentServiceListener;
import com.gemalto.mfs.mwsdk.payment.engine.PaymentService;
import com.gemalto.mfs.mwsdk.payment.engine.TransactionContext;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKError;

import java.util.HashMap;

import test.hcesdk.mpay.App;
import test.hcesdk.mpay.R;
import test.hcesdk.mpay.app.AppBuildConfigurations;
import test.hcesdk.mpay.payment.contactless.pfp.PFPHCEService;
import test.hcesdk.mpay.payment.contactless.pfp.PFPHelper;
import test.hcesdk.mpay.util.AppLogger;
import test.hcesdk.mpay.util.PaymentExecutor;
import test.hcesdk.mpay.app.AppConstants;

import static test.hcesdk.mpay.payment.contactless.PaymentContactlessActivity.PAYMENT_STATE_EXTRA;

public class ContactlessPayListener implements ContactlessPaymentServiceListener {

    enum PaymentState {
        STATE_NONE,
        STATE_ON_TRANSACTION_STARTED,
        STATE_ON_AUTHENTICATION_REQUIRED,
        STATE_ON_READY_TO_TAP,
        STATE_ON_TRANSACTION_COMPLETED,
        STATE_ON_ERROR
    }

    private static final String TAG = ContactlessPayListener.class.getSimpleName();
    private static final int ERROR_THRESHOLD = 3;
    private static final int ERROR_DELAY = 2000;
    private PaymentState currentState = PaymentState.STATE_NONE;
    private Context context;
    private PaymentService paymentService;
    private TransactionContext transactionContext;
    private CHVerificationMethod chVerificationMethod;
    private PaymentServiceErrorCode paymentServiceErrorCode;
    private HashMap<String, Object> additionalInformation;
    private long cvmResetTimeout;
    private String errorMessage;
    private int posCommDisconnectedErrCount = 0;

    private PaymentExecutor paymentExecutor;

    public ContactlessPayListener(App app) {
        this.context = app.getApplicationContext();
        paymentExecutor = app.getAppExecutors().paymentThread();
    }

    @Nullable
    public PaymentService getPaymentService() {
        return paymentService;
    }

    @Nullable
    public TransactionContext getTransactionContext() {
        return transactionContext;
    }

    @Nullable
    public CHVerificationMethod getChVerificationMethod() {
        return chVerificationMethod;
    }

    @NonNull
    public PaymentState getCurrentState() {
        return currentState;
    }

    public PaymentServiceErrorCode getPaymentServiceErrorCode() {
        return paymentServiceErrorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getCvmResetTimeout() {
        return cvmResetTimeout;
    }

    public void setCvmResetTimeout(long cvmResetTimeout) {
        this.cvmResetTimeout = cvmResetTimeout;
    }


    /**
     * First callback indicating payment transaction is started. This is called when the first APDU is received via NFC link
     */
    @Override
    public void onTransactionStarted() {
        AppLogger.i(AppConstants.APP_TAG, ".onTransactionStarted()"+AppConstants.STARTED);
        resetState();
        posCommDisconnectedErrCount = 0;
        currentState = PaymentState.STATE_ON_TRANSACTION_STARTED;
        if (AppBuildConfigurations.IS_PFP_ENABLED) {
            launchPaymentScreen();
        }else{
            //Note: Any UI operation in this callback delays the payment. Ideally in 1 tap ,
            //there is no need to show any UI. Be extra cautious if you are introducing any UI calls
            //as it will have performance degradation in 1-tap
//            Toast.makeText(
//                    context,
//                    R.string.transaction_started,
//                    Toast.LENGTH_LONG).show();
        }
        AppLogger.i(AppConstants.APP_TAG, ".onTransactionStarted()"+AppConstants.ENDED);
    }
    /**
     * Conditional
     * callback indicating first tap  transaction is completed. This is called only in case of PFP first tap completed
     *
     */
    @Override
    public void onFirstTapCompleted() {
        AppLogger.i(AppConstants.APP_TAG, "onFirstTapCompleted"+ AppConstants.STARTED);
        AppLogger.d(AppConstants.APP_TAG, "T1_Activity " + AppConstants.ENDED);
        showPaymentTransitionScreenForPFP();
        AppLogger.d(AppConstants.APP_TAG, "T2_Activity " + AppConstants.STARTED);
        PFPHelper.INSTANCE.initSDKs(context,false);
        AppLogger.i(AppConstants.APP_TAG, "onFirstTapCompleted"+ AppConstants.ENDED);
    }

    /**
     * Optional
     * <p>
     * Callback to indicate that payment requires CVM method. Use chVerificationMethod to determine what is the cvm type and display appropriate UI.
     */
    @Override
    public void onAuthenticationRequired(PaymentService paymentService, CHVerificationMethod chVerificationMethod, long cvmResetTimeout) {
        AppLogger.d(AppConstants.APP_TAG, ".onAuthenticationRequired() " + AppConstants.STARTED);
        AppLogger.d(AppConstants.APP_TAG, "T2_Activity "+AppConstants.ENDED);
        AppLogger.d(AppConstants.APP_TAG, "T0_TILL_T2_Activity " + AppConstants.ENDED);

        AppLogger.d(AppConstants.APP_TAG, "T3_Activity "+AppConstants.STARTED);
        resetState();
        posCommDisconnectedErrCount = 0;
        currentState = PaymentState.STATE_ON_AUTHENTICATION_REQUIRED;
        this.paymentService = paymentService;
        this.chVerificationMethod = chVerificationMethod;
        this.cvmResetTimeout = cvmResetTimeout;
        this.transactionContext = paymentService.getTransactionContext();
        launchPaymentScreen();
        AppLogger.d(AppConstants.APP_TAG, ".onPaymentServiceActivated() " + AppConstants.ENDED);

    }

    /**
     * Optional
     * <p>
     * Callback to indicate that CVM had been successfully provided by user. Show timer to user to tap on POS again for transaction.
     * <p>
     * This is called after onPaymentServiceActivated.
     *
     * @param paymentService
     */
    @Override
    public void onReadyToTap(PaymentService paymentService) {
        AppLogger.d(AppConstants.APP_TAG, ".onReadyToTap"+
                    " "+AppConstants.STARTED);
        AppLogger.d(AppConstants.APP_TAG, "T3_Activity "+AppConstants.ENDED);
        AppLogger.d(AppConstants.APP_TAG, "T4_Activity "+AppConstants.STARTED);

        resetState();

        posCommDisconnectedErrCount = 0;
        currentState = PaymentState.STATE_ON_READY_TO_TAP;
        this.paymentService = paymentService;
        this.transactionContext = paymentService.getTransactionContext();
        launchPaymentScreen();
        AppLogger.d(AppConstants.APP_TAG, ".onReadyToTap"+
                    " "+AppConstants.ENDED);
    }

    /**
     * Last callback for successful sending of payment data to POS. Please note that transaction result is not known to SDK since transactions are
     * online.
     * This only indicates that payment data had been sent to POS successfully.
     *
     * @param transactionContext
     */
    @Override
    public void onTransactionCompleted(TransactionContext transactionContext) {
        PFPHCEService.apduCounter=0;
        AppLogger.d(AppConstants.APP_TAG, "T5_Activity " + AppConstants.ENDED);
        AppLogger.d(AppConstants.APP_TAG, ".onTransactionCompleted"+
                    " "+AppConstants.STARTED);

        resetState();
        posCommDisconnectedErrCount = 0;
        currentState = PaymentState.STATE_ON_TRANSACTION_COMPLETED;
        this.transactionContext = transactionContext;



        launchPaymentScreen();
        AppLogger.d(AppConstants.APP_TAG, ".onTransactionCompleted"+
                    " "+AppConstants.ENDED);



    }




    /**
     * Error callback to indicate that an error had happen.
     *
     * @param sdkPaymentServiceErrorCode
     */
    @Override
    public void onError(SDKError<PaymentServiceErrorCode> sdkPaymentServiceErrorCode) {
        AppLogger.d(AppConstants.APP_TAG, ".onError"+
                    " "+AppConstants.STARTED);
        resetState();

        currentState = PaymentState.STATE_ON_ERROR;
        this.transactionContext = null;
        this.paymentServiceErrorCode = sdkPaymentServiceErrorCode.getErrorCode();
        this.errorMessage = sdkPaymentServiceErrorCode.getErrorMessage();
        this.additionalInformation = sdkPaymentServiceErrorCode.getAdditionalInformation();

        if (sdkPaymentServiceErrorCode == null || paymentServiceErrorCode == PaymentServiceErrorCode.POS_COMM_DISCONNECTED) {
            if (posCommDisconnectedErrCount < ERROR_THRESHOLD) {
                posCommDisconnectedErrCount++;
                paymentExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        PFPHCEService.apduCounter=0;
                        launchPaymentScreen();
                    }
                }, ERROR_DELAY);
            } else {
                posCommDisconnectedErrCount = 0;
            }
        } else {
            PFPHCEService.apduCounter=0;
            //other errors
            posCommDisconnectedErrCount = 0;
            launchPaymentScreen();
        }
        AppLogger.d(AppConstants.APP_TAG, ".onError"+
                    " "+AppConstants.STARTED);

    }


    /**
     * Start of non-callback methods.
     */

    /**
     * Reset state variables
     */
    void resetState() {
        paymentExecutor.cancelAllPendingOps();
        this.currentState = PaymentState.STATE_NONE;
        this.paymentService = null;
        this.chVerificationMethod = null;
        this.paymentServiceErrorCode = null;
        this.transactionContext = null;
        this.errorMessage = null;
        AppLogger.d(TAG, "payment resetState is invoked to clear payment data in between states");
    }

    private void launchPaymentScreen() {
        AppLogger.d(AppConstants.APP_TAG, "launchPaymentScreen() with state :"
                + this.currentState + " " + AppConstants.STARTED);
        Intent intent = new Intent(context, PaymentContactlessActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(PAYMENT_STATE_EXTRA, currentState);
        context.startActivity(intent);
        AppLogger.d(AppConstants.APP_TAG, "launchPaymentScreen() with state :"
                + this.currentState + " " + AppConstants.ENDED);

    }


    public void showPaymentTransitionScreenForPFP(){
        AppLogger.d(TAG, "showPaymentTransitionScreenForPFP");
        this.currentState=PaymentState.STATE_ON_TRANSACTION_STARTED;
        launchPaymentScreen();
    }

    public void showPaymentTimeoutScreen(){
        AppLogger.d(TAG, "showPaymentTimeoutScreen");
        this.currentState=PaymentState.STATE_ON_ERROR;
        this.errorMessage=context.getString(R.string.payment_timeout);
        launchPaymentScreen();
    }

}
