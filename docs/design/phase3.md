# Phase 3 설계: 생성 (C)

> PLAN: [PLAN.md](../PLAN.md) — Phase 3
> 선행: Phase 2 완료

---

## 목표

`add` 커맨드로 필드를 하나씩 입력받아 새 `Product`를 생성하고 JSON 파일에 저장한다.

---

## 수정 파일

```
src/main/java/org/example/app/
├── ProductHandler.java     # promptAdd() 추가
└── ConsoleApp.java         # add 케이스 추가, handleAdd() 구현
```

---

## ProductHandler 변경

### promptAdd

```java
public Product promptAdd(Scanner scanner, long nextId)
```

- 필드별로 `"  필드명 : "` 프롬프트 출력 후 입력 대기
- `name` : 문자열, 빈 입력 허용하지 않음 (빈 값이면 재입력 요청)
- `price` : `Double.parseDouble` — 실패 시 `"[오류] 숫자를 입력하세요."` 후 재입력 (Phase 5 선제 적용)
- `tags` : 쉼표 구분 문자열 → `List<String>` (빈 입력 시 빈 리스트)
- `in_stock` : `"true"` / `"false"` 외 입력 시 재입력 요청 (Phase 5 선제 적용)
- `metadata` : 이 Phase에서는 입력받지 않고 빈 Map으로 고정
- 완성된 `Product` 반환 (`id = nextId`)

### 입력 흐름 예시

```
  name      : 무선 키보드
  price     : 89900
  tags      : 전자제품,키보드,무선
  in_stock  : true
```

---

## ConsoleApp 변경

### dispatch() 케이스 추가

```java
case "add" -> handleAdd(scanner);
```

> `scanner`를 `run()` 루프에서 `handleAdd`까지 전달한다.  
> `Scanner`를 여러 곳에서 생성하면 `System.in` 스트림 충돌 위험이 있으므로 루프에서 생성한 단일 인스턴스를 공유한다.

### dispatch 시그니처 변경

```java
// 변경 전
private void dispatch(String[] tokens)

// 변경 후
private void dispatch(String[] tokens, Scanner scanner)
```

`run()` 루프에서 `dispatch(line.split("\\s+"), scanner)` 로 호출.

### handleAdd

```java
private void handleAdd(Scanner scanner)
```

1. `storage.findAll()`로 현재 목록 조회
2. `nextId` = max id + 1 (빈 목록이면 1)
3. `productHandler.promptAdd(scanner, nextId)` 호출
4. `storage.append(product)`
5. `"저장 완료 [id=N]"` 출력
6. `IOException` → `"[오류] 파일을 저장할 수 없습니다."` 출력

---

## 완료 기준

```
> add
  name      : 무선 키보드
  price     : 89900
  tags      : 전자제품,키보드,무선
  in_stock  : true
저장 완료 [id=1]

> add
  name      : 게이밍 마우스
  price     : 59900
  tags      : 전자제품,마우스
  in_stock  : false
저장 완료 [id=2]

> list
ID    NAME                  PRICE       IN_STOCK  TAGS
----  --------------------  ----------  --------  --------------------
1     무선 키보드           89900.0     true      [전자제품, 키보드, 무선]
2     게이밍 마우스         59900.0     false     [전자제품, 마우스]

> find 1
{
  "id" : 1,
  "name" : "무선 키보드",
  "price" : 89900.0,
  "tags" : [ "전자제품", "키보드", "무선" ],
  "in_stock" : true,
  "metadata" : { }
}
```
