package test.hcesdk.mpay;import android.content.BroadcastReceiver;import android.content.Context;import android.content.Intent;import android.util.Log;public class MPAPreEntryReceiver extends BroadcastReceiver {    private static String TAG = MPAPreEntryReceiver.class.getName();    @Override    public void onReceive(Context context, Intent intent) {        long ts = System.currentTimeMillis();        App.lastAuthTS = ts;        Log.d(TAG, "Register for PreEntry is done at " + ts);    }}