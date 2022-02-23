package test.hcesdk.mpay.history;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import test.hcesdk.mpay.R;
import test.hcesdk.mpay.model.TransactionHistory;

public class HistoryListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private List<TransactionHistory> transactionHistory;

    public HistoryListAdapter(Context context, List<TransactionHistory> transactionHistory) {
        this.context = context;
        this.transactionHistory = transactionHistory;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int position) {
        LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return new MyViewHolder(inflater.inflate(R.layout.list_item_history, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        MyViewHolder holder = (MyViewHolder) viewHolder;
        final TransactionHistory history = transactionHistory.get(position);
        holder.transactionID.setText("Transaction ID : " + history.getTransactionId());
        holder.transactionAmount.setText("Transaction Amount : " + history.getTransactionAmount());
        holder.merchantName.setText("Merchant Name : " + history.getMerchantName());
        holder.transactionStatus.setText("Transaction Status : " + history.getTransactionStatus());
    }

    @Override
    public int getItemCount() {
        return transactionHistory.size();
    }

    private class MyViewHolder extends RecyclerView.ViewHolder {

        TextView transactionID, transactionAmount, merchantName, transactionStatus;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            transactionID = itemView.findViewById(R.id.transaction_id);
            transactionAmount = itemView.findViewById(R.id.transaction_amount);
            merchantName = itemView.findViewById(R.id.transaction_merchantName);
            transactionStatus = itemView.findViewById(R.id.transaction_status);
        }
    }
}
