package com.koala.promosupport.qh360;

/**
 * 3.2.5 刷新access token–服务器端接口, 应用服务器调用 应用获取的access token有时间限制.
 * 如果用户登录时间长于这个时间限制, 就会导致token过期, 调用开放平台接口会失败. 应用服务器可再次调用/oauth2/access_token接口,
 * 用refresh token 去换新access token. 同时, refresh token也会更新.
 * 
 * <pre>
 * 参数  必选  参数说明 
 * grant_type  Y  定值 refresh_token 
 * refresh_token  Y  用于刷新access token用的refresh token 
 * client_id  Y  app key 
 * client_secret  Y  app secret 
 * scope  Y  当前只支持值 basic 
 * 请求示例： 
 * https://openapi.360.cn/oauth2/access_token?grant_type=refresh_token&refresh_token=120659618
 * 68762ec8ab911a3089a7ebdf11f8264d5836fd41&client_id=0fb2676d5007f123756d1c1b4b5968bc&
 * client_secret=8d9e3305c1ab18384f56.....&scope=basic 
 * 
 * 返回参数： 
 *   参数同3.2.2节获取access token时的返回, 只是access token和refresh token都换了新的.
 * 
 *     access_token  Y  Access Token值 
 *     expires_in  Y  Access Token的有效期 以秒计 
 *     refresh_token  Y  用于刷新Access Token的Token,  有效期14天 
 *     scope Y  Access Token最终的访问范围，即用户实际授予的权限列表当前只支持值basic
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class QH360RefreshAccessToken {

	
	
}
