package com.koala.promosupport.zhishanghudong;

import java.util.HashMap;
import java.util.Map;

import org.jdom.Element;

import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.AbstractPromoChannel;

public class AGameChannel extends AbstractPromoChannel {

	private String cpid;
	private String gameid;
	private String gamekey;
	private String gamename;

	public AGameChannel(int promoID, String description) {
		super(promoID, description);
	}

	@Override
	public void init(Element xml, boolean reload) throws Exception {
		super.init(xml, reload);
		cpid = xml.getChildTextTrim("cpid");
		gameid = xml.getChildTextTrim("gameid");
		gamekey = xml.getChildTextTrim("gamekey");
		gamename = xml.getChildTextTrim("gamename");
	}

	@Override
	public IPayCallback newPayCallback() {
		return new AGamePayCallback(this);
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

	public String getCpid() {
		return cpid;
	}

	public String getGameid() {
		return gameid;
	}

	public String getGamekey() {
		return gamekey;
	}

	public String getGamename() {
		return gamename;
	}

	@Override
	public String toString() {
		return "AGameChannel [cpid=" + cpid + ", gameid=" + gameid
				+ ", gamekey=" + gamekey + ", gamename=" + gamename
				+ ", toString()=" + super.toString() + "]";
	}

}
