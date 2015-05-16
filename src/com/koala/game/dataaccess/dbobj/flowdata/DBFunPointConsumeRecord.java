package com.koala.game.dataaccess.dbobj.flowdata;

import com.koala.game.dataaccess.dbobj.DBGameObj;

public class DBFunPointConsumeRecord extends DBGameObj{

	/**流水ID*/
	public long id;
	/**账号ID*/
	public long playerId;
	/**角色ID*/
	public long roleId;
	/**功能使用类型*/
	public int funType;
	/**消费点数*/
	public int consumePoint;
	/**功能使用时间*/
	public long consumeTime;
	/**账号推广子渠道ID*/
	public int promoId;
	/**账号推广父渠道ID*/
	public int parentPromoId;
	/**功能描述*/
	public String desc;

	//是否首次使用点数
	public boolean isFirstUsePoint;
	
	public DBFunPointConsumeRecord(long playerId, long roleId, int funType,
			int consumePoint, long consumeTime, int promoId,int parentPromoId, String desc,boolean isFirstUsePoint) {
		super();
		this.playerId = playerId;
		this.roleId = roleId;
		this.funType = funType;
		this.consumePoint = consumePoint;
		this.consumeTime = consumeTime;
		this.promoId = promoId;
		this.parentPromoId = parentPromoId;
		this.desc = desc;
		this.isFirstUsePoint = isFirstUsePoint;
	}

	@Override
	public void setId(long id) {		
	}

	

}
