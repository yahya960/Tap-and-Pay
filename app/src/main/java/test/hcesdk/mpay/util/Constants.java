package test.hcesdk.mpay.util;

public class Constants {

    public static final String PRELOADED_CARD_PAN_VISA = "4622943127006808";
    public static final String PRELOADED_CARD_PAN_MASTERCARD = "5186161550060003";


    public static final String PRELOADED_CARD_PAN = PRELOADED_CARD_PAN_VISA;
    public static final String PRELOADED_CARD_EXPIRY = "1225";
    public static final String PRELOADED_CARD_CVV = "123";


    public static final String OTP_YELLOW_FLOW="123456";

    public static int MG_CONNECTION_TIMEOUT = 30000;
    public static int MG_CONNECTION_READ_TIMEOUT = 30000;
    public static int MG_CONNECTION_RETRY_COUNT = 3;
    public static int MG_CONNECTION_RETRY_INTERVAL = 10000;


    public static final String EXTRA_OPERATION = "com.gemalto.mfs.action.dcm.operation";
    public static final String EXTRA_OPERATION_WIPE_ALL = "com.gemalto.mfs.action.dcm.wipeAll";

    public static final String USERID = "useridtest";
    public static final String TAG_USER_ID = "userId";
    public static final String CPS_SENDER_ID = "188445501380";
    public static final boolean USE_SECURE_KEYPAD = false;
    public static String ASSETS_MAC_TABLE = "masked_PS_tables.bin";
    public static String ASSETS_ZCL_TABLE = "aes128_decrypt.bin";

    public static final String WALLET_PIN = "87654321";
}
