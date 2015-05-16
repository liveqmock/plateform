package com.koala.game.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

/**
 * <pre>
 * 代表一个excel文件的整体数据，此类会把excel文件的
 * 所有工作表加载进来。一般情况下，可以通过{@link #getAllSheetNames()}
 * 去获取所有工作表的名字数组，然后循环这个数组，通过{@link #getTable(String, int)}
 * 来获取指定数据表的KGameExcelTable对象
 * </pre>
 * @author PERRY CHAN
 */
public class KGameExcelFile {

	private String[] _allSheetNames;
	private Map<String, Sheet> _allSheets;
	private Map<String, String> _lowercaseName;
	
	/**
	 * 
	 * @param path
	 * @throws IOException
	 * @throws BiffException
	 */
	public KGameExcelFile(String path) throws IOException, BiffException{
		//关闭JXL内部GC by camus@201305082234
		WorkbookSettings ws=new WorkbookSettings();
		ws.setGCDisabled(true);
		//
		Workbook wb = Workbook.getWorkbook(new File(path),ws);
		_allSheetNames = wb.getSheetNames();
		_allSheets = new HashMap<String, Sheet>(_allSheetNames.length);
		_lowercaseName = new HashMap<String, String>(_allSheetNames.length);
		Sheet[] sheets = wb.getSheets();
		Sheet sheet;
		for(int i = 0; i < sheets.length; i++){
			sheet = sheets[i];
			this._allSheets.put(sheet.getName(), sheet);
			this._lowercaseName.put(sheet.getName().toLowerCase(), sheet.getName());
		}
	}
	
	/**
	 * 获取此excel文件包含的所有工作表的名字
	 * @return
	 */
	public String[] getAllSheetNames(){
		return _allSheetNames;
	}
	
	/**
	 * <pre>
	 * 根据工作表的名字，以及标题行的索引（即你所指定的表头在excel中处于第几行，从1开始计算），
	 * 然后返回一个KGameExcelTable对象
	 * </pre>
	 * @param sheetName
	 * @param headerIndex
	 * @return
	 */
	public KGameExcelTable getTable(String sheetName, int headerIndex) {
		KGameExcelTable table = new KGameExcelTable();
		Sheet sheet = this._allSheets.get(sheetName);
		if (sheet == null) {
			String name = this._lowercaseName.get(sheetName.toLowerCase());
			sheet = this._allSheets.get(name);
			if (sheet == null) {
				throw new NullPointerException("找不到名字为：[" + sheetName + "]的工作表！");
			}
		}
		table.loadData(sheet, headerIndex);
		return table;
	}
}
