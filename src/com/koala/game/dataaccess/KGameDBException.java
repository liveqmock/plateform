package com.koala.game.dataaccess;

public class KGameDBException extends KGameDataAccessException{
	
	public final static int CAUSE_RECORD_NOT_EXIST = 1001;
	public final static int CAUSE_DB_TIMEOUT = 1002;
	public final static int CAUSE_UNKNOWN_ERROR = 1003;
	public final static int CAUSE_DB_SHUTDOWN_WITH_UNKNOW_REASON = 9999;
	
	public KGameDBException(String message, Throwable cause, int errorCode,
			String desc) {
		super(message, cause, errorCode, desc);
	}	
	
	

}
