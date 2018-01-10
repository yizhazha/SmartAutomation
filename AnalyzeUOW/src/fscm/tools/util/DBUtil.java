/**
 * 
 */
package fscm.tools.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author qidai
 *  Database Access and Operate
 */
public class DBUtil {
    

    private Connection connection;
    private Statement statement;
    
      public DBUtil(DBInfo db) throws SQLException, ClassNotFoundException {
    	
    	//  System.setProperty(arg0, arg1)
        Class.forName("oracle.jdbc.OracleDriver");
        //DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
        this.connection
            = DriverManager.getConnection(db.url, db.user, db.password);
        this.statement = this.connection.createStatement();
      }

      public Connection getConnection() {
        return this.connection;
      }

      public ResultSet getQueryResult(String sql) throws SQLException {
        return this.statement.executeQuery(sql);
//        return this.connection.createStatement().executeQuery(sql);
      }
    

      public void closeConnection() throws SQLException {
        this.statement.close();
        this.connection.close();
      }


}
