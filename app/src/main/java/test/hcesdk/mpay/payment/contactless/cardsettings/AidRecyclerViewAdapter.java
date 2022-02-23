package test.hcesdk.mpay.payment.contactless.cardsettings;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatToggleButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.gemalto.mfs.mwsdk.dcm.Aid;

import java.util.Collections;
import java.util.List;

import test.hcesdk.mpay.R;

public class AidRecyclerViewAdapter extends RecyclerView.Adapter<AidRecyclerViewAdapter.ViewHolder> {

    private List<Aid> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    AidRecyclerViewAdapter(Context context, List<Aid> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.list_item_aid, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Aid aidItem = mData.get(position);
        holder.aidTextView.setText(aidItem.getAid());
        if (aidItem.getLabel().isEmpty()) { // Label field is optional.
            holder.aidLabelTextView.setText("No Label");
        } else {
            holder.aidLabelTextView.setText(aidItem.getLabel());
        }
        holder.compatToggleButton.setChecked(!(aidItem.getLockStatus() == Aid.LockStatus.LOCKED));
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    public List<Aid> getData() {
        return mData;
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView aidLabelTextView;
        TextView aidTextView;
        ImageButton imageButtonOrderUp;
        ImageButton imageButtonOrderDown;
        AppCompatToggleButton compatToggleButton;

        ViewHolder(View itemView) {
            super(itemView);
            aidLabelTextView = itemView.findViewById(R.id.aidLabel);
            aidTextView = itemView.findViewById(R.id.aid);

            compatToggleButton = itemView.findViewById(R.id.lockStatus);
            compatToggleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (compatToggleButton.getText().equals("UNLOCKED")) {
                        mData.get(getAdapterPosition()).setLockStatus(Aid.LockStatus.UNLOCKED);
                    } else {
                        mData.get(getAdapterPosition()).setLockStatus(Aid.LockStatus.LOCKED);
                    }
                }
            });

            imageButtonOrderUp = itemView.findViewById(R.id.orderUp);
            imageButtonOrderUp.setOnClickListener(view -> moveUp());

            imageButtonOrderDown = itemView.findViewById(R.id.orderDown);
            imageButtonOrderDown.setOnClickListener(view -> moveDown());

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }

        private void moveUp() {
            if (mData.size() < 2) {
                return;
            }
            int currentPosition = getAdapterPosition();
            if (currentPosition <= 0) {
                return;
            }

            swapeItem(currentPosition, currentPosition - 1);
        }

        private void moveDown() {
            if (mData.size() < 2) {
                return;
            }
            int currentPosition = getAdapterPosition();
            if (currentPosition >= mData.size() - 1) {
                return;
            }

            swapeItem(currentPosition, currentPosition + 1);
        }

        private void swapeItem(int fromPosition,int toPosition){
            Collections.swap(mData, fromPosition, toPosition);
            notifyItemMoved(fromPosition, toPosition);
        }

    }

    // convenience method for getting data at click position
    Aid getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}