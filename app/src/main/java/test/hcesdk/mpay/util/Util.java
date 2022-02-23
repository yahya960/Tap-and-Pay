package test.hcesdk.mpay.util;

import android.content.Context;
import android.util.Log;

import com.gemalto.mfs.mwsdk.utils.chcodeverifier.CHCodeVerifier;
import com.gemalto.mfs.mwsdk.utils.securekeypad.SecureKeypadBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import test.hcesdk.mpay.R;

public class Util {

    private static final String TAG = "mpay.util.Util";

    public static SecureKeypadBuilder decorateKeyPadWithDefaultSettings(CHCodeVerifier chCodeVerifier, Context context) {
        int keyPadBg = context.getResources().getColor(R.color.secure_pin_pad_color_1);
        int keyTextColor = context.getResources().getColor(R.color.secure_pin_pad_color_2);
        int keyFocusTextColor = context.getResources().getColor(R.color.secure_pin_pad_color_3);
        int keyPadSelectedBg = context.getResources().getColor(R.color.secure_pin_pad_color_4);
        int keyPadDisabledBg = context.getResources().getColor(R.color.secure_pin_pad_color_5);
        int buttonSelectedBg = context.getResources().getColor(R.color.secure_pin_pad_color_6);
        try {
            SecureKeypadBuilder mKeypadModel = chCodeVerifier.getSecureKeypadBuilder();

            //Configure Key Pad
            mKeypadModel.setIsLayoutScrambled(true);
            mKeypadModel.setMaximumInputLength(16);
            mKeypadModel.setMinimumInputLength(4);
            mKeypadModel.setDistanceBetweenButtonAndAdditionalCharacter(4);
            mKeypadModel.setScreenBackGroundColor(keyPadBg);
            mKeypadModel.setKeypadGridGradientColors(keyPadBg, keyPadBg);
            mKeypadModel.setButtonGradientSelectedColors(buttonSelectedBg, buttonSelectedBg);
            mKeypadModel.setOkButtonGradientSelectedColors(buttonSelectedBg, buttonSelectedBg);
            mKeypadModel.setDeleteButtonGradientSelectedColors(buttonSelectedBg, buttonSelectedBg);
            mKeypadModel.setDeleteButtonGradientDisabledColors(buttonSelectedBg, buttonSelectedBg);
            mKeypadModel.setOkButtonGradientDisabledColors(buttonSelectedBg, buttonSelectedBg);
            mKeypadModel.setKeypadFrameColor(keyPadBg);
            mKeypadModel.setVisibleButtonPress(true);

            //Configure Look and Feel
            mKeypadModel.setButtonSpacing(8);
            mKeypadModel.setNumberOfColumns(3);
            mKeypadModel.setNumberOfRows(4);

            //Set PIN Border Color
            mKeypadModel.setTextBorderUnfocusColor(keyPadDisabledBg);
            mKeypadModel.setTextBackgroundUnfocusedColor(keyPadDisabledBg);
            mKeypadModel.setTextBorderFocusColor(keyPadSelectedBg);
            mKeypadModel.setTextBackgroundFocusedColor(keyPadSelectedBg);

            //Set Text Boarder
            mKeypadModel.setTextLabelFontSize(20);
            mKeypadModel.setTextLabelColor(keyTextColor);

            //Button Character Color
            mKeypadModel.setButtonGradientNormalColors(keyPadBg, keyPadBg);
            mKeypadModel.setButtonCharacterColorSelected(keyPadSelectedBg);

            mKeypadModel.setOkButtonTextSelectedColor(keyPadSelectedBg);
            mKeypadModel.setOkButtonTextDisabledColor(keyFocusTextColor);
            mKeypadModel.setDeleteButtonTextDisabledColor(keyFocusTextColor);
            mKeypadModel.setDeleteButtonTextSelectedColor(keyPadSelectedBg);

            return mKeypadModel;
        } catch (Exception e) {
            AppLogger.e(TAG, "Error - Setup GSK Exception", e);
        }
        return null;
    }

    /**
     * Converts byte array to a hexadecimal string.
     *
     * @param dataToPrint
     *            the byte array to be converted
     * @return The hexadecimal string representing the byte array.
     */
    public static String byteArrayToHexaStr(final byte[] dataToPrint) {
        if (dataToPrint == null) {
            return "";
        }

        final int dataLen = dataToPrint.length;
        final StringBuffer buff = new StringBuffer(dataLen * 2);

        for (int i = 0; i < dataLen; i++) {
            buff.append(convertDigit(dataToPrint[i] >> 4));
            buff.append(convertDigit(dataToPrint[i] & 15));
        }

        return buff.toString().toUpperCase();
    }
    /**
     * Converts an integer to ascii character
     *
     * @param value
     *            the int to convert
     * @return char converted string
     */
    private static char convertDigit(final int value) {
        int convertedValue = value;
        convertedValue &= 15;

        if (convertedValue >= 10) {
            return (char) (convertedValue - 10 + 'a');
        } else {
            return (char) (convertedValue + '0');
        }
    }

    public static byte[] hexStringToByteArray(String hexString) {
        byte[] result = null;
        if ((hexString == null) || (hexString.length() == 0)) {
            return null;
        }
        result = new byte[hexString.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int j = i * 2;
            char msb = hexString.charAt(j);
            char lsb = hexString.charAt(j + 1);
            result[i] = (byte) (char) (((hexCharToInt(msb) << 4) & 0x00F0) | (hexCharToInt(lsb) & 0x000F));
        }
        return result;
    }

    public static String bytesToHex(byte[] bytes){
        final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
            char[] hexChars = new char[bytes.length * 2];
            for (int j = 0; j < bytes.length; j++) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 2] = HEX_ARRAY[v >>> 4];
                hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
            }
            return new String(hexChars);
    }

    /**
     * Method to convert a hex digit to an integer format
     *
     * @param c
     * @return
     */
    public static int hexCharToInt(char c) {
        int i = 0;
        if((c >= '0') && (c <= '9'))
            i = c - '0';
        else if((c >= 'A') && (c <= 'F'))
            i = c - 'A' + 10;
        else if((c >= 'a') && (c <= 'f'))
            i = c - 'a' + 10;
        return i;
    }

    //copy tables from assets folder to the files
    public static void copyTableFromAssets(Context androidContext, File outputFile, String assetsName) {
        java.io.FileOutputStream fosZCL = null;
        InputStream assetStreamZCL = null;
        try {
            fosZCL = new java.io.FileOutputStream(outputFile);

            // Write ZCL
            byte[] buffZCL = new byte[1024];
            int read;
            assetStreamZCL = androidContext.getAssets().open(assetsName);

            while ((read = assetStreamZCL.read(buffZCL)) > 0) {
                fosZCL.write(buffZCL, 0, read);
            }
            Log.i(TAG,"Table creation from Assets successful ");
        }catch (IOException e){
            e.printStackTrace();
            Log.i(TAG,"Table creation from Assets Exception occurred "+e.getMessage());

        } finally {
            try {
                if (fosZCL != null) {
                    fosZCL.close();
                }
                if (assetStreamZCL != null) {
                    assetStreamZCL.close();
                }
            }catch (Exception e){
                Log.i(TAG,"Table close creation from Assets exception occurred "+e.getMessage());
            }
        }
    }


    /**
     * this method shall be responsible for printing time taken for each operation
     */
    public static long getTimeTakenForOperation(final String operation,final long startTime){
        Log.d(TAG,"operation is"+operation);
        long endTime= System.currentTimeMillis();
        long timeTaken=endTime-startTime;
        Log.wtf(TAG,"TimeTaken for operation:"+operation+" is:"+timeTaken);
        return timeTaken;

    }


}
