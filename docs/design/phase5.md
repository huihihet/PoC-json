# Phase 5 설계: 예외 처리 & 입력 유효성 보완

> PLAN: [PLAN.md](../PLAN.md) — Phase 5
> 선행: Phase 4 완료

---

## 선제 완료 확인

Phase 3/4에서 이미 처리된 항목 (추가 구현 불필요):

| 항목 | 처리 위치 |
|------|-----------|
| `find` 인자 누락 → `"사용법: find <id>"` | Phase 2 |
| `update` / `delete` 인자 누락 → 사용법 안내 | Phase 4 |
| `<id>` 숫자 아닐 때 오류 메시지 | Phase 2, 4 |
| `price` 숫자 아닐 때 재입력 | Phase 3 |
| `in_stock` 값 오류 시 재입력 | Phase 3 |
| `IOException` → 안내 메시지, 루프 유지 | Phase 2, 3, 4 |
| `name` 빈 입력 재요청 (`add`) | Phase 3 |

---

## 실제 보완 항목

### 보완 1: EOF 무한루프 버그 수정 (버그)

**현상:** `add` / `update` 도중 stdin이 닫히면 (`Ctrl+D`, 파이프 종료 등) read 헬퍼의 `while(true)` 루프가 무한 반복.

**원인:** 모든 read 헬퍼가 `scanner.hasNextLine() ? scanner.nextLine() : ""` 패턴을 사용하는데,  
EOF 이후 `hasNextLine()` = false → value = "" → 오류/재입력 메시지 → 다시 루프 → **무한루프**.

**해결:**

`ProductHandler` 내부에 package-private 예외 클래스 추가:

```java
static class InputTerminatedException extends RuntimeException {}
```

모든 read 헬퍼의 while(true) 루프 상단에 EOF 체크 추가:

```java
if (!scanner.hasNextLine()) throw new InputTerminatedException();
```

`ConsoleApp.handleAdd` / `handleUpdate` 에서 catch:

```java
} catch (ProductHandler.InputTerminatedException e) {
    running = false;
}
```

`run()` 루프는 `running = false`가 되어 자연스럽게 종료.

---

### 보완 2: `update` 무변경 감지 (UX)

**현상:** 모든 필드에 빈 입력만 해도 `"수정 완료 [id=N]"` 출력 및 파일 재저장.

**해결:** `promptUpdate` 반환값과 `existing` 을 필드별로 비교해 변경이 없으면  
`"수정 완료 [id=N]"` 대신 `"변경 사항이 없습니다."` 출력하고 파일 저장 생략.

변경 감지 위치: `ConsoleApp.handleUpdate` — `promptUpdate` 호출 후, `saveAll` 호출 전.

```java
Product updated = productHandler.promptUpdate(scanner, existing);
if (isUnchanged(existing, updated)) {
    System.out.println("변경 사항이 없습니다.");
    return;
}
storage.saveAll(saved);
System.out.println("수정 완료 [id=" + id + "]");
```

```java
private boolean isUnchanged(Product a, Product b) {
    return Objects.equals(a.getName(),     b.getName())
        && a.getPrice()    == b.getPrice()
        && a.isInStock()   == b.isInStock()
        && Objects.equals(a.getTags(),     b.getTags());
}
```

---

## 수정 파일

```
src/main/java/org/example/app/
├── ProductHandler.java     # InputTerminatedException 추가, read 헬퍼 EOF 체크
└── ConsoleApp.java         # handleAdd/handleUpdate EOF catch, isUnchanged 추가
```

---

## 완료 기준

**EOF 처리:**
```
> add
  name      : [Ctrl+D / 파이프 종료]
              → 무한루프 없이 앱 정상 종료
```

**무변경 감지:**
```
> update 1
  name      (무선 키보드) :
  price     (89900.0)    :
  tags      (전자제품)   :
  in_stock  (true)       :
변경 사항이 없습니다.

> update 1
  name      (무선 키보드) : 유선 키보드
  ...
수정 완료 [id=1]
```
