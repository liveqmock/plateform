package com.koala.promosupport.kudong;

import org.jdom.Element;

import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.AbstractPromoChannel;

public class KudongChannel extends AbstractPromoChannel {

	private String app_id;// 130
	private String app_key;// WoPVtdCt
	private String channel_code;// 12|128
	private String signKey;

	public KudongChannel(int promoID, String description) {
		super(promoID, description);
	}

	@Override
	public void init(Element xml, boolean reload) throws Exception {
		super.init(xml, reload);
		app_id = xml.getChildTextTrim("app_id");
		app_key = xml.getChildTextTrim("app_key");
		channel_code = xml.getChildTextTrim("channel_code");
		signKey = xml.getChildTextTrim("signKey");
	}

	@Override
	public IPayCallback newPayCallback() {
		return new KudongPayCallback(this);
	}

	public String getApp_id() {
		return app_id;
	}

	public String getApp_key() {
		return app_key;
	}

	public String getChannel_code() {
		return channel_code;
	}
	
	public String getSignKey(){
		return signKey;
	}

	@Override
	public String toString() {
		return "KudongChannel [app_id=" + app_id + ", app_key=" + app_key
				+ ", channel_code=" + channel_code + ", toString()="
				+ super.toString() + "]";
	}

}
