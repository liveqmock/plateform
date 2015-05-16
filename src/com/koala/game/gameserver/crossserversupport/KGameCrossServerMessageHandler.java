package com.koala.game.gameserver.crossserversupport;

import com.koala.game.KGameMessagePayload;
import com.koala.game.exception.KGameServerException;

/**
 * 需要监听跨服务器消息的请实现本接口，并调用
 * {@link KGameCrossServerSupport#addCrossServerMessageHandler(KGameCrossServerMessageHandler)}
 * 设置监听器
 * <br><br>
 * 注意：目前只实现了功能，未是最优性能-20131209
 * @author AHONG
 * 
 */
public interface KGameCrossServerMessageHandler {
	
	/**
	 * 收到跨服务器消息
	 * @param senderGsID 发送者GS的ID
	 * @param msgPayload 消息内容（平台只是透传作用，并不改动消息任何内容）
	 * @return 如果返回<b>true</b>代表此消息已经被当前模块处理掉，无须再分发给其它模块；反之，返回<b>false</b>
	 *         代表这个消息不属于当前模块的消息， 让分发底层把这条消息发给其它模块试试
	 * @throws KGameServerException
	 */
	boolean crossmessageReceived(int senderGsID,KGameMessagePayload msgPayload)
			throws KGameServerException;
}
