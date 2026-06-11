# Phase 1 설계: 콘솔 루프 뼈대

> PLAN: [PLAN.md](../PLAN.md) — Phase 1

---

## 목표

앱이 실행되어 `> ` 프롬프트를 띄우고, 커맨드를 입력받아 처리하는 최소 골격.
`exit` 종료, `help` 안내, 미등록 커맨드 안내까지 동작하면 완료.

---

## 신규 파일

```
src/main/java/org/example/
├── app/
│   ├── ConsoleApp.java     # 메인 루프 & 커맨드 디스패치
│   └── CrudMain.java       # 콘솔 앱 전용 진입점
```

> 기존 `Main.java`는 수정하지 않는다.

---

## ConsoleApp

### 역할
- `Scanner(System.in)` 으로 라인 단위 입력 읽기
- 입력을 공백 기준으로 토큰 분리 → 첫 토큰이 커맨드, 나머지는 인자
- 커맨드별 분기 처리
- `JsonFileStorage<Product>` 보유 (이후 Phase에서 실제 사용)

### 생성자

```java
public ConsoleApp(String dataFilePath)
```

- `dataFilePath` 로 `JsonFileStorage<Product>` 초기화
- 기본값: `"data/products.json"`

### 메서드

```java
public void run()
```

- 시작 배너 출력: `JSON CRUD Console (type 'help' for commands)`
- 루프:
  1. `> ` 프롬프트 출력 (`print`, 줄바꿈 없음)
  2. 라인 읽기 — `null` 이면 (EOF, pipe 종료) 루프 탈출
  3. 빈 입력 → 무시, 재프롬프트
  4. 토큰 분리 → `dispatch(tokens)`

```java
private void dispatch(String[] tokens)
```

- `tokens[0]` 기준 switch:
  - `"exit"` → 루프 종료 플래그 설정
  - `"help"` → `printHelp()`
  - 그 외 → `"알 수 없는 커맨드: '<cmd>'. 'help'를 입력하면 명령어 목록을 볼 수 있습니다."`
- 이후 Phase에서 `list`, `add`, `find`, `update`, `delete` 케이스 추가

```java
private void printHelp()
```

```
사용 가능한 커맨드:
  list              전체 목록 출력
  add               새 항목 추가
  find <id>         ID로 단건 조회
  update <id>       ID로 수정
  delete <id>       ID로 삭제
  help              커맨드 목록
  exit              종료
```

---

## CrudMain

### 역할
- `ConsoleApp` 을 생성하고 `run()` 호출
- stdout UTF-8 고정 (기존 `Main.java`와 동일 방식)
- 첫 번째 인자(`args[0]`)가 있으면 저장 경로로 사용, 없으면 기본값

```java
public static void main(String[] args) {
    System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
    String path = args.length > 0 ? args[0] : "data/products.json";
    new ConsoleApp(path).run();
}
```

---

## build.gradle 변경

`application.mainClass` 를 `CrudMain` 으로 교체.
(기존 POC `Main` 은 `.\gradlew.bat run -PmainClass=org.example.Main` 으로 실행 가능하도록 유지)

```groovy
application {
    mainClass = 'org.example.app.CrudMain'
    applicationDefaultJvmArgs = ['-Dfile.encoding=UTF-8', '-Dstdout.encoding=UTF-8']
}
```

---

## 완료 기준

```
JSON CRUD Console (type 'help' for commands)
> asdf
알 수 없는 커맨드: 'asdf'. 'help'를 입력하면 명령어 목록을 볼 수 있습니다.
> help
사용 가능한 커맨드:
  list              전체 목록 출력
  add               새 항목 추가
  find <id>         ID로 단건 조회
  update <id>       ID로 수정
  delete <id>       ID로 삭제
  help              커맨드 목록
  exit              종료
> exit
```

---

## 미결 사항

- `data/` 디렉토리를 `.gitignore` 에 추가 (Phase 3 파일 저장 전에 처리)
