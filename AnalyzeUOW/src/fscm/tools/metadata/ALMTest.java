/**
 * 
 */
package fscm.tools.metadata;

/**
 * @author qidai
 *
 */
public class ALMTest {
	String Test_ID;
	String Product;
	String PTF_Test;
	String PTF_Test_Case;
	

	public ALMTest(String test_ID, String product, String pTF_Test, String pTF_Test_Case) {
		super();
		Test_ID = test_ID;
		Product = product;
		PTF_Test = pTF_Test;
		PTF_Test_Case = pTF_Test_Case;
		
	}
	public ALMTest() {
		Test_ID = "";
		Product = "";
		PTF_Test = "";
		PTF_Test_Case = "";
		
	}


	public String getTest_ID() {
		return Test_ID;
	}

	public void setTest_ID(String test_ID) {
		Test_ID = test_ID;
	}

	public String getProduct() {
		return Product;
	}

	public void setProduct(String product) {
		Product = product;
	}

	public String getPTF_Test() {
		return PTF_Test;
	}

	public void setPTF_Test(String pTF_Test) {
		PTF_Test = pTF_Test;
	}

	public String getPTF_Test_Case() {
		return PTF_Test_Case;
	}

	public void setPTF_Test_Case(String pTF_Test_Case) {
		PTF_Test_Case = pTF_Test_Case;
	}
	
	@Override
	public String toString() {

		return this.Product+":"+this.PTF_Test+"";
	}
	@Override  
	public int hashCode() {  
	    //System.out.println(this.Test_Case+"....hashCode");  
	    return Test_ID.hashCode()+PTF_Test.hashCode();  
	}  
	  
	@Override  
	public boolean equals(Object obj) {  
	    if(!(obj instanceof ALMTest)){  
	        return false;  
	    }  
	    ALMTest test=(ALMTest)obj;  
	   // System.out.println(this.Test_Name+"??? equals ???"+test.Test_Name);  
	    return this.Test_ID.equals(test.Test_ID)&& this.PTF_Test.equals(test.PTF_Test);  
	}  
}
