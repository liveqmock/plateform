package com.koala.game.dataaccess.dbobj.flowdata;

import java.util.List;

import com.koala.game.dataaccess.dbobj.DBGameObj;

public class DBFlowGlobalData extends DBGameObj{
	
	List<DBCreateRoleRecord> createRoleRecordList;
	
	List<DBPlayerRoleLoginRecord> loginRecordList;
	
	List<DBPlayerRoleLoginRecord> logoutRecordList;
	
	List<DBFunPointConsumeRecord> funPointConsumeRecordList;
	
	List<DBShopSellItemRecord> shopSellItemRecordList;
	
	List<DBRoleUpgradeLvRecord> roleUpgradeLvRecordList;

	public DBFlowGlobalData() {
		super();		
	}
	
	public List<DBCreateRoleRecord> getCreateRoleRecordList() {
		return createRoleRecordList;
	}

	public void setCreateRoleRecordList(List<DBCreateRoleRecord> createRoleRecordList) {
		this.createRoleRecordList = createRoleRecordList;
	}

	public List<DBPlayerRoleLoginRecord> getLoginRecordList() {
		return loginRecordList;
	}

	public void setLoginRecordList(List<DBPlayerRoleLoginRecord> loginRecordList) {
		this.loginRecordList = loginRecordList;
	}

	public List<DBPlayerRoleLoginRecord> getLogoutRecordList() {
		return logoutRecordList;
	}

	public void setLogoutRecordList(List<DBPlayerRoleLoginRecord> logoutRecordList) {
		this.logoutRecordList = logoutRecordList;
	}

	public List<DBFunPointConsumeRecord> getFunPointConsumeRecordList() {
		return funPointConsumeRecordList;
	}

	public void setFunPointConsumeRecordList(
			List<DBFunPointConsumeRecord> funPointConsumeRecordList) {
		this.funPointConsumeRecordList = funPointConsumeRecordList;
	}

	public List<DBShopSellItemRecord> getShopSellItemRecordList() {
		return shopSellItemRecordList;
	}

	public void setShopSellItemRecordList(
			List<DBShopSellItemRecord> shopSellItemRecordList) {
		this.shopSellItemRecordList = shopSellItemRecordList;
	}
		

	public List<DBRoleUpgradeLvRecord> getRoleUpgradeLvRecordList() {
		return roleUpgradeLvRecordList;
	}

	public void setRoleUpgradeLvRecordList(List<DBRoleUpgradeLvRecord> roleUpgradeLvRecordList) {
		this.roleUpgradeLvRecordList = roleUpgradeLvRecordList;
	}

	@Override
	public void setId(long id) {
		
	}
	
	

}
