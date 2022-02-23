package test.hcesdk.mpay.payment.qr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.gemalto.mfs.mwsdk.dcm.DigitalizedCard;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardManager;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardStatus;
import com.gemalto.mfs.mwsdk.dcm.PaymentType;
import com.gemalto.mfs.mwsdk.payment.engine.TransactionContext;
import com.gemalto.mfs.mwsdk.payment.engine.qrcode.QRCodeData;
import com.gemalto.mfs.mwsdk.provisioning.ProvisioningServiceManager;
import com.gemalto.mfs.mwsdk.utils.async.AbstractAsyncHandler;
import com.gemalto.mfs.mwsdk.utils.async.AsyncResult;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.CharacterSetECI;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.payneteasy.tlv.BerTag;
import com.payneteasy.tlv.BerTlvBuilder;
import com.payneteasy.tlv.HexUtil;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import test.hcesdk.mpay.App;
import test.hcesdk.mpay.R;
import test.hcesdk.mpay.util.AppLogger;
import test.hcesdk.mpay.util.Util;

public class QRCodeFragment extends Fragment {

    private static final String TAG = ".QRCodeFragment";

    QRCodeData qrCodeData;
    TransactionContext transactionContext;
    ImageView qrImage;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qr, container, false);
        qrImage = view.findViewById(R.id.qrImage);
        view.findViewById(R.id.btnOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().onBackPressed();
            }
        });
        setQRImage();
        sendReplenishmentRequestIfNeeded();
        return view;
    }

    private void setQRImage() {
        if (qrCodeData == null || transactionContext == null) {
            return;
        }
        String qrDataBase64 = Base64.encodeToString(Util.hexStringToByteArray(buildQrTlvStandard(qrCodeData, transactionContext)), Base64.DEFAULT);
        QRCodeWriter writer = new QRCodeWriter();
        try {
            // ECI Mode for QR code and error correction level
            Map hintMap = new HashMap();
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            hintMap.put(EncodeHintType.CHARACTER_SET, CharacterSetECI.ISO8859_2);
            BitMatrix bitMatrix = writer.encode(qrDataBase64, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap;
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 50, bs);
            Bitmap b = BitmapFactory.decodeByteArray(bs.toByteArray(), 0, bs.toByteArray().length);
            qrImage.setImageBitmap(b);
        } catch (WriterException e) {
            AppLogger.e("WriterException: ", e.getMessage());
        }
    }

    private String buildQrTlvStandard(QRCodeData qrCodeData, TransactionContext transactionContext) {
        String tag85Str = null;
        String template61Str1 = null;
        String template63Str1 = null;
        String template63str2 = null;
        String template61Str2 = null;
        String template62str1 = null;
        String tag82str = null;
        String tag95str = null;
        String tag9Astr = null;
        String tag9Cstr = null;
        String tag9F1Astr = null;
        String tag9F03str = null;
        String tag9F27str = null;
        String tagC5str = null;

        // build payload formater
        byte[] tag85 = new BerTlvBuilder()
                .addHex(new BerTag(0x85), "4350563031") //CPV01
                .buildArray();
        tag85Str = HexUtil.toHexString(tag85);

        // build Static Chip data field
        byte[] tag82 = new BerTlvBuilder()
                .addHex(new BerTag(0x82), "0000") // ApplicationInterchangeProfile
                .buildArray();
        tag82str = HexUtil.toHexString(tag82);
        byte[] tag95 = new BerTlvBuilder()
                .addHex(new BerTag(0x95), "0000000000") // TransactionVerificationResult (TVR)
                .buildArray();
        tag95str = HexUtil.toHexString(tag95);
        byte[] tag9A = new BerTlvBuilder()
                .addHex(new BerTag(0x9A), HexUtil.toHexString(transactionContext.getTrxDate())) // Transaction Date
                .buildArray();
        tag9Astr = HexUtil.toHexString(tag9A);
        byte[] tag9C = new BerTlvBuilder()
                // NOTE: Currently sdk returning one zero to construct proper tag appended extra zero, later to be removed.
                .addHex(new BerTag(0x9C), Byte.toString(transactionContext.getTrxType()) + 0) // TransactionType
                .buildArray();
        tag9Cstr = HexUtil.toHexString(tag9C);
        byte[] tag9F1A = new BerTlvBuilder()
                .addHex(new BerTag(0x9F, 0x1A), "0000") // terminalCountryCode
                .buildArray();
        tag9F1Astr = HexUtil.toHexString(tag9F1A);
        byte[] tag9F03 = new BerTlvBuilder()
                .addHex(new BerTag(0x9F, 0x03), "000000000000") // amountOther
                .buildArray();
        tag9F03str = HexUtil.toHexString(tag9F03);
        byte[] tag9F27 = new BerTlvBuilder()
                .addHex(new BerTag(0x9F, 0x27), "80") // cryptogramInformationData
                .buildArray();
        tag9F27str = HexUtil.toHexString(tag9F27);
        byte[] tagC5 = new BerTlvBuilder()
                .addHex(new BerTag(0xC5), HexUtil.toHexString(qrCodeData.getCID()))
                .buildArray();
        tagC5str = HexUtil.toHexString(tagC5);

        if (qrCodeData.getCardMainAppTemplate() != null) {
            // build template 61 for application 1
            byte[] tag63 = new BerTlvBuilder()
                    .addHex(new BerTag(0x63),
                            HexUtil.toHexString(qrCodeData.getChipDataField()) + tag82str + tag95str + tag9Astr + tag9Cstr + tag9F1Astr + tag9F03str + tag9F27str)
                    .buildArray();
            template63Str1 = HexUtil.toHexString(tag63);
            byte[] tag61 = new BerTlvBuilder()
                    .addHex(new BerTag(0x61), HexUtil.toHexString(qrCodeData.getCardMainAppTemplate()) + tagC5str + template63Str1)
                    .buildArray();
            template61Str1 = HexUtil.toHexString(tag61);
        } else {
            template61Str1 = "";
        }

        if (qrCodeData.getCardAliasAppTemplate() != null) {
            // build template 61 for application 2
            byte[] tag632 = new BerTlvBuilder()
                    .addHex(new BerTag(0x63),
                            HexUtil.toHexString(qrCodeData.getChipDataField()) + tag82str + tag95str + tag9Astr + tag9Cstr + tag9F1Astr + tag9F03str + tag9F27str)
                    .buildArray();
            template63str2 = HexUtil.toHexString(tag632);
            byte[] tag612 = new BerTlvBuilder()
                    .addHex(new BerTag(0x61), HexUtil.toHexString(qrCodeData.getCardAliasAppTemplate()) + tagC5str + template63str2)
                    .buildArray();
            template61Str2 = HexUtil.toHexString(tag612);

        } else {
            template61Str2 = "";
        }

        if (qrCodeData.getCommonDataTemplate() != null) {
            // build template 62
            byte[] tag62 = new BerTlvBuilder()
                    .addHex(new BerTag(0x62), HexUtil.toHexString(qrCodeData.getCommonDataTemplate()))
                    .buildArray();
            template62str1 = HexUtil.toHexString(tag62);
        } else {
            template62str1 = "";
        }
        //  create complete QR TLV
        return tag85Str + template61Str1 + template61Str2 + template62str1;
    }


    private void sendReplenishmentRequestIfNeeded() {
        final String token = DigitalizedCardManager.getDefault(PaymentType.CONTACTLESS, null).waitToComplete().getResult();
        if (token != null) {
            DigitalizedCard defaultCard = DigitalizedCardManager.getDigitalizedCard(token);
            defaultCard.getCardState(new AbstractAsyncHandler<DigitalizedCardStatus>() {
                @Override
                public void onComplete(AsyncResult<DigitalizedCardStatus> asyncResult) {
                    if (!asyncResult.isSuccessful()) {
                        AppLogger.e(TAG, "Failed to get card status");
                        return;
                    }
                    if (getActivity() == null) {
                        AppLogger.e(TAG, "Fragment is already gone");
                        return;
                    }

                    DigitalizedCardStatus status = asyncResult.getResult();
                    if (status.needsReplenishment()) {
                        AppLogger.i(TAG, "Requesting replenishment");
                        ProvisioningServiceManager.getProvisioningBusinessService().sendRequestForReplenishment(token,
                                (App) getActivity().getApplication());
                    } else {
                        AppLogger.i(TAG, "Requesting is not needed");
                    }
                }
            });
        }

    }

}
