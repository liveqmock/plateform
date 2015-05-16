package com.koala.promosupport.duokoo;

import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;

import com.koala.game.logging.KGameLogger;
import com.koala.paymentserver.IPayCallback;
import com.koala.paymentserver.PayExtParam;
import com.koala.paymentserver.PayOrder;
import com.koala.promosupport.MD5;
import com.koala.promosupport.PromoSupport;

/**
 * 支付购买结果通知
 * 
 * <pre>
 * 参数名       参数类型       参数说明     备注 
 * ----------+------------+---------+----------
 * amount         String  成功充值金额（单位为元，已按汇率结算），汇率请参看附表 必选 
 * cardtype       String  充值类型（参见附表）  必选 
 * orderid        String  订单ID  必选 
 * result         String  充值结果，1 表示成功，2表示失败  必选 
 * timetamp       String  订单成功时间，北京UNIX时间  必选 
 * aid            String  商品描述（原文返回）,对应客户端传入的字段 可选 
 * client_secret  String  所有参数+ app_secret的MD5码（字母小写） 
 *                        amount值+ cardtype值+ orderid值+ result值+ timetamp 值+app_secret值+urlencode(aid)值(计算时无+号) 
 *                        注:aid参数有特殊字符和中文的时候，一定要进行urlencode后参与签名。 必选
 * -------------------------
 * 请求返回格式 
 * HTTP响应  含义  备注 
 * --------+------+-----------------
 * SUCCESS  成功   
 * ERROR_TIME  时间戳超时  正负15分钟内有效 
 * ERROR_SIGN  验证码错误  client_secret 验证错误 
 * ERROR_REPEAT  订单重复提交   
 * ERROR_USER  无此用户或角色不存在   
 * ERROR_FAIL  其他错误
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class DkPayCallback implements IPayCallback {

	private static final KGameLogger logger = KGameLogger
			.getLogger(DkPayCallback.class);
	private DkChannel ch;
	private PayOrder payOrder;

	private final static long MS_OF_15MIN = 15 * 60 * 1000;// ms

	// -------------------------------------------------------------------
	private String amount;// 成功充值金额（单位为元，已按汇率结算），汇率请参看附表 必选
	private String cardtype;// 充值类型（参见附表） 必选
	private String orderid;// 订单ID 必选
	private String result;// 充值结果，1 表示成功，2表示失败 必选
	private String timetamp;// 订单成功时间，北京UNIX时间 必选
	private String aid;// 商品描述（原文返回）,对应客户端传入的字段 可选
	private String client_secret;// 所有参数+ app_secret的MD5码（字母小写）: amount值+
									// cardtype值+ orderid值+ result值+ timetamp
									// 值+app_secret值+urlencode(aid)值(计算时无+号)
									// 注:aid参数有特殊字符和中文的时候，一定要进行urlencode后参与签名。
									// 必选
	// 请求返回格式
	// HTTP响应 含义 备注
	// --------+------+-----------------
	private final static String SUCCESS = "SUCCESS";// 成功
	private final static String ERROR_TIME = "ERROR_TIME";// 时间戳超时 正负15分钟内有效
	private final static String ERROR_SIGN = "ERROR_SIGN";// 验证码错误
	private final static String ERROR_REPEAT = "ERROR_REPEAT";// 订单重复提交
	private final static String ERROR_USER = "ERROR_USER";// 无此用户或角色不存在
	private final static String ERROR_FAIL = "ERROR_FAIL";// 其他错误

	// -------------------------------------------------------------------

	public DkPayCallback(DkChannel ch) {
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {

		this.amount = params.get("amount");
		this.cardtype = params.get("cardtype");
		this.orderid = params.get("orderid");
		this.result = params.get("result");
		this.timetamp = params.get("timetamp");
		this.aid = params.get("aid");// 商品描述（原文返回）,对应客户端传入的字段 可选
		this.client_secret = params.get("client_secret");

		logger.info("【{}充值】生成callback {}", ch.getPromoID(), this);

		try {
			// 充值结果，1 表示成功，2表示失败 必选----------------
			if (!"1".equals(result)) {
				logger.info("【{}充值】失败:{}", ch.getPromoID(), "result!=1");
				return SUCCESS;//提示：result=2也要返回SUCCESS，表示接收“充值失败”这个事实 成功
			}

			// 检测时间超时-----------------
			long ts = Long.parseLong(timetamp)*1000;//s
			long ct = System.currentTimeMillis();
			if ((ct-ts) > MS_OF_15MIN) {// 超时了
				logger.info("【{}充值】失败:{}", ch.getPromoID(), "时间戳超时 正负15分钟内有效");
				return ERROR_TIME;
			}

			// 检测签名----------------------
			if (!PromoSupport.getInstance().isDebugPayNoSign()) {
				// amount值+ cardtype值+ orderid值+ result值+ timetamp
				// 值+app_secret值+urlencode(aid)值(计算时无+号)
				StringBuilder buf = new StringBuilder();
				buf.append(amount).append(cardtype).append(orderid)
						.append(result).append(timetamp)
						.append(ch.getAppSecret())
						.append(URLEncoder.encode(aid, "utf-8"));
				if (!MD5.MD5Encode(buf.toString()).equalsIgnoreCase(
						client_secret)) {
					logger.info("【{}充值】失败:{}", ch.getPromoID(),
							"验证码错误 client_secret验证错误");
					return ERROR_SIGN;
				}
			}

			//开始生成订单
			PayExtParam pext = new PayExtParam(aid);
			String payWay = cardtype;// 支付渠道没有返回payWay就用0代替了
			String paytime = PayOrder.FORMAT_PAYTIME.format(new Date(ts));
			this.payOrder = new PayOrder(pext, orderid, String.valueOf(Float
					.parseFloat(amount) * 100), "", payWay, paytime, "timetamp="+this.timetamp);

			logger.info("【{}充值】生成订单  {}", ch.getPromoID(), payOrder);

		} catch (Exception e) {
			e.printStackTrace();
			return ERROR_FAIL;
		}
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
		return ERROR_REPEAT;
	}

	@Override
	public String toString() {
		return "DkPayCallback [amount=" + amount + ", cardtype=" + cardtype
				+ ", orderid=" + orderid + ", result=" + result + ", timetamp="
				+ timetamp + ", aid=" + aid + ", client_secret="
				+ client_secret + "]";
	}
	
	
}
