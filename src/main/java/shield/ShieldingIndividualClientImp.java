/**
 * To implement
 */

package shield;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;

public class ShieldingIndividualClientImp implements ShieldingIndividualClient {

    /** Fields **/
    private String endpoint;
    private boolean registered;
    private String CHI;
    private String postcode;
    private boolean orderedThisWeek;
    private String pickedFoodBoxId;
    private List<FoodBox> allFoodBox;
    private Map<String, FoodBox> idToFoodBox;
    private int currentOrderId;
    private Map<Integer, Order> idToOrder;
    private List<Order> allOrders;

    public ShieldingIndividualClientImp(String endpoint) {
        this.endpoint = endpoint;
        this.registered = false;
        this.idToFoodBox = new HashMap<>();
        this.idToOrder = new HashMap<>();
        for (String id: showFoodBoxes("")) {
            idToFoodBox.put(id, getFoodBoxById(id));
        }
    }

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

    // **UPDATE2** REMOVED PARAMETER
    @Override
    public boolean placeOrder() {
        assert isRegistered();
        assert !orderedThisWeek;
        assert pickedFoodBoxId != null;
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
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

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

    // **UPDATE**
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

    // **UPDATE**
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

    @Override
    public boolean isRegistered() {
        return this.registered;
    }

    @Override
    public String getCHI() {
        return this.CHI;
    }

    @Override
    public int getFoodBoxNumber() {
        return showFoodBoxes("").size();
    }

    @Override
    public String getDietaryPreferenceForFoodBox(int foodBoxId) {
        FoodBox foodBox = getFoodBoxById(Integer.toString(foodBoxId));
        assert foodBox != null;
        return foodBox.getDiet();
    }

    @Override
    public int getItemsNumberForFoodBox(int foodBoxId) {
        FoodBox foodBox = getFoodBoxById(Integer.toString(foodBoxId));
        assert foodBox != null;
        return foodBox.getContents().size();
    }

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

    @Override
    public boolean changeItemQuantityForPickedFoodBox(int itemId, int quantity) {
        FoodBox foodBox = getFoodBoxById(pickedFoodBoxId);
        assert foodBox != null;
        for (Product product: foodBox.getContents()) {
            if (product.getId() == itemId) {
                product.setQuantity(quantity);
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<Integer> getOrderNumbers() {
        List<Integer> orderNumbers = new ArrayList<>();
        for (Order order: allOrders) {
            orderNumbers.add(order.getId());
        }
        return orderNumbers;
    }

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

    // **UPDATE2** REMOVED METHOD getDeliveryTimeForOrder

    // **UPDATE**
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

    private boolean validCHI(String CHI) {
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

    private FoodBox getFoodBoxById(String id) {
        for (FoodBox foodBox: allFoodBox) {
            if (foodBox.getId().equals(id)) {
                return foodBox;
            }
        }
        return null;
    }

    private Order getOrderById(int id) {
        for (Order order: allOrders) {
            if (order.getId() == id) {
                return order;
            }
        }
        return null;
    }
}
