package com.koala.game.gameserver.paysupport;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.channel.Channel;

import com.koala.game.KGameMessage;
import com.koala.game.communication.KGameCommunication;
import com.koala.game.gameserver.KGameServer;
import com.koala.game.logging.KGameLogger;
import com.koala.paymentserver.PayOrder;
import com.koala.paymentserver.PaymentProtocol;
import com.koala.promosupport.PromoSupport;
import com.koala.thirdpart.json.JSONObject;

/**
 * 游戏中的【支付/充值】支持。重点：1、发货（即加元宝）；2、充值流水的记录。
 * 
 * <pre>
 * 流水表结构
 * =====================================================================
 *   `id` bigint(15) unsigned NOT NULL AUTO_INCREMENT,  //
 *   `player_id` bigint(15) unsigned DEFAULT NULL,     //账号ID
 *   `role_id` bigint(15) unsigned DEFAULT NULL,       //角色ID
 *   `role_name` varchar(25) NOT NULL,                 //角色名
 *   `role_level` int(11) DEFAULT NULL,                //等级
 *   `is_first_charge` tinyint(1) DEFAULT 0,           //是否首次充值
 *   `rmb` int(11) DEFAULT NULL,                       //充值金额
 *   `charge_point` int(11) unsigned DEFAULT NULL,     //充值点数
 *   `card_num` varchar(40) DEFAULT NULL,              //充值卡号
 *   `card_password` varchar(40) DEFAULT NULL,         //充值卡密码
 *   `charge_time` datetime DEFAULT NULL,              //充值时间
 *   `charge_type` int(11) DEFAULT NULL,               //充值类型（如：神州行卡、支付宝等）
 *   `promo_id` int(11) DEFAULT NULL,                  //推广渠道ID
 *   `parent_promo_id` int(11) DEFAULT NULL,           //大渠道ID
 *   `channel_id` int(11) DEFAULT NULL,                //充值渠道ID
 *   `server_id` int(11) DEFAULT NULL,                 //游戏区ID
 *   `descr` varchar(300) DEFAULT NULL,                //描述
 * =====================================================================
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class KGamePaymentSupport implements PaymentProtocol {

	public final static String LoggerNameOfPaymentSupport = "chargeLogger";
	
	private final KGameLogger logger = KGameLogger
			.getLogger(LoggerNameOfPaymentSupport);
	private static KGamePaymentSupport instance;
	private KGamePaymentListener listener;
	private Channel verifiedPS2GSChannel;//payment server channel
	// private String yuanBaoPrice;//元宝价格，单位：元

	// 当客户端要进行充值时会向GS获取一些参数（各渠道不同）发去给SDK服务器。
	// 所以在PS跟GS握手的时候就把这些参数先发给GS缓存起来（注：GS是没有渠道相关的内容的）
	// <promoid,<key,value>>
	private final ConcurrentHashMap<Integer, ConcurrentHashMap<String, String>> promochannelpayparams = new ConcurrentHashMap<Integer, ConcurrentHashMap<String, String>>();

	private KGamePaymentSupport() {
	}

	public static KGamePaymentSupport getInstance() {
		if (instance == null) {
			instance = new KGamePaymentSupport();
		}
		return instance;
	}

	public void setPaymentListener(KGamePaymentListener listener) {
		this.listener = listener;
	}

	public KGamePaymentListener getPaymentListener() {
		return listener;
	}

	public void messageReceived(Channel channel, KGameMessage kmsg)
			throws Exception {
		switch (kmsg.getMsgID()) {

		case MID_HANDSHAKE:
			String code = kmsg.readUtf8String();

			logger.debug("MID_HANDSHAKE  code:{}  channal:{}",code, channel);
			// TODO 对code做验证

			// 当客户端要进行充值时会向GS获取一些参数（各渠道不同）发去给SDK服务器。
			// 所以在PS跟GS握手的时候就把这些参数先发给GS缓存起来（注：GS是没有渠道相关的内容的）
			int pcN = kmsg.readInt();
			for (int i = pcN; --i >= 0;) {
				int pid = kmsg.readInt();
				int ppsN = kmsg.readInt();
				ConcurrentHashMap<String, String> pps = new ConcurrentHashMap<String, String>(
						ppsN, 1.0f);
				logger.debug("--------------------------------------------{}({})", pid, ppsN);
				for (int j = ppsN; --j >= 0;) {
					String k = kmsg.readUtf8String();
					String v = kmsg.readUtf8String();
					logger.debug("promochannel({}) paybefore's params: {} = {}", pid, k, v);
					pps.put(k, v);
				}
				promochannelpayparams.put(pid, pps);
			}

			// 验证通过的通道
			verifiedPS2GSChannel = channel;

			KGameMessage handshakeResp = KGameCommunication.newMessage(
					kmsg.getMsgType(), kmsg.getClientType(), MID_HANDSHAKE);
			JSONObject json = new JSONObject();
			json.put("gsid", KGameServer.getInstance().getGSID());
			handshakeResp.writeUtf8String(json.toString());
			channel.write(handshakeResp);
			break;

		case MID_PING:
			if (verifiedPS2GSChannel != channel) {
				logger.error("Channel未HANDSHAKE! {}", channel);
				//channel.close();
				break;
			}
			long pingt = kmsg.readLong();
			KGameMessage pingresp = KGameCommunication.newMessage(
					kmsg.getMsgType(), kmsg.getClientType(), MID_PING);
			pingresp.writeLong(pingt);
			channel.write(pingresp);
			break;

		case MID_PS2GS_PAY:
			if (verifiedPS2GSChannel != channel) {
				logger.error("Channel未HANDSHAKE! {}", channel);
				channel.close();
				break;
			}
			String orderstring = kmsg.readUtf8String();
			PayOrder payOrder = new PayOrder(orderstring);
			PayOrderDealResult result = KGamePaymentSupport.getInstance()
					.getPaymentListener().dealPayOrder(payOrder);
			logger.info("【GS充值支撑】收到PS来的订单：{} ；逻辑处理结果：{}", payOrder, result);

			// 回复PS处理结果
			KGameMessage payresponse = KGameCommunication.newMessage(
					kmsg.getMsgType(), kmsg.getClientType(), MID_PS2GS_PAY);
			payresponse.writeInt(result.getResult());
			payresponse.writeUtf8String(orderstring);
			channel.write(payresponse);

//			// 主动PUSH(MID_PAY_RESULT)通知客户端，充值结果
//			if(result.getPlayerSession()!=null){//有可能null例如刚好已经下线了
//				KGameMessage pushmsg_payresult = KGameCommunication.newMessage(
//						kmsg.getMsgType(), kmsg.getClientType(), MID_PAY_RESULT);
//				pushmsg_payresult.writeUtf8String(result.getPayOrder().getOrderId());
//				pushmsg_payresult.writeInt(result.getResult());
//				pushmsg_payresult.writeUtf8String(result.getTipsToPlayer());
//				result.getPlayerSession().send(pushmsg_payresult);
//			}
			break;

		default:
			break;
		}
	}

	// public Channel getVerifiedChannel() {
	// return verifiedChannel;
	// }
	//
	// public void sendMessage2PaymentServer(KGameMessage msg) {
	// // 先设置以下客户端类型
	// msg.setInt(KGameMessage.INDEX_CLIENTTYPE, KGameMessage.CTYPE_PAYMENT);
	//
	// if (verifiedChannel != null && verifiedChannel.isConnected()) {
	// verifiedChannel.write(msg);
	// }
	// }

	public boolean isSupportedPromoChannel(int promoid) {
		// 注：要用父渠道ID来判断
		return promochannelpayparams.keySet().contains(promoid)
				|| promochannelpayparams.keySet().contains(
						PromoSupport.computeParentPromoID(promoid));
	}

	public Map<String, String> getParamsToClientBeforePay(int promoid) {
		Map<String, String> p =  promochannelpayparams.get(promoid);
		if(p==null){
			p =  promochannelpayparams.get(PromoSupport
					.computeParentPromoID(promoid));
		}
		return p;
	}
}
