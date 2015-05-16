package com.koala.promosupport.kudong;

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
 * 参数名 参数说明
 * uid 用户名，传送之前 urlencode，使用utf-8 编码
 * oid 订单号，不允许超过 30 位
 * gold 充值金额（人民币或者游戏币，默认人民币 单位为元）
 * sid 游戏服，为 Sn 的格式，n 为大于/等于 1 的整数，注意“S”为大写
 * time 发送请求的时间，UNIX 时间戳
 * eif Cp 自定义信息原样返回
 * sign 验证签名，其中的 key 测试使用正式的 key 由 CP 告知蘑菇技术人员mogoo-wsydUhNvdap7EFYX-test
 * Sign 签名规则sign = md5(uid-sid-oid-gold-time-key)注意：Key 为字符串安全码，由 CP 提供， 请在联合调试的时候告知蘑菇技术,中间的 “-” 是必须连接字符， MD5 加密结果为 32 位大写字母和数字混合串
 * 
 * 
 * 服务器返回数据 通知划账成功：{error_code:0,error_message:""}
 * 通知划账失败：{error_code:1,error_message:"错误原因"}
 * 
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class KudongPayCallback implements IPayCallback {

	private static final KGameLogger logger = KGameLogger
			.getLogger(KudongPayCallback.class);
	private KudongChannel ch;
	private PayOrder payOrder;
	private static final String SUCCESS = "{error_code:0,error_message:\"\"}";
	private static final String FAILED = "{error_code:1,error_message:\"签名失败\"}";

	private String uid ;//用户名，传送之前 urlencode，使用utf-8 编码
	private String oid ;//订单号，不允许超过 30 位
	private String gold;//充值金额（人民币或者游戏币，默认人民币 单位为元）
	private String sid ;//游戏服，为 Sn 的格式，n 为大于/等于 1 的整数，注意“S”为大写
	private String time;//发送请求的时间，UNIX 时间戳
	private String eif ;//Cp 自定义信息原样返回        

	public KudongPayCallback(KudongChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {
		this.uid =params.get("uid"); //用户名，传送之前 urlencode，使用utf-8 编码                      
		this.oid =params.get("oid"); //订单号，不允许超过 30 位                                     
		this.gold=params.get("gold");//充值金额（人民币或者游戏币，默认人民币 单位为元）                          
		this.sid =params.get("sid"); //游戏服，为 Sn 的格式，n 为大于/等于 1 的整数，注意“S”为大写               
		this.time=params.get("time");//发送请求的时间，UNIX 时间戳                                   
		this.eif =params.get("eif"); //Cp 自定义信息原样返回                                       
		String sign = params.get("sign");

		logger.info("【{}充值】生成callback {}", ch.getPromoID(), this);

		// 签名 签名规则sign = md5(uid-sid-oid-gold-time-key)
		if (!PromoSupport.getInstance().isDebugPayNoSign()) {
			StringBuilder sb = new StringBuilder();
			sb.append(uid).append("-")
			.append(sid).append("-")
			.append(oid).append("-")
			.append(gold).append("-")
			.append(time).append("-")
			.append(ch.getSignKey());
			String gen_sign = MD5.MD5Encode(sb.toString());
			if (!gen_sign.equalsIgnoreCase(sign)) {
				logger.info("【{}充值】签名不一致.", ch.getPromoID());
				return FAILED;// ?
			}
		}

		PayExtParam pext = new PayExtParam(eif);
		String payWay = "0";// 支付渠道没有返回payWay就用0代替了
		String paytime = PayOrder.FORMAT_PAYTIME.format(new Date(Long.parseLong(time)*1000));
		this.payOrder = new PayOrder(pext, oid, String.valueOf(Float
				.parseFloat(gold) * 100), uid, payWay, paytime, sid);

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
		return "KudongPayCallback [uid=" + uid + ", oid=" + oid + ", gold="
				+ gold + ", sid=" + sid + ", time=" + time + ", eif=" + eif
				+ "]";
	}
	
}
