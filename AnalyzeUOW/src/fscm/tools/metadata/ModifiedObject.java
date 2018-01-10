/**
 * 
 */
package fscm.tools.metadata;

/**
 * @author qidai
 *
 */
public class ModifiedObject {
	private int objType;
	private String objValue1;
	private String objValue2;
	private String objValue3;
	private String objValue4;
	
	public String getObjValue3() {
		return objValue3;
	}

	public ModifiedObject() {
		objValue1="";
		objValue2="";
		objValue3="";
		objValue4="";
	}

	public void setObjValue3(String objValue3) {
		this.objValue3 = objValue3;
	}

	public String getObjValue4() {
		return objValue4;
	}

	public void setObjValue4(String objValue4) {
		this.objValue4 = objValue4;
	}

	public int getObjType() {
		return objType;
	}

	public void setObjType(int objType) {
		this.objType = objType;
	}

	public String getObjValue1() {
		return objValue1;
	}

	public void setObjValue1(String objValue) {
		this.objValue1 = objValue;
	}


	public String getObjValue2() {
		return objValue2;
	}

	public void setObjValue2(String objValue2) {
		this.objValue2 = objValue2;
	}
}
