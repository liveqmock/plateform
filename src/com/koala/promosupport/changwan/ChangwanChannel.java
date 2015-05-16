package com.koala.promosupport.changwan;

import org.jdom.Element;

import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.AbstractPromoChannel;

public class ChangwanChannel extends AbstractPromoChannel {

	private String AppID   ;
	private String PacketID;
	private String AppKey  ;
	private String SignKey ;
	
	public ChangwanChannel(int promoID, String description) {
		super(promoID, description);
	}

	@Override
	public void init(Element xml, boolean reload) throws Exception {
		super.init(xml, reload);
		AppID    = xml.getChildTextTrim("AppID");
		PacketID = xml.getChildTextTrim("PacketID");
		AppKey   = xml.getChildTextTrim("AppKey");
		SignKey  = xml.getChildTextTrim("SignKey"); 
	}

	@Override
	public IPayCallback newPayCallback() {
		return new ChangwanPayCallback(this);
	}

	public String getAppID() {
		return AppID;
	}

	public String getPacketID() {
		return PacketID;
	}

	public String getAppKey() {
		return AppKey;
	}

	public String getSignKey() {
		return SignKey;
	}

	@Override
	public String toString() {
		return "ChangwanChannel [AppID=" + AppID + ", PacketID=" + PacketID
				+ ", AppKey=" + AppKey + ", SignKey=" + SignKey
				+ ", toString()=" + super.toString() + "]";
	}

//	@Override
//	public Map<String, String> getParamsToClientBeforePay() {
//		if (paramsToClientBeforePay == null) {
//			paramsToClientBeforePay = new HashMap<String, String>();
//			paramsToClientBeforePay.put(PARAM_KEY_OPENOURPRICEUI,Boolean.toString(openDefaultRechargeUI()));
//			//
//			paramsToClientBeforePay.put(CONST_PAYMENT_KEY_ORDERID, "");
//			paramsToClientBeforePay.put(CONST_PAYMENT_KEY_EXT, "");	
//			paramsToClientBeforePay.put(PROMO_KEY_NOTIFYURL,getPayCallbackUrl());
//		}
//		return paramsToClientBeforePay;
//	}


}
