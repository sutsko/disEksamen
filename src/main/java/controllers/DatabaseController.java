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
        if (connection == null) {
            // Set the dataabase connect with the data from the config
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

  /**
   * Do a query in the database
   *
   * @return a ResultSet or Null if Empty
   */
  public ResultSet query(String sql) {

    // Check if we have a connection
    if (connection == null)
      connection = getConnection();

    // We set the resultset as empty.
    ResultSet rs = null;

    try {
      // Build the statement as a prepared statement
      PreparedStatement stmt = connection.prepareStatement(sql);

      // Actually fire the query to the DB
      rs = stmt.executeQuery();

      // Return the results
      return rs;
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }

    // Return the resultset which at this point will be null
    return rs;
  }

    /**
     * Do an insert in the database
     *
     * @return a key or Null if zero
     */
    public int insert(String sql) {
        // Set result / the key to 0 as a start
        int result = 0;

        // Check that we have connection
        if (connection == null)
            connection = getConnection();

        try {
            // Build the statement up in a safe way
            PreparedStatement statement =
                    connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            // Execute update
            result = statement.executeUpdate();

            // Get our key back in order to apply it to an object as ID
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        // Return the result which at this point will be null
        return result;
    }

    /**
     * Do an update in the database
     *
     * @return true if update goes through and false if not
     */
    public boolean update (String sql) {

      //
        if (connection == null)
            connection = getConnection();

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);

            int rowaffected = preparedStatement.executeUpdate();

            if (rowaffected==1)
                return true;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Do a delete (essentially an update) in the database.
     * The functionality is the same as with update(), but for the sake of beginner-friendliness they have different names.
     * @return a ResultSet or Null if Empty
     */
    public boolean delete(String sql) {

        // Check that we have connection
        if (connection == null)
            connection = getConnection();

        try {
            // Build the statement as a prepared statement
            PreparedStatement stmt = connection.prepareStatement(sql);


            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 1) {
                return true;
            }
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
        return false;
    }



}
