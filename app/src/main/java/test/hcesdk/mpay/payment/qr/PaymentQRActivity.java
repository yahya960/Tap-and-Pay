package test.hcesdk.mpay.payment.qr;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.gemalto.mfs.mwsdk.payment.CHVerificationMethod;
import com.gemalto.mfs.mwsdk.payment.PaymentBusinessManager;
import com.gemalto.mfs.mwsdk.payment.PaymentBusinessService;
import com.gemalto.mfs.mwsdk.payment.PaymentServiceErrorCode;
import com.gemalto.mfs.mwsdk.payment.engine.PaymentService;
import com.gemalto.mfs.mwsdk.payment.engine.QRCodePaymentServiceListener;
import com.gemalto.mfs.mwsdk.payment.engine.TransactionContext;
import com.gemalto.mfs.mwsdk.payment.engine.qrcode.QRCodeData;
import com.gemalto.mfs.mwsdk.utils.chcodeverifier.CHCodeVerifier;
import com.gemalto.mfs.mwsdk.utils.chcodeverifier.CHCodeVerifierListener;
import com.gemalto.mfs.mwsdk.utils.chcodeverifier.CHCodeVerifierErrorCode;
import com.gemalto.mfs.mwsdk.utils.securekeypad.SecureKeypadBuilder;
import com.gemalto.mfs.mwsdk.utils.securekeypad.SecureKeypadEventsListener;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKError;
import com.payneteasy.tlv.HexUtil;

import org.json.JSONException;
import org.json.JSONObject;

import test.hcesdk.mpay.R;
import test.hcesdk.mpay.payment.DeviceCVMActivity;
import test.hcesdk.mpay.util.AppLogger;
import test.hcesdk.mpay.util.Util;

public class PaymentQRActivity extends AppCompatActivity implements QRCodePaymentServiceListener {
    private static final String TAG = ".PaymentQRActivity";
    View progressLayout;

    private static final int REQ_CODE_FINGERPRINT = 10000;

    CHCodeVerifier chCodeVerifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.title_payment);
        setContentView(R.layout.activity_payment);
        progressLayout = findViewById(R.id.progress_layout);

        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().setDisplayShowHomeEnabled(true);

        try {
            generateQRCode();
        } catch (JSONException e) {
            e.printStackTrace();
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
        SecureKeypadBuilder mKeypadModel = Util.decorateKeyPadWithDefaultSettings(chCodeVerifier, getApplicationContext());
        mKeypadModel.setTextLabel(message);
        //boolean isScramble, boolean isDoublePassword, boolean isDialog,
        Fragment fragment = mKeypadModel.build(false, false, false, new SecureKeypadEventsListener() {
            @Override
            public void keyPressedCountChanged(int i, int i1) {
                AppLogger.i(TAG, "keyPressedCountChanged");
            }

            @Override
            public void textFieldSelected(int i) {
                AppLogger.i(TAG, "textFieldSelected");
            }
        });
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    private void generateQRCode() throws JSONException {
        toggleProgress(true);
        //payload
        final String DEFAULT_AMOUNT_ZERO = "000000000000";
        final String DEFAULT_CURRENCYCODE_ZERO = "0000";
        final String DEFAULT_AID = "0000000000";
        JSONObject payload = new JSONObject();
        payload.put("aid", DEFAULT_AID);
        payload.put("amountAuthorized", DEFAULT_AMOUNT_ZERO);
        payload.put("currencyCode", DEFAULT_CURRENCYCODE_ZERO);
        //payload.put("IssuerDiscretionaryData", Idd bytes);

        PaymentBusinessService paymentServ = PaymentBusinessManager.getPaymentBusinessService();
        paymentServ.generateQRCodePaymentData(payload.toString(), this);
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
        if (chVerificationMethod == CHVerificationMethod.WALLET_PIN) {
            chCodeVerifier = (CHCodeVerifier) paymentService.getCHVerifier(chVerificationMethod);
            chCodeVerifier.setCHCodeVerifierListener(chCodeVerifierListener);
            setupGSK_43(chCodeVerifier, getResources().getString(R.string.enter_pin));
        } else if (chVerificationMethod == CHVerificationMethod.BIOMETRICS
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
        toggleProgress(false);
        QRCodeData qrCodeData = paymentService.getQRCodeData();
        String statusCode = HexUtil.toHexString(qrCodeData.getStatusWord());
        if (statusCode.equals("9000")) {
            final QRCodeFragment qrFragment = new QRCodeFragment();
            qrFragment.qrCodeData = qrCodeData;
            qrFragment.transactionContext = transactionContext;
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
