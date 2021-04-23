package shield;

import org.junit.jupiter.api.*;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class SystemTest {
    private final static String clientPropsFilename = "client.cfg";

    private Properties clientProps;
    private ShieldingIndividualClientImp client;
    private CateringCompanyClientImp clientCateringCompany;
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
    public void setup(){
        clientProps = loadProperties(clientPropsFilename);
        client = new ShieldingIndividualClientImp(clientProps.getProperty("endpoint"));
        clientCateringCompany = new CateringCompanyClientImp(clientProps.getProperty("endpoint"));
        clientSupermarket = new SupermarketClientImp(clientProps.getProperty("endpoint"));
    }

    @Test
    public void testShieldingIndividualNewRegistration(){
        // invalid CHI number length
        String chi = "374848";
        assertFalse(client.registerShieldingIndividual(chi));
        // invalid CHI number characters
        chi = "qwertyasdf";
        assertFalse(client.registerShieldingIndividual(chi));
        // invalid CHI number birth day
        chi = "5609089090";
        assertFalse(client.registerShieldingIndividual(chi));
        // invalid CHI number birth month
        chi = "1619081820";
        assertFalse(client.registerShieldingIndividual(chi));

        // valid CHI number
        chi = generateRandomValidCHI();
        assertTrue(client.registerShieldingIndividual(chi));
        assertTrue(client.isRegistered());
        assertEquals(client.getCHI(), chi);

        // register again
        assertTrue(client.registerShieldingIndividual(chi));
    }

    @Test
    public void testCateringCompanyNewRegistration(){
        String name = generateRandomName();
        String postCode = generateRandomValidPostcode();

        assertTrue(clientCateringCompany.registerCateringCompany(name, postCode));
        assertTrue(clientCateringCompany.isRegistered());
        assertEquals(clientCateringCompany.getName(), name);
        assertEquals(clientCateringCompany.getPostCode(), postCode);

        // register again
        assertTrue(clientCateringCompany.registerCateringCompany(name, postCode));
    }

    @Test
    public void testSupermarketNewRegistration() {
        String name = generateRandomName();
        String postCode = generateRandomValidPostcode();

        assertTrue(clientSupermarket.registerSupermarket(name, postCode));
        assertTrue(clientSupermarket.isRegistered());
        assertEquals(clientSupermarket.getName(), name);
        assertEquals(clientSupermarket.getPostCode(), postCode);

        // register again
        assertTrue(clientSupermarket.registerSupermarket(name, postCode));
    }

    @Test
    public void testPlaceOrder(){
        // test placeOrder before registers
        assertFalse(client.placeOrder());

        // register ShieldingIndividual
        String chi = generateRandomValidCHI();
        client.registerShieldingIndividual(chi);

        // test placeOrder before pick food box
        assertFalse(client.placeOrder());

        // test placeOrder
        assertTrue(client.pickFoodBox(1));
        assertTrue(client.placeOrder());

        // test placeOrder after placed one this week
        assertFalse(client.placeOrder());
    }

    @Test
    public void testCancelOrder(){
        // cancel an invalid order
        assertFalse(client.cancelOrder(-9));

        // register ShieldingIndividual and place order
        String chi = generateRandomValidCHI();
        assertTrue(client.registerShieldingIndividual(chi));
        assertTrue(client.pickFoodBox(1));
        assertTrue(client.placeOrder());

        // register catering company
        String name = generateRandomName();
        String postCode = generateRandomValidPostcode();
        assertTrue(clientCateringCompany.registerCateringCompany(name, postCode));

        // test cancelOrder when the order had already been dispatched, delivered or cancelled
        int order_id = client.getCurrentOrderId();
        clientCateringCompany.updateOrderStatus(order_id,"dispatched");
        assertTrue(client.requestOrderStatus(order_id));
        assertFalse(client.cancelOrder(order_id));
        clientCateringCompany.updateOrderStatus(order_id, "delivered");
        assertTrue(client.requestOrderStatus(order_id));
        assertFalse(client.cancelOrder(order_id));

        // place another order
        client.setOrderedThisWeek(false);
        assertTrue(client.pickFoodBox(2));
        assertTrue(client.placeOrder());

        // test normal cancelOrder process
        order_id = client.getCurrentOrderId();
        assertTrue(client.cancelOrder(order_id));
    }

    @Test
    public void testEditOrder(){
        // register ShieldingIndividual
        String chi = generateRandomValidCHI();
        assertTrue(client.registerShieldingIndividual(chi));
        assertEquals(client.getCHI(), chi);

        // place order
        assertTrue(client.pickFoodBox(1));
        assertTrue(client.placeOrder());

        // test editOrder when increasing quantity
        assertFalse(client.changeItemQuantityForPickedFoodBox(1, 10));

        // change quantity
        assertTrue(client.changeItemQuantityForPickedFoodBox(2, 1));

        // register catering company
        String name = generateRandomName();
        String postCode = generateRandomValidPostcode();
        assertTrue(clientCateringCompany.registerCateringCompany(name, postCode));

        // test editOrder when already packed, dispatched or delivered
        int orderId = client.getCurrentOrderId();
        assertTrue(clientCateringCompany.updateOrderStatus(orderId, "packed"));
        assertFalse(client.editOrder(orderId));
        assertTrue(clientCateringCompany.updateOrderStatus(orderId, "dispatched"));
        assertFalse(client.editOrder(orderId));
        assertTrue(clientCateringCompany.updateOrderStatus(orderId, "delivered"));
        assertFalse(client.editOrder(orderId));

        // test editOrder when editing immediately after order is placed
        client.setOrderedThisWeek(false);
        assertTrue(client.pickFoodBox(2));
        assertTrue(client.placeOrder());
        assertTrue(client.changeItemQuantityForPickedFoodBox(1, 1));
        orderId = client.getCurrentOrderId();
        assertTrue(client.editOrder(orderId));
    }

    @Test
    public void testRequestOrderStatus(){
        // Invalid order number
        assertFalse(client.requestOrderStatus(-9));

        // register ShieldingIndividual
        String chi = generateRandomValidCHI();
        assertTrue(client.registerShieldingIndividual(chi));
        assertEquals(client.getCHI(), chi);

        // place order
        assertTrue(client.pickFoodBox(1));
        assertTrue(client.placeOrder());

        // test requestOrderStatus
        int orderId = client.getCurrentOrderId();
        assertTrue(client.requestOrderStatus(orderId));
    }

    @Test
    public void testSupermarketUpdateOrderStatus(){
        // register ShieldingIndividual
        String chi = generateRandomValidCHI();
        assertTrue(client.registerShieldingIndividual(chi));
        assertEquals(client.getCHI(), chi);

        // place order
        assertTrue(client.pickFoodBox(1));
        assertTrue(client.placeOrder());

        // get order id
        int orderId = client.getCurrentOrderId();

        // register supermarket
        String name = generateRandomName();
        String postcode = generateRandomValidPostcode();
        assertTrue(clientSupermarket.registerSupermarket(name, postcode));

        // record order
        assertTrue(clientSupermarket.recordSupermarketOrder(chi, orderId));

        // test if the order status is wrong
        assertFalse(clientSupermarket.updateOrderStatus(orderId,"deli"));
        //test if the order status is packed/dispatched/delivered
        assertTrue(clientSupermarket.updateOrderStatus(orderId,"packed"));
        assertTrue(clientSupermarket.updateOrderStatus(orderId,"dispatched"));
        assertTrue(clientSupermarket.updateOrderStatus(orderId,"delivered"));
    }

    @Test
    public void testCateringCompanyUpdateOrderStatus(){
        // register ShieldingIndividual
        String chi = generateRandomValidCHI();
        client.registerShieldingIndividual(chi);

        // place order
        assertTrue(client.pickFoodBox(1));
        assertTrue(client.placeOrder());

        // get order id
        int orderId = client.getCurrentOrderId();
        // register catering company
        String name = generateRandomName();
        String postcode = generateRandomValidPostcode();
        assertTrue(clientCateringCompany.registerCateringCompany(name, postcode));

        // test if the order status is wrong
        assertFalse(clientCateringCompany.updateOrderStatus(orderId,"deli"));
        // test if the order status is packed/dispatched/delivered
        assertTrue(clientCateringCompany.updateOrderStatus(orderId,"packed"));
        assertTrue(clientCateringCompany.updateOrderStatus(orderId,"dispatched"));
        assertTrue(clientCateringCompany.updateOrderStatus(orderId,"delivered"));
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

    private String generateRandomValidPostcode() {
        int num1 = randInterval(1,17);
        int num2 = randInterval(1,9);
        char c1 = (char) randInterval(65, 90);
        char c2 = (char) randInterval(65, 90);
        return String.format("EH%d_%d%c%c", num1, num2, c1, c2);
    }

    private String generateRandomName() {
        StringBuilder name = new StringBuilder();
        // length set to min 4 max 10
        int length = randInterval(4,10);
        for (int i = 0; i < length; i++) {
            name.append((char) randInterval(97,122));
        }
        return name.toString();
    }

    private int randInterval(int start, int end) {
        Random rand = new Random();
        return rand.nextInt(end - start + 1) + start;
    }
}
