/**
 * To implement
 */

package shield;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;

public class ShieldingIndividualClientImp implements ShieldingIndividualClient {

    /** The endpoint of the server **/
    private String endpoint;
    /** whether the shielding individual is registered **/
    private boolean registered;
    /** CHI number of the shielding individual **/
    private String CHI;
    /** Postcode of the shielding individual's current location **/
    private String postcode;
    /** Whether the shielding individual has ordered this week **/
    private boolean orderedThisWeek;
    /** the id of the food box the shielding individual has picked **/
    private String pickedFoodBoxId;
    /** all food boxes available (no dietary preference restricted) **/
    private List<FoodBox> allFoodBox;
    /** A map that has each id of a food box to its corresponding food box object **/
    private Map<String, FoodBox> idToFoodBox;
    /** the id of the current processing order **/
    private int currentOrderId;
    /** A map that has each id of an order to its corresponding order object **/
    private Map<Integer, Order> idToOrder;
    /** all orders the shielding individual has placed **/
    private List<Order> allOrders;

    public ShieldingIndividualClientImp(String endpoint) {
        this.endpoint = endpoint;
        this.registered = false;
        this.idToFoodBox = new HashMap<>();
        this.idToOrder = new HashMap<>();
        this.allOrders = new ArrayList<>();
        this.allFoodBox = new ArrayList<>();
        for (String id: showFoodBoxes("")) {
            idToFoodBox.put(id, getFoodBoxById(id));
        }
    }

    /**
     * registers a shielding individual based on the CHI number.
     * @param CHI CHI number of the shielding individual
     * @return A boolean value represents the success of the register action. Returns false
     * when the CHI is invalid or the server throws an exception. Returns true if the
     * register is successful or the CHI is already registered.
     */
    @Override
    public boolean registerShieldingIndividual(String CHI) {
        if (CHI.length() != 10 || !validCHI(CHI)) {
            return false;
        }
        String request = "/registerShieldingIndividual?CHI=" + CHI;

        try {
            String response = ClientIO.doGETRequest(endpoint + request);
            if (!response.equals("already registered")) {
                this.registered = true;
                this.CHI = CHI;
                String[] personalInfo = new Gson().fromJson(response, String[].class);
                this.postcode = personalInfo[0].replace(' ', '_');
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * show food boxes for a particular dietary preference passed in as a parameter.
     * @param dietaryPreference The dietary preference for the food box. if an empty string
     *                          is used then show all available food boxes regardless of
     *                          dietary preference
     * @return a collection of the food box ids of the queried food boxes
     */
    @Override
    public Collection<String> showFoodBoxes(String dietaryPreference) {
        String request = "/showFoodBox?orderOption=catering&dietaryPreference=" + dietaryPreference;

        List<FoodBox> allFoodBoxes;
        List<String> foodBoxIds = new ArrayList<>();

        try {
            String response = ClientIO.doGETRequest(endpoint + request);
            Type foodBoxListType = new TypeToken<List<FoodBox>>() {} .getType();
            allFoodBoxes = new Gson().fromJson(response, foodBoxListType);

            for (FoodBox foodBox: allFoodBoxes) {
                foodBoxIds.add(foodBox.getId());
            }
        } catch (Exception e) {
            return null;
        }
        if (dietaryPreference.equals("")) {
            this.allFoodBox = allFoodBoxes;
        }
        return foodBoxIds;
    }

    /**
     * place an order. Checks preconditions whether the shielding individual is registered,
     * whether an order has been placed this week and whether the shielding individual has picked
     * a valid food box.
     * @return a boolean value represents whether the order is successfully placed.
     */
    @Override
    public boolean placeOrder() {
        if (!isRegistered() || orderedThisWeek || pickedFoodBoxId == null) {
            return false;
        }
        showFoodBoxes(""); // Update the list of the food boxes
        String closestCateringCompany = getClosestCateringCompany();
        String providerName = closestCateringCompany.split(",")[1];
        String providerPostcode = closestCateringCompany.split(",")[2];
        String request = String.format("/placeOrder?individual_id=%s&catering_business_name=%s" +
                "&catering_postcode=%s", CHI, providerName, providerPostcode);
        FoodBox pickedFoodBox = getFoodBoxById(pickedFoodBoxId);
        try {
            String data = new Gson().toJson(pickedFoodBox);
            String response = ClientIO.doPOSTRequest(endpoint + request, data);
            this.currentOrderId = Integer.parseInt(response);
            Order newOrder = new Order(currentOrderId, 0, pickedFoodBox);
            this.allOrders.add(newOrder);
            this.idToOrder.put(currentOrderId, newOrder);
            this.orderedThisWeek = true;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * edit an order based the order number. Checks the precondition that the order does exist.
     * @param orderNumber the order number
     * @return a boolean value represents the success of the editing action
     */
    @Override
    public boolean editOrder(int orderNumber) {
        Order order = getOrderById(orderNumber);
        if (order != null) {
            if (requestOrderStatus(orderNumber) && order.getStatus() < 1) {
                String request = "/editOrder?order_id=" + orderNumber;
                String data = new Gson().toJson(order.getFoodBox());
                try {
                    String response = ClientIO.doPOSTRequest(endpoint + request, data);
                    return Boolean.parseBoolean(response);
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * cancel an order based on the order number. Checks the precondition that the order does exist.
     * @param orderNumber the order number
     * @return a boolean value represents the success of the cancelling action
     */
    @Override
    public boolean cancelOrder(int orderNumber) {
        Order order = getOrderById(orderNumber);
        if (order == null) {
            return false;
        }
        String request = "/cancelOrder?order_id=" + orderNumber;
        try {
            String response = ClientIO.doGETRequest(endpoint + request);
            boolean cancelled = Boolean.parseBoolean(response);
            if (cancelled) {
                order.setStatus(4);
                this.orderedThisWeek = false;
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * request order status based on the order number. Checks the precondition that the order does exist.
     * @param orderNumber the order number
     * @return a boolean value represents the success of the requesting action
     */
    @Override
    public boolean requestOrderStatus(int orderNumber) {
        Order order = getOrderById(orderNumber);
        if (order == null) {
            return false;
        }
        String request = "/requestStatus?order_id=" + orderNumber;
        try {
            String response = ClientIO.doGETRequest(endpoint + request);
            int orderStatus = Integer.parseInt(response);
            if (orderStatus == -1) {
                return false;
            }
            order.setStatus(orderStatus);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * get all the catering companies.
     * @return a collection of all the catering companies in String
     */
    @Override
    public Collection<String> getCateringCompanies() {
        String request = "/getCaterers";
        List<String> allCateringCompanies;

        try {
            String response = ClientIO.doGETRequest(endpoint + request);
            Type stringList = new TypeToken<List<String>>() {} .getType();
            allCateringCompanies = new Gson().fromJson(response, stringList);
        } catch (Exception e) {
            return null;
        }
        return allCateringCompanies;
    }

    /**
     * get the distance between two postcodes.
     * @param postCode1 post code of one location
     * @param postCode2 post code of another location
     * @return the distance between the locations in float
     */
    @Override
    public float getDistance(String postCode1, String postCode2) {
        String request = String.format("/distance?postcode1=%s&postcode2=%s",
                postCode1, postCode2);
        try {
            String response = ClientIO.doGETRequest(endpoint + request);
            return Float.parseFloat(response);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * check if the shielding individual is registered.
     * @return a boolean value represents whether the shielding individual is registered
     */
    @Override
    public boolean isRegistered() {
        return this.registered;
    }

    /**
     * get CHI number of the shielding individual.
     * @return a String represents the CHI of the shielding individual
     */
    @Override
    public String getCHI() {
        return this.CHI;
    }

    /**
     * get the total number of available food boxes.
     * @return an integer represents the total number of available food boxes
     */
    @Override
    public int getFoodBoxNumber() {
        return showFoodBoxes("").size();
    }

    /**
     * get the dietary preference of the food box based on the food box id.
     * Checks the precondition that the food box exists.
     * @param  foodBoxId the food box id as last returned from the server
     * @return a String representing the dietary preference of the food box.
     */
    @Override
    public String getDietaryPreferenceForFoodBox(int foodBoxId) {
        FoodBox foodBox = getFoodBoxById(Integer.toString(foodBoxId));
        assert foodBox != null;
        return foodBox.getDiet();
    }

    /**
     * get the number of items in the food box based on the food box id.
     * Checks the precondition that the food box exists.
     * @param  foodBoxId the food box id as last returned from the server
     * @return an integer representing the total number of items in that food box.
     */
    @Override
    public int getItemsNumberForFoodBox(int foodBoxId) {
        FoodBox foodBox = getFoodBoxById(Integer.toString(foodBoxId));
        assert foodBox != null;
        return foodBox.getContents().size();
    }

    /**
     * get the item ids of all the items in the food box based on the food box id.
     * Checks the precondition that the food box exists.
     * @param foodboxId the food box id to be queried
     * @return a collection of all the item ids of all the items in the food box.
     */
    @Override
    public Collection<Integer> getItemIdsForFoodBox(int foodboxId) {
        FoodBox foodBox = getFoodBoxById(Integer.toString(foodboxId));
        assert foodBox != null;
        List<Integer> itemIds = new ArrayList<>();
        for (Product product: foodBox.getContents()) {
            itemIds.add(product.getId());
        }
        return itemIds;
    }

    /**
     * get the item name for an item in the food box based on food box id and item id.
     * Checks the precondition that the food box exists.
     * @param  itemId the food box id as last returned from the server
     * @param  foodBoxId the food box id as last returned from the server
     * @return a String representing the item name. Return null if the item id
     *         does not match any item.
     */
    @Override
    public String getItemNameForFoodBox(int itemId, int foodBoxId) {
        FoodBox foodBox = getFoodBoxById(Integer.toString(foodBoxId));
        assert foodBox != null;
        for (Product product: foodBox.getContents()) {
            if (product.getId() == itemId) {
                return product.getName();
            }
        }
        return null;
    }

    /**
     * get the quantity for an item in the food box based on food box id and item id.
     * Checks the precondition that the food box exists.
     * @param  itemId the food box id as last returned from the server
     * @param  foodBoxId the food box id as last returned from the server
     * @return an integer representing the quantity of an item in the food box. Returns -1
     *         if the item id match no item in the food box.
     */
    @Override
    public int getItemQuantityForFoodBox(int itemId, int foodBoxId) {
        FoodBox foodBox = getFoodBoxById(Integer.toString(foodBoxId));
        assert foodBox != null;
        for (Product product: foodBox.getContents()) {
            if (product.getId() == itemId) {
                return product.getQuantity();
            }
        }
        return -1;
    }

    /**
     * pick the food box specified by the food box id, set the field pickedFoodBoxId if success.
     * @param  foodBoxId the food box id as last returned from the server
     * @return whether the food box with the specified food box id is picked.
     */
    @Override
    public boolean pickFoodBox(int foodBoxId) {
        Collection<String> foodBoxIds = showFoodBoxes("");
        String idString = Integer.toString(foodBoxId);
        if (foodBoxIds.contains(idString)) {
            this.pickedFoodBoxId = idString;
            return true;
        }
        return false;
    }

    /**
     * change the item quantity in the picked food box based on the item id and the quantity.
     * Checks the precondition that a food box has been picked
     * @param  itemId the food box id as last returned from the server
     * @param  quantity the food box item quantity to be set
     * @return a boolean value represents whether the item quantity is changed.
     */
    @Override
    public boolean changeItemQuantityForPickedFoodBox(int itemId, int quantity) {
        FoodBox foodBox = getFoodBoxById(pickedFoodBoxId);
        assert foodBox != null;
        for (Product product: foodBox.getContents()) {
            if (product.getId() == itemId && product.getQuantity() > quantity) {
                product.setQuantity(quantity);
                return true;
            }
        }
        return false;
    }

    /**
     * get the list of order numbers placed by the shielding individual.
     * @return a collection of the order numbers placed by the shielding individual.
     */
    @Override
    public Collection<Integer> getOrderNumbers() {
        List<Integer> orderNumbers = new ArrayList<>();
        for (Order order: allOrders) {
            orderNumbers.add(order.getId());
        }
        return orderNumbers;
    }

    /**
     * get the status of an order based on the order number. Checks the precondition
     * that the order exists.
     * @param orderNumber the order number
     * @return an integer represents the status of the order
     */
    @Override
    public String getStatusForOrder(int orderNumber) {
        Order order = getOrderById(orderNumber);
        assert order != null;
        if (order.getStatus() == 0) {
            return "placed";
        } else if (order.getStatus() == 1) {
            return "packed";
        } else if (order.getStatus() == 2) {
            return "dispatched";
        } else {
            return "cancelled";
        }
    }

    /**
     * get the list of item ids for the order based on the order number. Checks the precondition
     * that the order exists.
     * @param  orderNumber the order number
     * @return a collection of the item ids for the order
     */
    @Override
    public Collection<Integer> getItemIdsForOrder(int orderNumber) {
        Order order = getOrderById(orderNumber);
        assert order != null;
        List<Integer> itemIds = new ArrayList<>();
        for (Product product: order.getFoodBox().getContents()) {
            itemIds.add(product.getId());
        }
        return itemIds;
    }

    /**
     * get the item name of an order based on the item id and the order number. Checks the precondition
     * that the order exists.
     * @param  itemId the food box id as last returned from the server
     * @param  orderNumber the order number
     * @return a String representing the item name for the item queried. Return null if the
     *         item id match no item in the food box.
     */
    @Override
    public String getItemNameForOrder(int itemId, int orderNumber) {
        Order order = getOrderById(orderNumber);
        assert order != null;
        for (Product product: order.getFoodBox().getContents()) {
            if (product.getId() == itemId) {
                return product.getName();
            }
        }
        return null;
    }

    /**
     * get the item quantity of an item in the order specified by the item id and the order number.
     * Checks the precondition that the food box exists.
     * @param  itemId the food box id as last returned from the server
     * @param  orderNumber the order number
     * @return a boolean representing the item quantity of the order. Return -1 if the item id
     *         match no item in the food box.
     */
    @Override
    public int getItemQuantityForOrder(int itemId, int orderNumber) {
        Order order = getOrderById(orderNumber);
        assert order != null;
        for (Product product: order.getFoodBox().getContents()) {
            if (product.getId() == itemId) {
                return product.getQuantity();
            }
        }
        return -1;
    }

    /**
     * set the item quantity of the order based on the item id, the order number and the quantity.
     * @param  itemId the food box id as last returned from the server
     * @param  orderNumber the order number
     * @param  quantity the food box item quantity to be set
     * @return a boolean representing whether the item quantity for the item is set.
     *         Returns false if the order does not exist or the item id does not match any
     *         item in the food box.
     */
    @Override
    public boolean setItemQuantityForOrder(int itemId, int orderNumber, int quantity) {
        Order order = getOrderById(orderNumber);
        if (order == null) {
            return false;
        }
        for (Product product: order.getFoodBox().getContents()) {
            if (product.getId() == itemId) {
                product.setQuantity(quantity);
                return true;
            }
        }
        return false;
    }

    /**
     * get the closest catering company to the current shielding individual.
     * @return a String representing the closest catering company. Returns null
     *         if such catering company does not exist.
     */
    @Override
    public String getClosestCateringCompany() {
        Collection<String> allCateringCompanies = getCateringCompanies();
        double curMin = Double.MAX_VALUE;
        String curMinCateringCompany = null;
        for (String cateringCompany: allCateringCompanies) {
            String cateringCompanyPostcode = cateringCompany.split(",")[2];
            float distance = getDistance(cateringCompanyPostcode, postcode);
            if (distance == -1) {
                continue;
            }
            if (distance < curMin) {
                curMin = distance;
                curMinCateringCompany = cateringCompany;
            }
        }
        return curMinCateringCompany;
    }

    /**
     * method for checking whether the CHI is valid. A valid CHI should have string length 10 of
     * all digits, with the first 6 in the format DD/MM/YY.
     * @param CHI CHI number of the shielding individual
     * @return a boolean value represents whether the CHI is valid or not.
     */
    private boolean validCHI(String CHI) {
        for (char c: CHI.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        try {
            // get info from CHI
            int year = Integer.parseInt(CHI.substring(4,6));
            if (year <= 21) {
                year += 2000;
            } else {
                year += 1900;
            }
            int month = Integer.parseInt(CHI.substring(2,4));
            int day = Integer.parseInt(CHI.substring(0,2));
            // Check the birth date
            Calendar birth = Calendar.getInstance();
            birth.setLenient(false);
            birth.set(year, month - 1, day);
            birth.getTime();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * get the food box by its food box id.
     * @param id the food box id
     * @return the corresponding food box object. Returns null if the food box does not exist.
     */
    private FoodBox getFoodBoxById(String id) {
        for (FoodBox foodBox: allFoodBox) {
            if (foodBox.getId().equals(id)) {
                return foodBox;
            }
        }
        return null;
    }

    /**
     * get the order by its order id.
     * @param id the order id
     * @return the corresponding order object. Returns null if the order does not exist.
     */
    private Order getOrderById(int id) {
        for (Order order: allOrders) {
            if (order.getId() == id) {
                return order;
            }
        }
        return null;
    }

    /**
     * getter method for the field currentOrderId.
     * @return an integer representing the value of the field currentOrderId
     */
    public int getCurrentOrderId() {
        return currentOrderId;
    }

    /**
     * setter method for the field orderedThisWeek.
     */
    public void setOrderedThisWeek(boolean orderedThisWeek) {
        this.orderedThisWeek = orderedThisWeek;
    }
}
