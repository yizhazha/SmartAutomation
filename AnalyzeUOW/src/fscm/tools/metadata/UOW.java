/**
 * 
 */
package fscm.tools.metadata;

/**
 * @author qidai
 *
 */
public class UOW {
private String UOW_ID;
private String bugNo;
private String user_ID;

public String getUser_ID() {
	return user_ID;
}

public void setUser_ID(String user_ID) {
	this.user_ID = user_ID;
}

public String getBugNo() {
	return bugNo;
}

public void setBugNo(String bugNo) {
	this.bugNo = bugNo;
}

public String getUOW_ID() {
	return UOW_ID;
}

public void setUOW_ID(String uOW_ID) {
	UOW_ID = uOW_ID;
}

public UOW(String uOW_ID) {
	super();
	UOW_ID = uOW_ID;
}

}
