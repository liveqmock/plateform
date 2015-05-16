package com.koala.game.communication;

import org.jboss.netty.channel.Channel;

import com.koala.game.gameserver.KGameServerHandler;
import com.koala.game.player.KGamePlayerSession;

/**
 * 为了将{@link Channel}和{@link KGamePlayerSession}绑定，在{@link Channel}对象上会set一个本对象。<br>
 * 注意：只有登录后的{@link Channel}可以获取到{@link KGameChannelAttachment}
 * @author AHONG
 * 
 */
public class KGameChannelAttachment {

	private final long playerID;
	private int disconnectedCause = KGameServerHandler.CAUSE_PLAYEROUT_UNKNOWN;
	private boolean isOverlap; // 是否顶掉别人的账号上来的
	private KGamePlayerSession overlapedPlayersession;

	public KGameChannelAttachment(long playerID) {
		this.playerID = playerID;
	}

	public long getPlayerID() {
		return playerID;
	}

	public int getDisconnectedCause() {
		return disconnectedCause;
	}

	public void setDisconnectedCause(int disconnectedCause) {
		this.disconnectedCause = disconnectedCause;
	}

	public KGamePlayerSession getOverlapedPlayersession() {
		return overlapedPlayersession;
	}

	public void setOverlapedPlayersession(KGamePlayerSession overlapedPlayersession) {
		this.overlapedPlayersession = overlapedPlayersession;
	}
	
	/**
	 * 
	 * 判断这个channel是否顶掉别人的账号上来的
	 * 
	 * @return
	 */
	public boolean isOverLap() {
		return isOverlap;
	}
	
	/**
	 * 
	 * 当是顶掉别人的账号，设为true
	 * 
	 * @param flag
	 */
	public void setOverlap(boolean flag) {
		this.isOverlap = flag;
	}
 
	@Override
	public String toString() {
		return "KGameChannelAttachment [playerID=" + playerID
				+ ", disconnectedCause=" + disconnectedCause
				+ ", overlapedPlayersession=" + overlapedPlayersession + "]";
	}
	
}
