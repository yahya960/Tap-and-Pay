package test.hcesdk.mpay.util;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;

import test.hcesdk.mpay.R;
import test.hcesdk.mpay.model.SettingsItem;

public class SettingsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = SettingsListAdapter.class.getName();
    //private AuthenticationPriorToPaymentManager authenticationPriorToPaymentManager;
    private Context context;
    private List<SettingsItem> settingsItems;
    private SettingsListAdapter.SettingsListAdapterCallback callback;
    public SettingsListAdapter(Context context, SettingsListAdapter.SettingsListAdapterCallback callback, List<SettingsItem> settingsItems) {
        this.settingsItems = settingsItems;
        this.context = context;
        this.callback = callback;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return new SettingsListAdapter.MyViewHolder(inflater.inflate(R.layout.list_item_settings, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SettingsListAdapter.MyViewHolder mHolder = (SettingsListAdapter.MyViewHolder) holder;

        final SettingsItem item = settingsItems.get(position);

        switch (item.getType()){
            case INFO:
                mHolder.label.setText(item.getLabel());
                mHolder.value.setText(item.getValue());
                mHolder.spinner1.setVisibility(View.GONE);
                break;
            case DROPDOWN:
                //mHolder.label.setVisibility(View.GONE);
                mHolder.value.setVisibility(View.GONE);
                mHolder.label.setText(item.getLabel());
                mHolder.value.setText(item.getValue());

                mHolder.spinner1.setPrompt(item.getLabel());

                ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(context,
                        android.R.layout.simple_spinner_item, item.getPossibleValues());
                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mHolder.spinner1.setAdapter(dataAdapter);
                mHolder.spinner1.setSelection(item.getPossibleValues().indexOf(item.getValue()));
                mHolder.spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        AppLogger.d(TAG,"onItemSelected");
                        AppLogger.d(TAG,"item.getLabel()"+item.getLabel());

                        if(item.getLabel().equalsIgnoreCase("Benchmark Scheme")){
                            callback.onItemSelectedForBenchmarkingScheme(item.getPossibleValues(),position);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                break;
        }



    }



    @Override
    public int getItemCount() {
        return settingsItems.size();
    }

    public interface SettingsListAdapterCallback {

        void onItemSelectedForBenchmarkingScheme(List<String> list, int position);
    }

    private class MyViewHolder extends RecyclerView.ViewHolder {
        TextView label;
        TextView value;
        Spinner spinner1;

        public MyViewHolder(View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.label);
            value = itemView.findViewById(R.id.value);
            spinner1 = itemView.findViewById(R.id.spinner1);

        }
    }


}
