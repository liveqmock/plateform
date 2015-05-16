package com.koala.promosupport.zhidiantong;

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
 * 登陆后获得票据
 * BXGameSDK.defaultSDK().getTicket()
 * 冰雪服务器验证接口：														
 *  http://service.fanjie.com/service?service=user.validate
 * 提交方式: GET 
 * 提交参数说明
 * 参数	数据类型	说明
 * cp_id	Int	合作伙伴编号，CP唯一编号，由冰雪平台提供
 * game_id	Int	游戏编号
 * server_id	Int	服务器编号
 * ticket	String	登录成功后，SDK返回的用户票据
 * sign	String	签名 用于验证是否是合法通知。Upper(MD5(cp_id+game_id+server_id+ticket+cp_key))其中加号(+)为连接字符串，签名中不要。需要将MD5后转为大写。
 * formart	staring	数据返回格式 默认为json 可以为xml
 * 
 * 提交返回参数
 * 
 * 参数	数据类型	说明
 * timestamp	int	Unix时间戳
 * code	int	返回代码 1时为成功
 * msg	string	返回状态说明 
 * data	String	响应数据
 * 		user_id	Int	冰雪用户ID
 * 		nick_name	string	用户昵称
 * 		user_name	string	用户名
 * 		adult	Int	用户成年标识 1=成年 0=未成年
 * 
 * Json 格式
 * {"timestamp":123456790,"code":1,"msg":"SUCCESS","data":{"user_id":"123456","nick_name":"","adult":1}}
 * 
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class BXUserVerify implements IUserVerify {

	private BXChannel ch;

	public BXUserVerify(BXChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public void request(final KGamePlayerSession playersession,
			final KGameMessage pverifyResp, final int promoID,
			final Map<String, String> params, final Map<String, String> analysisInfoNeedSaveToDB) throws Exception {

		final String ticket = params.get(PROMO_KEY_TICKET);
		if (ticket == null || ticket.length() <= 0) {
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
					// * cp_id Int 合作伙伴编号，CP唯一编号，由冰雪平台提供
					// * game_id Int 游戏编号
					// * server_id Int 服务器编号
					// * ticket String 登录成功后，SDK返回的用户票据
					// * sign String 签名
					// 用于验证是否是合法通知。Upper(MD5(cp_id+game_id+server_id+ticket+cp_key))其中加号(+)为连接字符串，签名中不要。需要将MD5后转为大写。
					// * formart staring 数据返回格式 默认为json 可以为xml
					StringBuilder sb = new StringBuilder();
					sb.append("cp_id=").append(ch.getCp_id());
					sb.append("&game_id=").append(ch.getGame_id());
					sb.append("&server_id=").append(ch.getServer_id());
					sb.append("&ticket=").append(ticket);
					sb.append("&sign=").append(
							MD5.MD5Encode(
									(new StringBuilder().append(ch.getCp_id())
											.append(ch.getGame_id())
											.append(ch.getServer_id())
											.append(ticket).append(ch
											.getCp_key())).toString())
									.toUpperCase());
					sb.append("&formart=").append("json");

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
					// * timestamp int Unix时间戳
					// * code int 返回代码 1时为成功
					// * msg string 返回状态说明
					// * data String 响应数据
					// * user_id Int 冰雪用户ID
					// * nick_name string 用户昵称
					// * user_name string 用户名
					// * adult Int 用户成年标识 1=成年 0=未成年
					JSONObject jobj = new JSONObject(respstring);
					int timestamp = jobj.optInt("timestamp");
					int code = jobj.optInt("code");
					String msg = jobj.optString("msg");
					String data = jobj.optString("data");
					JSONObject jdata = new JSONObject(data);
					int user_id = jdata.optInt("user_id");
					String nick_name = jdata.optString("nick_name");
					String user_name = jdata.optString("user_name");
					int adult = jdata.optInt("adult");

					if (code != 1) {
						// 与第三方服务器验证失败
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp.writeUtf8String(KGameTips
								.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
										promoID, msg));
						playersession.send(pverifyResp);
						return;
					}
					String promoMask = String.valueOf(user_id);

					// 返回信息中添加参数（直接重用上面的Map对象）
					params.clear();
					params.put(KGameProtocol.PROMO_KEY_UID, promoMask);
					params.put("user_name", user_name);
					params.put("nick_name", nick_name);

					PromoSupport.getInstance().afterUserVerified(playersession,
							promoID, ch.getPromoID(), promoMask, user_name,
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
