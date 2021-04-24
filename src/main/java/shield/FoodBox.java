package shield;

import java.util.List;

public class FoodBox {
    private List<Product> contents;
    private String delivered_by;
    private String diet;
    private String id;
    private String name;

    public FoodBox(List<Product> contents, String delivered_by, String diet, String id, String name) {
        this.contents = contents;
        this.delivered_by = delivered_by;
        this.diet = diet;
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public List<Product> getContents() {
        return contents;
    }

    public String getDiet() {
        return diet;
    }

    public void setContents(List<Product> contents) {
        this.contents = contents;
    }

    public void setDelivered_by(String delivered_by) {
        this.delivered_by = delivered_by;
    }

    public void setDiet(String diet) {
        this.diet = diet;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
}
