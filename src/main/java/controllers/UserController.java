package controllers;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

    ResultSet rs = null;

    try{

    // Check for connection
    if (dbCon == null || dbCon.getConnection().isClosed()) {
      dbCon = new DatabaseController();
    }

          //Building SQL statement and executing query
          String sql = "SELECT * FROM user where u_id = ?";
          // Build the query for DB
          PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql);
          preparedStatement.setInt(1, id);


          // Actually do the query, declare an object to null.
          rs = preparedStatement.executeQuery();
          User user = null;


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
      finally {
      try {
        rs.close();
      }
        catch (SQLException h) {
        h.printStackTrace();
          try {
          dbCon.getConnection().close();

        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
    }
    // Return null
    return null;
  }



  /**
   * @return User arraylist
   * 1. The getUsers() methods gets the all the users from the database
   * 2. First we build an SQL query to get all information regarding our users in a resultset
   * 3. We excecute prepared statement and form the users to be returned. We add the users to arraylist and returns.
   */
  public static ArrayList<User> getUsers() {

    ResultSet rs = null;

    try{
    // Check for DB connection
    if (dbCon == null || dbCon.getConnection().isClosed()) {
      dbCon = new DatabaseController();
    }

      //Building SQL statement and executing query
           String sql = "SELECT * FROM user";

           PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql);

           rs = preparedStatement.executeQuery();

    ArrayList<User> users = new ArrayList<User>();

      // Loop through DB Data
      while (rs.next()) {
        User user = formUser(rs);
        // Add element to list
        users.add(user);
      }
      return users;
    } catch (SQLException ex) {
      System.out.println(ex.getMessage());
    } finally {
      try {
        rs.close();
      } catch (SQLException h) {
        h.printStackTrace();
        try {
          dbCon.getConnection().close();

        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
    }
    // Return the list of users
    return null;
  }



  /** @param user
   * @return user
   * 1. The createUser() methods takes the User we initialized in the endpoint
   * 2. First we set a created_at for the user
   * 3. Next we build the SQL-prepared statement and insert it to our database
   * 4. The genereated key is set to the user_id, and the user is returned.
   */
  public static User createUser(User user) {

    try{
    // Write in log that we've reach this step
    Log.writeLog(UserController.class.getName(), user, "Actually creating a user in DB", 0);


    // Set creation time for user.
    user.setCreatedTime(System.currentTimeMillis() / 1000L);

    // Check for DB Connection
    if (dbCon == null || dbCon.getConnection().isClosed()) {
      dbCon = new DatabaseController();
    }


      // TODO: Hash the user password before saving it. FIX
      //Based on the users password, we use the hashing utill to hash the password. We use sha.
      user.setPassword(Hashing.sha(user.getPassword()));

      // Insert the user in the DB
      //Building SQL statement and executing query
            String sql = "INSERT INTO user(first_name, last_name, password, email, created_at) VALUES(?,?,?,?,?)";

            PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, user.getFirstname());
            preparedStatement.setString(2, user.getLastname());
            preparedStatement.setString(3, user.getPassword()  );
            preparedStatement.setString(4, user.getEmail());
            preparedStatement.setLong(5, user.getCreatedTime());


            int affectedRows = preparedStatement.executeUpdate();

            // Get our key back in order to apply it to an object as ID
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next() && affectedRows==1) {
              user.setId(generatedKeys.getInt(1));

              return user;
            } else {
              // Return null if user has not been inserted into database
              return null;
            }
    }catch (SQLException e){
      e.printStackTrace();
    }finally {
      try {
        dbCon.getConnection().close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }

    // Return user
    return user;
  }



  /** @param user
   * @return boolean
   * 1. The updateUser() methods takes the User we initialized in the endpoint
   * 2. First we hash the password
   * 3. Next we build the SQL-prepared statement and update it in our database
   * 4. If the update is succesful, it will return true. False if not. The affected value is return.
   */
  public static boolean updateUser(User user) {

    try {
      //Setting log to know where we are
      Log.writeLog(UserController.class.getName(), user, "Actually updating a user in DB", 0);

      //Checking for connection
      if (dbCon == null || dbCon.getConnection().isValid(1)) {
        dbCon = new DatabaseController();
      }

      //Hashing user password
      user.setPassword(Hashing.sha(user.getPassword()));

      //Building SQL statement and executing query
      String sql = "UPDATE user SET first_name = ?, last_name = ?, password = ?, email = ? WHERE u_id = ?";

      PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      preparedStatement.setString(1, user.getFirstname());
      preparedStatement.setString(2, user.getLastname());
      preparedStatement.setString(3, user.getPassword());
      preparedStatement.setString(4, user.getEmail());
      preparedStatement.setLong(5, user.getId());


      int rowsAffected = preparedStatement.executeUpdate();

      if (rowsAffected==1){
        return true;
      } else {
        return false;
      }

    }catch (SQLException e){
      e.printStackTrace();
    }
    finally {
      try {
        dbCon.getConnection().close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return false;
  }



  /** @param user
   * @return user
   * 1. The login() methods takes the User we initialized in the endpoint
   * 2. First we hash the password
   * 3. Next we build the SQL-prepared statement and compare it with our user-data in the database.
   * 4. If a match is found a token will be generated and declared to the user. User is returned
   */
  public static User login(User user) {

    ResultSet rs = null;
    // Write in log that we've reach this step
    Log.writeLog(UserController.class.getName(), user, "Trying to log on", 0);
    try {
    //Check for connection
    if (dbCon == null || dbCon.getConnection().isClosed() ) {
      dbCon = new DatabaseController();
    }

      //Hashing password
      user.setPassword(Hashing.sha(user.getPassword()));


      //Building SQL statement and executing query
      String sql = "SELECT * FROM user WHERE email = ? AND password = ?";

      PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql);
      preparedStatement.setString(1, user.getEmail());
      preparedStatement.setString(2, user.getPassword());

      rs = preparedStatement.executeQuery();

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
    } finally {
      try{
        rs.close();

      }catch (SQLException h){
        h.printStackTrace();
        try {
          dbCon.getConnection().close();
        }catch (SQLException e){
         e.printStackTrace();
        }
      }
    }
    return null;
  }

  /** @param idUser
   * @return boolean
   * 1. The deleteUser() methods deletes the user from the database based on the id of the user
   * 2. Next we build the SQL-prepared statement and compare it with our user-data in the database.
   * 3. If the delete is succesful, it will return true. False if not. The deleted value is return.
   */
  public static boolean deleteUser(int idUser) {

    try {
      // Check for DB Connection
      if (dbCon == null || dbCon.getConnection().isClosed()) {
        dbCon = new DatabaseController();
      }

      //Building SQL statement and executing query
      String sql = "Delete FROM user where u_id = ?";
      // Build the query for DB
      PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql);

      preparedStatement.setInt(1, idUser);

      int affectedRows = preparedStatement.executeUpdate();

      if (affectedRows == 1) {
        return true;
      }else{
        return false;
      }

    }catch (SQLException e){
      e.printStackTrace();
    }finally {
      try {
        dbCon.getConnection().close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return false;
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
