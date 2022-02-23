/*
 * ----------------------------------------------------------------------------
 *
 *     Copyright (c)  2015  -  GEMALTO DEVELOPEMENT - R&D
 *
 * -----------------------------------------------------------------------------
 * GEMALTO MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. GEMALTO SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * THIS SOFTWARE IS NOT DESIGNED OR INTENDED FOR USE OR RESALE AS ON-LINE
 * CONTROL EQUIPMENT IN HAZARDOUS ENVIRONMENTS REQUIRING FAIL-SAFE
 * PERFORMANCE, SUCH AS IN THE OPERATION OF NUCLEAR FACILITIES, AIRCRAFT
 * NAVIGATION OR COMMUNICATION SYSTEMS, AIR TRAFFIC CONTROL, DIRECT LIFE
 * SUPPORT MACHINES, OR WEAPONS SYSTEMS, IN WHICH THE FAILURE OF THE
 * SOFTWARE COULD LEAD DIRECTLY TO DEATH, PERSONAL INJURY, OR SEVERE
 * PHYSICAL OR ENVIRONMENTAL DAMAGE ("HIGH RISK ACTIVITIES"). GEMALTO
 * SPECIFICALLY DISCLAIMS ANY EXPRESS OR IMPLIED WARRANTY OF FITNESS FOR
 * HIGH RISK ACTIVITIES.
 *
 * -----------------------------------------------------------------------------
 */

package test.hcesdk.mpay.dsrp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.gemalto.mfs.mwsdk.dcm.DigitalizedCard;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardManager;
import com.gemalto.mfs.mwsdk.dcm.PaymentType;
import com.gemalto.mfs.mwsdk.payment.engine.CryptogramDataType;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKController;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKServiceState;

import test.hcesdk.mpay.R;
import test.hcesdk.mpay.payment.contactless.CardChooserActivity;
import test.hcesdk.mpay.payment.contactless.PaymentContactlessActivity;

public class DsrpActivity extends AppCompatActivity {

    /**
     * Tag for logging purpose
     */
    private final static String TAG = DsrpActivity.class.getName();
    public static final String EXTRA_TOKEN_ID = "DsrpActivity.EXTRA_TOKEN_ID";
    public static final String EXTRA_DIGITALIZED_CARD_ID = "DsrpActivity.EXTRA_DIGITALIZED_CARD_ID";
    private EditText editTextAmount;
    private Spinner spinnerTransactionType;
    private Spinner spinnerCryptogramType;
    private Button buttonSubmit;
    private String digitalCardId;
    private String tokenId;
    private final String[] transactionTypes = new String[] {
            "PURCHASE",
            "CASH",
            "PURCHASE_CASHBACK",
            "REFUND"
    };

    private final byte[][] transactionTypeIds = new byte[][] {
            {0x00},
            {0x01},
            {0x09},
            {0x20}
    };

    private final String[] cryptogramTypes = new String[] {
            "DE55",
            "UCAF"
    };

    private final CryptogramDataType[] cryptogramTypeObjects = new CryptogramDataType[] {
            CryptogramDataType.DE55,
            CryptogramDataType.UCAF
    };
    private View buttonChangeCard;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Loading UI
        setContentView(R.layout.activity_dsrp);
        editTextAmount = findViewById(R.id.editTextAmount);
        spinnerTransactionType = findViewById(R.id.spinnerTransactionType);
        spinnerCryptogramType = findViewById(R.id.spinnerCryptogramType);
        buttonSubmit = findViewById(R.id.buttonSubmit);
        buttonChangeCard = findViewById(R.id.buttonChangeCard);
        tokenId = getIntent().getStringExtra(EXTRA_TOKEN_ID);
        digitalCardId = getIntent().getStringExtra(EXTRA_DIGITALIZED_CARD_ID);

        buttonChangeCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DsrpActivity.this, CardChooserActivity.class);
                startActivity(intent);

            }
        });


        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), PaymentDSRPActivity.class);

                intent.putExtra("amount", Long.valueOf(editTextAmount.getText().toString()));
                intent.putExtra("transactionType", transactionTypeIds[spinnerTransactionType.getSelectedItemPosition()]);
                intent.putExtra("cryptogramType", cryptogramTypeObjects[spinnerCryptogramType.getSelectedItemPosition()]);
                intent.putExtra("tokenId", tokenId);
                startActivity(intent);
                finish();
            }
        });

        populateSpinners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleCardDisplay(findViewById(R.id.card));
    }

    private void populateSpinners() {
        ArrayAdapter<String> adapterTransactionType = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, transactionTypes);
        adapterTransactionType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTransactionType.setAdapter(adapterTransactionType);

        ArrayAdapter<String> adapterCryptogramType = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cryptogramTypes);
        adapterCryptogramType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCryptogramType.setAdapter(adapterCryptogramType);
    }
    private void handleCardDisplay(final View view) {
        if(!SDKController.getInstance().getSDKServiceState().equals(SDKServiceState.STATE_INITIALIZED)){
            return;
        }


            final String text=

                    "<b>Token ID: </b> <font color='blue'>" + tokenId + "</font> <br>"
                            + "<b>Digital Card ID: </b><font color='blue'>" + digitalCardId + "</font><br>";

            final TextView defaultCardInfo = view.findViewById(R.id.tvCardInfo);
            defaultCardInfo.setText(Html.fromHtml(text));


    }
}
