package com.koala.game.dataaccess.dbobj.flowdata;

import com.koala.game.dataaccess.dbobj.DBGameObj;

public class DBPresentPointRecord extends DBGameObj{

	public long player_id;
	public long role_id;
	public String role_name;
	public int present_point;
	public int type;
	public String desc;
	public long present_time;
	public int promo_id;
	public int parent_promo_id;
	public int server_id;

	

	public DBPresentPointRecord(long player_id, long role_id, String role_name,
			int present_point, int type, String desc, long present_time,
			int promo_id, int parent_promo_id, int server_id) {
		super();
		this.player_id = player_id;
		this.role_id = role_id;
		this.role_name = role_name;
		this.present_point = present_point;
		this.type = type;
		this.desc = desc;
		this.present_time = present_time;
		this.promo_id = promo_id;
		this.parent_promo_id = parent_promo_id;
		this.server_id = server_id;
	}



	@Override
	public void setId(long id) {
		
	}
	
	

}
