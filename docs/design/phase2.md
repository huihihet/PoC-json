# Phase 2 설계: 조회 (R)

> PLAN: [PLAN.md](../PLAN.md) — Phase 2
> 선행: Phase 1 완료

---

## 목표

`list` — 전체 목록 테이블 출력  
`find <id>` — ID 기준 단건 pretty JSON 출력  
쓰기 기능 전에 출력 포맷을 이 Phase에서 확정한다.

---

## 신규 파일

```
src/main/java/org/example/app/
└── ProductHandler.java     # 출력 포맷 전담 (입력 프롬프트는 Phase 3에서 추가)
```

## 수정 파일

```
src/main/java/org/example/app/
└── ConsoleApp.java         # list / find 케이스 추가
```

---

## ProductHandler

### 역할

출력 포맷 전담 클래스. `ConsoleApp`이 보유하며 커맨드 처리 시 호출한다.

### printTable

```java
public void printTable(List<Product> products)
```

- 목록이 비어있으면 `"데이터가 없습니다."` 출력 후 반환
- 헤더 + 구분선 + 행 순서로 출력
- 컬럼 너비 고정 (좌측 정렬)

```
ID    NAME                  PRICE       IN_STOCK  TAGS
----  --------------------  ----------  --------  --------------------
1     무선 키보드           89900.0     true      [전자제품, 키보드]
2     게이밍 마우스         59900.0     false     [전자제품, 마우스]
```

| 컬럼 | 너비 |
|------|------|
| ID | 5 |
| NAME | 22 |
| PRICE | 12 |
| IN_STOCK | 10 |
| TAGS | 제한 없음 |

### printOne

```java
public void printOne(Product product)
```

- Jackson `ObjectMapper`(INDENT_OUTPUT)로 pretty JSON 출력
- `IOException` 은 `RuntimeException`으로 래핑 (호출부 단순화)

```json
{
  "id" : 1,
  "name" : "무선 키보드",
  "price" : 89900.0,
  "tags" : [ "전자제품", "키보드" ],
  "in_stock" : true,
  "metadata" : { }
}
```

---

## ConsoleApp 변경

### 필드 추가

```java
private final ProductHandler productHandler = new ProductHandler();
```

### dispatch() 케이스 추가

```java
case "list"   -> handleList();
case "find"   -> handleFind(tokens);
```

### handleList()

```java
private void handleList()
```

1. `storage.findAll()` 호출
2. `productHandler.printTable(products)`
3. `IOException` → `"[오류] 파일을 읽을 수 없습니다."` 출력

### handleFind()

```java
private void handleFind(String[] tokens)
```

1. `tokens` 길이 < 2 → `"사용법: find <id>"` 출력 후 반환
2. `Long.parseLong(tokens[1])` 실패 → `"[오류] id는 숫자여야 합니다."` 출력 후 반환
3. `storage.findAll()`에서 id 일치 항목 탐색
4. 없으면 `"[오류] id=N 항목을 찾을 수 없습니다."` (F-07)
5. 있으면 `productHandler.printOne(product)`

---

## 완료 기준

```
> list
데이터가 없습니다.

> find
사용법: find <id>

> find abc
[오류] id는 숫자여야 합니다.

> find 99
[오류] id=99 항목을 찾을 수 없습니다.
```

파일에 데이터가 있을 경우 (Phase 3 이후 연동 확인):

```
> list
ID    NAME                  PRICE       IN_STOCK  TAGS
----  --------------------  ----------  --------  --------------------
1     무선 키보드           89900.0     true      [전자제품, 키보드]

> find 1
{
  "id" : 1,
  "name" : "무선 키보드",
  ...
}
```

---

## 미결 사항

- `printTable` 의 TAGS 컬럼은 긴 값이 들어올 경우 줄바꿈 없이 그대로 출력 (단순성 우선)
- `metadata` 는 `printTable`에서 생략, `printOne`(JSON)에서만 노출
