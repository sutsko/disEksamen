
package cache;

        import controllers.UserController;
        import java.util.ArrayList;
        import model.User;
        import utils.Config;

//TODO: Build this cache and use it. FIX
public class UserCache {

    // List of users
    private static ArrayList<User> users;

    // Time cache should live
    private long ttl;

    // Sets when the cache has been created
    private static long created;

    public UserCache() {
        this.ttl = Config.getUserTtl();
    }

    public ArrayList<User> getUsers(Boolean forceUpdate) {
        /**If we whis to clear cache, we can set force update.
        // Otherwise we look at the age of the cache and figure out if we should update.
        // If the list is empty we also check for new products
         // otherwise we return our already made cache
         **/
        if (forceUpdate
                || ((this.created + this.ttl) <= (System.currentTimeMillis())) || this.users==null) {

            // Get users from controller, since we wish to update.
            ArrayList<User> users = UserController.getUsers();

            // Set users for the instance and set created timestamp
            this.users = users;
            this.created = System.currentTimeMillis();
        }
        // Return the users
        return this.users;
    }

    public User getUser(boolean forceUpdate, int userID) {
        User user = new User();

        /**If we wish to clear cache, we can set force update.
         // Otherwise we look at the age of the cache and figure out if we should get user from database.
         // If the list is empty we also get the product from the database directly.
         // otherwise we check our our already made cache and return the user
         **/
        if (forceUpdate
                || ((this.created + this.ttl) <= (System.currentTimeMillis())) || this.users==null) {

            // Get user from controller.
            user = UserController.getUser(userID);

            return user;
        } else {
            // Get user from already made arraylist by checking against ID
            for (User u : users){
                if (userID==u.getId())
                    user = u;
                return user;
            }
        }

        return null;
    }

}
