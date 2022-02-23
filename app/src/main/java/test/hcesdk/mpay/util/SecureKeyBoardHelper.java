package test.hcesdk.mpay.util;

import android.graphics.Color;

import androidx.fragment.app.FragmentManager;

import com.gemalto.mfs.mwsdk.utils.chcodeverifier.CHCodeVerifier;
import com.gemalto.mfs.mwsdk.utils.securekeypad.SecureKeypadBuilder;
import com.gemalto.mfs.mwsdk.utils.securekeypad.SecureKeypadEventsListener;

public class SecureKeyBoardHelper {


    public static void inputCodeWithSecureKeypad(CHCodeVerifier chCodeVerifier, String textLabel, FragmentManager manager) {
        // use securekeypad
        final int btnSelectedColor = Color.parseColor("#FF000000");
        SecureKeypadBuilder secureKeypadBuilder = chCodeVerifier.getSecureKeypadBuilder();
        secureKeypadBuilder.setMinimumInputLength(1);
        secureKeypadBuilder.setTextLabel(textLabel);

        secureKeypadBuilder.setNumberOfRows(3);
        secureKeypadBuilder.setNumberOfColumns(4);
        secureKeypadBuilder.setVisibleButtonPress(true);
        secureKeypadBuilder.setButtonGradientSelectedColors(btnSelectedColor, btnSelectedColor);
        secureKeypadBuilder.setMaximumInputLength(16);

        //disable scrambling
        secureKeypadBuilder.build(false, false, true, new SecureKeypadEventsListener() {
            @Override
            public void keyPressedCountChanged(int i, int i1) {

            }

            @Override
            public void textFieldSelected(int i) {

            }
        }).show(manager, "");
    }
}
