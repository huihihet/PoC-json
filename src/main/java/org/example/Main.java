package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import org.example.gson.GsonDemo;
import org.example.jackson.JacksonDemo;
import org.example.model.Product;
import org.example.storage.JsonFileStorage;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class Main {

    static final String SAMPLE_JSON = """
            {
              "id": 1,
              "name": "무선 키보드",
              "price": 89900,
              "tags": ["전자제품", "키보드", "무선"],
              "in_stock": true,
              "metadata": {
                "brand": "로지텍",
                "color": "블랙",
                "warranty": "1년"
              }
            }
            """;

    static final String SAMPLE_JSON_LIST = """
            [
              {"id": 1, "name": "무선 키보드", "price": 89900, "tags": ["전자제품"], "in_stock": true, "metadata": {}},
              {"id": 2, "name": "게이밍 마우스", "price": 59900, "tags": ["전자제품", "마우스"], "in_stock": false, "metadata": {}},
              {"id": 3, "name": "모니터", "price": 350000, "tags": ["전자제품", "디스플레이"], "in_stock": true, "metadata": {}}
            ]
            """;

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        System.out.println("=== Jackson POC ===");
        jacksonDemo();

        System.out.println("\n=== Gson POC ===");
        gsonDemo();

        System.out.println("\n=== JsonFileStorage POC ===");
        fileStorageDemo();
    }

    static void jacksonDemo() throws Exception {
        JacksonDemo demo = new JacksonDemo();

        // 1. 문자열 파싱
        Product product = demo.parseFromString(SAMPLE_JSON);
        System.out.println("[파싱 결과] " + product);

        // 2. 리스트 파싱
        List<Product> products = demo.parseListFromString(SAMPLE_JSON_LIST);
        System.out.println("[리스트 파싱] " + products.size() + "개");

        // 3. 객체 → JSON 문자열
        String json = demo.toJson(product);
        System.out.println("[직렬화]\n" + json);

        // 4. JsonNode로 동적 접근
        demo.exploreWithJsonNode(SAMPLE_JSON);

        // 5. Map으로 파싱 (스키마 없을 때)
        Map<String, Object> map = demo.toMap(SAMPLE_JSON);
        System.out.println("[Map 파싱] keys=" + map.keySet());

        // 6. 클래스패스 리소스 파일 읽기
        try (InputStream is = Main.class.getResourceAsStream("/sample.json")) {
            String fileJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Product fromFile = demo.parseFromString(fileJson);
            System.out.println("[리소스 파일 파싱] " + fromFile.getName());
        }
    }

    static void gsonDemo() throws Exception {
        GsonDemo demo = new GsonDemo();

        // 1. 문자열 파싱
        Product product = demo.parseFromString(SAMPLE_JSON);
        System.out.println("[파싱 결과] " + product);

        // 2. 리스트 파싱
        List<Product> products = demo.parseListFromString(SAMPLE_JSON_LIST);
        System.out.println("[리스트 파싱] " + products.size() + "개");

        // 3. 직렬화
        String json = demo.toJson(product);
        System.out.println("[직렬화]\n" + json);

        // 4. JsonElement 동적 접근
        demo.exploreWithJsonElement(SAMPLE_JSON);
    }

    static void fileStorageDemo() throws Exception {
        String storagePath = System.getProperty("java.io.tmpdir") + "/poc_products.json";
        JsonFileStorage<Product> storage = new JsonFileStorage<>(storagePath, new TypeReference<>() {});

        // 기존 파일 정리
        storage.clear();

        // 항목 추가
        JacksonDemo jacksonDemo = new JacksonDemo();
        List<Product> products = jacksonDemo.parseListFromString(SAMPLE_JSON_LIST);
        storage.saveAll(products);
        System.out.println("[저장 완료] " + storage.count() + "개 → " + storagePath);

        // 단일 항목 추가
        Product newProduct = jacksonDemo.parseFromString(
            """
            {"id": 4, "name": "USB 허브", "price": 25000, "tags": ["전자제품"], "in_stock": true, "metadata": {}}
            """
        );
        storage.append(newProduct);
        System.out.println("[항목 추가 후] " + storage.count() + "개");

        // 조건 조회 (재고 있는 것만)
        List<Product> inStock = storage.findBy(Product::isInStock);
        System.out.println("[재고 있는 상품] " + inStock.stream().map(Product::getName).toList());

        // 파일에서 다시 읽기 확인
        List<Product> reloaded = storage.findAll();
        System.out.println("[파일 재로딩] " + reloaded.size() + "개 확인");

        // 저장된 파일 내용 일부 출력
        File f = storage.getStorageFile();
        System.out.println("[저장 파일 크기] " + f.length() + " bytes");
    }
}
