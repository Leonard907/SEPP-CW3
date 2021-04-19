package shield;

public class Order {
    private int id;
    private int status;
    private FoodBox foodBox;

    public Order(int id, int status, FoodBox foodBox) {
        this.id = id;
        this.status = status;
        this.foodBox = foodBox;
    }

    public int getStatus() {
        return status;
    }

    public int getId() {
        return id;
    }

    public FoodBox getFoodBox() {
        return foodBox;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setFoodBox(FoodBox foodBox) {
        this.foodBox = foodBox;
    }
}
