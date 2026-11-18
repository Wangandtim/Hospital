package com.hospital.console;

import java.util.Scanner;

public class ConsoleUtils {

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static void waitEnter() {
        System.out.println("\n按回车键继续...");
        new Scanner(System.in).nextLine();
    }

    public static int readInt(Scanner scanner, String prompt) {
        while (true) {
            try {
                System.out.print(prompt + "：");
                String input = scanner.nextLine().trim();
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("请输入有效的数字！");
            }
        }
    }

    public static String readString(Scanner scanner, String prompt) {
        System.out.print(prompt + "：");
        return scanner.nextLine().trim();
    }

    public static String readString(Scanner scanner, String prompt, String defaultValue) {
        System.out.print(prompt + "（默认：" + defaultValue + "）：");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }

    public static boolean confirm(Scanner scanner, String message) {
        System.out.print(message + "（Y/N）：");
        String input = scanner.nextLine().trim().toUpperCase();
        return input.equals("Y") || input.equals("YES");
    }

    public static void printSuccess(String message) {
        System.out.println(" ✓ " + message);
    }

    public static void printError(String message) {
        System.out.println(" ✗ " + message);
    }

    public static void printWarning(String message) {
        System.out.println("⚠ " + message);
    }

    public static void printInfo(String message) {
        System.out.println("ℹ " + message);
    }

    public static int readInt(Scanner scanner, String prompt, int defaultValue) {
        System.out.print(prompt + "（默认：" + defaultValue + "）：");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("请输入有效的数字，使用默认值：" + defaultValue);
            return defaultValue;
        }
    }

    public static void printDivider() {
        System.out.println("─".repeat(50));
    }

    public static void printTitle(String title) {
        System.out.println("╔" + "═".repeat(50) + "╗");
        System.out.println("║ " + title + " ".repeat(50 - title.length() - 2) + "║");
        System.out.println("╚" + "═".repeat(50) + "╝");
    }
}