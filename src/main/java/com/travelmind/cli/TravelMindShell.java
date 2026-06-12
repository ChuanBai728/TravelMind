package com.travelmind.cli;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * TravelMind 命令行 Shell
 */
@Component
public class TravelMindShell implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TravelMindShell.class);

    private final CommandRouter commandRouter;
    private final CliRenderer cliRenderer;

    public TravelMindShell(CommandRouter commandRouter, CliRenderer cliRenderer) {
        this.commandRouter = commandRouter;
        this.cliRenderer = cliRenderer;
    }

    @Override
    public void run(String... args) {
        cliRenderer.renderWelcome();

        try {
            // 创建 JLine Terminal
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();

            LineReader lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .parser(new DefaultParser())
                    .build();

            // 主循环
            boolean running = true;
            while (running) {
                try {
                    String line = lineReader.readLine("TravelMind > ");

                    if (line == null) {
                        // Ctrl+D 或 EOF
                        break;
                    }

                    if (line.isBlank()) {
                        continue;
                    }

                    running = commandRouter.handle(line.trim());
                } catch (Exception e) {
                    log.error("Error processing input", e);
                    cliRenderer.renderError("处理输入时出错: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Shell initialization failed", e);
            cliRenderer.renderError("Shell 初始化失败: " + e.getMessage());

            // 降级到简单输入模式
            runSimpleMode();
        }

        cliRenderer.renderGoodbye();
    }

    /**
     * 简单输入模式（降级方案）
     */
    private void runSimpleMode() {
        java.util.Scanner scanner = new java.util.Scanner(System.in);

        boolean running = true;
        while (running) {
            try {
                System.out.print("TravelMind > ");
                if (!scanner.hasNextLine()) {
                    break;
                }

                String line = scanner.nextLine();
                if (line == null || line.isBlank()) {
                    continue;
                }

                running = commandRouter.handle(line.trim());
            } catch (Exception e) {
                log.error("Error processing input", e);
                cliRenderer.renderError("处理输入时出错: " + e.getMessage());
            }
        }

        scanner.close();
    }
}
