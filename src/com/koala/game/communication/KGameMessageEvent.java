package com.koala.game.communication;

import com.koala.game.KGameMessage;
import com.koala.game.player.KGamePlayerSession;

/**
 * 消息事件，其实就是对通道和消息体封装一起交给游戏逻辑处理的时候方便些
 * 
 * @author AHONG
 * 
 */
public final class KGameMessageEvent {

	private KGamePlayerSession playerSession;
	private KGameMessage message;

	public KGameMessageEvent(KGamePlayerSession playerSession, KGameMessage msg) {
		this.playerSession = playerSession;
		this.message = msg;
	}

	public KGameMessage getMessage() {
		return message;
	}

	public KGamePlayerSession getPlayerSession() {
		return playerSession;
	}

	@Override
	public String toString() {
		return playerSession+"->"+message;
	}
	
}
