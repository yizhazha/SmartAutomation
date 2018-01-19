/**
 * 
 */
package fscm.tools.autocal;

import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fscm.tools.metadata.ModifiedObject;
import fscm.tools.metadata.PTFTest;
import fscm.tools.metadata.UOW;
import fscm.tools.util.DBInfo;
import fscm.tools.util.FileUtil;

/**
 * @author qidai
 *
 */
public class SmartAnalyze {
	static List<ModifiedObject> mol = null;
	static HashSet<String> set_comp;
	static HashSet<String> set_rec;
	static HashSet<String> set_page;
	static HashSet<PageRecordField> set_field;
	static HashSet<String> set_menu;
	static HashSet<PTFTest> set_test;
	static HashSet<String> set_ae;
	static HashSet<String> set_job;
	
	static Logger log = LogManager.getLogger(SmartAnalyze.class);


	public static void main(String[] args) {		
		//args[0]="49155";
		//args[3]="ALL";
		String product=null;
		
		if(args.length>3) {
			if (args[3].toUpperCase().equals("ALL"))
				product="";
			else
				product=args[3];		
		}
		
		log.warn("=================New Log Start===========================");
		if(args.length<3) {
			log.error("Please enter correct parameter!!!!");
			return;
		}
		if(args[0]==null ||args[0].equals("")) {
			log.warn("UOW ID is empty!");
			return;
		}
		if(args[1]==null ||args[1].equals("")) {
			log.warn("Test DB is empty!");
			return;
		}
		if(args[2]==null ||args[2].equals("")) {
			log.warn("UserName is empty!");
			return;
		}
		
		UOW uow = new UOW(args[0]);
		uow.setUser_ID(args[2]);
		AnalyzePeopleCode pc=new AnalyzePeopleCode();
		AnalyzeBass2 bass2=new AnalyzeBass2();
		AnalyzePTF ptf=new AnalyzePTF();
		AnalyzeALM alm= new AnalyzeALM();
		List<PTFTest> testlist=null;
		
		if(!DBInfo.setDBInfo("PTF", "EP92PROD")) return;
		if(!DBInfo.setDBInfo("TESTDB", args[1].toUpperCase()))return;
		
		log.info("<<< Compute " + args[0] + " Modified Objects on Bass2 >>>");
		mol=bass2.computeMOL(uow);
		
		if(mol==null|| mol.size()==0) {
			log.warn("Not Find any Modified Objects for this UOW:"+args[1]);
			return;		
		}
		
		log.info("<<< Step 1. Compute Dependency on " + args[1] + " >>>");
		if (pc.computeDependency(mol)) {			
			log.info("<<< Step 2. Matching PTF Test according to Dependency >>>");
			if (ptf.computePTFTest(uow.getBugNo(), product)) {
				log.info("<<< Step 3. Filter Product Test>>>");
				testlist=alm.filterTestFromALM(product);
				if (testlist != null && testlist.size() > 0) {					
					log.info("<<< Step 4. Output to JSON File >>>");
					StringBuilder json = FileUtil.generateJson(uow, testlist,alm);					
					String fileName = uow.getUOW_ID() + "_" +args[1]+"_"+ args[2].trim() + "_" + FileUtil.Date2FileName();
					if (FileUtil.createJsonFile(fileName, json)) {
						log.info("Generate File Successfully: " + fileName + ".json");
					}
				}
				else {
					log.warn("Not find any related PTF test.");
				}
			}
		}
	}
	
}
