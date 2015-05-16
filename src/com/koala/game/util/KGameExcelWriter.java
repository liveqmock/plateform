package com.koala.game.util;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

public class KGameExcelWriter {

	private static final List<List<Object>> _EMPTY_LIST = Collections.emptyList();
	
	private String _path;
	private Map<String, List<List<Object>>> _sheetDatas;
	private Map<String, List<String>> _colTitlesOfEachSheet;
	private Map<String, Integer> _titleIndex;
	private final AtomicBoolean _isOutputed = new AtomicBoolean();
	
	public KGameExcelWriter(String path) {
		this._path = path;
		this._sheetDatas = new LinkedHashMap<String, List<List<Object>>>();
		this._colTitlesOfEachSheet = new LinkedHashMap<String, List<String>>();
		this._titleIndex = new LinkedHashMap<String, Integer>();
	}
	
	public void addColTitleForSheet(String sheetName, List<String> colTitles) {
		if(_isOutputed.get()) {
			throw new RuntimeException("the file is already outputed");
		}
		if (colTitles != null && colTitles.size() > 0) {
			this._colTitlesOfEachSheet.put(sheetName, colTitles);
		}
	}
	
	public void putDataToSheet(String sheetName, List<Object> aRow) {
		if(_isOutputed.get()) {
			throw new RuntimeException("the file is already outputed");
		}
		if (aRow != null && aRow.size() > 0) {
			List<List<Object>> currentSheet = this._sheetDatas.get(sheetName);
			if (currentSheet == null) {
				currentSheet = new ArrayList<List<Object>>();
				this._sheetDatas.put(sheetName, currentSheet);
			}
			currentSheet.add(aRow);
		}
	}
	
	public void putAllDataToSheet(String sheetName, List<List<Object>> rows) {
		if(_isOutputed.get()) {
			throw new RuntimeException("the file is already outputed");
		}
		for (int i = 0; i < rows.size(); i++) {
			this.putDataToSheet(sheetName, rows.get(i));
		}
	}
	
	public void setTitleIndex(String sheetName, int index) {
		if(index >= 0) {
			_titleIndex.put(sheetName, index);
		}
	}
	
	public void output() throws Exception {
		this._isOutputed.compareAndSet(false, true);
		FileOutputStream fos = new FileOutputStream(new File(_path));
		WritableWorkbook workBook = Workbook.createWorkbook(fos);
		String sheetName;
		for (Iterator<String> itr = _colTitlesOfEachSheet.keySet().iterator(); itr.hasNext();) {
			sheetName = itr.next();
			if (!_sheetDatas.containsKey(sheetName)) {
				_sheetDatas.put(sheetName, _EMPTY_LIST);
			}
		}
		int sheetNum = 0;
		Map.Entry<String, List<List<Object>>> entry;
		List<List<Object>> contentList;
		List<Object> aRow;
		for (Iterator<Map.Entry<String, List<List<Object>>>> itr = _sheetDatas.entrySet().iterator(); itr.hasNext();) {
			entry = itr.next();
			WritableSheet sheet = workBook.createSheet(entry.getKey(), sheetNum);
			sheetNum++;
			String temp;
			Integer rowNum = _titleIndex.get(entry.getKey());
			if(rowNum == null) {
				rowNum = 0;
			}
			List<String> title = _colTitlesOfEachSheet.get(entry.getKey());
			contentList = entry.getValue();
			if (title != null && title.size() > 0) {
				for (int i = 0; i < title.size(); i++) {
					temp = title.get(i);
					sheet.addCell(new Label(i, rowNum, temp == null ? "" : temp));
				}
			}
			for (int i = 0; i < contentList.size(); i++) {
				aRow = contentList.get(i);
				rowNum++;
				for (int k = 0; k < aRow.size(); k++) {
					sheet.addCell(new Label(k, rowNum, aRow.get(k).toString()));
				}
			}
			workBook.write();
			workBook.close();
			fos.close();
		}
	}
}
