package com.koala.promosupport.zhishanghudong;

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
 * SDK接受请求地址：http://119.147.224.162:8081/loginvalid.php
 * 请求方式：POST/GET
 * 请求参数：
 * “accountid“：”<用户账号ID>”         (由SDk客户端获取)
 * “sessionid“：”<用户登录后的sessionid>” (由SDk客户端获取)
 * “gameid”: “<游戏ID(由游戏接入时给定)>”
 * “sign”: “<签名信息>”
 * 请求响应：text/json
 * 成功：{"code":”0”,"msg":"成功"}
 * 失败：{"code":”<错误码>”,"msg":"<说明信息>"}
 * 失败code说明：
 * Code=81(账号ID错误)，
 * code=82(游戏ID错误)，
 * code=83(游戏ID未找到)，
 * code=84(您还未登录,请先登录再试)
 * code=85(您的账号在其他地方登陆,请重新登陆后再试)
 * 
 * 请求示例:
 * http://119.147.224.162:8081/loginvalid.php?accountid=10&gameid=100123&sessionid=aab49ef0-1486-fa67-17cc-9c4872293863&sign=5a33e241dfd3549ff29659c3e0812fe0
 * Sign生成规则:
 * 1.将除sign以外的参数按键进行自然排序
 * $params = array(
 * “accountid”=>”10”,
 * “gameid”=>”100123”,
 * “sessionid”=>”aab49ef0-1486-fa67-17cc-9c4872293863”,
 * );
 * 2.参数键值对使用k1=v1&k2=v2...的方式拼接
 * 如:accountid=10&gameid=100123&sessionid=aab49ef0-1486-fa67-17cc-9c4872293863
 * 3.将sdk平台分配的appkey追加到字符串结尾进行md5运算
 * 如:Md5( accountid=10&gameid=100123&sessionid=aab49ef0-1486-fa67-17cc-9c487229386312fhd5748sayuh48)
 * 
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class AGameUserVerify implements IUserVerify {

	private AGameChannel ch;

	public AGameUserVerify(AGameChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public void request(final KGamePlayerSession playersession,
			final KGameMessage pverifyResp, final int promoID,
			final Map<String, String> params, final Map<String, String> analysisInfoNeedSaveToDB) throws Exception {

		final String accountid = params.get(PROMO_KEY_ACCOUNTID);
		final String sessionid = params.get(PROMO_KEY_SESSIONID);
		if (accountid == null || accountid.length() <= 0 || sessionid == null
				|| sessionid.length() <= 0) {
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
					// * “accountid“：”<用户账号ID>” (由SDk客户端获取)
					// * “sessionid“：”<用户登录后的sessionid>” (由SDk客户端获取)
					// * “gameid”: “<游戏ID(由游戏接入时给定)>”
					// * “sign”: “<签名信息>”
					StringBuilder sb = new StringBuilder();
					sb.append("accountid=").append(accountid);
					sb.append("&sessionid=").append(sessionid);
					sb.append("&gameid=").append(ch.getGameid());
					sb.append("&sign=").append(
							MD5.MD5Encode(
									(new StringBuilder().append("accountid=")
											.append(accountid)
											.append("&gameid=")
											.append(ch.getGameid())
											.append("&sessionid=")
											.append(sessionid).append(ch
											.getGamekey())).toString())
									/*.toUpperCase()*/);

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
					// * 请求响应：text/json
					// * 成功：{"code":”0”,"msg":"成功"}
					// * 失败：{"code":”<错误码>”,"msg":"<说明信息>"}
					// * 失败code说明：
					// * Code=81(账号ID错误)，
					// * code=82(游戏ID错误)，
					// * code=83(游戏ID未找到)，
					// * code=84(您还未登录,请先登录再试)
					// * code=85(您的账号在其他地方登陆,请重新登陆后再试)
					JSONObject jobj = new JSONObject(respstring);
					String code = jobj.optString("code");
					String msg = jobj.optString("msg");

					if (!"0".equalsIgnoreCase(code)) {
						// 与第三方服务器验证失败
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp.writeUtf8String(KGameTips
								.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
										promoID, msg));
						playersession.send(pverifyResp);
						return;
					}
					String promoMask = String.valueOf(accountid);

					// 返回信息中添加参数（直接重用上面的Map对象）
					params.clear();
					params.put(KGameProtocol.PROMO_KEY_ACCOUNTID, promoMask);

					PromoSupport.getInstance().afterUserVerified(playersession,
							promoID, ch.getPromoID(), promoMask, promoMask,
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
