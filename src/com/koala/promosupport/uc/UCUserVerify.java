package com.koala.promosupport.uc;

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
 * 例子
 * 
 * <pre>
 * 返回内容（json格式）：
 * {
 * "id":1330395827,
 * "state":{"code":1,"msg":"会话刷新成功"},
 * "data":{"ucid":123456,"nickName":"张三"}
 * }
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class UCUserVerify implements IUserVerify {

	class Response {
		long id;
		int state_code;
		String state_msg;
		int data_ucid;
		String data_nickName;
	}

	private UCChannel ch;

	public UCUserVerify(UCChannel ch) {
		super();
		this.ch = ch;
	}

	/**
	 * <pre>
	 * 例 
	 * ----------------------------------------------------------
	 * HTTP请求的body内容：
	 * 	{
	 * 	"id":1330395827,
	 * 	"service":"ucid.user.sidInfo",
	 * 	"data":{"sid":"abcdefg123456"},
	 * 	"game":{"cpId":100,"gameId":12345,"channelId":"2","serverId":0}
	 * 	,
	 * 	"sign":"6e9c3c1e7d99293dfc0c81442f9a9984"
	 * 	}
	 * 	假定cpId=109，apiKey=202cb962234w4ers2aaa
	 * 	sign的签名规则：MD5(cpId+sid=...+apiKey)（去掉+；替换...为实际值）
	 * -----------------------------------------------------------
	 * 返回内容（json格式）：
	 * {
	 * "id":1330395827,
	 * "state":{"code":1,"msg":"会话刷新成功"},
	 * "data":{"ucid":123456,"nickName":"张三"}
	 * }
	 * </pre>
	 * 
	 * @throws Exception
	 */
	@Override
	public void request(final KGamePlayerSession playersession,
			final KGameMessage pverifyResp, final int promoID,
			final Map<String, String> params, final Map<String, String> analysisInfoNeedSaveToDB) throws Exception {

		final String sid = params.get(PROMO_KEY_UC_SID);
		if (sid == null || sid.length() <= 0) {
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
					long id = System.currentTimeMillis();
					JSONObject obj0 = new JSONObject();
					obj0.put("id", id);
					obj0.put("service", ch.getService_sidinfo());
					obj0.put("data", (new JSONObject()).put("sid", sid));
					JSONObject obj1 = new JSONObject();
					obj1.put("cpId", ch.getCpId());
					obj1.put("gameId", ch.getGameId());
					obj1.put("channelId", ch.getChannelId());
					obj1.put("serverId", ch.getServerId());
					obj0.put("game", obj1);
					obj0.put(
							"sign",
							MD5.MD5Encode(
									ch.getCpId() + "sid=" + sid
											+ ch.getApiKey()).toLowerCase());

					// 发起HTTP连接
					String respstring = PromoSupport
							.getInstance()
							.getHttp()
							.request(ch.getSdkServerURL(), obj0.toString(),
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

					JSONObject rj = new JSONObject(respstring);
					Response re = new Response();
					id = rj.optLong("id");
					Object o1 = rj.opt("state");
					if (o1!=null && (o1 instanceof JSONObject)) {
						JSONObject rj1 = (JSONObject) o1;
						re.state_code = rj1.optInt("code");
						re.state_msg = rj1.optString("msg");
					}
					Object o = rj.opt("data");
					if (o!=null && (o instanceof JSONObject)) {
						JSONObject rj2 = (JSONObject) o;
						re.data_ucid = rj2.optInt("ucid");
						re.data_nickName = rj2.optString("nickName");
					}

					// 通过返回值中判断是否成功。
					if (re.state_code != 1) {
						// 与第三方服务器验证失败
						pverifyResp.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp.writeUtf8String(KGameTips
								.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
										promoID,
										re.state_code));
						playersession.send(pverifyResp);
						return;
					}
					String promoMask = String.valueOf(re.data_ucid);
					
					//返回信息中添加参数（直接重用上面的Map对象）
					params.clear();
					params.put(KGameProtocol.PROMO_KEY_UC_UCID, promoMask);
					
					PromoSupport.getInstance().afterUserVerified(
							playersession, promoID, ch.getPromoID(), promoMask,
							re.data_nickName, pverifyResp, params, analysisInfoNeedSaveToDB);
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
