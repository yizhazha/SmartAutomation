/**
 * 
 */
package fscm.tools.metadata;

/**
 * @author qidai
 *
 */
public class PTFTest {
	String Test_Name;
	String Test_Case;

	public String getTest_Name() {
		return Test_Name;
	}

	public void setTest_Name(String test_Name) {
		Test_Name = test_Name;
	}

	public String getTest_Case() {
		return Test_Case;
	}

	public void setTest_Case(String test_Case) {
		Test_Case = test_Case;
	}

	@Override
	public int hashCode() {
		// System.out.println(this.Test_Case+"....hashCode");
		return Test_Name.hashCode() + Test_Case.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PTFTest)) {
			return false;
		}
		PTFTest test = (PTFTest) obj;
		// System.out.println(this.Test_Name+"??? equals ???"+test.Test_Name);
		return this.Test_Name.equals(test.Test_Name) && this.Test_Case.equals(test.Test_Case);
	}

	public String toString() {
		return this.Test_Name;
	}
}
