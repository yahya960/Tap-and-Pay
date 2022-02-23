package test.hcesdk.mpay.addCard;


import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputEditText;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.gemalto.mfs.mwsdk.mobilegateway.MGCardEnrollmentService;
import com.gemalto.mfs.mwsdk.mobilegateway.MobileGatewayError;
import com.gemalto.mfs.mwsdk.mobilegateway.MobileGatewayManager;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.InputMethod;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.IssuerData;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.TermsAndConditions;
import com.gemalto.mfs.mwsdk.mobilegateway.listener.CardEligibilityListener;
import com.gemalto.mfs.mwsdk.mobilegateway.utils.MGCardInfoEncryptor;

import test.hcesdk.mpay.R;
import test.hcesdk.mpay.util.AppLogger;
import test.hcesdk.mpay.util.Constants;
import test.hcesdk.mpay.util.SDKHelper;

public class AddCardFragment extends Fragment implements CardEligibilityListener {

    private static final String TAG= AddCardFragment.class.getSimpleName();
    EditText etPAN;
    TextInputEditText etCVV, etExpiry;
    Button btnAdd;

    boolean isPANvisa;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        Thread thread =new Thread(new Runnable() {
            @Override
            public void run() {
                //Firebase API has limitation when there are multiple sender ID, the onNewToken is triggered only for default SENDER_ID.
                // So it is prudent to check for updatePushToken regularly after SDK initialization as well.
                //And it is prudent to check for updatePushToken just before card enrollment process begin as well.
                SDKHelper.updateFirebaseToken(getContext());
            }
        });
        thread.start();

        View view = inflater.inflate(R.layout.fragment_add_card, container, false);
        etPAN = view.findViewById(R.id.etPAN);
        etCVV = view.findViewById(R.id.etCVV);
        etExpiry = view.findViewById(R.id.etExpiry);
        btnAdd = view.findViewById(R.id.btnAdd);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addCard();
            }
        });

        //preload card info
        isPANvisa = true;
        etPAN.setText(Constants.PRELOADED_CARD_PAN);
        etExpiry.setText(Constants.PRELOADED_CARD_EXPIRY);
        etCVV.setText(Constants.PRELOADED_CARD_CVV);

        etPAN.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View view) {

                if(isPANvisa) {
                    etPAN.setText(Constants.PRELOADED_CARD_PAN_MASTERCARD);
                } else {
                    etPAN.setText(Constants.PRELOADED_CARD_PAN_VISA);
                }

                isPANvisa = !isPANvisa;

                return true;
            }
        });


        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        toggleProgress(false);

    }

    private void addCard() {
        //validate input
        if (etPAN.getText().length() < 16) {
            Toast.makeText(getActivity(), R.string.invalid_pan, Toast.LENGTH_SHORT).show();
            return;
        }

        if (etExpiry.getText().length() < 4) {
            Toast.makeText(getActivity(), R.string.invalid_expiry, Toast.LENGTH_SHORT).show();
            return;
        }

        if (etCVV.getText().length() < 3) {
            Toast.makeText(getActivity(), R.string.invalid_cvv, Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] pubKeyBytes = MGCardInfoEncryptor.parseHex(Constants.PUBLIC_KEY_USED);
        byte[] subKeyBytes = MGCardInfoEncryptor.parseHex(Constants.SUBJECT_IDENTIFIER_USED);
        byte[] panBytes = etPAN.getText().toString().trim().replace(" ", "").getBytes();
        byte[] expBytes = etExpiry.getText().toString().getBytes();
        byte[] cvvBytes = etCVV.getText().toString().getBytes();
        byte[] encData = MGCardInfoEncryptor.encrypt(pubKeyBytes, subKeyBytes,
                panBytes, expBytes, cvvBytes);

        toggleProgress(true);
        AppLogger.d(TAG,"Starting enrollment checkcardEligility");
        MGCardEnrollmentService enrollmentService = MobileGatewayManager.INSTANCE.getCardEnrollmentService();

        //InputMethod.BANK_APP is required for GreenFlow
        enrollmentService.checkCardEligibility(encData, InputMethod.MANUAL, "en", this, getDeviceSerial());
        AppLogger.d(TAG,"Started enrollment checkcardEligility");
    }

    private String getDeviceSerial() {
        return Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    @Override
    public void onSuccess(TermsAndConditions termsAndConditions, IssuerData issuerData) {
        if(getActivity() == null){
            return;
        }
        toggleProgress(false);

        TermsFragment frag = new TermsFragment();
        frag.termsAndConditions = termsAndConditions;
        ((AddCardActivity) getActivity()).switchFragment(frag, true);
    }

    @Override
    public void onError(MobileGatewayError mobileGatewayError) {
        toggleProgress(false);
        Toast.makeText(getActivity(), mobileGatewayError.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private void toggleProgress(boolean show) {
        if (getActivity() != null) {
            ((AddCardActivity) getActivity()).toggleProgress(show);
        }
    }
}
