/**
 * 
 */
package fscm.tools.autocal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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

/**
 * @author qidai
 *
 */
public class AnalyzePTF {
	static Logger log = LogManager.getLogger(AnalyzePTF.class);

	List<PTFTest> getPTFTestbyLibrary(List<PTFTest> libList) throws SQLException, ClassNotFoundException {
		String sql;
		DBUtil ptf = new DBUtil(new DBInfo("PTF"));
		log.info("Start Processing Library: " + libList.size() + " Items");
		log.debug("______Library List]:" + libList.toString());
		Iterator<PTFTest> it = new HashSet<PTFTest>(libList).iterator();
		List<PTFTest> temp = new LinkedList<PTFTest>();
		String libName = "";
		ResultSet rs_test = null;
		while (it.hasNext()) {
			libName = it.next().getTest_Name();
			sql = "select distinct test.pttst_name,case.pttst_case_name from pspttstcommand cmd, pspttstdefn test, PSPTTSTCASEVAL case where test.pttst_name =cmd.pttst_name AND case.pttst_name =test.pttst_name AND test.pttst_type='S' AND cmd.pttst_cmd_status='A' AND cmd.pttst_cmd_recog ='"
					+ libName + "' ORDER BY test.pttst_name,case.pttst_case_name";
			rs_test = ptf.getQueryResult(sql);
			while (rs_test.next()) {
				PTFTest test = new PTFTest();
				test.setTest_Name(rs_test.getString("pttst_name"));
				test.setTest_Case(rs_test.getString("pttst_case_name"));
				temp.add(test);
			}
		}
		ptf.closeConnection();
		removeDuplicate(temp);
		log.info("______Finding Test by Library:" + temp.size());
		log.trace("______Tests by Library:" + temp.toString());
		return temp;
	}

	List<PTFTest> getPTFShellByTest(List<PTFTest> testList) throws SQLException {
		log.info("Start Finding Shell Test: ");
		List<PTFTest> retList = new ArrayList<PTFTest>();
		List<PTFTest> temp = new ArrayList<PTFTest>();
		String sql = "";
		String testName = "";
		Iterator<PTFTest> it = new HashSet<PTFTest>(testList).iterator();

		while (it.hasNext()) {
			testName = it.next().getTest_Name();
			sql = "select distinct test.pttst_name,case.pttst_case_name from pspttstcommand cmd, pspttstdefn test, PSPTTSTCASEVAL case "
					+ "WHERE TEST.PTTST_NAME =CMD.PTTST_NAME AND CASE.PTTST_NAME=TEST.PTTST_NAME AND CASE.PTTST_CMD_ID =CMD.PTTST_CMD_ID AND test.pttst_type in ('H')"
					+ " AND cmd.pttst_cmd_type=35001 AND cmd.pttst_cmd_status='A' AND cmd.PTTST_CMD_RECOG ='" + testName
					+ "' ORDER BY test.pttst_name,case.pttst_case_name";
			temp = findPTFTestBySQL(sql);
			retList.addAll(temp);
		}
		removeDuplicate(retList);
		log.info("Finding Shell Test:" + retList.size());
		log.debug("______Shell List:" + retList.toString());
		return retList;
	}

	/**
	 * 
	 * Find Test and Library by Component
	 * 
	 * @param testList
	 * @param libList
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	void searchPTFTestByCompList(List<PTFTest> testList, List<PTFTest> libList, Set<String> comList)
			throws SQLException, ClassNotFoundException {

		String sql = "";
		if (comList.size() == 0) {
			return;
		}

		log.info("Start Processing Components: " + comList.size() + " Items");
		log.trace("Processing Components: " + comList.toString());

		Iterator<String> it_comp = comList.iterator();
		String compName = "";

		while (it_comp.hasNext()) {
			// for each component, get's reference test , put into cand_test
			compName = it_comp.next();
			// get testname and casename by comp
			sql = "select distinct test.pttst_name,case.pttst_case_name from pspttstcommand cmd, pspttstdefn test, PSPTTSTCASEVAL case "
					+ "WHERE TEST.PTTST_NAME =CMD.PTTST_NAME AND CASE.PTTST_NAME=TEST.PTTST_NAME AND CASE.PTTST_CMD_ID =CMD.PTTST_CMD_ID AND test.pttst_type in ('S','H')"
					+ " AND cmd.pttst_cmd_status='A' AND cmd.pnlgrpname ='" + compName
					+ "' ORDER BY test.pttst_name,case.pttst_case_name";
			testList.addAll(findPTFTestBySQL(sql));

			// get library by component

			sql = "select distinct test.pttst_name,case.pttst_case_name from pspttstcommand cmd, pspttstdefn test, PSPTTSTCASEVAL case where test.pttst_name =cmd.pttst_name AND case.pttst_name =test.pttst_name AND test.pttst_type='L' AND cmd.pttst_cmd_status='A' AND cmd.pnlgrpname ='"
					+ compName + "'";
			libList.addAll(findPTFTestBySQL(sql));
		}
		removeDuplicate(testList);
		log.debug("___Candidate PTF Tests by Components: " + testList.size());
		removeDuplicate(libList);
		log.debug("___Candidate PTF Library by Components: " + libList.size());
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
	void searchPTFTestbyAEList(List<PTFTest> testList, List<PTFTest> libList, Set<String> aeList)
			throws SQLException, ClassNotFoundException {
		String sql = "";
		DBUtil testdb = new DBUtil(new DBInfo("TESTDB"));
		List<PTFTest> temp1 = null;
		List<PTFTest> temp2 = null;
		if (aeList.size() == 0)
			return;
		log.info("Start Processing App Engine: " + aeList.size() + " Items");

		Iterator<String> it_ae = aeList.iterator();
		List<String> compList = new ArrayList<String>();
		List<String> jobList = new ArrayList<String>();
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
			// log.debug("[AE]" + ae_id + " Called by Component: " +
			// compList.toString());

			jobList = findProcessJobsByAE(testdb, ae_id);

			for (String item : jobList) {
				sql = "select distinct PNLGRPNAME from ps_prcsjobpnl WHERE prcsjobname='" + item + "'";
				ResultSet rs4 = testdb.getQueryResult(sql);
				while (rs4.next()) {
					compList.add(rs4.getString("PNLGRPNAME"));
				}
			}
			HashSet<String> hs3 = new HashSet<String>(compList);
			log.debug("[App Engine]" + ae_id + " and its Jobs are Called by Components:" + hs3.toString());
			compList.clear();
			compList.addAll(hs3);

			Iterator<String> it_comp = compList.iterator();
			String compName = "";

			while (it_comp.hasNext()) {
				// for each comp, find related PTF test and library
				compName = it_comp.next();
				sql = "select distinct test.pttst_name,case.pttst_case_name from pspttstcommand cmd, pspttstdefn test, PSPTTSTCASEVAL case "
						+ "WHERE TEST.PTTST_NAME =CMD.PTTST_NAME AND CASE.PTTST_NAME=TEST.PTTST_NAME AND CASE.PTTST_CMD_ID =CMD.PTTST_CMD_ID AND test.pttst_type in ('S','H')"
						+ " AND cmd.pttst_cmd_type=30100 AND cmd.pttst_cmd_status='A' AND cmd.pnlgrpname ='" + compName
						+ "' ORDER BY test.pttst_name,case.pttst_case_name";
				temp1 = findPTFTestBySQL(sql);
				testList.addAll(temp1);

				sql = "select distinct test.pttst_name,case.pttst_case_name from pspttstcommand cmd, pspttstdefn test, PSPTTSTCASEVAL case where test.pttst_name =cmd.pttst_name AND case.pttst_name =test.pttst_name AND test.pttst_type='L' AND cmd.pttst_cmd_type=30100 AND cmd.pttst_cmd_status='A' AND cmd.pnlgrpname ='"
						+ compName + "'";
				temp2 = findPTFTestBySQL(sql);
				libList.addAll(temp2);

				if (temp1.size() == 0 && temp2.size() == 0) {
					Set<String> comp_temp = new HashSet<String>();
					comp_temp.add(compName);
					this.searchPTFTestByCompList(testList, libList, comp_temp);
				}
			}
			
			removeDuplicate(testList);
			log.debug("___Candidate PTF Tests by AE: " + testList.size());
			removeDuplicate(libList);
			log.debug("___Candidate PTF Library by AE: " + libList.size());
		}
		testdb.closeConnection();
	}

	void searchPTFTestbyRecList(List<PTFTest> testList, List<PTFTest> libList, HashSet<String> set_rec) {
		if (set_rec.size() == 0)
			return;

		String sql = "";
		log.info("Start Processing Records: " + set_rec.size() + " Items");

		Iterator<String> it_comp = set_rec.iterator();
		String recname = "";

		while (it_comp.hasNext()) {
			// for each component, get's reference test , put into cand_test
			recname = it_comp.next();
			// get testname and casename by recname
			sql = "select distinct test.pttst_name,case.pttst_case_name from pspttstcommand cmd, pspttstdefn test, PSPTTSTCASEVAL case "
					+ "WHERE TEST.PTTST_NAME =CMD.PTTST_NAME AND CASE.PTTST_NAME=TEST.PTTST_NAME AND CASE.PTTST_CMD_ID =CMD.PTTST_CMD_ID AND test.pttst_type in ('S','H')"
					+ " AND cmd.pttst_cmd_status='A' AND cmd.recname ='" + recname
					+ "' ORDER BY test.pttst_name,case.pttst_case_name";
			testList.addAll(findPTFTestBySQL(sql));

			// get library by recname
			sql = "select distinct test.pttst_name,case.pttst_case_name from pspttstcommand cmd, pspttstdefn test, PSPTTSTCASEVAL case where test.pttst_name =cmd.pttst_name AND case.pttst_name =test.pttst_name AND test.pttst_type='L' AND cmd.pttst_cmd_status='A' AND cmd.recname ='"
					+ recname + "'";
			libList.addAll(findPTFTestBySQL(sql));
		}
		removeDuplicate(testList);
		log.debug("___Candidate PTF Tests by Records: " + testList.size());
		removeDuplicate(libList);
		log.debug("___Candidate PTF Library by Records: " + libList.size());

	}

	void searchPTFTestbyFieldList(List<PTFTest> testList, List<PTFTest> libList, HashSet<PageRecordField> set_field) {
		if (set_field.size() == 0)
			return;

		String sql = "";
		log.info("Start Processing Pages Fields: " + set_field.size() + " Items");
		log.trace(set_field.toString());

		Iterator<PageRecordField> it = set_field.iterator();
		PageRecordField field = null;

		while (it.hasNext()) {
			// for each fieldname, get's reference test , put into cand_test
			field = it.next();
			// get testname and casename by recname
			sql = "select distinct test.pttst_name,case.pttst_case_name from pspttstcommand cmd, pspttstdefn test, PSPTTSTCASEVAL case "
					+ "WHERE TEST.PTTST_NAME =CMD.PTTST_NAME AND CASE.PTTST_NAME=TEST.PTTST_NAME AND CASE.PTTST_CMD_ID =CMD.PTTST_CMD_ID AND test.pttst_type in ('S','H')"
					+ " AND cmd.pttst_cmd_status='A' AND cmd.pnlname LIKE '" + field.getPage() + "' AND recname LIKE '"
					+ field.getRecord() + "' AND fieldname LIKE '" + field.getField()
					+ "' ORDER BY test.pttst_name,case.pttst_case_name";
			testList.addAll(findPTFTestBySQL(sql));

			// get library by recname
			sql = "select distinct test.pttst_name,case.pttst_case_name from pspttstcommand cmd, pspttstdefn test, PSPTTSTCASEVAL case where test.pttst_name =cmd.pttst_name AND case.pttst_name =test.pttst_name AND test.pttst_type='L' AND cmd.pttst_cmd_status='A' AND cmd.pnlname LIKE '"
					+ field.getPage() + "' AND recname LIKE '" + field.getRecord() + "' AND fieldname LIKE '"
					+ field.getField() + "'";
			libList.addAll(findPTFTestBySQL(sql));
		}
		HashSet<PTFTest> hs = new HashSet<PTFTest>(testList);
		testList.clear();
		testList.addAll(hs);
		log.debug("___Candidate PTF Tests by Fields: " + hs.size());
		HashSet<PTFTest> hs2 = new HashSet<PTFTest>(libList);
		libList.clear();
		libList.addAll(hs2);
		log.debug("___Candidate PTF Library by Fields: " + hs2.size());

	}

	void searchPTFTestbyPageList(List<PTFTest> testList, List<PTFTest> libList, HashSet<String> set_page) {
		if (set_page.size() == 0)
			return;

		String sql = "";
		log.info("Start Processing Pages: " + set_page.size() + " Items");

		Iterator<String> it_comp = set_page.iterator();
		String page = "";

		while (it_comp.hasNext()) {
			// for each component, get's reference test , put into cand_test
			page = it_comp.next();
			// get testname and casename by recname
			sql = "select distinct test.pttst_name,case.pttst_case_name from pspttstcommand cmd, pspttstdefn test, PSPTTSTCASEVAL case "
					+ "WHERE TEST.PTTST_NAME =CMD.PTTST_NAME AND CASE.PTTST_NAME=TEST.PTTST_NAME AND CASE.PTTST_CMD_ID =CMD.PTTST_CMD_ID AND test.pttst_type in ('S','H')"
					+ " AND cmd.pttst_cmd_status='A' AND cmd.pnlname ='" + page
					+ "' ORDER BY test.pttst_name,case.pttst_case_name";
			testList.addAll(findPTFTestBySQL(sql));

			// get library by recname
			sql = "select distinct test.pttst_name,case.pttst_case_name from pspttstcommand cmd, pspttstdefn test, PSPTTSTCASEVAL case where test.pttst_name =cmd.pttst_name AND case.pttst_name =test.pttst_name AND test.pttst_type='L' AND cmd.pttst_cmd_status='A' AND cmd.pnlname ='"
					+ page + "'";
			libList.addAll(findPTFTestBySQL(sql));
		}
		HashSet<PTFTest> hs = new HashSet<PTFTest>(testList);
		testList.clear();
		testList.addAll(hs);
		log.debug("___Candidate PTF Tests by Pages: " + hs.size());
		HashSet<PTFTest> hs2 = new HashSet<PTFTest>(libList);
		libList.clear();
		libList.addAll(hs2);
		log.debug("___Candidate PTF Library by Pages: " + hs2.size());

	}

	void searchPTFTestbyMenuList(List<PTFTest> testList, List<PTFTest> libList, HashSet<String> set_menu) {
		if (set_menu.size() == 0)
			return;

		String sql = "";
		log.info("Start Processing Menus: " + set_menu.size() + " Items");

		Iterator<String> it = set_menu.iterator();
		String menu = "";

		while (it.hasNext()) {
			// for each component, get's reference test , put into cand_test
			menu = it.next();
			// get testname and casename by recname
			sql = "select distinct test.pttst_name,case.pttst_case_name from pspttstcommand cmd, pspttstdefn test, PSPTTSTCASEVAL case "
					+ "WHERE TEST.PTTST_NAME =CMD.PTTST_NAME AND CASE.PTTST_NAME=TEST.PTTST_NAME AND CASE.PTTST_CMD_ID =CMD.PTTST_CMD_ID AND test.pttst_type in ('S','H')"
					+ " AND cmd.pttst_cmd_status='A' AND cmd.menuname ='" + menu
					+ "' ORDER BY test.pttst_name,case.pttst_case_name";
			testList.addAll(findPTFTestBySQL(sql));

			// get library by recname
			sql = "select distinct test.pttst_name,case.pttst_case_name from pspttstcommand cmd, pspttstdefn test, PSPTTSTCASEVAL case where test.pttst_name =cmd.pttst_name AND case.pttst_name =test.pttst_name AND test.pttst_type='L' AND cmd.pttst_cmd_status='A' AND cmd.menuname ='"
					+ menu + "'";
			libList.addAll(findPTFTestBySQL(sql));
		}
		HashSet<PTFTest> hs = new HashSet<PTFTest>(testList);
		testList.clear();
		testList.addAll(hs);
		log.debug("___Candidate PTF Tests by Menu: " + hs.size());
		HashSet<PTFTest> hs2 = new HashSet<PTFTest>(libList);
		libList.clear();
		libList.addAll(hs2);
		log.debug("___Candidate PTF Library by Menu: " + hs2.size());

	}

	void searchPTFTestbyJobList(List<PTFTest> testList, List<PTFTest> libList, HashSet<String> jobList)
			throws ClassNotFoundException, SQLException {
		if (jobList.size() == 0)
			return;

		String sql = "";
		DBUtil testdb = new DBUtil(new DBInfo("TESTDB"));

		log.info("Start Processing Job: " + jobList.size() + " Items");

		List<String> temp = new ArrayList<String>();

		for (String job : jobList) {
			sql = "select distinct PRCSJOBNAME from ps_prcsjobitem  WHERE PRCSTYPE='PSJob' AND prcsname='" + job + "'";
			ResultSet rs = testdb.getQueryResult(sql);
			while (rs.next()) {
				temp.add(rs.getString("PRCSJOBNAME"));
			}
		}
		jobList.addAll(temp);
		temp.clear();

		List<String> compList = new ArrayList<>();
		for (String item : jobList) {
			sql = "select distinct PNLGRPNAME from ps_prcsjobpnl WHERE prcsjobname='" + item + "'";
			ResultSet rs4 = testdb.getQueryResult(sql);
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
			sql = "select distinct test.pttst_name,case.pttst_case_name from pspttstcommand cmd, pspttstdefn test, PSPTTSTCASEVAL case "
					+ "WHERE TEST.PTTST_NAME =CMD.PTTST_NAME AND CASE.PTTST_NAME=TEST.PTTST_NAME AND CASE.PTTST_CMD_ID =CMD.PTTST_CMD_ID AND test.pttst_type in ('S','H')"
					+ " AND cmd.pttst_cmd_type=30100 AND cmd.pttst_cmd_status='A' AND cmd.pnlgrpname ='" + compName
					+ "' ORDER BY test.pttst_name,case.pttst_case_name";
			testList.addAll(findPTFTestBySQL(sql));

			// get library by component
			sql = "select distinct test.pttst_name,case.pttst_case_name from pspttstcommand cmd, pspttstdefn test, PSPTTSTCASEVAL case where test.pttst_name =cmd.pttst_name AND case.pttst_name =test.pttst_name AND test.pttst_type='L' AND cmd.pttst_cmd_type=30100 AND cmd.pttst_cmd_status='A' AND cmd.pnlgrpname ='"
					+ compName + "'";
			libList.addAll(findPTFTestBySQL(sql));
		}
		HashSet<PTFTest> hs = new HashSet<PTFTest>(testList);
		testList.clear();
		testList.addAll(hs);
		log.debug("___Candidate PTF Tests by Job: " + hs.size());
		HashSet<PTFTest> hs2 = new HashSet<PTFTest>(libList);
		libList.clear();
		libList.addAll(hs2);
		log.debug("___Candidate PTF Library by Job: " + hs2.size());
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
				removeDuplicate(temp);
			}
			// log.debug("SQL:" + sql.toString());
			// log.debug("SQL Results:" + temp.toString());
			return temp;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return temp;
	}

	boolean computePTFTest(String BugNo) {

		List<PTFTest> testList = new LinkedList<PTFTest>();
		List<PTFTest> libList = new LinkedList<PTFTest>();

		try {
			// get matched library and test
			searchPTFTestByCompList(testList, libList, SmartAnalyze.set_comp);

			searchPTFTestbyAEList(testList, libList, SmartAnalyze.set_ae);

			searchPTFTestbyRecList(testList, libList, SmartAnalyze.set_rec);

			searchPTFTestbyFieldList(testList, libList, SmartAnalyze.set_field);

			searchPTFTestbyPageList(testList, libList, SmartAnalyze.set_page);

			searchPTFTestbyMenuList(testList, libList, SmartAnalyze.set_menu);

			searchPTFTestbyJobList(testList, libList, SmartAnalyze.set_job);
			removeDuplicate(libList);

			if (libList.size() != 0) {
				testList.addAll(getPTFTestbyLibrary(libList));
			}
			removeDuplicate(testList);
			
			if (testList.size() != 0) {
				testList.addAll(getPTFShellByTest(testList));
				removeDuplicate(testList);
				log.info("[Summary]-Test List after find shell and library]:" + testList.size());
				log.trace("[Summary]-Test List after find shell and library]:" + testList.toString());
			}

			testList.addAll(searchPTFTestByBugNo(BugNo));
			removeDuplicate(testList);
			if (testList.size() != 0) {
				SmartAnalyze.set_test = new HashSet<PTFTest>(testList);
				log.info("[Summary]-Finding PTF Test:" + SmartAnalyze.set_test.size());

				return true;
			} else {
				log.warn("Not find any related PTF test.");
			}

		} catch (Exception e) {

			e.printStackTrace();
			return false;
		}
		return false;
	}

	/**
	 * @param ptfList
	 * @return
	 */
	private void removeDuplicate(List<PTFTest> ptfList) {
	
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
