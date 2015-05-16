package com.koala.promosupport.cooguo;

import java.util.HashMap;
import java.util.Map;

import org.jdom.Element;

import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.AbstractPromoChannel;

public class CooguoChannel extends AbstractPromoChannel {

	private String appkey;
	private String gameid;
	private String channelid;

	public CooguoChannel(int promoID, String description) {
		super(promoID, description);
	}

	@Override
	public void init(Element xml, boolean reload) throws Exception {
		super.init(xml, reload);
		appkey = xml.getChildTextTrim("appkey");
		gameid = xml.getChildTextTrim("gameid");
		channelid = xml.getChildTextTrim("channelid");
	}

	@Override
	public IPayCallback newPayCallback() {
		return new CooguoPayCallback(this);
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

	public String getAppkey() {
		return appkey;
	}

	public String getGameid() {
		return gameid;
	}

	public String getChannelid() {
		return channelid;
	}

	@Override
	public String toString() {
		return "CooguoChannel [appkey=" + appkey + ", gameid=" + gameid
				+ ", channelid=" + channelid + ", toString()="
				+ super.toString() + "]";
	}

}
