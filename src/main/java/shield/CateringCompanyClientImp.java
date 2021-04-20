/**
 *
 */

package shield;

public class CateringCompanyClientImp implements CateringCompanyClient {
    private String endpoint;
    private String postcode;
    private String name;
    private boolean registered;

    public CateringCompanyClientImp(String endpoint) {
        this.endpoint = endpoint;
        this.registered = false;
    }

    @Override
    public boolean registerCateringCompany(String name, String postCode) {
        String request = String.format("/registerCateringCompany?business_name=%s&postcode=%s", name, postCode);

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

    private boolean validPostcode(String postcode) {
        return false;
    }
}
