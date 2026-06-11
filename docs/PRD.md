# PRD: JSON CRUD 콘솔 앱

## 배경 및 목적

기존 JSON 파싱·저장 POC(`JsonFileStorage`, `Product`)를 기반으로,
실제로 데이터를 생성·조회·수정·삭제할 수 있는 커맨드 기반 콘솔 앱을 구현한다.
외부 DB 없이 JSON 파일만으로 데이터를 영속 관리하는 경량 도구.

---

## 범위

### In Scope

- 커맨드 입력 방식의 CRUD 인터페이스
- JSON 파일 영속 저장 (기존 `JsonFileStorage` 재사용)
- `Product` 엔티티 기준으로 구현 (신규 엔티티 추가 가능한 구조)
- ID 자동 채번 (저장 시 max id + 1)

### Out of Scope

- GUI / TUI
- 외부 DB 연동
- 네트워크 API
- 인증·권한

---

## 사용자 시나리오

```
$ ./gradlew run

JSON CRUD Console (type 'help' for commands)
> help

  list              전체 목록 출력
  add               새 항목 추가 (필드 입력 프롬프트)
  find <id>         ID로 단건 조회
  update <id>       ID로 수정 (변경할 필드만 입력, 빈 입력 시 기존값 유지)
  delete <id>       ID로 삭제
  help              명령어 목록
  exit              종료

> add
  name      : 게이밍 마우스
  price     : 59900
  tags      : 전자제품,마우스
  in_stock  : true
저장 완료 [id=1]

> list
ID  NAME            PRICE     IN_STOCK  TAGS
1   게이밍 마우스    59900.0   true      [전자제품, 마우스]

> find 1
{
  "id" : 1,
  "name" : "게이밍 마우스",
  ...
}

> update 1
  name      (게이밍 마우스) : 무선 게이밍 마우스
  price     (59900.0)      :
  tags      (전자제품,마우스):
  in_stock  (true)         : false
수정 완료 [id=1]

> delete 1
삭제 완료 [id=1]

> exit
```

---

## 기능 요구사항

| ID   | 요구사항                                                  | 우선순위 |
|------|-----------------------------------------------------------|----------|
| F-01 | `list` — 전체 목록을 테이블 형식으로 출력                 | Must     |
| F-02 | `add` — 필드별 입력 프롬프트로 신규 항목 생성             | Must     |
| F-03 | `find <id>` — ID 기준 단건 조회 (pretty JSON 출력)        | Must     |
| F-04 | `update <id>` — 빈 입력 시 기존값 유지하는 부분 수정      | Must     |
| F-05 | `delete <id>` — ID 기준 삭제                             | Must     |
| F-06 | ID 자동 채번 (max id + 1)                                 | Must     |
| F-07 | 존재하지 않는 ID 접근 시 오류 메시지 출력                 | Must     |
| F-08 | 잘못된 커맨드 입력 시 안내 메시지 출력                    | Must     |
| F-09 | 저장 경로 시작 시 설정 가능 (기본값: `data/products.json`)| Should   |

---

## 기술 설계

### 패키지 구조 (신규)

```
src/main/java/org/example/
├── app/
│   ├── ConsoleApp.java       # 메인 루프, 커맨드 파싱 & 디스패치
│   └── ProductHandler.java   # Product 전용 add/update 필드 입력 처리
└── Main.java                 # (기존 유지) — 추후 ConsoleApp 실행으로 교체 or 별도 진입점
```

> 기존 `model/`, `jackson/`, `gson/`, `storage/` 패키지는 수정하지 않는다.

### 데이터 흐름

```
사용자 입력
    └─▶ ConsoleApp (커맨드 파싱)
            └─▶ ProductHandler (필드 입력 / 출력 포맷)
                    └─▶ JsonFileStorage<Product> (파일 읽기/쓰기)
                            └─▶ data/products.json
```

### ID 채번 규칙

- `findAll()` 후 `stream().mapToLong(Product::getId).max()` + 1
- 목록이 비어있으면 id = 1

---

## 비기능 요구사항

- 콘솔 출력 인코딩: UTF-8 고정 (기존 방식 유지)
- 저장 파일: pretty-printed JSON (가독성)
- 오류 발생 시 스택트레이스 대신 사용자 친화적 메시지 출력
