# Phase 6 설계: 테스트 & 마무리

> PLAN: [PLAN.md](../PLAN.md) — Phase 6
> 선행: Phase 5 완료

---

## 목표

핵심 로직 단위 테스트 작성, F-09(저장 경로 인자) 확인, CLAUDE.md 업데이트.

---

## 신규 파일

```
src/test/java/org/example/app/
├── ProductHandlerTest.java   # promptAdd / promptUpdate / EOF 검증
└── ConsoleAppTest.java       # 커맨드별 end-to-end 흐름 검증
```

---

## ProductHandlerTest

입력을 `ByteArrayInputStream` → `Scanner`로 주입해 실제 stdin 없이 테스트.

### 테스트 목록

| 테스트 | 검증 내용 |
|--------|-----------|
| `promptAdd_정상입력` | name·price·tags·inStock 모두 정상 반환 |
| `promptAdd_tags_빈입력` | tags 빈 입력 → 빈 리스트 |
| `promptAdd_price_재입력` | 첫 입력 "abc" → 오류 → 두 번째 입력 "89900" 성공 |
| `promptAdd_EOF` | name 입력 전 EOF → `InputTerminatedException` throw |
| `promptUpdate_전체_빈입력` | 모두 빈 입력 → 기존값 그대로 반환 |
| `promptUpdate_일부_변경` | name만 변경, 나머지 빈 입력 → name만 교체 |

### 입력 주입 헬퍼

```java
private Scanner scannerOf(String... lines) {
    String input = String.join("\n", lines) + "\n";
    return new Scanner(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
}
```

---

## ConsoleAppTest

임시 파일을 저장소로 사용하고, stdout을 `ByteArrayOutputStream`으로 캡처해 출력 검증.

### 테스트 목록

| 테스트 | 커맨드 흐름 | 검증 내용 |
|--------|-------------|-----------|
| `list_빈상태` | `list` | "데이터가 없습니다." 포함 |
| `add_후_list` | `add` → `list` | 저장된 항목 테이블 출력 |
| `add_후_find` | `add` → `find 1` | pretty JSON 출력, name 포함 |
| `find_인자누락` | `find` | "사용법: find <id>" 포함 |
| `find_숫자아님` | `find abc` | "[오류] id는 숫자여야 합니다." 포함 |
| `find_없는id` | `find 99` | "[오류] id=99 항목을 찾을 수 없습니다." 포함 |
| `update_무변경` | `add` → `update 1` (빈 입력) | "변경 사항이 없습니다." 포함 |
| `update_후_find` | `add` → `update 1` → `find 1` | 변경된 name 포함 |
| `delete_후_list` | `add` → `delete 1` → `list` | "삭제 완료 [id=1]" 후 "데이터가 없습니다." 포함 |
| `delete_없는id` | `delete 99` | "[오류] id=99 항목을 찾을 수 없습니다." 포함 |

### 테스트 픽스처

```java
@TempDir Path tempDir;
ConsoleApp app;
ByteArrayOutputStream out;

@BeforeEach
void setUp() {
    out = new ByteArrayOutputStream();
    System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
    app = new ConsoleApp(tempDir.resolve("test.json").toString());
}

@AfterEach
void tearDown() {
    System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
}

private void run(String... lines) {
    String input = String.join("\n", lines) + "\nexit\n";
    System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    app.run();
}

private String output() {
    return out.toString(StandardCharsets.UTF_8);
}
```

---

## F-09 확인

`CrudMain` 에 이미 구현됨:
```java
String path = args.length > 0 ? args[0] : "data/products.json";
```

별도 코드 추가 없이 CLAUDE.md에 실행법만 추가.

---

## CLAUDE.md 업데이트

`CrudMain` 실행법 섹션에 아래 내용 추가:

```markdown
## 콘솔 앱 실행

.\gradlew.bat run                          # 기본 경로 (data/products.json)
.\gradlew.bat run --args="my/path.json"    # 저장 경로 직접 지정
```

---

## 완료 기준

- `.\gradlew.bat test` 전체 통과 (기존 `JsonParsingTest` 포함)
- `.\gradlew.bat run` 으로 전체 CRUD 시나리오 수동 확인
