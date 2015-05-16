package com.koala.promosupport.lenovo;

import java.io.ByteArrayInputStream;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;

import com.koala.game.KGameMessage;
import com.koala.game.KGameProtocol;
import com.koala.game.logging.KGameLogger;
import com.koala.game.player.KGamePlayerSession;
import com.koala.game.tips.KGameTips;
import com.koala.game.util.XmlUtil;
import com.koala.promosupport.IUserVerify;
import com.koala.promosupport.PromoSupport;

public class LenovoUserVerify implements IUserVerify {

	private LenovoChannel ch;
	private static final KGameLogger logger = KGameLogger.getLogger(LenovoUserVerify.class);
	
	private static final String TAG_ERROR ="Error";
	private static final String TAG_IDENTITYINFO="IdentityInfo";
	

	public LenovoUserVerify(LenovoChannel ch) {
		this.ch = ch;
	}
	
	class Response{
		int status;//0-失败；1-成功
		
		String ErrorCode;//status==0
		
		String AccountID;//对于用户帐号，该字段为用户ID。对于PID帐号，该字段为PID值。
		String Username ;//用户名（可选项）
		String DeviceID ;//登录所用设备ID（可选项）
		String verified ;//用户名是否已验证。0:未验证，1：已验证。
	}

	@Override
	public void request(final KGamePlayerSession playersession,
			final KGameMessage pverifyResp, final int promoID,
			final Map<String, String> params, final Map<String, String> analysisInfoNeedSaveToDB) throws Exception {
		final String lpsust = params.get(PROMO_KEY_LENOVO_LPSUST);
		if (lpsust == null || lpsust.length() <= 0) {
			// 格式错误
			pverifyResp.writeInt(PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG);
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
					sb.append("lpsust=").append(lpsust);
					sb.append("&realm=").append(ch.getRealm());
					// 发起HTTP连接
					String respstring = PromoSupport
							.getInstance()
							.getHttp()
							.request(ch.getUserverifyUrl(), sb.toString(),
									ch.getUserverfiyHttpMethod());

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
					
					logger.debug(respstring);
					
					Document doc = XmlUtil.openXml(new ByteArrayInputStream(respstring.getBytes("utf-8")));
					
					Response re = new Response();
					
					Element root = doc.getRootElement();
					if (TAG_IDENTITYINFO.equalsIgnoreCase(root.getName())) {
						re.status = 1;
						re.AccountID = root.getChildTextTrim("AccountID");
						re.Username = root.getChildTextTrim("Username");
						re.DeviceID = root.getChildTextTrim("DeviceID");
						re.verified = root.getChildTextTrim("verified");
					} else {
						re.status = 0;
						re.ErrorCode = root.getChildTextTrim("Code");
					}
					
					//检测
					if (re.status == 0/*||(!"1".equals(re.verified))*/) {
						// 与第三方服务器验证失败
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp.writeUtf8String(KGameTips
								.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
										promoID,
										re.ErrorCode));
						playersession.send(pverifyResp);
						return;
					}
					
					String promoMask = re.AccountID;

					// 返回信息中添加参数（直接重用上面的Map对象）
					params.clear();
					params.put(KGameProtocol.PROMO_KEY_LENOVO_ACCOUNTID, promoMask);

					PromoSupport.getInstance().afterUserVerified(playersession,
							promoID, ch.getPromoID(), promoMask, re.Username,
							pverifyResp, params, analysisInfoNeedSaveToDB);
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
