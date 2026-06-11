package org.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class Product {

    private Long id;
    private String name;
    private double price;
    private List<String> tags;

    @JsonProperty("in_stock")
    private boolean inStock;

    private Map<String, String> metadata;

    public Product() {}

    public Product(Long id, String name, double price, List<String> tags, boolean inStock, Map<String, String> metadata) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.tags = tags;
        this.inStock = inStock;
        this.metadata = metadata;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public boolean isInStock() { return inStock; }
    public void setInStock(boolean inStock) { this.inStock = inStock; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    @Override
    public String toString() {
        return "Product{id=" + id + ", name='" + name + "', price=" + price +
               ", tags=" + tags + ", inStock=" + inStock + ", metadata=" + metadata + "}";
    }
}
