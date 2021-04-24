/**
 *
 */

package shield;

public class SupermarketClientImp implements SupermarketClient {

    /** endpoint of the server **/
    private String endpoint;
    /** register status **/
    private boolean registered;
    /** name of the supermarket **/
    private String name;
    /** postcode of the supermarket **/
    private String postcode;

    public SupermarketClientImp(String endpoint) {
        this.endpoint = endpoint;
        this.registered = false;
    }

    /**
     * registers a supermarket given name and postcode. Checks postcode is valid.
     * @param name name of the business
     * @param postCode postcode of the supermarket
     * @return boolean value represents the success of the registration
     */
    @Override
    public boolean registerSupermarket(String name, String postCode) {
        if (!validPostcode(postCode)) {
            return false;
        }
        String request = String.format("/registerSupermarket?business_name=%s&postcode=%s", name, postCode);

        try {
            String response = ClientIO.doGETRequest(endpoint + request);
            if (response.equals("registered new")) {
                this.name = name;
                this.postcode = postCode;
                this.registered = true;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * record an order to the supermarket given a CHI and an order number. 
     * @param CHI CHI number of the shiedling individual associated with this order
     * @param orderNumber the order number
     * @return
     */
    @Override
    public boolean recordSupermarketOrder(String CHI, int orderNumber) {
        String request = String.format(
                "/recordSupermarketOrder?individual_id=%s" +
                        "&order_number=%s&supermarket_business_name=%s" +
                        "&supermarket_postcode=%s", CHI, orderNumber, name, postcode);
        try {
            String response = ClientIO.doGETRequest(endpoint + request);
            return Boolean.parseBoolean(response);
        } catch (Exception e) {
            return false;
        }
    }

    // **UPDATE**
    @Override
    public boolean updateOrderStatus(int orderNumber, String status) {
        if (!isRegistered()) {
            return false;
        }
        String request = String.format("/updateSupermarketOrderStatus?order_id=%s&newStatus=%s",
                orderNumber, status);
        try {
            String response = ClientIO.doGETRequest(endpoint + request);
            return Boolean.parseBoolean(response);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isRegistered() {
        return this.registered;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getPostCode() {
        return this.postcode;
    }

    private boolean validPostcode(String postcode) {
        try {
            String[] parts = postcode.split("_");
            if (!parts[0].substring(0,2).equals("EH")) {
                return false;
            }
            int num1 = Integer.parseInt(parts[0].substring(2));
            if (num1 < 1 || num1 > 17) {
                return false;
            }
            return Character.isDigit(parts[1].charAt(0)) && Character.isUpperCase(parts[1].charAt(1))
                    && Character.isUpperCase(parts[1].charAt(2));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * setter method for field registered
     * @param registered Boolean value for field registered.
     */
    public void setRegistered(boolean registered) {
        this.registered = registered;
    }
}
