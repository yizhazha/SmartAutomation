/**
 * 
 */
package fscm.tools.autocal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fscm.tools.metadata.ModifiedObject;
import fscm.tools.util.DBInfo;
import fscm.tools.util.DBUtil;

/**
 * @author qidai
 *
 */
public class AnalyzePeopleCode {
	static Logger log = LogManager.getLogger(AnalyzePeopleCode.class);
	List<String> cand_comp = new ArrayList<String>();
	List<PageRecordField> cand_field = new ArrayList<PageRecordField>();
	List<String> cand_rec = new ArrayList<String>();
	List<String> cand_ae = new ArrayList<String>();
	List<String> cand_page = new ArrayList<String>();
	List<String> cand_menu = new ArrayList<String>();
	List<String> cand_job = new ArrayList<String>();

	/**
	 * Compute Dependencies according to MOL
	 * 
	 * @param mol
	 * @return
	 */
	boolean computeDependency(List<ModifiedObject> mol) {

		DBUtil testdb = null;
		if (mol.size() < 0) {
			return false;
		} else {
			String value1 = null;
			String value2 = null;

			int type = -1;
			try {
				testdb = new DBUtil(new DBInfo("TESTDB"));
				for (int i = 0; i < mol.size(); i++) {
					type = mol.get(i).getObjType();
					value1 = mol.get(i).getObjValue1();
					value2 = mol.get(i).getObjValue2();

					if (type == 0) {
						log.info("Object " + i + ": [RECORD][" + value1 + "]");
						computeRecordDependency(testdb, value1);
					}
					if (type == 8) {
						log.info("Object " + i + ": [RECORD PeopleCode][" + value1 + "][" + value2 + "]");
						computeRecordFieldDependency(testdb, value1, value2);
					}

					if (type == 2) {
						log.info("Object " + i + ": [Field][" + value1 + "]");
						computePageFieldDependency(testdb, value1);
					}

					// if mo is page/Panel=5/44
					if (type == 5 || type == 44 || type == 45) {
						log.info("Object " + i + ": [Page][" + value1 + "]");
						computePageDependency(testdb, value1);
					}

					// if mo is Menu
					if (type == 6) {
						log.info("Object " + i + ": [Menu][" + value1 + "]");
						computeMenuDependency(testdb, value1);
					}

					// if MO is Component
					if (type == 7 || type == 46 || type == 47 || type == 48) {
						log.info("Object " + i + ": [Component][" + value1 + "]");
						cand_comp.add(value1);
					}

					// if MO is Process
					if (type == 20) {
						log.info("Object " + i + ": [Process][" + value2 + "]");
						cand_ae.add(value1);
					}
					// if MO is Job
					if (type == 23) {
						log.info("Object " + i + ": [Process Job][" + value1 + "]");
						cand_job.add(value1);
					}
					// if MO is SQL
					if (type == 30) {
						log.info("Object " + i + ": [SQL][" + value1 + "]");
						computeSQLDependency(testdb, value1);
					}
					if (type == 33 || type == 34 || type == 43) {
						log.info("Object " + i + ": [Application Engine][" + value1 + "]");
						cand_ae.add(value1);
					}
					if (type == 55) {
						log.info("Object " + i + ": [Portal Registry Structure][" + value1 + "]");
						computePortalDependency(mol.get(i));
					}
					if (type == 58) {
						log.info("Object " + i + ": [Application Package PeopleCode][" + value1 + "]["+value2+"]");
						computeAppPackageDependency(mol.get(i));
					}
					if (type == 68) {
						computeFileRefDependency(mol.get(i));
						log.info("Object " + i + ": [File Reference][" + value1 + "]");
					}

					// Find in Project

				}

				log.info("[Summary Dependency as Below]:");
				SmartAnalyze.set_menu = new HashSet<String>(cand_menu);
				log.info("Candidate Menu:" + SmartAnalyze.set_menu.size());
				log.debug(SmartAnalyze.set_menu.toString());

				SmartAnalyze.set_ae = new HashSet<String>(cand_ae);
				log.info("Candidate AE:" + SmartAnalyze.set_ae.size());
				log.debug(SmartAnalyze.set_ae.toString());

				SmartAnalyze.set_job = new HashSet<String>(cand_job);
				log.info("Candidate Job:" + SmartAnalyze.set_job.size());
				log.debug(SmartAnalyze.set_job.toString());

				// cand_rec.clear();
				SmartAnalyze.set_rec = new HashSet<String>(cand_rec);
				log.info("Candidate Record: " + SmartAnalyze.set_rec.size());
				log.debug(SmartAnalyze.set_rec.toString());

				SmartAnalyze.set_field = new HashSet<PageRecordField>(cand_field);
				log.info("Candidate Page Field: " + SmartAnalyze.set_field.size());
				log.debug(SmartAnalyze.set_field.toString());

				SmartAnalyze.set_page = new HashSet<String>(cand_page);
				log.info("Candidate Page: " + SmartAnalyze.set_page.size());
				log.debug(SmartAnalyze.set_page.toString());

				SmartAnalyze.set_comp = new HashSet<String>(cand_comp);
				log.info("Candidate Component are " + SmartAnalyze.set_comp.size() + ": ");
				log.debug(SmartAnalyze.set_comp.toString());
				testdb.closeConnection();

				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	void computePortalDependency(ModifiedObject mo) {
		StringBuilder sql = new StringBuilder();
		String portal = mo.getObjValue1().trim();
		String type = mo.getObjValue2().trim();
		String objName = mo.getObjValue3().trim();

		if (portal.equals("") || type.equals("") || objName.equals(""))
			return;

		if (type.equals("C")) {
			sql.append("SELECT distinct PORTAL_URI_SEG2 FROM PSPRSMDEFN where PORTAL_REFTYPE='C' AND PORTAL_NAME='");
			sql.append(portal);
			sql.append("' AND PORTAL_OBJNAME = '");
			sql.append(objName + "'");
			log.trace(sql);

			try {
				DBUtil db = new DBUtil(new DBInfo("TESTDB"));
				ResultSet rs = db.getQueryResult(sql.toString());
				List<String> temp = new LinkedList<String>();
				String comp = "";
				while (rs.next()) {
					comp = rs.getString("PORTAL_URI_SEG2").trim();
					if (!comp.equals(""))
						temp.add(comp);
				}
				if (temp.size() > 0)
					cand_comp.addAll(temp);
				db.closeConnection();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	void computeMenuDependency(DBUtil testdb, String value1) {
		cand_menu.add(value1);
		List<String> temp = new ArrayList<String>();
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT distinct MENUNAME From PSMENUITEM where menuname='");
		sql.append(value1);
		sql.append("' OR ITEMNAME='");
		sql.append(value1);
		sql.append("'");
		ResultSet rs;
		try {
			rs = testdb.getQueryResult(sql.toString());
			while (rs.next()) {
				temp.add(rs.getString("MENUNAME"));
			}
			cand_menu.addAll(temp);
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	void computePageDependency(DBUtil testdb, String value1) {
		cand_page.add(value1);
	}

	void computeSQLDependency(DBUtil testdb, String value1) {
		List<String> temp = null;
		try {
			temp = findAppEngineBySQLID(testdb, value1);
			cand_ae.addAll(temp);
			log.debug("[SQL][" + value1 + "] is used in AppEngine: " + temp.toString());
			temp = findAppEngineByAE(temp, testdb);
			log.debug("________[AppEngine] is used in AppEngine: " + temp.toString());
			cand_ae.addAll(temp);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Find Record's Dependency,
	 * 
	 * @param testdb
	 * @param recname
	 * @throws SQLException
	 */
	void computeRecordDependency(DBUtil testdb, String recname) {
		List<String> recList = new ArrayList<String>();
		List<String> aeList = new ArrayList<String>();
		List<String> compList = new ArrayList<String>();
		List<PageRecordField> fieldList = new ArrayList<PageRecordField>();
		List<String> temp = new ArrayList<String>();
		StringBuffer sql = new StringBuffer();
		ResultSet rs = null;
		ResultSet rs2 = null;
		cand_rec.add(recname);
		try {
			
			sql.append("select rectype, parentrecname from psrecdefn where recname='");
			sql.append(recname);
			sql.append("'");
			rs = testdb.getQueryResult(sql.toString());
			if (rs.next()) {
				String parent = rs.getString("parentrecname");
				int recType = rs.getInt("rectype");
				// 1.1 if has parent
				if (!(rs.getString("parentrecname").trim().equals(""))) {
					cand_rec.add(parent);
					log.debug("[Record][" + recname + "] Parent Record is " + parent);
					// Find PC Reference Table: get all Record call this parent
					findPCRefByRecord(testdb, parent);
				}

				// temp table, find AE
				if (recType == 7) {
					sql.delete(0, sql.length());
					sql.append(
							"SELECT DISTINCT A.AE_APPLID FROM PSAEAPPLDEFN A JOIN PSAEAPPLTEMPTBL B ON A.AE_APPLID= B.AE_APPLID AND B.recname='");
					sql.append(recname);
					sql.append("'");
				} else {
					sql.delete(0, sql.length());
					sql.append("SELECT DISTINCT AE_APPLID FROM PSAEAPPLSTATE WHERE AE_STATE_RECNAME='");
					sql.append(recname);
					sql.append("'");
				}
				rs2 = testdb.getQueryResult(sql.toString());
				aeList.clear();
				while (rs2.next()) {
					temp.add(rs2.getString("AE_APPLID"));
				}
				log.debug("[Record][" + recname + "] is used in " + temp.size() + " AE: " + temp.toString());
				aeList.addAll(temp);
				cand_ae.addAll(aeList);

				// find prompt table / default Value of record
				sql.delete(0, sql.length());
				sql.append("select distinct RECNAME from PSRECFIELDDB where DEFRECNAME='");
				sql.append(recname);
				sql.append("' OR EDITTABLE='");
				sql.append(recname);
				sql.append("'");
				rs = testdb.getQueryResult(sql.toString());
				while (rs.next()) {
					recList.add(rs.getString("RECNAME"));
				}
				log.debug("[Record][" + recname + "] is used as " + recList.size() + " PROMPT TABLE "
						+ recList.toString());
				cand_rec.addAll(recList);

				// find Page.Field call this record
				fieldList = getPageFieldByRecord(testdb, recname);
				log.debug("[Record][" + recname + "] is used in Page Field: " + fieldList.size());
				cand_field.addAll(fieldList);
				// Find PC Reference Table
				findPCRefByRecord(testdb, recname);
				compList = findCompByRecord(testdb, recname);
				cand_comp.addAll(compList);
				log.debug("________[Record][" + recname + "] is used as " + compList.size() + " Add_Search in Component. ");
			} else {
				log.warn("[Error]: Can't Find this Record's Definition: " + recname);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Find Record Field Dependency,
	 * 
	 * @param testdb
	 * @param recname
	 * @param value2
	 * @throws SQLException
	 */
	void computeRecordFieldDependency(DBUtil testdb, String recname, String value2) {
		List<String> aeList = new ArrayList<String>();
		List<String> temp = new ArrayList<String>();
		StringBuffer sql = new StringBuffer();
		ResultSet rs = null;
		ResultSet rs2 = null;

		try {
			// 1.Find Record Definition
			sql.append("select rectype, parentrecname from psrecdefn where recname='");
			sql.append(recname);
			sql.append("'");
			rs = testdb.getQueryResult(sql.toString());
			if (rs.next()) {
				int recType = rs.getInt("rectype");

				// cand_rec.add(recname);
				// String parent = rs.getString("parentrecname");
				// if (!(rs.getString("parentrecname").trim().equals(""))) {
				// cand_rec.add(parent);
				// log.debug("[Record][" + recname + "] Parent Record is " + parent);
				// findPCRefByRecord(testdb, parent);
				// }

				if (recType == 7) {
					sql.delete(0, sql.length());
					sql.append(
							"SELECT DISTINCT A.AE_APPLID FROM PSAEAPPLDEFN A JOIN PSAEAPPLTEMPTBL B ON A.AE_APPLID= B.AE_APPLID AND B.recname='");
					sql.append(recname);
					sql.append("'");
				} else {
					sql.delete(0, sql.length());
					sql.append("SELECT DISTINCT AE_APPLID FROM PSAEAPPLSTATE WHERE AE_STATE_RECNAME='");
					sql.append(recname);
					sql.append("'");
				}
				rs2 = testdb.getQueryResult(sql.toString());
				aeList.clear();
				while (rs2.next()) {
					temp.add(rs2.getString("AE_APPLID"));
				}
				log.debug("________[Record][" + recname + "] is used in " + temp.size() + " AE: " + temp.toString());
				aeList.addAll(temp);
				cand_ae.addAll(aeList);

				findPCRefByRecordField(testdb, recname, value2);

			} else {
				log.warn("[Error]: Can't Find this Record's Definition: " + recname);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	void computePageFieldDependency(DBUtil testdb, String value1) {

		List<PageRecordField> fieldList = new ArrayList<PageRecordField>();

		try {
			fieldList = getPageFieldByField(testdb, value1);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		log.debug("________[Field][" + value1 + "] is used in Page Field: " + fieldList.size());
		cand_field.addAll(fieldList);
	}

	void computeAppPackageDependency(ModifiedObject mo) {

		List<PageRecordField> fieldList = new ArrayList<PageRecordField>();
		String pck_root = mo.getObjValue1();
		String refName = "%";
		String dbfield = "";
		int id1;
		String recname = "";

		String app_path = "";
		if (!mo.getObjValue4().trim().equals("")) {			
			app_path = mo.getObjValue2() + ":" + mo.getObjValue3();
			refName = mo.getObjValue4().trim().toUpperCase();
		} else if (!mo.getObjValue3().trim().equals("")) {			
			app_path = mo.getObjValue2();
			refName = mo.getObjValue3().trim().toUpperCase();
		}
		else if (!mo.getObjValue2().trim().equals("")){
			app_path="";
			refName=mo.getObjValue2().toUpperCase();
		}
		else {
			app_path="";
			refName=mo.getObjValue1().toUpperCase();
		}

		StringBuilder sql = new StringBuilder();
		sql.append(
				"SELECT distinct OBJECTID1,OBJECTVALUE1, OBJECTID2, OBJECTVALUE2,OBJECTID3,OBJECTVALUE3,OBJECTID4,OBJECTVALUE4 FROM PSPCMNAME WHERE PACKAGEROOT='");
		sql.append(pck_root);
		
		if (!app_path.equals("")) {
			sql.append("' AND QUALIFYPATH = '"+app_path+"'");			
		}
		else
		{
			sql.append("' AND trim(QUALIFYPATH) is null");
		}
		sql.append(" AND REFNAME like '" + refName + "'  ORDER BY OBJECTID1");

		try {
			DBUtil db = new DBUtil(new DBInfo("TESTDB"));
			ResultSet rs = db.getQueryResult(sql.toString());
			log.trace(sql);
			while (rs.next()) {
				id1 = rs.getInt("OBJECTID1");

				if (id1 == 1 && rs.getInt("OBJECTID2") == 2) {
					recname = rs.getString("OBJECTVALUE1");
					dbfield = rs.getString("OBJECTVALUE2");
					PageRecordField field = new PageRecordField();
					field.setRecord(recname);
					field.setField(dbfield);
					field.setPage("%");
					fieldList.add(field);
				}
				if (id1 == 10 && rs.getInt("OBJECTID3") == 1 && rs.getInt("OBJECTID4") == 2) {
					PageRecordField field = new PageRecordField();
					recname = rs.getString("OBJECTVALUE3").trim();
					dbfield = rs.getString("OBJECTVALUE4").trim();
					field.setRecord(recname);
					field.setField(dbfield);
					field.setPage("%");
					fieldList.add(field);
				}
				if (id1 == 104) {					
/*					ModifiedObject pMO = new ModifiedObject();
					if(rs.getInt("OBJECTID2")==107) {
						if (rs.getString("OBJECTVALUE1").trim().equals(mo.getObjValue1().trim())
								&& (rs.getString("OBJECTVALUE2").trim().equals(mo.getObjValue2().trim())))
							;
						else {
							pMO.setObjValue1(rs.getString("OBJECTVALUE1").trim());
							pMO.setObjValue2(rs.getString("OBJECTVALUE2").trim());
							computePackageDependency(pMO);
						}					
					}
					
					if(rs.getInt("OBJECTID3")==107) {
						if (rs.getString("OBJECTVALUE1").trim().equals(mo.getObjValue1().trim())
								&& (rs.getString("OBJECTVALUE2").trim().equals(mo.getObjValue2().trim()))
								&& (rs.getString("OBJECTVALUE3").trim().equals(mo.getObjValue3().trim())))
							;

						else {							
							pMO.setObjValue1(rs.getString("OBJECTVALUE1").trim());
							pMO.setObjValue2(rs.getString("OBJECTVALUE2").trim());
							pMO.setObjValue3(rs.getString("OBJECTVALUE3").trim());
							computePackageDependency(pMO);
						}			
					}
					
					if(rs.getInt("OBJECTID4")==107) {
						if (rs.getString("OBJECTVALUE1").trim().equals(mo.getObjValue1().trim())
								&& (rs.getString("OBJECTVALUE2").trim().equals(mo.getObjValue2().trim()))
								&& (rs.getString("OBJECTVALUE3").trim().equals(mo.getObjValue3().trim()))
								&& (rs.getString("OBJECTVALUE4").trim().equals(mo.getObjValue4().trim())))
							;

						else {							
							pMO.setObjValue1(rs.getString("OBJECTVALUE1").trim());
							pMO.setObjValue2(rs.getString("OBJECTVALUE2").trim());
							pMO.setObjValue3(rs.getString("OBJECTVALUE3").trim());
							pMO.setObjValue4(rs.getString("OBJECTVALUE4").trim());
							computePackageDependency(pMO);
						}							
					}		*/
					}			
			}
			db.closeConnection();
		} catch (
		Exception e) {
			e.printStackTrace();
		}
		cand_field.addAll(fieldList);
		log.debug("________[Package][" + pck_root + "][" + refName + "] is used in PeopleCode: " + fieldList.size());
	}

	void computeFileRefDependency(ModifiedObject mo) {

		List<String> aeList = new LinkedList<String>();

		String value2 = mo.getObjValue2();
		String prcs = mo.getObjValue1();
		String prcs_type = "";

		if (value2.equals("SQR")) {
			prcs = prcs.substring(0, prcs.indexOf("_SQ"));
			prcs_type = "SQR%";
		} else if (value2.equals("COBOL")) {
			prcs = prcs.substring(0, prcs.indexOf("_CBL"));
			prcs_type = "COBOL%";
		} else {
			log.debug("________[File Reference][" + mo.getObjValue1() + "] is used in PeopleCode: 0");
			return;
		}
		try {
			DBUtil testdb = new DBUtil(new DBInfo("TESTDB"));

			StringBuilder sql = new StringBuilder();
			sql.append("SELECT DISTINCT PRCSNAME FROM PS_PRCSDEFN WHERE PRCSTYPE LIKE '" + prcs_type
					+ "' AND PRCSNAME='" + prcs + "'");
			ResultSet rs = testdb.getQueryResult(sql.toString());
			while (rs.next()) {
				prcs = rs.getString("PRCSNAME");
				aeList.add(prcs);
			}
			testdb.closeConnection();
		} catch (Exception e) {
			e.printStackTrace();
		}
		cand_ae.addAll(aeList);
		log.debug("________[File Ference][" + mo.getObjValue1() + "] is used in PeopleCode: " + aeList.size());
	}

	List<PageRecordField> getPageFieldByField(DBUtil testdb, String fieldname) throws SQLException {

		String sql = "";
		String page = null;
		String recname = null;
		List<PageRecordField> fieldList = new ArrayList<PageRecordField>();

		if (!fieldname.equals("")) {
			sql = "select distinct PNLNAME,recname from pspnlfield where fieldname ='" + fieldname + "'";

			ResultSet rs = testdb.getQueryResult(sql);
			while (rs.next()) {
				page = rs.getString("PNLNAME");
				recname = rs.getString("RECNAME");
				PageRecordField pageField = new PageRecordField();
				pageField.setRecord(recname);
				pageField.setPage(page);
				pageField.setField(fieldname);
				fieldList.add(pageField);
			}
		}
		return fieldList;
	}

	/**
	 * @param rec
	 * @param testdb
	 * @throws SQLException
	 * @return Pages List
	 */
	List<PageRecordField> getPageFieldByRecord(DBUtil testdb, String recname) throws SQLException {

		String sql = "";
		String page = null;
		String field = null;
		List<PageRecordField> fieldList = new ArrayList<PageRecordField>();

		if (!recname.equals("")) {
			sql = "select distinct PNLNAME,FIELDNAME from pspnlfield where recname ='" + recname + "'";

			ResultSet rs = testdb.getQueryResult(sql);
			while (rs.next()) {
				PageRecordField pageField = new PageRecordField();
				page = rs.getString("PNLNAME");
				field = rs.getString("FIELDNAME");
				pageField.setRecord(recname);
				pageField.setPage(page);
				pageField.setField(field);
				fieldList.add(pageField);
			}
		}

		return fieldList;
	}

	void findPCRefByRecordField(DBUtil testdb, String recname, String fieldname) throws SQLException {
		int id1 = -1;
		String value1 = "";
		String value2 = "";
		SmartAnalyze.log.debug("Find Record.Field[" + recname + "][" + fieldname + "]Reference in People Code:");

		List<PageRecordField> fieldList = new ArrayList<PageRecordField>();
		StringBuffer sql = new StringBuffer();
		// Get Record call this one
		sql.append("SELECT distinct OBJECTID1,OBJECTVALUE1, OBJECTID2, OBJECTVALUE2 FROM PSPCMNAME WHERE RECNAME='");
		sql.append(recname);
		sql.append("' AND REFNAME = '");
		sql.append(fieldname);
		sql.append("' ORDER BY OBJECTID1");

		ResultSet rs = testdb.getQueryResult(sql.toString());
		while (rs.next()) {
			id1 = rs.getInt("OBJECTID1");
			value1 = rs.getString("OBJECTVALUE1");
			value2 = rs.getString("OBJECTVALUE2");
			if (id1 == 1 && rs.getInt("OBJECTID2") == 2) {
				PageRecordField field = new PageRecordField();
				field.setRecord(value1);
				field.setField(value2);
				field.setPage("%");
				fieldList.add(field);
			}
		}
		cand_field.addAll(fieldList);
		SmartAnalyze.log
				.debug("________[Record][" + recname + "][" + fieldname + "] is used in PeopleCode: " + fieldList.size());
	}

	/**
	 * Find Reference in People Code
	 * 
	 * @param testdb
	 * @param recname
	 * @return
	 * @throws SQLException
	 */
	void findPCRefByRecord(DBUtil testdb, String recname) throws SQLException {
		int id1 = -1;
		String value1 = null;
		ResultSet rs;
		SmartAnalyze.log.debug("Find Record[" + recname + "] Reference in People Code:");
		List<String> compList = new ArrayList<String>();
		List<String> aeList = new ArrayList<String>();
		List<String> recList = new ArrayList<String>();
		StringBuffer sql = new StringBuffer();
		// Get Record call this one
		sql.append("SELECT distinct OBJECTID1,OBJECTVALUE1 FROM PSPCMNAME WHERE (RECNAME = '");
		sql.append(recname);
		sql.append("') OR (REFNAME = '");
		sql.append(recname);
		sql.append("' AND RECNAME='RECORD') ORDER BY OBJECTID1");

		rs = testdb.getQueryResult(sql.toString());
		while (rs.next()) {
			id1 = rs.getInt("OBJECTID1");
			value1 = rs.getString("OBJECTVALUE1");
			if (id1 == 1) {
				recList.add(value1);
			}
			if (id1 == 10)
				compList.add(value1);
			if (id1 == 66)
				aeList.add(value1);
		}
		SmartAnalyze.log.debug("________[Record][" + recname + "] is used in Record: " + recList.size() + recList.toString());
		SmartAnalyze.log.debug("________[Record][" + recname + "] is used in AE: " + aeList.size() + aeList.toString());
		SmartAnalyze.log.debug("________[Record][" + recname + "] is used in Component: " + compList.size());
		cand_rec.addAll(recList);
		cand_ae.addAll(aeList);
		cand_comp.addAll(compList);
	}

	/**
	 * @param testdb
	 * @param value1
	 * @return aeList
	 * @throws SQLException
	 */
	List<String> findAppEngineBySQLID(DBUtil testdb, String value1) throws SQLException {
		List<String> rsList = new ArrayList<String>();
		String sql = "select DISTINCT step.ae_applid from PSAESTEPDEFN step, PSAESTMTDEFN action where ACTION.SQLID='"
				+ value1 + "' AND ACTION.ae_applid= STEP.AE_DO_APPL_ID";

		ResultSet rs = testdb.getQueryResult(sql);
		while (rs.next()) {
			rsList.add(rs.getString("AE_APPLID"));
		}
		return rsList;
	}

	List<String> findAppEngineByAE(List<String> aeList, DBUtil testdb) throws SQLException {
		List<String> rsList = new ArrayList<String>();

		for (String ae : aeList) {

			String sql = "select distinct B.AE_APPLID FROM PSAESTEPDEFN A Join PSAESTEPDEFN B ON A.AE_DO_APPL_ID = '"
					+ ae + "' AND B.AE_DO_APPL_ID = A.AE_APPLID";

			ResultSet rs = testdb.getQueryResult(sql);
			while (rs.next()) {
				if (!aeList.contains(rs.getString("AE_APPLID")))
					;
				rsList.add(rs.getString("AE_APPLID"));
			}
		}
		return rsList;
	}

	/**
	 * find Add Search Record of Component
	 * 
	 * @param testdb
	 * @param value1
	 * @return component list
	 * @throws SQLException
	 */
	List<String> findCompByRecord(DBUtil testdb, String recname) throws SQLException {
		List<String> compList = new ArrayList<String>();
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT distinct PNLGRPNAME from pspnlgrpdefn where searchrecname='");
		sql.append(recname);
		sql.append("' OR ADDSRCHRECNAME='");
		sql.append(recname);
		sql.append("'");

		ResultSet rs = testdb.getQueryResult(sql.toString());
		while (rs.next()) {
			compList.add(rs.getString("PNLGRPNAME").trim());
		}
		return compList;
	}

	void removeDuplicateString(List<String> list) {

		LinkedHashSet<String> set = new LinkedHashSet<String>(list.size());
		set.addAll(list);
		list.clear();
		list.addAll(set);
	}
}
