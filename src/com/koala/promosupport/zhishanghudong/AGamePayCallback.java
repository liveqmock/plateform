package com.koala.promosupport.zhishanghudong;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.koala.game.logging.KGameLogger;
import com.koala.paymentserver.IPayCallback;
import com.koala.paymentserver.PayExtParam;
import com.koala.paymentserver.PayOrder;
import com.koala.promosupport.MD5;
import com.koala.promosupport.PromoSupport;

/**
 * <pre>
 * 2.	SDK服务器在成功充值以后主动通知游戏服务器返回用户充值信息API
 * 游戏服务器地址：http://游戏服务器地址/
 * 如:http://www.example.com/pay_notify.php
 * 请求方式：POST
 * 请求参数：
 * “account“:”<用户账号ID>”,
 * “money”:”<充值金额(单位:分)>”,
 * “addtime”:”<创建时间(时间戳)>”,
 * “customorderid”:”<商户订单号,请求充值时传递给sdk的订单号>”,
 * “paytype”:”<充值类型>”,
 * “senddate”:”<发送时间(时间戳)>”,
 * “custominfo”:”<商户自定义信息,请求充值时传递给sdk的信息>”,
 * “success”:”<是否成功:1成功,其他失败>”
 * “sign”:”<签名信息>”
 * 
 * 请求响应：text/html
 * 成功:success
 * 失败:failure
 * Sign生成规则:
 * 1.将除sign以外的参数按键进行自然排序
 * $params = array(
 * 	"account"	=>"10",
 * 	"money"		=>"100",
 * 	"addtime"	=>"13090394",
 * 	"customorderid"=>"10001",
 * 	"paytype"	=>"alipay",
 * 	"senddate"	=>"13090394",
 * 	"custominfo"=>"test",
 * 	"success"	=>"1"
 * );
 * 2.参数键值对使用k1=v1&k2=v2...的方式拼接
 * 如:account=10&addtime=13090394&custominfo=test&customorderid=10001&money=100&paytype=alipay&senddate=13090394&success=1
 * 3.将sdk平台分配的appkey追加到字符串结尾进行md5运算
 * 如:Md5( account=10&addtime=13090394&custominfo=test&customorderid=10001&money=100&paytype=alipay&senddate=13090394&success=112fhd5748sayuh48)
 * 
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class AGamePayCallback implements IPayCallback {

	private static final KGameLogger logger = KGameLogger
			.getLogger(AGamePayCallback.class);
	private AGameChannel ch;
	private PayOrder payOrder;
	private static final String SUCCESS = "success";
	private static final String FAILED = "failure";

	private String account       ;//<用户账号ID>,
	private String money         ;//<充值金额(单位       分)>,
	private String addtime       ;//<创建时间(时间戳)>,
	private String customorderid ;//<商户订单号,请求充值时传递给sdk的订单号>,
	private String paytype       ;//<充值类型>,
	private String senddate      ;//<发送时间(时间戳)>,
	private String custominfo    ;//<商户自定义信息,请求充值时传递给sdk的信息>,
	private String success       ;//<是否成功       1成功,其他失败>
	private String sign          ;//<签名信息>

	public AGamePayCallback(AGameChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {
		this.account      = params.get("account"      );//<用户账号ID>,                                   
		this.money        = params.get("money"        );//<充值金额(单位       分)>,                      
		this.addtime      = params.get("addtime"      );//<创建时间(时间戳)>,                             
		this.customorderid= params.get("customorderid");//<商户订单号,请求充值时传递给sdk的订单号>,       
		this.paytype      = params.get("paytype"      );//<充值类型>,                                     
		this.senddate     = params.get("senddate"     );//<发送时间(时间戳)>,                             
		this.custominfo   = params.get("custominfo"   );//<商户自定义信息,请求充值时传递给sdk的信息>,     
		this.success      = params.get("success"      );//<是否成功       1成功,其他失败>                 
		this.sign         = params.get("sign"         );//<签名信息>                                      

		logger.info("【{}充值】生成callback {}", ch.getPromoID(), this);

		if(!"1".equals(success)){
			logger.info("【{}充值】SDK服务器返回失败状态.{}", ch.getPromoID(),success);
			return FAILED;
		}
		
		// 签名
		if (!PromoSupport.getInstance().isDebugPayNoSign()) {
			List<String> sortkey = new ArrayList<String>();
			for (String string : params.keySet()) {
				if(!"sign".equalsIgnoreCase(string)){
					sortkey.add(string);
				}
			}	
			
			Collections.sort(sortkey);// 根据参数名首字母升序
			StringBuilder sb = new StringBuilder();
			for (String string : sortkey) {
				String v = params.get(string);
				sb.append("&").append(string).append("=").append(v);
			}
			sb.deleteCharAt(0);//删除前面的&
			sb.append(ch.getGamekey());
			logger.info(sb.toString());
			//debug 签名不一致的对比
			//account=10&addtime=13090394&custominfo=test&customorderid=10001&money=100&paytype=alipay&senddate=13090394&success=112fhd5748sayuh48
			//account=914515&addtime=1384188364&custominfo=p1204g223r52&customorderid=P1204R52T20131112004429801N17&money=100&orderid=2013111227684839&paytype=alipay&senddate=1384188364&success=19df4g243fd890kd4
			//account=914515&addtime=1384188364&custominfo=p1204g223r52&customorderid=P1204R52T20131112004429801N17&money=100&paytype=alipay&senddate=1384188364&success=19df4g243fd890kd4
			String gen_sign = MD5.MD5Encode(sb.toString());
			if (!gen_sign.equalsIgnoreCase(this.sign)) {
				logger.info("【{}充值】签名不一致.", ch.getPromoID());
				return FAILED;// ?
			}
		}

		PayExtParam pext = new PayExtParam(custominfo);
		String paytime = PayOrder.FORMAT_PAYTIME.format(new Date(Long.parseLong(addtime)*1000));
		this.payOrder = new PayOrder(pext, customorderid, money, account, paytype,
				paytime, "senddate="+senddate);

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
		return "AGamePayCallback [account=" + account + ", money=" + money
				+ ", addtime=" + addtime + ", customorderid=" + customorderid
				+ ", paytype=" + paytype + ", senddate=" + senddate
				+ ", custominfo=" + custominfo + ", success=" + success
				+ ", sign=" + sign + "]";
	}


}
