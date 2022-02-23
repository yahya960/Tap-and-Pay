package test.hcesdk.mpay.payment.contactless;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardManager;
import com.gemalto.mfs.mwsdk.dcm.PaymentType;
import com.gemalto.mfs.mwsdk.payment.PaymentBusinessManager;
import com.gemalto.mfs.mwsdk.payment.PaymentBusinessService;
import com.gemalto.mfs.mwsdk.payment.engine.TransactionContext;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKController;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKServiceState;

import test.hcesdk.mpay.App;
import test.hcesdk.mpay.R;
import test.hcesdk.mpay.util.AppLogger;
import test.hcesdk.mpay.util.SharedPreferenceUtils;
import test.hcesdk.mpay.util.TransactionContextHelper;

public class FragmentResult extends Fragment {
    private static final String KEY_RESULT = "test.hcesdk.mpay.payment.contactless.KEY_RESULT";
    private static final String KEY_MESSAGE = "test.hcesdk.mpay.payment.contactless.KEY_MESSAGE";
    private static final String KEY_AMOUNT = "test.hcesdk.mpay.payment.contactless.KEY_AMOUNT";

    private static final String TAG = FragmentResult.class.getSimpleName();

    TextView lblResult;
    private ContactlessPayListener listener;

    public static FragmentResult getInstance(boolean success, String message, TransactionContext transactionContext){
        Bundle args = new Bundle();
        args.putBoolean(KEY_RESULT, success);
        args.putString(KEY_MESSAGE, message);
        if(transactionContext != null){
            args.putString(KEY_AMOUNT, TransactionContextHelper.formatAmountWithCurrency(transactionContext));
        }
        FragmentResult frag = new FragmentResult();
        frag.setArguments(args);

        return frag;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment_result, container, false);
        lblResult = view.findViewById(R.id.payment_result);
        listener = ((App) getActivity().getApplication()).getContactlessPayListener();
        // Trying to set back the default card is not needed for payment flow with PFP
        // CPS should take care when deactivate is called.
        PaymentBusinessService pbs = PaymentBusinessManager.getPaymentBusinessService();
        if(pbs != null) {
            pbs.deactivate();
        }
        view.findViewById(R.id.btnOk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finish();
            }
        });

        final Bundle arguments = getArguments();

        if(arguments != null){
            boolean result = arguments.getBoolean(KEY_RESULT);
            String message = arguments.getString(KEY_MESSAGE);
            if(result){
                lblResult.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_payment_success, 0, 0);
            }else{
                lblResult.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_payment_failure, 0, 0);
            }
            lblResult.setText(message);


            final TextView tvAmount = view.findViewById(R.id.tv_amount);
            if(arguments.containsKey(KEY_AMOUNT)){
                tvAmount.setText(arguments.getString(KEY_AMOUNT));
                final TransactionContext transactionContext = listener.getTransactionContext();
                if(transactionContext != null) {
                    tvAmount.setText(TransactionContextHelper.formatAmountWithCurrency(transactionContext));
                }
            }
            else{
                view.findViewById(R.id.layout_transaction_info).setVisibility(View.GONE);
            }

            handleCardDisplay(view);
        }
        else{
            AppLogger.e(TAG, "Arguments were not provided ant thus UI has not been updated.");
        }



        return view;
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
        String originalDefaultCardTokenID= SharedPreferenceUtils.getDefaultCard(FragmentResult.this.getActivity().getApplicationContext());
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
