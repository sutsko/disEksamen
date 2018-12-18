package controllers;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import model.User;
import utils.Log;
import utils.Hashing;
import utils.Token;

public class UserController {

  private static DatabaseController dbCon;
  public UserController() {
    dbCon = new DatabaseController();
  }


  /** @param id
   * @return Product
   * 1. The getUser() methods gets the user based on the id from the database
   * 2. First we build an SQL query to get all information regarding our user in a resultset
   * 3. We excecute prepared statement and form the user to be returned.
   */
  public static User getUser(int id) {

    // Check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Build the query for DB
    String sql = "SELECT * FROM user where u_id=" + id;

    // Actually do the query, declare an object to null.
    ResultSet rs = dbCon.query(sql);
    User user = null;

    try {
      // Get first object, since we only have one, form the user and return it.
      if (rs.next()) {
        user = formUser(rs);
        return user;
      } else {
        System.out.println("No user found");
      }
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

    // Return null
    return user;
  }

  /**
   * @return User arraylist
   * 1. The getUsers() methods gets the all the users from the database
   * 2. First we build an SQL query to get all information regarding our users in a resultset
   * 3. We excecute prepared statement and form the users to be returned. We add the users to arraylist and returns.
   */
  public static ArrayList<User> getUsers() {

    // Check for DB connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Build SQL
    String sql = "SELECT * FROM user";

    // Do the query and initialize an empty list for use if we don't get results
    ResultSet rs = dbCon.query(sql);
    ArrayList<User> users = new ArrayList<User>();

    try {
      // Loop through DB Data
      while (rs.next()) {
        User user = formUser(rs);
        // Add element to list
        users.add(user);
      }
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

    // Return the list of users
    return users;
  }

  /** @param user
   * @return user
   * 1. The createUser() methods takes the User we initialized in the endpoint
   * 2. First we set a created_at for the user
   * 3. Next we build the SQL-prepared statement and insert it to our database
   * 4. The genereated key is set to the user_id, and the user is returned.
   */
  public static User createUser(User user) {

    // Write in log that we've reach this step
    Log.writeLog(UserController.class.getName(), user, "Actually creating a user in DB", 0);


    // Set creation time for user.
    user.setCreatedTime(System.currentTimeMillis() / 1000L);

    // Check for DB Connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }
    // Insert the user in the DB

    // TODO: Hash the user password before saving it. FIX
    //Based on the users password, we use the hashing utill to hash the password. We use sha.
    user.setPassword(Hashing.sha(user.getPassword()));

    int userID = dbCon.insert(
        "INSERT INTO user(first_name, last_name, password, email, created_at) VALUES('"
            + user.getFirstname()
            + "', '"
            + user.getLastname()
            + "', '"
            + user.getPassword()
            + "', '"
            + user.getEmail()
            + "', "
            + user.getCreatedTime()
            + ")");

    if (userID != 0) {
      //Update the userid of the user before returning
      user.setId(userID);
    } else{
      // Return null if user has not been inserted into database
      return null;
    }

    // Return user
    return user;
  }

  /** @param user
   * @return affected
   * 1. The updateUser() methods takes the User we initialized in the endpoint
   * 2. First we hash the password
   * 3. Next we build the SQL-prepared statement and update it in our database
   * 4. If the update is succesful, it will return true. False if not. The affected value is return.
   */
  public static boolean updateUser(User user) {

    //Setting log to know where we are
    Log.writeLog(UserController.class.getName(), user, "Actually updating a user in DB", 0);

    //Checking for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    //Hashing user password
    user.setPassword(Hashing.sha(user.getPassword()));

    //Updating in database, and declaring a boolean based on the answer. Return it afterwards.
    boolean affected = dbCon.update(
            "UPDATE user SET " +
                    "first_name = " + "'" + user.getFirstname() + "'," +
                    "last_name = " + "'" + user.getLastname() + "'," +
                    "password = " + "'" + user.getPassword() + "'," +
                    "email = " + "'" + user.getEmail() + "'" +
                    "WHERE u_id = " + "'" + user.getId() + "'");

    return affected;
  }

  /** @param user
   * @return user
   * 1. The login() methods takes the User we initialized in the endpoint
   * 2. First we hash the password
   * 3. Next we build the SQL-prepared statement and compare it with our user-data in the database.
   * 4. If a match is found a token will be generated and declared to the user. User is returned
   */
  public static User login(User user) {

    // Write in log that we've reach this step
    Log.writeLog(UserController.class.getName(), user, "Trying to log on", 0);

    //Check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    //Hashing password
    user.setPassword(Hashing.sha(user.getPassword()));

    try {
      //Building SQL statement and executing query
      String sql = "SELECT * FROM user WHERE email = ? AND password = ?";

      PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql);
      preparedStatement.setString(1, user.getEmail());
      preparedStatement.setString(2, user.getPassword());

      ResultSet rs = preparedStatement.executeQuery();

      //Loop through resultset once, form the user and generate a token and return user
      if (rs.next()) {
        user = formUser(rs);
        user.setToken(Token.generateToken(user));

        return user;
      } else {
        System.out.println("No user found");
      }
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
    return null;
  }

  /** @param idUser
   * @return deleted
   * 1. The deleteUser() methods deletes the user from the database based on the id of the user
   * 2. Next we build the SQL-prepared statement and compare it with our user-data in the database.
   * 3. If the delete is succesful, it will return true. False if not. The deleted value is return.
   */
  public static boolean deleteUser(int idUser) {

      // Check for DB Connection
      if (dbCon == null) {
        dbCon = new DatabaseController();
      }
      // Build the query for DB
      String sql = "Delete FROM user where u_id=" + idUser;

      boolean deleted = dbCon.delete(sql);

      return deleted;

  }

  /** @param rs
   * @return user (u)
   * initialising, instansiating and declaring a user, to be returned.
   */
  public static User formUser(ResultSet rs){

    try{
    User u = new User(rs.getInt("u_id"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("password"),
            rs.getString("email"));

    return u;
    }catch(SQLException e){

    }
    return null;
  }


}
