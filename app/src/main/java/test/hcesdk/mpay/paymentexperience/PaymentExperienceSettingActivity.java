package test.hcesdk.mpay.paymentexperience;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;


import com.gemalto.mfs.mwsdk.payment.experience.PaymentExperience;
import com.gemalto.mfs.mwsdk.payment.experience.PaymentExperienceSettings;

import test.hcesdk.mpay.R;

//to create pull request
public class PaymentExperienceSettingActivity extends AppCompatActivity implements
        AdapterView.OnItemSelectedListener {

    Button btnSetPaymentExperience, btnGetPaymentExperience, btnCheckPaymentExperience, btnCheckPaymentExperienceWithTolerance;
    TextView txtPaymentExperience, txtPaymentExperiencewithTolerance, txtGetPaymentExperience;
    EditText edtPaymentExpwithTolerance;
    CheckBox chkTolerance;
    CardView cardViewWithTolerance, cardViewNormal;
    Spinner spinner_payment_exp;
    Spinner check_spinner_payment_exp;
    ProgressBar progressBar, progressBarwithTolerance;
    String[] payment_behaviour = {"ONE_TAP_REQUIRES_SDK_INTIALIZED", "ONE_TAP_ENABLED", "TWO_TAP_ENABLED"};
    ArrayAdapter adapter;
    PaymentExperience userSelectedPaymentExp = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_experience_setting);

        declareUI();
        setArrayAdapter();
        onEvents();
    }

    private void declareUI() {
        btnCheckPaymentExperience = findViewById(R.id.btncheck_payment_exp);
        btnGetPaymentExperience = findViewById(R.id.btnget_payment_exp);
        btnSetPaymentExperience = findViewById(R.id.btnset_payment_exp);
        btnCheckPaymentExperienceWithTolerance = findViewById(R.id.btncheck_payment_exp_withTolerance);
        spinner_payment_exp = findViewById(R.id.spinner_payment_exp);
        check_spinner_payment_exp = findViewById(R.id.check_spinner_payment_exp);
        txtPaymentExperience = findViewById(R.id.txtPaymentExperience);
        txtPaymentExperiencewithTolerance = findViewById(R.id.txtPaymentExperiencewithTolerance);
        txtGetPaymentExperience = findViewById(R.id.txtGetPaymentExperience);
        edtPaymentExpwithTolerance = findViewById(R.id.edtPaymentExperiencewithTolerance);
        progressBar = findViewById(R.id.progloading);
        progressBarwithTolerance = findViewById(R.id.progloading_withTolerance);
        chkTolerance = findViewById(R.id.checkTolerance);
        cardViewWithTolerance = findViewById(R.id.cardWithTolerance);
        cardViewNormal = findViewById(R.id.cardViewNormal);
        progressBar.setVisibility(View.GONE);
        progressBarwithTolerance.setVisibility(View.GONE);
        cardViewWithTolerance.setVisibility(View.GONE);
        spinner_payment_exp.setOnItemSelectedListener(this);
        check_spinner_payment_exp.setOnItemSelectedListener(this);
    }

    private void setArrayAdapter() {
        adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, payment_behaviour);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Setting the ArrayAdapter data on the Spinner
        spinner_payment_exp.setAdapter(adapter);
        check_spinner_payment_exp.setAdapter(adapter);
    }

    private void onEvents() {
        btnCheckPaymentExperience.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                txtPaymentExperience.setText("");
                PaymentExperienceAysncTask asyncTask = new PaymentExperienceAysncTask(0);
                asyncTask.execute();
            }
        });

        chkTolerance.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    cardViewWithTolerance.setVisibility(View.VISIBLE);
                    cardViewNormal.setVisibility(View.GONE);
                } else {
                    cardViewWithTolerance.setVisibility(View.GONE);
                    cardViewNormal.setVisibility(View.VISIBLE);
                }
            }
        });

        btnCheckPaymentExperienceWithTolerance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtPaymentExperiencewithTolerance.setText("");

                if (edtPaymentExpwithTolerance.getText().toString().equalsIgnoreCase("")) {
                    Toast.makeText(getApplicationContext(), "Please enter the tolerance number between 1 to 10", Toast.LENGTH_LONG).show();
                } else {
                    int toleranceNum = 0;
                    toleranceNum = Integer.parseInt(edtPaymentExpwithTolerance.getText().toString());

                    PaymentExperienceAysncTask asyncTask = new PaymentExperienceAysncTask(toleranceNum);
                    asyncTask.execute();
                }
            }
        });

        btnSetPaymentExperience.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (userSelectedPaymentExp != null) {
                    PaymentExperienceSettings.setPaymentExperience(PaymentExperienceSettingActivity.this, userSelectedPaymentExp);
                    Toast.makeText(getApplicationContext(), "Set Payment Experience is done !", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Please select payment experience !", Toast.LENGTH_LONG).show();
                }

            }
        });

        btnGetPaymentExperience.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GetPaymentExperienceAysncTask getPaymentExperience = new GetPaymentExperienceAysncTask();
                getPaymentExperience.execute();
            }
        });

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        if (position == 0) {
            userSelectedPaymentExp = PaymentExperience.ONE_TAP_REQUIRES_SDK_INITIALIZED;
        } else if (position == 1) {
            userSelectedPaymentExp = PaymentExperience.ONE_TAP_ENABLED;
        } else {
            userSelectedPaymentExp = PaymentExperience.TWO_TAP_ALWAYS;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public class PaymentExperienceAysncTask extends AsyncTask<Integer, Integer, Boolean> {
        int tolerance;

        public PaymentExperienceAysncTask(int tolerance) {
            this.tolerance = tolerance;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (tolerance <= 0) {
                progressBar.setVisibility(View.VISIBLE);
                progressBarwithTolerance.setVisibility(View.GONE);
            } else {
                progressBarwithTolerance.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        }

        @Override
        protected Boolean doInBackground(Integer... integers) {
            boolean paymentExperience;
            if (tolerance <= 0) {
                paymentExperience = PaymentExperienceSettings.checkPaymentExperienceSupport(getApplicationContext(), userSelectedPaymentExp);
            } else {
                paymentExperience = PaymentExperienceSettings.checkPaymentExperienceSupport(getApplicationContext(), userSelectedPaymentExp, tolerance);
            }

            return paymentExperience;
        }


        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Boolean s) {
            super.onPostExecute(s);
            if (tolerance <= 0) {
                progressBar.setVisibility(View.GONE);
                progressBarwithTolerance.setVisibility(View.GONE);
                if (s) {
                    txtPaymentExperience.setText("Your device is capable of " + userSelectedPaymentExp.name() + " payment.");
                } else {
                    txtPaymentExperience.setText("Your device is not capable of " + userSelectedPaymentExp.name() + " payment.");
                }

            } else {
                progressBarwithTolerance.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                if (s) {
                    txtPaymentExperiencewithTolerance.setText("Your device is capable of " + userSelectedPaymentExp.name() + " payment.");
                } else {
                    txtPaymentExperiencewithTolerance.setText("Your device is not capable of " + userSelectedPaymentExp.name() + " payment.");
                }
            }

        }
    }

    public class GetPaymentExperienceAysncTask extends AsyncTask<Void, Void, PaymentExperience> {

        @Override
        protected PaymentExperience doInBackground(Void... voids) {
            return PaymentExperienceSettings.getPaymentExperience(getApplicationContext());
        }

        @Override
        protected void onPostExecute(PaymentExperience paymentExperience) {
            super.onPostExecute(paymentExperience);
            txtGetPaymentExperience.setText("Your device is executing " + paymentExperience.name() + " payment.");
        }
    }

}
