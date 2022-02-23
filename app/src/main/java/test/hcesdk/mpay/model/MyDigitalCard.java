package test.hcesdk.mpay.model;

import com.gemalto.mfs.mwsdk.dcm.DigitalizedCard;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardStatus;

import java.io.Serializable;

public class MyDigitalCard implements Serializable {

    private String tokenId;
    private String digitalizedCardId;
    private boolean isDefaultCardFlag;
    private boolean isRemotePaymentSupported;

    private DigitalizedCardStatus cardStatus;
    private boolean selected;

    public MyDigitalCard(DigitalizedCard card) {
        setTokenId(card.getTokenizedCardID());
    }


    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getDigitalizedCardId() {
        return digitalizedCardId;
    }

    public void setDigitalizedCardId(String digitalizedCardId) {
        this.digitalizedCardId = digitalizedCardId;
    }

    public boolean isDefaultCardFlag() {
        return isDefaultCardFlag;
    }

    public void setDefaultCardFlag(boolean defaultCard) {
        isDefaultCardFlag = defaultCard;
    }

    public boolean isRemotePaymentSupported() {
        return isRemotePaymentSupported;
    }

    public void setRemotePaymentSupported(boolean remotePaymentSupported) {
        isRemotePaymentSupported = remotePaymentSupported;
    }

    public DigitalizedCardStatus getCardStatus() {
        return cardStatus;
    }



    public void setCardStatus(DigitalizedCardStatus cardStatus) {
        this.cardStatus = cardStatus;
    }

    @Override
    public String toString() {
        return "MyDigitalCard{" + "tokenId='" + tokenId + '\'' + ", digitalizedCardId='"
               + digitalizedCardId + '\'' + ", isDefaultCardFlag=" + isDefaultCardFlag + ", cardState="
               + (cardStatus!=null?cardStatus.getState():"unknown")
               + ", needsReplenishment=" + (cardStatus!=null? cardStatus.needsReplenishment():"unknown")
               + ", numberOfPaymentsLeft=" + (cardStatus!=null?cardStatus.getNumberOfPaymentsLeft():"unknown")
               + ", expiryDate='" +  (cardStatus!=null?cardStatus.getExpiryDate():"unknown") + '\''
               + '}';
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean getSelected() {
        return selected;
    }
}
