package com.koala.promosupport.mi;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.jdom.Element;

import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.IUserVerify;
import com.koala.promosupport.PromoChannel;
import com.koala.promosupport.PromoSupport;

public class MiChannel implements PromoChannel {

	private int promoID;
	private String description;

	private String appId;
	private String appKey;
	private String userverifyUrl;
	private String userverfiyHttpMethod;

	private PayCallbackMethod payCallbackMethod;

	private IUserVerify action_userverify;

	public MiChannel(int promoID, String description) {
		this.promoID = promoID;
		this.description = description;
	}

	public String getAppId() {
		return appId;
	}

	public String getAppKey() {
		return appKey;
	}

	public String getUserVerifyUrl() {
		return userverifyUrl;
	}

	public String getUserVerifyHttpMethod() {
		return userverfiyHttpMethod;
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

		appId = xml.getChildTextTrim("AppId");
		appKey = xml.getChildTextTrim("AppKey");

		String pm = xml.getChildTextTrim("PayCallbackMethod");
		if ("GET".equalsIgnoreCase(pm)) {
			payCallbackMethod = PayCallbackMethod.GET;
		} else if ("POST".equalsIgnoreCase(pm)) {
			payCallbackMethod = PayCallbackMethod.POST;
		} else {
			payCallbackMethod = PayCallbackMethod.GET_POST;
		}

		// if(!reload){
		// action_userverify= new MiUserVerify(this);
		// }

		Element eUserVerify = xml.getChild("UserVerify");
		userverifyUrl = eUserVerify.getAttributeValue("url");
		userverfiyHttpMethod = eUserVerify.getAttributeValue("method");

		@SuppressWarnings("unchecked")
		Class<IUserVerify> clazz = (Class<IUserVerify>) Class
				.forName(eUserVerify.getAttributeValue("clazz"));
		Constructor<IUserVerify> constructor = clazz.getConstructor(this
				.getClass());
		action_userverify = constructor.newInstance(this);

		// System.out.println("================================"+action_userverify);
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
		return new MiPayCallback(this);
	}

	@Override
	public PayCallbackMethod getPayCallbackMethod() {
		return payCallbackMethod;
	}

	@Override
	public String toString() {
		return "MiChannel [promoID=" + promoID + ", description=" + description
				+ ", appId=" + appId + ", appKey=" + appKey
				+ ", userverifyUrl=" + userverifyUrl
				+ ", userverfiyHttpMethod=" + userverfiyHttpMethod
				+ ", payCallbackMethod=" + payCallbackMethod + ", canLogin="
				+ canLogin + ", canPay=" + canPay + ", getParentPromoID="
				+ getParentPromoID() + "]";
	}

	private Map<String, String> paramsToClientBeforePay;

	@Override
	public Map<String, String> getParamsToClientBeforePay() {
		if (paramsToClientBeforePay == null) {
			paramsToClientBeforePay = new HashMap<String, String>();
			// -充值S2C的参数MAP：orderId、ext、price
			paramsToClientBeforePay.put("orderId", "");
			paramsToClientBeforePay.put("ext", "");
			// paramsToClientBeforePay.put(
			// "price",
			// /*String.valueOf(PromoSupport.getInstance().getYuanBaoPrice(
			// PriceUnit.FEN))*/"1");
			paramsToClientBeforePay.put(PARAM_KEY_OPENOURPRICEUI,
					String.valueOf(true));
		}
		return paramsToClientBeforePay;
	}
}
