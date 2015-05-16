package com.koala.game;

public interface KGameServerType {
	
	public static final int SERVER_TYPE_FE = 0x0;
	public static final int SERVER_TYPE_GS = 0x1;
	public static final int SERVER_TYPE_GMS = 0x2;
	public static final int SERVER_TYPE_LOGS = 0x3;
	
	int getType();
	
	String getName();
	
}
