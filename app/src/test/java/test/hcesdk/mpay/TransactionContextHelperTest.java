package test.hcesdk.mpay;

import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardStatus;
import com.gemalto.mfs.mwsdk.payment.engine.TransactionContext;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import test.hcesdk.mpay.util.TransactionContextHelper;

import static org.junit.Assert.assertEquals;

public class TransactionContextHelperTest {

    /**
     * Test to retrieve most European and some worldwide currencies
     */
    @Test
    public void testGetCurrency(){

        Currency c;

        final Map<byte[], String> testData = new HashMap<>();
        testData.put(new byte[]{0x00, 0x08}, "ALL");
        testData.put(new byte[]{0x00, 0x36}, "AUD");
        testData.put(new byte[]{0x00, 0x51}, "AMD");
        testData.put(new byte[]{0x01, 0x24}, "CAD");
        testData.put(new byte[]{0x01, 0x52}, "CLP");
        testData.put(new byte[]{0x01, (byte)0x91}, "HRK");
        testData.put(new byte[]{0x02, 0x03}, "CZK");
        testData.put(new byte[]{0x02, 0x08}, "DKK");
        testData.put(new byte[]{0x03, 0x44}, "HKD");
        testData.put(new byte[]{0x03, 0x48}, "HUF");
        testData.put(new byte[]{0x03, 0x52}, "ISK");
        testData.put(new byte[]{0x03, 0x56}, "INR");
        testData.put(new byte[]{0x04, 0x14}, "KWD");
        testData.put(new byte[]{0x04, (byte)0x84}, "MXN");
        testData.put(new byte[]{0x04, (byte)0x98}, "MDL");
        testData.put(new byte[]{0x05, 0x78}, "NOK");
        testData.put(new byte[]{0x06, 0x34}, "QAR");
        testData.put(new byte[]{0x06, 0x43}, "RUB");
        testData.put(new byte[]{0x07, 0x02}, "SGD");
        testData.put(new byte[]{0x07, 0x52}, "SEK");
        testData.put(new byte[]{0x07, 0x56}, "CHF");
        testData.put(new byte[]{0x08, 0x26}, "GBP");
        testData.put(new byte[]{0x08, 0x07}, "MKD");
        testData.put(new byte[]{0x08, 0x40}, "USD");
        testData.put(new byte[]{0x09, 0x33}, "BYN");
        testData.put(new byte[]{0x09, 0x41}, "RSD");
        testData.put(new byte[]{0x09, 0x44}, "AZN");
        testData.put(new byte[]{0x09, 0x46}, "RON"); // This might cause troubles as a legacy currency ROL might be returned on some implementations of Currency
        testData.put(new byte[]{0x09, 0x49}, "TRY");
        testData.put(new byte[]{0x09, 0x75}, "BGN");
        testData.put(new byte[]{0x09, 0x77}, "BAM");
        testData.put(new byte[]{0x09, 0x78}, "EUR");
        testData.put(new byte[]{0x09, (byte)0x80}, "UAH");
        testData.put(new byte[]{0x09, (byte)0x81}, "GEL");
        testData.put(new byte[]{0x09, (byte)0x85}, "PLN");

        for(byte[] numericCode : testData.keySet()){
            c = TransactionContextHelper.getCurrency(numericCode);
            assertEquals("Currency code does not match!", testData.get(numericCode), c.getCurrencyCode());
        }

    }

    private static final TransactionContext usdTrx = new TransactionContext() {
        @Override
        public String getAid() {
            return null;
        }

        @Override
        public double getAmount() {
            return 7;
        }

        @Override
        public byte[] getCurrencyCode() {
            return new byte[]{0x08, 0x40};
        }

        @Override
        public byte[] getTrxDate() {
            return new byte[0];
        }

        @Override
        public byte getTrxType() {
            return 0;
        }

        @Override
        public DigitalizedCardStatus getDigitalizedCardStatus() {
            return null;
        }

        @Override
        public void wipe() {

        }
    };
    private static final TransactionContext inrTrx = new TransactionContext() {
        @Override
        public String getAid() {
            return null;
        }

        @Override
        public double getAmount() {
            return 34;
        }

        @Override
        public byte[] getCurrencyCode() {
            return new byte[]{0x03, 0x56};
        }

        @Override
        public byte[] getTrxDate() {
            return new byte[0];
        }

        @Override
        public byte getTrxType() {
            return 0;
        }

        @Override
        public DigitalizedCardStatus getDigitalizedCardStatus() {
            return null;
        }

        @Override
        public void wipe() {

        }
    };
    private static final TransactionContext jpyTrx = new TransactionContext() {
        @Override
        public String getAid() {
            return null;
        }

        @Override
        public double getAmount() {
            return 99;
        }

        @Override
        public byte[] getCurrencyCode() {
            return new byte[]{0x03, (byte)0x92};
        }

        @Override
        public byte[] getTrxDate() {
            return new byte[0];
        }

        @Override
        public byte getTrxType() {
            return 0;
        }

        @Override
        public DigitalizedCardStatus getDigitalizedCardStatus() {
            return null;
        }

        @Override
        public void wipe() {

        }

        @Override
        public byte[] getRawAmount() {
            return new byte[0];
        }
    };

    @Test
    public void testFormatAmountWithCurrency(){
        // This is to ensure what is the default locale as the next assertions will expect en_US to be the default
        assertEquals("en_US", Locale.getDefault().toString());

        assertEquals("EUR12.34", TransactionContextHelper.formatAmountWithCurrency(TransactionContextHelper.getMockTransactionContext()));
        assertEquals("$7.00", TransactionContextHelper.formatAmountWithCurrency(usdTrx));
        assertEquals("INR34.00", TransactionContextHelper.formatAmountWithCurrency(inrTrx));
        assertEquals("JPY99", TransactionContextHelper.formatAmountWithCurrency(jpyTrx));

        // try with different Locales
        Locale.setDefault(Locale.GERMANY);
        assertEquals("12,34 €", TransactionContextHelper.formatAmountWithCurrency(TransactionContextHelper.getMockTransactionContext()));
        assertEquals("7,00 USD", TransactionContextHelper.formatAmountWithCurrency(usdTrx));
        assertEquals("$7.00", TransactionContextHelper.formatAmountWithCurrency(usdTrx, Locale.US));

        Locale.setDefault(Locale.JAPAN);
        assertEquals("￥99", TransactionContextHelper.formatAmountWithCurrency(jpyTrx));

    }

    @Ignore
    @Test
    public void testPrintCurrencies(){
        final Set<Currency> availableCurrencies = Currency.getAvailableCurrencies();
        for(Currency c : availableCurrencies){
            final boolean isObsolete = c.getDisplayName().matches(".+\\(\\d+\\-\\d+\\)");
            System.out.println("Currency: " + c.getDisplayName() +
                               " code=" + c.getCurrencyCode() +
                               " num=" + c.getNumericCode()
                               + (isObsolete ? " OBSOLETE" : ""));
        }

        Currency[] cArray = new Currency[availableCurrencies.size()];
        cArray = availableCurrencies.toArray(cArray);

        final List<Currency> list = Arrays.asList(cArray);
        list.sort((c1, c2) -> c1.getNumericCode() - c2.getNumericCode());

        System.out.println();
        System.out.println();

        final HashMap<Integer, String> map = new HashMap<>();

        for(Currency c : list){
            final boolean isObsolete = c.getDisplayName().matches(".+\\(\\d+\\-\\d+\\)");
//            System.out.println("Currency: " + c.getDisplayName() +
//                               " code=" + c.getCurrencyCode() +
//                               " num=" + c.getNumericCode()
//                               + (isObsolete ? " OBSOLETE" : ""));

            if(!isObsolete){
                // generate a code like
//                map.put(498, "MDL"); // Moldovan Leu
                System.out.println("map.put("+c.getNumericCode()+", \""+c.getCurrencyCode()+"\"); // " + c.getDisplayName());

                if(map.containsKey(c.getNumericCode())){
                    System.out.println("WARNING: Overwriting currency with numCode="+c.getNumericCode());
                }
                map.put(c.getNumericCode(), c.getCurrencyCode());

            }
            else{
                // generate a code like
                // map.put(31, "AZM"); // Azerbaijani Manat (1993-2006) - OBSOLETE
                System.out.println("//map.put("+c.getNumericCode()+", \""+c.getCurrencyCode()+"\"); // " + c.getDisplayName() + " - OBSOLETE");
            }


        }
    }
}
