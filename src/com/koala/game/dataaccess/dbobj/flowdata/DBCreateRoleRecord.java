package com.koala.game.dataaccess.dbobj.flowdata;

import com.koala.game.dataaccess.dbobj.DBGameObj;

public class DBCreateRoleRecord  extends DBGameObj{
	
	public long id;
	public long playerId;
	public long roleId;
	public String roleName;
	public String uid;
	public int job;
	public long createTime;
	public int promoId;
	public int parentPromoId;
		
	public DBCreateRoleRecord(long playerId, long roleId, String roleName, String uid, int job, long createTime, int promoId, int parentPromoId) {
		super();
		this.playerId = playerId;
		this.roleId = roleId;
		this.roleName = roleName;
		this.uid = uid;
		this.job = job;
		this.createTime = createTime;
		this.promoId = promoId;
		this.parentPromoId = parentPromoId;
	}



	@Override
	public void setId(long id) {
		// TODO Auto-generated method stub
		
	}

}
