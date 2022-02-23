package test.hcesdk.mpay.model;

import com.gemalto.mfs.mwsdk.mobilegateway.MGTransactionRecord;
import com.gemalto.mfs.mwsdk.mobilegateway.utils.TransactionStatus;

public class TransactionHistory {
    private String transactionId;
    private String transactionAmount;
    private String merchantName;
    private TransactionStatus transactionStatus;

    public TransactionHistory(MGTransactionRecord allRecords) {
        this.transactionId = allRecords.getTransactionId();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(String transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public TransactionStatus getTransactionStatus() {
        return transactionStatus;
    }

    public void setTransactionStatus(TransactionStatus transactionStatus) {
        this.transactionStatus = transactionStatus;
    }


}
