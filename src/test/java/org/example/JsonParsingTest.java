package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import org.example.gson.GsonDemo;
import org.example.jackson.JacksonDemo;
import org.example.model.Product;
import org.example.storage.JsonFileStorage;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonParsingTest {

    static final String SINGLE_JSON = """
            {"id":1,"name":"키보드","price":89900,"tags":["전자제품"],"in_stock":true,"metadata":{"brand":"로지텍"}}
            """;

    static final String LIST_JSON = """
            [
              {"id":1,"name":"키보드","price":89900,"tags":["전자제품"],"in_stock":true,"metadata":{}},
              {"id":2,"name":"마우스","price":59900,"tags":["전자제품"],"in_stock":false,"metadata":{}}
            ]
            """;

    // ────────── Jackson ──────────

    @Nested
    class JacksonTests {
        JacksonDemo demo = new JacksonDemo();

        @Test
        void 단일_객체_파싱() throws IOException {
            Product p = demo.parseFromString(SINGLE_JSON);
            assertAll(
                () -> assertEquals(1L, p.getId()),
                () -> assertEquals("키보드", p.getName()),
                () -> assertEquals(89900.0, p.getPrice()),
                () -> assertTrue(p.isInStock()),
                () -> assertEquals(List.of("전자제품"), p.getTags()),
                () -> assertEquals("로지텍", p.getMetadata().get("brand"))
            );
        }

        @Test
        void 리스트_파싱() throws IOException {
            List<Product> list = demo.parseListFromString(LIST_JSON);
            assertEquals(2, list.size());
            assertFalse(list.get(1).isInStock());
        }

        @Test
        void 직렬화_후_역직렬화_동등성() throws IOException {
            Product original = demo.parseFromString(SINGLE_JSON);
            String json = demo.toJson(original);
            Product restored = demo.parseFromString(json);
            assertAll(
                () -> assertEquals(original.getId(), restored.getId()),
                () -> assertEquals(original.getName(), restored.getName()),
                () -> assertEquals(original.getPrice(), restored.getPrice())
            );
        }

        @Test
        void Map으로_파싱() throws IOException {
            Map<String, Object> map = demo.toMap(SINGLE_JSON);
            assertEquals("키보드", map.get("name"));
            // Jackson은 소수점 없는 숫자를 Integer로 파싱하므로 Number로 비교
            assertEquals(89900.0, ((Number) map.get("price")).doubleValue());
        }
    }

    // ────────── Gson ──────────

    @Nested
    class GsonTests {
        GsonDemo demo = new GsonDemo();

        @Test
        void 단일_객체_파싱() {
            Product p = demo.parseFromString(SINGLE_JSON);
            assertAll(
                () -> assertEquals(1L, p.getId()),
                () -> assertEquals("키보드", p.getName()),
                () -> assertEquals(89900.0, p.getPrice()),
                () -> assertTrue(p.isInStock())
            );
        }

        @Test
        void 리스트_파싱() {
            List<Product> list = demo.parseListFromString(LIST_JSON);
            assertEquals(2, list.size());
        }

        @Test
        void 직렬화_후_역직렬화_동등성() {
            Product original = demo.parseFromString(SINGLE_JSON);
            String json = demo.toJson(original);
            Product restored = demo.parseFromString(json);
            assertEquals(original.getName(), restored.getName());
        }
    }

    // ────────── FileStorage ──────────

    @Nested
    class FileStorageTests {
        File tempFile;
        JsonFileStorage<Product> storage;

        @BeforeEach
        void setUp() throws IOException {
            tempFile = Files.createTempFile("poc_test_", ".json").toFile();
            tempFile.deleteOnExit();
            storage = new JsonFileStorage<>(tempFile.getAbsolutePath(), new TypeReference<>() {});
        }

        @AfterEach
        void tearDown() {
            tempFile.delete();
        }

        @Test
        void 파일_없으면_빈_리스트() throws IOException {
            tempFile.delete();
            assertEquals(0, storage.count());
        }

        @Test
        void 저장_후_읽기() throws IOException {
            JacksonDemo jackson = new JacksonDemo();
            List<Product> products = jackson.parseListFromString(LIST_JSON);
            storage.saveAll(products);

            List<Product> loaded = storage.findAll();
            assertEquals(2, loaded.size());
            assertEquals("키보드", loaded.get(0).getName());
        }

        @Test
        void 항목_추가() throws IOException {
            JacksonDemo jackson = new JacksonDemo();
            Product p = jackson.parseFromString(SINGLE_JSON);
            storage.append(p);
            storage.append(p);
            assertEquals(2, storage.count());
        }

        @Test
        void 조건_조회() throws IOException {
            JacksonDemo jackson = new JacksonDemo();
            List<Product> products = jackson.parseListFromString(LIST_JSON);
            storage.saveAll(products);

            List<Product> inStock = storage.findBy(Product::isInStock);
            assertEquals(1, inStock.size());
            assertEquals("키보드", inStock.get(0).getName());
        }
    }
}
