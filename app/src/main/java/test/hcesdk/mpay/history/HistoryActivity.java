package test.hcesdk.mpay.history;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.gemalto.mfs.mwsdk.mobilegateway.MGTransactionRecord;
import com.gemalto.mfs.mwsdk.mobilegateway.MobileGatewayError;
import com.gemalto.mfs.mwsdk.mobilegateway.MobileGatewayManager;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.MGErrorCode;
import com.gemalto.mfs.mwsdk.mobilegateway.listener.TransactionHistoryListener;
import com.gemalto.mfs.mwsdk.provisioning.ProvisioningServiceManager;
import com.gemalto.mfs.mwsdk.provisioning.listener.AccessTokenListener;
import com.gemalto.mfs.mwsdk.provisioning.model.GetAccessTokenMode;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServiceError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import test.hcesdk.mpay.App;
import test.hcesdk.mpay.R;
import test.hcesdk.mpay.model.TransactionHistory;
import test.hcesdk.mpay.util.AppExecutors;
import test.hcesdk.mpay.util.AppLogger;

public class HistoryActivity extends AppCompatActivity {

    public static final String EXTRA_DIGITAL_CARD_ID = "HistoryActivity.EXTRA_DIGITAL_CARD_ID";
    public static final String TAG = "HistoryActivity";
    private static final int MAX_RETRY_COUNT = 3;
    private static final int RETRY_DELAY = 3000;
    private static final List<Integer> SERVER_ERROR_CODES_TO_RETRY = Arrays.asList(10001, 10002);
    View progressLayout;
    private boolean mTokenRefreshRequested;
    private int retryCount;
    private String digitalCardId;
    private RecyclerView recyclerView;
    private AppExecutors appExecutors;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        AppLogger.i(TAG, "[HistoryActivity onCreate]");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_history);
        progressLayout = findViewById(R.id.progress_layout);
        appExecutors = ((App) getApplication()).getAppExecutors();
        recyclerView = findViewById(R.id.recyclerView);
        digitalCardId = getIntent().getStringExtra(EXTRA_DIGITAL_CARD_ID);
        fetchHistory(digitalCardId, GetAccessTokenMode.NO_REFRESH);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setHasFixedSize(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        appExecutors.mainThread().cancelAllPendingOps();
    }


    private void fetchHistory(String digitalCardId, GetAccessTokenMode accessMode) {
        AppLogger.i(TAG, "[HistoryActivity fetchHistory]");
        ProvisioningServiceManager.getProvisioningBusinessService().getAccessToken(digitalCardId, accessMode,
                new AccessTokenListener() {
                    @Override
                    public void onSuccess(String digitalCardId, String accessToken) {
                        AppLogger.i(TAG, "[HistoryActivity fetchHistory onSuccess]");
                        getTransactionHistory(digitalCardId, accessToken);
                    }

                    @Override
                    public void onError(String digitalCardId, ProvisioningServiceError provisioningServiceError) {
                        AppLogger.i(TAG, "[HistoryActivity fetchHistory onError]");
                        Toast.makeText(getApplicationContext(), "Get access token error:  " + provisioningServiceError.getErrorMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void getTransactionHistory(String digitalCardId, String accessToken) {
        AppLogger.i(TAG, "[HistoryActivity getTransactionHistory]");
        //java.lang.String accessToken, java.lang.String digitalCardId, java.lang.String timeStamp
        MobileGatewayManager.INSTANCE.getTransactionHistoryService().refreshHistory(accessToken, digitalCardId, null,
                new TransactionHistoryListener() {
                    @Override
                    public void onSuccess(final List<MGTransactionRecord> transactionRecord, String digitalCardId, String timeStamp) {
                        AppLogger.i(TAG, "[HistoryActivity getTransactionHistory onSuccess]");
                        appExecutors.mainThread().execute(new Runnable() {
                            @Override
                            public void run() {
                                AppLogger.i(TAG, "[HistoryActivity getTransactionHistory onSuccess run]");
                                final List<TransactionHistory> history = new ArrayList<>();
                                Toast.makeText(getApplicationContext(), "Received transaction history : " + transactionRecord.size(),
                                        Toast.LENGTH_SHORT).show();
                                for (final MGTransactionRecord allRecords : transactionRecord) {
                                    final TransactionHistory mRecords = new TransactionHistory(allRecords);
                                    mRecords.setTransactionId(allRecords.getTransactionId());
                                    mRecords.setTransactionAmount(allRecords.getDisplayAmount());
                                    mRecords.setMerchantName(allRecords.getMerchantName());
                                    mRecords.setTransactionStatus(allRecords.getTransactionStatus());

                                    history.add(mRecords);
                                }
                                recyclerView.setAdapter(new HistoryListAdapter(getApplicationContext(), history));
                            }
                        });
                    }

                    @Override
                    public void onError(String digitalCardId, MobileGatewayError mobileGatewayError) {
                        handleTransactionHistoryError(mobileGatewayError);
                    }
                });
    }

    private void handleTransactionHistoryError(@NonNull final MobileGatewayError error) {
        if (error.getSDKErrorCode() == MGErrorCode.SERVER_ERROR) {
            int serverErrorCode = error.getServerErrorCode();
            if (SERVER_ERROR_CODES_TO_RETRY.contains(serverErrorCode)
                    && retryCount < MAX_RETRY_COUNT) {
                retryCount++;
                fetchHistory(digitalCardId, GetAccessTokenMode.NO_REFRESH);
            } else {
                appExecutors.mainThread().execute(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Failed to get transaction History :" + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

        } else {
            switch (error.getHTTPStatusCode()) { //SDKErrorCode is not yet implemented. so, use 401
                // to retry
                //case TOKEN_NOT_VALID:
                case 401:
                    if (!mTokenRefreshRequested) {
                        mTokenRefreshRequested = true;
                        fetchHistory(digitalCardId, GetAccessTokenMode.REFRESH);
                        break;
                    }
                default:
                    appExecutors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Failed to get transaction History :" + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

            }
        }
    }
}
