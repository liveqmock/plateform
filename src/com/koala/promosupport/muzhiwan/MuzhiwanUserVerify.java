package com.koala.promosupport.muzhiwan;

import java.util.Map;

import com.koala.game.KGameMessage;
import com.koala.game.KGameProtocol;
import com.koala.game.player.KGamePlayerSession;
import com.koala.game.tips.KGameTips;
import com.koala.promosupport.IUserVerify;
import com.koala.promosupport.PromoSupport;
import com.koala.thirdpart.json.JSONObject;

/**
 * <pre>
 * URL	http://sdk.muzhiwan.com/oauth2/getuser.php
 * 请求方式	GET
 * 请求参数	参数名	类型	参数描述
 * 	token	字符串	拇指玩SDK传递到接入方服务器的校验字符串
 * 	appkey	字符串	接入方应用唯一标识 
 * 返回数据	返回数据格式为：
 * 
 * {
 * “code”:1,
 * “msg”:””,
 * ”user”:
 * {
 *   “username”:””,
 *   “uid”:””,
 *   “sex”:0,
 *   “mail”:””,
 *   “icon”:””
 * }
 * }
 * 
 * 字段解释：
 * 1、	code：返回码，具体返回码含义请参见登录返回码定义
 * 登录返回码：
 * 1：成功登录
 * -1：用户名格式不正确（注册）
 * -2：用户名或密码错误
 * 1005：密码格式不正确
 * 2001：appkey无效
 * 2010：token无效
 * 2011：token已过期
 * 2、	msg：返回信息，如有错误，则为错误信息
 * 3、	user：用户数据内容
 * 
 * user对象字段解释：
 * 1、	username：用户名
 * 2、	uid：拇指玩用户ID，唯一标记一个用户
 * 3、	sex： 性别，0为女，1为男
 * 4、	mail：用户邮箱
 * 5、	icon：用户头像
 * 
 * 请求样例	http://sdk.muzhiwan.com/oauth2/getuser.php?token=f89a5ab7835ca176&appkey=f8134343ac876
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class MuzhiwanUserVerify implements IUserVerify {

	private MuzhiwanChannel ch;

	public MuzhiwanUserVerify(MuzhiwanChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public void request(final KGamePlayerSession playersession,
			final KGameMessage pverifyResp, final int promoID,
			final Map<String, String> params, final Map<String, String> analysisInfoNeedSaveToDB) throws Exception {

		final String token = params.get(PROMO_KEY_TOKEN);// token
		if (token == null || token.length() <= 0) {
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
					StringBuilder sb = new StringBuilder();
					sb.append("token=").append(token);
					sb.append("&appkey=").append(ch.getAppkey());
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
					// * {
					// * “code”:1,
					// * “msg”:””,
					// * ”user”:
					// * {
					// * “username”:””,
					// * “uid”:””,
					// * “sex”:0,
					// * “mail”:””,
					// * “icon”:””
					// * }
					// * }
					JSONObject jobj = new JSONObject(respstring);
					int code = jobj.optInt("code");
					String msg = jobj.optString("msg");
					if (code != 1) {
						// 与第三方服务器验证失败
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp.writeUtf8String(KGameTips
								.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
										promoID, msg+"(code="+code+")"));
						playersession.send(pverifyResp);
						return;
					}
					JSONObject juser = jobj.optJSONObject("user");
					String username = juser.optString("username");
					String uid = juser.optString("uid");
					int sex = juser.optInt("sex");
					String mail = juser.optString("mail");
					String icon = juser.optString("icon");					
					
					String promoMask = uid;

					// 返回信息中添加参数（直接重用上面的Map对象）
					params.clear();
					params.put(KGameProtocol.PROMO_KEY_UID, promoMask);
					params.put("username", username);

					PromoSupport.getInstance().afterUserVerified(playersession,
							promoID, ch.getPromoID(), promoMask, uid,
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
