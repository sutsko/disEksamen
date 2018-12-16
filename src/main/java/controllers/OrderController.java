package controllers;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import model.*;
import utils.Log;

import javax.xml.crypto.Data;


public class OrderController {


  //For establishing a connection with the database later
  private static DatabaseController dbCon;

  //making a constructor
  public OrderController() {
    dbCon = new DatabaseController();
  }

  /** @param order
   * @return
   * 1. The createOrder() methods takes the order we created in the endpoint, now to add more information to it.
   * 2. First we set a created- and updatedAt for the order.
   * 3. Creating an order is a lot of steps - to ensure they all are declared right, we use transactions
   * 4. After we have begun the transaction we set the 2 address-IDs with the keys returned from the getXXXAddress() methods
   * 5. We get the user who created the order and set the order.Customer to this.
   * 6. We insert the order based on the things we found out in previuos steps
   */
  public static Order createOrder(Order order) {


    // Write in log that we've reach this step
    Log.writeLog(OrderController.class.getName(), order, "Actually creating a order in DB", 0);

    // Set creation and updated time for order.
    order.setCreatedAt(System.currentTimeMillis() / 1000L);
    order.setUpdatedAt(System.currentTimeMillis() / 1000L);

    // Check for DB Connection
    if (dbCon == null) {
      dbCon = new DatabaseController();

    }

    try {

      //We set the autocommit to false, making the way to use transactions
      dbCon.getConnection().setAutoCommit(false);




      //Setting the IDs of billing- and shippingAddress to the order
      //in other word: Save addresses to database and save them back to initial order instance
      order.setBillingAddress(AddressController.createAddress(order.getBillingAddress()));
      order.setShippingAddress(AddressController.createAddress(order.getShippingAddress()));

      //Setting the ID of the user to the order.
     order.setCustomer(UserController.getUser(order.getCustomer().getId()));

        // TODO: Enable transactions in order for us to not save the order if somethings fails for some of the other inserts: FIX
        // Insert the order in the DB
        int orderID = dbCon.insert("INSERT INTO orders(user_id, billing_address_id, shipping_address_id, order_total, order_created_at, order_updated_at) VALUES("
                + order.getCustomer().getId()
                + ", "
                + order.getBillingAddress().getId()
                + ", "
                + order.getShippingAddress().getId()
                + ", "
                + order.calculateOrderTotal()
                + ", "
                + order.getCreatedAt()
                + ", "
                + order.getUpdatedAt()
                + ")");

        if (orderID != 0) {
          //Update the order of the order before returning further down
          order.setId(orderID);
        }

        // Create an empty list in order to go trough items and then save them back with ID
        ArrayList<LineItem> items = new ArrayList<LineItem>();

        // Save line items to database with the respective order id
        for (LineItem item : order.getLineItems()) {
          item = LineItemController.createLineItem(item, order.getId());
          items.add(item);
        }

        //Add line items to the order, commit and return the order
        order.setLineItems(items);
        dbCon.getConnection().commit();

      // adding nullpointerexception, since we are using getUser() instead of createUser() - we would like people to be
      // logged in before they create an order - like Amazon.
    } catch (SQLException | NullPointerException e) {
      System.out.println(e.getMessage());
      try {
        //If exception was catched, we roll our statements to the database back.
        System.out.println("rolling back");
        dbCon.getConnection().rollback();
      } catch (SQLException ex) {
        ex.printStackTrace();
      }
    }
    finally {
      try {
        //Setting the autocommit to true.
        dbCon.getConnection().setAutoCommit(true);

      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return  order;
  }


  public static Order formOrder1(ResultSet rs, User user, ArrayList<LineItem> lineItemsList, Address billingsAddress, Address shippingsAddres) {
    try {
      Order order = new Order(
              rs.getInt("o_id"),
              user,
              lineItemsList,
              billingsAddress,
              shippingsAddres,
              rs.getFloat("order_total"),
              rs.getLong("order_created_at"),
              rs.getLong("order_updated_at"));

      return order;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }


  /** @param orderId
   * @return
   * 1. The getOrder() methods gets the order based on the id
   * 2. First we build an SQL query to get all information regarding our orders in a resultset.
   * 3. Creating an order is a lot of steps - to ensure they all are declared right, we use transactions
   * 4. After we have begun the transaction we set the 2 address-IDs with the keys returned from the getXXXAddress() methods
   * 5. We get the user who created the order and set the order.Customer to this.
   * 6. We insert the order based on the things we found out in previuos steps
   */
  public static Order getOrder(int orderId) {
    // check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Build SQL string to query
    String sql = "SELECT * FROM orders\n" +
            "inner join\n" +
            "user ON orders.user_id = user.u_id\n" +
            "inner join \n" +
            "line_item ON orders.o_id = line_item.order_id \n" +
            "inner join \n" +
            "address AS ba ON orders.billing_address_id = ba.a_id\n" +
            "inner join \n" +
            "address as sa ON orders.shipping_address_id = sa.a_id\n" +
            "inner join \n" +
            "product ON line_item.product_id  = product.p_id \n" +
            "where orders.o_id = " + orderId;

    // Do the query in the database and create an empty object for the results
    ResultSet rs = dbCon.query(sql);
    // New order object
    Order order = null;
    // User object
    User user = null;
    // New lineitem object
    LineItem lineItem = null;
    //New LineitemList
    ArrayList<LineItem> lineItemsList = new ArrayList<>();
    //New productlist
    Product product = null;
    // New adress object
    Address billingAddress = null;
    // New adress object
    Address shippingAddress = null;

    try {
      while (rs.next()) {
        if (order == null) {
          user = UserController.formUser(rs);

          product = ProductController.formProduct(rs);

          lineItem = LineItemController.formLineItem(rs, product);

          lineItemsList.add(lineItem);

          // Creating new billingAddress
          billingAddress = AddressController.formBillingAddress(rs);

          // Creating new Shippingaddress
          shippingAddress = AddressController.formShippingAddress(rs);

          order = formOrder1(rs, user, lineItemsList, billingAddress, shippingAddress);

        }else {
          product = ProductController.formProduct(rs);
          lineItem = LineItemController.formLineItem(rs, product);
          order.getLineItems().add(lineItem);
        }
      }
      // Returns the build order
      return order;

    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }
    return null;
  }


   public static ArrayList<Order> getOrders() {
     // check for connection
     if (dbCon == null) {
       dbCon = new DatabaseController();
     }
     // Orders instead of order in sql statement

     String sql = "SELECT * FROM orders\n" +
             "inner join\n" +
             "             user ON orders.user_id = user.u_id\n" +
             "             inner join \n" +
             "             line_item ON orders.o_id = line_item.order_id \n" +
             "             inner join \n" +
             "             address AS ba ON orders.billing_address_id = ba.a_id\n" +
             "             inner join \n" +
             "             address as sa ON orders.shipping_address_id = sa.a_id\n" +
             "             inner join \n" +
             "             product ON line_item.product_id  = product.p_id\n" +
             "             order by orders.o_id";

     ArrayList<Order> orders = new ArrayList<Order>();
     // Do the query in the database and create an empty object for the results
     ResultSet rs = dbCon.query(sql);
     // New order object
    // Order order = null;

     try {
       while(rs.next()) {

         // User object
         User user = null;
         // New lineitem object
         LineItem lineItem = null;
         // New adress object
         Address billingAddress = null;
         // New adress object
         Address shippingAddress = null;
         // new product object
         Product product = null;
         //New LineitemList
         ArrayList<LineItem> lineItemsList = new ArrayList<>();

        if (orders.isEmpty() || rs.getInt("o_id") != orders.get(orders.size()-1).getId()) {

          // Creating new user object
          user = UserController.formUser(rs);

          product = ProductController.formProduct(rs);

          lineItem = LineItemController.formLineItem(rs, product);

          lineItemsList.add(lineItem);

          // Creating new billingAddress
          billingAddress = AddressController.formBillingAddress(rs);
          // Creating new shippingAddress
          shippingAddress = AddressController.formShippingAddress(rs);

          // Creating new order
         Order order = formOrder1(rs, user, lineItemsList, billingAddress, shippingAddress);

          // Adding order to arraylist
          orders.add(order);
        } else if (rs.getInt("o_id") == orders.get(orders.size()-1).getId()){
          product = ProductController.formProduct(rs);

          lineItem = LineItemController.formLineItem(rs, product);
          lineItemsList.add(lineItem);

          orders.get(orders.size()-1).getLineItems().add(lineItem);
        }

       }
       return orders;
     } catch (SQLException ex) {
       System.out.println(ex.getMessage());
     }

     // return the orders
     return orders;

   }




}
