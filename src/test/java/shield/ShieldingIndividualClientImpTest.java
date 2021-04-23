/**
 *
 */

package shield;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Properties;
import java.time.LocalDateTime;
import java.io.InputStream;

import java.util.Random;

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
        String chi = String.valueOf("2908041081");

        assertTrue(client.registerShieldingIndividual(chi));
        assertTrue(client.isRegistered());
        assertEquals(client.getCHI(), chi);
    }

    @Test
    public void testPlaceOrder() {
        String request = "/registerShieldingIndividual?CHI=2908041082";
        try{
        String endpoint = clientProps.getProperty("endpoint");
        String response = ClientIO.doGETRequest(endpoint + request);
            if (!response.equals("already registered")) {
                client.pickFoodBox(1);
                client.placeOrder();
            }
        }
        catch (Exception e) {
        }
    }

    /*
    @Test
    public void testCancelOrder() {
        client.registerShieldingIndividual("2908041083");
        client.pickFoodBox(1);
        client.placeOrder();

        int order_id = client.getCurrentOrderId();
        assertTrue(client.cancelOrder(order_id));
    }

    @Test
    public void testEditOrder(){
        client.registerShieldingIndividual("2908041084");
        client.pickFoodBox(1);
        client.placeOrder();

        int order_id = client.getCurrentOrderId();
        client.editOrder(order_id);
    }

    @Test
    public void testShowFoodBoxes(){
        client.showFoodBoxes("none");
        client.showFoodBoxes("vegan");
        client.showFoodBoxes("pollotarian");
    }

    @Test
    public void testRequestOrderStatus(){
        client.registerShieldingIndividual("2908041085");
        client.pickFoodBox(1);
        client.placeOrder();

        int order_id = client.getCurrentOrderId();
        client.requestOrderStatus(order_id);
    }

    @Test
    public void testGetCateringCompanies(){
        client.getCateringCompanies();
    }

    @Test
    public void testGetDistance(){
        client.getDistance("EH16_5AY","EH7_5AA");
    }

    @Test
    public void testGetDietaryPreferenceForFoodBox(){
        client.getDietaryPreferenceForFoodBox(1);
    }*/
}
