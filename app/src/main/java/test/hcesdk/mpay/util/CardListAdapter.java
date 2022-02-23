package test.hcesdk.mpay.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.gemalto.mfs.mwsdk.dcm.DigitalizedCard;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardDetails;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardManager;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardState;
import com.gemalto.mfs.mwsdk.dcm.PaymentType;
import com.gemalto.mfs.mwsdk.mobilegateway.MobileGatewayManager;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.CardArt;
import com.gemalto.mfs.mwsdk.mobilegateway.exception.NoSuchCardException;
import com.gemalto.mfs.mwsdk.utils.async.AbstractAsyncHandler;
import com.gemalto.mfs.mwsdk.utils.async.AsyncResult;

import java.util.List;

import test.hcesdk.mpay.R;
import test.hcesdk.mpay.model.MyDigitalCard;

public class CardListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    public enum DisplayType {
        CARD_LIST_MAIN,
        CARD_LIST_CHOOSER
    }

    private static final String TAG = CardListAdapter.class.getName();
    //private AuthenticationPriorToPaymentManager authenticationPriorToPaymentManager;
    private Context context;
    private List<MyDigitalCard> cards;
    private CardListAdapterCallback callback;
    private final DisplayType displayType;

    public CardListAdapter(Context context, CardListAdapterCallback callback, List<MyDigitalCard> cards, final DisplayType displayType) {
        this.cards = cards;
        this.context = context;
        this.callback = callback;
        this.displayType = displayType;

    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return new MyViewHolder(inflater.inflate(R.layout.list_item_card, parent, false));
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MyViewHolder mHolder = (MyViewHolder) holder;

        final MyDigitalCard card = cards.get(position);
        int numberOfPayment = card.getCardStatus().getNumberOfPaymentsLeft();

        if (card.getSelected()) {
            mHolder.radioButton.setChecked(true);
        } else {
            mHolder.radioButton.setChecked(false);
        }
        //added code for Reperso Test
        DigitalizedCard dCard = DigitalizedCardManager.getDigitalizedCard(card.getTokenId());

        dCard.getCardDetails(new AbstractAsyncHandler<DigitalizedCardDetails>() {
            @Override
            public void onComplete(AsyncResult<DigitalizedCardDetails> asyncResult) {

                if(asyncResult.isSuccessful()) {

                    final String tvInfoText1 =
                            "<b>Token ID: </b> <font color='blue'>" + card.getTokenId() + "</font> <br>"
                                    + "<b>Digital Card ID: </b><font color='blue'>" + card.getDigitalizedCardId() + "</font><br>"
                                    + "<b>Default: </b><font color='blue'>" + card.isDefaultCardFlag() + "</font><br>"
                                    + "<b>Card State: </b><font color='blue'>" + card.getCardStatus().getState() + "</font><br>"
                                    + "<b>Payment Remaining: </b><font color='blue'>" + numberOfPayment + "</font><br>"
                                    + "<b>PAN Expiry: </b><font color='blue'>" + asyncResult.getResult().getPanExpiry() + "</font><br>"
                                    + "<b>DPAN LastDigit: </b><font color='blue'>" + asyncResult.getResult().getLastFourDigitsOfDPAN()+"" + "</font><br>"
                                    +  "<b>FPAN LastDigit: </b><font color='blue'>" + asyncResult.getResult().getLastFourDigits()+"" + "</font><br>";

                    mHolder.tvInfo.setText(Html.fromHtml(tvInfoText1));

                }
            }
        });
        //end code for Reperso Test

        final String tvInfoText =
                "<b>Token ID: </b> <font color='blue'>" + card.getTokenId() + "</font> <br>"
                        + "<b>Digital Card ID: </b><font color='blue'>" + card.getDigitalizedCardId() + "</font><br>"
                        + "<b>Default: </b><font color='blue'>" + card.isDefaultCardFlag() + "</font><br>"
                        + "<b>Card State: </b><font color='blue'>" + card.getCardStatus().getState() + "</font><br>"
                        + "<b>Payment Remaining: </b><font color='blue'>" + numberOfPayment + "</font><br>";

        mHolder.tvInfo.setText(Html.fromHtml(tvInfoText));

        if (card.isDefaultCardFlag()) {

            if (card.getCardStatus().getState() != DigitalizedCardState.ACTIVE) {
                DigitalizedCardManager.unsetDefaultCard(PaymentType.CONTACTLESS, new AbstractAsyncHandler<Void>() {
                    @Override
                    public void onComplete(AsyncResult<Void> asyncResult) {
                        Toast.makeText(context, R.string.unset_default_card_complete, Toast.LENGTH_LONG).show();
                    }
                });
                mHolder.imgBtnSetDefault.setImageResource(R.drawable.ic_not_default);

            } else {
                mHolder.imgBtnSetDefault.setImageResource(R.drawable.ic_default);
            }

        } else {
            mHolder.imgBtnSetDefault.setImageResource(R.drawable.ic_not_default);
        }

        mHolder.imgBtnSetDefault.setEnabled(!card.isDefaultCardFlag());
        mHolder.imgBtnSetDefault.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.setDefaultCardAction(card);
            }
        });

        mHolder.imgBtnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.onBtnDelete(card);
            }
        });

        mHolder.imgBtnGetHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.onBtnHistoryClicked(card);
            }
        });
        mHolder.cardPayment.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                callback.onLongClickCard(card);
                return true;

            }
        });
        mHolder.imgPayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.onPaymentClicked(card);
            }
        });

        mHolder.radioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onCheckboxClicked(card);
            }
        });
        if(card.isRemotePaymentSupported()) {
            mHolder.btnDsrp.setVisibility(View.VISIBLE);
            mHolder.btnDsrp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callback.onDsrpClicked(card);
                }
            });
        }else{
            mHolder.btnDsrp.setVisibility(View.GONE);
        }

        updateDisplay(displayType,mHolder);
    }

    private void updateDisplay(final DisplayType displayType, final MyViewHolder mHolder) {
        if (DisplayType.CARD_LIST_CHOOSER == displayType) {
            mHolder.imgBtnDelete.setVisibility(View.GONE);
            mHolder.imgBtnGetHistory.setVisibility(View.GONE);
            mHolder.imgBtnSetDefault.setVisibility(View.GONE);
            mHolder.imgPayment.setVisibility(View.GONE);
            mHolder.btnDsrp.setVisibility(View.GONE);
        }else {
            mHolder.radioButton.setVisibility(View.GONE);
        }
    }


    @Override
    public int getItemCount() {
        return cards.size();
    }

    public interface CardListAdapterCallback {
        void onBtnDelete(MyDigitalCard card);

        void onPaymentClicked(MyDigitalCard card);

        void setDefaultCardAction(MyDigitalCard card);

        void onBtnHistoryClicked(MyDigitalCard card);

        void onLongClickCard(MyDigitalCard card);

        void onCheckboxClicked(MyDigitalCard card);

        void onDsrpClicked(MyDigitalCard card);
    }

    private class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvInfo;
        CardView cardPayment;
        ImageButton imgBtnDelete, imgBtnSetDefault, imgBtnGetHistory,imgPayment;
        RadioButton radioButton;
        ImageView btnDsrp;

        public MyViewHolder(View itemView) {
            super(itemView);
            itemView.setTag(this);
            tvInfo = itemView.findViewById(R.id.tvCardInfo);
            imgBtnDelete = itemView.findViewById(R.id.imgBtnDelete);
            imgPayment = itemView.findViewById(R.id.imgBtnPayment);
            imgBtnSetDefault = itemView.findViewById(R.id.imgBtnSetDefault);
            imgBtnGetHistory = itemView.findViewById(R.id.imgBtnGetHistory);
            cardPayment = itemView.findViewById(R.id.card);
            radioButton = itemView.findViewById(R.id.radioButtonListView);
            btnDsrp = itemView.findViewById(R.id.btnDsrp);
        }
    }


}
