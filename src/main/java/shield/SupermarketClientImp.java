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
            if (response.equals(Utilities.NEW_REGISTER_MESSAGE)) {
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
    return false;
    }

    // **UPDATE**
    @Override
    public boolean updateOrderStatus(int orderNumber, String status) {
    return false;
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
