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

  public static User getUser(int id) {

    // Check for connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Build the query for DB
    String sql = "SELECT * FROM user where u_id=" + id;

    // Actually do the query
    ResultSet rs = dbCon.query(sql);
    User user = null;

    try {
      // Get first object, since we only have one
      if (rs.next()) {
        user =
            new User(
                rs.getInt("u_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("password"),
                rs.getString("email"));

        // return the create object
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
   * Get all users in database
   *
   * @return
   */
  public static ArrayList<User> getUsers() {

    // Check for DB connection
    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    // Build SQL
    String sql = "SELECT * FROM user";

    // Do the query and initialyze an empty list for use if we don't get results
    ResultSet rs = dbCon.query(sql);
    ArrayList<User> users = new ArrayList<User>();

    try {
      // Loop through DB Data
      while (rs.next()) {
        User user =
            new User(
                rs.getInt("u_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("password"),
                rs.getString("email"));
        // Add element to list
        users.add(user);
      }
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    }

    // Return the list of users
    return users;
  }

  public static User createUser(User user) {

    // Write in log that we've reach this step
    Log.writeLog(UserController.class.getName(), user, "Actually creating a user in DB", 0);

    /** kan man fjerne 100L?**/
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

  public static boolean updateUser(User user) {

    Log.writeLog(UserController.class.getName(), user, "Actually updating a user in DB", 0);

    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    user.setPassword(Hashing.sha(user.getPassword()));

    boolean affected = dbCon.update(
            "UPDATE user SET " +
                    "first_name = " + "'" + user.getFirstname() + "'," +
                    "last_name = " + "'" + user.getLastname() + "'," +
                    "password = " + "'" + user.getPassword() + "'," +
                    "email = " + "'" + user.getEmail() + "'" +
                    "WHERE u_id = " + "'" + user.getId() + "'");

    return affected;
  }

  public static User login(User user) {

    // Write in log that we've reach this step
    Log.writeLog(UserController.class.getName(), user, "Trying to log on", 0);

    if (dbCon == null) {
      dbCon = new DatabaseController();
    }

    user.setPassword(Hashing.sha(user.getPassword()));

    try {
      /** til rapporten - intetn forhindre i at to har samme email og password**/

      String sql = "SELECT * FROM user WHERE email = ? AND password = ?";

      PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql);
      preparedStatement.setString(1, user.getEmail());
      preparedStatement.setString(2, user.getPassword());

      ResultSet rs = preparedStatement.executeQuery();

      if (rs.next()) {
        user =
                new User(
                        rs.getInt("u_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("password"),
                        rs.getString("email"));

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
