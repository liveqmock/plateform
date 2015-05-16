package com.koala.game;

import com.koala.game.player.KGamePlayerSession;

/**
 * 玩家会话生命周期监听器，可以在模块中注册
 * 
 * @author AHONG
 * 
 */
public interface KGamePlayerSessionListener {

//	public static final int CAUSE_DISCONNECT_CLIENTCLOSE = 1;
//	public static final int CAUSE_DISCONNECT_IDLE_CLIENTWRITE = 2;
//	public static final int CAUSE_DISCONNECT_IDLE_CLIENTREAD = 3;
//	public static final int CAUSE_DISCONNECT_UNKNOWN = 0;
	
	

	void playerLogined(KGamePlayerSession playerSession);

	void playerSessionDisconnected(KGamePlayerSession playerSession, int cause);

	void playerLogouted(KGamePlayerSession playerSession, int cause);

	void playerSessionReconnected(KGamePlayerSession playerSession);
}
