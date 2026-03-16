package org.springaicommunity;

import java.io.PrintStream;
import java.util.Scanner;
import org.springaicommunity.tool.confirmation.ConfirmationResult;
import org.springaicommunity.tool.confirmation.ConfirmationHandler;
import org.springaicommunity.tool.confirmation.ConfirmationRequest;

public class ConsoleConfirmationHandler implements ConfirmationHandler {

    private final PrintStream out;
    private final Scanner scanner;

    public ConsoleConfirmationHandler() {
        this(System.out, new Scanner(System.in));
    }

    public ConsoleConfirmationHandler(PrintStream out, Scanner scanner) {
        this.out = out;
        this.scanner = scanner;
    }

    @Override
    public ConfirmationResult confirm(ConfirmationRequest request) {
        out.printf("%n⚠️  Tool call requires confirmation%n");
        out.printf("   Tool  : %s%n", request.toolDefinition().name());
        out.printf("   Input : %s%n", request.toolInput());
        out.printf("Approve? (y/n): ");

        String response = scanner.nextLine().trim();

        return response.equalsIgnoreCase("y")
            ? ConfirmationResult.approved()
            : ConfirmationResult.rejected("User declined at console");
    }
}
