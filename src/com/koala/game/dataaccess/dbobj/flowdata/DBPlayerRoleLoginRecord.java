package com.koala.game.dataaccess.dbobj.flowdata;

import com.koala.game.dataaccess.dbobj.DBGameObj;

public class DBPlayerRoleLoginRecord extends DBGameObj{
	public long id;
	public long playerId;
	public long roleId;
	public int vipLevel;
	public long loginTime;
	public long logoutTime;
	public long onlineTime;
	public int promoId;
	public int parentPromoId;
	
	
	
	public DBPlayerRoleLoginRecord(long id, long playerId, long roleId,
			int vipLevel, long loginTime, long logoutTime, long onlineTime,
			int promoId, int parentPromoId) {
		super();
		this.id = id;
		this.playerId = playerId;
		this.roleId = roleId;
		this.vipLevel = vipLevel;
		this.loginTime = loginTime;
		this.logoutTime = logoutTime;
		this.onlineTime = onlineTime;
		this.promoId = promoId;
		this.parentPromoId = parentPromoId;
	}



	@Override
	public void setId(long id) {
		
	}

	
	
	

}
