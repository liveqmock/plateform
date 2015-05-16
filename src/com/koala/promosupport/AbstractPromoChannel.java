package com.koala.promosupport;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Element;

import com.koala.game.KGameProtocol;
import com.koala.game.util.StringUtil;

public abstract class AbstractPromoChannel implements PromoChannel,KGameProtocol {

	private int promoID;
	private String description;

	private String userverifyUrl;
	private String userverfiyHttpMethod;
//	private String payCallbackUrl;
	private PayCallbackMethod payCallbackMethod;
	private IUserVerify action_userverify;

	private boolean canLogin;
	private boolean canPay;
//	private boolean openpriceui;

	private final Map<String, String> paramsToClientBeforePay = new HashMap<String, String>();

	public AbstractPromoChannel(int promoID, String description) {
		this.promoID = promoID;
		this.description = description;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void init(Element xml, boolean reload) throws Exception {
		canLogin = Boolean.parseBoolean(xml.getAttributeValue("canlogin"));
		canPay = Boolean.parseBoolean(xml.getAttributeValue("canpay"));
//		String openpriceuiStr = xml.getAttributeValue("openpriceui");
//		openpriceui = openpriceuiStr!=null?Boolean.parseBoolean(openpriceuiStr):false;

		Element eUserVerify = xml.getChild("UserVerify");
		userverifyUrl = eUserVerify.getAttributeValue("url");
		userverfiyHttpMethod = eUserVerify.getAttributeValue("method");

		//生成IUserVerify对象实例
		Class<IUserVerify> clazz = (Class<IUserVerify>) Class
				.forName(eUserVerify.getAttributeValue("clazz"));
		Constructor<IUserVerify> constructor = clazz.getConstructor(this
				.getClass());
		action_userverify = constructor.newInstance(this);
		// System.out.println("================================"+action_userverify);
		
		
//		payCallbackUrl = xml.getChildTextTrim("PayCallbackUrl");
		String pm = xml.getChildTextTrim("PayCallbackMethod");
		if ("GET".equalsIgnoreCase(pm)) {
			payCallbackMethod = PayCallbackMethod.GET;
		} else if ("POST".equalsIgnoreCase(pm)) {
			payCallbackMethod = PayCallbackMethod.POST;
		} else {
			payCallbackMethod = PayCallbackMethod.GET_POST;
		}
		//支付/充值发给客户端的参数KV，包括客户端发给SDK服务器的参数
		Element ePayCallbackParams2Client = xml.getChild("PayCallbackParams2Client");
		if(ePayCallbackParams2Client!=null){
			List<Element> params = ePayCallbackParams2Client.getChildren("param");
			if (reload) {
				paramsToClientBeforePay.clear();
			}
			for (Element eparam : params) {
				String k = eparam.getAttributeValue("key");
				String v = eparam.getAttributeValue("value");
				System.out.println("to client param【"+promoID+"】--- "+k+":"+v);
				if(!StringUtil.hasNullOr0LengthString(k)){
					paramsToClientBeforePay.put(k, v);
				}
			}
		}
	}

	@Override
	public int getPromoID() {
		return promoID;
	}

	@Override
	public String getDescription() {
		return description;
	}

	public String getUserVerifyUrl() {
		return userverifyUrl;
	}

	public String getUserVerifyHttpMethod() {
		return userverfiyHttpMethod;
	}

	@Override
	public boolean canLogin() {
		return canLogin;
	}

	@Override
	public boolean canPay() {
		return (canLogin && canPay);
	}

	//@Override
//	public boolean openDefaultRechargeUI() {
//		return openpriceui;
//	}

	@Override
	public Map<String, String> getParamsToClientBeforePay() {
		return paramsToClientBeforePay;
	}
	
	public String getParamToClientBeforePay(String key){
		return getParamsToClientBeforePay().get(key);
	}

	@Override
	public IUserVerify getUserVerify() {
		return action_userverify;
	}

	@Override
	public int getParentPromoID() {
		return PromoSupport.computeParentPromoID(promoID);
	}

	@Override
	public PayCallbackMethod getPayCallbackMethod() {
		return payCallbackMethod;
	}

//	public String getPayCallbackUrl() {
//		return payCallbackUrl;
//	}

	@Override
	public String toString() {
		return "AbstractPromoChannel [promoID=" + promoID + ", description="
				+ description + ", userverifyUrl=" + userverifyUrl
				+ ", userverfiyHttpMethod=" + userverfiyHttpMethod
				+ ", payCallbackMethod=" + payCallbackMethod + ", canLogin="
				+ canLogin + ", canPay=" + canPay + "]";
	}
	
	
}
