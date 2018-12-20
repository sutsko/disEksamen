package controllers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import model.Order;
import model.User;
import utils.Config;

public class DatabaseController {

    private static Connection connection;
    

    public DatabaseController() {
    connection = getConnection();
  }

  /**
   * Get database connection
   *
   * @return a Connection object
   */
  public static Connection getConnection() {
    try {

        //If there aren't a connection we create one - otherwise we return the one we have.
        if (connection == null || connection.isClosed()) {
            // Set the database connect with the data from the config
            String url =
                    "jdbc:mysql://"
                            + Config.getDatabaseHost()
                            + ":"
                            + Config.getDatabasePort()
                            + "/"
                            + Config.getDatabaseName()
                            + "?serverTimezone=CET";

            String user = Config.getDatabaseUsername();
            String password = Config.getDatabasePassword();

            // Register the driver in order to use it
            DriverManager.registerDriver(new com.mysql.jdbc.Driver());

            // create a connection to the database
            connection = DriverManager.getConnection(url, user, password);
        }
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }

    return connection;
  }


}
