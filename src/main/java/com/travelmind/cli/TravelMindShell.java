package com.travelmind.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * TravelMind command line shell.
 */
@Component
@ConditionalOnProperty(name = "travelmind.cli.enabled", havingValue = "true", matchIfMissing = true)
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
        configureConsoleEncoding();
        cliRenderer.renderWelcome();

        try (Scanner scanner = new Scanner(System.in, consoleCharset())) {
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
        }

        cliRenderer.renderGoodbye();
    }

    private void configureConsoleEncoding() {
        Charset charset = consoleCharset();
        try {
            System.setOut(new PrintStream(System.out, true, charset));
            System.setErr(new PrintStream(System.err, true, charset));
        } catch (Exception e) {
            log.debug("Failed to configure console charset: {}", charset, e);
        }
    }

    private Charset consoleCharset() {
        String encoding = System.getProperty("native.encoding");
        if (encoding == null || encoding.isBlank()) {
            encoding = System.getProperty("sun.stdout.encoding");
        }
        if (encoding == null || encoding.isBlank()) {
            encoding = Charset.defaultCharset().name();
        }

        try {
            return Charset.forName(encoding);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }
}
