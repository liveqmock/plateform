package com.koala.game.dataaccess.dbobj.flowdata;

import com.koala.game.dataaccess.dbobj.DBGameObj;

public class DBShopSellItemRecord extends DBGameObj{
	/**流水ID*/
	public long id;
	/**流水账号*/
	public long playerId;
	/**角色ID*/
	public long roleId;
	/**道具ID*/
	public long itemId;
	/**道具编码*/
	public String itemCode;
	/**购买数量*/
	public int count;
	/**消费点数*/
	public int consumePoint;
	/**购买时间*/
	public long consumeTime;
	/**账号推广子渠道ID*/
	public int promoId;
	/**账号推广父渠道ID*/
	public int parentPromoId;
	/**道具描述（暂时为道具名）*/
	public String desc;
	/**商店类型*/
	public int shopType;
	

	public DBShopSellItemRecord(long playerId, long roleId,
			long itemId, String itemCode, int count, int consumePoint,
			long consumeTime, int promoId, int parentPromoId, String desc,int shopType) {
		super();
		this.playerId = playerId;
		this.roleId = roleId;
		this.itemId = itemId;
		this.itemCode = itemCode;
		this.count = count;
		this.consumePoint = consumePoint;
		this.consumeTime = consumeTime;
		this.promoId = promoId;
		this.parentPromoId = parentPromoId;
		this.desc = desc;
		this.shopType = shopType;
	}



	@Override
	public void setId(long id) {
		
	}	

}
