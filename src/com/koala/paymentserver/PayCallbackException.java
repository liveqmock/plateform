package com.koala.paymentserver;

public class PayCallbackException extends Exception {

	private static final long serialVersionUID = -8199950117730239521L;
	private ExCode code;

	public enum ExCode {
		/** 没找到对应的渠道 */
		PS2GS_NOTFOUND,
		/** 签名验证失败 */
		SIGNVERIFY_FAILED,
		/** 订单生成失败 */
		ORDER_GEN_EXCEPTION,
		/** SDK服务器给的信息就是失败结果 */
		SDK_RESULTPARAM_FAILED,
		/** 解析SDK服务器请求参数错误 */
		ANALYZE_CALLBACKPARAMS_ERROR
	}

	public PayCallbackException(ExCode code, String message) {
		super(message);
		this.code = code;
	}

	public ExCode getCode() {
		return code;
	}
}
