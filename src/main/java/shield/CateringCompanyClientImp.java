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
        if (!validPostcode(postCode)) {
            return false;
        }
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
        if (!isRegistered()) {
            return false;
        }
        String request = String.format("/updateOrderStatus?order_id=%s&newStatus=%s",
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
     * setter method for field registered.
     * @param registered Boolean value for field registered.
     */
    public void setRegistered(boolean registered) {
        this.registered = registered;
    }
}
