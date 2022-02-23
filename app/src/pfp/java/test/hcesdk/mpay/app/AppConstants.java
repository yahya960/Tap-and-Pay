package test.hcesdk.mpay.app;

import test.hcesdk.mpay.payment.contactless.HCEService;
import test.hcesdk.mpay.payment.contactless.pfp.PFPHCEService;

public abstract class AppConstants {

    public enum Schemes{
        VISA,MASTERCARD,PURE
    }
    //PFP related constants
    public static final String PFP_ENABLED_FLAG = "PFP.enabled";
    public static final String ALLOW_PAYMENT_WHEN_SCREEN_OFF_FLAG = "Payment.allowTransactionScreenOff";
    public static final String APP_TAG = AppBuildConfigurations.IS_PFP_ENABLED ? "PFP_APP_TAG" : "APP_TAG";

    public static Schemes BENCHMARKING_SCHEME_SELECTED = Schemes.MASTERCARD;

    public static final String CANONICAL_PAYMENT_SERVICENAME =
            AppBuildConfigurations.IS_PFP_ENABLED ?
                    PFPHCEService.class.getCanonicalName()
                    : HCEService.class.getCanonicalName();

    //benchmark related constants
    public static final String BENCHMARK_TAG = "INIT_CPS_SDK";
    public static final String STARTED = " started";
    public static final String ENDED = " ended";

    //Related to init broadcasts
    public static final String ACTION_INIT_DONE = "com.gemalto.sdkinitDone";
    public static final String INIT_FAILED_EXTRA = "InitFailedBoolean";
    public static final String INIT_UI_UPDATE_NEEDED = "UIUpdateNeeded";



    public static final String ACTION_RELOAD_CARDS = "com.gemalto.test.app.ACTION_RELOAD_CARDS";
}
