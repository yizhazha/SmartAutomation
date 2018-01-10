/**
 * 
 */
package fscm.tools.util;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fscm.tools.autocal.AnalyzeALM;
import fscm.tools.metadata.PTFTest;
import fscm.tools.metadata.UOW;

/**
 * @author qidai
 *
 */
/**
 * @author qidai
 *
 */
public class FileUtil {

	public static StringBuilder generateJson(UOW uow, List<PTFTest> testList, AnalyzeALM almtest) {
		StringBuilder json = new StringBuilder();
		json.append("{\"UOW\":\"" + uow.getUOW_ID() + "\",\"BugNo\":\"" + uow.getBugNo() + "\",\"User\":\"" + uow.getUser_ID() + "\",\"Products\":");
		ArrayList<String> product = almtest.prod_name;
		ArrayList<Integer> prodSize = almtest.test_size;
		List<PTFTest> subList = null;
		json.append(arraylist2json(product));
		
		for (int i = 0, j=0; i < product.size(); i++) {
			json.append(",");
			json.append("\"" + product.get(i) + "\":");			
			subList = testList.subList(j, j+prodSize.get(i));
			j=j+prodSize.get(i);			
			json.append(list2json((subList)));
		}
		json.append("}");
		return json;
	}

	private static String arraylist2json(ArrayList<String> list) {
		StringBuilder json = new StringBuilder();
		json.append("[");
		if (list != null && list.size() > 0) {
			for (Object obj : list) {
				json.append(object2json(obj));
				json.append(",");
			}
			json.setCharAt(json.length() - 1, ']');
		} else {
			json.append("]");
		}
		return json.toString();
	}

	public static String getProfileString(String file, String section, String variable, String defaultValue)
			throws IOException {
		String strLine, value = "";
		File aa = new File(file);
		if (aa.exists()) {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
			boolean isInSection = false;
			try {
				while ((strLine = bufferedReader.readLine()) != null) {
					strLine = strLine.trim();
					strLine = strLine.split("[;]")[0];
					Pattern p;
					Matcher m;
					p = Pattern.compile("\\[\\s*.*\\s*\\]");
					m = p.matcher((strLine));
					if (m.matches()) {
						p = Pattern.compile("\\[\\s*" + section + "\\s*\\]");
						m = p.matcher(strLine);
						if (m.matches()) {
							isInSection = true;
						} else {
							isInSection = false;
						}
					}
					if (isInSection == true) {
						strLine = strLine.trim();
						String[] strArray = strLine.split("=");
						if (strArray.length == 1) {
							value = strArray[0].trim();
							if (value.equalsIgnoreCase(variable)) {
								value = "";
								return value;
							}
						} else if (strArray.length == 2) {
							value = strArray[0].trim();
							if (value.equalsIgnoreCase(variable)) {
								value = strArray[1].trim();
								return value;
							}
						} else if (strArray.length > 2) {
							value = strArray[0].trim();
							if (value.equalsIgnoreCase(variable)) {
								value = strLine.substring(strLine.indexOf("=") + 1).trim();
								return value;
							}
						}
					}
				}
			} finally {
				bufferedReader.close();
			}
		}
		return defaultValue;
	}

	public static boolean setProfileString(String file, String section, String variable, String value)
			throws IOException {
		String allLine, strLine, newLine, remarkStr;
		String getValue;
		StringBuffer fileContent = new StringBuffer();
		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
		boolean isInSection = false;
		boolean tempInSection = false;
		try {

			while ((allLine = bufferedReader.readLine()) != null) {
				allLine = allLine.trim();
				if (allLine.split("[;]").length > 1) {
					remarkStr = ";" + allLine.split(";")[1];
				} else {
					remarkStr = "";
				}
				strLine = allLine.split(";")[0];
				Pattern p;
				Matcher m;
				p = Pattern.compile("\\[\\s*.*\\s*\\]");
				m = p.matcher((strLine));
				if (m.matches()) {
					p = Pattern.compile("\\[\\s*" + section + "\\s*\\]");
					m = p.matcher(strLine);
					if (m.matches()) {
						isInSection = true;
						tempInSection = true;
					} else {
						isInSection = false;
					}
				}
				if (isInSection == true) {
					strLine = strLine.trim();
					String[] strArray = strLine.split("=");
					getValue = strArray[0].trim();
					if (getValue.equalsIgnoreCase(variable)) {
						newLine = getValue + " = " + value + " " + remarkStr;
						fileContent.append(newLine + "\r\n");
						while ((allLine = bufferedReader.readLine()) != null) {
							fileContent.append(allLine + "\r\n");
						}
						bufferedReader.close();
						BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, false));
						bufferedWriter.write(fileContent.toString());
						bufferedWriter.flush();
						bufferedWriter.close();
						return true;
					}
				}
				fileContent.append(allLine + "\r\n");
			}
			if (tempInSection == false) {
				bufferedReader.close();
				BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, false));
				bufferedWriter.write(fileContent.toString());
				bufferedWriter.write("[" + section + "]" + "\r\n");
				bufferedWriter.write(variable + " = " + value + "\r\n");
				bufferedWriter.flush();
				bufferedWriter.close();
			} else {
				bufferedReader.close();
				BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, false));
				int indx = fileContent.indexOf("[" + section + "]" + "\r\n");
				indx = indx + section.length() + 4;
				fileContent.insert(indx, variable + " = " + value + "\r\n");
				bufferedWriter.write(fileContent.toString());
				bufferedWriter.flush();
				bufferedWriter.close();
			}
		} catch (IOException ex) {
			throw ex;
		} finally {
			bufferedReader.close();
		}
		return false;
	}

	/**
	 * Create .json file
	 */
	public static boolean createJsonFile(String fileName, StringBuilder json) {

		boolean flag = true;
		String relativelyPath = System.getProperty("user.dir");
		//relativelyPath ="\\\\slcnas463.us.oracle.com\\enterprise\\QEShare\\SmartAutomation";
		String fullPath = relativelyPath + "/Cal_UOW_Output/" + fileName + ".json";
		try {
			// create a new file
			File file = new File(fullPath);
			if (!file.getParentFile().exists()) { // create parent if not exist
				file.getParentFile().mkdirs();
			}
			if (file.exists()) { // delete if exist
				file.delete();
			}
			file.createNewFile();

			// write to file
			Writer write = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
			write.write(json.toString());
			write.flush();
			write.close();
		} catch (Exception e) {
			flag = false;
			e.printStackTrace();
		}

		// return successful or not
		return flag;
	}

	public static String list2json(List<PTFTest> subList) {
		StringBuilder json = new StringBuilder();
		json.append("[");
		if (subList != null && subList.size() > 0) {
			for (Object obj : subList) {
				json.append(object2json(obj));
				json.append(",");
			}
			json.setCharAt(json.length() - 1, ']');
		} else {
			json.append("]");
		}
		return json.toString();
	}

	public static String string2json(String s) {
		if (s == null)
			return "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			switch (ch) {
			case '"':
				sb.append("\\\"");
				break;
			case '\\':
				sb.append("\\\\");
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			case '/':
				sb.append("\\/");
				break;
			default:
				if (ch >= '\u0000' && ch <= '\u001F') {
					String ss = Integer.toHexString(ch);
					sb.append("\\u");
					for (int k = 0; k < 4 - ss.length(); k++) {
						sb.append('0');
					}
					sb.append(ss.toUpperCase());
				} else {
					sb.append(ch);
				}
			}
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	public static String object2json(Object obj) {
		StringBuilder json = new StringBuilder();
		if (obj == null) {
			json.append("\"\"");
		} else if (obj instanceof String || obj instanceof Integer || obj instanceof Float || obj instanceof Boolean
				|| obj instanceof Short || obj instanceof Double || obj instanceof Long || obj instanceof BigDecimal
				|| obj instanceof BigInteger || obj instanceof Byte) {
			json.append("\"").append(string2json(obj.toString())).append("\"");
		} else if (obj instanceof HashSet) {
			json.append(set2json((HashSet<Object>) obj));
		} else {
			json.append(bean2json(obj));
		}
		return json.toString();
	}

	public static String bean2json(Object bean) {
		StringBuilder json = new StringBuilder();
		json.append("{");
		PropertyDescriptor[] props = null;
		try {
			props = Introspector.getBeanInfo(bean.getClass(), Object.class).getPropertyDescriptors();
		} catch (IntrospectionException e) {
		}
		if (props != null) {
			for (int i = 0; i < props.length; i++) {
				try {
					String name = object2json(props[i].getName());
					String value = object2json(props[i].getReadMethod().invoke(bean));
					json.append(name);
					json.append(":");
					json.append(value);
					json.append(",");
				} catch (Exception e) {
				}
			}
			json.setCharAt(json.length() - 1, '}');
		} else {
			json.append("}");
		}
		return json.toString();
	}

	public static StringBuilder set2json(Set<Object> set) {
		StringBuilder json = new StringBuilder();
		json.append("[");
		if (set != null && set.size() > 0) {
			for (Object obj : set) {
				json.append(object2json(obj));
				json.append(",");
			}
			json.setCharAt(json.length() - 1, ']');
		} else {
			json.append("]");
		}
		return json;
	}

	/**
	 * Generate file name using current time
	 * 
	 * @return
	 */
	public static String Date2FileName() {
		Date date = new Date(System.currentTimeMillis());
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		String fileName = dateFormat.format(date);
		return fileName;
	}

}
