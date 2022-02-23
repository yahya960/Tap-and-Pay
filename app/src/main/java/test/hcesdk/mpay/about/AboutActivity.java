package test.hcesdk.mpay.about;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import com.gemalto.mfs.mwsdk.SDKEnv;

import java.util.ArrayList;
import java.util.List;

import test.hcesdk.mpay.R;
import test.hcesdk.mpay.app.AppConstants;
import test.hcesdk.mpay.model.SettingsItem;
import test.hcesdk.mpay.model.SettingsItemType;
import test.hcesdk.mpay.util.AppLogger;
import test.hcesdk.mpay.util.SettingsListAdapter;
import test.hcesdk.mpay.util.SharedPreferenceUtils;

public class AboutActivity extends AppCompatActivity implements SettingsListAdapter.SettingsListAdapterCallback {
    public static final String TAG=AboutActivity.class.getSimpleName();

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar myToolbar = findViewById(R.id.topToolBar);
        setToolbarProperties(myToolbar);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        loadSettingsItems();

    }

    private void setToolbarProperties(Toolbar myToolbar) {

        myToolbar.setTitle(R.string.app_name);

    }

        private void loadSettingsItems() {
        List<SettingsItem> listSettings=populateSettingsItems();
        recyclerView.setAdapter(
                new SettingsListAdapter(
                        getApplicationContext(),
                AboutActivity.this,
                        listSettings
                )
        );
    }

    private List<SettingsItem> populateSettingsItems() {
        List<SettingsItem> settingsItems = new ArrayList<SettingsItem>();
        settingsItems.add(populateSettingsItem("CPS SDK Version",SDKEnv.SDK_VERSION));
        settingsItems.add(populateSettingsItem("CPS SDK Variant",SDKEnv.DEBUG ? "Debug":"Release"));
        settingsItems.add(populateSettingsItem("MG SDK Version", com.gemalto.mfs.mwsdk.mobilegateway.SDKEnv.SDK_VERSION));
        settingsItems.add(populateSettingsItem("MG SDK Variant", com.gemalto.mfs.mwsdk.mobilegateway.SDKEnv.DEBUG ? "Debug":"Release"));
        List<String> possibleValuesForSchemes= new ArrayList<>();
        possibleValuesForSchemes.add(AppConstants.Schemes.VISA.name());
        possibleValuesForSchemes.add(AppConstants.Schemes.MASTERCARD.name());
        possibleValuesForSchemes.add(AppConstants.Schemes.PURE.name());
        settingsItems.add(populateSettingsItem("Benchmark Scheme",
                SharedPreferenceUtils.getBenchmark(getApplicationContext())
                ));
        return settingsItems;
    }

    private SettingsItem populateSettingsItem(String label, String value) {
        SettingsItem item = new SettingsItem();
        item.setLabel(label);
        item.setValue(value);
        item.setType(SettingsItemType.INFO);
        return item;
    }

    private SettingsItem populateSettingsItem(String label, String value,List<String> possibleValues) {
        SettingsItem item = new SettingsItem();
        item.setLabel(label);
        item.setValue(value);
        item.setPossibleValues(possibleValues);
        item.setType(SettingsItemType.DROPDOWN);
        return item;
    }

    @Override
    public void onItemSelectedForBenchmarkingScheme(List<String> list, int position) {

        String schemeSelected=list.get(position);
        AppLogger.d(TAG,"Scheme selected::"+schemeSelected);
        String benchmark = SharedPreferenceUtils.getBenchmark(getApplicationContext());
        if (benchmark.equalsIgnoreCase(AppConstants.Schemes.VISA.name())) {
            AppConstants.BENCHMARKING_SCHEME_SELECTED = AppConstants.Schemes.VISA;
        } else if (benchmark.equalsIgnoreCase(AppConstants.Schemes.MASTERCARD.name())) {
            AppConstants.BENCHMARKING_SCHEME_SELECTED = AppConstants.Schemes.MASTERCARD;
        }else{
            AppConstants.BENCHMARKING_SCHEME_SELECTED = AppConstants.Schemes.PURE;
        }
        loadSettingsItems();

    }
}
