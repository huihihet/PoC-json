package org.example.gson;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.example.model.Product;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class GsonDemo {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            // Gson은 @JsonProperty 대신 @SerializedName 사용
            // Product 모델과 필드명 불일치(in_stock)는 아래 커스텀 없이 snake_case 정책 적용
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    /** JSON 문자열 → 객체 */
    public Product parseFromString(String json) {
        return gson.fromJson(json, Product.class);
    }

    /** JSON 문자열 → 리스트 */
    public List<Product> parseListFromString(String json) {
        Type listType = new TypeToken<List<Product>>() {}.getType();
        return gson.fromJson(json, listType);
    }

    /** 객체 → JSON 문자열 */
    public String toJson(Object obj) {
        return gson.toJson(obj);
    }

    /** JSON 파일 → 객체 */
    public Product readFromFile(File file) throws IOException {
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, Product.class);
        }
    }

    /** 객체 → JSON 파일 저장 */
    public void writeToFile(Object obj, File file) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(obj, writer);
        }
    }

    /** JsonElement로 동적 필드 접근 */
    public void exploreWithJsonElement(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        System.out.println("[Gson JsonElement]");
        System.out.println("  name  : " + root.get("name").getAsString());
        System.out.println("  price : " + root.get("price").getAsDouble());

        JsonArray tags = root.getAsJsonArray("tags");
        tags.forEach(tag -> System.out.println("  tag   : " + tag.getAsString()));

        JsonObject meta = root.getAsJsonObject("metadata");
        meta.entrySet().forEach(e ->
            System.out.println("  meta." + e.getKey() + " = " + e.getValue().getAsString())
        );
    }
}
