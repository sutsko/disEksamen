package controllers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import model.Product;
import utils.Log;


public class ProductController {

  private static DatabaseController dbCon;

  public ProductController() {
    dbCon = new DatabaseController();
  }

  /** @param id
   * @return Product
   * 1. The getProduct() methods gets the product based on the id
   * 2. First we build an SQL query to get all information regarding our product in a resultset
   * 3. We excecute prepared statement and form the product to be returned.
   */
  public static Product getProduct(int id) {

    // check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Build the SQL query for the DB
    String sql = "SELECT * FROM product where p_id=" + id;

    // Run the query in the DB and declare an empty object to return
    ResultSet rs = dbCon.query(sql);
    Product product = null;

    try {
      // Get first row and initialize through formProduct() the object and return it
      if (rs.next()) {
        product = formProduct(rs);

        // Return the product
        return product;
      } else {
        System.out.println("No product found");
      }
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

    // Return empty object
    return product;
  }

  /** @param sku
   * @return Product
   * 1. The getProductBySku() methods gets the product based on the sku
   * 2. First we build an SQL query to get all information regarding our product in a resultset
   * 3. We excecute prepared statement and form the product to be returned.
   */
  public static Product getProductBySku(String sku) {

    // check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Build the SQL query for the DB
    String sql = "SELECT * FROM product where sku='" + sku + "'";

    // Run the query in the DB and declare an empty object to return
    ResultSet rs = dbCon.query(sql);
    Product product = null;

    try {
      // Get first row and initialize through formProduct() the object and return it
      if (rs.next()) {
        product = formProduct(rs);
        return product;
      } else {
        System.out.println("No product found");
      }
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

    return product;
  }

  /**
   * @return Product arraylist
   * 1. The getProducts() methods gets the all the products from the database
   * 2. First we build an SQL query to get all information regarding our product in a resultset
   * 3. We excecute prepared statement and form the products to be returned. We add the products to arraylist adn returns.
   */
  public static ArrayList<Product> getProducts() {

    //Check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // TODO: Use caching layer. FIX - See caching
    // Build the SQL query for the DB
    String sql = "SELECT * FROM product";

    // Run the query in the DB and initialize an empty arraylist to return later
    ResultSet rs = dbCon.query(sql);
    ArrayList<Product> products = new ArrayList<Product>();

    try {
      // Get first row and initialize through formProduct() the object. Then second row and so on. Add to arraylist
      //and return.
      while (rs.next()) {
        Product product = formProduct(rs);
        products.add(product);
      }
      return products;
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }
    return products;
  }

  /** @param product
   * @return product
   * 1. The createProduct() methods takes the product we initialized in the endpoint
   * 2. First we set a created_at for the product
   * 3. Next we build the SQL-prepared statement and insert it to our database
   * 4. The genereated key is set to the product_id, and the product is returned.
   */
  public static Product createProduct(Product product) {

    // Write in log that we've reach this step
    Log.writeLog(ProductController.class.getName(), product, "Actually creating a product in DB", 0);

    // Set creation time for product.
    product.setCreatedTime(System.currentTimeMillis());

    // Check for DB Connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Insert the product in the DB
    int productID = dbCon.insert(
        "INSERT INTO product(product_name, sku, price, description, stock, product_created_at) VALUES('"
            + product.getName()
            + "', '"
            + product.getSku()
            + "', '"
            + product.getPrice()
            + "', '"
            + product.getDescription()
            + "', '"
            + product.getStock()
            + "', '"
            + product.getCreatedTime()
            + "')");

    if (productID != 0) {
      //Update the productid of the product before returning
      product.setId(productID);
    } else{
      // Return null if product has not been inserted into database
      return null;
    }

    // Return product
    return product;
  }

  /**
   * Initialising, instantiating and declaring a product.
   */
  public static Product formProduct(ResultSet rs) {
    try {
      Product product = new Product(rs.getInt("p_id"),
              rs.getString("product_name"),
              rs.getString("sku"),
              rs.getFloat("price"),
              rs.getString("description"),
              rs.getInt("stock"));

      return product;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

}
