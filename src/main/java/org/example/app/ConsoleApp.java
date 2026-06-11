package org.example.app;

import com.fasterxml.jackson.core.type.TypeReference;
import org.example.model.Product;
import org.example.storage.JsonFileStorage;

import java.util.Scanner;

public class ConsoleApp {

    private final JsonFileStorage<Product> storage;
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

                dispatch(line.split("\\s+"));
            }
        }
    }

    private void dispatch(String[] tokens) {
        switch (tokens[0]) {
            case "exit" -> running = false;
            case "help" -> printHelp();
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

    JsonFileStorage<Product> getStorage() {
        return storage;
    }
}
