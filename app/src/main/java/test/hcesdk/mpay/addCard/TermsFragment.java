package test.hcesdk.mpay.addCard;


import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.gemalto.mfs.mwsdk.mobilegateway.MGCardEnrollmentService;
import com.gemalto.mfs.mwsdk.mobilegateway.MobileGatewayManager;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.TermsAndConditions;
import com.gemalto.mfs.mwsdk.mobilegateway.listener.MGDigitizationListener;

import test.hcesdk.mpay.R;

public class TermsFragment extends Fragment{


    Button btnAccept;

    TermsAndConditions termsAndConditions;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terms, container, false);
        btnAccept = view.findViewById(R.id.btnAccept);
        TextView tvTerms = view.findViewById(R.id.tvTerms);
        if (termsAndConditions != null) {
            tvTerms.setText(termsAndConditions.getText());
        }

        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                proceedDigitize();
            }
        });
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        toggleProgress(false);
    }

    private void proceedDigitize() {
        if (termsAndConditions == null) {
            return;
        }
        toggleProgress(true);
        MGCardEnrollmentService enrollmentService  = MobileGatewayManager.INSTANCE.getCardEnrollmentService();
        enrollmentService.digitizeCard(termsAndConditions.accept(), null, ((AddCardActivity) getActivity()));
    }

    private void toggleProgress(boolean show) {
        if (getActivity() != null) {
            ((AddCardActivity) getActivity()).toggleProgress(show);
        }
    }

}
