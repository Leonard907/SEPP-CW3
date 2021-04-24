/**
 *
 */

package shield;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;

import java.util.*;
import java.time.LocalDateTime;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 */

public class SupermarketClientImpTest {
    private final static String clientPropsFilename = "client.cfg";

    private Properties clientProps;
    private ShieldingIndividualClientImp client;
    private SupermarketClientImp clientSupermarket;

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
        clientSupermarket = new SupermarketClientImp(clientProps.getProperty("endpoint"));
    }


    @Test
    public void testSupermarketNewRegistration() {
        String name = generateRandomName();
        // invalid registration
        assertFalse(clientSupermarket.registerSupermarket(name, "postcode"));

        // normal registration
        String postCode = generateRandomValidPostcode();

        assertTrue(clientSupermarket.registerSupermarket(name, postCode));
        assertTrue(clientSupermarket.isRegistered());
    }

    @Test
    public void testRecordOrderAndUpdateOrderStatus(){
        //set precondition for registerShieldingIndividual
        String chi = generateRandomValidCHI();

        // not registered
        assertFalse(clientSupermarket.updateOrderStatus(1, "packed"));
        // wrong number and status
        assertFalse(clientSupermarket.updateOrderStatus(-1, "packed"));
        assertFalse(clientSupermarket.updateOrderStatus(1, "lost"));
        // invalid record CHI and order number
        assertFalse(clientSupermarket.recordSupermarketOrder(chi, -1));
        assertFalse(clientSupermarket.recordSupermarketOrder("CHI", 1));

        // registerShieldingIndividual without use public method expect getter and setter
        String request = "/registerShieldingIndividual?CHI="+chi;
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
                request = String.format("/registerSupermarket?business_name=%s&postcode=%s", providerName, providerPostcode);
                try {
                    ClientIO.doGETRequest(endpoint + request);
                    clientSupermarket.setRegistered(true);
                } catch (Exception e) {
                    fail();
                }
                request = String.format("/registerCateringCompany?business_name=%s&postcode=%s", providerName, providerPostcode);
                try {
                    ClientIO.doGETRequest(endpoint + request);
                } catch (Exception e) {
                    fail();
                }
                try {
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

                        request = String.format(
                                "/recordSupermarketOrder?individual_id=%s" +
                                        "&order_number=%s&supermarket_business_name=%s" +
                                        "&supermarket_postcode=%s", chi, client.getCurrentOrderId(),
                                providerName, providerPostcode);
                        try {
                            response = ClientIO.doGETRequest(endpoint + request);
                            assertTrue(Boolean.parseBoolean(response));

                            // update order
                            assertTrue(clientSupermarket.updateOrderStatus(client.getCurrentOrderId(), "packed"));
                        } catch (Exception e) {
                            fail();
                        }
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
