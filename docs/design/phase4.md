# Phase 4 설계: 수정 / 삭제 (U, D)

> PLAN: [PLAN.md](../PLAN.md) — Phase 4
> 선행: Phase 3 완료

---

## 목표

`update <id>` — 기존값을 괄호로 보여주며 필드별 부분 수정  
`delete <id>` — ID 기준 항목 삭제

---

## 수정 파일

```
src/main/java/org/example/app/
├── ProductHandler.java     # promptUpdate() 추가
└── ConsoleApp.java         # update / delete 케이스 추가
```

---

## ProductHandler 변경

### promptUpdate

```java
public Product promptUpdate(Scanner scanner, Product existing)
```

- 각 필드에 기존값을 괄호로 표시, 빈 입력이면 기존값 유지
- `name` : 빈 입력 → 기존값 유지 (add와 달리 재입력 요청 없음)
- `price` : 빈 입력 → 기존값 유지 / 숫자 아닐 시 재입력 요청
- `tags` : 빈 입력 → 기존값 유지 / 값 있으면 새 리스트로 교체
- `in_stock` : 빈 입력 → 기존값 유지 / true·false 외 재입력 요청
- 기존 `id`, `metadata` 는 변경 없이 그대로 유지

### 프롬프트 형식

```
  name      (무선 키보드)        :
  price     (89900.0)           :
  tags      (전자제품,키보드)    :
  in_stock  (true)              : false
```

기존값은 `(값)` 형태로 필드명 뒤에 표시.  
tags 기존값은 `List.toString()` 대신 쉼표 구분 문자열로 표시 (`전자제품,키보드`).

---

## ConsoleApp 변경

### dispatch() 케이스 추가

```java
case "update" -> handleUpdate(tokens, scanner);
case "delete" -> handleDelete(tokens);
```

### handleUpdate

```java
private void handleUpdate(String[] tokens, Scanner scanner)
```

1. `tokens` 길이 < 2 → `"사용법: update <id>"` 출력 후 반환
2. id 파싱 실패 → `"[오류] id는 숫자여야 합니다."` 출력 후 반환
3. `storage.findAll()`에서 id 탐색 → 없으면 F-07 오류 메시지
4. `productHandler.promptUpdate(scanner, existing)` 호출
5. 전체 목록에서 해당 항목 교체 → `storage.saveAll(all)`
6. `"수정 완료 [id=N]"` 출력

### handleDelete

```java
private void handleDelete(String[] tokens)
```

1. `tokens` 길이 < 2 → `"사용법: delete <id>"` 출력 후 반환
2. id 파싱 실패 → `"[오류] id는 숫자여야 합니다."` 출력 후 반환
3. `storage.findAll()`에서 id 탐색 → 없으면 F-07 오류 메시지
4. 목록에서 해당 항목 제거 → `storage.saveAll(remaining)`
5. `"삭제 완료 [id=N]"` 출력

---

## 완료 기준

```
> update 1
  name      (무선 키보드)        :
  price     (89900.0)           :
  tags      (전자제품,키보드,무선):
  in_stock  (true)              : false
수정 완료 [id=1]

> find 1
{
  "id" : 1,
  "name" : "무선 키보드",
  "price" : 89900.0,
  "tags" : [ "전자제품", "키보드", "무선" ],
  "in_stock" : false,
  "metadata" : { }
}

> delete 2
삭제 완료 [id=2]

> list
ID    NAME                  PRICE       IN_STOCK  TAGS
----  --------------------  ----------  --------  --------------------
1     무선 키보드           89900.0     false     [전자제품, 키보드, 무선]

> update 99
[오류] id=99 항목을 찾을 수 없습니다.

> delete 99
[오류] id=99 항목을 찾을 수 없습니다.
```
