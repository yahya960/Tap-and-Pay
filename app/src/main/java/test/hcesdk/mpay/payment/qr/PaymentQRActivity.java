package test.hcesdk.mpay.payment.qr;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.gemalto.mfs.mwsdk.cdcvm.DeviceCVMVerifier;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCard;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardManager;
import com.gemalto.mfs.mwsdk.dcm.PaymentType;
import com.gemalto.mfs.mwsdk.payment.CHVerificationMethod;
import com.gemalto.mfs.mwsdk.payment.PaymentBusinessManager;
import com.gemalto.mfs.mwsdk.payment.PaymentBusinessService;
import com.gemalto.mfs.mwsdk.payment.PaymentServiceErrorCode;
import com.gemalto.mfs.mwsdk.payment.engine.PaymentInputData;
import com.gemalto.mfs.mwsdk.payment.engine.PaymentService;
import com.gemalto.mfs.mwsdk.payment.engine.QRCodePaymentServiceListener;
import com.gemalto.mfs.mwsdk.payment.engine.TransactionContext;
import com.gemalto.mfs.mwsdk.payment.engine.qrcode.QRCodeData;
import com.gemalto.mfs.mwsdk.utils.chcodeverifier.CHCodeVerifier;
import com.gemalto.mfs.mwsdk.utils.chcodeverifier.CHCodeVerifierListener;
import com.gemalto.mfs.mwsdk.utils.chcodeverifier.CHCodeVerifierErrorCode;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKError;
import com.payneteasy.tlv.HexUtil;

import org.json.JSONException;
import org.json.JSONObject;

import test.hcesdk.mpay.App;
import test.hcesdk.mpay.R;
import test.hcesdk.mpay.payment.DeviceCVMActivity;
import test.hcesdk.mpay.payment.contactless.FragmentResult;
import test.hcesdk.mpay.payment.delegatedauth.DelegatedCDCVMActivity;
import test.hcesdk.mpay.util.AppLogger;
import test.hcesdk.mpay.util.SharedPreferenceUtils;
import test.hcesdk.mpay.util.TransactionContextHelper;
import test.hcesdk.mpay.util.Util;

import static test.hcesdk.mpay.payment.delegatedauth.DelegatedCDCVMActivity.EXTRA_CVM;
import static test.hcesdk.mpay.payment.delegatedauth.DelegatedCDCVMActivity.EXTRA_DESCRIPTION;
import static test.hcesdk.mpay.payment.delegatedauth.DelegatedCDCVMActivity.EXTRA_TITLE;

public class PaymentQRActivity extends AppCompatActivity implements QRCodePaymentServiceListener {
    private static final String TAG = ".PaymentQRActivity";
    private static final int MAX_ALLOWED_TIME_DIFFERENCE = 1000;
    View progressLayout;
    private TransactionContext transactionContext;
    public static final String EXTRA_QR_TOKEN_ID = "PaymentQRActivity.EXTRA_QR_TOKEN_ID";
    private DeviceCVMVerifier verifier;
    private static final int REQ_CODE_FINGERPRINT = 10000;
    public static final int REQ_CODE_DELEGATE_CDCVM = 110;
    private String qrType = "Standard";
    private String qrTokenId;

    CHCodeVerifier chCodeVerifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.title_payment);
        setContentView(R.layout.activity_payment);
        progressLayout = findViewById(R.id.progress_layout);

        qrType = getIntent().getExtras().getString("QR_TYPE");
        qrTokenId = getIntent().getExtras().getString(EXTRA_QR_TOKEN_ID);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().setDisplayShowHomeEnabled(true);

        generateQRCode(qrTokenId);
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
                finish();
            }
        } else if (requestCode == REQ_CODE_DELEGATE_CDCVM) {
            verifyTransactionStatus(resultCode == RESULT_OK, System.currentTimeMillis());
        }
    }

    public void switchFragment(Fragment fragment, boolean replace) {
        if (replace) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
        } else {
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commit();
        }
    }

    public void toggleProgress(boolean show) {
        progressLayout.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void deactivatePaymentService() {
        PaymentBusinessService paymentServ = PaymentBusinessManager.getPaymentBusinessService();
        if (paymentServ != null) {
            paymentServ.deactivate();
        }
    }

    private void setupGSK_43(CHCodeVerifier chCodeVerifier, String message) {

    }

    private void generateQRCode(String qrTokenId) {
        toggleProgress(true);
        //payload
        final String DEFAULT_AMOUNT_ZERO = "000000000000";
        final String DEFAULT_AID = "0000000000";
        try {
            PaymentInputData paymentInputData = new PaymentInputData.PaymentInputBuilder(PaymentType.QR)
                    .withQRCodePaymentParameters(DEFAULT_AMOUNT_ZERO, (char) 0, (char) 0)
                    .withPureQRCodePaymentParameters("".getBytes(), DEFAULT_AID.getBytes())
                    .build();

            DigitalizedCard card = DigitalizedCardManager.getDigitalizedCard(qrTokenId);
            if (card.isDefault(PaymentType.QR,null).waitToComplete().getResult()){
                PaymentBusinessManager.getPaymentBusinessService().generateApplicationCryptogram(PaymentType.QR, paymentInputData, this);
            }else{
                PaymentBusinessManager.getPaymentBusinessService().generateApplicationCryptogram(card,PaymentType.QR, paymentInputData, this);
            }

        } catch (IllegalStateException exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    CHCodeVerifierListener chCodeVerifierListener = new CHCodeVerifierListener() {
        @Override
        public void onVerificationError(final int i, SDKError<CHCodeVerifierErrorCode> sdkCHCCodeVerifierErrorCode) {
            AppLogger.i(TAG, "wallet pin error: " + i);
            AlertDialog.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = new AlertDialog.Builder(getApplicationContext(), android.R.style.Theme_Material_Dialog_Alert);
            } else {
                builder = new AlertDialog.Builder(getApplicationContext());
            }
            builder.setTitle(getString(R.string.wallet_pin))
                    .setMessage(getString(R.string.text_retry_wallet_pin, i))
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            setupGSK_43(chCodeVerifier, getString(R.string.text_retry_wallet_pin, i));
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            deactivatePaymentService();
                            finish();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }

        @Override
        public void onVerificationSuccess() {
            AppLogger.i(TAG, ".onVerificationSuccess. expect callback for onDataReady");
            toggleProgress(true);
        }

        @Override
        public void maxRetryReached() {
            // NOTE: Dialog fragment for secure key pad,later will change Secure key pad as a view
            AppLogger.i(TAG, "wallet pin max reached");
            AlertDialog.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = new AlertDialog.Builder(getApplicationContext(), android.R.style.Theme_Material_Dialog_Alert);
            } else {
                builder = new AlertDialog.Builder(getApplicationContext());
            }
            builder.setTitle(getString(R.string.wallet_pin))
                    .setMessage(getString(R.string.maximum_attempt))
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            deactivatePaymentService();
                            finish();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    };


    /**********************************************************/
    /*                Payment Service listener                */

    /**********************************************************/
    @Override
    public void onAuthenticationRequired(PaymentService paymentService, CHVerificationMethod chVerificationMethod, long cvmResetTimeout) {
        toggleProgress(false);
        transactionContext = paymentService.getTransactionContext();
        boolean delegatedCDCVM = SharedPreferenceUtils.getDelegatedCDCVMStatus(this);

        if (chVerificationMethod == CHVerificationMethod.WALLET_PIN) {
            chCodeVerifier = (CHCodeVerifier) paymentService.getCHVerifier(chVerificationMethod);
            chCodeVerifier.setCHCodeVerifierListener(chCodeVerifierListener);
            setupGSK_43(chCodeVerifier, getResources().getString(R.string.enter_pin));
        } else if (chVerificationMethod == CHVerificationMethod.BIOMETRICS
                || chVerificationMethod == CHVerificationMethod.DEVICE_KEYGUARD) {

            if(delegatedCDCVM) {
                //For Delegated CDCVM
                verifier = (DeviceCVMVerifier) paymentService
                        .getCHVerifier(chVerificationMethod);

                long authTimeDifference = System.currentTimeMillis() - App.lastAuthTS;
                long finalDiff = cvmResetTimeout - authTimeDifference;
                Log.d(TAG, "Auth time difference is " + authTimeDifference);
                Log.d(TAG, "CVM Reset time is " + cvmResetTimeout);
                Log.d(TAG, "Final time difference is " + finalDiff);
                if (finalDiff >= MAX_ALLOWED_TIME_DIFFERENCE) {
                    Log.d(TAG, "Final time difference is more than " + MAX_ALLOWED_TIME_DIFFERENCE);
                    verifyTransactionStatus(true, App.lastAuthTS);
                    App.lastAuthTS = 0;
                    return;
                }

                final Intent intent = new Intent(this, DelegatedCDCVMActivity.class);
                intent.putExtra(EXTRA_TITLE, getString(R.string.verify_using_fingerprint));
                intent.putExtra(EXTRA_CVM, chVerificationMethod);
                String description = "Transaction Amount: " +
                        TransactionContextHelper.formatAmountWithCurrency(paymentService.getTransactionContext());
                if(description.contains("null")) {
                    intent.putExtra(EXTRA_DESCRIPTION, "");
                } else {
                    intent.putExtra(EXTRA_DESCRIPTION, description);
                }
                startActivityForResult(intent, REQ_CODE_DELEGATE_CDCVM);

            } else {
                Intent intent = new Intent(this, DeviceCVMActivity.class);
                intent.putExtra(DeviceCVMActivity.EXTRA_CVM, chVerificationMethod);
                overridePendingTransition(0, 0);
                startActivityForResult(intent, REQ_CODE_FINGERPRINT);
            }
        } else {
            // NOTE: Not supported any other verification
            AppLogger.e(TAG, "Verification method " + chVerificationMethod + " not currently supported!");
            deactivatePaymentService();
        }
    }

    private void verifyTransactionStatus(boolean isSuccess, long successCurrentTS) {
        if(isSuccess) {
            Toast.makeText(this, "Delegated Authentication Successful", Toast.LENGTH_LONG).show();
            verifier.onDelegatedAuthPerformed(successCurrentTS);
        } else {

            Toast.makeText(this, "Delegated Authentication Cancelled", Toast.LENGTH_LONG).show();
            verifier.onDelegatedAuthCancelled();
            FragmentResult resultFrag = FragmentResult.getInstance(false,
                    "Transaction authentication failed", transactionContext);
            switchFragment(resultFrag, true);
        }
    }

    @Override
    public void onDataReadyForPayment(PaymentService paymentService, TransactionContext transactionContext) {
        toggleProgress(false);
        QRCodeData qrCodeData = paymentService.getQRCodeData();
        String statusCode = HexUtil.toHexString(qrCodeData.getStatusWord());
        if (statusCode.equals("9000")) {
            final QRCodeFragment qrFragment = new QRCodeFragment();
            qrFragment.qrCodeData = qrCodeData;
            qrFragment.transactionContext = transactionContext;
            qrFragment.qrType = qrType;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    switchFragment(qrFragment, true);
                }
            }, 200);
        } else {
            AppLogger.e(TAG, "Failed to generate QRCode with status word " + statusCode);
        }
        deactivatePaymentService();
    }

    @Override
    public void onError(SDKError<PaymentServiceErrorCode> sdkPaymentServiceErrorCode) {
        AppLogger.e(TAG, "Failed to generate QR code with error code " + sdkPaymentServiceErrorCode.getErrorCode());
        Toast.makeText(this, "Failed to generate QR code with error code " + sdkPaymentServiceErrorCode.getErrorCode(), Toast.LENGTH_SHORT).show();
        deactivatePaymentService();
    }
}
