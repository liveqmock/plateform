package com.koala.promosupport.shoumeng;

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
 * 充值回调
 * --------------------------------------
 * orderId	渠道商订单ID	String	必填	是
 * uid	渠道商用户唯一标识	String	必填	是
 * amount	充值金额(单位：元)	int	必填	是
 * coOrderId	游戏商订单号	String	必填	是
 * serverId	充值服的ID	int	必填	否
 * success	订单状态：0成功，1失败	int	必填	是
 * msg	订单失败时的错误信息	String	否	否
 * sign	MD5签名结果	String	必填	否
 * --------------------------------------
 * MD5签名规则
 * 先将指定参数串按照参数名范例指定的次序，并将参数secret(由畅娱提供)及其值附加在排序后的参数串末尾，进行md5得到待签名字符串。
 * MD5签名采用标准MD5算法，得到32位小写的签名。
 * Secret参数由双方协商，不以参数的形式传递，供MD5签名时使用，不可以泄漏到第三方。
 * 
 * 签名示例
 * sign=md5(orderId=de123131&uid=1234&amount=50&coOrderId=abcdefg &success=0&secret=abcd)
 * 
 * 返回信息
 * 游戏服务器在接收到到渠道商的充值回调信息以后会返回 SUCCESS 该值只表示游戏服务器成功接收到了充值回调信息， 不代表游戏服务器为用户发放元宝成功。
 * 如果渠道商未接收到 SUCCESS 信息，则需要再次回调。
 * 
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class ShoumengPayCallback implements IPayCallback {

	private static final KGameLogger logger = KGameLogger
			.getLogger(ShoumengPayCallback.class);
	private ShoumengChannel ch;
	private PayOrder payOrder;
	public static final String SUCCESS = "SUCCESS";
	public static final String FAILED = "FAILED";

	private String orderId;
	private String uid;
	private String amount;
	private String coOrderId;
	private String serverId;
	private String success;
	private String msg;
	private String sign;

	public ShoumengPayCallback(ShoumengChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {
		this.orderId = params.get("orderId");// 渠道商订单ID String 必填 是
		this.uid = params.get("uid");// 渠道商用户唯一标识 String 必填 是
		this.amount = params.get("amount");// 充值金额(单位：元) int 必填 是
		this.coOrderId = params.get("coOrderId");// 游戏商订单号 String 必填 是
		this.serverId = params.get("serverId");// 充值服的ID int 必填 否
		this.success = params.get("success");// 订单状态：0成功，1失败 int 必填 是
		this.msg = params.get("msg");// 订单失败时的错误信息 String 否 否
		this.sign = params.get("sign");// MD5签名结果 String 必填 否

		logger.info("【{}充值】生成callback {}", ch.getPromoID(), this);

		// 状态
		if (!"0".equalsIgnoreCase(success)) {
			logger.info("【{}充值】订单状态不为0,{}", ch.getPromoID(), msg);
			return SUCCESS;// ?
		}

		// 签名
		if (!PromoSupport.getInstance().isDebugPayNoSign()) {
			String gen_sign = MD5.MD5Encode("orderId=" + orderId + "&uid="
					+ uid + "&amount=" + amount + "&coOrderId=" + coOrderId
					+ "&success=" + success + "&secret=" + ch.getPayKey());
			if (!gen_sign.equalsIgnoreCase(sign)) {
				logger.info("【{}充值】签名不一致.", ch.getPromoID());
				return FAILED;// ?
			}
		}

		PayExtParam pext = new PayExtParam(coOrderId);
		String payWay = "0";// 支付渠道没有返回payWay就用0代替了
		String otherinfo = "serverId" + serverId;
		String paytime = PayOrder.FORMAT_PAYTIME.format(new Date());
		this.payOrder = new PayOrder(pext, orderId, String.valueOf(Float
				.parseFloat(amount) * 100), uid, payWay, paytime, otherinfo);

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
		return "ShoumengPayCallback [orderId=" + orderId + ", uid=" + uid
				+ ", amount=" + amount + ", coOrderId=" + coOrderId
				+ ", serverId=" + serverId + ", success=" + success + ", msg="
				+ msg + ", sign=" + sign + "]";
	}

}
