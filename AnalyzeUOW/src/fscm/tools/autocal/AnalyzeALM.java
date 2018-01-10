package fscm.tools.autocal;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fscm.tools.metadata.ALMTest;
import fscm.tools.metadata.PTFTest;
import fscm.tools.util.DBInfo;
import fscm.tools.util.DBUtil;
import fscm.tools.util.FileUtil;

public class AnalyzeALM {
	static Logger log = LogManager.getLogger(AnalyzeALM.class);
	public ArrayList<String> prod_name = new ArrayList<String>();
	public ArrayList<Integer> test_size = new ArrayList<Integer>();

	/**
	 * @param prodName
	 * @return PTF Test List after filtering from ALM
	 */
	List<PTFTest> filterTestFromALM(String prodName) {
		try {
			StringBuffer sql = new StringBuffer();
			DBUtil ptf = new DBUtil(new DBInfo("ALM"));
			List<PTFTest> delList = new ArrayList<PTFTest>();

			Iterator<PTFTest> it = SmartAnalyze.set_test.iterator();

			while (it.hasNext()) {
				PTFTest test = it.next();
				sql.delete(0, sql.length());
				sql.append(
						"SELECT TS_TEST_ID FROM SCM_SCM_92_PRD_DB.TEST WHERE ts_status='Ready' and ts_type='PTF' and ts_user_08='Complete' and ts_user_37='");
				sql.append(test.getTest_Name());
				if (prodName != null && !prodName.equals("*")) {
					sql.append("' AND TS_USER_03='" + prodName);
				}
				sql.append("' AND ts_user_39='");

				sql.append(test.getTest_Case());
				sql.append(
						"' UNION SELECT TS_TEST_ID FROM FMS_FMS_92_PRD_DB.TEST where ts_status='Ready' and ts_type='PTF' and ts_user_08='Complete' and ts_user_37='");
				sql.append(test.getTest_Name());
				if (prodName != null && !prodName.equals("*")) {
					sql.append("' AND TS_USER_03='" + prodName);
				}
				sql.append("' AND ts_user_39='");
				sql.append(test.getTest_Case());
				sql.append("'");

				ResultSet rs_test = ptf.getQueryResult(sql.toString());
				if (!rs_test.next()) {
					delList.add(test);
				}
			}
			ptf.closeConnection();
			SmartAnalyze.set_test.removeAll(delList);

			// find product, order by product name, test id
			if (SmartAnalyze.set_test.size() == 0)
				return null;

			List<ALMTest> alm_tests = getALMTestList(SmartAnalyze.set_test);
			log.debug("[Summary]-ALM Test Matched]:");
			log.debug("______Product:" + prod_name.toString());
			log.debug("______PTF Test:" + alm_tests.size() + test_size.toString());

			// Version 1.1 Add Compute Pre-req PTF
			// log.info("B. Finding PTF Test Pre-req");
			// alm_tests = getPrereqList(alm_tests);
			// log.debug("[Summary-PTF Test with Pre-req]:" + alm_tests.size());
			// alm_tests = addGeneralSetup(alm_tests);
			// log.info("[Summary-PTF Test with Pre-req]:" + alm_tests.size());

			return convertALMtoPTF(alm_tests);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 
	 * @param alm_tests
	 * @throws IOException
	 */
	private List<ALMTest> addGeneralSetup(List<ALMTest> alm_tests) throws Exception {
		String relativelyPath = System.getProperty("user.dir");
		List<ALMTest> current = new LinkedList<ALMTest>(alm_tests);
		String testName = "";
		String testcaseName = "";
		if (alm_tests.size() > 0) {

			for (int i = 0; i < this.prod_name.size(); i++) {
				String prod = this.prod_name.get(i);
				String general = FileUtil
						.getProfileString(relativelyPath + "/" + "prodSetup.ini", "ProductGeneralSetup", prod, "")
						.trim();
				log.debug("Product [" + prod + "] General Setup:" + general);
				if (general != null && !general.equals("")) {
					testName = general.substring(general.indexOf("+") + 1, general.lastIndexOf("+")).trim();
					testcaseName = general.substring(general.lastIndexOf("+") + 1).trim();
					ALMTest head = new ALMTest();
					head.setTest_ID(general.substring(0, general.indexOf("+")));
					head.setPTF_Test(testName);
					head.setPTF_Test_Case(testcaseName);
					int first_in_prod = firstProductTestPos(i, this.test_size);
					if (first_in_prod < 0)
						throw new Exception("Can't find the first Test in subProduct:" + prod);
					if (current.contains(head)) {
						int dup = current.indexOf(head);
						current.remove(dup);
					}
					current.add(first_in_prod, head);
					this.test_size.set(i, this.test_size.get(i) + 1);
				}
			}
		}
		return current;
	}

	LinkedList<ALMTest> getALMTestList(HashSet<PTFTest> testset) {

		LinkedList<ALMTest> list = new LinkedList<ALMTest>();
		list.addAll(getALMTestByProject("SCM_SCM_92_PRD_DB", testset));
		list.addAll(getALMTestByProject("FMS_FMS_92_PRD_DB", testset));
		return list;
	}

	List<ALMTest> getALMTestByProject(String project, HashSet<PTFTest> testset) {
		// if(testset.size()==0) return null;
		StringBuilder col = new StringBuilder();

		List<ALMTest> temp = new ArrayList<ALMTest>();
		Iterator<PTFTest> it = testset.iterator();

		ResultSet rs = null;
		ArrayList<String> prod = new ArrayList<String>();
		String prodname = "";
		ArrayList<Integer> size = new ArrayList<Integer>();
		try {
			DBUtil alm = new DBUtil(new DBInfo("ALM"));
			col.append("SELECT TS_TEST_ID, TS_USER_03, TS_USER_37,ts_user_39 FROM " + project
					+ ".TEST where ts_test_id in (select ts_test_id from " + project + ".TEST WHERE ");
			while (it.hasNext()) {
				PTFTest test = it.next();
				String temp1 = "(ts_user_37='" + test.getTest_Name() + "' AND ts_user_39='" + test.getTest_Case()
						+ "')";
				col.append(temp1);
				if (it.hasNext())
					col.append(" OR ");
			}

			col.append(") ORDER BY TS_USER_03, TS_USER_37");

			// log.info(col.toString());
			rs = alm.getQueryResult(col.toString());

			int i = 1;
			int flag = -1;
			while (rs.next()) {
				ALMTest almtst = new ALMTest();
				prodname = rs.getString("TS_USER_03");
				almtst.setProduct(prodname);
				almtst.setPTF_Test(rs.getString("TS_USER_37"));
				almtst.setPTF_Test_Case(rs.getString("TS_USER_39"));
				almtst.setTest_ID(rs.getString("TS_TEST_ID"));

				temp.add(almtst);
				log.trace("ALM Test:" + almtst.getTest_ID() + "-" + almtst.getProduct());
				if (this.prod_name.contains(prodname)) {
					i = this.test_size.get(this.prod_name.indexOf(prodname));
					i++;
				} else if (prod.contains(prodname)) {
					i++;
				} else {
					prod.add(prodname);
					flag++;
				}
				if (flag > 0) {
					size.add(i);
					i = 1;
					flag = 0;
				}
			}
			if (flag > -1)
				size.add(i);
			alm.closeConnection();
		} catch (Exception e) {

			e.printStackTrace();
		}
		this.prod_name.addAll(prod);
		this.test_size.addAll(size);

		return temp;
	}

	LinkedList<PTFTest> convertALMtoPTF(List<ALMTest> alm) {
		LinkedList<PTFTest> temp = new LinkedList<PTFTest>();

		for (ALMTest test : alm) {
			PTFTest ptftest = new PTFTest();
			ptftest.setTest_Name(test.getPTF_Test());
			ptftest.setTest_Case(test.getPTF_Test_Case());
			if (temp.contains(ptftest)) {
				String prod = test.getProduct();
				int i = prod_name.indexOf(prod);
				int oldsize = test_size.get(i);
				if (oldsize > 1)
					test_size.set(i, oldsize - 1);
			} else {
				temp.add(ptftest);
			}
		}

		log.info("[Summary]-PTF Test after filter ]:" + temp.size());
		log.info("______Product:" + prod_name.toString());
		log.info("______PTF Test:" + test_size.toString());
		log.trace("Finally PTF Test: " + temp.toString());
		return temp;
	}

	List<ALMTest> getPrereqList(List<ALMTest> origin_tests) {
		LinkedList<ALMTest> subprod_tests = new LinkedList<ALMTest>();
		LinkedList<ALMTest> ret_tests = new LinkedList<ALMTest>();

		HashSet<ALMTest> subprod_set = new HashSet<ALMTest>();
		ArrayList<Integer> origin_size = test_size;
		List<ALMTest> prod_tests = origin_tests.subList(0, (origin_size.get(0)));
		int cur = 0;
		int pos = 0;
		for (int i = 0; i < prod_name.size(); i++) {

			subprod_set.clear();
			subprod_tests.clear();

			prod_tests = origin_tests.subList(pos, pos + origin_size.get(i));
			log.trace("[Product Tests]:" + prod_tests.toString());

			for (ALMTest origin : prod_tests) {
				pos++;
				if (subprod_set.add(origin)) {
					subprod_tests.add(origin);
				}
				cur = subprod_tests.indexOf(origin);
				int dup = cur;
				log.trace("origin test[" + cur + "]:" + origin.toString());

				ArrayList<ALMTest> related = getCallTestList(origin);
				log.trace("related:" + related.toString());
				for (ALMTest tst : related) {
					if (!subprod_set.add(tst)) {
						dup = subprod_tests.indexOf(tst);
						if (dup < cur)
							cur = dup;
						subprod_tests.remove(tst);
					}
				}
				if (related.size() > 0)
					subprod_tests.addAll(cur, related);
				log.trace("___Current List with pre-req:" + subprod_tests.toString());
			}
			test_size.set(i, subprod_tests.size());
			ret_tests.addAll(subprod_tests);
		}
		return ret_tests;
	}

	ArrayList<ALMTest> getCallTestList(ALMTest test) {
		ArrayList<ALMTest> parentList = new ArrayList<ALMTest>();
		parentList.add(test);
		log.trace("ParentList1:" + parentList.toString());
		ALMTest parent = getCallTest(test);
		if (parent != null) {
			parentList.addAll(parentList.indexOf(test), getCallTestList(parent));
			log.trace("ParentList2:" + parentList.toString());
		}
		return parentList;
	}

	ALMTest getCallTest(ALMTest alm_test) {
		if (alm_test == null)
			return null;

		StringBuilder sql = new StringBuilder();
		ResultSet rs = null;
		ALMTest call_test = new ALMTest();
		String test_id = alm_test.getTest_ID();
		if (test_id == null || test_id.equals(""))
			return null;
		sql.append(
				"SELECT DS_LINK_TEST, TS_USER_03, TS_USER_37, TS_USER_39 FROM SCM_SCM_92_PRD_DB.DESSTEPS A, SCM_SCM_92_PRD_DB.TEST B WHERE A.DS_LINK_TEST=B.TS_TEST_ID AND A.DS_TEST_ID=");
		sql.append(test_id);
		sql.append(
				" AND A.DS_STEP_ORDER=0 AND A.DS_STEP_NAME='Call' AND B.TS_TYPE='PTF' AND B.TS_STATUS='Ready' AND B.TS_USER_08='Complete'");
		try {
			DBUtil alm = new DBUtil(new DBInfo("ALM"));

			rs = alm.getQueryResult(sql.toString());
			if (rs.next()) {
				if (rs.getString("DS_LINK_TEST") != null && !rs.getString("DS_LINK_TEST").equals("")) {
					call_test.setTest_ID(rs.getString("DS_LINK_TEST"));
					call_test.setProduct(rs.getString("TS_USER_03"));
					call_test.setPTF_Test(rs.getString("TS_USER_37"));
					call_test.setPTF_Test_Case(rs.getString("TS_USER_39"));
					return call_test;
				}
			}
			alm.closeConnection();
			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	int firstProductTestPos(int prodIndex, ArrayList<Integer> sizeList) {
		if (prodIndex == 0)
			return 0;
		if (prodIndex == 1)
			return sizeList.get(0);
		if (prodIndex > sizeList.size())
			return -1;

		int ret = 0;
		for (int i = 0; i < prodIndex; i++) {
			ret = ret + sizeList.get(i);
		}

		return ret;
	}

}
