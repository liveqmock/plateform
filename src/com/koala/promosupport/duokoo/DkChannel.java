package com.koala.promosupport.duokoo;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.jdom.Element;

import com.koala.game.KGameProtocol;
import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.IUserVerify;
import com.koala.promosupport.PromoChannel;
import com.koala.promosupport.PromoSupport;
import com.koala.promosupport.PromoSupport.PriceUnit;

public class DkChannel implements PromoChannel {

	private int promoID;
	private String description;

	private int appId;
	private String appKey;
	private String appSecret;

	private String urlUserVerify;
	private String methodUserVerify;

	private PayCallbackMethod payCallbackMethod;

	private IUserVerify action_userverify;

	public DkChannel(int promoID, String description) {
		this.promoID = promoID;
		this.description = description;
	}

	private boolean canLogin;

	@Override
	public boolean canLogin() {
		return canLogin;
	}

	private boolean canPay;

	@Override
	public boolean canPay() {
		return (canLogin && canPay);
	}

	public void init(Element xml, boolean reload) throws Exception {
		canLogin = Boolean.parseBoolean(xml.getAttributeValue("canlogin"));
		canPay = Boolean.parseBoolean(xml.getAttributeValue("canpay"));

		appId = Integer.parseInt(xml.getChildTextTrim("AppId"));
		appKey = xml.getChildTextTrim("AppKey");
		appSecret = xml.getChildTextTrim("AppSecret");

		Element eUserVerify = xml.getChild("UserVerify");
		urlUserVerify = eUserVerify.getAttributeValue("url");
		methodUserVerify = eUserVerify.getAttributeValue("method");

		String pm = xml.getChildTextTrim("PayCallbackMethod");
		if ("GET".equalsIgnoreCase(pm)) {
			payCallbackMethod = PayCallbackMethod.GET;
		} else if ("POST".equalsIgnoreCase(pm)) {
			payCallbackMethod = PayCallbackMethod.POST;
		} else {
			payCallbackMethod = PayCallbackMethod.GET_POST;
		}

		@SuppressWarnings("unchecked")
		Class<IUserVerify> clazz = (Class<IUserVerify>) Class
				.forName(eUserVerify.getAttributeValue("clazz"));
		Constructor<IUserVerify> constructor = clazz.getConstructor(this
				.getClass());
		action_userverify = constructor.newInstance(this);
	}

	public int getAppId() {
		return appId;
	}

	public String getAppKey() {
		return appKey;
	}

	public String getAppSecret() {
		return appSecret;
	}

	public String getUrlUserVerify() {
		return urlUserVerify;
	}

	public String getMethodUserVerify() {
		return methodUserVerify;
	}

	@Override
	public IUserVerify getUserVerify() {
		return action_userverify;
	}

	@Override
	public int getPromoID() {
		return promoID;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public int getParentPromoID() {
		return PromoSupport.computeParentPromoID(promoID);
	}

	@Override
	public IPayCallback newPayCallback() {
		return new DkPayCallback(this);
	}

	@Override
	public PayCallbackMethod getPayCallbackMethod() {
		return payCallbackMethod;
	}

	@Override
	public String toString() {
		return "DkChannel [promoID=" + promoID + ", description=" + description
				+ ", appId=" + appId + ", appKey=" + appKey + ", appSecret="
				+ appSecret + ", urlUserVerify=" + urlUserVerify
				+ ", methodUserVerify=" + methodUserVerify
				+ ", payCallbackMethod=" + payCallbackMethod + ", canLogin="
				+ canLogin + ", canPay=" + canPay + ", getParentPromoID="
				+ getParentPromoID() + "]";
	}

	private Map<String, String> paramsToClientBeforePay;

	@Override
	public Map<String, String> getParamsToClientBeforePay() {
		if (paramsToClientBeforePay == null) {
			paramsToClientBeforePay = new HashMap<String, String>();
			// -充值S2C的参数MAP：orderId、ext、price、product、exchange_ratio
			paramsToClientBeforePay.put("ext", "");
			paramsToClientBeforePay.put("price", "0");
			paramsToClientBeforePay.put("product",
					KGameProtocol.CONST_PAYMENT_VALUE_PRODUCE);
			paramsToClientBeforePay.put("orderId", "");
			paramsToClientBeforePay.put("exchange_ratio", String
					.valueOf((int) PromoSupport.getInstance().getYuanBaoPrice(
							PriceUnit.FEN)));// 人民币与游戏充值币的默认比例，例如2，代表1元人民币可以兑换2个游戏币，整数。
			paramsToClientBeforePay.put(PARAM_KEY_OPENOURPRICEUI,
					String.valueOf(true)); // 测试全部开放UI 2013-09-25
		}
		return paramsToClientBeforePay;
	}
}
