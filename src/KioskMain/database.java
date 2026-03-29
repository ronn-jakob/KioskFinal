
package KioskMain;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author admin
 */
public class database {
    public static Connection connectdatabase() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try {
                conn = DriverManager.getConnection("jdbc:mysql://localhost/kioskEDP", "root", "@Fifteen200515");
                System.out.println("Successfully Connected");
            } catch (SQLException ex) {
                
                System.out.println("Not Connected: ");
                ex.printStackTrace();
            }

        } catch (ClassNotFoundException ex) {
            
            System.out.println("Missing Driver: ");
            ex.printStackTrace();
        }

        return conn;
    }
}
