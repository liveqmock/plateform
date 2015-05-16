package com.koala.game.util;

import org.jboss.netty.channel.group.DefaultChannelGroup;

import com.koala.game.KGameMessage;
import com.koala.game.player.KGamePlayerSession;

public class PlayerSessionGroup {

	private DefaultChannelGroup channelGroup;

	public PlayerSessionGroup() {
		channelGroup = new DefaultChannelGroup();
	}

	public PlayerSessionGroup(String name) {
		channelGroup = new DefaultChannelGroup(name);
	}

	public boolean add(KGamePlayerSession session) {
		return channelGroup.add(session.getChannel());
	}

	public void broadcast(KGameMessage msg) {
		
	}
}
