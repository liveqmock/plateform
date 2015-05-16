package com.koala.promosupport.baoruan;

import java.util.HashMap;
import java.util.Map;

import org.jdom.Element;

import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.AbstractPromoChannel;

public class BaoruanChannel extends AbstractPromoChannel {

	private String appid;// <appid>176528395463506163</appid>
	private String uniquekey;// <uniquekey>dfc174e4ddf99552508f465e1bb83289</uniquekey>
	private String cid;// <cid>920</cid>
	private String key;// <key>ecbbee9f70a0c0f9d4ac3c426879f595</key>

	public BaoruanChannel(int promoID, String description) {
		super(promoID, description);
	}

	@Override
	public void init(Element xml, boolean reload) throws Exception {
		super.init(xml, reload);
		appid = xml.getChildTextTrim("appid");
		uniquekey = xml.getChildTextTrim("uniquekey");
		cid = xml.getChildTextTrim("cid");
		key = xml.getChildTextTrim("key");
	}

	@Override
	public IPayCallback newPayCallback() {
		return new BaoruanPayCallback(this);
	}

//	@Override
//	public Map<String, String> getParamsToClientBeforePay() {
//		if (paramsToClientBeforePay == null) {
//			paramsToClientBeforePay = new HashMap<String, String>();
//			paramsToClientBeforePay.put(PARAM_KEY_OPENOURPRICEUI,Boolean.toString(openDefaultRechargeUI()));
//			//
//			paramsToClientBeforePay.put(CONST_PAYMENT_KEY_ORDERID, "");
//			//paramsToClientBeforePay.put(CONST_PAYMENT_KEY_EXT, "");	
//			paramsToClientBeforePay.put(PROMO_KEY_NOTIFYURL,getPayCallbackUrl()+"?ext=");
//		}
//		return paramsToClientBeforePay;
//	}

	public String getAppid() {
		return appid;
	}

	public String getUniquekey() {
		return uniquekey;
	}

	public String getCid() {
		return cid;
	}

	public String getKey() {
		return key;
	}

	@Override
	public String toString() {
		return "BaoruanChannel [appid=" + appid + ", uniquekey=" + uniquekey
				+ ", cid=" + cid + ", key=" + key + ", toString()="
				+ super.toString() + "]";
	}

}
