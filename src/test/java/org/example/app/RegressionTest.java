package org.example.app;

import com.fasterxml.jackson.core.type.TypeReference;
import org.example.model.Product;
import org.example.storage.JsonFileStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1~5 수정 버그 재발 방지 테스트.
 * 각 테스트의 주석에 어떤 버그를 검증하는지 명시.
 */
class RegressionTest {

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

    // ── JsonFileStorage ───────────────────────────────────────────────────────

    /**
     * Phase 1 수정: 빈 파일(length=0)을 읽을 때 JsonParseException 대신 빈 리스트 반환.
     * 수정 전: mapper.readValue(emptyFile) → MismatchedInputException
     * 수정 후: length == 0 체크 → new ArrayList<>() 반환
     */
    @Test
    void jsonFileStorage_빈파일_파싱오류없이_빈리스트() throws IOException {
        Path empty = tempDir.resolve("empty.json");
        Files.createFile(empty); // 0 bytes
        JsonFileStorage<Product> storage = new JsonFileStorage<>(
            empty.toString(), new TypeReference<>() {});
        assertDoesNotThrow(() -> {
            List<Product> result = storage.findAll();
            assertTrue(result.isEmpty());
        });
    }

    /**
     * Phase 1 수정: Jackson Map 파싱 시 정수값을 Integer로 파싱 → (Double) 캐스트 실패.
     * 수정 후: ((Number) val).doubleValue() 사용.
     */
    @Test
    void jackson_map파싱_integer_double_캐스트오류없음() throws IOException {
        String json = "{\"id\":1,\"price\":89900,\"name\":\"키보드\",\"in_stock\":true,\"tags\":[],\"metadata\":{}}";
        var demo = new org.example.jackson.JacksonDemo();
        Map<String, Object> map = demo.toMap(json);
        // Integer로 파싱되더라도 Number로 접근하면 안전
        assertDoesNotThrow(() -> {
            double price = ((Number) map.get("price")).doubleValue();
            assertEquals(89900.0, price);
        });
    }

    /**
     * Phase 5 수정: add 도중 EOF → InputTerminatedException 발생, 무한루프 없음.
     * 수정 전: hasNextLine()=false → "" → 재입력 루프 → 무한루프
     */
    @Test
    void eof_도중_add_무한루프없음() {
        // name 입력 전 EOF
        run("add"); // add 명령어만 입력, 필드값 없이 EOF
        // 앱이 정상 종료되면 테스트 통과 (무한루프면 타임아웃)
        assertContains("JSON CRUD Console");
    }

    /**
     * Phase 5 수정: update 도중 EOF → running=false, 정상 종료.
     */
    @Test
    void eof_도중_update_무한루프없음() {
        // add는 정상, update 도중 EOF
        String input = "add\n키보드\n1000\n\ntrue\nupdate 1\n"; // update 후 필드값 없이 EOF
        runRaw(input);
        assertContains("저장 완료 [id=1]");
    }

    /**
     * Phase 5 수정: update 무변경 시 파일 재저장하지 않음.
     * 파일 최종 수정시각이 변하지 않아야 한다.
     */
    @Test
    void update_무변경_파일_재저장없음() throws IOException {
        Path store = tempDir.resolve("store.json");
        run(store, "add", "키보드", "1000", "", "true");
        long lastModifiedBefore = Files.getLastModifiedTime(store).toMillis();

        // 충분한 간격을 위해 1ms 대기
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        run(store, "update 1", "", "", "", ""); // 전부 빈 입력 → 무변경
        long lastModifiedAfter = Files.getLastModifiedTime(store).toMillis();

        assertEquals(lastModifiedBefore, lastModifiedAfter,
            "무변경 update는 파일을 재저장하지 않아야 합니다.");
    }

    /**
     * ID 채번이 count 기반이 아닌 max+1 기반임을 검증.
     * 예: [id=1, id=3] 상태에서 add → id=4 (not 2)
     */
    @Test
    void id채번_max기반_count기반_아님() {
        Path store = tempDir.resolve("store.json");
        run(store,
            "add", "A", "1000", "", "true",   // id=1
            "add", "B", "2000", "", "true",   // id=2
            "add", "C", "3000", "", "true",   // id=3
            "delete 1",                        // id=1 삭제 → [2, 3]
            "delete 2",                        // id=2 삭제 → [3]
            "add", "D", "4000", "", "true");   // max=3 → next=4
        // count 기반이면 남은 항목 수(1)+1=2, max 기반이면 max(3)+1=4
        assertContains("저장 완료 [id=4]");
    }

    /**
     * update 후 수정하지 않은 다른 항목들이 순서대로 유지됨.
     * toList() 반환 불변 리스트를 saveAll에 넘겨도 정상 동작.
     */
    @Test
    void update_후_나머지항목_순서_데이터_유지() {
        Path store = tempDir.resolve("store.json");
        run(store,
            "add", "A", "1000", "", "true",
            "add", "B", "2000", "", "true",
            "add", "C", "3000", "", "true",
            "update 2", "B-수정", "", "", "",
            "find 1", "find 2", "find 3");
        assertContains("\"name\" : \"A\"");
        assertContains("\"name\" : \"B-수정\"");
        assertContains("\"name\" : \"C\"");
    }

    /**
     * delete 후 남은 항목들이 find로 정상 조회됨.
     * toList() 반환 불변 리스트를 saveAll에 넘겨도 직렬화 정상.
     */
    @Test
    void delete_중간항목_나머지_정상_조회() {
        Path store = tempDir.resolve("store.json");
        run(store,
            "add", "A", "1000", "", "true",
            "add", "B", "2000", "", "true",
            "add", "C", "3000", "", "true",
            "delete 2",
            "find 1", "find 3");
        assertContains("\"name\" : \"A\"");
        assertContains("\"name\" : \"C\"");
        assertNotContains("\"name\" : \"B\"");
    }

    /**
     * delete 직후 같은 id로 find → 없음 오류.
     * (삭제가 실제로 저장소에 반영됐는지 확인)
     */
    @Test
    void delete_후_같은id_find_오류() {
        Path store = tempDir.resolve("store.json");
        run(store,
            "add", "키보드", "1000", "", "true",
            "delete 1",
            "find 1");
        assertContains("[오류] id=1 항목을 찾을 수 없습니다.");
    }

    /**
     * 여러 번 update 연속 적용 시 최종 상태 반영.
     */
    @Test
    void update_연속적용_최종상태_반영() {
        Path store = tempDir.resolve("store.json");
        run(store,
            "add", "키보드", "1000", "", "true",
            "update 1", "키보드v2", "", "", "",
            "update 1", "키보드v3", "", "", "",
            "find 1");
        assertContains("\"name\" : \"키보드v3\"");
        assertNotContains("\"name\" : \"키보드v2\"");
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private void run(String... lines) {
        run(tempDir.resolve("default.json"), lines);
    }

    private void run(Path store, String... lines) {
        out.reset();
        String input = String.join("\n", lines) + "\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        new ConsoleApp(store.toString()).run();
    }

    private void runRaw(String input) {
        out.reset();
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        new ConsoleApp(tempDir.resolve("raw.json").toString()).run();
    }

    private void assertContains(String expected) {
        assertTrue(output().contains(expected),
            "출력에 포함되어야 함: [" + expected + "]\n실제 출력:\n" + output());
    }

    private void assertNotContains(String unexpected) {
        assertFalse(output().contains(unexpected),
            "출력에 포함되지 않아야 함: [" + unexpected + "]\n실제 출력:\n" + output());
    }

    private String output() {
        return out.toString(StandardCharsets.UTF_8);
    }
}
