package com.koala.game.player;

import com.koala.thirdpart.json.JSONException;
import com.koala.thirdpart.json.JSONObject;

public final class KGameSimpleRoleInfo {
	// public int gsID;
	// public long roleID;
	// public String roleName;
	private KGameSimpleRoleInfoCID cid;
	public int roleLV;
	public int roleJobType;

	public KGameSimpleRoleInfo(int gsID, long roleID) {
		this.cid = new KGameSimpleRoleInfoCID(gsID, roleID);
	}

	public KGameSimpleRoleInfoCID getCid() {
		return cid;
	}

	@Override
	public String toString() {
		return cid.gsID + "," + cid.roleID + "," + roleLV + "," + roleJobType;
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject j = new JSONObject();
		j.put("gsid", cid.gsID);
		j.put("id", cid.roleID);
		j.put("lv", roleLV);
		j.put("job", roleJobType);
		return j;
	}

	public KGameSimpleRoleInfo(JSONObject j) throws JSONException {
		//JSONObject j = new JSONObject(json);
		this.cid = new KGameSimpleRoleInfoCID(j.optInt("gsid"), j.optLong("id"));
		this.roleLV = j.optInt("lv");
		this.roleJobType = j.optInt("job");
	}

}
