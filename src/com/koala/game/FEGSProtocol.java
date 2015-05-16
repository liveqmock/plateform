package com.koala.game;

public interface FEGSProtocol extends KGameProtocol {

	public final static int MID_GS2FE_UPDATESTATUS = 2;// 单单GS向FE发送的消息

	public final static int MID_GS2FE_ISHUTDOWN = 3;// 单单GS向FE发送的消息
	
	public final static int MID_GS2FE_DELAY_REMOVE = 4; // 单单GS向FE发送的消息
	
	public final static int MID_GS2FE_REALLY_REMOVE = 5; // 单单GS向FE发送的消息
}
