package com.koala.game.dataaccess.dbobj.flowdata;

import com.koala.game.dataaccess.dbobj.DBGameObj;

public class DBRoleUpgradeLvRecord extends DBGameObj{
	
	public long id;
	public long playerId;
	public long roleId;
	public String roleName;
	public String uid;
	public int preLv;
	public int nowLv;
	public long upgradeTime;
	public int useTimeSecond;
	public int promoId;
	public int parentPromoId;
		

	public DBRoleUpgradeLvRecord(long playerId, long roleId, String roleName, String uid, int preLv, int nowLv, long upgradeTime, int useTimeSecond, int promoId, int parentPromoId) {
		super();
		this.playerId = playerId;
		this.roleId = roleId;
		this.roleName = roleName;
		this.uid = uid;
		this.preLv = preLv;
		this.nowLv = nowLv;
		this.upgradeTime = upgradeTime;
		this.useTimeSecond = useTimeSecond;
		this.promoId = promoId;
		this.parentPromoId = parentPromoId;
	}







	@Override
	public void setId(long id) {
		
	}
	
	

}
