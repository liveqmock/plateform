package com.koala.promosupport.cooguo;

import java.net.URLDecoder;
import java.util.Map;

import com.koala.game.logging.KGameLogger;
import com.koala.paymentserver.IPayCallback;
import com.koala.paymentserver.PayExtParam;
import com.koala.paymentserver.PayOrder;
import com.koala.promosupport.MD5;
import com.koala.promosupport.PromoSupport;
import com.koala.promosupport.cooguo.util.MD5_System;

/**
 * <pre>
 * 
 * 参数名  类型  长度  参数说明  签名顺序 
 * openId  String  11   唯一标识，对应 COOGUO用户ID  1 
 * serverId  String  30  充值服务器 ID，接入时应与 COOGUO协商  2 
 * serverName   String  30  充值服务器名字  3 
 * roleId   String  30  游戏角色ID  4 
 * roleName   String  30  游戏角色名称  5 
 * orderId   String  20  COOGUO订单号  6 
 * orderStatus   String  4  订单状态，1-成功；其他为失败  7 
 * payType  String  30  支付类型，如：2_13_支付宝、4_15_银联等  8 
 * amount   String  10  成功充值金额，单位(分)，实际float 类型  9 
 * remark   String  255  结果说明，描述订单状态  10 
 * callBackInfo  String  255  合作商自定义参数，SDK调用时传入的数据  11  
 * payTime  String  20  玩家充值时间，yyyyMMddHHmmss  12 
 * paySUTime   String  20  玩家充值成功时间，yyyyMMddHHmmss  13 
 * sign   String  32  参数签名（用于验签对比）   
 * app_key  String  32  由COOGUO提供，不通过请求传输  14
 * 
 * -----
 * sign = MD5(“openId=100000& serverId=123& serverName=测试服务器& roleId=147& 
 * roleName= 测试角色& orderId=20121129115114758& orderStatus=1& payType=支付宝& 
 * amount=100.0& remark=&callBackInfo= 自定义数据&payTime=20130101125612&  
 * paySUTime=20130101126001&app_key=1478523698”) 
 *  
 * 注意：验签参数值为urldecode后的内容，如果参数没有数据值，请以“key=”
 * 的形式进行签名，例如：remark=&callBackInfo= 自定义。要注意字段的大小写哦 
 *  
 * appkey 由COOGUO提供，登录、充值使用同一个 appkey
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class CooguoPayCallback implements IPayCallback {

	private static final KGameLogger logger = KGameLogger
			.getLogger(CooguoPayCallback.class);
	private CooguoChannel ch;
	private PayOrder payOrder;
	private static final String SUCCESS = "success";// 接收成功
	private static final String ERRORSIGN = "errorSign";// 签名错误
	private static final String ERROR = "error";// 未知错误

	private String openId       ;//唯一标识，对应 COOGUO用户ID          
	private String serverId     ;//充值服务器 ID，接入时应与 COOGUO协商 
	private String serverName   ;//充值服务器名字                       
	private String roleId       ;//游戏角色ID                           
	private String roleName     ;//游戏角色名称                         
	private String orderId      ;//COOGUO订单号                           
	private String orderStatus  ;//订单状态，1-成功；其他为失败           
	private String payType      ;//支付类型，如：2_13_支付宝、4_15_银联等 
	private String amount       ;//成功充值金额，单位(分)，实际float 类型 
	private String remark       ;//结果说明，描述订单状态                 
	private String callBackInfo ;//合作商自定义参数，SDK调用时传入的数据  
	private String payTime      ;//玩家充值时间，yyyyMMddHHmmss           
	private String paySUTime    ;//玩家充值成功时间，yyyyMMddHHmmss       
	private String sign         ;//参数签名（用于验签对比）     

	public CooguoPayCallback(CooguoChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {

		openId       =  URLDecoder.decode(params.get("openId"),"UTF-8");         
		serverId     =  URLDecoder.decode(params.get("serverId"),"UTF-8");
		serverName   =  URLDecoder.decode(params.get("serverName"),"UTF-8");
		roleId       =  URLDecoder.decode(params.get("roleId"),"UTF-8");
		roleName     =  URLDecoder.decode(params.get("roleName"),"UTF-8");
		orderId      =  URLDecoder.decode(params.get("orderId"),"UTF-8");
		orderStatus  =  URLDecoder.decode(params.get("orderStatus"),"UTF-8");
		payType      =  URLDecoder.decode(params.get("payType"),"UTF-8");
		amount       =  URLDecoder.decode(params.get("amount"),"UTF-8");
		remark       =  URLDecoder.decode(params.get("remark"),"UTF-8");
		callBackInfo =  URLDecoder.decode(params.get("callBackInfo"),"UTF-8");
		payTime      =  URLDecoder.decode(params.get("payTime"),"UTF-8");
		paySUTime    =  URLDecoder.decode(params.get("paySUTime"),"UTF-8");
		sign         =  URLDecoder.decode(params.get("sign"),"UTF-8");
		
		logger.info("【{}充值】生成callback {}", ch.getPromoID(), this);

		if (!"1".equals(orderStatus)) {
			logger.info("【{}充值】SDK服务器返回失败状态.{}", ch.getPromoID(), orderStatus);
			return ERROR;
		}

		// 签名
		if (!PromoSupport.getInstance().isDebugPayNoSign()) {
			// sign = MD5(“openId=100000& serverId=123& serverName=测试服务器&
			// roleId=147&
			// roleName= 测试角色& orderId=20121129115114758& orderStatus=1&
			// payType=支付宝&
			// amount=100.0& remark=&callBackInfo= 自定义数据&payTime=20130101125612&
			// paySUTime=20130101126001&app_key=1478523698”)
			StringBuilder sb = new StringBuilder();
			sb.append("openId=").append(openId      ==null?"":openId      );         
			sb.append("&serverId=").append(serverId    ==null?"":serverId    );
			sb.append("&serverName=").append(serverName  ==null?"":serverName  );
			sb.append("&roleId=").append(roleId      ==null?"":roleId      );
			sb.append("&roleName=").append(roleName    ==null?"":roleName    );
			sb.append("&orderId=").append(orderId     ==null?"":orderId     );
			sb.append("&orderStatus=").append(orderStatus ==null?"":orderStatus );
			sb.append("&payType=").append(payType     ==null?"":payType     );
			sb.append("&amount=").append(amount      ==null?"":amount      );
			sb.append("&remark=").append(remark      ==null?"":remark      );
			sb.append("&callBackInfo=").append(callBackInfo==null?"":callBackInfo);
			sb.append("&payTime=").append(payTime     ==null?"":payTime     );
			sb.append("&paySUTime=").append(paySUTime   ==null?"":paySUTime   );
			sb.append("&app_key=").append(ch.getAppkey());
			
			String gen_sign = MD5_System.getInstance().Md5(sb.toString());
			if (!gen_sign.equalsIgnoreCase(this.sign)) {
				logger.info("【{}充值】签名不一致.", ch.getPromoID());
				return ERRORSIGN;// ?
			}
		}

		PayExtParam pext = new PayExtParam(callBackInfo);
		String paytime = PayOrder.convert2PaytimeFormat(payTime, "yyyyMMddHHmmss");
		this.payOrder = new PayOrder(pext, orderId, amount, openId,
				payType, paytime, remark);

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
		return "CooguoPayCallback [openId=" + openId + ", serverId=" + serverId
				+ ", serverName=" + serverName + ", roleId=" + roleId
				+ ", roleName=" + roleName + ", orderId=" + orderId
				+ ", orderStatus=" + orderStatus + ", payType=" + payType
				+ ", amount=" + amount + ", remark=" + remark
				+ ", callBackInfo=" + callBackInfo + ", payTime=" + payTime
				+ ", paySUTime=" + paySUTime + ", sign=" + sign + "]";
	}


}
