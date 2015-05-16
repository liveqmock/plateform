package com.koala.promosupport.kunlun;

import org.jdom.Element;

import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.AbstractPromoChannel;

public class KunlunChannel extends AbstractPromoChannel {
	
	private String key;
	private String payCurrencyCode;
	private String payKey;
	
	private String coinName;
	private String coinPrice;
	
	public KunlunChannel(int promoID, String description) {
		super(promoID, description);
	}

	@Override
	public void init(Element xml, boolean reload) throws Exception {
		super.init(xml, reload);
		key = xml.getChildTextTrim("key");
		payCurrencyCode = xml.getChildTextTrim("PayCurrencyCode");
		payKey = xml.getChildTextTrim("PayKey");
		coinName = xml.getChildTextTrim("CoinName");
		coinPrice = xml.getChildTextTrim("CoinPrice");
	}

	@Override
	public IPayCallback newPayCallback() {
		return new KunlunPayCallback(this);
	}

	public String getKey() {
		return key;
	}
	
	public String getPayKey(){
		return payKey;
	}

	public String getPayCurrencyCode() {
		return payCurrencyCode;
	}

	public String getCoinName() {
		return coinName;
	}

	public String getCoinPrice() {
		return coinPrice;
	}

	@Override
	public String toString() {
		return "KunlunChannel [key=" + key + ", payCurrencyCode="
				+ payCurrencyCode + ", payKey=" + payKey + ", coinName="
				+ coinName + ", coinPrice=" + coinPrice + ", toString()="
				+ super.toString() + "]";
	}

}
