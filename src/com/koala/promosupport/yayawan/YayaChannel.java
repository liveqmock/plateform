package com.koala.promosupport.yayawan;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.jdom.Element;

import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.IUserVerify;
import com.koala.promosupport.PromoChannel;
import com.koala.promosupport.PromoSupport;

public class YayaChannel implements PromoChannel {
	
	private int promoID;
	private String description;
	private String urlUserVerify;
	private String methodUserVerify;
	
	private PayCallbackMethod payCallbackMethod;
	private IUserVerify action_userverify;
	
	private boolean canLogin;
	private boolean canPay;
	
	private Map<String, String> paramsToClientBeforePay;
	
	////////////////////////////////////////////////
	private String yayaywan_game_id;//>3469129058</yayaywan_game_id>
	private String yayawan_game_key;//>63d4cae708009e6f79a34c4dc8a0776d</yayawan_game_key>
	private String yayawan_game_secret;//>503825f178b1b0ae16c7a69b6328cd2f</yayawan_game_secret>
	private String yayawan_payment_key;//b1bf94e322509fa894d0b78b695b4dad</yayawan_payment_key>
	private String source_id;//>txbcs</source_id>
	////////////////////////////////////////////////
	
	public YayaChannel(int promoID, String description) {
		this.promoID = promoID;
		this.description = description;
	}
	
	@Override
	public void init(Element xml, boolean reload) throws Exception {
		canLogin = Boolean.parseBoolean(xml.getAttributeValue("canlogin"));
		canPay = Boolean.parseBoolean(xml.getAttributeValue("canpay"));
		
		yayaywan_game_id = xml.getChildTextTrim("yayaywan_game_id");
		yayawan_game_key = xml.getChildTextTrim("yayawan_game_key");
		yayawan_game_secret = xml.getChildTextTrim("yayawan_game_secret");
		yayawan_payment_key = xml.getChildTextTrim("yayawan_payment_key");
		source_id = xml.getChildTextTrim("source_id");
		
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

	@Override
	public int getPromoID() {
		return promoID;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public boolean canLogin() {
		return canLogin;
	}

	@Override
	public boolean canPay() {
		return (canLogin && canPay);
	}

	@Override
	public int getParentPromoID() {
		return PromoSupport.computeParentPromoID(promoID);
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
	public IPayCallback newPayCallback() {
		return new YayaPayCallback(this);
	}

	@Override
	public PayCallbackMethod getPayCallbackMethod() {
		return payCallbackMethod;
	}

	@Override
	public Map<String, String> getParamsToClientBeforePay() {
		if (paramsToClientBeforePay == null) {
			paramsToClientBeforePay = new HashMap<String, String>();
			// -充值S2C的参数MAP：orderId、ext、price、product、exchange_ratio
			paramsToClientBeforePay.put("ext", "");
			paramsToClientBeforePay.put("price", "0");
			paramsToClientBeforePay.put("orderId", "");
			paramsToClientBeforePay.put(PromoChannel.PARAM_KEY_OPENOURPRICEUI, String.valueOf(true));
		}
		return paramsToClientBeforePay;
	}

	public String getYayaywan_game_id() {
		return yayaywan_game_id;
	}

	public String getYayawan_game_key() {
		return yayawan_game_key;
	}

	public String getYayawan_game_secret() {
		return yayawan_game_secret;
	}

	public String getYayawan_payment_key() {
		return yayawan_payment_key;
	}

	public String getSource_id() {
		return source_id;
	}

	@Override
	public String toString() {
		return "YayaChannel [promoID=" + promoID + ", description="
				+ description + ", urlUserVerify=" + urlUserVerify
				+ ", methodUserVerify=" + methodUserVerify
				+ ", payCallbackMethod=" + payCallbackMethod + ", canLogin="
				+ canLogin + ", canPay=" + canPay + ", yayaywan_game_id="
				+ yayaywan_game_id + ", yayawan_game_key=" + yayawan_game_key
				+ ", yayawan_game_secret=" + yayawan_game_secret
				+ ", yayawan_payment_key=" + yayawan_payment_key
				+ ", source_id=" + source_id + "]";
	}

	
}
