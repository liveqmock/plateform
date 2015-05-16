package com.koala.game.resserver;

import java.util.HashMap;
import java.util.Map;

/**
 * 资源对照清单
 * 
 * @author AHONG
 * 
 */
final class KGameResfileList {

	int revision;// 清单的修订版本号
	/** resid,resrevision */
	Map<Integer, Integer> list;

	public KGameResfileList(int revision) {
		this.revision = revision;
		list = new HashMap<Integer, Integer>();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Integer resid : list.keySet()) {
			sb.append(resid).append(" ").append(list.get(resid)).append("; ");
		}
		return sb.toString();
	}

}
