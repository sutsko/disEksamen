package controllers;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import model.*;
import utils.Log;


public class OrderController {

  private static DatabaseController dbCon;

  public OrderController() {
    dbCon = new DatabaseController();
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

      order.setBillingAddress(AddressController.createAddress(order.getBillingAddress()));

      order.setShippingAddress(AddressController.createAddress(order.getShippingAddress()));

      order.setCustomer(UserController.getUser(order.getCustomer().getId()));
      // Save addresses to database and save them back to initial order instance


      // TODO: Enable transactions in order for us to not save the order if somethings fails for some of the other inserts.: FIX
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
        //Update the order of the order before returning
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

      // Return order
      return order;

      //adding nullpointerexception, since we are using getUser() instead of create user - we would like people to be logged
    } catch (SQLException | NullPointerException e) {
      System.out.println(e.getMessage());
      try {
        System.out.println("rolling back");
        dbCon.getConnection().rollback();
      } catch (SQLException ex) {
        ex.printStackTrace();
      }
    }
    finally {
      try {
        dbCon.getConnection().setAutoCommit(true);
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return  null;
  }




  public static Order getOrder(int id) {

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
        User user;
        LineItem lineItem;
        Address billingsAddress;
        Address shippingAddress;
        Product product;
        ArrayList <LineItem> lineItemsList;


        if (id==rs.getInt("o_id")){

          if (order==null) {

            user = UserController.formUser(rs);

            product = ProductController.formProduct(rs);


            lineItem = LineItemController.formLineItem(rs, product);
            lineItemsList = new ArrayList<>();
            lineItemsList.add(lineItem);


            billingsAddress = AddressController.formAddress(rs);

            // Create an order from the database data
            order = formOrder(rs, user,lineItemsList,billingsAddress);
          }
        } else if (order!=null && rs.getInt("a_id")==order.getBillingAddress().getId()+1){

          shippingAddress = AddressController.formAddress(rs);
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

  public static ArrayList<Order> getOrders() {

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
