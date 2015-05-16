package com.koala.game.gameserver.crossserversupport;

import java.util.HashSet;
import java.util.Set;

import com.koala.game.KGameMessage;
import com.koala.game.KGameMessagePayload;
import com.koala.game.KGameProtocol;
import com.koala.game.communication.KGameCommunication;
import com.koala.game.exception.KGameServerException;
import com.koala.game.gameserver.KGameServer;

/**
 * 跨服务器支持，例如GS1发送一条消息到GS2等操作。20131209增加，功能将不断完善增加
 * 
 * @author AHONG
 * 
 */
public class KGameCrossServerSupport {

	private static Set<KGameCrossServerMessageHandler> handlers = new HashSet<KGameCrossServerMessageHandler>();

	public static boolean addCrossServerMessageHandler(
			KGameCrossServerMessageHandler handler) {
		return (handler != null) ? handlers.add(handler) : false;
	}

	/**
	 * 发送一条跨GS的消息
	 * 
	 * @param senderGsID
	 *            发送者GS的ID（即本GS）
	 * @param receiverGsID
	 *            接收者GS的ID
	 * @param msg
	 *            待发送的完整消息（平台只是透传作用，并不改动消息任何内容）
	 * @throws KGameServerException
	 */
	public static void sendGrossServerMessage(int senderGsID, int receiverGsID,
			KGameMessage msg) throws KGameServerException {
		if (msg == null) {
			throw new KGameServerException("msg is null.");
		}
		KGameMessage msgCross = KGameCommunication.newMessage(
				KGameMessage.MTYPE_PLATFORM, KGameMessage.CTYPE_GS,
				KGameProtocol.MID_CROSS_SERVER_MSG);
		msgCross.writeInt(senderGsID);
		msgCross.writeInt(receiverGsID);
		byte[] bytes = KGameCommunication.message2bytes(msg);
		if (bytes == null) {
			// bytes = new byte[0];
			throw new KGameServerException("msg's bytes is null.");
		}
		msgCross.writeInt(bytes.length);
		msgCross.writeBytes(bytes);
		KGameServer.getInstance().getGS2FE().sendCrossMessage(msgCross);
	}

	public static void notifyHandlers(int senderGsID,
			KGameMessagePayload payload) throws KGameServerException {
		for (KGameCrossServerMessageHandler h : handlers) {
			if (h != null && h.crossmessageReceived(senderGsID, payload)) {
				break;
			}
		}
	}
	
}
