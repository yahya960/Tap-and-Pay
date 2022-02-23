package test.hcesdk.mpay.util;

import android.content.Context;
import android.util.Log;

import java.io.File;

public class TableDownloadHelper {

    private File filesDir;
    private File destnationZCL_Table;
    private File destinationMAC_Table;
    private static final String ZCL_NAME = "safswefd";
    private static final String MAC_NAME = "heptsdgd";

    public void downloadTablestoFileDest(Context context)
    {
        filesDir = context.getFilesDir();
        destnationZCL_Table = new File(filesDir,ZCL_NAME);
        destinationMAC_Table = new File(filesDir,MAC_NAME);
        try
        {
            Log.e("Enrollment","Download Table Starts ");
            Util.copyTableFromAssets(context,destnationZCL_Table,Constants.ASSETS_ZCL_TABLE);
            Util.copyTableFromAssets(context,destinationMAC_Table,Constants.ASSETS_MAC_TABLE);
            Log.e("Enrollment","Download Table Ends ");
        }catch (Exception e)
        {
            Log.e("Enrollment","Download Table Error "+e.getMessage());
            throw new RuntimeException("Error in creating tables");
        }
    }
}
