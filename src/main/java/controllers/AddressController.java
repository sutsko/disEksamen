package controllers;

import java.sql.*;

import model.Address;
import utils.Log;

import javax.xml.crypto.Data;

public class AddressController {

  private static DatabaseController dbCon;

  public AddressController() {
    dbCon = new DatabaseController();
  }

  /**
   * @param address
   * @return address
   * 1. The createAddress() method takes an address and saves it in the database
   * 2. AddressId will be set to the generated key from the insert to the database.
   * 3. Returns the new address
   */
  public static Address createAddress(Address address) {


    // Write in log that we've reach this step
    Log.writeLog(ProductController.class.getName(), address, "Actually creating an address in DB", 0);

    try {
      // Check for DB Connection
      if (dbCon == null) {
        dbCon = new DatabaseController();
      }

      //Building SQL statement and executing query
      String sql = "INSERT INTO address(name, city, zipcode, street_address) VALUES(?,?,?,?)";

      PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      preparedStatement.setString(1, address.getName());
      preparedStatement.setString(2, address.getCity());
      preparedStatement.setString(3, address.getZipCode());
      preparedStatement.setString(4, address.getStreetAddress());

      int rowsAffected = preparedStatement.executeUpdate();

      // Get our key back in order to apply it to an object as ID
      ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
      if (generatedKeys.next()&&rowsAffected==1) {
        address.setId(generatedKeys.getInt(1));
        return address;
      }

    } catch (SQLException e){
      e.printStackTrace();
    }
      // Return null if address has not been inserted into database
    return address;

  }



  /**
   * @param rs which is a Resultset
   * @return address
   * 1. The formAddress() declare and instantiates an Address-object based on information from the resultset
   * 2. Returns the new address
   */
  public static Address formBillingAddress(ResultSet rs) {
    try {
      Address address = new Address(rs.getInt("ba.a_id"),
              rs.getString("ba.name"),
              rs.getString("ba.street_address"),
              rs.getString("ba.city"),
              rs.getString("ba.zipcode")
      );

      return address;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static Address formShippingAddress(ResultSet rs) {
    try {
      Address address = new Address(rs.getInt("sa.a_id"),
              rs.getString("sa.name"),
              rs.getString("sa.street_address"),
              rs.getString("sa.city"),
              rs.getString("sa.zipcode")
      );

      return address;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }
  
}
