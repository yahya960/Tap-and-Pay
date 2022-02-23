package test.hcesdk.mpay.payment.contactless.cardsettings;

import android.icu.text.UnicodeSetSpanner;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gemalto.mfs.mwsdk.dcm.Aid;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCard;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardManager;
import com.gemalto.mfs.mwsdk.exception.InternalComponentException;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import test.hcesdk.mpay.R;

public class CardSettingsActivity extends AppCompatActivity implements AidRecyclerViewAdapter.ItemClickListener{

    AidRecyclerViewAdapter adapter;
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_settings);

        String tokenId = getIntent().getExtras().getString("selected_token_id");

        DigitalizedCard digitalizedCard = DigitalizedCardManager.getDigitalizedCard(tokenId);
        try {
            List<Aid> aidList = digitalizedCard.getAllAids();

            RecyclerView recyclerView = findViewById(R.id.aid_list_recyclerview);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            adapter = new AidRecyclerViewAdapter(this, aidList);
            adapter.setClickListener(this);
            recyclerView.setAdapter(adapter);

            View parentLayout = findViewById(android.R.id.content);
            AppCompatButton btnUpdateAidList = findViewById(R.id.btnUpdateAidList);
            btnUpdateAidList.setOnClickListener(view -> {
                try {
                    List<Aid> aidListUpdated = adapter.getData();
                    digitalizedCard.updateAidList(aidListUpdated);
                    Toast.makeText(CardSettingsActivity.this, "AID list updated!", Toast.LENGTH_LONG).show();
                } catch (InternalComponentException e) {
                    Snackbar.make(parentLayout, e.getMessage(), Snackbar.LENGTH_INDEFINITE).show();
                }
            });
        } catch (InternalComponentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onItemClick(View view, int position) {

    }
}