package com.koala.promosupport.baoruan;

import java.sql.Timestamp;
import java.util.Date;
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
 * appid	string	是	分配的appid	
 * token	string	是	SDK处理的token	
 * login_time	int	是	当前时间戳	
 * is_json	int	是	要求返回 json	1：json，0：xml
 * verifystring	string	是	传送数据验证字串	生成格式见说明
 * verifystring= md5(appid+token+login_time+is_json+uniquekey)
 * 
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class BaoruanUserVerify implements IUserVerify {

	private BaoruanChannel ch;

	public BaoruanUserVerify(BaoruanChannel ch) {
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
					sb.append("appid=").append(ch.getAppid());
					sb.append("&token=").append(token);
					int login_time = (int) (System.currentTimeMillis() / 1000);
					sb.append("&login_time=").append(login_time);
					sb.append("&is_json=").append("1");
					sb.append("&verifystring=").append(
							MD5.MD5Encode(ch.getAppid() + token + login_time
									+ "1" + ch.getUniquekey()));
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
					// {"uid":"158837254514069058","username":"1002077999","gender":"1","avatar":"http:\/\/userapp.leqi.cc\/images\/head\/default.jpg"}
					JSONObject jobj = new JSONObject(respstring);
					String uid = jobj.optString("uid");
					String username = jobj.optString("username");
					String gender = jobj.optString("gender");
					if (uid == null || uid.length() == 0) {
						// 与第三方服务器验证失败
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp.writeUtf8String(KGameTips
								.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
										promoID, respstring));
						playersession.send(pverifyResp);
						return;
					}
					String promoMask = uid;

					// 返回信息中添加参数（直接重用上面的Map对象）
					params.clear();
					params.put(KGameProtocol.PROMO_KEY_UID, promoMask);
					params.put("username", username);
					params.put("gender", gender);

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
