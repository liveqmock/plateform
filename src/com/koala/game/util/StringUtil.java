package com.koala.game.util;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

public class StringUtil {

	/**
	 * 字符串的格式化，Logger的方式。比java.text.MessageFormat快10倍！
	 * 
	 * <pre>
	 * 例子：
	 * 1. format("Hi {}. {}","kola",520); 返回"Hi kola. 520"
	 * 2. 如果{}中间有内容就直接输出，不算是格式中的{}。如：format("Set{1,2,3} size={}",3); return "Set{1,2,3} size=3"
	 * 3. 如果本身就想输出"{}"那可以用"\\"作为转义符。如：format("\\{} {}","kola"); return "{} kola"
	 * 4. format(File name is C:\\\\{}., "file.zip"); 返回"File name is C:\file.zip"
	 * </pre>
	 * 
	 * @param messagePattern
	 *            格式，中间含{}标记
	 * @param arguments
	 *            可以配N个参数，生成的字符串中参数会直接按照上面format的{}顺序代入
	 * @return
	 */
	public static String format(String messagePattern, Object... arguments) {
		FormattingTuple ft = MessageFormatter.arrayFormat(messagePattern, arguments);
		return ft.getMessage();
	}

	/**
	 * 看看有没有为null或长度为0的字符串（其中有一个为true即返回true）
	 * @param arguments
	 * @return
	 */
	public static boolean hasNullOr0LengthString(String... arguments){
		for (String string : arguments) {
			if(string==null||string.length()<=0){
				return true;
			}
		}
		return false;
	}
}
