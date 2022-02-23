package test.hcesdk.mpay.paymentexperience;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.gemalto.mfs.mwsdk.payment.cdcvm.DeviceCVMPreEntryReceiver;
import com.gemalto.mfs.mwsdk.payment.experience.PaymentExperience;
import com.gemalto.mfs.mwsdk.payment.experience.PaymentExperienceSettings;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import test.hcesdk.mpay.App;
import test.hcesdk.mpay.R;
import test.hcesdk.mpay.app.AppBuildConfigurations;
import test.hcesdk.mpay.util.AppLogger;

public class PaymentExperienceSettingActivity extends AppCompatActivity {
    private static final String TAG = PaymentExperienceSettingActivity.class.getSimpleName();
    Button btnCheckPaymentExperience;
    TextView txtPaymentExperience,txtGetPaymentExperience;
    CardView cardViewNormal;
    Spinner spinnerPaymentExp;
    Spinner spinnerTolerance;
    ProgressBar progressBar;

    Integer[] toleranceArray = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    ArrayAdapter paymentExpAdapter, toleranceAdapter;
    PaymentExperience userSelectedPaymentExp = null;
    int toleranceSelected = 0;

    private static boolean bPaymentExpSupported;

    private static boolean isBenchmarkRunning = false;

    private void startBenchmark(final Context context, final PaymentExperience paymentExperience, final int tolerance) {
        if (isBenchmarkRunning) {
            return;
        }

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        final Future<Boolean> futureHandler = executor.submit(() -> {
            isBenchmarkRunning = true;

            if (tolerance <= 0) {
                bPaymentExpSupported = PaymentExperienceSettings.checkPaymentExperienceSupport(context, paymentExperience);
            } else {
                bPaymentExpSupported = PaymentExperienceSettings.checkPaymentExperienceSupport(context, paymentExperience, tolerance);
            }

            isBenchmarkRunning = false;

            return bPaymentExpSupported;
        });

        /*
         * Benchmark doesn't complete within 2 minutes, consider device performance is not good and
         * cancel the benchmark operation.
         */
        executor.submit(() -> {
            try {
                bPaymentExpSupported = futureHandler.get(120, TimeUnit.SECONDS);
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                bPaymentExpSupported = false;
                futureHandler.cancel(true);
                isBenchmarkRunning = false;
                Log.d("PaymentExperience", "futureHandler timeout!.");
            }

            PaymentExperienceSettingActivity.this.runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                StringBuilder text = new StringBuilder();
                if (bPaymentExpSupported) {
                    text.append(userSelectedPaymentExp).append(" is supported AND saved.");
                    txtPaymentExperience.setText(text);
                    PaymentExperienceSettings.setPaymentExperience(getApplicationContext(), userSelectedPaymentExp);
                    updateCurrentPaymentExpUI();
                    registerPreEntryReceiverForOneTap(userSelectedPaymentExp);
                } else {
                    text.append(userSelectedPaymentExp).append(" is NOT supported and NOT saved.");
                    txtPaymentExperience.setText(text);
                }

                btnCheckPaymentExperience.setEnabled(true);
            });

            Log.d("PaymentExperience", "bOneTapSupported: " + bPaymentExpSupported);
        });

        executor.shutdown();

        progressBar.setVisibility(View.VISIBLE);
    }

    private void registerPreEntryReceiverForOneTap(PaymentExperience paymentExperience){
        Log.i(TAG,"Pre Entry Receiver for One Tap Enabler "+paymentExperience.name());
        if(paymentExperience.name().equals(PaymentExperience.ONE_TAP_ENABLED.name())|| paymentExperience.name().equals(PaymentExperience.ONE_TAP_REQUIRES_SDK_INITIALIZED.name())){
            AppLogger.d(TAG, "registerPreFpEntry for one tap experience");

            if (App.mPreEntryReceiver != null) {
                unregisterReceiver(App.mPreEntryReceiver);
                App.mPreEntryReceiver = null;
            }

            IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
            App.mPreEntryReceiver = new DeviceCVMPreEntryReceiver();
            App.mPreEntryReceiver .init();
            registerReceiver(App.mPreEntryReceiver, filter);

            AppLogger.d(TAG, "registerPreFpEntry for one tap done");


        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_experience_setting);
        declareUI();
        setArrayAdapter();
    }

    private void declareUI() {
        btnCheckPaymentExperience = findViewById(R.id.btncheck_payment_exp);
        txtPaymentExperience = findViewById(R.id.txtPaymentExperience);
        txtGetPaymentExperience = findViewById(R.id.txtGetPaymentExperience);
        progressBar = findViewById(R.id.progloading);
        cardViewNormal = findViewById(R.id.cardViewNormal);
        spinnerPaymentExp = findViewById(R.id.check_spinner_payment_exp);
        spinnerTolerance = findViewById(R.id.toleranceSpinner);
        progressBar.setVisibility(View.GONE);

        btnCheckPaymentExperience.setOnClickListener(v -> {
            txtPaymentExperience.setText("");
            btnCheckPaymentExperience.setEnabled(false);
            startBenchmark(PaymentExperienceSettingActivity.this, userSelectedPaymentExp, toleranceSelected);
        });

        updateCurrentPaymentExpUI();
    }

    private void updateCurrentPaymentExpUI() {
        PaymentExperience paymentExperience = PaymentExperienceSettings.getPaymentExperience(getApplicationContext());
        txtGetPaymentExperience.setText("Current setting: " + paymentExperience.name());
    }

    private void setArrayAdapter() {

        if(AppBuildConfigurations.IS_ONETAP_ENABLED) { // For implementation choice of ONE_TAP_ENABLED
            String[] payment_behaviour = {
                    PaymentExperience.ONE_TAP_ENABLED.name(),
                    PaymentExperience.TWO_TAP_ALWAYS.name()
            };
            paymentExpAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, payment_behaviour);
        } else if (AppBuildConfigurations.IS_PFP_ENABLED) { // For implementation choice of PFP
            String[] payment_behaviour = {
                    PaymentExperience.ONE_TAP_ENABLED.name(),
                    PaymentExperience.TWO_TAP_ALWAYS.name()
            };
            paymentExpAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, payment_behaviour);
        } else { // For implementation choice of Foreground Service
            String[] payment_behaviour = {
                    PaymentExperience.ONE_TAP_ENABLED.name(),
                    PaymentExperience.ONE_TAP_REQUIRES_SDK_INITIALIZED.name()
            };
            paymentExpAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, payment_behaviour);
        }

        paymentExpAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPaymentExp.setAdapter(paymentExpAdapter);
        spinnerPaymentExp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String text = spinnerPaymentExp.getSelectedItem().toString();
                if (text.equals(PaymentExperience.ONE_TAP_ENABLED.name())) {
                    userSelectedPaymentExp = PaymentExperience.ONE_TAP_ENABLED;
                } else if (text.equals(PaymentExperience.TWO_TAP_ALWAYS.name())) {
                    userSelectedPaymentExp = PaymentExperience.TWO_TAP_ALWAYS;
                } else {
                    userSelectedPaymentExp = PaymentExperience.ONE_TAP_REQUIRES_SDK_INITIALIZED;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //userSelectedPaymentExp = PaymentExperience.ONE_TAP_ENABLED;
            }
        });

        toleranceAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, toleranceArray);
        toleranceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTolerance.setAdapter(toleranceAdapter);
        spinnerTolerance.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                toleranceSelected = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //toleranceSelected = 0;
            }
        });
    }
}
