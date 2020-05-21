import com.sun.xml.internal.ws.api.model.wsdl.WSDLOutput;
import org.sqlite.JDBC;

import java.beans.Customizer;
import java.sql.*;

public class DBHandler {
    private static final String dbPath = "jdbc:sqlite:GeeServer\\db\\users.db";
    private static DBHandler instance = null;

    private DBHandler(){
        try {
            DriverManager.registerDriver(new JDBC());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static synchronized DBHandler getInstance() {
        if(instance == null)
            instance = new DBHandler();
        return instance;
    }

    
    public boolean authorization(String login, String password) {
        String passFromDb = " ";
        try(Connection connection = DriverManager.getConnection(dbPath);
            PreparedStatement statement = connection.prepareStatement("SELECT password FROM users WHERE login=?")) {
            statement.setString(1, login);
            ResultSet resultSet = statement.executeQuery();
            passFromDb = resultSet.getString("password");
            resultSet.close();
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return password.equals(passFromDb);
    }


}
