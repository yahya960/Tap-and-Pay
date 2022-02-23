package test.hcesdk.mpay.addCard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.PendingCardActivation;

import test.hcesdk.mpay.R;

public class EnterOtpFragment extends Fragment {

    private EditText etOTP;
    private Button btnOk;
    PendingCardActivation pendingCardActivation;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_enter_otp, container, false);
        etOTP = view.findViewById(R.id.et_otp);
        btnOk = view.findViewById(R.id.ok_button);

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (etOTP.getText().toString().length() > 0) {
                    toggleProgress(true);
                    pendingCardActivation.activate(etOTP.getText().toString().getBytes(), (AddCardActivity) getActivity());
                } else {
                    Toast.makeText(getContext(), "Please enter OTP first", Toast.LENGTH_LONG).show();
                }
            }
        });

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        toggleProgress(false);

    }

    private void toggleProgress(boolean show) {
        if (getActivity() != null) {
            ((AddCardActivity) getActivity()).toggleProgress(show);
        }
    }
}
