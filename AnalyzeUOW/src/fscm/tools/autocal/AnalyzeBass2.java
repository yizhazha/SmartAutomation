package fscm.tools.autocal;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fscm.tools.metadata.ModifiedObject;
import fscm.tools.metadata.UOW;
import fscm.tools.util.DBInfo;
import fscm.tools.util.DBUtil;

public class AnalyzeBass2 {
	static Logger log = LogManager.getLogger(AnalyzeBass2.class);

	/**
	 * @param uow
	 */
	List<ModifiedObject> computeMOL(UOW uow) {
		DBUtil bass2;
		ResultSet result = null;
		List<ModifiedObject> mol = new ArrayList<ModifiedObject>();
		DBInfo db = new DBInfo("BASS2");
		if (db != null) {
			try {
				bass2 = new DBUtil(db);
				String sql = "SELECT distinct MOL.OBJECTTYPE,TYPE.DESCR254,OBJECTVALUE1,OBJECTVALUE2 ,OBJECTVALUE3,OBJECTVALUE4,UOW.BUG_RPTNO FROM  PS_UOW_DEFN uow , PS_UOW_MOL mol, PS_UOW_OBJTYPECDVW type"
						+ " where  uow.uow_id=mol.uow_id and  mol.objecttype=type.objecttype and MOL.UOW_ID="
						+ uow.getUOW_ID() + " ORDER BY MOL.OBJECTTYPE ASC";
				result = bass2.getQueryResult(sql);
				// Query MO from Bass2
				int type = -1;
				String value1 = "";
				String value2 = "";
				if (result.getFetchSize() <1) {					
					return null;
				}
				while (result.next()) {
					uow.setBugNo(result.getString("BUG_RPTNO"));
					ModifiedObject mo = new ModifiedObject();
					type = result.getInt("OBJECTTYPE");
					value1 = result.getString("OBJECTVALUE1");
					value2 = result.getString("OBJECTVALUE2");
					if (type == 20) {
						value1 = value2;
					}
					mo.setObjType(type);
					mo.setObjValue1(value1);
					mo.setObjValue2(value2);
					mo.setObjValue3(result.getString("OBJECTVALUE3"));
					mo.setObjValue4(result.getString("OBJECTVALUE4"));

					mol.add(mo);
					//log.debug("[TYPE=" + type + "]" + value1);
				}
				bass2.closeConnection();

				log.info("[Summary]-Candidate Modified Objects for UOW:" + uow.getUOW_ID() + " is " + mol.size());

			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		return mol;
	}
}
