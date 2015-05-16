package com.koala.game.communication;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.koala.game.KGame;
import com.koala.game.communication.KGameHttpRequestSender.KGameHttpRequestResult;
import com.koala.game.logging.KGameLogger;
import com.koala.game.util.StringUtil;
import com.koala.promosupport.MD5;
import com.koala.thirdpart.json.JSONArray;
import com.koala.thirdpart.json.JSONObject;

public class YYHTTPPatch {
	
	static final KGameLogger _LOGGER = KGameLogger.getLogger(YYHTTPPatch.class);
	
	public static void main(String[] strs) throws Exception{
		String[] codes = new String[]{
				"0000561337",
				"0000757419",
				"0000757653",
				"0002020864",
				"0002337657",
				"0003685812",
				"0003685939",
				"0003687014",
				"0003687492",
				"0003689490",
				"0028287071",
				"0043709198",
				"0076793787",
				"0100939806",
				"0124783885",
				"0125748849",
				"0130886066",
				"0133716641",
				"0145223435",
				"0190191758",
				"0231113362",
				"0243815035",
				"0243815657",
				"0000561494",
				"0014503196",
				"0148628467"		};
		
		for(String code: codes){
			notifyYYByHttpForMain(-1, -1, "roleName", code, "H");
		}
	}
	
	private static void notifyYYByHttpForMain(long playerId, long roleId, String roleName, String activationCode, String giftType) {
		// 反馈给YY信息：
//		“passport” : “my_good_game”,  // 合作游戏厂商在YY注册的通行证，不区分大小写。（获取方式见附录）， “timestamp”: “2012-01-02 03:04:05”,  // 时间戳- 格式：YYYY-MM-DD hh:mm:ss, 防重放用
//		“tid”: 1321,  //  task id - 任务标识 – 因游戏厂商名下可能有多个任务，需要明确指定是操作哪个任务
//		“data”: [  // 玩家已经完成的任务码（玩家进入游戏任务时输入的值）
//		“RHT781723JF70GO3C”, 
//		“Y03A8K8562X72Q84J”, 
//		“JX1A3O5G484Y02KJ9”,
//		………………………………….,
//		………………………………….,
//		“9F0Q5HU7OQ832GGE4”
//		]
		
		String YYHTTPAddress="http://task.g.yy.com/task/daily/complete.do";
		String YYPassport = "xxwancftg";
		String YYTid = "2331";
		String YYKey = "7j9$2im9dq";
		
		SimpleDateFormat DATE_FORMAT2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		long nowTime = System.currentTimeMillis();
		//
		
		//
		String json;
		String code;
		try {
			JSONObject josnO = new JSONObject();
			josnO.put("passport", YYPassport);
			josnO.put("timestamp", DATE_FORMAT2.format(new Date(nowTime)));
			josnO.put("tid", YYTid);
			
			JSONArray jsonA = new JSONArray();
			jsonA.put(activationCode);
			
			josnO.put("data", jsonA);
			//
			json = josnO.toString();
			code = MD5.MD5Encode( json + ";" + YYKey);
		} catch(Exception e){
			_LOGGER.error(",yyHttp,gsId=,{},playerId=,{},角色ID=,{},角色名=,{},giftType=,{},giftCode=,{},result=,{},exception=,{}", KGame.getGSID(), playerId,
					roleId, roleName, giftType, activationCode, "json失败", e.getMessage());
			
			_LOGGER.error(e.getMessage(), e);
			return;
		}
		
		Map<String, Object> paraMap = new HashMap<String, Object>();
		paraMap.put("code", code);
		paraMap.put("json", json);
		
		List<String> needEncodeKeys = new ArrayList<String>();
		needEncodeKeys.add("json");

		try {
			Future<KGameHttpRequestResult> httpResult = KGame.sendPostRequest(YYHTTPAddress, paraMap, needEncodeKeys, null);
			
			String tips = StringUtil.format(",yyHttp,gsId=,{},playerId=,{},角色ID=,{},角色名=,{},giftType=,{},giftCode=,{},result=,{},http=,{}", KGame.getGSID(), playerId, roleId,
					roleName, giftType, activationCode);//成功或失败,失败原因,留给时效任务处理
			
			// YY
			JSONObject jsonObj = new JSONObject(httpResult.get().content);
			if (jsonObj.getInt("result")==0) {
				_LOGGER.warn(tips, "成功", "");
			} else {
				_LOGGER.warn(tips, "失败", httpResult.get().content);
			}
			
		} catch (Exception e) {
			_LOGGER.error(",yyHttp,gsId=,{},playerId=,{},角色ID=,{},角色名=,{},giftType=,{},giftCode=,{},result=,{},exception=,{}", KGame.getGSID(), playerId,
					roleId, roleName, giftType, activationCode, "失败", e.getMessage());

			_LOGGER.error(e.getMessage(), e);
		}
	}	
}
