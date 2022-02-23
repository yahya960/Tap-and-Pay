package test.hcesdk.mpay.dsrp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardManager;
import com.gemalto.mfs.mwsdk.dcm.PaymentType;
import com.gemalto.mfs.mwsdk.payment.CHVerificationMethod;
import com.gemalto.mfs.mwsdk.payment.PaymentBusinessManager;
import com.gemalto.mfs.mwsdk.payment.PaymentBusinessService;
import com.gemalto.mfs.mwsdk.payment.PaymentServiceErrorCode;
import com.gemalto.mfs.mwsdk.payment.engine.CryptogramDataType;
import com.gemalto.mfs.mwsdk.payment.engine.MasterCardTransactionContext;
import com.gemalto.mfs.mwsdk.payment.engine.PaymentInputData;
import com.gemalto.mfs.mwsdk.payment.engine.PaymentService;
import com.gemalto.mfs.mwsdk.payment.engine.RemotePaymentServiceListener;
import com.gemalto.mfs.mwsdk.payment.engine.TransactionContext;
import com.gemalto.mfs.mwsdk.payment.engine.TransactionType;
import com.gemalto.mfs.mwsdk.payment.engine.remote.RemotePaymentOutputData;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKController;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKError;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKServiceState;
import com.gemalto.mfs.mwsdk.utils.chcodeverifier.CHCodeVerifier;

import androidx.appcompat.app.AppCompatActivity;

import test.hcesdk.mpay.R;
import test.hcesdk.mpay.payment.DeviceCVMActivity;
import test.hcesdk.mpay.payment.contactless.FragmentResult;
import test.hcesdk.mpay.util.AppLogger;
import test.hcesdk.mpay.util.SharedPreferenceUtils;
import test.hcesdk.mpay.util.Util;

public class PaymentDSRPActivity extends AppCompatActivity implements RemotePaymentServiceListener {
    private static final String TAG = ".PaymentDSRPActivity";
    View progressbar;

    private static final int REQ_CODE_FINGERPRINT = 10000;

    private View outputView;
    private TextView tvPaymentOutput;
    private Spanned spannedTransactionContext;
    private Spanned spannedRemotePaymentData;
    private View tvProgressbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.title_payment);
        setContentView(R.layout.activity_payment_dsrp);
        progressbar = findViewById(R.id.progressBar);
        tvProgressbar = findViewById(R.id.tv_payment_processing_msg);

        outputView = findViewById(R.id.scrollview_dsrp);

        tvPaymentOutput = findViewById(R.id.tv_payment_output);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().setDisplayShowHomeEnabled(true);
        byte[] transactionType = getIntent().getExtras().getByteArray("transactionType");
        TransactionType transactionType1 = TransactionType.get(transactionType[0]);
        char currencyCode = 65;
        char countryCode = 702;
        long amount = getIntent().getExtras().getLong("amount");
        CryptogramDataType cryptogramDataType = (CryptogramDataType) getIntent().getExtras().get("cryptogramType");
        performMCDSRPCryptogram(amount, currencyCode, countryCode, transactionType1, cryptogramDataType);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE_FINGERPRINT) {
            // Check for result code RESULT_FIRST_USER.
            // Activity result is RESULT_CANCELLED if activity stack is cleared by singleInstance
            // launch mode.
            if (resultCode == RESULT_FIRST_USER) {
                //cancel current payment
                deactivatePaymentService();
                //finish();
            }
        }
    }

    public void toggleProgress(boolean show) {
        progressbar.setVisibility(show ? View.VISIBLE : View.GONE);
        tvProgressbar.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void deactivatePaymentService() {
        PaymentBusinessService paymentServ = PaymentBusinessManager.getPaymentBusinessService();
        if (paymentServ != null) {
            paymentServ.deactivate();
        }
    }

    private void performMCDSRPCryptogram(long amount, char currencyCode, char countryCode, TransactionType transactionType, CryptogramDataType cryptogramDataType) {
        toggleProgress(true);

        try {
            PaymentInputData paymentInputData = new PaymentInputData.PaymentInputBuilder(PaymentType.DSRP)
                    .withRemotePaymentParameters(amount, currencyCode)
                    .withMCRemotePaymentParameters(countryCode, transactionType, cryptogramDataType,
                            generateUnPredictableNumber())
                    .build();
            PaymentBusinessManager.getPaymentBusinessService().generateApplicationCryptogram(PaymentType.DSRP,
                    paymentInputData, this);
        }catch (IllegalStateException illegalStateException){
            illegalStateException.printStackTrace();
            Toast.makeText(this,illegalStateException.getMessage(),Toast.LENGTH_LONG).show();
        }catch (IllegalArgumentException illegalArgumentException){
            illegalArgumentException.printStackTrace();
            Toast.makeText(this,illegalArgumentException.getMessage(),Toast.LENGTH_LONG).show();
        }

    }

    private long generateUnPredictableNumber() {
        long unPredictableNumber = 12345;
        return unPredictableNumber;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**********************************************************/
    /*                Payment Service listener                */
    /**********************************************************/
    @Override
    public void onAuthenticationRequired(PaymentService paymentService, CHVerificationMethod chVerificationMethod, long cvmResetTimeout) {
        toggleProgress(false);
        if (chVerificationMethod == CHVerificationMethod.BIOMETRICS
                || chVerificationMethod == CHVerificationMethod.DEVICE_KEYGUARD) {
            Intent intent = new Intent(this, DeviceCVMActivity.class);
            intent.putExtra(DeviceCVMActivity.EXTRA_CVM, chVerificationMethod);
            overridePendingTransition(0, 0);
            startActivityForResult(intent, REQ_CODE_FINGERPRINT);
        } else {
            // NOTE: Not supported any other verification
            AppLogger.e(TAG, "Verification method " + chVerificationMethod + " not currently supported!");
            deactivatePaymentService();
        }
    }

    @Override
    public void onDataReadyForPayment(PaymentService paymentService, TransactionContext transactionContext) {
        AppLogger.i(TAG, "onDataReadyForPayment");

        toggleProgress(false);
        RemotePaymentOutputData remotePaymentData = paymentService.getRemotePaymentData();

        outputView.setVisibility(View.VISIBLE);

        readRemotePaymentDataAndTrxContext(remotePaymentData,transactionContext);
        handleCardDisplay(findViewById(R.id.parent));
        deactivatePaymentService();
    }

    private void readRemotePaymentDataAndTrxContext(RemotePaymentOutputData remotePaymentData,TransactionContext transactionContext) {
        AppLogger.i(TAG, "readRemotePaymentDataAndTrxContext");
        String spannedTextInput = "<font color='blue'><b>Remote Payment Output</b></font><br><br>"
                + "<b>CryptogramData</b><br><font color='blue'>" + Util.byteArrayToHexaStr(remotePaymentData.getCryptogramData()) + "</font><br>"
                + "<b>DPAN</b><br><font color='blue'>" + remotePaymentData.getDpan() + "</font><br>"
                + "<b>DPANSequenceNumber</b><br><font color='blue'>" + remotePaymentData.getDpanSequenceNumber() + "</font><br>"
                + "<b>DPANExpirationDate</b><br><font color='blue'>" + remotePaymentData.getDpanExpirationDate() + "</font><br>"
                + "<b>PAR</b><br><font color='blue'>" + remotePaymentData.getPAR() + "</font><br>"
                + "<b>Track2EquivalentData</b><br><font color='blue'>" + remotePaymentData.getTrack2EquvalentData() + "</font><br>"
                + "<b>CryptogramType</b><br><font color='blue'>" + remotePaymentData.getCryptogramDataType().name() + "</font><br>";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            spannedRemotePaymentData = Html.fromHtml(spannedTextInput, Html.FROM_HTML_MODE_COMPACT);
        } else {
            spannedRemotePaymentData = Html.fromHtml(spannedTextInput);
        }
        String transactionIDString="";
        if(transactionContext instanceof MasterCardTransactionContext){
            MasterCardTransactionContext masterCardTransactionContext=(MasterCardTransactionContext)transactionContext;
            if(masterCardTransactionContext.getTransactionId()!=null && masterCardTransactionContext.getTransactionId().length>0) {
                transactionIDString = "<b>TransactionId </b><br><font color='blue'>" + Util.byteArrayToHexaStr(masterCardTransactionContext.getTransactionId()) + "</font><br>";
            }
        }
        spannedTextInput += "<br><br><font color='blue'><b>TransactionContext</b></font><br><br>"
                +"<b>Amount</b><br><font color='blue'>" + transactionContext.getAmount() + "</font><br>"
                +transactionIDString
                + "<b>Currency Code</b><br><font color='blue'>" + Util.byteArrayToHexaStr(transactionContext.getCurrencyCode()) + "</font><br>"
                + "<b>Transaction Date</b><br><font color='blue'>" + Util.byteArrayToHexaStr(transactionContext.getTrxDate()) + "</font><br>"
                + "<b>Transaction Type</b><br><font color='blue'>" + Util.byteArrayToHexaStr(new byte[]{transactionContext.getTrxType()}) + "</font><br>"
                + "<b>Aid</b><br><font color='blue'>" + transactionContext.getAid() + "</font><br>"
                + "<b>Scheme</b><br><font color='blue'>" + transactionContext.getScheme().name() + "</font><br>";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            spannedTransactionContext = Html.fromHtml(spannedTextInput, Html.FROM_HTML_MODE_COMPACT);
        } else {
            spannedTransactionContext = Html.fromHtml(spannedTextInput);
        }

        tvPaymentOutput.setText(spannedTransactionContext);
    }





    @Override
    public void onError(SDKError<PaymentServiceErrorCode> sdkPaymentServiceErrorCode) {
        AppLogger.e(TAG, "Failed to generate DSRP payment data with error code " + sdkPaymentServiceErrorCode.getErrorCode());
        Toast.makeText(this, "Failed to generate DSRP payment data with error code " + sdkPaymentServiceErrorCode.getErrorCode(), Toast.LENGTH_SHORT).show();
        deactivatePaymentService();
    }

    private void handleCardDisplay(final View view) {
        if(!SDKController.getInstance().getSDKServiceState().equals(SDKServiceState.STATE_INITIALIZED)){
            return;
        }
        String currentDefaultCardTokenID = DigitalizedCardManager
                .getDefault(PaymentType.CONTACTLESS, null).waitToComplete().getResult();
        if(currentDefaultCardTokenID != null && currentDefaultCardTokenID.length()>0) {
            String currentDigitalizedCardID = DigitalizedCardManager.getDigitalCardId(currentDefaultCardTokenID);

            final String text=

                    "<b>Token ID: </b> <font color='blue'>" + currentDefaultCardTokenID + "</font> <br>"
                            + "<b>Digital Card ID: </b><font color='blue'>" + currentDigitalizedCardID + "</font><br>";

            final TextView defaultCardInfo = view.findViewById(R.id.tvCardInfo);
            defaultCardInfo.setText(Html.fromHtml(text));
            handleForDefaultCardChangesWithinTransaction(view,currentDefaultCardTokenID,currentDigitalizedCardID);
        }

    }

    private void handleForDefaultCardChangesWithinTransaction(final View view,final String currentDefaultCardTokenID,final String currentDigitalizedCardID) {
        AppLogger.d(TAG, ".handleForDefaultCardChangesWithinTransaction");

        AppLogger.d(TAG, ".currentDefaultCardTokenID"+currentDefaultCardTokenID);
        String originalDefaultCardTokenID= SharedPreferenceUtils.getDefaultCard(this.getApplicationContext());
        AppLogger.d(TAG, ".originalDefaultCardTokenID"+originalDefaultCardTokenID);
        String originalDigitalizedCardID="";

        if(originalDefaultCardTokenID!=null && !originalDefaultCardTokenID.equalsIgnoreCase(currentDefaultCardTokenID)){
            AppLogger.d(TAG, " set originalDefaultCardTokenID as default started");
            DigitalizedCardManager.getDigitalizedCard(originalDefaultCardTokenID).setDefault(PaymentType.CONTACTLESS, null).waitToComplete();
            AppLogger.d(TAG, " set originalDefaultCardTokenID as default ended");
            originalDigitalizedCardID=DigitalizedCardManager.getDigitalCardId(originalDefaultCardTokenID);
        }else{
            originalDefaultCardTokenID=currentDefaultCardTokenID;
            originalDigitalizedCardID=currentDigitalizedCardID;
        }

        final TextView defaultCardInfo = view.findViewById(R.id.tvCardInfoOriginalDefault);
        final String text=

                "<b>Token ID: </b> <font color='blue'>" + originalDefaultCardTokenID + "</font> <br>"
                        + "<b>Digital Card ID: </b><font color='blue'>" + originalDigitalizedCardID + "</font><br>";

        defaultCardInfo.setText(Html.fromHtml(text));

    }
}
