package com.koala.game.player;

public class KGameSimpleRoleInfoCID {
	public int gsID;
	public long roleID;

	public KGameSimpleRoleInfoCID(int gsID, long roleID) {
		this.gsID = gsID;
		this.roleID = roleID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + gsID;
		result = prime * result + (int) (roleID ^ (roleID >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		KGameSimpleRoleInfoCID other = (KGameSimpleRoleInfoCID) obj;
		if (gsID != other.gsID)
			return false;
		if (roleID != other.roleID)
			return false;
		return true;
	}

}
