/**
 * 
 */
package fscm.tools.autocal;

/**
 * @author qidai
 *
 */
public class PageRecordField {
String page;
String field;
String record;

public String getRecord() {
	return record;
}
public void setRecord(String record) {
	this.record = record;
}

public String getPage() {
	return page;
}
public void setPage(String page) {
	this.page = page;
}
public String getField() {
	return field;
}
public void setField(String field) {
	this.field = field;
}
@Override  
public int hashCode() {   
    return page.hashCode()+field.hashCode()+record.hashCode();  
}  
@Override  
public String toString() {  
   
    return page+"."+record+"."+field;  
}  

@Override  
public boolean equals(Object obj) {  
    if(!(obj instanceof PageRecordField)){  
        return false;  
    }  
    PageRecordField test=(PageRecordField)obj;  
   // System.out.println(this.Test_Name+"??? equals ???"+test.Test_Name);  
    return this.page.equals(test.page)&& this.field.equals(test.field) && this.record.equals(test.record);  
}  
}
