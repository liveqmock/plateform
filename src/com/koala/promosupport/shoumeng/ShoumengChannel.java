package com.koala.promosupport.shoumeng;

import java.util.HashMap;
import java.util.Map;

import org.jdom.Element;

import com.koala.game.KGameProtocol;
import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.AbstractPromoChannel;
import com.koala.promosupport.PromoSupport;
import com.koala.promosupport.PromoSupport.PriceUnit;

public class ShoumengChannel  extends AbstractPromoChannel{

	private String umengchannel;
	private String appKey;
	private String gameId;
	private String loginKey;
	private String payKey;
	
	public ShoumengChannel(int promoID, String description) {
		super(promoID, description);
	}
	
	@Override
	public void init(Element xml, boolean reload) throws Exception {
		super.init(xml, reload);
		umengchannel = xml.getChildTextTrim("UMENG_CHANNEL");
		appKey = xml.getChildTextTrim("UMENG_APPKEY");
		gameId = xml.getChildTextTrim("SHOUMENG_GAME_ID");
		loginKey = xml.getChildTextTrim("LoginKey");
		payKey = xml.getChildTextTrim("PayKey");
	}

	@Override
	public IPayCallback newPayCallback() {
		return new ShoumengPayCallback(this);
	}

//	@Override
//	public Map<String, String> getParamsToClientBeforePay() {
//		if (paramsToClientBeforePay == null) {
//			paramsToClientBeforePay = new HashMap<String, String>();
//			paramsToClientBeforePay.put(PARAM_KEY_OPENOURPRICEUI,Boolean.toString(openDefaultRechargeUI()));
//			//
//			paramsToClientBeforePay.put(CONST_PAYMENT_KEY_ORDERID, "");
//			paramsToClientBeforePay.put(CONST_PAYMENT_KEY_EXT, "");	
//			paramsToClientBeforePay.put("ratio", ""+PromoSupport.getInstance().getYuanBaoPrice(PriceUnit.FEN));
//			paramsToClientBeforePay.put("coinName", CONST_PAYMENT_VALUE_PRODUCE);
//		}
//		return paramsToClientBeforePay;
//	}

	public String getUmengchannel() {
		return umengchannel;
	}

	public String getAppKey() {
		return appKey;
	}

	public String getGameId() {
		return gameId;
	}

	public String getLoginKey() {
		return loginKey;
	}

	public String getPayKey() {
		return payKey;
	}

	@Override
	public String toString() {
		return "ShoumengChannel [umengchannel=" + umengchannel + ", appKey="
				+ appKey + ", gameId=" + gameId + ", loginKey=" + loginKey
				+ ", payKey=" + payKey + ", toString()=" + super.toString()
				+ "]";
	}

	
}
