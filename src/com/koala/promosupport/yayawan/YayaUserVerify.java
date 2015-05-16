package com.koala.promosupport.yayawan;

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
 * 用户信息接口描述如下： 
 * 
 *   URL 
 *     http://passport.yayawan.com/oauth/userinfo
 * 
 *   返回格式 
 *     JSON 
 * 
 *   HTTP 请求方式 
 *     POST 
 * 
 *   请求参数 
 *      参数  是否必需 类型                  说明 
 *      app_id 是          int         yayaywan_game_id 由 yayawan 生成。 
 *      uid    是          bigint       用户注册或登录后返回的uid 参数。注：uid  为 yayawan 用户唯一不变标识，强烈建议用于绑定用户游戏色，
 * 					以避免当用户其他属性变更时，出现角色不符的  情况。 
 *      token  是          string     用户注册或登录后返回的token 参数。 
 *      sign    是          string     由token 与yayawan_game_key拼接，再经MD5 加密后的加密 
 *                                   串。用来保证接口访问者的合法性。 
 *                                   生成公式：sig=MD5(token|yayawan_game_key)      （中间有“|”） 
 * 
 *    成功返回示例： 
 *      { 
 *           "id":346456457473234534, 
 *           "username":"yayawan", 
 *           "reg_time":2012-03-04 11:11:11, 
 *           "token":"F9A0F6A0E0D4564F56C483165A607735FA4F324", 
 *           "error_code":0 
 *       } 
 * 
 * 字段说明 
 *      字段名         字段类型 字段说明 
 *      id    bigint       乐乐号 
 *      username    string     用户名
 *      reg_time     string   注册时间 （since：1970-1-1 00:00:00） 
 *      token       string    接口访问令牌，原样返回。 
 *      error_code  int       错误码 
 *      error_msg   string    错误描述
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class YayaUserVerify implements IUserVerify {

	class Response {
		String id;// "id":346456457473234534,
		String username;// "username":"yayawan",
		String reg_time;// "reg_time":2012-03-04 11:11:11,
		String token;// "token":"F9A0F6A0E0D4564F56C483165A607735FA4F324",
		String error_code;// "error_code":0
		String error_msg;
	}

	private YayaChannel ch;

	public YayaUserVerify(YayaChannel ch) {
		this.ch = ch;
	}

	@Override
	public void request(final KGamePlayerSession playersession,
			final KGameMessage pverifyResp, final int promoID,
			final Map<String, String> params, final Map<String, String> analysisInfoNeedSaveToDB) throws Exception {
		final String uid = params.get(PROMO_KEY_DUOKOO_UID);
		final String token = params.get(PROMO_KEY_DOWNJOY_TOKEN);
		if (uid == null || uid.length() <= 0 || token == null
				|| token.length() <= 0) {
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
					sb.append("app_id=").append(ch.getYayaywan_game_id());
					sb.append("&uid=").append(uid);
					sb.append("&token=").append(token);
					// 生成公式：sig=MD5(token|yayawan_game_key) （中间有“|”）
					sb.append("&sign=").append(
							MD5.MD5Encode(
									(new StringBuilder()).append(token)
											.append("|")
											.append(ch.getYayawan_game_key())
											.toString()).toLowerCase());
					// 发起HTTP连接
					String respstring = PromoSupport
							.getInstance()
							.getHttp()
							.request(ch.getUrlUserVerify(), sb.toString(),
									ch.getMethodUserVerify());

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

					// {
					// "id":346456457473234534,
					// "username":"yayawan",
					// "reg_time":2012-03-04 11:11:11,
					// "token":"F9A0F6A0E0D4564F56C483165A607735FA4F324",
					// "error_code":0
					// }
					JSONObject jobj = new JSONObject(respstring);
					Response re = new Response();
					re.id = jobj.optString("id");
					re.username = jobj.optString("username");
					re.reg_time = jobj.optString("reg_time");
					re.token = jobj.optString("token");
					re.error_code = jobj.optString("error_code");
					re.error_msg = jobj.optString("error_msg");

					// 根据error_code值来判断，若为0，则有效
					if (!"0".equals(re.error_code)) {
						// 与第三方服务器验证失败
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp.writeUtf8String(KGameTips
								.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
										promoID, re.error_code));
						playersession.send(pverifyResp);
						return;
					}
					String promoMask = uid;

					// 返回信息中添加参数（直接重用上面的Map对象）
					params.clear();
					params.put(KGameProtocol.PROMO_KEY_DUOKOO_UID, promoMask);

					PromoSupport.getInstance().afterUserVerified(playersession,
							promoID, ch.getPromoID(), promoMask, re.username,
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
