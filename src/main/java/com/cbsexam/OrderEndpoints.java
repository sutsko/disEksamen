package com.cbsexam;

import cache.OrderCache;

import com.google.gson.Gson;
import com.sun.jmx.remote.util.OrderClassLoaders;
import controllers.OrderController;
import java.util.ArrayList;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import model.Order;
import utils.Encryption;
import controllers.UserController;

@Path("orders")
public class OrderEndpoints {

  //This is the cache we save the orders in.
  private static OrderCache orderCache = new OrderCache();

  //This variable is used to tell if an update of the cache is needed. Default true.
  private static boolean forceUpdate=true;

  /**
   * @param idOrder
   * @return Responses
   * 1. The getOrder(id) method checks for an order in the cache
   * 2. If it is not there / cache needs update, it will get it from database.
   * 3. Then the order is converted to Json and encrypted
   * 4. If we can succesfully return it to the user we build the json, otherwise we return status 404.
   */
  @GET
  @Path("/{idOrder}")
  public Response getOrder(@PathParam("idOrder") int idOrder) {

    // Call our cache and maybe our controller-layer in order to get the order from the DB,
    // if it is not in the cache already
    Order order = orderCache.getOrder(forceUpdate,idOrder);

    // TODO: Add Encryption to JSON FIX
    // We convert the java object to json with GSON library imported in Maven and encrypt it with XOR
    String json = new Gson().toJson(order);
    json = Encryption.encryptDecryptXOR(json);

    // Return the data to the user if there was an order
    if (order != null) {
      // Return a response with status 200 and JSON as type
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
    } else {
      // Return a response with status 400 and a message in text
      return Response.status(404).entity("Could not get the order").build();
    }
  }

  /** @return Responses
   * 1. The getOrders() method checks for orders in the cache
   * 2. If there are none there/cache needs update, it will contact database and get them there + create a cache
   * 3. Then the order is converted to Json and encrypted
   * 4. If we can succesfully return it to the user; we build the json, otherwise we return status 404.
   */
  @GET
  @Path("/")
  public Response getOrders() {

    // Call our cache in order to get the order from there or DB
    ArrayList<Order> orders = orderCache.getOrders(forceUpdate);

    // TODO: Add Encryption to JSON FIX
    // We convert the java object to json with GSON library imported in Maven
    String json = new Gson().toJson(orders);
    json = Encryption.encryptDecryptXOR(json);

    //Setting the forceUpdate to false, since we just created a new cache
    this.forceUpdate = false;

    // Return the data to the user
    if (orders != null) {
      this.forceUpdate = true;
      // Return a response with status 200 and JSON as type
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
    } else {
      // Return a response with status 400 and a message in text
      return Response.status(404).entity("Could not get the orders").build();
    }

  }

  /** @return Responses
   * 1. The createOrder() creates an order based on the information provided from client-side
   * 2. We use the controller to create the order and save it in the database.
   * 3. Then the order is converted to Json
   * 4. If we can succesfully return it as confirmation to the user we build the json, otherwise we return status 400.
   */
  @POST
  @Path("/")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createOrder(String body) {

    // Read the json from body and transfer it to a order class
    Order newOrder = new Gson().fromJson(body, Order.class);

    // Use the controller to add the user
    Order createdOrder = OrderController.createOrder(newOrder);

    // Get the user back with the added ID and return it to the user
    String json = new Gson().toJson(createdOrder);

    // Return the data to the user
    if (createdOrder != null) {
      this.forceUpdate = true;
      // Return a response with status 200 and JSON as type
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
    } else {

      // Return a response with status 400 and a message in text
      return Response.status(400).entity("Could not create order").build();
    }
  }
}