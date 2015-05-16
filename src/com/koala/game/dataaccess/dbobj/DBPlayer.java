package com.koala.game.dataaccess.dbobj;

public class DBPlayer {
	private long playerId;
	private String playerName;
	private String password;
	private String mobileNum;
	private int type;
	private int promoId;
	private int parentPromoId;
	private int securityQuestionIdx;
	private int securityAnswerIdx;
	private String securityAnswer;
	private String remark;
	private long createTimeMillis;
	private long lastestLoginTimeMillis;
	private long lastestLogoutTimeMillis;
	private long totalLoginCount;
	private long totalOnlineTimeMillis;
	private String attribute;
	//推广渠道的账号标识（唯一），例如：当乐渠道用当乐账号作为标识
	private String promoMask;
    
	/**
	 * 获取账号ID
	 * @return
	 */
	public long getPlayerId() {
		return playerId;
	}

	public void setPlayerId(long playerId) {
		this.playerId = playerId;
	}
	
	/**
	 * 获取账号名
	 * @return
	 */
	public String getPlayerName() {
		return playerName;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getMobileNum() {
		return mobileNum;
	}

	public void setMobileNum(String mobileNum) {
		this.mobileNum = mobileNum;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getPromoId() {
		return promoId;
	}

	public void setPromoId(int promoId) {
		this.promoId = promoId;
	}

	public int getSecurityQuestionIdx() {
		return securityQuestionIdx;
	}

	public void setSecurityQuestionIdx(int securityQuestionIdx) {
		this.securityQuestionIdx = securityQuestionIdx;
	}

	public int getSecurityAnswerIdx() {
		return securityAnswerIdx;
	}

	public void setSecurityAnswerIdx(int securityAnswerIdx) {
		this.securityAnswerIdx = securityAnswerIdx;
	}

	public String getSecurityAnswer() {
		return securityAnswer;
	}

	public void setSecurityAnswer(String securityAnswer) {
		this.securityAnswer = securityAnswer;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public long getCreateTimeMillis() {
		return createTimeMillis;
	}

	public void setCreateTimeMillis(long createTimeMillis) {
		this.createTimeMillis = createTimeMillis;
	}

	public long getLastestLoginTimeMillis() {
		return lastestLoginTimeMillis;
	}

	public void setLastestLoginTimeMillis(long lastestLoginTimeMillis) {
		this.lastestLoginTimeMillis = lastestLoginTimeMillis;
	}

	public long getLastestLogoutTimeMillis() {
		return lastestLogoutTimeMillis;
	}

	public void setLastestLogoutTimeMillis(long lastestLogoutTimeMillis) {
		this.lastestLogoutTimeMillis = lastestLogoutTimeMillis;
	}

	public long getTotalLoginCount() {
		return totalLoginCount;
	}

	public void setTotalLoginCount(long totalLoginCount) {
		this.totalLoginCount = totalLoginCount;
	}

	public long getTotalOnlineTimeMillis() {
		return totalOnlineTimeMillis;
	}

	public void setTotalOnlineTimeMillis(long totalOnlineTimeMillis) {
		this.totalOnlineTimeMillis = totalOnlineTimeMillis;
	}

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	public String getPromoMask() {
		return promoMask;
	}

	public void setPromoMask(String promoMask) {
		this.promoMask = promoMask;
	}

	public int getParentPromoId() {
		return parentPromoId;
	}

	public void setParentPromoId(int parentPromoId) {
		this.parentPromoId = parentPromoId;
	}

	@Override
	public String toString() {
		return "DBPlayer [playerId=" + playerId + ", playerName=" + playerName
				+ ", password=" + password + ", promoId=" + promoId
				+ ", remark=" + remark + ", promoMask=" + promoMask + "]";
	}
	
	
}
