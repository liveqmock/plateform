package com.koala.promosupport.souhu;

import org.jdom.Element;

import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.AbstractPromoChannel;

public class SouhuChannel extends AbstractPromoChannel {

	private String appId;// 对接应用 ID String Y 对接前搜狐分配
	private String appKey;// 对接应用 Key String Y 对接前搜狐分配
	private String appSecret;

	public SouhuChannel(int promoID, String description) {
		super(promoID, description);
	}

	@Override
	public void init(Element xml, boolean reload) throws Exception {
		super.init(xml, reload);
		appId = xml.getChildTextTrim("appId");
		appKey = xml.getChildTextTrim("appKey");
		appSecret = xml.getChildTextTrim("appSecret");
	}

	@Override
	public IPayCallback newPayCallback() {
		return new SouhuPayCallback(this);
	}

//	@Override
//	public Map<String, String> getParamsToClientBeforePay() {
//		if (paramsToClientBeforePay == null) {
//			paramsToClientBeforePay = new HashMap<String, String>();
//			paramsToClientBeforePay.put(PARAM_KEY_OPENOURPRICEUI,Boolean.toString(openDefaultRechargeUI()));
//			//
//			paramsToClientBeforePay.put(CONST_PAYMENT_KEY_ORDERID, "");
//			paramsToClientBeforePay.put(CONST_PAYMENT_KEY_EXT, "");	
//			paramsToClientBeforePay.put("productName", CONST_PAYMENT_VALUE_PRODUCE);	
////			paramsToClientBeforePay.put(PROMO_KEY_NOTIFYURL,getPayCallbackUrl());
//		}
//		return paramsToClientBeforePay;
//	}

	public String getAppId() {
		return appId;
	}

	public String getAppKey() {
		return appKey;
	}

	public String getAppSecret() {
		return appSecret;
	}

	@Override
	public String toString() {
		return "ShouhuChannel [appId=" + appId + ", appKey=" + appKey
				+ ", appSecret=" + appSecret + ", toString()="
				+ super.toString() + "]";
	}

}
