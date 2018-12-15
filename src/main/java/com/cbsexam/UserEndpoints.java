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

  //This is the cache we save the users in.
  private static UserCache userCache = new UserCache();

  //This variable is used to tell if an update of the cache is needed. Default true.
  private boolean forceUpdate = true;

  /**
   * @param idUser
   * @return Responses
   * 1. The getUser(id) method checks for a user in the cache
   * 2. If it is not there/or cache needs update, it will get it from database.
   * 3. Then the user is converted to Json and encrypted
   * 4. If we can successfully return it to the user we build the json, otherwise we return status 404.
   */

  @GET
  @Path("/{idUser}")
  public Response getUser(@PathParam("idUser") int idUser) {

    // Use the ID to get the user from the controller via the cache where we check for a user first.
    User user = userCache.getUser(forceUpdate, idUser);

    // TODO: Add Encryption to JSON FIX
    // Convert the user object to json in order to return the object and encrypt it.
    String json = new Gson().toJson(user);
    json = Encryption.encryptDecryptXOR(json);

    // Return the user with the status code 200 for success or 404 if failed
// TODO: What should happen if something breaks down? FIX
    if (user != null) {
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
   } else {
     return Response.status(404).entity("this user has not yet been created :-(").build();
    }
  }

  /** @return Responses
   * 1. The getUsers() method checks for users in the cache
   * 2. If there are none there/cache needs update, it will contact database and get them there + create a new cache
   * 3. Then the users are converted to Json and encrypted
   * 4. If we can succesfully return it to the user we build the json, otherwise we return status 404.
   */
  @GET
  @Path("/")
  public Response getUsers() {

    // Write to log know that we are here
    Log.writeLog(this.getClass().getName(), this, "Get all users", 0);

    // Get a list of users by first checking the cache and therein if there is not cache have the usercontroller contact
    // the database and get them + create a new cache.
    ArrayList<User> users = userCache.getUsers(forceUpdate);


    // TODO: Add Encryption to JSON FIX
    // Transfer users to json in order to return it to the user and have ti encrypted
    String json = new Gson().toJson(users);
    json = Encryption.encryptDecryptXOR(json);

    // Return the users with the status code 200 for success or 404 if failed
    if (users != null) {
      // Now that we have created a cache, we do not need to force update before there are changes made.
      this.forceUpdate = false;
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
    } else {
      return Response.status(404).entity("Couldn't find the users :-(").build();
    }
  }

  /** @return Responses
   * 1. The createUser() creates a user based on the information provided from client-side
   * 2. We use the controller to create the user object and save it in the database.
   * 3. Then the user is converted to Json
   * 4. If we can succesfully return it as confirmation to the user: we build the json. Otherwise we return status 400.
   */
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

    // Return the users with the status code 200 for success or 400 if failed
    if (createUser != null) {
      //Now that we have made changes to the amount of users, we set a need for a forced update.
      this.forceUpdate = true;
      // Return a response with status 200 and JSON as type
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
    } else {
      return Response.status(400).entity("Could not create user").build();
    }
  }

  /** @return Responses
   * 1. The loginUser() takes the information given from client-side and turn it to a user object
   * 2. We use the controller to create the user object and save it in the database.
   * 3. Then the user is converted to Json
   * 4. If we can succesfully return the logged in user: we send the message. Otherwise we return status 401.
   */
  // TODO: Make the system able to login users and assign them a token to use throughout the system. FIX - see also utils.Token
  @POST
  @Path("/login")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response loginUser(String body) {

    // Read the json from body and transfer it to a user class
    User userToBe = new Gson().fromJson(body, User.class);

    // Use the email and password to get the user verify the user in the controller which also gives them a token.
    User user = UserController.login(userToBe);

    // Return the user with the status code 200 if succesful or 401 if failed
    if (user != null) {
      //Welcoming the user and providing him/her with the token they need in order to delete or update their user.
      String msg = "Welcome back "+user.getFirstname() + "! You are now logged on and will receive a token - please save" +
              " it, as you will need it throughout the system. This is your token:\n\n"+user.getToken() + "\n\nShould you" +
              "loose your token, you can always log in again :D Enjoy!";
      return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(msg).build();
    } else {
      return Response.status(401).entity("We could not find the user or it does not exist - please try again").build();
    }

  }

  /** @return Responses
   * 1. The deleteUser() methods takes the information given from client-side and turn it to a user object. The only thing
   * they need to provide is their id and the token we gave - ensuring only logged on users can delete themselves, as their ID
   * is incorporated in the token.
   * 2. We use the token-verify function to verify it is the right user. If it checks out, we delete the user from database.
   * If not, we let them know they are not authorised with status 401
   * 3. If we successfully delete the user from the database, we call a force update on cache.
   * 4. Lastly we let the user know if the request was successful or not with status 200 or Otherwise we return status 400.
   */
  // TODO: Make the system able to delete users FIX
  @DELETE
  @Path("/{idUser}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response deleteUser(@PathParam("idUser") int idUser, String body) {

    //Setting a user from the information - note the changes to userobject - we have added token as a instance variable
    User userToDelete = new Gson().fromJson(body, User.class);
    
    // Write to log that we are here
    Log.writeLog(this.getClass().getName(), this, "Deleting a user", 0);

    // Use the ID and token to first verify the possibly delete the user from the database via controller.
    if (Token.verifyToken(userToDelete.getToken(), userToDelete)) {
      boolean deleted = UserController.deleteUser(idUser);

      //if user was deleted we need to force an update on cache and let the user know it was successfull with status 200
      //and a message
      if (deleted) {
        forceUpdate = true;
        // Return a response with status 200 and a massage
        return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity("User deleted").build();
      } else {
        // Return a response with status 200 and a message
        return Response.status(400).entity("Could not delete user - try again").build();
      }
    } else {
      //If the token verifier does not check out.
      return Response.status(401).entity("You're not authorized to do this - please log in").build();
    }
  }

  /** @return Responses
   * 1. The updateUser() methods takes the information given from client-side and turn it to a user object. The only thing
   * they need to provide is their id, the token we gave and their information with the desired changes in it.
   * - the token ensures only logged on users can update themselves, as their ID is incorporated in the token.
   * 2. We use the token-verify function to verify it is the right user. If it checks out, we update the user in the database.
   * If not, we let them know they are not authorised with status 401.
   * 3. If we successfully delete the user from the database, we call a force update on cache.
   * 4. Lastly we let the user know if the request was successful or not with status 200 or Otherwise we return status 400.
   */
  // TODO: Make the system able to update users FIX
  @PUT
  @Path("/update/{idUser}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateUser(@PathParam("idUser") int idUser, String body) {

    //Setting a user from the information - note the changes to the user object - we have added token as a instance variable
    User userToUpdate = new Gson().fromJson(body, User.class);

    //Writing log to let know we are here.
    Log.writeLog(this.getClass().getName(), this, "Updating a user", 0);

    // Use the ID and token to first verify the possibly update the user in the database via controller.
    if (Token.verifyToken(userToUpdate.getToken(), userToUpdate)) {
      boolean affected = UserController.updateUser(userToUpdate);

      //If we have updated the user, we need to force an update on cache and let the user know it was successfull with status 200
      //and returning the json.
      if (affected) {
        forceUpdate = true;
        String json = new Gson().toJson(userToUpdate);

        //Returning responses to user
        return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(json).build();
      } else {
        return Response.status(400).entity("Could not update user").build();
      }
    } else {
      //If the token verifier does not check out.
      return Response.status(401).entity("You're not authorized to do this - please log in").build();
    }
  }

}
