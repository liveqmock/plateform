package com.koala.promosupport.kunlun;

import java.util.Date;
import java.util.Map;

import com.koala.game.logging.KGameLogger;
import com.koala.paymentserver.IPayCallback;
import com.koala.paymentserver.PayExtParam;
import com.koala.paymentserver.PayOrder;
import com.koala.promosupport.MD5;
import com.koala.promosupport.PromoSupport;
import com.koala.promosupport.PromoSupport.PriceUnit;
import com.koala.thirdpart.json.JSONObject;

/**
 * <pre>
 * 本请求用于昆仑平台代理游戏充值回调
 * 1.用户选择支付渠道进行支付--》2.用户完成支付--》3.支付渠道回调昆仑平台-》4.昆仑平台回调游戏加金币接口
 * 
 * 参数
 * 参数 含义
 * String oid 昆仑平台订单号20位字符
 * String uid 昆仑平台订单用户ID64位整数1-2^63-1注意数据库字段长度
 * String amount 昆仑平台订单金额注意二位小数1.00
 * String coins 昆仑平台订单金币
 * String dtime 昆仑平台订单完成时间戳例如：1293840000表示2011-01-01 00:00:00
 * String sign 请求检验串加密规则 md5(oid+uid+amount+coins+dtime+KEY) KEY：由昆仑平台提供 
 * String ext 回调扩展字段 {"uname":"maohangjun@gmail.com","partnersorderid":"test_order_12345"}
 * -------------------
 * 返回值[JSON格式]
 * 值 含义
 * Integer retcode 0表示成功如：{"retcode":0,"retmsg":"success"}其它值表示失败可以使用除0外任意数字如：{"retcode":-1,"retmsg":"加金币失败"}
 * String retmsg 提示信息
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class KunlunPayCallback implements IPayCallback {

	private static final KGameLogger logger = KGameLogger
			.getLogger(KunlunPayCallback.class);
	private KunlunChannel ch;
	private PayOrder payOrder;

	private static final String SUCCESS = "{\"retcode\":0,\"retmsg\":\"success\"}";// 接收成功
	private static final String ERRORSIGN = "{\"retcode\":1,\"retmsg\":\"error sign\"}";// 签名错误
	private static final String ERROR = "{\"retcode\":2,\"retmsg\":\"error\"}";// 未知错误
	
	private String oid    ;//昆仑平台订单号20位字符
	private String uid    ;//昆仑平台订单用户ID64位整数1-2^63-1注意数据库字段长度
	private String amount ;//昆仑平台订单金额注意二位小数1.00
	private String coins  ;//昆仑平台订单金币
	private String dtime  ;//昆仑平台订单完成时间戳例如：1293840000表示2011-01-01 00:00:00
	private String sign   ;//请求检验串加密规则 md5(oid+uid+amount+coins+dtime+KEY) KEY：由昆仑平台提供 
	private String ext    ;//回调扩展字段 {"uname":"maohangjun@gmail.com","partnersorderid":"test_order_12345"}

	public KunlunPayCallback(KunlunChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {

		oid   = params.get("oid");
		uid   = params.get("uid");
		amount= params.get("amount");
		coins = params.get("coins");
		dtime = params.get("dtime");
		sign  = params.get("sign");
		ext   = params.get("ext");

		logger.info("【{}充值】生成callback {}", ch.getPromoID(), this);

		// 签名
		if (!PromoSupport.getInstance().isDebugPayNoSign()) {
			StringBuilder sb = new StringBuilder();
			//请求检验串加密规则 md5(oid+uid+amount+coins+dtime+KEY) KEY：由昆仑平台提供 
			sb.append(oid).append(uid).append(amount).append(coins).append(dtime).append(ch.getPayKey());
			String gen_sign = MD5.MD5Encode(sb.toString(),"utf-8");
			if (!gen_sign.equalsIgnoreCase(this.sign)) {
				logger.info("【{}充值】签名不一致.", ch.getPromoID());
				return ERRORSIGN;// ?
			}
		}
		
		JSONObject jext = new JSONObject(ext);
		String uname = jext.optString("uname");
		String partnersorderid = jext.optString("partnersorderid");

		PayExtParam pext = new PayExtParam(partnersorderid);
		this.payOrder = new PayOrder(pext, 
				oid,
				String.valueOf(Float.parseFloat(amount)*100), 
				uid,
				"0",
				PayOrder.FORMAT_PAYTIME.format(new Date(Long.parseLong(dtime)*1000)),
				"coins="+coins+",uname="+uname);
		//商品附加信息
		this.payOrder.setGoodsName(ch.getCoinName());
		this.payOrder.setGoodsPrice(Float.parseFloat(ch.getCoinPrice()));
		this.payOrder.setGoodsCount(Integer.parseInt(coins));
		this.payOrder.setPayCurrencyCode(ch.getPayCurrencyCode());
		//this.payOrder.setCoins(Integer.parseInt(coins));

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
		return "KunlunPayCallback [oid=" + oid + ", uid=" + uid + ", amount="
				+ amount + ", coins=" + coins + ", dtime=" + dtime + ", sign="
				+ sign + ", ext=" + ext + "]";
	}



}
