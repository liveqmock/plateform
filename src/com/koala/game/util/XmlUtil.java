package com.koala.game.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * XML操作工具类，以jdom作为引擎
 * 
 * @author AHONG
 * 
 */
public class XmlUtil {

	/**
	 * <pre>
	 * 不处理
	 * 
	 * @param xml
	 * @return
	 * @author CamusHuang
	 * @creation 2014-3-5 上午10:02:08
	 * </pre>
	 */
	public static Document openXml(File xml) {
		SAXBuilder builder = new SAXBuilder();
		Document doc = null;
		try {
			doc = builder.build(xml);
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (doc != null) {
			// 解释Element嵌套的所有<ck-include>节点，并将内容附加到该Element下
			paramCKInclude(doc.getRootElement());
		}

		return doc;
	}

	public static Document openXml(InputStream srcIn) {
		SAXBuilder builder = new SAXBuilder();
		Document doc = null;
		try {
			doc = builder.build(srcIn);
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (doc != null) {
			// 解释Element嵌套的所有<ck-include>节点，并将内容附加到该Element下
			paramCKInclude(doc.getRootElement());
		}
		return doc;
	}

	public static Document openXml(String xml) {
		return openXml(new File(xml));
	}

	/**
	 * <pre>
	 * 解释Element嵌套的所有<ck-include>节点，并将内容附加到该Element下
	 * 
	 * @param root
	 * @author CamusHuang
	 * @creation 2014-3-5 上午9:47:54
	 * </pre>
	 */
	private static void paramCKInclude(Element root) {
		ArrayList listE = new ArrayList<Element>(root.getChildren());
		for (Object temp : listE) {
			Element tempE = (Element) temp;
			if (!tempE.getName().equals("ck-include")) {
				paramCKInclude(tempE);
			} else {
				paramCKInclude2(root, tempE);
			}
		}
	}

	/**
	 * <pre>
	 * 处理嵌套数据
	 * 
	 * @param root
	 * @param tempE
	 * @author CamusHuang
	 * @creation 2014-3-5 上午9:48:46
	 * </pre>
	 */
	private static void paramCKInclude2(Element root, Element tempE) {
		int startIndex = root.indexOf(tempE);
		root.removeContent(tempE);

		Document tempdoc = XmlUtil.openXml(tempE.getTextTrim());
		Element temproot = tempdoc.getRootElement();

		ArrayList listE2 = new ArrayList<Element>(temproot.getChildren());
		for (Object temp2 : listE2) {
			Element tempE2 = (Element) temp2;
			temproot.removeContent(tempE2);
			root.addContent(startIndex++, tempE2);
		}
	}

	public static void updateXmlFile(Document ndoc, String xml) {
		XMLOutputter xmlOutputter = new XMLOutputter();
		xmlOutputter.setFormat(Format.getPrettyFormat().setEncoding("utf-8"));
		try {
			xmlOutputter.output(ndoc, new FileOutputStream(xml));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Document转换为字符串
	 */
	public static String doc2String(Document doc) throws Exception {
		Format format = Format.getRawFormat();// .getPrettyFormat();
		format.setEncoding("UTF-8");// 设置xml文件的字符为UTF-8，解决中文问题
		XMLOutputter xmlout = new XMLOutputter(format);
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		xmlout.output(doc, bo);
		return bo.toString("UTF-8");
	}
}
