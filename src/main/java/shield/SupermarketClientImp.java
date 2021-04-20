/**
 *
 */

package shield;

public class SupermarketClientImp implements SupermarketClient {

    private String endpoint;
    private boolean registered;
    private String name;
    private String postcode;

    public SupermarketClientImp(String endpoint) {
        this.endpoint = endpoint;
        this.registered = false;
    }

    @Override
    public boolean registerSupermarket(String name, String postCode) {
        String request = String.format("registerSupermarket?business_name=%s&postcode=%s", name, postCode);

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

    // **UPDATE2** ADDED METHOD
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
}
