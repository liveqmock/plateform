package com.koala.promosupport.zhidiantong;

import java.util.HashMap;
import java.util.Map;

import org.jdom.Element;

import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.AbstractPromoChannel;

public class BXChannel extends AbstractPromoChannel {

	private String cp_id;
	private String cp_key;
	private String game_id;
	private String server_id;
	private String channel_id;

	public BXChannel(int promoID, String description) {
		super(promoID, description);
	}

	@Override
	public void init(Element xml, boolean reload) throws Exception {
		super.init(xml, reload);
		cp_id = xml.getChildTextTrim("cp_id");
		cp_key = xml.getChildTextTrim("cp_key");
		game_id = xml.getChildTextTrim("game_id");
		server_id = xml.getChildTextTrim("server_id");
		channel_id = xml.getChildTextTrim("channel_id");
	}

	@Override
	public IPayCallback newPayCallback() {
		return new BXPayCallback(this);
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

	public String getCp_id() {
		return cp_id;
	}

	public String getCp_key() {
		return cp_key;
	}

	public String getGame_id() {
		return game_id;
	}

	public String getServer_id() {
		return server_id;
	}

	public String getChannel_id() {
		return channel_id;
	}

	@Override
	public String toString() {
		return "BXChannel [cp_id=" + cp_id + ", cp_key=" + cp_key
				+ ", game_id=" + game_id + ", server_id=" + server_id
				+ ", channel_id=" + channel_id + ", toString()="
				+ super.toString() + "]";
	}

}
