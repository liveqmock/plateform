package com.koala.promosupport._5gwan;

import java.util.Map;

import com.koala.game.KGameMessage;
import com.koala.game.KGameProtocol;
import com.koala.game.player.KGamePlayerSession;
import com.koala.game.tips.KGameTips;
import com.koala.promosupport.IUserVerify;
import com.koala.promosupport.MD5;
import com.koala.promosupport.PromoSupport;
import com.koala.thirdpart.json.JSONObject;

/**
 * <pre>
 * 2、用户信息验证
 * 用户信息验证接口提供检验用户是否合法，该接口在游戏方需要验证时候调用。
 * A、接口地址：http://app.5gwan.com:9000/user/info.php
 * B、接口参数：
 * 参数		描述
 * app_id	应用id
 * token	通讯令牌，由客户端登录成功传回
 * sign	签名，由客户端登录成功传回
 * C、返回格式：
 * json
 * D、实例：
 * http://app.5gwan.com:9000/user/info.php?sign=e7dd6bb3f0845f5f416a767524dc83c0&token=b3066822e277f30638966f3e23719de2&app_id=a001
 * 返回：
 * {"state":"1","data":{"userid":"2","username":"tttttt"}}	
 * state	描述
 * 1	正确
 * 0	失败
 * 
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class _5GwanUserVerify implements IUserVerify {

	private _5GwanChannel ch;

	public _5GwanUserVerify(_5GwanChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public void request(final KGamePlayerSession playersession,
			final KGameMessage pverifyResp, final int promoID,
			final Map<String, String> params, final Map<String, String> analysisInfoNeedSaveToDB) throws Exception {

		final String token = params.get(PROMO_KEY_TOKEN);
		//final String sign = params.get("sign");
		final String appid = params.get("appid");//20131122改成由客户端发过来
		
		//20140120根据5G玩要求》》》》》》》》》》》》》》》》》》》》》》》》》》》》》》》》》》》
		//(1.5之前版本用户登录验证接口的sign参数由客户端传回，1.5版本以后改由游戏服务器计算生成sign，请各合作伙伴在服务端用户验证接口上做兼容新旧版本判断，
		//如旧版本客户端传回sign则保持原有处理方法，新版本客户端不传回sign则按服务端接口文档中的算法计算出sign。（其他验证流程不变）)
		//Sign 计算方法：Sign = MD5(MD5(Appkey+"_"+Token));
		String sign_c = params.get("sign");
		if(sign_c == null || sign_c.length() <= 0){
			//客户端没有的时候就服务器自己生成
			sign_c = MD5.MD5Encode(MD5.MD5Encode(ch.getApp_key()+"_"+token));
		}
		final String sign = sign_c;
		//要做兼容《《《《《《《《《《《《《《《《《《《《《《《《《《《《《《《《《《《《《《《《《《《《
		
		if (token == null || token.length() <= 0 ) {
			// 格式错误
			pverifyResp
					.writeInt(PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG);
			pverifyResp
					.writeUtf8String(KGameTips
							.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG",
									promoID,
									PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG));
			playersession.send(pverifyResp);
			return;
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// * app_id 应用id
					// * token 通讯令牌，由客户端登录成功传回
					// * sign 签名，由客户端登录成功传回
					StringBuilder sb = new StringBuilder();
					sb.append("app_id=").append(/*ch.getApp_id()*/appid);
					sb.append("&token=").append(token);
					sb.append("&sign=").append(sign);

					// 发起HTTP连接
					String respstring = PromoSupport
							.getInstance()
							.getHttp()
							.request(ch.getUserVerifyUrl(), sb.toString(),
									ch.getUserVerifyHttpMethod());

					// 分析返回结果
					if (respstring == null || respstring.length() <= 0) {
						// 渠道服务器无返回
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp.writeUtf8String(KGameTips
								.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
										promoID, 167167));
						playersession.send(pverifyResp);
						return;
					}
					// {"state":"1","data":{"userid":"2","username":"tttttt"}}
					JSONObject jobj = new JSONObject(respstring);
					String state = jobj.optString("state");
					if (!"1".equals(state)) {
						// 与第三方服务器验证失败
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp.writeUtf8String(KGameTips
								.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
										promoID, respstring));
						playersession.send(pverifyResp);
						return;
					}
					String data = jobj.optString("data");
					JSONObject jdata = new JSONObject(data);
					String userid = jdata.optString("userid");
					String username = jdata.optString("username");

					String promoMask = username;

					// 返回信息中添加参数（直接重用上面的Map对象）
					params.clear();
					params.put(KGameProtocol.PROMO_KEY_UID, promoMask);
					params.put("username", username);

					PromoSupport.getInstance().afterUserVerified(playersession,
							promoID, ch.getPromoID(), promoMask, userid,
							pverifyResp, params, analysisInfoNeedSaveToDB);
				} catch (Exception e) {
					e.printStackTrace();
					// 其它失败情况
					pverifyResp
							.writeInt(KGameProtocol.PL_UNKNOWNEXCEPTION_OR_SERVERBUSY);
					pverifyResp.writeUtf8String(KGameTips
							.get("PL_UNKNOWNEXCEPTION_OR_SERVERBUSY"));
					playersession.send(pverifyResp);
				}
			}
		}).start();
	}

}
