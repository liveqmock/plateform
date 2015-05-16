package com.koala.paymentserver;


/**
 * 在SDK支付回调中，可以包含一段由CP自定义数据ext，这个ext数据从GS->Client->SDKServer->PaymentServer。<br>
 * ext是一个非常重要的一个数据，我们的定义在本类中确定
 * 
 * <pre>
 * 格式： p1000g1r12345
 * 其中1000是渠道ID
 * 1是gsID
 * 12345是角色ID
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class PayExtParam {

	private int promoID;
	private int gsID;
	private long roleID;
//	private String promoMask;

	public PayExtParam(int promoID, int gsID, long roleID/*,String promoMask*/) {
		this.promoID = promoID;
		this.gsID = gsID;
		this.roleID = roleID;
//		this.promoMask = promoMask;
	}

	/**
	 * <pre>
	 * 格式： p1000g1r12345
	 * 其中1000是渠道ID
	 * 1是gsID
	 * 12345是角色ID
	 * </pre>
	 * 
	 * @param encodedStr
	 */
	public PayExtParam(String encodedStr) {
//		try {
//			JSONObject json = new JSONObject(encodedStr);
//			this.promoID = json.optInt("pid");
//			this.gsID = json.optInt("gid");
//			this.roleID = json.optInt("rid");
//		} catch (JSONException e) {
//			throw new IllegalArgumentException(e);
//		}
		int gid = encodedStr.indexOf("g");
		this.promoID = Integer.parseInt(encodedStr.substring(1, gid));
		int rid = encodedStr.indexOf("r", gid);
		this.gsID = Integer.parseInt(encodedStr.substring(gid + 1, rid));
		this.roleID = Long.parseLong(encodedStr.substring(rid + 1));
//		int pm = encodedStr.indexOf("pm",rid);
//		this.roleID = Integer.parseInt(encodedStr.substring(rid + 3,pm));
//		this.promoMask = encodedStr.substring(pm + 2);
	}

	public int getPromoID() {
		return promoID;
	}

	public int getGsID() {
		return gsID;
	}

	public long getRoleID() {
		return roleID;
	}
	
//	/**promoMask，目前只有联想用到，其它有些如91的自定义字符串控制了20长度*/
//	public String getPromoMask() {
//		return promoMask;
//	}

	@Override
	public String toString() {
//		JSONObject json = new JSONObject();
//		try {
//			json.put("pid", promoID);
//			json.put("gid", gsID);
//			json.put("rid", roleID);
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
//		return json.toString();
		return (new StringBuilder().append("p").append(promoID).append("g")
				.append(gsID).append("r").append(roleID)/*.append("pm")
				.append(promoMask)*/).toString();
	}
	
}
