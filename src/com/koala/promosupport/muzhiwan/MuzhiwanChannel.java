package com.koala.promosupport.muzhiwan;

import org.jdom.Element;

import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.AbstractPromoChannel;

public class MuzhiwanChannel extends AbstractPromoChannel {

	private String appkey;
	private String signkey;

	public MuzhiwanChannel(int promoID, String description) {
		super(promoID, description);
	}

	@Override
	public void init(Element xml, boolean reload) throws Exception {
		super.init(xml, reload);
		appkey = xml.getChildTextTrim("appkey");
		signkey = xml.getChildTextTrim("signkey");
	}

	@Override
	public IPayCallback newPayCallback() {
		return new MuzhiwanPayCallback(this);
	}

	public String getAppkey() {
		return appkey;
	}

	public String getSignkey() {
		return signkey;
	}

	@Override
	public String toString() {
		return "MuzhiwanChannel [appkey=" + appkey +",signkey="+signkey+ ", toString()="
				+ super.toString() + "]";
	}

}
