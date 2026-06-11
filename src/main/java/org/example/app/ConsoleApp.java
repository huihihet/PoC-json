package org.example.app;

import com.fasterxml.jackson.core.type.TypeReference;
import org.example.model.Product;
import org.example.storage.JsonFileStorage;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class ConsoleApp {

    private final JsonFileStorage<Product> storage;
    private final ProductHandler productHandler = new ProductHandler();
    private boolean running;

    public ConsoleApp(String dataFilePath) {
        this.storage = new JsonFileStorage<>(dataFilePath, new TypeReference<>() {});
    }

    public void run() {
        System.out.println("JSON CRUD Console (type 'help' for commands)");
        running = true;

        try (Scanner scanner = new Scanner(System.in)) {
            while (running) {
                System.out.print("> ");
                String line = scanner.hasNextLine() ? scanner.nextLine() : null;

                if (line == null) break;
                line = line.trim();
                if (line.isEmpty()) continue;

                dispatch(line.split("\\s+"), scanner);
            }
        }
    }

    private void dispatch(String[] tokens, Scanner scanner) {
        switch (tokens[0]) {
            case "exit"   -> running = false;
            case "help"   -> printHelp();
            case "list"   -> handleList();
            case "find"   -> handleFind(tokens);
            case "add"    -> handleAdd(scanner);
            default -> System.out.println("알 수 없는 커맨드: '" + tokens[0] + "'. 'help'를 입력하면 명령어 목록을 볼 수 있습니다.");
        }
    }

    private void printHelp() {
        System.out.println("""
                사용 가능한 커맨드:
                  list              전체 목록 출력
                  add               새 항목 추가
                  find <id>         ID로 단건 조회
                  update <id>       ID로 수정
                  delete <id>       ID로 삭제
                  help              커맨드 목록
                  exit              종료""");
    }

    private void handleList() {
        try {
            productHandler.printTable(storage.findAll());
        } catch (IOException e) {
            System.out.println("[오류] 파일을 읽을 수 없습니다.");
        }
    }

    private void handleFind(String[] tokens) {
        if (tokens.length < 2) {
            System.out.println("사용법: find <id>");
            return;
        }
        long id;
        try {
            id = Long.parseLong(tokens[1]);
        } catch (NumberFormatException e) {
            System.out.println("[오류] id는 숫자여야 합니다.");
            return;
        }
        try {
            List<Product> all = storage.findAll();
            all.stream()
               .filter(p -> id == p.getId())
               .findFirst()
               .ifPresentOrElse(
                   productHandler::printOne,
                   () -> System.out.println("[오류] id=" + id + " 항목을 찾을 수 없습니다.")
               );
        } catch (IOException e) {
            System.out.println("[오류] 파일을 읽을 수 없습니다.");
        }
    }

    private void handleAdd(Scanner scanner) {
        try {
            List<Product> all = storage.findAll();
            long nextId = all.stream().mapToLong(Product::getId).max().orElse(0L) + 1;
            Product product = productHandler.promptAdd(scanner, nextId);
            storage.append(product);
            System.out.println("저장 완료 [id=" + product.getId() + "]");
        } catch (IOException e) {
            System.out.println("[오류] 파일을 저장할 수 없습니다.");
        }
    }

    JsonFileStorage<Product> getStorage() {
        return storage;
    }
}
