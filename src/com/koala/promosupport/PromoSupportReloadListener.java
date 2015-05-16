package com.koala.promosupport;

/**
 * 渠道支持重新加载监听器，当xml配置修改后会热重加载
 * @author AHONG
 *
 */
public interface PromoSupportReloadListener {

	/**渠道信息全部重新加载了*/
	void notifyPromoSupportReloaded();
	
}
