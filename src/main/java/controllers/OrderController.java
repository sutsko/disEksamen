package controllers;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import model.*;
import utils.Log;


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
      int orderID = dbCon.insert( "INSERT INTO orders(user_id, billing_address_id, shipping_address_id, order_total, order_created_at, order_updated_at) VALUES("
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
      for(LineItem item : order.getLineItems()){
        item = LineItemController.createLineItem(item, order.getId());
        items.add(item);
      }

      //Add line items to the order, commit and return the order
      order.setLineItems(items);
      dbCon.getConnection().commit();
      return order;

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
    return  null;
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

    Order order = null;
    ArrayList <LineItem> lineItemsList = new ArrayList<>();
    User user;
    LineItem lineItem;
    Address billingsAddress;
    Product product;
    Address shippingAddress;

    // check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    try {

      dbCon.getConnection().setAutoCommit(false);
      // Build SQL string to query
      String sql1 = "SELECT *" +
              "FROM address\n" +
              "LEFT JOIN orders ON address.a_id=orders.billing_address_id\n" +
              "LEFT JOIN user ON orders.user_id = user.u_id\n" +
              "LEFT JOIN line_item ON line_item.order_id = orders.o_id\n" +
              "LEFT JOIN product ON product.p_id = line_item.product_id\n" +
              "WHERE orders.o_id="+orderId;


      // Do the query in the database
      ResultSet rs = dbCon.query(sql1);

      while (rs.next()) {
          if (order==null) {

            user = UserController.formUser(rs);

            product = ProductController.formProduct(rs);


            lineItem = LineItemController.formLineItem(rs, product);

            lineItemsList.add(lineItem);


            billingsAddress = AddressController.formAddress(rs);

            // Create an order from the database data
            order = formOrder(rs, user,lineItemsList,billingsAddress);
          } else{
              product = ProductController.formProduct(rs);
              lineItem = LineItemController.formLineItem(rs, product);
              order.getLineItems().add(lineItem);
          }
      }

      //Making our second query
      String sql2 ="SELECT * FROM address WHERE address.a_id="+(order.getBillingAddress().getId()+1);
      ResultSet rs2 = dbCon.query(sql2);

      if (rs2.next()){
        shippingAddress = AddressController.formAddress(rs2);
        order.setShippingAddress(shippingAddress);
      }


      dbCon.getConnection().commit();
      return order;
    } catch (SQLException | NullPointerException e) {
      System.out.println(e.getMessage());
      try {
        //If exception was catched, we roll our statements to the database back.
        System.out.println("rolling back");
        dbCon.getConnection().rollback();
      } catch (SQLException ex) {
        ex.printStackTrace();
      }
    }finally {
      try {
        //Setting the autocommit to true.
        dbCon.getConnection().setAutoCommit(true);
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    // Returns null
    return order;
  }

  public static ArrayList<Order> getOrders() {

    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    String sql = "SELECT *" +
            "FROM address\n" +
            "LEFT JOIN orders ON address.a_id=orders.billing_address_id\n" +
            "\n" +
            "LEFT JOIN user ON orders.user_id = user.u_id\n" +
            "\n" +
            "LEFT JOIN line_item ON line_item.order_id = orders.o_id\n" +
            "\n" +
            "LEFT JOIN product ON product.p_id = line_item.product_id\n" +
            "\n" +
            "ORDER BY address.a_id\n";

    ResultSet rs = dbCon.query(sql);
    ArrayList<Order> orders = new ArrayList<Order>();



    try {
      while(rs.next()) {
        User user;
        LineItem lineItem;
        Address billingsAddress;
        Address shippingAddress;
        Product product;
        ArrayList <LineItem> lineItemsList = new ArrayList<LineItem>();


        if(orders.isEmpty()){

          user = UserController.formUser(rs);

          product = ProductController.formProduct(rs);

          lineItem = LineItemController.formLineItem(rs, product);

          lineItemsList.add(lineItem);

          billingsAddress = AddressController.formAddress(rs);

          // Create an order from the database data
          Order order = formOrder(rs, user,lineItemsList,billingsAddress);

          orders.add(order);

          /**This if-statement will add products to existing order:
           * If the next line in the resultset where orderId (o_id) is
          the same as the previous line which was saved as an order and added to the arraylist
           **/
        } else if (rs.getInt("o_id") == orders.get(orders.size()-1).getId() && rs.getInt("o_id")!=0){

          product = ProductController.formProduct(rs);


          lineItem = LineItemController.formLineItem(rs, product);
          lineItemsList.add(lineItem);

          orders.get(orders.size()-1).getLineItems().add(lineItem);

          //In our generated resultset and due to the way it looks, if the o_id = null, it means that this line in
          //the cursor in resultset is where the shippingaddress to the order in the line above is.
        } else if(rs.getInt("o_id")==0){

          shippingAddress = AddressController.formAddress(rs);

          orders.get(orders.size()-1).setShippingAddress(shippingAddress);


        } else{

          user = UserController.formUser(rs);

          product = ProductController.formProduct(rs);


          lineItem =  LineItemController.formLineItem(rs, product);
          lineItemsList.add(lineItem);


          billingsAddress = AddressController.formAddress(rs);

          // Create an order from the database data
          Order order = formOrder(rs, user,lineItemsList,billingsAddress);
          //
          orders.add(order);
        }

      }

    } catch (SQLException  | ArrayIndexOutOfBoundsException ex) {
      System.out.println(ex.getMessage());
    }

    // return the orders
    return orders;
  }

  public static Order formOrder(ResultSet rs, User user, ArrayList<LineItem> lineItemsList, Address billingsAddress) {
    try {
      Order order = new Order(
              rs.getInt("o_id"),
              user,
              lineItemsList,
              billingsAddress,
              rs.getFloat("order_total"),
              rs.getLong("order_created_at"),
              rs.getLong("order_updated_at"));

      return order;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

}
