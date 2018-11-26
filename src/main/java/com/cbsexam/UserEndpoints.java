package com.cbsexam;

import cache.UserCache;
import com.google.gson.Gson;
import controllers.UserController;
import java.util.ArrayList;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import model.User;
import utils.Encryption;
import utils.Log;
import utils.Token;

@Path("user")
public class UserEndpoints {

  UserCache userCache = new UserCache();
  /**Kan den her boolean være private?**/
  private boolean forceUpdate = true;

  /**
   * @param idUser
   * @return Responses
   */
  @GET
  @Path("/{idUser}")
  public Response getUser(@PathParam("idUser") int idUser) {

    // Use the ID to get the user from the controller.
    User user = UserController.getUser(idUser);

    // TODO: Add Encryption to JSON FIX
    // Convert the user object to json in order to return the object
    String json = new Gson().toJson(user);
    json = Encryption.encryptDecryptXOR(json);

    // Return the user with the status code 200
// TODO: What should happen if something breaks down? FIX
    if (user != null) {
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
   } else {
     return Response.status(400).entity("this user has not yet been created :-(").build();
    }
  }

  /** @return Responses */
  @GET
  @Path("/")
  public Response getUsers() {

    // Write to log that we are here
    Log.writeLog(this.getClass().getName(), this, "Get all users", 0);

    // Get a list of users
    ArrayList<User> users = userCache.getUsers(forceUpdate);


    // TODO: Add Encryption to JSON FIX
    // Transfer users to json in order to return it to the user
    String json = new Gson().toJson(users);
    json = Encryption.encryptDecryptXOR(json);

    this.forceUpdate = false;
    // Return the users with the status code 200
    return Response.status(200).type(MediaType.APPLICATION_JSON).entity(json).build();
  }

  @POST
  @Path("/")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createUser(String body) {

    // Read the json from body and transfer it to a user class
    User newUser = new Gson().fromJson(body, User.class);

    // Use the controller to add the user
    User createUser = UserController.createUser(newUser);

    // Get the user back with the added ID and return it to the user
    String json = new Gson().toJson(createUser);

    // Return the data to the user
    if (createUser != null) {
      this.forceUpdate = true;
      // Return a response with status 200 and JSON as type
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
    } else {
      return Response.status(400).entity("Could not create user").build();
    }
  }

  // TODO: Make the system able to login users and assign them a token to use throughout the system.
  @POST
  @Path("/login")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response loginUser(String body) {

    User userToBe = new Gson().fromJson(body, User.class);

    // Use the email and password to get the user from the controller.
    User user = UserController.login(userToBe);

    // Return the user with the status code 200 if succesful
    if (user != null) {
      String msg = "Welcome back "+user.getFirstname() + "! You are now logged on and will receive a token - please save" +
              " it, as you will need it throughout the system. This is your token:\n\n"+user.getToken() + "\n\nShould you" +
              "loose your token, you can always log in again :D Enjoy!";
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(msg).build();
    } else {
      return Response.status(400).entity("We could not find the user - please try again").build();
    }

  }

  // TODO: Make the system able to delete users FIX
  @DELETE
  @Path("/{idUser}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response deleteUser(@PathParam("idUser") int idUser, String body) {

    User userToDelete = new Gson().fromJson(body, User.class);

    // Write to log that we are here
    Log.writeLog(this.getClass().getName(), this, "Deleting a user", 0);

    if (Token.verifyToken(userToDelete.getToken(), userToDelete)) {
      // Use the ID to delete the user from the database via controller.
      boolean deleted = UserController.deleteUser(idUser);
      if (deleted) {
        forceUpdate = true;
        // Return a response with status 200 and JSON as type
        /**ændre tekst**/
        return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity("User deleted").build();
      } else {
        // Return a response with status 200 and JSON as type
        /**ændre tekst**/
        return Response.status(400).entity("Could not delete user").build();
      }
    } else {
      /**ændre tekst**/
      return Response.status(401).entity("You're not authorized to do this").build();
    }
  }


  // TODO: Make the system able to update users FIX
  @PUT
  @Path("/update/{idUser}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateUser(@PathParam("idUser") int idUser, String body) {

    User userToUpdate = new Gson().fromJson(body, User.class);

    Log.writeLog(this.getClass().getName(), this, "Updating a user", 0);

    if (Token.verifyToken(userToUpdate.getToken(), userToUpdate)) {
      boolean affected = UserController.updateUser(userToUpdate);
      if (affected) {
        forceUpdate = true;
        String json = new Gson().toJson(userToUpdate);
        return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
      } else {
        return Response.status(400).entity("Could not update user").build();
      }
    } else {
      return Response.status(401).entity("You're not authorized to do this").build();
    }
  }

}
