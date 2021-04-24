/**
 *
 */

package shield;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;

import java.util.*;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */

public class ShieldingIndividualClientImpTest {
    private final static String clientPropsFilename = "client.cfg";

    private Properties clientProps;
    private ShieldingIndividualClientImp client;


    private Properties loadProperties(String propsFilename) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Properties props = new Properties();

        try {
            InputStream propsStream = loader.getResourceAsStream(propsFilename);
            props.load(propsStream);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return props;
    }

    @BeforeEach
    public void setup() {
        clientProps = loadProperties(clientPropsFilename);
        client = new ShieldingIndividualClientImp(clientProps.getProperty("endpoint"));
    }


    @Test
    public void testShieldingIndividualNewRegistration() {
        // test invalid registration
        String chi = "c8s9g0o81324";
        assertFalse(client.registerShieldingIndividual(chi));
        chi = "validCHI";
        assertFalse(client.registerShieldingIndividual(chi));

        // valid registration
        chi = generateRandomValidCHI();

        assertTrue(client.registerShieldingIndividual(chi));
        assertTrue(client.isRegistered());
        assertEquals(client.getCHI(), chi);

        // register again
        assertTrue(client.registerShieldingIndividual(chi));
    }


    @Test
    public void testShowFoodBoxes(){
        // invalid dietary preference
        assertEquals(client.showFoodBoxes("invalid").size(), 0);
        // valid dietary preference
        assertNotNull(client.showFoodBoxes(""));
        assertNotNull(client.showFoodBoxes("none"));
        assertNotNull(client.showFoodBoxes("vegan"));
        assertNotNull(client.showFoodBoxes("pollotarian"));
    }

    @Test
    public void testPlaceOrder() {
        String providerName = generateRandomName();
        String providerPostcode = generateRandomValidPostcode();
        String endpoint = clientProps.getProperty("endpoint");
        String request = String.format("/registerCateringCompany?business_name=%s&postcode=%s", providerName, providerPostcode);
        try {
            String response = ClientIO.doGETRequest(endpoint + request);
        } catch (Exception e) {
            fail();
        }
        // place before registers
        assertFalse(client.placeOrder());

        // registers shielding individual
        String chi = generateRandomValidCHI();
        request = "/registerShieldingIndividual?CHI="+chi;

        // registerShieldingIndividual without use public method expect getter and setter
        try {
            endpoint = clientProps.getProperty("endpoint");
            String response = ClientIO.doGETRequest(endpoint + request);
            if (!response.equals("already registered")) {
                String[] personalInfo = new Gson().fromJson(response, String[].class);
                String postcode = personalInfo[0].replace(' ', '_');
                client.setCHI(chi);
                client.setRegistered(true);
                client.setPostcode(postcode);
                client.setOrderedThisWeek(false);

                // place before picks food box
                assertFalse(client.placeOrder());

                // fake pick food box
                client.setPickedFoodBoxId("1");

                // register with all preconditions met
                assertTrue(client.placeOrder());

                // place after placed this week
                assertFalse(client.placeOrder());
            }
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testEditOrder(){
        //set precondition for registerShieldingIndividual
        String chi = generateRandomValidCHI();
        String request = "/registerShieldingIndividual?CHI="+chi;

        // registerShieldingIndividual without use public method expect getter and setter
        try {
            String endpoint = clientProps.getProperty("endpoint");
            String response = ClientIO.doGETRequest(endpoint + request);
            if (!response.equals("already registered")) {
                String[] personalInfo = new Gson().fromJson(response, String[].class);
                String postcode = personalInfo[0].replace(' ', '_');
                client.setCHI(chi);
                client.setRegistered(true);
                client.setPostcode(postcode);

                //set precondition for placeOrder
                client.setPickedFoodBoxId("1");

                //placeOrder without use public method expect getter and setter
                String providerName = generateRandomName();
                String providerPostcode = generateRandomValidPostcode();
                request = String.format("/registerCateringCompany?business_name=%s&postcode=%s", providerName, providerPostcode);
                try {
                    response = ClientIO.doGETRequest(endpoint + request);
                    request = String.format("/placeOrder?individual_id=%s&catering_business_name=%s" +
                            "&catering_postcode=%s", client.getCHI(), providerName, providerPostcode);
                    FoodBox pickedFoodBox = generateDefualtFoodBoxForTest();
                    try {
                        String data = new Gson().toJson(pickedFoodBox);
                        response = ClientIO.doPOSTRequest(endpoint + request, data);
                        client.setCurrentOrderId(Integer.parseInt(response));
                        Order newOrder = new Order(client.getCurrentOrderId(), 0, pickedFoodBox);
                        List<Order> allOrders = new ArrayList<>();
                        allOrders.add(newOrder);
                        client.setAllOrders(allOrders);
                        client.setOrderedThisWeek(true);

                        // edit order with increased quantity
                        newOrder.getFoodBox().getContents().get(0).setQuantity(10);
                        assertFalse(client.editOrder(client.getCurrentOrderId()));

                        // edit order with correctly reduce quantity
                        newOrder.getFoodBox().getContents().get(0).setQuantity(0);
                        assertTrue(client.editOrder(client.getCurrentOrderId()));
                    } catch (Exception e) {
                        fail();
                    }
                } catch (Exception e) {
                    fail();
                }
            }
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testCancelOrder() {
        // invalid order id
        assertFalse(client.cancelOrder(-2));

        //set precondition for registerShieldingIndividual
        String chi = generateRandomValidCHI();
        String request = "/registerShieldingIndividual?CHI="+chi;

        //registerShieldingIndividual without use public method expect getter and setter
        try{
            String endpoint = clientProps.getProperty("endpoint");
            String response = ClientIO.doGETRequest(endpoint + request);
            if (!response.equals("already registered")) {
                String[] personalInfo = new Gson().fromJson(response, String[].class);
                String postcode = personalInfo[0].replace(' ', '_');
                client.setCHI(chi);
                client.setRegistered(true);
                client.setPostcode(postcode);

                //set precondition for placeOrder
                client.setPickedFoodBoxId("1");

                //placeOrder without use public method expect getter and setter
                String providerName = generateRandomName();
                String providerPostcode = generateRandomValidPostcode();
                request = String.format("/registerCateringCompany?business_name=%s&postcode=%s", providerName, providerPostcode);
                try {
                    response = ClientIO.doGETRequest(endpoint + request);
                    request = String.format("/placeOrder?individual_id=%s&catering_business_name=%s" +
                            "&catering_postcode=%s", client.getCHI(), providerName, providerPostcode);
                    FoodBox pickedFoodBox = generateDefualtFoodBoxForTest();
                    try {
                        String data = new Gson().toJson(pickedFoodBox);
                        response = ClientIO.doPOSTRequest(endpoint + request, data);
                        client.setCurrentOrderId(Integer.parseInt(response));
                        Order newOrder = new Order(client.getCurrentOrderId(), 0, pickedFoodBox);
                        List<Order> allOrders = new ArrayList<>();
                        allOrders.add(newOrder);
                        client.setAllOrders(allOrders);
                        client.setOrderedThisWeek(true);

                        // set status to be dispatched
                        newOrder.setStatus(2);
                        assertFalse(client.cancelOrder(client.getCurrentOrderId()));
                        // set status to be delivered
                        newOrder.setStatus(3);
                        assertFalse(client.cancelOrder(client.getCurrentOrderId()));
                        // set status to be cancelled
                        newOrder.setStatus(4);
                        assertFalse(client.cancelOrder(client.getCurrentOrderId()));
                        // set status to be packed
                        newOrder.setStatus(1);
                        assertTrue(client.cancelOrder(client.getCurrentOrderId()));
                    } catch (Exception e) {
                        fail();
                    }
                } catch (Exception e) {
                    fail();
                }
            }
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testRequestOrderStatus(){
        // invalid order id
        assertFalse(client.requestOrderStatus(-2));

        //set precondition for registerShieldingIndividual
        String chi = generateRandomValidCHI();
        String request = "/registerShieldingIndividual?CHI="+chi;

        //registerShieldingIndividual without use public method expect getter and setter
        try{
            String endpoint = clientProps.getProperty("endpoint");
            String response = ClientIO.doGETRequest(endpoint + request);
            if (!response.equals("already registered")) {
                String[] personalInfo = new Gson().fromJson(response, String[].class);
                String postcode = personalInfo[0].replace(' ', '_');
                client.setCHI(chi);
                client.setRegistered(true);
                client.setPostcode(postcode);

                //set precondition for placeOrder
                client.setPickedFoodBoxId("1");

                //placeOrder without use public method expect getter and setter
                String providerName = generateRandomName();
                String providerPostcode = generateRandomValidPostcode();
                request = String.format("/registerCateringCompany?business_name=%s&postcode=%s", providerName, providerPostcode);
                try {
                    response = ClientIO.doGETRequest(endpoint + request);
                    request = String.format("/placeOrder?individual_id=%s&catering_business_name=%s" +
                            "&catering_postcode=%s", client.getCHI(), providerName, providerPostcode);
                    FoodBox pickedFoodBox = generateDefualtFoodBoxForTest();
                    try {
                        String data = new Gson().toJson(pickedFoodBox);
                        response = ClientIO.doPOSTRequest(endpoint + request, data);
                        client.setCurrentOrderId(Integer.parseInt(response));
                        Order newOrder = new Order(client.getCurrentOrderId(), 0, pickedFoodBox);
                        List<Order> allOrders = new ArrayList<>();
                        allOrders.add(newOrder);
                        client.setAllOrders(allOrders);
                        client.setOrderedThisWeek(true);

                        // request status
                        assertTrue(client.requestOrderStatus(client.getCurrentOrderId()));
                        assertEquals(newOrder.getStatus(), 0);
                    } catch (Exception e) {
                        fail();
                    }
                } catch (Exception e) {
                    fail();
                }
            }
        } catch (Exception e) {
            fail();
        }
    }



    @Test
    public void testGetCateringCompanies(){
        String providerName = generateRandomName();
        String providerPostcode = generateRandomValidPostcode();
        String request = String.format("/registerCateringCompany?business_name=%s&postcode=%s", providerName, providerPostcode);
        try {
            String endpoint = clientProps.getProperty("endpoint");
            String response = ClientIO.doGETRequest(endpoint + request);
            boolean found = false;
            for (String provider: client.getCateringCompanies()) {
                String[] info = provider.split(",");
                if (providerName.equals(info[1]) && providerPostcode.equals(info[2])) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testGetDistance(){
        assertEquals(-1, client.getDistance("invalid1", "invalid2"));
        assertTrue(client.getDistance(generateRandomValidPostcode(),generateRandomValidPostcode()) != -1);
    }

    @Test
    public void testGetDietaryPreferenceForFoodBox(){
        assertNull(client.getDietaryPreferenceForFoodBox(-1));
        assertEquals(client.getDietaryPreferenceForFoodBox(1), "none");
        assertEquals(client.getDietaryPreferenceForFoodBox(2), "pollotarian");
        assertEquals(client.getDietaryPreferenceForFoodBox(3), "none");
        assertEquals(client.getDietaryPreferenceForFoodBox(4), "none");
        assertEquals(client.getDietaryPreferenceForFoodBox(5), "vegan");
    }

    @Test
    public void testGetItemsNumberForFoodBox(){
        assertEquals(client.getItemsNumberForFoodBox(-1), -1);
        assertEquals(client.getItemsNumberForFoodBox(1), 3);
        assertEquals(client.getItemsNumberForFoodBox(2), 3);
        assertEquals(client.getItemsNumberForFoodBox(3), 3);
        assertEquals(client.getItemsNumberForFoodBox(4), 4);
        assertEquals(client.getItemsNumberForFoodBox(5), 3);
    }

    @Test
    public void testGetItemIdsForFoodBox(){
        assertNull(client.getItemIdsForFoodBox(-1));
        List<Integer> itemIds = new ArrayList<>(client.getItemIdsForFoodBox(1));
        assertEquals((int) itemIds.get(0), 1);
        assertEquals((int) itemIds.get(1), 2);
        assertEquals((int) itemIds.get(2), 6);
    }

    @Test
    public void testGetItemNameForFoodBox(){
        assertNull(client.getItemNameForFoodBox(-1, -1));
        assertNull(client.getItemNameForFoodBox(-1, 1));
        assertNull(client.getItemNameForFoodBox(1, -1));
        assertEquals(client.getItemNameForFoodBox(1,1), "cucumbers");
        assertEquals(client.getItemNameForFoodBox(2,1), "tomatoes");
        assertEquals(client.getItemNameForFoodBox(6,1), "pork");
    }

    @Test
    public void testGetItemQuantityForFoodBox(){
        assertEquals(client.getItemQuantityForFoodBox(-1, -1), -1);
        assertEquals(client.getItemQuantityForFoodBox(-1, 1), -1);
        assertEquals(client.getItemQuantityForFoodBox(1, -1), -1);
        // valid
        assertEquals(client.getItemQuantityForFoodBox(1, 1), 1);
        assertEquals(client.getItemQuantityForFoodBox(2, 1), 2);
        assertEquals(client.getItemQuantityForFoodBox(6, 1), 1);
    }

    @Test
    public void testPickFoodBox(){
        assertFalse(client.pickFoodBox(-1));
        assertTrue(client.pickFoodBox(1));
        assertEquals(client.getPickedFoodBoxId(), "1");
    }

    @Test
    public void testChangeItemQuantityForPickedFoodBox(){
        // invalid item id
        assertFalse(client.changeItemQuantityForPickedFoodBox(-1, 1));
        // trying to increase quantity
        client.setPickedFoodBoxId("1");
        assertFalse(client.changeItemQuantityForPickedFoodBox(1,10));
        // decrease quantity
        assertTrue(client.changeItemQuantityForPickedFoodBox(2,1));
    }

    @Test
    public void testGetOrderNumbers() {
        //set precondition for registerShieldingIndividual
        String chi = generateRandomValidCHI();
        String request = "/registerShieldingIndividual?CHI="+chi;

        //registerShieldingIndividual without use public method expect getter and setter
        try{
            String endpoint = clientProps.getProperty("endpoint");
            String response = ClientIO.doGETRequest(endpoint + request);
            if (!response.equals("already registered")) {
                String[] personalInfo = new Gson().fromJson(response, String[].class);
                String postcode = personalInfo[0].replace(' ', '_');
                client.setCHI(chi);
                client.setRegistered(true);
                client.setPostcode(postcode);

                //set precondition for placeOrder
                client.setPickedFoodBoxId("1");

                //placeOrder without use public method expect getter and setter
                String providerName = generateRandomName();
                String providerPostcode = generateRandomValidPostcode();
                request = String.format("/registerCateringCompany?business_name=%s&postcode=%s", providerName, providerPostcode);
                try {
                    response = ClientIO.doGETRequest(endpoint + request);
                    request = String.format("/placeOrder?individual_id=%s&catering_business_name=%s" +
                            "&catering_postcode=%s", client.getCHI(), providerName, providerPostcode);
                    FoodBox pickedFoodBox = generateDefualtFoodBoxForTest();
                    try {
                        String data = new Gson().toJson(pickedFoodBox);
                        response = ClientIO.doPOSTRequest(endpoint + request, data);
                        client.setCurrentOrderId(Integer.parseInt(response));
                        Order newOrder = new Order(client.getCurrentOrderId(), 0, pickedFoodBox);
                        List<Order> allOrders = new ArrayList<>();
                        allOrders.add(newOrder);
                        client.setAllOrders(allOrders);
                        client.setOrderedThisWeek(true);

                        // should contain the currently placed order
                        assertTrue(client.getOrderNumbers().contains(client.getCurrentOrderId()));
                    } catch (Exception e) {
                        fail();
                    }
                } catch (Exception e) {
                    fail();
                }
            }
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testGetStatusForOrder(){
        // invalid order id
        assertNull(client.getStatusForOrder(-1));

        // set precondition for registerShieldingIndividual
        String chi = generateRandomValidCHI();
        String request = "/registerShieldingIndividual?CHI="+chi;

        // registerShieldingIndividual without use public method expect getter and setter
        try{
            String endpoint = clientProps.getProperty("endpoint");
            String response = ClientIO.doGETRequest(endpoint + request);
            if (!response.equals("already registered")) {
                String[] personalInfo = new Gson().fromJson(response, String[].class);
                String postcode = personalInfo[0].replace(' ', '_');
                client.setCHI(chi);
                client.setRegistered(true);
                client.setPostcode(postcode);

                // set precondition for placeOrder
                client.setPickedFoodBoxId("1");

                // placeOrder without use public method expect getter and setter
                String providerName = generateRandomName();
                String providerPostcode = generateRandomValidPostcode();
                request = String.format("/registerCateringCompany?business_name=%s&postcode=%s", providerName, providerPostcode);
                try {
                    response = ClientIO.doGETRequest(endpoint + request);
                    request = String.format("/placeOrder?individual_id=%s&catering_business_name=%s" +
                            "&catering_postcode=%s", client.getCHI(), providerName, providerPostcode);
                    FoodBox pickedFoodBox = generateDefualtFoodBoxForTest();
                    try {
                        String data = new Gson().toJson(pickedFoodBox);
                        response = ClientIO.doPOSTRequest(endpoint + request, data);
                        client.setCurrentOrderId(Integer.parseInt(response));
                        Order newOrder = new Order(client.getCurrentOrderId(), 0, pickedFoodBox);
                        List<Order> allOrders = new ArrayList<>();
                        allOrders.add(newOrder);
                        client.setAllOrders(allOrders);
                        client.setOrderedThisWeek(true);

                        // status should be placed
                        assertEquals(client.getStatusForOrder(client.getCurrentOrderId()), "placed");
                    } catch (Exception e) {
                        fail();
                    }
                } catch (Exception e) {
                    fail();
                }
            }
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testGetItemIdsForOrder(){
        // invalid order id
        assertNull(client.getItemIdsForOrder(-1));

        // set precondition for registerShieldingIndividual
        String chi = generateRandomValidCHI();
        String request = "/registerShieldingIndividual?CHI="+chi;

        // registerShieldingIndividual without use public method expect getter and setter
        try{
            String endpoint = clientProps.getProperty("endpoint");
            String response = ClientIO.doGETRequest(endpoint + request);
            if (!response.equals("already registered")) {
                String[] personalInfo = new Gson().fromJson(response, String[].class);
                String postcode = personalInfo[0].replace(' ', '_');
                client.setCHI(chi);
                client.setRegistered(true);
                client.setPostcode(postcode);

                // set precondition for placeOrder
                client.setPickedFoodBoxId("1");

                // placeOrder without use public method expect getter and setter
                String providerName = generateRandomName();
                String providerPostcode = generateRandomValidPostcode();
                request = String.format("/registerCateringCompany?business_name=%s&postcode=%s", providerName, providerPostcode);
                try {
                    response = ClientIO.doGETRequest(endpoint + request);
                    request = String.format("/placeOrder?individual_id=%s&catering_business_name=%s" +
                            "&catering_postcode=%s", client.getCHI(), providerName, providerPostcode);
                    FoodBox pickedFoodBox = generateDefualtFoodBoxForTest();
                    try {
                        String data = new Gson().toJson(pickedFoodBox);
                        response = ClientIO.doPOSTRequest(endpoint + request, data);
                        client.setCurrentOrderId(Integer.parseInt(response));
                        Order newOrder = new Order(client.getCurrentOrderId(), 0, pickedFoodBox);
                        List<Order> allOrders = new ArrayList<>();
                        allOrders.add(newOrder);
                        client.setAllOrders(allOrders);
                        client.setOrderedThisWeek(true);

                        // should contain the currently placed order
                        List<Integer> itemIds = new ArrayList<>(client.getItemIdsForOrder(client.getCurrentOrderId()));
                        assertEquals((int) itemIds.get(0), 1);
                        assertEquals((int) itemIds.get(1), 2);
                        assertEquals((int) itemIds.get(2), 6);
                    } catch (Exception e) {
                        fail();
                    }
                } catch (Exception e) {
                    fail();
                }
            }
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testGetItemNameForOrder(){
        // invalid order id
        assertNull(client.getItemNameForOrder(1, -1));

        // set precondition for registerShieldingIndividual
        String chi = generateRandomValidCHI();
        String request = "/registerShieldingIndividual?CHI="+chi;

        // registerShieldingIndividual without use public method expect getter and setter
        try{
            String endpoint = clientProps.getProperty("endpoint");
            String response = ClientIO.doGETRequest(endpoint + request);
            if (!response.equals("already registered")) {
                String[] personalInfo = new Gson().fromJson(response, String[].class);
                String postcode = personalInfo[0].replace(' ', '_');
                client.setCHI(chi);
                client.setRegistered(true);
                client.setPostcode(postcode);

                // set precondition for placeOrder
                client.setPickedFoodBoxId("1");

                // placeOrder without use public method expect getter and setter
                String providerName = generateRandomName();
                String providerPostcode = generateRandomValidPostcode();
                request = String.format("/registerCateringCompany?business_name=%s&postcode=%s", providerName, providerPostcode);
                try {
                    response = ClientIO.doGETRequest(endpoint + request);
                    request = String.format("/placeOrder?individual_id=%s&catering_business_name=%s" +
                            "&catering_postcode=%s", client.getCHI(), providerName, providerPostcode);
                    FoodBox pickedFoodBox = generateDefualtFoodBoxForTest();
                    try {
                        String data = new Gson().toJson(pickedFoodBox);
                        response = ClientIO.doPOSTRequest(endpoint + request, data);
                        client.setCurrentOrderId(Integer.parseInt(response));
                        Order newOrder = new Order(client.getCurrentOrderId(), 0, pickedFoodBox);
                        List<Order> allOrders = new ArrayList<>();
                        allOrders.add(newOrder);
                        client.setAllOrders(allOrders);
                        client.setOrderedThisWeek(true);

                        // should contain the currently placed order
                        assertEquals(client.getItemNameForOrder(1, client.getCurrentOrderId()), "cucumbers");
                        assertEquals(client.getItemNameForOrder(2, client.getCurrentOrderId()), "tomatoes");
                        assertEquals(client.getItemNameForOrder(6, client.getCurrentOrderId()), "pork");
                    } catch (Exception e) {
                        fail();
                    }
                } catch (Exception e) {
                    fail();
                }
            }
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testGetItemQuantityForOrder(){
        // invalid order id
        assertEquals(client.getItemQuantityForOrder(1, -1), -1);

        // set precondition for registerShieldingIndividual
        String chi = generateRandomValidCHI();
        String request = "/registerShieldingIndividual?CHI="+chi;

        // registerShieldingIndividual without use public method expect getter and setter
        try {
            String endpoint = clientProps.getProperty("endpoint");
            String response = ClientIO.doGETRequest(endpoint + request);
            if (!response.equals("already registered")) {
                String[] personalInfo = new Gson().fromJson(response, String[].class);
                String postcode = personalInfo[0].replace(' ', '_');
                client.setCHI(chi);
                client.setRegistered(true);
                client.setPostcode(postcode);

                // set precondition for placeOrder
                client.setPickedFoodBoxId("1");

                // placeOrder without use public method expect getter and setter
                String providerName = generateRandomName();
                String providerPostcode = generateRandomValidPostcode();
                request = String.format("/registerCateringCompany?business_name=%s&postcode=%s", providerName, providerPostcode);
                try {
                    response = ClientIO.doGETRequest(endpoint + request);
                    request = String.format("/placeOrder?individual_id=%s&catering_business_name=%s" +
                            "&catering_postcode=%s", client.getCHI(), providerName, providerPostcode);
                    FoodBox pickedFoodBox = generateDefualtFoodBoxForTest();
                    try {
                        String data = new Gson().toJson(pickedFoodBox);
                        response = ClientIO.doPOSTRequest(endpoint + request, data);
                        client.setCurrentOrderId(Integer.parseInt(response));
                        Order newOrder = new Order(client.getCurrentOrderId(), 0, pickedFoodBox);
                        List<Order> allOrders = new ArrayList<>();
                        allOrders.add(newOrder);
                        client.setAllOrders(allOrders);
                        client.setOrderedThisWeek(true);

                        // should contain the currently placed order
                        assertEquals(client.getItemQuantityForOrder(1, client.getCurrentOrderId()), 1);
                        assertEquals(client.getItemQuantityForOrder(2, client.getCurrentOrderId()), 2);
                        assertEquals(client.getItemQuantityForOrder(6, client.getCurrentOrderId()), 1);
                    } catch (Exception e) {
                        fail();
                    }
                } catch (Exception e) {
                    fail();
                }
            }
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testSetItemQuantityForOrder(){
        // invalid order id
        assertFalse(client.setItemQuantityForOrder(1, -1, 1));

        // set precondition for registerShieldingIndividual
        String chi = generateRandomValidCHI();
        String request = "/registerShieldingIndividual?CHI="+chi;

        // registerShieldingIndividual without use public method expect getter and setter
        try {
            String endpoint = clientProps.getProperty("endpoint");
            String response = ClientIO.doGETRequest(endpoint + request);
            if (!response.equals("already registered")) {
                String[] personalInfo = new Gson().fromJson(response, String[].class);
                String postcode = personalInfo[0].replace(' ', '_');
                client.setCHI(chi);
                client.setRegistered(true);
                client.setPostcode(postcode);

                // set precondition for placeOrder
                client.setPickedFoodBoxId("1");

                // placeOrder without use public method expect getter and setter
                String providerName = generateRandomName();
                String providerPostcode = generateRandomValidPostcode();
                request = String.format("/registerCateringCompany?business_name=%s&postcode=%s", providerName, providerPostcode);
                try {
                    response = ClientIO.doGETRequest(endpoint + request);
                    request = String.format("/placeOrder?individual_id=%s&catering_business_name=%s" +
                            "&catering_postcode=%s", client.getCHI(), providerName, providerPostcode);
                    FoodBox pickedFoodBox = generateDefualtFoodBoxForTest();
                    try {
                        String data = new Gson().toJson(pickedFoodBox);
                        response = ClientIO.doPOSTRequest(endpoint + request, data);
                        client.setCurrentOrderId(Integer.parseInt(response));
                        Order newOrder = new Order(client.getCurrentOrderId(), 0, pickedFoodBox);
                        List<Order> allOrders = new ArrayList<>();
                        allOrders.add(newOrder);
                        client.setAllOrders(allOrders);
                        client.setOrderedThisWeek(true);

                        // set the quantity
                        assertTrue(client.setItemQuantityForOrder(2, client.getCurrentOrderId(), 1));
                    } catch (Exception e) {
                        fail();
                    }
                } catch (Exception e) {
                    fail();
                }
            }
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testGetClosestCateringCompany(){
        String providerName = generateRandomName();
        String providerPostcode = generateRandomValidPostcode();
        String request = String.format("/registerCateringCompany?business_name=%s&postcode=%s", providerName, providerPostcode);
        try {
            String endpoint = clientProps.getProperty("endpoint");
            String response = ClientIO.doGETRequest(endpoint + request);
            client.setPostcode(providerPostcode);
            String[] closestCC = client.getClosestCateringCompany().split(",");
            assertEquals(closestCC[1], providerName);
            assertEquals(closestCC[2], providerPostcode);
        } catch (Exception e) {
            fail();
        }
    }







    // several private method for generate random date for test

    private FoodBox generateDefualtFoodBoxForTest(){
        List<Product> contents = new ArrayList<>();

        Product cucumbers = new Product(1,"cucumbers",1);
        Product tomatoes = new Product(2,"tomatoes",2);
        Product pork = new Product(6,"pork",1);

        contents.add(cucumbers);
        contents.add(tomatoes);
        contents.add(pork);
        FoodBox box = new FoodBox(contents,"catering","none","1","box a");
        return box;
    }

    private String generateRandomValidCHI() {
        GregorianCalendar calendar = new GregorianCalendar();

        int year = randInterval(1922, 2021);
        calendar.set(Calendar.YEAR, year);
        int day = randInterval(1, calendar.getActualMaximum(Calendar.DAY_OF_YEAR));
        calendar.set(Calendar.DAY_OF_YEAR, day);
        StringBuilder chi = new StringBuilder();
        // Construct the chi string
        String dayString = Integer.toString(calendar.get(Calendar.DAY_OF_MONTH));
        if (dayString.length() == 1) {
            chi.append(0);
        }
        chi.append(dayString);
        String monthString = Integer.toString(calendar.get(Calendar.MONTH) + 1);
        if (monthString.length() == 1) {
            chi.append(0);
        }
        chi.append(monthString);
        String yearString;
        if (calendar.get(Calendar.YEAR) < 2000) {
            yearString = Integer.toString(calendar.get(Calendar.YEAR) - 1900);
        } else {
            yearString = Integer.toString(calendar.get(Calendar.YEAR) - 2000);
        }
        if (yearString.length() == 1) {
            chi.append(0);
        }
        chi.append(yearString);
        for (int i = 0; i < 4; i++) {
            chi.append(randInterval(0,9));
        }
        return chi.toString();
    }

    private String generateRandomValidPostcode(){
        int num1 = randInterval(1,17);
        int num2 = randInterval(1,9);
        char c1 = (char) randInterval(65, 90);
        char c2 = (char) randInterval(65, 90);
        return String.format("EH%d_%d%c%c", num1, num2, c1, c2);
    }

    private String generateRandomName(){
        StringBuilder name = new StringBuilder();
        // length set to min 4 max 10
        int length = randInterval(4,10);
        for (int i = 0; i < length; i++) {
            name.append((char) randInterval(97,122));
        }
        return name.toString();
    }

    private int randInterval(int start, int end){
        Random rand = new Random();
        return rand.nextInt(end - start + 1) + start;
    }
}
