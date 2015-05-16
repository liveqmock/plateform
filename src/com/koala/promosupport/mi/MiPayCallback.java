package com.koala.promosupport.mi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.koala.game.logging.KGameLogger;
import com.koala.paymentserver.IPayCallback;
import com.koala.paymentserver.PayExtParam;
import com.koala.paymentserver.PayOrder;
import com.koala.promosupport.PromoSupport;
import com.koala.thirdpart.json.JSONException;
import com.koala.thirdpart.json.JSONObject;

/**
 * <pre>
 * appId 必须 appId
 * cpOrderId 必须 开发商订单ID
 * cpUserInfo 可选 开发商透传信息
 * uid 必须 用户ID
 * orderId 必须 游戏平台订单ID
 * orderStatus 必须 订单状态TRADE_SUCCESS代表成功
 * payFee 必须 支付金额，单位为分，即0.01米币。
 * productCode 必须 商品代码
 * productName 必须 商品名称
 * productCount 必须 商品数量
 * payTime 必须 支付时间，格式yyyy-MM-ddHH:mm:ss
 * signature 必须 签名,签名方法见后面说明
 * ------------------------------------
 * errcode 必须 状态码，200成功
 *                 1506 cpOrderId错误
 *                 1515 appId错误
 *                 1516 uid错误
 *                 1525 signature错误
 *                 3515 订单信息不一致，用于和CP的订单校验
 * errMsg 可选 错误信息
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class MiPayCallback implements IPayCallback {

	private String appId;// 必须 appId
	private String cpOrderId;// 必须 开发商订单ID
	private String cpUserInfo;// 可选 开发商透传信息
	private String uid;// 必须 用户ID
	private String orderId;// 必须 游戏平台订单ID
	private String orderStatus;// 必须 订单状态TRADE_SUCCESS代表成功
	private String payFee;// 必须 支付金额，单位为分，即0.01米币。
	private String productCode;// 必须 商品代码
	private String productName;// 必须 商品名称
	private String productCount;// 必须 商品数量
	private String payTime;// 必须 支付时间，格式yyyy-MM-dd HH:mm:ss
	private String signature;// 必须 签名,签名方法见后面说明

	private static final KGameLogger logger = KGameLogger
			.getLogger(MiPayCallback.class);
	private MiChannel ch;
	private PayOrder payOrder;

	public MiPayCallback(MiChannel ch) {
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {

		this.appId = params.get("appId");
		this.cpOrderId = params.get("cpOrderId");
		this.cpUserInfo = params.get("cpUserInfo");
		this.uid = params.get("uid");
		this.orderId = params.get("orderId");
		this.orderStatus = params.get("orderStatus");
		this.payFee = params.get("payFee");
		this.productCode = params.get("productCode");
		this.productName = params.get("productName");
		this.productCount = params.get("productCount");
		this.payTime = params.get("payTime");
		this.signature = params.get("signature");

		logger.info("【{}充值】生成callback {}", ch.getPromoID(), this);

		/*
		 * errcode 必须 状态码，200成功 1506 cpOrderId错误 1515 appId错误 1516 uid错误 1525
		 * signature错误 3515 订单信息不一致，用于和CP的订单校验 errMsg 可选 错误信息
		 */
		if (!ch.getAppId().equals(appId)) {
			logger.info("【{}充值】AppId错误. got{}!={}", ch.getPromoID(), appId,
					ch.getAppId());
			return (new ErrResp(1515, "appId错误")).toString();
		}

		if (!"TRADE_SUCCESS".equalsIgnoreCase(orderStatus)) {
			logger.info("【{}充值】订单状态不为TRADE_SUCCESS. but {}", ch.getPromoID(),
					orderStatus);
			return (new ErrResp(3515, "订单状态不为TRADE_SUCCESS")).toString();
		}

		if (!PromoSupport.getInstance().isDebugPayNoSign()) {
			// 生成签名字符
			List<String> signkeys = new ArrayList<String>();
			for (String k : params.keySet()) {
				if (!"signature".equals(k)) {
					signkeys.add(k);
				}
			}
			Collections.sort(signkeys);// 根据参数名首字母升序
			StringBuilder buf = new StringBuilder();
			for (String k : signkeys) {
				String v = params.get(k);
				// 1. 必选参数必须有值, 而且参数值必须不为空，不为0.
				if (v != null && v.length() > 0) {
					buf.append("&").append(k).append("=").append(v);
				}
			}
			String mysig = HmacSHA1Encryption.hmacSHA1Encrypt(buf.substring(1),
					ch.getAppKey());
			if (!mysig.equals(signature)) {
				logger.info("【{}充值】signature错误. got:{}!={}", ch.getPromoID(),
						signature, mysig);
				return (new ErrResp(1525, "signature错误")).toString();
			}
		}

		PayExtParam pext = new PayExtParam(cpUserInfo);
		String payWay = "0";// 支付渠道没有返回payWay就用0代替了
		String otherinfo = (new StringBuilder().append("cpOrderId=")
				.append(cpOrderId).append("&productCode=").append(productCode)
				.append("&productName=").append(productName)
				.append("&productCount=").append(productCount)).toString();
		this.payOrder = new PayOrder(pext, orderId, payFee, uid, payWay,
				payTime, otherinfo);

		logger.info("【{}充值】生成订单  {}", ch.getPromoID(), payOrder);

		return (new ErrResp(200, "成功")).toString();
	}

	@Override
	public String parse(String content) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String responseOfRepeatCallback() {
		return (new ErrResp(200, "成功")).toString();
	}

	@Override
	public PayOrder getGeneratedPayOrder() {
		return payOrder;
	}

	@Override
	public String toString() {
		return "MiPayCallback [appId=" + appId + ", cpOrderId=" + cpOrderId
				+ ", cpUserInfo=" + cpUserInfo + ", uid=" + uid + ", orderId="
				+ orderId + ", orderStatus=" + orderStatus + ", payFee="
				+ payFee + ", productCode=" + productCode + ", productName="
				+ productName + ", productCount=" + productCount + ", payTime="
				+ payTime + ", signature=" + signature + "]";
	}

	/**
	 * errcode 必须 状态码，200成功 1506 cpOrderId错误 1515 appId错误 1516 uid错误 1525
	 * signature错误 3515 订单信息不一致，用于和CP的订单校验 errMsg 可选 错误信息
	 */
	private class ErrResp {
		int errcode;
		String errMsg;

		public ErrResp(int errcode, String errMsg) {
			this.errcode = errcode;
			this.errMsg = errMsg;
		}

		@Override
		public String toString() {
			try {
				JSONObject j = new JSONObject();
				j.put("errcode", errcode);
				j.put("errMsg", errMsg);
				return j.toString();
			} catch (JSONException e) {
				return (new StringBuilder().append("{\"errcode\":")
						.append(errcode).append(",\"errMsg\":\"")
						.append(errMsg).append("\"}")).toString();
			}
		}
	}

}
