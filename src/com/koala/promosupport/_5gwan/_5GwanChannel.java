package com.koala.promosupport._5gwan;

import java.util.HashMap;
import java.util.Map;

import org.jdom.Element;

import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.AbstractPromoChannel;

public class _5GwanChannel extends AbstractPromoChannel {

	private String app_id;
	private String app_key;

	public _5GwanChannel(int promoID, String description) {
		super(promoID, description);
	}

	@Override
	public void init(Element xml, boolean reload) throws Exception {
		super.init(xml, reload);
		app_id = xml.getChildTextTrim("app_id");
		app_key = xml.getChildTextTrim("app_key");
	}

	@Override
	public IPayCallback newPayCallback() {
		return new _5GwanPayCallback(this);
	}

//	@Override
//	public Map<String, String> getParamsToClientBeforePay() {
//		if (paramsToClientBeforePay == null) {
//			paramsToClientBeforePay = new HashMap<String, String>();
//			paramsToClientBeforePay.put(PARAM_KEY_OPENOURPRICEUI,Boolean.toString(openDefaultRechargeUI()));
//			//
//			paramsToClientBeforePay.put(CONST_PAYMENT_KEY_ORDERID, "");
//			paramsToClientBeforePay.put(CONST_PAYMENT_KEY_EXT, "");	
////			paramsToClientBeforePay.put(PROMO_KEY_NOTIFYURL,getPayCallbackUrl());
//		}
//		return paramsToClientBeforePay;
//	}

//	public String getApp_id() {
//		return app_id;
//	}

	public String getApp_key() {
		return app_key;
	}

	@Override
	public String toString() {
		return "_5GwanChannel [app_id=" + app_id + ", app_key=" + app_key
				+ ", toString()=" + super.toString() + "]";
	}

}
