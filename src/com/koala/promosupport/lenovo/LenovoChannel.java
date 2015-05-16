package com.koala.promosupport.lenovo;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.jdom.Element;

import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.IUserVerify;
import com.koala.promosupport.PromoChannel;
import com.koala.promosupport.PromoSupport;
import com.koala.promosupport.PromoSupport.PriceUnit;

public class LenovoChannel implements PromoChannel {

	private int promoID;
	private String description;

	private String appId;
	private String payKey;
	private String realm;
	private String appKey;

	private String userverifyUrl;
	private String userverfiyHttpMethod;

	private PayCallbackMethod payCallbackMethod;
	private String payCallbackUrl;

	private IUserVerify action_userverify;

	public LenovoChannel(int promoID, String description) {
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

		appId = xml.getChildTextTrim("AppId");
		payKey = xml.getChildTextTrim("PayKey");
		realm = xml.getChildTextTrim("Realm");
		appKey = xml.getChildTextTrim("AppKey");

		Element eUserVerify = xml.getChild("UserVerify");
		userverifyUrl = eUserVerify.getAttributeValue("url");
		userverfiyHttpMethod = eUserVerify.getAttributeValue("method");

		payCallbackUrl = xml.getChildTextTrim("PayCallbackUrl");
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

	public String getAppId() {
		return appId;
	}

	public String getPayKey() {
		return payKey;
	}

	public String getAppKey() {
		return appKey;
	}

	public String getRealm() {
		return realm;
	}

	public String getUserverifyUrl() {
		return userverifyUrl;
	}

	public String getUserverfiyHttpMethod() {
		return userverfiyHttpMethod;
	}

	@Override
	public int getParentPromoID() {
		return PromoSupport.computeParentPromoID(promoID);
	}

	@Override
	public IPayCallback newPayCallback() {
		return new LenovoPayCallback(this);
	}

	@Override
	public PayCallbackMethod getPayCallbackMethod() {
		return payCallbackMethod;
	}

	@Override
	public String toString() {
		return "LenovoChannel [promoID=" + promoID + ", description="
				+ description + ", appId=" + appId + ", payKey=" + payKey
				+ ", realm=" + realm + ", appKey=" + appKey
				+ ", userverifyUrl=" + userverifyUrl
				+ ", userverfiyHttpMethod=" + userverfiyHttpMethod
				+ ", payCallbackMethod=" + payCallbackMethod
				+ ", payCallbackUrl=" + payCallbackUrl + ", canLogin="
				+ canLogin + ", canPay=" + canPay + ", getParentPromoID="
				+ getParentPromoID() + "]";
	}

	private Map<String, String> paramsToClientBeforePay;

	@Override
	public Map<String, String> getParamsToClientBeforePay() {
		if (paramsToClientBeforePay == null) {
			paramsToClientBeforePay = new HashMap<String, String>();
			// 充值S2C的参数MAP：orderId、ext、appKey、notifyUri、productCode、price(单位分)
			paramsToClientBeforePay.put("ext", "");
			paramsToClientBeforePay.put(
					"price",
					String.valueOf(PromoSupport.getInstance().getYuanBaoPrice(
							PriceUnit.FEN)));
			paramsToClientBeforePay.put("orderId", "");
			paramsToClientBeforePay.put("appKey", getPayKey());
			paramsToClientBeforePay.put("notifyUri", payCallbackUrl);// TODO
																		// 要测试确认
			paramsToClientBeforePay.put("productCode", "2");// 后台配置的商品编号
			paramsToClientBeforePay.put(PARAM_KEY_OPENOURPRICEUI,
					String.valueOf(true));
		}
		return paramsToClientBeforePay;
	}

	// public static void main(String[] args) {
	// System.out.println(String.valueOf(Float.parseFloat(PromoSupport.getInstance().getYuanBaoPrice4RMBYuan())*100));
	// }
}
