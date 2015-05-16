package com.koala.game.dataaccess.dbobj.flowdata;

import com.koala.game.dataaccess.dbobj.DBGameObj;

public class DBChargeRecord extends DBGameObj{

	/**账号ID*/
	public long player_id;
	/**角色ID*/
	public long role_id;
	/**角色名称*/
	public String role_name;
	/**充值时角色等级*/
	public int role_level;
	/**是否首次充值*/
	public byte is_first_charge;
	/**充值金额（单位：分）*/
	public int rmb;
	/**充值点数*/
	public int charge_point;
	/**充值卡号*/
	public String card_num;
	/**（暂为充值通道信息）*/
	public String card_password;
	/**充值时间*/
	public long charge_time;
	/**充值类型*/
	public int charge_type;
	/**账号推广子渠道ID*/
	public int promo_id;
	/**账号推广父亲渠道ID*/
	public int parent_promo_id;
	/**充值通道ID*/
	public int channel_id;
	/**服务器ID*/
	public int server_id;
	/**充值描述*/
	public String desc;

	public DBChargeRecord(long player_id, long role_id, String role_name,
			int role_level, byte is_first_charge, int rmb,
			int charge_point, String card_num, String card_password,
			long charge_time, int charge_type, int promo_id,int parent_promo_id,
			int channel_id, int server_id, String desc) {
		super();
		this.player_id = player_id;
		this.role_id = role_id;
		this.role_name = role_name;
		this.role_level = role_level;
		this.is_first_charge = is_first_charge;
		this.rmb = rmb;
		this.charge_point = charge_point;
		this.card_num = card_num;
		this.card_password = card_password;
		this.charge_time = charge_time;
		this.charge_type = charge_type;
		this.promo_id = promo_id;
		this.parent_promo_id = parent_promo_id;
		this.channel_id = channel_id;
		this.server_id = server_id;
		this.desc = desc;
	}

	@Override
	public void setId(long id) {
				
	}
	
	

}
