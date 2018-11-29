package com.cbsexam;

import cache.ProductCache;
import com.google.gson.Gson;
import controllers.ProductController;
import java.util.ArrayList;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import model.Product;
import utils.Encryption;

@Path("product")
public class ProductEndpoints {

  //This is the cache we save the products in.
  public static ProductCache cache = new ProductCache();

  //This variable is used to tell if an update of the cache is needed. Default true.
  private static boolean forceUpdate=true;

  /**
   * @param idProduct
   * @return Responses
   * 1. The getProduct(id) method checks for a product in the cache
   * 2. If it is not there/cache needs update, it will get it from database.
   * 3. Then the product is converted to Json and encrypted
   * 4. If we can successfully return it to the user we build the json, otherwise we return status 404.
   */
  @GET
  @Path("/{idProduct}")
  public Response getProduct(@PathParam("idProduct") int idProduct) {

    // Call our controller-layer in product to get the product from the DB
    Product product = cache.getProduct(forceUpdate, idProduct);

    // TODO: Add Encryption to JSON FIX
    // We convert the java object to json with GSON library imported in Maven
    String json = new Gson().toJson(product);
    json= Encryption.encryptDecryptXOR(json);

    // Return the data to the user
    if (product != null) {
      // Return a response with status 200 and JSON as type
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
    } else {
      return Response.status(404).entity("Could not get the product").build();
    }
  }

  /** @return Responses
   * 1. The getProducts() method checks for products in the cache
   * 2. If there are none there / cache needs update, it will contact database and get them there + create a cache
   * 3. Then the products are converted to Json and encrypted
   * 4. If we can succesfully return it to the user we build the json, otherwise we return status 404.
   */
  @GET
  @Path("/")
  public Response getProducts() {

    // Call our cache and maybe our controller-layer in order to get the order from the DB
    ArrayList<Product> products = cache.getProducts(forceUpdate);

    // TODO: Add Encryption to JSON FIX
    // We convert the java object to json with GSON library imported in Maven and encrypt it.
    String json = new Gson().toJson(products);
    json = Encryption.encryptDecryptXOR(json);

    // Return the data to the user
    if (products != null) {
      //Now that we have got all the products and created a cache, we do not need to force an update.
      this.forceUpdate = false;
      // Return a response with status 200 and JSON as type or 400 if condition is failed
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
    } else {
      return Response.status(404).entity("Could not get the products").build();
    }
  }


  /** @return Responses
   * 1. The createProduct() creates a products based on the information provided from client-side
   * 2. We use the controller to create the product object and save it in the database.
   * 3. Then the product is converted to Json
   * 4. If we can succesfully return it as confirmation to the user: we build the json. Otherwise we return status 400.
   */
  @POST
  @Path("/")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createProduct(String body) {

    // Read the json from body and transfer it to a product class
    Product newProduct = new Gson().fromJson(body, Product.class);

    // Use the controller to add the product
    Product createdProduct = ProductController.createProduct(newProduct);

    // Get the user back with the added ID and return it to the user
    String json = new Gson().toJson(createdProduct);

    // Return the data to the user
    if (createdProduct != null) {
     //Letting know, that a new product has been created, and therefore, the cache should be updated when getting the products
      this.forceUpdate = true;
      // Return a response with status 200 and JSON as type
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
    } else {
      return Response.status(400).entity("Could not create product").build();
    }
  }
}
