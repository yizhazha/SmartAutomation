/**
 * 
 */
package fscm.tools.autocal;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fscm.tools.metadata.PTFTest;
import fscm.tools.util.DBInfo;
import fscm.tools.util.DBUtil;
import fscm.tools.util.FileUtil;

/**
 * @author qidai
 *
 */
public class AnalyzePTF {
	static Logger log = LogManager.getLogger(AnalyzePTF.class);

	static String findPath = "";

	/**
	 * @param testList
	 * @param comList
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	void searchPTFTestByCompList(List<PTFTest> testList, Set<String> comList)
			throws SQLException, ClassNotFoundException {
		StringBuilder sql = new StringBuilder();
		if (comList.size() == 0) {
			return;
		}

		log.info("Start Processing Components: " + comList.size() + " Items");
		log.debug("Processing Components: " + comList.toString());

		Iterator<String> it_comp = comList.iterator();
		String compName = "";

		while (it_comp.hasNext()) {
			// for each component, get's reference test , put into cand_test
			sql.delete(0, sql.length());
			compName = it_comp.next();
			// get testname and casename by comp
			sql.append(
					"SELECT DISTINCT TEST.PTTST_NAME,CASE.PTTST_CASE_NAME FROM PSPTTSTCMDLBLVW CMD,PSPTTSTDEFN TEST, PSPTTSTCASEVAL CASE WHERE CMD.PTTST_NAME   =TEST.PTTST_NAME AND CMD.PTTST_NAME =CASE.PTTST_NAME  AND CMD.PTTST_CMD_ID =CASE.PTTST_CMD_ID AND TEST.PTTST_TYPE IN ('S','H') AND CMD.PTTST_CMD_STATUS='A' AND cmd.pnlgrpname ='"
							+ compName + "'" + findPath);
			sql.append(
					" UNION SELECT DISTINCT CMD.PTTST_NAME, CASE.PTTST_CASE_NAME FROM PSPTTSTCMDLBLVW CMD, PSPTTSTDEFN TEST, PSPTTSTCASEVAL CASE WHERE CMD.PTTST_NAME = TEST.PTTST_NAME AND CMD.PTTST_NAME = CASE.PTTST_NAME AND CMD.PTTST_CMD_ID = CASE.PTTST_CMD_ID AND TEST.PTTST_TYPE IN ('S','H') AND CMD.PTTST_CMD_STATUS='A' "
							+ findPath);
			sql.append(" AND CMD.PTTST_CMD_TYPE=35001 AND (CMD.PTTST_CMD_RECOG) IN (");
			sql.append(
					"SELECT DISTINCT TEST.PTTST_NAME FROM PSPTTSTCMDLBLVW CMD,PSPTTSTDEFN TEST, PSPTTSTCASEVAL CASE WHERE CMD.PTTST_NAME =TEST.PTTST_NAME AND CMD.PTTST_NAME =CASE.PTTST_NAME AND CMD.PTTST_CMD_ID =CASE.PTTST_CMD_ID AND TEST.PTTST_TYPE IN ('S','H','L') AND CMD.PTTST_CMD_STATUS='A' AND cmd.pnlgrpname ='"
							+ compName + "'" + findPath);
			sql.append(") ORDER BY PTTST_NAME,PTTST_CASE_NAME");
			log.debug(sql.toString());
			testList.addAll(findPTFTestBySQL(sql.toString()));
		}
		removeDuplicateTest(testList);
		log.debug("___Candidate PTF Tests by Components: " + testList.size());
	}

	/**
	 * Find Test and Library by AE List
	 * 
	 * @param testList
	 * @param libList
	 * @param aeList
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	void searchPTFTestbyPRCSList(List<PTFTest> testList, Set<String> aeList)
			throws SQLException, ClassNotFoundException {
		String sql = "";
		DBUtil testdb = new DBUtil(new DBInfo("TESTDB"));
		List<PTFTest> temp1 = null;
		if (aeList.size() == 0)
			return;
		log.info("Start Finding Process: " + aeList.size() + " Items");

		Iterator<String> it_ae = aeList.iterator();
		List<String> compList = new LinkedList<String>();
		List<String> jobList = new LinkedList<String>();
		String ae_id = "";

		while (it_ae.hasNext()) {
			// for each AE, find its comp
			compList.clear();
			ae_id = it_ae.next();
			sql = "select distinct PNLGRPNAME from ps_prcsdefnpnl where prcsname= '" + ae_id + "'";
			ResultSet rs = testdb.getQueryResult(sql);
			while (rs.next()) {
				compList.add(rs.getString("PNLGRPNAME"));
			}

			jobList = findProcessJobsByAE(testdb, ae_id);

			for (String item : jobList) {
				sql = "select distinct PNLGRPNAME from ps_prcsjobpnl WHERE prcsjobname='" + item + "'";
				ResultSet rs4 = testdb.getQueryResult(sql);
				while (rs4.next()) {
					compList.add(rs4.getString("PNLGRPNAME"));
				}
			}

			removeDuplicateString(compList);
			log.debug("[Process]" + ae_id + " and its Jobs are Called by Components:" + compList.toString());

			Iterator<String> it_comp = compList.iterator();
			String compName = "";
			StringBuilder sql2 = new StringBuilder();

			while (it_comp.hasNext()) {
				// for each comp, find related PTF test
				compName = it_comp.next();
				sql2.delete(0, sql2.length());
				sql2.append(
						"SELECT DISTINCT TEST.PTTST_NAME,CASE.PTTST_CASE_NAME FROM PSPTTSTCMDLBLVW CMD,PSPTTSTDEFN TEST, PSPTTSTCASEVAL CASE WHERE CMD.PTTST_NAME   =TEST.PTTST_NAME AND CMD.PTTST_NAME =CASE.PTTST_NAME  AND CMD.PTTST_CMD_ID =CASE.PTTST_CMD_ID AND TEST.PTTST_TYPE IN ('S','H') AND CMD.PTTST_CMD_STATUS='A' AND CMD.pttst_cmd_type=30100 AND cmd.pnlgrpname ='"
								+ compName + "'" + findPath);
				sql2.append(
						" UNION SELECT DISTINCT CMD.PTTST_NAME, CASE.PTTST_CASE_NAME FROM PSPTTSTCMDLBLVW CMD, PSPTTSTDEFN TEST, PSPTTSTCASEVAL CASE WHERE CMD.PTTST_NAME = TEST.PTTST_NAME AND CMD.PTTST_NAME = CASE.PTTST_NAME AND CMD.PTTST_CMD_ID = CASE.PTTST_CMD_ID AND TEST.PTTST_TYPE IN ('S','H') AND CMD.PTTST_CMD_STATUS='A' "
								+ findPath);
				sql2.append(" AND CMD.PTTST_CMD_TYPE=35001 AND (CMD.PTTST_CMD_RECOG) IN (");
				sql2.append(
						"SELECT DISTINCT TEST.PTTST_NAME FROM PSPTTSTCMDLBLVW CMD,  PSPTTSTDEFN TEST, PSPTTSTCASEVAL CASE  WHERE CMD.PTTST_NAME   =TEST.PTTST_NAME AND CMD.PTTST_NAME =CASE.PTTST_NAME  AND CMD.PTTST_CMD_ID =CASE.PTTST_CMD_ID  AND TEST.PTTST_TYPE IN ('S','H','L')    AND CMD.PTTST_CMD_STATUS='A' AND CMD.pttst_cmd_type=30100 AND cmd.pnlgrpname ='"
								+ compName + "'" + findPath);
				sql2.append(") ORDER BY PTTST_NAME,PTTST_CASE_NAME");
				log.trace(sql2.toString());
				temp1 = findPTFTestBySQL(sql2.toString());
				testList.addAll(temp1);

				if (temp1.size() == 0) {
					Set<String> comp_temp = new HashSet<String>();
					comp_temp.add(compName);
					this.searchPTFTestByCompList(testList, comp_temp);
				}
			}
			removeDuplicateTest(testList);
			log.debug("___Candidate PTF Tests by AE: " + testList.size());
		}
		testdb.closeConnection();
	}

	void searchPTFTestbyRecList(List<PTFTest> testList, HashSet<String> set_rec) {
		if (set_rec.size() == 0)
			return;

		StringBuilder sql = new StringBuilder();
		log.info("Start Processing Records: " + set_rec.size() + " Items");

		Iterator<String> it = set_rec.iterator();
		String recname = "";

		while (it.hasNext()) {
			sql.delete(0, sql.length());
			recname = it.next();
			// get testname and casename by recname
			sql.append(
					"SELECT DISTINCT TEST.PTTST_NAME,CASE.PTTST_CASE_NAME FROM PSPTTSTCMDLBLVW CMD,PSPTTSTDEFN TEST, PSPTTSTCASEVAL CASE WHERE CMD.PTTST_NAME   =TEST.PTTST_NAME AND CMD.PTTST_NAME =CASE.PTTST_NAME  AND CMD.PTTST_CMD_ID =CASE.PTTST_CMD_ID AND TEST.PTTST_TYPE IN ('S','H') AND CMD.PTTST_CMD_STATUS='A' AND cmd.recname ='"
							+ recname + "'" + findPath);
			sql.append(
					" UNION SELECT DISTINCT CMD.PTTST_NAME, CASE.PTTST_CASE_NAME FROM PSPTTSTCMDLBLVW CMD, PSPTTSTDEFN TEST, PSPTTSTCASEVAL CASE WHERE CMD.PTTST_NAME = TEST.PTTST_NAME AND CMD.PTTST_NAME = CASE.PTTST_NAME AND CMD.PTTST_CMD_ID = CASE.PTTST_CMD_ID AND TEST.PTTST_TYPE IN ('S','H') AND CMD.PTTST_CMD_STATUS='A' "
							+ findPath);
			sql.append(" AND CMD.PTTST_CMD_TYPE=35001 AND (CMD.PTTST_CMD_RECOG) IN (");
			sql.append(
					"SELECT DISTINCT TEST.PTTST_NAME FROM PSPTTSTCMDLBLVW CMD,PSPTTSTDEFN TEST, PSPTTSTCASEVAL CASE WHERE CMD.PTTST_NAME =TEST.PTTST_NAME AND CMD.PTTST_NAME =CASE.PTTST_NAME AND CMD.PTTST_CMD_ID =CASE.PTTST_CMD_ID AND TEST.PTTST_TYPE IN ('S','H','L') AND CMD.PTTST_CMD_STATUS='A' AND cmd.recname ='"
							+ recname + "'" + findPath);
			sql.append(") ORDER BY PTTST_NAME,PTTST_CASE_NAME");
			testList.addAll(findPTFTestBySQL(sql.toString()));
		}
		removeDuplicateTest(testList);
		log.debug("___Candidate PTF Tests by Records: " + testList.size());
	}

	void searchPTFTestbyFieldList(List<PTFTest> testList, HashSet<PageRecordField> set_field) {
		if (set_field.size() == 0)
			return;

		StringBuilder sql = new StringBuilder();
		log.info("Start Processing Pages Fields: " + set_field.size() + " Items");
		log.debug(set_field.toString());

		Iterator<PageRecordField> it = set_field.iterator();
		PageRecordField field = null;

		while (it.hasNext()) {
			field = it.next();
			sql.delete(0, sql.length());
			// get testname and casename by recname
			sql.append(
					"SELECT DISTINCT TEST.PTTST_NAME,CASE.PTTST_CASE_NAME FROM PSPTTSTCMDLBLVW CMD,PSPTTSTDEFN TEST, PSPTTSTCASEVAL CASE WHERE CMD.PTTST_NAME   =TEST.PTTST_NAME AND CMD.PTTST_NAME =CASE.PTTST_NAME  AND CMD.PTTST_CMD_ID =CASE.PTTST_CMD_ID AND TEST.PTTST_TYPE IN ('S','H') AND CMD.PTTST_CMD_STATUS='A' AND");
			sql.append(" cmd.pnlname LIKE '" + field.getPage() + "' AND recname LIKE '" + field.getRecord()
					+ "' AND fieldname LIKE '" + field.getField() + "' " + findPath);
			sql.append(
					" UNION SELECT DISTINCT CMD.PTTST_NAME, CASE.PTTST_CASE_NAME FROM PSPTTSTCMDLBLVW CMD, PSPTTSTDEFN TEST, PSPTTSTCASEVAL CASE WHERE CMD.PTTST_NAME = TEST.PTTST_NAME AND CMD.PTTST_NAME = CASE.PTTST_NAME AND CMD.PTTST_CMD_ID = CASE.PTTST_CMD_ID AND TEST.PTTST_TYPE IN ('S','H') AND CMD.PTTST_CMD_STATUS='A' "
							+ findPath);
			sql.append(" AND CMD.PTTST_CMD_TYPE=35001 AND (CMD.PTTST_CMD_RECOG) IN (");
			sql.append(
					"SELECT DISTINCT TEST.PTTST_NAME FROM PSPTTSTCMDLBLVW CMD,PSPTTSTDEFN TEST, PSPTTSTCASEVAL CASE WHERE CMD.PTTST_NAME =TEST.PTTST_NAME AND CMD.PTTST_NAME =CASE.PTTST_NAME AND CMD.PTTST_CMD_ID =CASE.PTTST_CMD_ID AND TEST.PTTST_TYPE IN ('S','H','L') AND CMD.PTTST_CMD_STATUS='A' AND ");
			sql.append(" cmd.pnlname LIKE '" + field.getPage() + "' AND recname LIKE '" + field.getRecord()
					+ "' AND fieldname LIKE '" + field.getField() + "' " + findPath);
			sql.append(") ORDER BY PTTST_NAME, PTTST_CASE_NAME");
			log.trace(sql.toString());
			testList.addAll(findPTFTestBySQL(sql.toString()));
		}
		removeDuplicateTest(testList);
		log.debug("________Candidate PTF Tests by Fields: " + testList.size());
	}

	void searchPTFTestbyPageList(List<PTFTest> testList, HashSet<String> set_page) {
		if (set_page.size() == 0)
			return;

		StringBuilder sql = new StringBuilder();
		log.info("Start Processing Pages: " + set_page.size() + " Items");

		Iterator<String> it = set_page.iterator();
		String page = "";

		while (it.hasNext()) {
			// for each component, get's reference test , put into cand_test
			sql.delete(0, sql.length());
			page = it.next();
			// get testname and casename by recname
			sql.append(
					"SELECT DISTINCT TEST.PTTST_NAME,CASE.PTTST_CASE_NAME FROM PSPTTSTCMDLBLVW CMD,PSPTTSTDEFN TEST, PSPTTSTCASEVAL CASE WHERE CMD.PTTST_NAME   =TEST.PTTST_NAME AND CMD.PTTST_NAME =CASE.PTTST_NAME  AND CMD.PTTST_CMD_ID =CASE.PTTST_CMD_ID AND TEST.PTTST_TYPE IN ('S','H') AND CMD.PTTST_CMD_STATUS='A' AND cmd.pnlname ='"
							+ page + "'" + findPath);
			sql.append(
					" UNION SELECT DISTINCT CMD.PTTST_NAME, CASE.PTTST_CASE_NAME FROM PSPTTSTCMDLBLVW CMD, PSPTTSTDEFN TEST, PSPTTSTCASEVAL CASE WHERE CMD.PTTST_NAME = TEST.PTTST_NAME AND CMD.PTTST_NAME = CASE.PTTST_NAME AND CMD.PTTST_CMD_ID = CASE.PTTST_CMD_ID AND TEST.PTTST_TYPE IN ('S','H') AND CMD.PTTST_CMD_STATUS='A' "
							+ findPath);
			sql.append(" AND CMD.PTTST_CMD_TYPE=35001 AND (CMD.PTTST_CMD_RECOG) IN (");
			sql.append(
					"SELECT DISTINCT TEST.PTTST_NAME FROM PSPTTSTCMDLBLVW CMD,PSPTTSTDEFN TEST, PSPTTSTCASEVAL CASE WHERE CMD.PTTST_NAME =TEST.PTTST_NAME AND CMD.PTTST_NAME =CASE.PTTST_NAME AND CMD.PTTST_CMD_ID =CASE.PTTST_CMD_ID AND TEST.PTTST_TYPE IN ('S','H','L') AND CMD.PTTST_CMD_STATUS='A' AND cmd.pnlname ='"
							+ page + "'" + findPath);
			sql.append(") ORDER BY PTTST_NAME,PTTST_CASE_NAME");
			testList.addAll(findPTFTestBySQL(sql.toString()));
		}
		removeDuplicateTest(testList);
		log.debug("________Candidate PTF Tests by Pages: " + testList.size());

	}

	void searchPTFTestbyMenuList(List<PTFTest> testList, HashSet<String> set_menu) {
		if (set_menu.size() == 0)
			return;

		StringBuilder sql = new StringBuilder();
		log.info("Start Processing Menus: " + set_menu.size() + " Items");

		Iterator<String> it = set_menu.iterator();
		String menu = "";

		while (it.hasNext()) {
			menu = it.next();
			sql.delete(0, sql.length());
			sql.append(
					"SELECT DISTINCT TEST.PTTST_NAME,CASE.PTTST_CASE_NAME FROM PSPTTSTCMDLBLVW CMD,PSPTTSTDEFN TEST, PSPTTSTCASEVAL CASE WHERE CMD.PTTST_NAME   =TEST.PTTST_NAME AND CMD.PTTST_NAME =CASE.PTTST_NAME  AND CMD.PTTST_CMD_ID =CASE.PTTST_CMD_ID AND TEST.PTTST_TYPE IN ('S','H') AND CMD.PTTST_CMD_STATUS='A' AND cmd.menuname ='"
							+ menu + "'" + findPath);
			sql.append(
					" UNION SELECT DISTINCT CMD.PTTST_NAME, CASE.PTTST_CASE_NAME FROM PSPTTSTCMDLBLVW CMD, PSPTTSTDEFN TEST, PSPTTSTCASEVAL CASE WHERE CMD.PTTST_NAME = TEST.PTTST_NAME AND CMD.PTTST_NAME = CASE.PTTST_NAME AND CMD.PTTST_CMD_ID = CASE.PTTST_CMD_ID AND TEST.PTTST_TYPE IN ('S','H') AND CMD.PTTST_CMD_STATUS='A' "
							+ findPath);
			sql.append(" AND CMD.PTTST_CMD_TYPE=35001 AND (CMD.PTTST_CMD_RECOG) IN (");
			sql.append(
					"SELECT DISTINCT TEST.PTTST_NAME FROM PSPTTSTCMDLBLVW CMD,PSPTTSTDEFN TEST, PSPTTSTCASEVAL CASE WHERE CMD.PTTST_NAME =TEST.PTTST_NAME AND CMD.PTTST_NAME =CASE.PTTST_NAME AND CMD.PTTST_CMD_ID =CASE.PTTST_CMD_ID AND TEST.PTTST_TYPE IN ('S','H','L') AND CMD.PTTST_CMD_STATUS='A' AND cmd.menuname ='"
							+ menu + "'" + findPath);
			sql.append(") ORDER BY PTTST_NAME,PTTST_CASE_NAME");
			testList.addAll(findPTFTestBySQL(sql.toString()));

		}
		removeDuplicateTest(testList);
		log.debug("________Candidate PTF Tests by Menu: " + testList.size());
	}

	void searchPTFTestbyJobList(List<PTFTest> testList, HashSet<String> jobList)
			throws ClassNotFoundException, SQLException {
		if (jobList.size() == 0)
			return;

		String sql2 = "";
		StringBuilder sql = new StringBuilder();
		DBUtil testdb = new DBUtil(new DBInfo("TESTDB"));

		log.info("Start Processing Job: " + jobList.size() + " Items");

		List<String> temp = new ArrayList<String>();

		for (String job : jobList) {
			sql2 = "select distinct PRCSJOBNAME from ps_prcsjobitem  WHERE PRCSTYPE='PSJob' AND prcsname='" + job + "'";
			ResultSet rs = testdb.getQueryResult(sql2);
			while (rs.next()) {
				temp.add(rs.getString("PRCSJOBNAME"));
			}
		}
		jobList.addAll(temp);
		temp.clear();

		List<String> compList = new ArrayList<>();
		for (String item : jobList) {
			sql2 = "select distinct PNLGRPNAME from ps_prcsjobpnl WHERE prcsjobname='" + item + "'";
			ResultSet rs4 = testdb.getQueryResult(sql2);
			while (rs4.next()) {
				compList.add(rs4.getString("PNLGRPNAME"));
			}
		}
		testdb.closeConnection();
		HashSet<String> hs3 = new HashSet<String>(compList);
		// log.info("[App Engine]" + ae_id + " and its Jobs are
		// Called by Components:"+ hs3.toString());
		compList.clear();
		compList.addAll(hs3);

		Iterator<String> it_comp = compList.iterator();
		String compName = "";

		while (it_comp.hasNext()) {
			compName = it_comp.next();
			sql.delete(0, sql.length());
			sql.append(
					"SELECT DISTINCT TEST.PTTST_NAME,CASE.PTTST_CASE_NAME FROM PSPTTSTCMDLBLVW CMD,PSPTTSTDEFN TEST, PSPTTSTCASEVAL CASE WHERE CMD.PTTST_NAME   =TEST.PTTST_NAME AND CMD.PTTST_NAME =CASE.PTTST_NAME  AND CMD.PTTST_CMD_ID =CASE.PTTST_CMD_ID AND TEST.PTTST_TYPE IN ('S','H') AND CMD.PTTST_CMD_STATUS='A' AND cmd.pnlgrpname ='"
							+ compName + "'" + findPath);
			sql.append(
					" UNION SELECT DISTINCT CMD.PTTST_NAME, CASE.PTTST_CASE_NAME FROM PSPTTSTCMDLBLVW CMD, PSPTTSTDEFN TEST, PSPTTSTCASEVAL CASE WHERE CMD.PTTST_NAME = TEST.PTTST_NAME AND CMD.PTTST_NAME = CASE.PTTST_NAME AND CMD.PTTST_CMD_ID = CASE.PTTST_CMD_ID AND TEST.PTTST_TYPE IN ('S','H') AND CMD.PTTST_CMD_STATUS='A' "
							+ findPath);
			sql.append(" AND CMD.PTTST_CMD_TYPE=35001 AND (CMD.PTTST_CMD_RECOG) IN (");
			sql.append(
					"SELECT DISTINCT TEST.PTTST_NAME FROM PSPTTSTCMDLBLVW CMD,  PSPTTSTDEFN TEST, PSPTTSTCASEVAL CASE  WHERE CMD.PTTST_NAME   =TEST.PTTST_NAME AND CMD.PTTST_NAME =CASE.PTTST_NAME  AND CMD.PTTST_CMD_ID =CASE.PTTST_CMD_ID  AND TEST.PTTST_TYPE IN ('S','H','L')    AND CMD.PTTST_CMD_STATUS='A' AND cmd.pnlgrpname ='"
							+ compName + "'" + findPath);
			sql.append(") ORDER BY PTTST_NAME,PTTST_CASE_NAME");
			testList.addAll(findPTFTestBySQL(sql.toString()));
		}

		removeDuplicateTest(testList);
		log.debug("________Candidate PTF Tests by Job: " + testList.size());
	}

	/**
	 * @param testdb
	 * @param temp
	 * @param jobList
	 * @param ae_id
	 * @throws SQLException
	 */
	List<String> findProcessJobsByAE(DBUtil testdb, String ae_id) throws SQLException {
		String sql;
		// find Job
		sql = "select distinct PRCSJOBNAME from ps_prcsjobitem  WHERE PRCSTYPE='Application Engine' AND prcsname='"
				+ ae_id + "'";
		ResultSet rs = testdb.getQueryResult(sql);
		List<String> temp = new ArrayList<String>();
		List<String> jobList = new ArrayList<String>();
		while (rs.next()) {
			String job = rs.getString("PRCSJOBNAME");
			temp.add(job);
		}
		log.debug("[Process]" + ae_id + " Called by JOB: " + temp.toString());

		for (String job : temp) {
			sql = "select distinct PRCSJOBNAME from ps_prcsjobitem WHERE PRCSTYPE='PSJob' AND prcsname='" + job + "'";
			ResultSet rs2 = testdb.getQueryResult(sql);
			while (rs2.next()) {
				jobList.add(rs2.getString("PRCSJOBNAME"));
			}
		}

		log.debug("[Process] " + ae_id + " and its Job are Called by Jobs: " + temp.toString());
		return temp;
	}

	/**
	 * @param testList
	 * @param sql
	 */
	List<PTFTest> findPTFTestBySQL(String sql) {

		List<PTFTest> temp = new ArrayList<PTFTest>();
		try {
			DBUtil ptf = new DBUtil(new DBInfo("PTF"));
			ResultSet rs = ptf.getQueryResult(sql);
			while (rs.next()) {
				PTFTest test = new PTFTest();
				if (rs.getString("pttst_name") == null || rs.getString("pttst_name").equals(""))
					continue;
				if (rs.getString("pttst_case_name") == null || rs.getString("pttst_case_name").equals(""))
					throw new Exception("Test Case Name is Empty");
				test.setTest_Name(rs.getString("pttst_name"));
				test.setTest_Case(rs.getString("pttst_case_name"));
				temp.add(test);
			}
			ptf.closeConnection();
			if (temp.size() > 0) {
				removeDuplicateTest(temp);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// log.debug("SQL Results:" + temp.toString());
		return temp;
	}

	boolean computePTFTest(String BugNo, String product) {

		List<PTFTest> testList = new LinkedList<PTFTest>();
		// List<PTFTest> libList = new LinkedList<PTFTest>();

		try {
			convertPTFTestPath(product);

			searchPTFTestByCompList(testList, SmartAnalyze.set_comp);

			searchPTFTestbyPRCSList(testList, SmartAnalyze.set_ae);

			searchPTFTestbyRecList(testList, SmartAnalyze.set_rec);

			searchPTFTestbyFieldList(testList, SmartAnalyze.set_field);

			searchPTFTestbyPageList(testList, SmartAnalyze.set_page);

			searchPTFTestbyMenuList(testList, SmartAnalyze.set_menu);

			searchPTFTestbyJobList(testList, SmartAnalyze.set_job);

			removeDuplicateTest(testList);

			log.debug("[Summary]-Test List after find shell and library]:" + testList.size());
			log.debug("[Summary]-Test List after find shell and library]:" + testList.toString());

			testList.addAll(searchPTFTestByBugNo(BugNo));
			removeDuplicateTest(testList);
			if (testList.size() != 0) {
				SmartAnalyze.set_test = new HashSet<PTFTest>(testList);
				log.info("[Summary]-Finding PTF Test:" + SmartAnalyze.set_test.size());

				return true;
			} else {
				log.warn("[Summary]-Not find any related PTF test.");
			}

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}

	private void convertPTFTestPath(String product) throws IOException {
		String[] scm = new String[] { "INVENTORY", "MOBILE INVENTORY", "ORDER MANAGEMENT", "COST MANAGEMENT",
				"MANUFACTURING", "SUPPLIER SCORECARDING", "PURCHASING", "BILLING", "EBILL PAYMENT" };
		String[] fms = new String[] { "EXPENSES", "ACCOUNTS RECEIVABLE", "GENERAL LEDGER" };
		String relativelyPath = System.getProperty("user.dir");
		String keyword = FileUtil.getProfileString(relativelyPath + "/" + "prodSetup.ini", "PTFProduct", product, "")
				.trim().toUpperCase();
		log.info("Finding Test for Product [" + product + "]");
		if (!product.equals(""))
			product = product.toUpperCase();
		log.debug("Product [" + product + "] PTF Path Keyword:" + keyword);

		if (Arrays.asList(scm).contains(product))
			findPath = " AND (UPPER(test.pttst_parentfolder) like '\\QA AUTOMATION\\SCM\\%" + keyword + "%')";

		if (Arrays.asList(fms).contains(product))
			findPath = " AND (UPPER(test.pttst_parentfolder) like '\\QA AUTOMATION\\FMS\\%" + keyword + "%')";

		if (product.equals(""))
			findPath = " AND (UPPER(test.pttst_parentfolder) like '\\QA AUTOMATION\\SCM\\%' OR UPPER(test.pttst_parentfolder) like '\\QA AUTOMATION\\FMS\\%')";
	}

	void removeDuplicateString(List<String> list) {

		LinkedHashSet<String> set = new LinkedHashSet<String>(list.size());
		set.addAll(list);
		list.clear();
		list.addAll(set);
	}

	/**
	 * @param ptfList
	 * @return
	 */
	private void removeDuplicateTest(List<PTFTest> ptfList) {

		LinkedHashSet<PTFTest> set = new LinkedHashSet<PTFTest>(ptfList.size());
		set.addAll(ptfList);
		ptfList.clear();
		ptfList.addAll(set);
	}

	List<PTFTest> searchPTFTestByBugNo(String bugNo) {
		if (bugNo == null || bugNo.equals(""))
			return null;

		StringBuilder sql = new StringBuilder();
		ResultSet rs = null;
		List<PTFTest> retList = new ArrayList<PTFTest>();

		sql.append(
				"SELECT TS_TEST_ID, TS_USER_37, TS_USER_39 FROM SCM_SCM_92_PRD_DB.TEST WHERE TS_STATUS='Ready' AND TS_TYPE='PTF' AND TS_USER_37 LIKE '%");
		sql.append(bugNo);
		sql.append(
				"%' UNION SELECT TS_TEST_ID, TS_USER_37, TS_USER_39 FROM FMS_FMS_92_PRD_DB.TEST WHERE TS_STATUS='Ready' AND TS_TYPE='PTF' AND TS_USER_37 like '%");
		sql.append(bugNo + "%'");
		try {
			DBUtil alm = new DBUtil(new DBInfo("ALM"));
			rs = alm.getQueryResult(sql.toString());
			if (rs.next()) {
				PTFTest call_test = new PTFTest();
				if (rs.getString("TS_TEST_ID") != null && !rs.getString("TS_TEST_ID").equals("")) {
					call_test.setTest_Name(rs.getString("TS_USER_37"));
					call_test.setTest_Case(rs.getString("TS_USER_39"));
					retList.add(call_test);
				}
			}
			alm.closeConnection();

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return retList;
	}

}
