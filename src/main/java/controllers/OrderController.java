package controllers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import model.*;
import utils.Log;
import controllers.UserController;

public class OrderController {

  private static DatabaseController dbCon;


  public OrderController() {
    dbCon = new DatabaseController();
  }



  public static Order getOrder(int id) {

    // check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }


    // Build SQL string to query
    String sql = "SELECT * FROM orders where o_id=" + id;

    // Do the query in the database and create an empty object for the results
    ResultSet rs = dbCon.query(sql);
    Order order = null;


    try {
      if (rs.next()) {

        // Perhaps we could optimize things a bit here and get rid of nested queries.
        User user = UserController.getUser(rs.getInt("user_id"));
        ArrayList<LineItem> lineItems = LineItemController.getLineItemsForOrder(rs.getInt("o_id"));
        Address billingAddress = AddressController.getAddress(rs.getInt("billing_address_id"));
        Address shippingAddress = AddressController.getAddress(rs.getInt("shipping_address_id"));

        // Create an object instance of order from the database dataa
        order =
            new Order(
                rs.getInt("o_id"),
                user,
                lineItems,
                billingAddress,
                shippingAddress,
                rs.getFloat("order_total"),
                rs.getLong("order_created_at"),
                rs.getLong("order_updated_at"));

        // Returns the build order
        return order;
      } else {
        System.out.println("No order found");
      }
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

    // Returns null
    return order;
  }

  /**
   * Get all orders in database
   *
   * @return
   */
  public static ArrayList<Order> getOrders() {

    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    String sql = "SELECT * FROM orders";

    ResultSet rs = dbCon.query(sql);
    ArrayList<Order> orders = new ArrayList<Order>();

    try {
      while(rs.next()) {

        // Perhaps we could optimize things a bit here and get rid of nested queries. FIX
        User user = UserController.getUser(rs.getInt("user_id"));
        ArrayList<LineItem> lineItems = LineItemController.getLineItemsForOrder(rs.getInt("o_id"));
        Address billingAddress = AddressController.getAddress(rs.getInt("billing_address_id"));
        Address shippingAddress = AddressController.getAddress(rs.getInt("shipping_address_id"));

        // Create an order from the database data
        Order order =
            new Order(
                rs.getInt("o_id"),
                user,
                lineItems,
                billingAddress,
                shippingAddress,
                rs.getFloat("order_total"),
                rs.getLong("order_created_at"),
                rs.getLong("order_updated_at"));

        // Add order to our list
        orders.add(order);

      }

    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

     // return the orders
    return orders;
  }

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

      dbCon.getConnection().setAutoCommit(false);

      // Save addresses to database and save them back to initial order instance
      order.setBillingAddress(AddressController.createAddress(order.getBillingAddress()));
      order.setShippingAddress(AddressController.createAddress(order.getShippingAddress()));

      // Save the user to the database and save them back to initial order instance
      order.setCustomer(UserController.createUser(order.getCustomer()));

      // TODO: Enable transactions in order for us to not save the order if somethings fails for some of the other inserts.

      // Insert the product in the DB
      int orderID = dbCon.insert(
              "INSERT INTO orders(user_id, billing_address_id, shipping_address_id, order_total, order_created_at, order_updated_at) VALUES("
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
        //Update the productid of the product before returning
        order.setId(orderID);
      }

      // Create an empty list in order to go trough items and then save them back with ID
      ArrayList<LineItem> items = new ArrayList<LineItem>();

      // Save line items to database
      for(LineItem item : order.getLineItems()){
        item = LineItemController.createLineItem(item, order.getId());
        items.add(item);
      }

      order.setLineItems(items);


      dbCon.getConnection().commit();

    }catch (SQLException e){
      e.printStackTrace();
      //if database connection is closed, it will rollback
      if (dbCon!=null){
        try{
          System.out.print("Transaction will be rolled back");
          dbCon.getConnection().rollback();
        }catch(SQLException e1){
          e.printStackTrace();
        }
      }
    } finally {
      try {
        dbCon.getConnection().setAutoCommit(true);
      } catch (SQLException e2){
        e2.printStackTrace();
      }
    }
    // Return order
    return order;
  }







  public static ArrayList<Order> getOrderss() {

    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    String sql = "select orders.*, address.*, line_item.*, product.*, user.*\n" +
            "from address\n" +
            "left join orders on address.a_id=orders.billing_address_id\n" +
            "\n" +
            "left join user on orders.user_id = user.u_id\n" +
            "\n" +
            "left join line_item on line_item.order_id = orders.o_id\n" +
            "\n" +
            "left join product on product.p_id = line_item.product_id\n" +
            "\n" +
            "order by address.a_id\n";

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
          user = new User(rs.getInt("u_id"),
                  rs.getString("first_name"),
                  rs.getString("last_name"),
                  rs.getString("password"),
                  rs.getString("email"));

          product = new Product(rs.getInt("p_id"),
                  rs.getString("product_name"),
                  rs.getString("sku"),
                  rs.getFloat("price"),
                  rs.getString("description"),
                  rs.getInt("stock"));
          //Product(int id, String name, String sku, float price, String description, int stock)

          lineItem = new LineItem(rs.getInt("l_id"),product,
                  rs.getInt("quantity"),
                  rs.getFloat("l_price"));
          lineItemsList.add(lineItem);


          billingsAddress = new Address(rs.getInt("a_id"),
                  rs.getString("name"),
                  rs.getString("street_address"),
                  rs.getString("city"),
                  rs.getString("zipcode")
                  );

          // Create an order from the database data
          Order order =
                  new Order(
                          rs.getInt("o_id"),
                          user,
                          lineItemsList,
                          billingsAddress,
                          rs.getFloat("order_total"),
                          rs.getLong("order_created_at"),
                          rs.getLong("order_updated_at"));

          orders.add(order);

        } else if (rs.getInt("o_id") == orders.get(orders.size()-1).getId()&& rs.getInt("o_id")!=0){
          product = new Product(rs.getInt("p_id"),
                  rs.getString("product_name"),
                  rs.getString("sku"),
                  rs.getFloat("price"),
                  rs.getString("description"),
                  rs.getInt("stock"));
          //Product(int id, String name, String sku, float price, String description, int stock)

          lineItem = new LineItem(rs.getInt("l_id"),product,
                  rs.getInt("quantity"),
                  rs.getFloat("l_price"));
          lineItemsList.add(lineItem);

          orders.get(orders.size()-1).getLineItems().add(lineItem);


        } else if(rs.getInt("o_id")==0){

         shippingAddress = new Address(rs.getInt("a_id"),
                  rs.getString("name"),
                  rs.getString("street_address"),
                  rs.getString("city"),
                  rs.getString("zipcode")
          );

         orders.get(orders.size()-1).setShippingAddress(shippingAddress);

        } else{

          user = new User(rs.getInt("u_id"),
                  rs.getString("first_name"),
                  rs.getString("last_name"),
                  rs.getString("password"),
                  rs.getString("email"));

          product = new Product(rs.getInt("p_id"),
                  rs.getString("product_name"),
                  rs.getString("sku"),
                  rs.getFloat("price"),
                  rs.getString("description"),
                  rs.getInt("stock"));
          //Product(int id, String name, String sku, float price, String description, int stock)

          lineItem = new LineItem(rs.getInt("l_id"),product,
                  rs.getInt("quantity"),
                  rs.getFloat("l_price"));
          lineItemsList.add(lineItem);


          billingsAddress = new Address(rs.getInt("a_id"),
                  rs.getString("name"),
                  rs.getString("street_address"),
                  rs.getString("city"),
                  rs.getString("zipcode")
          );

          // Create an order from the database data
          Order order =
                  new Order(
                          rs.getInt("o_id"),
                          user,
                          lineItemsList,
                          billingsAddress,
                          rs.getFloat("order_total"),
                          rs.getLong("order_created_at"),
                          rs.getLong("order_updated_at"));

          orders.add(order);

          
        }

      }

    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

    // return the orders
    return orders;
  }


  public static Order getOrderr(int id) {

    // check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }


    // Build SQL string to query
    String sql = "select orders.*, address.*, line_item.*, product.*, user.*\n" +
            "from address\n" +
            "left join orders on address.a_id=orders.billing_address_id\n" +
            "\n" +
            "left join user on orders.user_id = user.u_id\n" +
            "\n" +
            "left join line_item on line_item.order_id = orders.o_id\n" +
            "\n" +
            "left join product on product.p_id = line_item.product_id\n" +
            "\n" +
            "order by address.a_id\n";

    // Do the query in the database and create an empty object for the results
    ResultSet rs = dbCon.query(sql);
    Order order = null;


    try {
      while (rs.next()) {
        System.out.println(rs.getRow());
        User user;
        LineItem lineItem;
        Address billingsAddress;
        Address shippingAddress;
        Product product;
        ArrayList <LineItem> lineItemsList;


        if (id==rs.getInt("o_id")){

          if (order==null) {
            user = new User(rs.getInt("u_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("password"),
                    rs.getString("email"));

            product = new Product(rs.getInt("p_id"),
                    rs.getString("product_name"),
                    rs.getString("sku"),
                    rs.getFloat("price"),
                    rs.getString("description"),
                    rs.getInt("stock"));


            lineItem = new LineItem(rs.getInt("l_id"), product,
                    rs.getInt("quantity"),
                    rs.getFloat("l_price"));
            lineItemsList = new ArrayList<>();
            lineItemsList.add(lineItem);


            billingsAddress = new Address(rs.getInt("a_id"),
                    rs.getString("name"),
                    rs.getString("street_address"),
                    rs.getString("city"),
                    rs.getString("zipcode")
            );

            // Create an order from the database data
            order = new Order(
                    rs.getInt("o_id"),
                    user,
                    lineItemsList,
                    billingsAddress,
                    rs.getFloat("order_total"),
                    rs.getLong("order_created_at"),
                    rs.getLong("order_updated_at"));
          }
        } else if (order!=null && rs.getInt("a_id")==order.getBillingAddress().getId()+1){
          shippingAddress = new Address(rs.getInt("a_id"),
                  rs.getString("name"),
                  rs.getString("street_address"),
                  rs.getString("city"),
                  rs.getString("zipcode")
          );
          order.setShippingAddress(shippingAddress);
          rs.afterLast();
        }
      }
      return order;
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }
    // Returns null
    return order;
  }


}
