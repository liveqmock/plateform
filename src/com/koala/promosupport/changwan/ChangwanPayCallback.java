package com.koala.promosupport.changwan;

import java.net.URLDecoder;
import java.util.Map;

import com.koala.game.logging.KGameLogger;
import com.koala.paymentserver.IPayCallback;
import com.koala.paymentserver.PayExtParam;
import com.koala.paymentserver.PayOrder;
import com.koala.promosupport.MD5;
import com.koala.promosupport.PromoSupport;

/**
 * <pre>

 * </pre>
 * 
 * @author AHONG
 * 
 */
public class ChangwanPayCallback implements IPayCallback {

	private static final KGameLogger logger = KGameLogger
			.getLogger(ChangwanPayCallback.class);
	private ChangwanChannel ch;
	private PayOrder payOrder;
//	1 通知成功
//	100 openid不存在
//	102 签名错误
//	103 其它未知错误
	private static final String SUCCESS = "1";// 接收成功
	private static final String ERROR_NOID = "100";// 接收成功
	private static final String ERRORSIGN = "102";// 签名错误
	private static final String ERROR = "103";// 未知错误

	private String serverid   ;//int 游戏服务器ID
	private String custominfo ;//string 客户端传入的自定义数据
	private String openid     ;//string 合作方账号唯一标识
	private String ordernum   ;//string 订单ID,畅玩订单系统唯一号
	private String status     ;//string 订单状态,1为成功,其他为失败
	private String paytype    ;//string充值类型, 详见附表PayType
	private String amount     ;//int 成功充值金额，单位为分
	private String errdesc    ;//string 充值失败错误码，成功为空
	private String paytime    ;//string 充值成功时间,yyyyMMddHHmmss
	private String sign       ;//string 所有参数+appkey的MD5签名拼串:serverid值|custominfo值|openid值|ordernum值|status值|paytype值|amount值|errdesc值|paytime值|appkey值(包含分隔符|)

	public ChangwanPayCallback(ChangwanChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {

		serverid  = params.get("serverid");
		custominfo= params.get("custominfo");
		openid    = params.get("openid");
		ordernum  = params.get("ordernum");
		status    = params.get("status");
		paytype   = params.get("paytype");
		amount    = params.get("amount");
		errdesc   = params.get("errdesc");
		paytime   = params.get("paytime");
		sign      = params.get("sign");
		
		logger.info("【{}充值】生成callback {}", ch.getPromoID(), this);

		if (!"1".equals(status)) {
			logger.info("【{}充值】SDK服务器返回失败状态.{} {}", ch.getPromoID(), status,errdesc);
			return ERROR;
		}

		// 签名
		if (!PromoSupport.getInstance().isDebugPayNoSign()) {
			StringBuilder sb = new StringBuilder();
			//所有参数+appkey的MD5签名拼串:serverid值|custominfo值|openid值|ordernum值|status值|paytype值|amount值|errdesc值|paytime值|appkey值(包含分隔符|)
			sb.append(serverid).append("|");
			sb.append(custominfo).append("|");
			sb.append(openid).append("|");
			sb.append(ordernum).append("|");
			sb.append(status).append("|");
			sb.append(paytype).append("|");
			sb.append(amount).append("|");
			sb.append(errdesc).append("|");
			sb.append(paytime).append("|");
			sb.append(ch.getAppKey());
			String gen_sign = MD5.MD5Encode(sb.toString());
			if (!gen_sign.equalsIgnoreCase(this.sign)) {
				logger.info("【{}充值】签名不一致.", ch.getPromoID());
				return ERRORSIGN;// ?
			}
		}

		PayExtParam pext = new PayExtParam(custominfo);
		this.payOrder = new PayOrder(pext, ordernum, amount, openid,
				paytype, PayOrder.convert2PaytimeFormat(paytime, "yyyyMMddHHmmss"), serverid);

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
		return "ChangwanPayCallback [serverid=" + serverid + ", custominfo="
				+ custominfo + ", openid=" + openid + ", ordernum=" + ordernum
				+ ", status=" + status + ", paytype=" + paytype + ", amount="
				+ amount + ", errdesc=" + errdesc + ", paytime=" + paytime
				+ ", sign=" + sign + "]";
	}



}
