package org.example.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.model.Product;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class ProductHandler {

    static class InputTerminatedException extends RuntimeException {}


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

    public Product promptAdd(Scanner scanner, long nextId) {
        String name = readRequiredString(scanner, "name");

        double price = readDouble(scanner, "price");

        List<String> tags = readTags(scanner);

        boolean inStock = readBoolean(scanner, "in_stock");

        return new Product(nextId, name, price, tags, inStock, new HashMap<>());
    }

    private String readRequiredString(Scanner scanner, String field) {
        while (true) {
            if (!scanner.hasNextLine()) throw new InputTerminatedException();
            System.out.print("  " + field + "      : ");
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) return value;
            System.out.println("[오류] 값을 입력하세요.");
        }
    }

    private double readDouble(Scanner scanner, String field) {
        while (true) {
            if (!scanner.hasNextLine()) throw new InputTerminatedException();
            System.out.print("  " + field + "     : ");
            String value = scanner.nextLine().trim();
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                System.out.println("[오류] 숫자를 입력하세요.");
            }
        }
    }

    private List<String> readTags(Scanner scanner) {
        if (!scanner.hasNextLine()) throw new InputTerminatedException();
        System.out.print("  tags      : ");
        String value = scanner.nextLine().trim();
        if (value.isEmpty()) return Collections.emptyList();
        return Arrays.stream(value.split(","))
                     .map(String::trim)
                     .filter(s -> !s.isEmpty())
                     .toList();
    }

    private boolean readBoolean(Scanner scanner, String field) {
        while (true) {
            if (!scanner.hasNextLine()) throw new InputTerminatedException();
            System.out.print("  " + field + "  : ");
            String value = scanner.nextLine().trim().toLowerCase();
            if (value.equals("true"))  return true;
            if (value.equals("false")) return false;
            System.out.println("[오류] true 또는 false를 입력하세요.");
        }
    }

    public Product promptUpdate(Scanner scanner, Product existing) {
        String tagsStr = existing.getTags() != null
                ? String.join(",", existing.getTags()) : "";

        String name = readOptionalString(scanner, "name", existing.getName());
        double price = readOptionalDouble(scanner, "price", existing.getPrice());
        List<String> tags = readOptionalTags(scanner, tagsStr, existing.getTags());
        boolean inStock = readOptionalBoolean(scanner, "in_stock", existing.isInStock());

        return new Product(existing.getId(), name, price, tags, inStock, existing.getMetadata());
    }

    private String readOptionalString(Scanner scanner, String field, String current) {
        if (!scanner.hasNextLine()) throw new InputTerminatedException();
        System.out.print("  " + field + "      (" + current + ") : ");
        String value = scanner.nextLine().trim();
        return value.isEmpty() ? current : value;
    }

    private double readOptionalDouble(Scanner scanner, String field, double current) {
        while (true) {
            if (!scanner.hasNextLine()) throw new InputTerminatedException();
            System.out.print("  " + field + "     (" + current + ") : ");
            String value = scanner.nextLine().trim();
            if (value.isEmpty()) return current;
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                System.out.println("[오류] 숫자를 입력하세요.");
            }
        }
    }

    private List<String> readOptionalTags(Scanner scanner, String currentStr, List<String> current) {
        if (!scanner.hasNextLine()) throw new InputTerminatedException();
        System.out.print("  tags      (" + currentStr + ") : ");
        String value = scanner.nextLine().trim();
        if (value.isEmpty()) return current;
        return Arrays.stream(value.split(","))
                     .map(String::trim)
                     .filter(s -> !s.isEmpty())
                     .toList();
    }

    private boolean readOptionalBoolean(Scanner scanner, String field, boolean current) {
        while (true) {
            if (!scanner.hasNextLine()) throw new InputTerminatedException();
            System.out.print("  " + field + "  (" + current + ") : ");
            String value = scanner.nextLine().trim().toLowerCase();
            if (value.isEmpty())           return current;
            if (value.equals("true"))      return true;
            if (value.equals("false"))     return false;
            System.out.println("[오류] true 또는 false를 입력하세요.");
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
