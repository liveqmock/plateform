package com.koala.promosupport.xxwan;

import org.jdom.Element;

import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.AbstractPromoChannel;

public class XXWanChannel extends AbstractPromoChannel {

	private String appkey;
	private String xx_game_id;
	private String platformId;
	private String payCurrencyCode;

	public XXWanChannel(int promoID, String description) {
		super(promoID, description);
	}

	@Override
	public void init(Element xml, boolean reload) throws Exception {
		super.init(xml, reload);
		appkey = xml.getChildTextTrim("appkey");
		xx_game_id = xml.getChildTextTrim("xx_game_id");
		platformId = xml.getChildTextTrim("platformId");
		payCurrencyCode = xml.getChildTextTrim("payCurrencyCode");
	}

	@Override
	public IPayCallback newPayCallback() {
		return new XXWanPayCallback(this);
	}

	public String getAppkey() {
		return appkey;
	}

	public String getXx_game_id() {
		return xx_game_id;
	}

	public String getPlatformId() {
		return platformId;
	}
	
	public String getPayCurrencyCode() {
		return payCurrencyCode;
	}

	@Override
	public String toString() {
		return "XXWanChannel [appkey=" + appkey + ", xx_game_id=" + xx_game_id
				+ ", platformId=" + platformId + ", toString()="
				+ super.toString() + "]";
	}


}
