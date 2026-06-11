package org.example.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleAppTest {

    @TempDir Path tempDir;

    ConsoleApp app;
    ByteArrayOutputStream out;
    InputStream originalIn;
    PrintStream originalOut;

    @BeforeEach
    void setUp() {
        originalIn  = System.in;
        originalOut = System.out;
        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        app = new ConsoleApp(tempDir.resolve("test.json").toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setIn(originalIn);
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
    }

    // ── list ───────────────────────────────────────────────

    @Test
    void list_빈상태() {
        run("list");
        assertContains("데이터가 없습니다.");
    }

    @Test
    void list_항목있음() {
        run("add", "키보드", "89900", "전자제품", "true",
            "list");
        assertContains("키보드");
        assertContains("89900.0");
    }

    // ── add ────────────────────────────────────────────────

    @Test
    void add_저장완료() {
        run("add", "게이밍 마우스", "59900", "전자제품,마우스", "false");
        assertContains("저장 완료 [id=1]");
    }

    @Test
    void add_id_자동증가() {
        run("add", "키보드", "89900", "", "true",
            "add", "마우스", "59900", "", "false");
        assertContains("저장 완료 [id=1]");
        assertContains("저장 완료 [id=2]");
    }

    // ── find ───────────────────────────────────────────────

    @Test
    void find_정상조회() {
        run("add", "무선 키보드", "89900", "전자제품", "true",
            "find 1");
        assertContains("\"name\" : \"무선 키보드\"");
    }

    @Test
    void find_인자누락() {
        run("find");
        assertContains("사용법: find <id>");
    }

    @Test
    void find_숫자아님() {
        run("find abc");
        assertContains("[오류] id는 숫자여야 합니다.");
    }

    @Test
    void find_없는id() {
        run("find 99");
        assertContains("[오류] id=99 항목을 찾을 수 없습니다.");
    }

    // ── update ─────────────────────────────────────────────

    @Test
    void update_무변경() {
        run("add", "키보드", "89900", "", "true",
            "update 1", "", "", "", "");
        assertContains("변경 사항이 없습니다.");
    }

    @Test
    void update_후_변경반영() {
        run("add", "키보드", "89900", "전자제품", "true",
            "update 1", "유선 키보드", "", "", "false",
            "find 1");
        assertContains("\"name\" : \"유선 키보드\"");
        assertContains("\"in_stock\" : false");
    }

    @Test
    void update_인자누락() {
        run("update");
        assertContains("사용법: update <id>");
    }

    @Test
    void update_없는id() {
        run("update 99", "", "", "", "");
        assertContains("[오류] id=99 항목을 찾을 수 없습니다.");
    }

    // ── delete ─────────────────────────────────────────────

    @Test
    void delete_후_목록_비어있음() {
        run("add", "키보드", "89900", "", "true",
            "delete 1",
            "list");
        assertContains("삭제 완료 [id=1]");
        assertContains("데이터가 없습니다.");
    }

    @Test
    void delete_인자누락() {
        run("delete");
        assertContains("사용법: delete <id>");
    }

    @Test
    void delete_없는id() {
        run("delete 99");
        assertContains("[오류] id=99 항목을 찾을 수 없습니다.");
    }

    // ── 기타 ───────────────────────────────────────────────

    @Test
    void 알수없는_커맨드() {
        run("unknown");
        assertContains("알 수 없는 커맨드: 'unknown'");
    }

    // ── 헬퍼 ───────────────────────────────────────────────

    private void run(String... lines) {
        // add 입력은 필드값을 커맨드 사이에 끼워 넣음 — exit는 자동 추가
        String input = String.join("\n", lines) + "\nexit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        app = new ConsoleApp(tempDir.resolve("test.json").toString());
        app.run();
    }

    private void assertContains(String expected) {
        assertTrue(output().contains(expected),
            "출력에 포함되어야 함: [" + expected + "]\n실제 출력:\n" + output());
    }

    private String output() {
        return out.toString(StandardCharsets.UTF_8);
    }
}
