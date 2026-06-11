package org.example.app;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class CrudMain {

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        String path = args.length > 0 ? args[0] : "data/products.json";
        new ConsoleApp(path).run();
    }
}
