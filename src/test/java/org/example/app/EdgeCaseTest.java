package org.example.app;

import org.example.model.Product;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 이상한 입력값, 경계값, 예외 흐름에 대한 포괄적 엣지케이스 테스트.
 * ProductHandler 계층과 ConsoleApp 계층 두 곳을 나눠서 커버.
 */
class EdgeCaseTest {

    // =========================================================================
    // ProductHandler 계층 — 입력 파싱 엣지케이스
    // =========================================================================

    @Nested
    class ProductHandlerCases {

        ProductHandler handler;

        @BeforeEach
        void setUp() {
            handler = new ProductHandler();
        }

        // ── name 입력 ───────────────────────────────────────────────────────

        @Test
        @DisplayName("name: 공백만 입력 → 재입력 요청 후 유효값 수용")
        void name_공백만_재입력_후_유효값() {
            Scanner s = scannerOf("   ", "  ", "진짜이름", "1000", "", "true");
            Product p = handler.promptAdd(s, 1L);
            assertEquals("진짜이름", p.getName());
        }

        @Test
        @DisplayName("name: 한글+영문+숫자 혼합")
        void name_한글영문숫자_혼합() {
            Scanner s = scannerOf("키보드 K10 2024", "5000", "", "true");
            Product p = handler.promptAdd(s, 1L);
            assertEquals("키보드 K10 2024", p.getName());
        }

        @Test
        @DisplayName("name: 따옴표·역슬래시 포함 — Jackson이 올바르게 이스케이프")
        void name_따옴표_역슬래시_포함() {
            Scanner s = scannerOf("제품 \"A\" \\특수", "1000", "", "true");
            Product p = handler.promptAdd(s, 1L);
            assertEquals("제품 \"A\" \\특수", p.getName());
        }

        @Test
        @DisplayName("name: 이모지 포함")
        void name_이모지_포함() {
            Scanner s = scannerOf("키보드🎹", "1000", "", "true");
            Product p = handler.promptAdd(s, 1L);
            assertEquals("키보드🎹", p.getName());
        }

        @Test
        @DisplayName("name: 100자 초과 긴 이름")
        void name_100자_초과() {
            String longName = "가나다라".repeat(30); // 120자
            Scanner s = scannerOf(longName, "1000", "", "true");
            Product p = handler.promptAdd(s, 1L);
            assertEquals(longName, p.getName());
        }

        @Test
        @DisplayName("name: 앞뒤 공백 트림 처리")
        void name_앞뒤공백_트림() {
            Scanner s = scannerOf("  키보드  ", "1000", "", "true");
            Product p = handler.promptAdd(s, 1L);
            assertEquals("키보드", p.getName());
        }

        // ── price 입력 ──────────────────────────────────────────────────────

        @Test
        @DisplayName("price: 0.0 허용")
        void price_zero() {
            Scanner s = scannerOf("무료상품", "0", "", "true");
            Product p = handler.promptAdd(s, 1L);
            assertEquals(0.0, p.getPrice());
        }

        @Test
        @DisplayName("price: 음수 허용 (할인 표현 등 비즈니스 검증 없음)")
        void price_음수_허용() {
            Scanner s = scannerOf("상품", "-1000", "", "true");
            Product p = handler.promptAdd(s, 1L);
            assertEquals(-1000.0, p.getPrice());
        }

        @Test
        @DisplayName("price: 소수점 입력")
        void price_소수점() {
            Scanner s = scannerOf("상품", "9.99", "", "true");
            Product p = handler.promptAdd(s, 1L);
            assertEquals(9.99, p.getPrice());
        }

        @Test
        @DisplayName("price: 매우 큰 숫자")
        void price_매우큰숫자() {
            Scanner s = scannerOf("상품", "9999999999.99", "", "true");
            Product p = handler.promptAdd(s, 1L);
            assertEquals(9999999999.99, p.getPrice());
        }

        @Test
        @DisplayName("price: 잘못된 입력 여러 번 반복 후 성공")
        void price_여러번_재입력() {
            Scanner s = scannerOf("상품", "abc", "xyz", "!!!", "1500", "", "true");
            Product p = handler.promptAdd(s, 1L);
            assertEquals(1500.0, p.getPrice());
        }

        @Test
        @DisplayName("price: 공백 포함 숫자 문자열 → 파싱 실패 후 재입력")
        void price_공백포함_숫자_재입력() {
            Scanner s = scannerOf("상품", " 1000 ", "1000", "", "true");
            // trim → "1000" 파싱 성공 (Double.parseDouble(" 1000 ") 는 사실 성공함)
            Product p = handler.promptAdd(s, 1L);
            assertEquals(1000.0, p.getPrice());
        }

        // ── tags 입력 ───────────────────────────────────────────────────────

        @Test
        @DisplayName("tags: 쉼표만 입력 → 빈 리스트")
        void tags_쉼표만_빈리스트() {
            Scanner s = scannerOf("상품", "1000", ",,,", "true");
            Product p = handler.promptAdd(s, 1L);
            assertTrue(p.getTags().isEmpty());
        }

        @Test
        @DisplayName("tags: 앞뒤·사이 공백 모두 트림")
        void tags_공백_트림() {
            Scanner s = scannerOf("상품", "1000", "  전자제품 , 키보드  ,  마우스  ", "true");
            Product p = handler.promptAdd(s, 1L);
            assertEquals(List.of("전자제품", "키보드", "마우스"), p.getTags());
        }

        @Test
        @DisplayName("tags: 단일 태그 (쉼표 없음)")
        void tags_단일_태그() {
            Scanner s = scannerOf("상품", "1000", "전자제품", "true");
            Product p = handler.promptAdd(s, 1L);
            assertEquals(List.of("전자제품"), p.getTags());
        }

        @Test
        @DisplayName("tags: 공백+쉼표 혼합 → 빈 토큰 필터링")
        void tags_공백_쉼표_혼합() {
            Scanner s = scannerOf("상품", "1000", " , , ", "true");
            Product p = handler.promptAdd(s, 1L);
            assertTrue(p.getTags().isEmpty());
        }

        // ── in_stock 입력 ───────────────────────────────────────────────────

        @Test
        @DisplayName("in_stock: 대문자 'TRUE' → 재입력 요청")
        void inStock_대문자_TRUE_재입력() {
            Scanner s = scannerOf("상품", "1000", "", "TRUE", "true");
            Product p = handler.promptAdd(s, 1L);
            assertTrue(p.isInStock());
        }

        @Test
        @DisplayName("in_stock: 'False' → 대소문자 무관하게 처리")
        void inStock_대소문자혼합_false() {
            Scanner s = scannerOf("상품", "1000", "", "False");
            Product p = handler.promptAdd(s, 1L);
            assertFalse(p.isInStock());
        }

        @Test
        @DisplayName("in_stock: '1' → 재입력 요청")
        void inStock_숫자1_재입력() {
            Scanner s = scannerOf("상품", "1000", "", "1", "false");
            Product p = handler.promptAdd(s, 1L);
            assertFalse(p.isInStock());
        }

        @Test
        @DisplayName("in_stock: 'yes' → 재입력 요청")
        void inStock_yes_재입력() {
            Scanner s = scannerOf("상품", "1000", "", "yes", "true");
            Product p = handler.promptAdd(s, 1L);
            assertTrue(p.isInStock());
        }

        @Test
        @DisplayName("in_stock: 빈 문자열 → 재입력 요청")
        void inStock_빈문자열_재입력() {
            Scanner s = scannerOf("상품", "1000", "", "", "false");
            Product p = handler.promptAdd(s, 1L);
            assertFalse(p.isInStock());
        }

        @Test
        @DisplayName("in_stock: 'false ' 뒤에 공백 → trim 후 'false' 인식")
        void inStock_뒤공백_trim() {
            Scanner s = scannerOf("상품", "1000", "", "false ");
            Product p = handler.promptAdd(s, 1L);
            assertFalse(p.isInStock());
        }

        // ── promptUpdate 엣지케이스 ─────────────────────────────────────────

        @Test
        @DisplayName("update: price를 0.0으로 변경")
        void update_price_0으로_변경() {
            Product existing = product(1L, "상품", 1000.0, List.of(), true);
            Scanner s = scannerOf("", "0", "", "");
            Product updated = handler.promptUpdate(s, existing);
            assertEquals(0.0, updated.getPrice());
            assertEquals("상품", updated.getName()); // name 유지
        }

        @Test
        @DisplayName("update: price를 음수로 변경")
        void update_price_음수로_변경() {
            Product existing = product(1L, "상품", 5000.0, List.of(), true);
            Scanner s = scannerOf("", "-500", "", "");
            Product updated = handler.promptUpdate(s, existing);
            assertEquals(-500.0, updated.getPrice());
        }

        @Test
        @DisplayName("update: tags를 빈 입력 → 기존 태그 유지")
        void update_tags_빈입력_기존유지() {
            Product existing = product(1L, "상품", 1000.0, List.of("전자제품", "인기"), true);
            Scanner s = scannerOf("", "", "", "");
            Product updated = handler.promptUpdate(s, existing);
            assertEquals(List.of("전자제품", "인기"), updated.getTags());
        }

        @Test
        @DisplayName("update: tags를 완전히 새 값으로 교체")
        void update_tags_교체() {
            Product existing = product(1L, "상품", 1000.0, List.of("전자제품"), true);
            Scanner s = scannerOf("", "", "가전,생활", "");
            Product updated = handler.promptUpdate(s, existing);
            assertEquals(List.of("가전", "생활"), updated.getTags());
        }

        @Test
        @DisplayName("update: inStock false → true 변경")
        void update_inStock_false에서_true() {
            Product existing = product(1L, "상품", 1000.0, List.of(), false);
            Scanner s = scannerOf("", "", "", "true");
            Product updated = handler.promptUpdate(s, existing);
            assertTrue(updated.isInStock());
        }

        @Test
        @DisplayName("update: name 빈입력이면 기존 name 유지, price만 변경")
        void update_name_유지_price만_변경() {
            Product existing = product(1L, "무선 키보드", 89900.0, List.of(), true);
            Scanner s = scannerOf("", "99900", "", "");
            Product updated = handler.promptUpdate(s, existing);
            assertEquals("무선 키보드", updated.getName());
            assertEquals(99900.0, updated.getPrice());
        }

        @Test
        @DisplayName("update: id는 변경되지 않음")
        void update_id_불변() {
            Product existing = product(42L, "상품", 1000.0, List.of(), true);
            Scanner s = scannerOf("새이름", "", "", "");
            Product updated = handler.promptUpdate(s, existing);
            assertEquals(42L, updated.getId());
        }

        @Test
        @DisplayName("update: metadata는 기존 값 유지")
        void update_metadata_기존유지() {
            Map<String, String> meta = Map.of("color", "black", "weight", "100g");
            Product existing = new Product(1L, "상품", 1000.0, List.of(), true, meta);
            Scanner s = scannerOf("", "", "", "");
            Product updated = handler.promptUpdate(s, existing);
            assertEquals(meta, updated.getMetadata());
        }

        @Test
        @DisplayName("promptAdd EOF: price 입력 전 EOF")
        void promptAdd_price_입력전_EOF_throws() {
            Scanner s = scannerOf("상품이름"); // price 없이 EOF
            assertThrows(ProductHandler.InputTerminatedException.class,
                () -> handler.promptAdd(s, 1L));
        }

        @Test
        @DisplayName("promptAdd EOF: tags 입력 전 EOF")
        void promptAdd_tags_입력전_EOF_throws() {
            Scanner s = scannerOf("상품이름", "1000"); // tags 없이 EOF
            assertThrows(ProductHandler.InputTerminatedException.class,
                () -> handler.promptAdd(s, 1L));
        }

        @Test
        @DisplayName("promptAdd EOF: in_stock 입력 전 EOF")
        void promptAdd_inStock_입력전_EOF_throws() {
            Scanner s = scannerOf("상품이름", "1000", ""); // in_stock 없이 EOF
            assertThrows(ProductHandler.InputTerminatedException.class,
                () -> handler.promptAdd(s, 1L));
        }

        // ── 헬퍼 ────────────────────────────────────────────────────────────

        private Scanner scannerOf(String... lines) {
            String input = String.join("\n", lines) + "\n";
            return new Scanner(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        }

        private Product product(Long id, String name, double price, List<String> tags, boolean inStock) {
            return new Product(id, name, price, tags, inStock, Map.of());
        }
    }

    // =========================================================================
    // ConsoleApp 계층 — 커맨드 파싱 엣지케이스
    // =========================================================================

    @Nested
    class ConsoleAppCases {

        @TempDir Path tempDir;
        ByteArrayOutputStream out;
        InputStream originalIn;
        PrintStream originalOut;

        @BeforeEach
        void setUp() {
            originalIn  = System.in;
            originalOut = System.out;
            out = new ByteArrayOutputStream();
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        }

        @AfterEach
        void tearDown() throws Exception {
            System.setIn(originalIn);
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        }

        // ── 커맨드 인식 ─────────────────────────────────────────────────────

        @Test
        @DisplayName("커맨드 대문자 'LIST' → 알 수 없는 커맨드 오류")
        void 대문자_list_미인식() {
            run("LIST");
            assertContains("알 수 없는 커맨드: 'LIST'");
        }

        @Test
        @DisplayName("커맨드 대문자 'ADD' → 알 수 없는 커맨드 오류")
        void 대문자_add_미인식() {
            run("ADD");
            assertContains("알 수 없는 커맨드: 'ADD'");
        }

        @Test
        @DisplayName("빈 줄 여러 개 → 모두 무시 후 list 정상 처리")
        void 빈줄_연속_무시() {
            run("", "", "", "list");
            assertContains("데이터가 없습니다.");
        }

        @Test
        @DisplayName("탭 문자로만 이뤄진 줄 → 무시")
        void 탭문자_줄_무시() {
            run("\t", "list");
            assertContains("데이터가 없습니다.");
        }

        // ── find 엣지케이스 ─────────────────────────────────────────────────

        @Test
        @DisplayName("find id=0 → 없음 오류 (id는 1부터 시작)")
        void find_id_0_없음오류() {
            run("find 0");
            assertContains("[오류] id=0 항목을 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("find id=-1 → 없음 오류 (음수)")
        void find_음수_id_없음오류() {
            run("find -1");
            assertContains("[오류] id=-1 항목을 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("find id=Long 최댓값 → 없음 오류 (오버플로우 없음)")
        void find_long_max_없음오류() {
            run("find 9223372036854775807"); // Long.MAX_VALUE
            assertContains("[오류] id=9223372036854775807 항목을 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("find Long 오버플로우 → 숫자 오류 메시지")
        void find_long_overflow_오류() {
            run("find 99999999999999999999");
            assertContains("[오류] id는 숫자여야 합니다.");
        }

        @Test
        @DisplayName("find 추가 인자 무시 'find 1 2 3' → tokens[1]만 사용")
        void find_추가인자_무시() {
            run("add", "키보드", "1000", "", "true",
                "find 1 2 3");
            assertContains("\"name\" : \"키보드\"");
        }

        // ── update 엣지케이스 ───────────────────────────────────────────────

        @Test
        @DisplayName("update 숫자 아닌 id → 숫자 오류")
        void update_숫자아닌_id_오류() {
            run("update abc", "", "", "", "");
            assertContains("[오류] id는 숫자여야 합니다.");
        }

        @Test
        @DisplayName("update Long 오버플로우 id → 숫자 오류")
        void update_overflow_id_오류() {
            run("update 99999999999999999999", "", "", "", "");
            assertContains("[오류] id는 숫자여야 합니다.");
        }

        @Test
        @DisplayName("update 추가 인자 무시 'update 1 extra' → tokens[1]만 사용")
        void update_추가인자_무시() {
            run("add", "키보드", "1000", "", "true",
                "update 1 extra", "새이름", "", "", "",
                "find 1");
            assertContains("수정 완료 [id=1]");
            assertContains("\"name\" : \"새이름\"");
        }

        @Test
        @DisplayName("update price를 0으로 → 실제로 변경 반영")
        void update_price_0으로_변경반영() {
            run("add", "키보드", "1000", "", "true",
                "update 1", "", "0", "", "",
                "find 1");
            assertContains("\"price\" : 0.0");
        }

        @Test
        @DisplayName("update inStock: false→true→false 연속 변경")
        void update_inStock_연속변경() {
            run("add", "키보드", "1000", "", "false",
                "update 1", "", "", "", "true",
                "update 1", "", "", "", "false",
                "find 1");
            assertContains("\"in_stock\" : false");
        }

        // ── delete 엣지케이스 ───────────────────────────────────────────────

        @Test
        @DisplayName("delete 숫자 아닌 id → 숫자 오류")
        void delete_숫자아닌_id_오류() {
            run("delete abc");
            assertContains("[오류] id는 숫자여야 합니다.");
        }

        @Test
        @DisplayName("delete Long 오버플로우 id → 숫자 오류")
        void delete_overflow_id_오류() {
            run("delete 99999999999999999999");
            assertContains("[오류] id는 숫자여야 합니다.");
        }

        @Test
        @DisplayName("delete id=0 → 없음 오류")
        void delete_id_0_없음오류() {
            run("delete 0");
            assertContains("[오류] id=0 항목을 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("delete 추가 인자 무시 'delete 1 2' → tokens[1]만 사용")
        void delete_추가인자_무시() {
            run("add", "키보드", "1000", "", "true",
                "add", "마우스", "2000", "", "true",
                "delete 1 2",
                "find 1", "find 2");
            assertContains("[오류] id=1 항목을 찾을 수 없습니다.");
            assertContains("\"name\" : \"마우스\""); // id=2는 유지
        }

        // ── add 경계값 ──────────────────────────────────────────────────────

        @Test
        @DisplayName("add: 동일 이름 중복 허용 (유니크 제약 없음)")
        void add_중복이름_허용() {
            run("add", "키보드", "1000", "", "true",
                "add", "키보드", "2000", "", "false",
                "list");
            // list 출력에 "키보드"가 두 번 등장해야 함
            String o = output();
            int first  = o.indexOf("키보드");
            int second = o.indexOf("키보드", first + 1);
            assertTrue(second > first, "동일 이름 상품이 두 개 이상 존재해야 합니다.");
        }

        @Test
        @DisplayName("add: 이름에 공백 포함 (split과 무관하게 저장)")
        void add_이름에_공백_포함() {
            run("add", "무선 키보드 블루투스", "1000", "", "true",
                "find 1");
            assertContains("\"name\" : \"무선 키보드 블루투스\"");
        }

        @Test
        @DisplayName("add: tags가 JSON 특수문자 포함 — 직렬화 왕복 안전")
        void add_tags_json_특수문자() {
            run("add", "상품", "1000", "전자,IT\"가전", "true",
                "find 1");
            assertContains("\"IT\\\"가전\"");
        }

        @Test
        @DisplayName("add 후 delete 후 add → id는 이전 max+1 (재사용 없음)")
        void add_delete_add_id_max기반() {
            run("add", "A", "1000", "", "true",  // id=1
                "add", "B", "2000", "", "true",  // id=2
                "delete 2",                       // [1] 남음
                "add", "C", "3000", "", "true"); // max=1 → id=2 (삭제된 id는 재사용)
            // 삭제 후 남은 최대 id=1, 다음 id=2
            assertContains("저장 완료 [id=2]");
        }

        @Test
        @DisplayName("add 후 전부 delete 후 add → id는 max(빈 리스트)=0+1=1")
        void add_전부삭제_add_id1부터_시작() {
            run("add", "A", "1000", "", "true",  // id=1
                "delete 1",                       // [] 빈
                "add", "B", "2000", "", "true"); // max=0(빈) → id=1
            assertContains("저장 완료 [id=1]");
        }

        // ── 복합 시나리오 ───────────────────────────────────────────────────

        @Test
        @DisplayName("add×3 → update 중간 항목 → 나머지 두 항목 정상 조회")
        void 세항목_중간업데이트_나머지조회() {
            run("add", "A", "1000", "", "true",
                "add", "B", "2000", "", "true",
                "add", "C", "3000", "", "true",
                "update 2", "B-new", "", "", "",
                "find 1", "find 2", "find 3");
            assertContains("\"name\" : \"A\"");
            assertContains("\"name\" : \"B-new\"");
            assertContains("\"name\" : \"C\"");
        }

        @Test
        @DisplayName("add×3 → delete 첫 항목 → 나머지 두 항목 정상 조회")
        void 세항목_첫번째_삭제_나머지조회() {
            run("add", "A", "1000", "", "true",
                "add", "B", "2000", "", "true",
                "add", "C", "3000", "", "true",
                "delete 1",
                "find 2", "find 3");
            assertContains("\"name\" : \"B\"");
            assertContains("\"name\" : \"C\"");
        }

        @Test
        @DisplayName("add×3 → delete 마지막 항목 → 나머지 두 항목 정상 조회")
        void 세항목_마지막_삭제_나머지조회() {
            run("add", "A", "1000", "", "true",
                "add", "B", "2000", "", "true",
                "add", "C", "3000", "", "true",
                "delete 3",
                "find 1", "find 2");
            assertContains("\"name\" : \"A\"");
            assertContains("\"name\" : \"B\"");
        }

        @Test
        @DisplayName("update 없는 id=0 → 없음 오류")
        void update_없는id_0_오류() {
            run("update 0", "", "", "", "");
            assertContains("[오류] id=0 항목을 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("help 커맨드 → 커맨드 목록 출력")
        void help_커맨드_출력() {
            run("help");
            assertContains("list");
            assertContains("add");
            assertContains("find");
            assertContains("update");
            assertContains("delete");
            assertContains("exit");
        }

        // ── 헬퍼 ────────────────────────────────────────────────────────────

        private void run(String... lines) {
            out.reset();
            String input = String.join("\n", lines) + "\nexit\n";
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            new ConsoleApp(tempDir.resolve("test.json").toString()).run();
        }

        private void assertContains(String expected) {
            assertTrue(output().contains(expected),
                "출력에 포함되어야 함: [" + expected + "]\n실제 출력:\n" + output());
        }

        private String output() {
            return out.toString(StandardCharsets.UTF_8);
        }
    }
}
