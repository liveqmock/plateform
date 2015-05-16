package com.koala.promosupport.downjoy;

/**
 * 当服务器收到用户购买物品的计费通知时，如果因某种原因无法发货，可以调用当乐的 退款接口，将款项退还给付款用户。
 * 
 * <pre>
 * 支付请求串格式： 
 * app_id=_&mid=_&order_no=_sig=_ 
 * 注：等号后的下划线位置为具体参数值，参数值说明如下： 
 * 参数名  参数说明 
 * app_id  当乐分配的APP_ID。 
 * mid  用户登录返回的mid参数。
 * order_no  支付订单号，在计费结果通知时传递的order参数。 
 * sig  MD5验证串，"app_id=_&mid=_&order_no=_&key=_"拼接结果通过MD5加密，并转为小写，得到sig参数值
 * 
 * ==================
 * 
 * 返回结果JSON
 * { 
 *   "error_code":0, 
 *   "error_msg":"成功" 
 * }
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class DownjoyRefund {

}
