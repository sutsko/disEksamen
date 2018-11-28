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

  public static Address getAddress(int id) {

    // Check for DB Connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Our SQL string
    String sql = "SELECT * FROM address where a_id=" + id;

    // Do the query and set the initial value to null
    ResultSet rs = dbCon.query(sql);
    Address address = null;

    try {
      // Get the first row and build an address object
      if (rs.next()) {
        address =
            new Address(
                rs.getInt("a_id"),
                rs.getString("name"),
                rs.getString("street_address"),
                rs.getString("city"),
                rs.getString("zipcode")
                );

        // Return our newly added object
        return address;
      } else {
        System.out.println("No address found");
      }
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

    // Returns null if we can't find anything.
    return address;
  }

  public static Address createAddress(Address address) {

    // Write in log that we've reach this step
    Log.writeLog(ProductController.class.getName(), address, "Actually creating an address in DB", 0);

    // Check for DB Connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Insert the product in the DB
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
      //Update the address id of the address before returning
      address.setId(addressID);
    } else{
      // Return null if address has not been inserted into database
      return null;
    }

    // Return address, will be null at this point
    return address;
  }
  
}
