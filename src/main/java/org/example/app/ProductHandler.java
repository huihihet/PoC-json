package org.example.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.model.Product;

import java.util.List;

public class ProductHandler {

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public void printTable(List<Product> products) {
        if (products.isEmpty()) {
            System.out.println("데이터가 없습니다.");
            return;
        }

        String fmt = "%-5s  %-22s  %-12s  %-10s  %s%n";
        System.out.printf(fmt, "ID", "NAME", "PRICE", "IN_STOCK", "TAGS");
        System.out.printf(fmt, "----", "--------------------", "----------", "--------", "--------------------");
        for (Product p : products) {
            System.out.printf(fmt,
                    p.getId(),
                    p.getName(),
                    p.getPrice(),
                    p.isInStock(),
                    p.getTags() != null ? p.getTags() : "[]"
            );
        }
    }

    public void printOne(Product product) {
        try {
            System.out.println(mapper.writeValueAsString(product));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
