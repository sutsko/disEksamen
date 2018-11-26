package cache;

import controllers.OrderController;
import java.util.ArrayList;
import model.Order;
import utils.Config;

//TODO: Build this cache and use it. FIX
public class OrderCache {

    // List of orders
    private static ArrayList<Order> orders;

    // Time cache should live
    private long ttl;

    // Sets when the cache has been created
    private static long created;

    public OrderCache() {
        this.ttl = Config.getOrderTtl();
    }

    public ArrayList<Order> getOrders(Boolean forceUpdate) {
        // If we whis to clear cache, we can set force update.
        // Otherwise we look at the age of the cache and figure out if we should update.
        // If the list is empty we also check for new products
        if (forceUpdate
                || ((this.created + this.ttl) <= (System.currentTimeMillis())) || this.orders==null) {

            System.out.println("hej1");
            // Get orders from controller, since we wish to update.
            ArrayList<Order> orders = OrderController.getOrders();

            // Set orders for the instance and set created timestamp
            this.orders = orders;
            this.created = System.currentTimeMillis();

        }

        // Return the documents
        return this.orders;
    }

    public Order getOrder(boolean forceUpdate, int orderID) {
        Order order = new Order();

        // If we whis to clear cache, we can set force update.
        // Otherwise we look at the age of the cache and figure out if we should update.
        // If the list is empty we also check for new products
        if (forceUpdate) {
            System.out.println("hej");

            order = OrderController.getOrder(orderID);

            return order;
        } else {
            for (Order o : orders){
                if (orderID==o.getId())
                    order = o;
                return order;
            }
        }
        return null;
    }
}
