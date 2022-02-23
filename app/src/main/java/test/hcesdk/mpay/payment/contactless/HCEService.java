/*
 * ------------------------------------------------------------
 * Copyright (C)  2017  -  GEMALTO DEVELOPMENT - R&D
 * ------------------------------------------------------------
 *
 *   GEMALTO MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF * THE SOFTWARE,
 *   EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED * TO THE IMPLIED WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. GEMALTO SHALL NOT
 *   BE * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, * MODIFYING OR
 *   DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. * * THIS SOFTWARE IS NOT DESIGNED OR INTENDED
 *   FOR USE OR RESALE AS ON-LINE * CONTROL EQUIPMENT IN HAZARDOUS ENVIRONMENTS REQUIRING
 *   FAIL-SAFE * PERFORMANCE, SUCH AS IN THE OPERATION OF NUCLEAR FACILITIES, AIRCRAFT *
 *   NAVIGATION OR COMMUNICATION SYSTEMS, AIR TRAFFIC CONTROL, DIRECT LIFE * SUPPORT MACHINES, OR
 *   WEAPONS SYSTEMS, IN WHICH THE FAILURE OF THE * SOFTWARE COULD LEAD DIRECTLY TO DEATH,
 *   PERSONAL INJURY, OR SEVERE * PHYSICAL OR ENVIRONMENTAL DAMAGE ("HIGH RISK ACTIVITIES").
 *   GEMALTO * SPECIFICALLY DISCLAIMS ANY EXPRESS OR IMPLIED WARRANTY OF FITNESS FOR * HIGH RISK
 *   ACTIVITIES. * *
 */
package test.hcesdk.mpay.payment.contactless;

import android.os.Bundle;
import android.util.Log;

import com.gemalto.mfs.mwsdk.payment.AbstractHCEService;
import com.gemalto.mfs.mwsdk.payment.PaymentServiceListener;

import test.hcesdk.mpay.App;
import test.hcesdk.mpay.app.AppBuildConfigurations;
import test.hcesdk.mpay.app.AppConstants;
import test.hcesdk.mpay.util.AppLogger;
import test.hcesdk.mpay.util.Util;

/**
 * HCE service
 */
public class HCEService extends AbstractHCEService {

    private static final String TAG = HCEService.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public PaymentServiceListener setupListener() {
        return ((App) getApplication()).getContactlessPayListener();
    }

    @Override
    public boolean setupCardActivation() {
        //If POS/Plugin (e.g Fidelity) request to change default card, we need to do it here
        //Otherwise, just return false.
        return false;
    }

    @Override
    public void setupPluginRegistration() {
        //If there is plugin to be registered
    }

    @Override
    public byte[] processCommandApdu(byte[] bytes, Bundle bundle) {

        AppLogger.i(TAG, "APDU Received from POS::" + Util.bytesToHex(bytes));
        Long startTime=System.currentTimeMillis();
        byte[] responseAPDU = super.processCommandApdu(bytes, bundle);
        if (null != responseAPDU) {
            AppLogger.i(TAG, "APDU sent to POS::" + Util.bytesToHex(responseAPDU));
        } else {
            AppLogger.i(TAG, "APDU sent to POS:: null");
        }
        String operation=Util.bytesToHex(bytes);
        Util.getTimeTakenForOperation(operation,startTime);
        Util.getTimeTakenForOperation("TotalPaymentTime at end of "+operation,
                ((App)getApplicationContext()).getPaymentStartTime());
        return responseAPDU;
    }
}
