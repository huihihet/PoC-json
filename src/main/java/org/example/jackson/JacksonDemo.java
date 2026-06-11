package org.example.jackson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.model.Product;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JacksonDemo {

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /** JSON 문자열 → 객체 역직렬화 */
    public Product parseFromString(String json) throws IOException {
        return mapper.readValue(json, Product.class);
    }

    /** JSON 문자열 → 리스트 역직렬화 */
    public List<Product> parseListFromString(String json) throws IOException {
        return mapper.readValue(json, new TypeReference<>() {});
    }

    /** 객체 → JSON 문자열 직렬화 */
    public String toJson(Object obj) throws IOException {
        return mapper.writeValueAsString(obj);
    }

    /** JSON 파일 → 객체 */
    public Product readFromFile(File file) throws IOException {
        return mapper.readValue(file, Product.class);
    }

    /** JSON 파일 → 리스트 */
    public List<Product> readListFromFile(File file) throws IOException {
        return mapper.readValue(file, new TypeReference<>() {});
    }

    /** 객체 → JSON 파일 저장 */
    public void writeToFile(Object obj, File file) throws IOException {
        mapper.writeValue(file, obj);
    }

    /** JsonNode로 동적 필드 접근 (스키마 불명확한 경우) */
    public void exploreWithJsonNode(String json) throws IOException {
        JsonNode root = mapper.readTree(json);
        System.out.println("[Jackson JsonNode]");
        System.out.println("  name  : " + root.path("name").asText());
        System.out.println("  price : " + root.path("price").asDouble());
        System.out.println("  tags  : " + root.path("tags"));

        // 중첩 필드
        root.path("metadata").fields().forEachRemaining(entry ->
            System.out.println("  meta." + entry.getKey() + " = " + entry.getValue().asText())
        );
    }

    /** 타입 정보 없이 Map으로 파싱 */
    public Map<String, Object> toMap(String json) throws IOException {
        return mapper.readValue(json, new TypeReference<>() {});
    }
}
