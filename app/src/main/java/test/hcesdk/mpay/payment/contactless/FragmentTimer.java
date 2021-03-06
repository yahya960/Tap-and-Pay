package test.hcesdk.mpay.payment.contactless;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gemalto.mfs.mwsdk.dcm.DigitalizedCard;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardManager;
import com.gemalto.mfs.mwsdk.dcm.PaymentType;
import com.gemalto.mfs.mwsdk.payment.CHVerificationMethod;
import com.gemalto.mfs.mwsdk.payment.CVMResetTimeoutListener;
import com.gemalto.mfs.mwsdk.payment.engine.PaymentService;
import com.gemalto.mfs.mwsdk.payment.engine.TransactionContext;

import test.hcesdk.mpay.App;
import test.hcesdk.mpay.R;
import test.hcesdk.mpay.app.AppBuildConfigurations;
import test.hcesdk.mpay.model.MyDigitalCard;
import test.hcesdk.mpay.util.AppExecutors;
import test.hcesdk.mpay.util.AppLogger;
import test.hcesdk.mpay.util.TransactionContextHelper;

public class FragmentTimer extends Fragment implements CVMResetTimeoutListener {

    private static final String TAG= FragmentTimer.class.getSimpleName();
    CircularProgressDrawable timerDrawable;
    AppExecutors appExecutors;
    TextView lblTimer;
    private Button btnChangeCard;
    ContactlessPayListener listener;

    private View.OnClickListener btnChangeCardOnClickListener;
    private MyDigitalCard defaultDigitalCard;

    /**
     * Interface to provide onClickListener for the button to change payment card.
     * This done to avoid setting the listener directly from the parent activity as that may lead
     * to crashes when fragment is being committed to the layout.
     * @param btnChangeCardListener
     * @return
     */
    public FragmentTimer setBtnChangeCardOnClickListener(final View.OnClickListener btnChangeCardListener) {
        this.btnChangeCardOnClickListener = btnChangeCardListener;
        return this;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment_timer, container, false);
        ImageView timerImg = view.findViewById(R.id.timer_image);
        lblTimer = view.findViewById(R.id.timer_label);


        timerDrawable = new CircularProgressDrawable(getContext());
        timerImg.setImageDrawable(timerDrawable);
        timerDrawable.start();

        listener = ((App) getActivity().getApplication()).getContactlessPayListener();
        appExecutors = ((App) getActivity().getApplication()).getAppExecutors();
        listener.getPaymentService().setCVMResetTimeoutListener(this);
        lblTimer.setText(String.valueOf(listener.getCvmResetTimeout()/1000));

        final ViewGroup buttonBar = view.findViewById(R.id.payment_button_bar);
        btnChangeCard = view.findViewById(R.id.btn_change_card);

        if (!AppBuildConfigurations.IS_PFP_ENABLED) {
            // The button is expected to be present
            // only while PFP is enabled
            buttonBar.setVisibility(View.GONE);
        }

        btnChangeCard.setOnClickListener(view1 -> {
            if (btnChangeCardOnClickListener != null) {
                btnChangeCard.setEnabled(false);
                btnChangeCard.setClickable(false);
                btnChangeCard.setActivated(false);
                btnChangeCardOnClickListener.onClick(view1);
            }
        });
        final TextView tvAmount = view.findViewById(R.id.tv_amount);
        final TransactionContext transactionContext = listener.getTransactionContext();
        if(transactionContext != null) {
            tvAmount.setText(TransactionContextHelper.formatAmountWithCurrency(transactionContext));
        }

        String defaultCardTokenID = DigitalizedCardManager
                .getDefault(PaymentType.CONTACTLESS, null).waitToComplete().getResult();

        DigitalizedCard sdkDigitalizeCard=DigitalizedCardManager.getDigitalizedCard(defaultCardTokenID);
        defaultDigitalCard=new MyDigitalCard(sdkDigitalizeCard);
        defaultDigitalCard.setDigitalizedCardId(DigitalizedCardManager.getDigitalCardId(defaultCardTokenID));

        final TextView defaultCardInfo = view.findViewById(R.id.tvCardInfo);

        final String text=

                "<b>Token ID: </b> <font color='blue'>" + defaultDigitalCard.getTokenId() + "</font> <br>"
                        + "<b>Digital Card ID: </b><font color='blue'>" + defaultDigitalCard.getDigitalizedCardId() + "</font><br>";

        defaultCardInfo.setText(Html.fromHtml(text));

        return view;
    }

    @Override
    public void onCredentialsTimeoutCountDown(final int i) {
        if (getActivity() == null) {
            return;
        }

        appExecutors.mainThread().execute(new Runnable() {
            @Override
            public void run() {
                if (getActivity() == null) {
                    return;
                }
                if(i<=10){
                    lblTimer.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }else if(i<=15){
                    lblTimer.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                }else{
                    lblTimer.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                }
                lblTimer.setText(String.valueOf(i));
            }
        });
    }

    @Override
    public void onCredentialsTimeout(PaymentService paymentService, CHVerificationMethod chVerificationMethod, long l) {

        AppLogger.d(TAG,"onCredentialsTimeout1");
        if (getActivity() == null) {
            return;
        }

        appExecutors.mainThread().execute(new Runnable() {
            @Override
            public void run() {
                AppLogger.d(TAG,"onCredentialsTimeout2");
                if (getActivity() == null) {
                    return;
                }
                AppLogger.d(TAG,"Show error and deactivate");
                FragmentResult resultFrag = FragmentResult.getInstance(false, getString(R.string.payment_timeout), listener.getTransactionContext());
                ((PaymentContactlessActivity) getActivity()).switchFragment(resultFrag, true);
                ((PaymentContactlessActivity) getActivity()).deactivatePaymentServiceAndResetState();
            }
        });
    }
}
