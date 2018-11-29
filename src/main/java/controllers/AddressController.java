package controllers;

import java.sql.ResultSet;
import java.sql.SQLException;
import model.Address;
import utils.Log;

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

    // Check for DB Connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Insert the address in the DB
    int addressID = dbCon.insert(
        "INSERT INTO address(name, city, zipcode, street_address) VALUES('"
            + address.getName()
            + "', '"
            + address.getCity()
            + "', '"
            + address.getZipCode()
            + "', '"
            + address.getStreetAddress()
            + "')");

    if (addressID != 0) {
      //Update the address id ofthe address before returning
      address.setId(addressID);
      return address;
    }

    // Return null if address has not been inserted into database
    return null;

  }

  /**
   * @param rs which is a Resultset
   * @return address
   * 1. The formAddress() declare and instantiates an Address-object based on information from the resultset
   * 2. Returns the new address
   */
  public static Address formAddress(ResultSet rs) {
    try {
      Address address = new Address(rs.getInt("a_id"),
              rs.getString("name"),
              rs.getString("street_address"),
              rs.getString("city"),
              rs.getString("zipcode")
      );

      return address;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }
  
}
