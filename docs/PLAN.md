# PLAN: JSON CRUD 콘솔 앱 구현 계획

> PRD: [docs/PRD.md](PRD.md)
> 기존 코드(`model/`, `jackson/`, `gson/`, `storage/`)는 **수정 금지**. 신규 패키지 `app/`만 추가.

---

## Phase 1 — 콘솔 루프 뼈대

**목표:** 앱이 실행되어 커맨드를 입력받고 `exit`로 종료되는 최소 골격 완성.
이 Phase가 끝나면 다른 모든 Phase는 여기에 기능을 추가하는 방식으로 진행된다.

### 작업 목록

| # | 파일 | 내용 |
|---|------|------|
| 1-1 | `app/ConsoleApp.java` | `Scanner` 기반 입력 루프. `> ` 프롬프트 출력 → 라인 읽기 → 커맨드 토큰 분리 → 알 수 없는 커맨드면 안내 메시지 |
| 1-2 | `app/ConsoleApp.java` | `exit` 커맨드 처리 (루프 종료) |
| 1-3 | `app/ConsoleApp.java` | `help` 커맨드 처리 (커맨드 목록 출력) |
| 1-4 | `app/ConsoleApp.java` | `JsonFileStorage<Product>` 초기화 (저장 경로 `data/products.json`) |
| 1-5 | `app/CrudMain.java` | `ConsoleApp` 실행 전용 진입점 (기존 `Main.java` 무수정) |

### 완료 기준

```
> asdf
알 수 없는 커맨드: 'asdf'. 'help'를 입력하면 명령어 목록을 볼 수 있습니다.
> help
  list | add | find <id> | update <id> | delete <id> | help | exit
> exit
```

---

## Phase 2 — 조회 (R)

**목표:** 저장된 데이터를 읽는 두 커맨드(`list`, `find`) 구현.
쓰기 기능 전에 읽기를 먼저 확정해 출력 포맷을 기준으로 삼는다.

### 작업 목록

| # | 파일 | 내용 |
|---|------|------|
| 2-1 | `app/ProductHandler.java` | `printTable(List<Product>)` — 테이블 헤더·행 포맷 (ID / NAME / PRICE / IN_STOCK / TAGS) |
| 2-2 | `app/ProductHandler.java` | `printOne(Product)` — pretty JSON 출력 (Jackson ObjectMapper 재사용) |
| 2-3 | `app/ConsoleApp.java` | `list` 커맨드 → `storage.findAll()` → `productHandler.printTable()` |
| 2-4 | `app/ConsoleApp.java` | `find <id>` 커맨드 → ID 파싱 → 목록에서 탐색 → `printOne()` 또는 "없음" 메시지 (F-07) |

### 완료 기준

```
> list
데이터가 없습니다.

> find 99
[오류] id=99 항목을 찾을 수 없습니다.
```

---

## Phase 3 — 생성 (C)

**목표:** `add` 커맨드로 새 `Product`를 생성하고 JSON 파일에 저장.

### 작업 목록

| # | 파일 | 내용 |
|---|------|------|
| 3-1 | `app/ProductHandler.java` | `promptAdd(Scanner)` — 필드별 입력 프롬프트 (name / price / tags / in_stock) |
| 3-2 | `app/ProductHandler.java` | tags 입력: 쉼표 구분 문자열 → `List<String>` 변환 |
| 3-3 | `app/ProductHandler.java` | ID 채번: `findAll()` max id + 1, 빈 목록이면 1 |
| 3-4 | `app/ConsoleApp.java` | `add` 커맨드 → `productHandler.promptAdd()` → `storage.append()` → "저장 완료 [id=N]" |

### 완료 기준

```
> add
  name      : 키보드
  price     : 89900
  tags      : 전자제품,키보드
  in_stock  : true
저장 완료 [id=1]

> list
ID  NAME    PRICE    IN_STOCK  TAGS
1   키보드  89900.0  true      [전자제품, 키보드]
```

---

## Phase 4 — 수정 / 삭제 (U, D)

**목표:** `update`와 `delete` 커맨드 구현. Phase 2 조회 로직을 재사용한다.

### 작업 목록

| # | 파일 | 내용 |
|---|------|------|
| 4-1 | `app/ProductHandler.java` | `promptUpdate(Scanner, Product)` — 각 필드에 기존값을 괄호로 표시, 빈 입력이면 기존값 유지 |
| 4-2 | `app/ConsoleApp.java` | `update <id>` 커맨드 → ID로 기존 항목 탐색 → `promptUpdate()` → 전체 목록 교체 저장 → "수정 완료 [id=N]" |
| 4-3 | `app/ConsoleApp.java` | `delete <id>` 커맨드 → ID로 탐색 → 목록에서 제거 → 전체 저장 → "삭제 완료 [id=N]" |
| 4-4 | `app/ConsoleApp.java` | update / delete 모두 ID 미존재 시 F-07 오류 메시지 |

### 완료 기준

```
> update 1
  name      (키보드)              : 무선 키보드
  price     (89900.0)            :
  tags      (전자제품,키보드)     :
  in_stock  (true)               : false
수정 완료 [id=1]

> delete 1
삭제 완료 [id=1]

> delete 99
[오류] id=99 항목을 찾을 수 없습니다.
```

---

## Phase 5 — 예외 처리 & 입력 유효성

**목표:** 비정상 입력에도 앱이 죽지 않고 안내 메시지를 출력하는 방어 로직 추가.

### 작업 목록

| # | 파일 | 내용 |
|---|------|------|
| 5-1 | `app/ConsoleApp.java` | `find` / `update` / `delete`의 `<id>` 인자 누락 시 사용법 안내 |
| 5-2 | `app/ConsoleApp.java` | `<id>`가 숫자가 아닐 때 `NumberFormatException` → 안내 메시지 (스택트레이스 미출력) |
| 5-3 | `app/ProductHandler.java` | `price` 입력이 숫자가 아닐 때 재입력 요청 |
| 5-4 | `app/ProductHandler.java` | `in_stock` 입력이 true/false 외일 때 재입력 요청 |
| 5-5 | `app/ConsoleApp.java` | `IOException` 발생 시 "[오류] 파일을 읽을 수 없습니다." 출력 후 루프 유지 |

### 완료 기준

```
> find
사용법: find <id>

> find abc
[오류] id는 숫자여야 합니다.

> add
  name      : 키보드
  price     : abc
  [오류] 숫자를 입력하세요.
  price     : 89900
```

---

## Phase 6 — 테스트 & 마무리

**목표:** 핵심 로직 단위 테스트 추가, 저장 경로 설정 지원 (F-09), 최종 통합 검증.

### 작업 목록

| # | 파일 | 내용 |
|---|------|------|
| 6-1 | `test/.../app/ProductHandlerTest.java` | `promptAdd` / `promptUpdate` 출력 포맷 검증 (ByteArrayInputStream으로 입력 주입) |
| 6-2 | `test/.../app/ConsoleAppTest.java` | `list` / `find` / `delete` 커맨드 흐름 검증 (임시 파일 사용) |
| 6-3 | `app/CrudMain.java` | 첫 번째 인자로 저장 경로 지정 가능 (`args[0]` 없으면 기본값 사용) — F-09 |
| 6-4 | `CLAUDE.md` | CrudMain 실행법, 저장 경로 인자 설명 추가 |

### 완료 기준

- `.\gradlew.bat test` 전체 통과
- `.\gradlew.bat run` 으로 전체 CRUD 시나리오 수동 확인

---

## 구현 순서 요약

```
Phase 1 (뼈대)  →  Phase 2 (R)  →  Phase 3 (C)  →  Phase 4 (U/D)  →  Phase 5 (예외)  →  Phase 6 (테스트)
```

각 Phase는 완료 기준을 충족한 뒤 다음 Phase로 진행한다.
Phase 3 이후부터는 실제 파일 I/O가 발생하므로 `data/` 디렉토리가 `.gitignore`에 포함되어야 한다.

---

## 관련 파일

| 파일 | 역할 |
|------|------|
| `docs/PRD.md` | 기능 요구사항 원문 |
| `src/.../storage/JsonFileStorage.java` | 영속화 재사용 (수정 금지) |
| `src/.../model/Product.java` | 엔티티 재사용 (수정 금지) |
| `data/products.json` | 런타임 생성, git 제외 |
