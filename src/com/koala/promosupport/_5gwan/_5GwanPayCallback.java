package com.koala.promosupport._5gwan;

import java.util.Date;
import java.util.Map;

import com.koala.game.logging.KGameLogger;
import com.koala.paymentserver.IPayCallback;
import com.koala.paymentserver.PayExtParam;
import com.koala.paymentserver.PayOrder;
import com.koala.promosupport.MD5;
import com.koala.promosupport.PromoSupport;

/**
 * <pre>
 * 
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class _5GwanPayCallback implements IPayCallback {

	private static final KGameLogger logger = KGameLogger
			.getLogger(_5GwanPayCallback.class);
	private _5GwanChannel ch;
	private PayOrder payOrder;
	private static final String SUCCESS = "1";
	private static final String FAILED = "0";

	private String username  ;//     用户名
	private String change_id ;//      订单号
	private String money     ;//  金额（元）
	private String hash      ;// md5(username|change_id|money |app_key)
	private String ad_key    ;//   渠道号
	private String object    ;//   游戏方从客户端传回的自定义扩展参数，服务器号可该参数来传（可选）    

	public _5GwanPayCallback(_5GwanChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {
		this.username = params.get("username");//     用户名                                                        
		this.change_id= params.get("change_id");//      订单号                                                       
		this.money    = params.get("money");//  金额（元）                                                       
		this.hash     = params.get("hash");// md5(username|change_id|money |app_key)                            
		this.ad_key   = params.get("ad_key");//   渠道号                                                          
		this.object   = params.get("object");//   游戏方从客户端传回的自定义扩展参数，服务器号可该参数来传（可选）

		logger.info("【{}充值】生成callback {}", ch.getPromoID(), this);

		// 签名 
		if (!PromoSupport.getInstance().isDebugPayNoSign()) {
			StringBuilder sb = new StringBuilder();
			sb.append(username).append("|").append(change_id).append("|").append(money).append("|").append(ch.getApp_key());
			String gen_sign = MD5.MD5Encode(sb.toString());
			if (!gen_sign.equalsIgnoreCase(hash)) {
				logger.info("【{}充值】签名不一致.", ch.getPromoID());
				return FAILED;// ?
			}
		}

		PayExtParam pext = new PayExtParam(object);
		String payWay = "0";// 支付渠道没有返回payWay就用0代替了
		String paytime = PayOrder.FORMAT_PAYTIME.format(new Date());
		this.payOrder = new PayOrder(pext, change_id, String.valueOf(Float.parseFloat(money)*100), username, payWay,
				paytime, ad_key);

		logger.info("【{}充值】生成订单  {}", ch.getPromoID(), payOrder);

		return SUCCESS;
	}

	@Override
	public String parse(String content) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PayOrder getGeneratedPayOrder() {
		return payOrder;
	}

	@Override
	public String responseOfRepeatCallback() {
		return SUCCESS;
	}

	@Override
	public String toString() {
		return "_5GwanPayCallback [username=" + username + ", change_id="
				+ change_id + ", money=" + money + ", hash=" + hash
				+ ", ad_key=" + ad_key + ", object=" + object + "]";
	}


}
