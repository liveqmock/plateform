package com.koala.promosupport.yayawan;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.netty.handler.codec.http.QueryStringDecoder;

import com.koala.game.logging.KGameLogger;
import com.koala.paymentserver.IPayCallback;
import com.koala.paymentserver.PayExtParam;
import com.koala.paymentserver.PayOrder;
import com.koala.promosupport.MD5;
import com.koala.promosupport.PromoSupport;

/**
 * <pre>
 * 计费通知地址 
 *     由游戏厂商提供充值接收地址用于接收充值结果 
 * 
 * 如何接收支付结果 
 *     在支付订单处理完后,yayawan会将支付结果发送到游戏厂商提供的接通知地址。由计费通 
 * 知地址对应的程序完成通知验证
 * 
 *   通知方式 
 *     HTTP   POST 请求。
 * 
 * 参数及说明
 * status	     状态  succeed 成功 pending 等待
 * money           金额	单位 分
 * uid                充值用户id
 * username           充值用户
 * transnum       平台流水号
 * orderid	    游戏商提供的订单号
 * ext                游戏商提供的附加信息
 * time			 时间戳
 * sign	       签名
 * 
 *  如何验证通知合法性 
 *     1.拼接sign，格式如下： 
 *     orderid.transnum.username.money.status.yayawan_payment_key
 *     . 为连接 符号，不参与签名 yayawan_payment_key 为 yayawan 平台分配
 *     2.将第一步的拼接结果通过MD5 加密，并转为小写，与通知接口接收到的sign 参数比较，如果相符，表明通知合法，反之则为非法通知
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class YayaPayCallback implements IPayCallback {

	private static final KGameLogger logger = KGameLogger
			.getLogger(YayaPayCallback.class);
	private YayaChannel ch;
	private PayOrder payOrder;

	private String status;// 状态 succeed 成功 pending 等待
	private String money;// 金额 单位 分
	private String uid;// 充值用户id
	private String username;// 充值用户
	private String transnum;// 平台流水号
	private String orderid;// 游戏商提供的订单号
	private String ext;// 游戏商提供的附加信息
	private String time;// 时间戳
	private String sign;// 签名

	private final String SUCCESS = "succeed";

	public YayaPayCallback(YayaChannel ch) {
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {
		
		if(params==null||params.size()<=0){
				return "FAILED";
		}
		
		status = params.get("status");
		money = params.get("money");
		uid = params.get("uid");
		username = params.get("username");
		transnum = params.get("transnum");
		orderid = params.get("orderid");
		ext = params.get("ext");
		time = params.get("time");
		sign = params.get("sign");

		if (!"succeed".equals(status)) {
			logger.info("【{}充值】失败:{}", ch.getPromoID(), status);
			return SUCCESS;
		}

		// 检测签名----------------------
		if (!PromoSupport.getInstance().isDebugPayNoSign()) {
			StringBuilder buf = new StringBuilder();
			buf.append(orderid).append(transnum).append(username).append(money)
					.append(status).append(ch.getYayawan_payment_key());
			if (!MD5.MD5Encode(buf.toString()).equalsIgnoreCase(sign)) {
				logger.info("【{}充值】失败:{}", ch.getPromoID(), "验证码错误 sign验证错误");
				return null;
			}
		}

		// 开始生成订单
		PayExtParam pext = new PayExtParam(ext);
		String payWay = "0";// 支付渠道没有返回payWay就用0代替了
		String paytime =  PayOrder.FORMAT_PAYTIME.format(new Date(Long.parseLong(time)*1000));
		this.payOrder = new PayOrder(pext, orderid, String.valueOf(Float
				.parseFloat(money)), uid, payWay, paytime, username + ","
				+ transnum);

		logger.info("【{}充值】生成订单  {}", ch.getPromoID(), payOrder);

		return SUCCESS;
	}

	@Override
	public String parse(String content) throws Exception {
		
		if (content == null || content.length() == 0) {
			return "FAILED";
		}
		
		Map<String, String> params = new HashMap<String, String>();
		QueryStringDecoder decoderQuery = new QueryStringDecoder(
				content, false);
		Map<String, List<String>> uriAttributes = decoderQuery
				.getParameters();
		for (String key : uriAttributes.keySet()) {
			for (String valuen : uriAttributes.get(key)) {
				params.put(key, valuen);
				logger.debug("[Param] {} = {}", key, valuen);
			}
		}
		
		return parse(params);
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
		return "YayaPayCallback [status=" + status + ", money=" + money
				+ ", uid=" + uid + ", username=" + username + ", transnum="
				+ transnum + ", orderid=" + orderid + ", ext=" + ext
				+ ", time=" + time + ", sign=" + sign + "]";
	}

	
}
