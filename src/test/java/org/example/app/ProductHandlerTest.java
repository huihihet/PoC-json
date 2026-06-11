package org.example.app;

import org.example.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class ProductHandlerTest {

    ProductHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProductHandler();
    }

    // ── promptAdd ──────────────────────────────────────────

    @Test
    void promptAdd_정상입력() {
        Scanner scanner = scannerOf("무선 키보드", "89900", "전자제품,키보드", "true");
        Product p = handler.promptAdd(scanner, 1L);
        assertAll(
            () -> assertEquals(1L,                          p.getId()),
            () -> assertEquals("무선 키보드",                p.getName()),
            () -> assertEquals(89900.0,                     p.getPrice()),
            () -> assertEquals(List.of("전자제품", "키보드"), p.getTags()),
            () -> assertTrue(p.isInStock())
        );
    }

    @Test
    void promptAdd_tags_빈입력() {
        Scanner scanner = scannerOf("키보드", "10000", "", "false");
        Product p = handler.promptAdd(scanner, 2L);
        assertTrue(p.getTags().isEmpty());
        assertFalse(p.isInStock());
    }

    @Test
    void promptAdd_price_재입력() {
        // 첫 번째 price 입력이 숫자 아님 → 재입력
        Scanner scanner = scannerOf("키보드", "abc", "59900", "", "true");
        Product p = handler.promptAdd(scanner, 3L);
        assertEquals(59900.0, p.getPrice());
    }

    @Test
    void promptAdd_inStock_재입력() {
        // 첫 번째 in_stock 입력이 잘못됨 → 재입력
        Scanner scanner = scannerOf("키보드", "10000", "", "yes", "true");
        Product p = handler.promptAdd(scanner, 4L);
        assertTrue(p.isInStock());
    }

    @Test
    void promptAdd_name_빈입력_재요청() {
        // 첫 번째 name 빈 입력 → 재입력 요청 → 두 번째 성공
        Scanner scanner = scannerOf("", "키보드", "10000", "", "true");
        Product p = handler.promptAdd(scanner, 5L);
        assertEquals("키보드", p.getName());
    }

    @Test
    void promptAdd_EOF_throws() {
        // name 입력 전 EOF → InputTerminatedException
        Scanner scanner = scannerOf();
        assertThrows(ProductHandler.InputTerminatedException.class,
            () -> handler.promptAdd(scanner, 6L));
    }

    // ── promptUpdate ───────────────────────────────────────

    @Test
    void promptUpdate_전체_빈입력_기존값_유지() {
        Product existing = product(1L, "무선 키보드", 89900.0, List.of("전자제품"), true);
        Scanner scanner = scannerOf("", "", "", "");
        Product updated = handler.promptUpdate(scanner, existing);
        assertAll(
            () -> assertEquals(existing.getName(),   updated.getName()),
            () -> assertEquals(existing.getPrice(),  updated.getPrice()),
            () -> assertEquals(existing.getTags(),   updated.getTags()),
            () -> assertEquals(existing.isInStock(), updated.isInStock()),
            () -> assertEquals(existing.getId(),     updated.getId())
        );
    }

    @Test
    void promptUpdate_일부_변경() {
        Product existing = product(1L, "무선 키보드", 89900.0, List.of("전자제품"), true);
        Scanner scanner = scannerOf("유선 키보드", "", "", "false");
        Product updated = handler.promptUpdate(scanner, existing);
        assertAll(
            () -> assertEquals("유선 키보드",         updated.getName()),
            () -> assertEquals(89900.0,              updated.getPrice()),
            () -> assertEquals(List.of("전자제품"),   updated.getTags()),
            () -> assertFalse(updated.isInStock())
        );
    }

    @Test
    void promptUpdate_EOF_throws() {
        Product existing = product(1L, "키보드", 10000.0, List.of(), true);
        Scanner scanner = scannerOf();
        assertThrows(ProductHandler.InputTerminatedException.class,
            () -> handler.promptUpdate(scanner, existing));
    }

    // ── 헬퍼 ───────────────────────────────────────────────

    private Scanner scannerOf(String... lines) {
        String input = String.join("\n", lines) + "\n";
        return new Scanner(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    }

    private Product product(Long id, String name, double price, List<String> tags, boolean inStock) {
        return new Product(id, name, price, tags, inStock, Map.of());
    }
}
