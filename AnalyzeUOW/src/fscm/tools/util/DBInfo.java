/**
 * 
 */
package fscm.tools.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author qidai
 *
 */
public class DBInfo {
	String url;
	String user;
	String password;

	public DBInfo(String name) {
		this.getDBInfo(name);
	}

	private void getDBInfo(String env) {

		String ip = "";
		String port = "";
		String db = "";
		try {
			String relativelyPath = System.getProperty("user.dir");
			
			// System.out.println(relativelyPath);
			ip = FileUtil.getProfileString(relativelyPath + "/" + "DB_Util.ini", env, "IP", "");
			port = FileUtil.getProfileString(relativelyPath + "/" + "DB_Util.ini", env, "port", "");
			db = FileUtil.getProfileString(relativelyPath + "/" + "DB_Util.ini", env, "DBName", "");
			this.user = FileUtil.getProfileString(relativelyPath + "/" + "DB_Util.ini", env, "user", "");
			this.password = FileUtil.getProfileString(relativelyPath + "/" + "DB_Util.ini", env, "password", "");
		} catch (IOException e) {
			System.out.print("Can't Read ConfigFile");
			e.printStackTrace();
		}
		// this.url = "jdbc:oracle:thin:@"+db+".oradev.oraclecorp.com";
		this.url = "jdbc:oracle:thin:@//" + ip + ':' + port + "/" + db;
		if (env == "TESTDB" || env == "PTF" || env == "PTF2") {
			this.url = "jdbc:oracle:thin:@" + ip + ":" + port + ":" + db;
			//
		}
		//System.out.println("......[DEBUG]: Connection Database: "+db+" using URL: "+url);
	}

	public String getTNSNamesStr(String tnsFileName) {
		String output = ")";
		String fileLine;
		InputStream in = null;
		BufferedReader sr = null;
		try {
			in = new FileInputStream(tnsFileName);
			sr = new BufferedReader(new InputStreamReader(in));
			while ((fileLine = sr.readLine()) != null) {
				if (fileLine.length() > 0 && fileLine.indexOf("#") == -1) {
					output += fileLine.trim();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				sr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		output = output.replaceAll(" ", "").toLowerCase();
		return output;
	}

	public List<Map<String, String>> loadTNSEntry(String tnsFileName) {

		String tNSNamesStr = this.getTNSNamesStr(tnsFileName);
		Pattern ptn = Pattern.compile("(\\)\\w+|host|port|service_name)\\=(\\(|\\d+\\.\\d+\\.\\d+\\.\\d+|\\d+|\\w+)");
		Matcher match = ptn.matcher(tNSNamesStr);
		int start = 0;
		int i = 0;
		Map<String, String> map = new LinkedHashMap<String, String>();
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		while (match.find(start)) {
			String str = tNSNamesStr.substring(match.start(), match.end()).replace(")", "");
			String[] strs = str.split("=");
			if ("(".equals(strs[1])) {
				if (i != 0) {
					list.add(map);
					map = new LinkedHashMap<String, String>();
				}
				map.put("sid", strs[0]);
			} else {
				if (map.containsKey(strs[0])) {
					map.put(strs[0] + "1", strs[1]);
				} else {
					map.put(strs[0], strs[1]);
				}
			}
			start = match.end();
			i++;
		}
		list.add(map);
		return list;
	}

	/**
	 * 
	 * @return Database host
	 * 
	 */
	public String[] loadTNSEntryHost() {
		List<Map<String, String>> list = this.loadTNSEntry("");
		List<String> tsnNameSID = new ArrayList<String>();
		for (Map<String, String> mp : list) {
			Set<String> set = mp.keySet();
			Iterator<String> it = set.iterator();
			while (it.hasNext()) {
				String key = it.next().toString();
				String value = mp.get(key).toString();
				if ("sid".equals(key)) {
					tsnNameSID.add(value.toUpperCase());
				}
			}
		}
		String[] str = new String[tsnNameSID.size()];
		return tsnNameSID.toArray(str);
	}

	/**
	 * @param testDB
	 * @param loginURL
	 */
	public static boolean setDBInfo(String section, String dbName) {
		// Update ConfigFile
		String relativelyPath = System.getProperty("user.dir");
		String tnsfile = System.getenv("TNS_ADMIN") + "/tnsnames.ora";
		String key = dbName + ".oradev.oraclecorp.com";
		String line = null;
		String testDBURL = null;
		try {
			BufferedReader in = new BufferedReader(new FileReader(tnsfile));
			while ((line = in.readLine()) != null) {
				line = line.replace(" ", "");
				if (line.indexOf(key) != -1) {
					for (int i = 1; i <= 6; i++) {
						line = in.readLine().replace(" ", "");
						int start = line.indexOf("HOST=");
						if (start != -1) {
							testDBURL = line.substring(line.indexOf("=") + 1, line.indexOf(")"));
							break;
						}
					}
					break;

				}

			}

			in.close();

			if (!dbName.equals("") && !testDBURL.equals("")) {
				FileUtil.setProfileString(relativelyPath + "/" + "DB_Util.ini", section, "DBName", dbName);
				FileUtil.setProfileString(relativelyPath + "/" + "DB_Util.ini", section, "IP", testDBURL);
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}
}
