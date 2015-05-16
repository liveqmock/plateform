package com.koala.promosupport.uc;

import java.util.Date;
import java.util.Map;

import com.koala.game.logging.KGameLogger;
import com.koala.game.util.DateUtil;
import com.koala.paymentserver.IPayCallback;
import com.koala.paymentserver.PayExtParam;
import com.koala.paymentserver.PayOrder;
import com.koala.promosupport.MD5;
import com.koala.promosupport.PromoSupport;
import com.koala.thirdpart.json.JSONObject;

/**
 * UC的充值结果回调实现
 * 
 * <pre>
 * 1.3.3充值结果回调接口
 * 1) 请求地址：即充值结果通知地址，由游戏合作商提供。游戏接入时，由游戏合
 * 作商提供给UC游戏运营人员，录入到UC的游戏平台中。
 * 2) 调用方式：HTTP POST
 * 3) 接口描述：
 * 用户在游戏中提交充值请求后，UC游戏平台会异步执行充值，在充值操作完
 * 成后，UC游戏平台通过该接口将充值结果发送给“游戏服务器”。
 * 此处定义本接口的规范，游戏合作商需根据此规范在“游戏服务器”实现本接口。
 * 此接口用于接收通过游戏SDK充值的结果通知，和通过PC页面进行直接充
 * 值的结果通知。
 * --------------------------------------
 * 6) 请求内容（json格式）：
 * 字段名称 字段说明 类型 必填 备注
 * data 支付结果数据 json Y
 * sign 签名参数 string Y MD5(cpId+签名内容+apiKey);签名内容为data所有子字段按字段名升序拼接（剔除&符号及回车和换行符）
 * 
 * 支付结果数据(对应data,采用json格式)
 * orderId 充值订单号 string Y 此订单号由UC游戏SDK生成，游戏客户端在进行充值时从SDK获得。
 * gameId 游戏编号 string Y 由UC分配
 * serverId 服务器编号 string Y 由UC分配
 * ucid UC账号 string Y
 * payWay 支付通道代码 string Y 支付通道代码见下文表格
 * amount 支付金额 string Y 单位：元。
 * callbackInfo 游戏合作商自定义参数 string Y 游戏客户端在充值时传入，SDK服务器不做任何处理，在进行充值结果回调时发送给游戏服务器。
 * orderStatus 订单状态 string Y S-成功支付F-支付失败
 * failedDesc 订单失败原因详细描述 string Y 如果是成功支付，则为空串。
 * roleId 角色编号 string N 对于通过PC页面直接充值的，此参数的值为之充值的角色ID；对于在SDK中充值的，无此参数。
 * intfType 订单场景 int N 0或无此字段：SDK方式1：WAP方式2：WEB方式仅接入了反充的游戏有此字段，如游戏同时使用反充和sdk支付，需兼容此字段有值和无值的情形。
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class UCPayCallback implements IPayCallback {

	private static final KGameLogger logger = KGameLogger
			.getLogger(UCPayCallback.class);

	// 6) 请求内容（json格式）：
	// data 支付结果数据 json Y
	// private String sign;// 签名参数 string Y
	// MD5(cpId+签名内容+apiKey);签名内容为data所有子字段按字段名升序拼接（剔除&符号及回车和换行符）

	// 支付结果数据(对应data,采用json格式)
	private String orderId;// 充值订单号 string Y 此订单号由UC游戏SDK生成，游戏客户端在进行充值时从SDK获得。
	private String gameId;// 游戏编号 string Y 由UC分配
	private String serverId;// 服务器编号 string Y 由UC分配
	private String ucid;// UC账号 string Y
	private String payWay;// 支付通道代码 string Y 支付通道代码见下文表格
	private String amount;// 支付金额 string Y 单位：元。
	private String callbackInfo;// 游戏合作商自定义参数 string Y
								// 游戏客户端在充值时传入，SDK服务器不做任何处理，在进行充值结果回调时发送给游戏服务器。
	private String orderStatus;// 订单状态 string Y S-成功支付F-支付失败
	private String failedDesc;// 订单失败原因详细描述 string Y 如果是成功支付，则为空串。

	private String roleId;// 角色编号 string N
							// 对于通过PC页面直接充值的，此参数的值为之充值的角色ID；对于在SDK中充值的，无此参数。
	private String intfType;// 订单场景 int N
							// 0或无此字段：SDK方式1：WAP方式2：WEB方式仅接入了反充的游戏有此字段，如游戏同时使用反充和sdk支付，需兼容此字段有值和无值的情形。
	private String sign;

	private UCChannel ch;
	private PayOrder payOrder;

	public UCPayCallback(UCChannel ch) {
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {
		logger.error("No Implement Method.");
		return null;
	}

	// HTTP请求的body内容：
	// {
	// "data":{
	// "orderId":"abcf1330",
	// "gameId":123,
	// "serverId":654,
	// "ucid":123456,
	// "payWay":1,
	// "amount":"100.00",
	// "callbackInfo":"custominfo=xxxxx#user=xxxx",
	// "orderStatus":"S",
	// "failedDesc":""
	// },
	// "sign":"e49bd00c3cf0744c7049e73e16ae8acd"
	// }
	@Override
	public String parse(String data) throws Exception {
		try{
		JSONObject json = new JSONObject(data);
		JSONObject jdata = json.optJSONObject("data");
		this.orderId = jdata.optString("orderId");
		this.gameId = jdata.optString("gameId");
		this.serverId = jdata.optString("serverId");
		this.ucid = jdata.optString("ucid");
		this.payWay = jdata.optString("payWay");
		this.amount = jdata.optString("amount");
		this.callbackInfo = jdata.optString("callbackInfo");
		this.orderStatus = jdata.optString("orderStatus");
		this.failedDesc = jdata.optString("failedDesc");

		// 角色编号 string N 对于通过PC页面直接充值的，此参数的值为之充值的角色ID；对于在SDK中充值的，无此参数。
		this.roleId = jdata.optString("roleId", "");
		// 订单场景 int N
		// 0或无此字段：SDK方式1：WAP方式2：WEB方式仅接入了反充的游戏有此字段，如游戏同时使用反充和sdk支付，需兼容此字段有值和无值的情形。
		this.intfType = jdata.optString("intfType", "0");
		
		this.sign = json.optString("sign");
		
		logger.info("【{}充值】生成callback {}",ch.getPromoID(), this);
		
		if ("S".equalsIgnoreCase(orderStatus)) {// 支付成功
			// 本地生成sign用于对比
			// sign的签名规则：
			// MD5(cpId+amount=...+callbackInfo=...+failedDesc=...+gameId=...
			// +orderId=...+orderStatus=...+payWay=...+serverId=...+ucid=...+apiK
			// ey)（去掉+；替换...为实际值）
			StringBuilder buf = new StringBuilder();
			buf.append(ch.getCpId());
			buf.append("amount=").append(amount);
			buf.append("callbackInfo=").append(callbackInfo);
			buf.append("failedDesc=").append(failedDesc);
			buf.append("gameId=").append(gameId);
			buf.append("orderId=").append(orderId);
			buf.append("orderStatus=").append(orderStatus);
			buf.append("payWay=").append(payWay);
			buf.append("serverId=").append(serverId);
			buf.append("ucid=").append(ucid);
			buf.append(ch.getApiKey());
			if (PromoSupport.getInstance().isDebugPayNoSign()||MD5.MD5Encode(buf.toString()).equalsIgnoreCase(sign)) {// 签名验证通过

				// 生成订单
				PayExtParam pext = new PayExtParam(callbackInfo);
				String otherinfo = "roleId=" + roleId + ",intfType=" + intfType;
				String paytime = DateUtil.formatReadability(new Date(System.currentTimeMillis()));
				payOrder = new PayOrder(pext, orderId, String.valueOf(Float
							.parseFloat(amount) * 100), ucid, payWay, paytime,
							otherinfo);
				logger.info("【{}充值】生成订单  {}",ch.getPromoID(), payOrder);

//				// 通知GS发货
//				PS2GS p2g = PaymentServer.getIntance().getPS2GS(pext.getGsID());
//				if (p2g != null) {
//					p2g.sendPayOrder2GS(payOrder);
//				} else {
//					logger.warn("【{}充值】回调找不到PS2GS(gsid={}),{}",ch.getPromoID(), pext.getGsID(),
//							this);
//				}
				return "SUCCESS";
			} else {
				logger.warn("【{}充值】sign验证失败 {}",ch.getPromoID(), this);
//				throw new PayCallbackException(ExCode.SIGNVERIFY_FAILED,
//						StringUtil.format("【UC充值】sign验证不通过 {}", this));
			}
		} else {
			// 支付失败
			logger.warn("【{}充值】失败({}),{}",ch.getPromoID(), failedDesc, this);
//			throw new PayCallbackException(ExCode.SDK_RESULTPARAM_FAILED,
//					StringUtil.format("【UC充值】失败({}),{}", failedDesc, this));
		}
		}catch(Exception e){
			return "FAILURE";
		}
		// SUCCESS 成功，表示游戏服务器成功接收了该次充值结果通知，对于充值结果为失败的，只要能成功接收，也应返回SUCCESS。
		// FAILURE 失败，表示游戏服务器无法接收或识别该次充值结果通知，如：签名检验不正确、游戏服务器接收失败
		return "FAILURE";
	}

	@Override
	public PayOrder getGeneratedPayOrder() {
		return payOrder;
	}
	
	@Override
	public String responseOfRepeatCallback() {
		return "SUCCESS";
	}

	@Override
	public String toString() {
		return "UCPayCallback [orderId=" + orderId + ", gameId=" + gameId
				+ ", serverId=" + serverId + ", ucid=" + ucid + ", payWay="
				+ payWay + ", amount=" + amount + ", callbackInfo="
				+ callbackInfo + ", orderStatus=" + orderStatus
				+ ", failedDesc=" + failedDesc + ", roleId=" + roleId
				+ ", intfType=" + intfType + ", sign=" + sign + "]";
	}

}
