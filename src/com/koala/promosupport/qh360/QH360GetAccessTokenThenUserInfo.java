package com.koala.promosupport.qh360;

import java.util.Map;

import com.koala.game.KGameMessage;
import com.koala.game.KGameProtocol;
import com.koala.game.player.KGamePlayerSession;
import com.koala.game.tips.KGameTips;
import com.koala.promosupport.IUserVerify;
import com.koala.promosupport.PromoSupport;
import com.koala.thirdpart.json.JSONObject;

/**
 * <b>第一步：</b>获取access token–服务器端接口, 应用服务器调用 应用从360SDK登录接口的回调中获得authorization
 * code，需要换为access token。应用把 authorization code发给应用服务器,
 * 由应用服务器向360服务器端的/oauth2/access_token接 口请求获取access token.
 * 
 * 注意, 不要从客户端发起请求, 否则app_secret会因此泄漏.
 * 
 * <pre>
 * 【请求参数】：
 *     grant_type  Y  定值 authorization_code 
 *     code  Y  通过360SDK获取的authorization code值，只能使用一次，有效期60秒，相同的参数,  请求一次就失效. 
 *     client_id  Y  app key 
 *     client_secret  Y  app secret 
 *     redirect_uri  Y  定值 oob
 *     
 *     例：
 *  https://openapi.360.cn/oauth2/access_token?grant_type=authorization_code&code=120653f48687
 * 763d6ddc486fdce6b51c383c7ee544e6e5eab&client_id=0fb2676d5007f123756d1c1b4b5968bc&cli
 * ent_secret=1234567890ab18384f562d7d3f.....&redirect_uri=oob 
 *  
 * 【返回参数】：
 *     access_token  Y  Access Token值 
 *     expires_in  Y  Access Token的有效期 以秒计 
 *     refresh_token  Y  用于刷新Access Token的Token,  有效期14天 
 *     scope Y  Access Token最终的访问范围，即用户实际授予的权限列表当前只支持值basic
 *     
 *  例：
 * {    
 *   "access_token":"120652e586871bb6bbcd1c7b77818fb9c95d92f9e0b735873", 
 *   "expires_in":"36000”, 
 *   “scope”:”basic”, 
 *   “refresh_token”:”12065961868762ec8ab911a3089a7ebdf11f8264d5836fd41” 
 * }
 * 
 * 
 * 
 * <b>第二步：</b>应用获取access token后, 可调用360开放平台服务器端接口/user/me, 获取用户信息. 应用可以 在服务器端或客户端来调用这个接口.
 * 但建议从服务器端发起请求, 避免将access token存在客户端.<br>
 * 获取用户信息后，应用需要保存自身账号与360账号的绑定关系。并且妥善保存用户信息留待以后 使用. 例如支付时，就需要使用360用户id.
 * 
 * 参数说明： 
 * 参数  必选  参数说明 
 * access_token  Y  授权的access token 
 * fields N  允许应用自定义返回字段，多个属性之间用英文半角逗号作为分隔符。不传递此参数则缺省返回id,name,avatar
 * 
 * 请求示例： https://openapi.360.cn/user/me.json?access_token=12345678983b38aabcdef387453ac8133ac3263987654321&fields=id,name,avatar,sex,area
 * -----------------------------
 * 返回参数： 
 * 参数  必选  参数说明 
 * id  Y  360用户ID,  缺省返回 
 * name  Y  360用户名, 缺省返回 
 * avatar  Y  360用户头像,  缺省返回 
 * sex N  360用户性别，仅在fields中包含时候才返回,返回值为：男，女或者未知 
 * area  N  360用户地区，仅在fields中包含时候才返回 
 * nick  N  用户昵称，无值时候返回空
 * 
 * 返回示例：
 * { 
 * "id": "201459001", 
 * "name": "360U201459001", 
 * "avatar": "http://u1.qhimg.com/qhimg/quc/48_48/22/02/55/220255dq9816.3eceac.jpg?f=d140ae40ee93e8b08ed6e9c53543903b", 
 * "sex": "未知" 
 * "area": "" 
 * }
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class QH360GetAccessTokenThenUserInfo implements IUserVerify {

	class ResponseGetAccessToken {
		String access_token;
		int expires_in;
		String scope;
		String refresh_token;
	}

	class ResponseGetUserInfo {
		String id;
		String name;
		String avatar;
		// String sex;
		// String area;
		// String nick;
	}

	private QH360Channel ch;

	public QH360GetAccessTokenThenUserInfo(QH360Channel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public void request(final KGamePlayerSession playersession,
			final KGameMessage pverifyResp, final int promoID,
			final Map<String, String> params, final Map<String, String> analysisInfoNeedSaveToDB) throws Exception {

		final String authcode = params.get(PROMO_KEY_360_AUTHCODE);
		if (authcode == null || authcode.length() <= 0) {
			// 格式错误
			pverifyResp
					.writeInt(PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG);
			pverifyResp.writeUtf8String(KGameTips.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG",
					promoID,PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG));
			playersession.send(pverifyResp);
			return;
		}

		// 开线程进行Http请求
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					StringBuilder sb = new StringBuilder();
					sb.append("grant_type=").append("authorization_code");
					sb.append("&");
					sb.append("code=").append(authcode);
					sb.append("&");
					sb.append("client_id=").append(ch.getAppKey());
					sb.append("&");
					sb.append("client_secret=").append(ch.getAppSecret());
					sb.append("&");
					sb.append("redirect_uri=").append("oob");

					// 发起HTTP连接
					String respstring = PromoSupport
							.getInstance()
							.getHttp()
							.request(ch.getUrlGetAccessToken(), sb.toString(),
									ch.getHttpMethod());

					// 分析返回结果
					if (respstring == null || respstring.length() <= 0) {
						// 渠道服务器无返回
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp
								.writeUtf8String(KGameTips
										.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
												promoID,
												167167));
						playersession.send(pverifyResp);
						return;
					}

					JSONObject obj = new JSONObject(respstring);
					ResponseGetAccessToken re = new ResponseGetAccessToken();
					re.access_token = obj.optString("access_token");
					re.expires_in = obj.optInt("expires_in");
					re.scope = obj.optString("scope");
					re.refresh_token = obj.optString("refresh_token");

					// 通过返回值中调用{@link DownjoyUserVerify#getError_code()}==0
					// 判断是否成功。
					if (re.access_token == null
							|| re.access_token.length() <= 0) {
						// 与第三方服务器验证失败
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp
								.writeUtf8String(KGameTips
										.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
												promoID,
												6767));
						playersession.send(pverifyResp);
						return;
					}

					// //////////////////////////////////////////////////
					// 接下来再做一次HTTP连接向SDK服务器拿用户信息
					String respstring2 = PromoSupport
							.getInstance()
							.getHttp()
							.request(ch.getUrlGetUserInfo(),
									"access_token=" + re.access_token,
									ch.getHttpMethod());
					// 分析返回结果
					if (respstring2 == null || respstring2.length() <= 0) {
						// 渠道服务器无返回
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp
								.writeUtf8String(KGameTips
										.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
												promoID,
												169169));
						playersession.send(pverifyResp);
						return;
					}

					JSONObject obj2 = new JSONObject(respstring2);
					ResponseGetUserInfo re2 = new ResponseGetUserInfo();
					re2.id = obj2.optString("id");
					re2.name = obj2.optString("name");
					re2.avatar = obj2.optString("avatar");

					String promoMask = re2.id;
					
					//返回信息中添加参数（直接重用上面的Map对象）
					params.clear();
					params.put(KGameProtocol.PROMO_KEY_360_USERID, promoMask);
					params.put(KGameProtocol.PROMO_KEY_360_ACCESSTOKEN, re.access_token);
					
					PromoSupport.getInstance().afterUserVerified(
							playersession, promoID, ch.getPromoID(), promoMask,
							re2.name, pverifyResp, params, analysisInfoNeedSaveToDB);

				} catch (Exception e) {
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
