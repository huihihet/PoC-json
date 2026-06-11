# CLAUDE.md

## 프로젝트 개요

Jackson / Gson 기반 JSON 파싱 및 파일 저장 POC 프로젝트.
커맨드 기반 CRUD 콘솔 앱 구현 완료 (Phase 1~6).

## 기술 스택

- Java 26 (OpenJDK 26.0.1 — `C:\Users\User\.jdks\openjdk-26.0.1`)
- Gradle 9.5 (Wrapper 사용)
- Jackson Databind 2.18.2
- Gson 2.11.0
- JUnit Jupiter 6.0.0

## 빌드 / 실행

```powershell
# 환경변수 (새 터미널에서 자동 적용됨)
$env:JAVA_HOME = "C:\Users\User\.jdks\openjdk-26.0.1"
$env:PATH = "C:\Users\User\.jdks\openjdk-26.0.1\bin;$env:PATH"

.\gradlew.bat test                         # 전체 테스트
.\gradlew.bat run                          # CRUD 콘솔 앱 실행 (기본 저장 경로: data/products.json)
.\gradlew.bat run --args="my/path.json"   # 저장 경로 직접 지정
.\gradlew.bat run -PmainClass=org.example.Main  # 기존 POC 데모 실행
```

## 코드 구조

```
src/main/java/org/example/
├── model/
│   └── Product.java              # 파싱 대상 POJO (id, name, price, tags, inStock, metadata)
├── jackson/
│   └── JacksonDemo.java          # Jackson 파싱·직렬화·파일 I/O 데모
├── gson/
│   └── GsonDemo.java             # Gson 파싱·직렬화·파일 I/O 데모
├── storage/
│   └── JsonFileStorage.java      # JSON 파일 영속화 제네릭 유틸 (findAll/saveAll/append/findBy)
└── Main.java                     # POC 전체 실행 진입점

src/main/resources/
└── sample.json                   # 단일 Product 샘플 데이터

src/main/java/org/example/app/
├── CrudMain.java                 # CRUD 콘솔 앱 진입점 (args[0]으로 저장 경로 지정 가능)
├── ConsoleApp.java               # 메인 루프, 커맨드 디스패치 (list/add/find/update/delete)
└── ProductHandler.java           # 출력 포맷, 입력 프롬프트, 유효성 검사

src/main/resources/
└── sample.json                   # 단일 Product 샘플 데이터

src/test/java/org/example/
├── JsonParsingTest.java          # Jackson / Gson / JsonFileStorage 단위 테스트
└── app/
    ├── ProductHandlerTest.java   # promptAdd / promptUpdate / EOF 검증
    └── ConsoleAppTest.java       # 커맨드별 end-to-end 흐름 검증
```

## 주요 설계 결정

- `JsonFileStorage<T>` : TypeReference를 생성자에서 받아 제네릭 타입 안전성 유지.
  파일이 없거나 비어있으면 빈 리스트 반환 (NPE / parse error 방지).
- `Product.inStock` : JSON 키가 `in_stock` (snake_case) 이므로 `@JsonProperty("in_stock")` 적용.
  Gson은 `FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES` 전역 정책으로 동일하게 처리.
- `Main.java` / `CrudMain.java` : stdout을 UTF-8 PrintStream으로 교체해 Windows 콘솔/IDE 한글 깨짐 방지.
- `ProductHandler.InputTerminatedException` : stdin EOF 시 read 헬퍼 while 루프 탈출용. `ConsoleApp`에서 catch해 `running=false` 처리.
- `ConsoleApp.isUnchanged()` : update 시 모든 필드가 동일하면 파일 저장 생략.

## 코딩 컨벤션

- 주석은 WHY가 명확히 필요한 경우만 작성 (WHAT 설명 금지).
- 신규 엔티티 추가 시 `model/` 에 POJO 생성 후 `storage/JsonFileStorage` 재사용.
- 기존 POC 클래스(`JacksonDemo`, `GsonDemo`, `Main`)는 수정하지 않고 신규 패키지로 확장.
