package com.koala.promosupport.downjoy;

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
 * 我方服务器与当乐服务器做一次用户的验证
 * 
 * <pre>
 * 参数  是否必需  类型  说明 
 * app_id  是  int  接入时由当乐分配的游戏/应用ID。 
 * mid  是  long  用户注册或登录后返回的mid参数。注：mid为当乐用户唯一不变标识，强烈建议用于绑定用户游戏角色，以避免当用户其他属性变更时，出现角色不符的情况。 
 * token  是  string  用户注册或登录后返回的token参数。
 * sig  是  string  由token 与app_key拼接，再经MD5加密后的加密串。用来保证接口访问者的合法性。 生成公式：sig=MD5(token|app_key) （中间有“|”）
 * 
 * 
 * 成功返回示例：
 * { 
 * 	  "memberId":32608510, 
 * 	  "username":"ym1988ym", 
 * 	  "nickname":"当乐_小牧", 
 * 	  "gender":"男", 
 * 	  "level":11, 
 * 	  "avatar_url":"http://d.cn/images/item/35/002.gif", 
 * 	  "created_date":1346140985873, 
 * 	  "token":"F9A0F6A0E0D4564F56C483165A607735FA4F324", 
 * 	  "error_code":0
 * }
 * 错误返回示例：
 * {
 *    "error_code":211, 
 *    "error_msg":"app_key错误"
 * }
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class DownjoyUserVerify implements IUserVerify {

	class Response {
		long memberId;// 乐乐号
		String username;// 用户名
		String nickname;// 昵称
		String gender;// 性别
		int level;// 等级
		String avatar_url;// 头像图片地址
		long created_date;// 注册时间毫秒值（since：1970-1-1 00:00:00）
		String token;// 接口访问令牌，原样返回。
		int error_code = -1;// 错误码 （错误码对应信息可查看1.4 errorCode状态码说明）
		String error_msg;// 错误描述
	}

	private DownjoyChannel ch;

	public DownjoyUserVerify(DownjoyChannel ch) {
		this.ch = ch;
	}

	@Override
	public void request(final KGamePlayerSession playersession,
			final KGameMessage pverifyResp, final int promoID,
			final Map<String, String> params, final Map<String, String> analysisInfoNeedSaveToDB) throws Exception {

		final String mid = params.get(PROMO_KEY_DOWNJOY_MID);
		final String token = params.get(PROMO_KEY_DOWNJOY_TOKEN);
		if ((mid == null || mid.length() <= 0)
				| (token == null || token.length() <= 0)) {
			// 格式错误
			pverifyResp
					.writeInt(PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG);
			pverifyResp.writeUtf8String(KGameTips.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG",
							promoID,PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG));
			playersession.send(pverifyResp);
			return;
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					StringBuilder sb = new StringBuilder();
					sb.append("app_id=").append(ch.getAppID());
					sb.append("&mid=").append(mid);
					sb.append("&token=").append(token);
					sb.append("&sig=").append(
							MD5.MD5Encode(token + "|" + ch.getAppKey()));
					// 发起HTTP连接
					String respstring = PromoSupport
							.getInstance()
							.getHttp()
							.request(ch.getUrlLoginVerify(), sb.toString(),
									ch.getHttpMethod());

					// 分析返回结果
					if (respstring == null || respstring.length() <= 0) {
						// 渠道服务器无返回
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp.writeUtf8String(KGameTips
								.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
										promoID,
										167167));
						playersession.send(pverifyResp);
						return;
					}
					JSONObject jobj = new JSONObject(respstring);
					Response re = new Response();
					re.memberId = jobj.optLong("memberId"); // 乐乐号
					re.username = jobj.optString("username"); // 用户名
					re.nickname = jobj.optString("nickname"); // 昵称
					re.gender = jobj.optString("gender"); // 性别
					re.level = jobj.optInt("level"); // 等级
					re.avatar_url = jobj.optString("avatar_url"); // 头像图片地址
					re.created_date = jobj.optLong("created_date"); // 注册时间毫秒值（since：1970-1-1
																	// 00:00:00）
					re.token = jobj.optString("token"); // 接口访问令牌，原样返回。
					re.error_code = jobj.optInt("error_code"); // 错误码
																// （错误码对应信息可查看1.4
																// errorCode状态码说明）
					if (re.error_code != 0) {
						re.error_msg = jobj.optString("error_msg"); // 错误描述
					}

					// 通过返回值中调用{@link DownjoyUserVerify#getError_code()}==0
					// 判断是否成功。
					if (re.error_code != 0) {
						// 与第三方服务器验证失败
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp.writeUtf8String(KGameTips
								.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
										promoID,
										re.error_code));
						playersession.send(pverifyResp);
						return;
					}
					String promoMask = mid;
					
					//返回信息中添加参数（直接重用上面的Map对象）
					params.clear();
					params.put(KGameProtocol.PROMO_KEY_DOWNJOY_MID, promoMask);
					
					PromoSupport.getInstance().afterUserVerified(
							playersession, promoID, ch.getPromoID(), promoMask,
							re.username, pverifyResp,params,analysisInfoNeedSaveToDB);
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
