package com.hospital.console;

import java.util.List;
import java.util.Scanner;

public class PaginationUtils {
    
    private static final int DEFAULT_PAGE_SIZE = 10;
    
    public static <T> void paginate(List<T> list, Scanner scanner, ItemRenderer<T> renderer) {
        paginate(list, scanner, DEFAULT_PAGE_SIZE, renderer);
    }
    
    public static <T> void paginate(List<T> list, Scanner scanner, int pageSize, ItemRenderer<T> renderer) {
        if (list == null || list.isEmpty()) {
            System.out.println("  暂无数据");
            ConsoleUtils.waitEnter();
            return;
        }
        
        int totalItems = list.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        int currentPage = 1;
        
        while (true) {
            ConsoleUtils.clearScreen();
            renderer.renderHeader();
            
            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, totalItems);
            
            for (int i = start; i < end; i++) {
                renderer.renderItem(list.get(i), i + 1);
            }
            
            renderer.renderFooter();
            
            System.out.printf("\n[第 %d/%d 页，共 %d 条记录]\n", currentPage, totalPages, totalItems);
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("P. 上一页 (Previous)");
            System.out.println("N. 下一页 (Next)");
            System.out.println("G. 跳转到指定页 (Go)");
            System.out.println("Q. 返回 (Quit)");
            
            String choice = ConsoleUtils.readString(scanner, "请输入操作选项").toUpperCase();
            switch (choice) {
                case "P":
                    if (currentPage > 1) {
                        currentPage--;
                    } else {
                        ConsoleUtils.printWarning("已经是第一页");
                        ConsoleUtils.waitEnter();
                    }
                    break;
                case "N":
                    if (currentPage < totalPages) {
                        currentPage++;
                    } else {
                        ConsoleUtils.printWarning("已经是最后一页");
                        ConsoleUtils.waitEnter();
                    }
                    break;
                case "G":
                    int targetPage = ConsoleUtils.readInt(scanner, "请输入页码");
                    if (targetPage >= 1 && targetPage <= totalPages) {
                        currentPage = targetPage;
                    } else {
                        ConsoleUtils.printError("无效页码");
                        ConsoleUtils.waitEnter();
                    }
                    break;
                case "Q":
                    return;
                default:
                    ConsoleUtils.printError("无效选项，请输入 P/N/G/Q");
                    ConsoleUtils.waitEnter();
            }
        }
    }
    
    /**
     * 分页显示并支持选择
     * @return 选中的项，返回null表示用户取消选择
     */
    public static <T> T paginateWithSelection(List<T> list, Scanner scanner, int pageSize, ItemRenderer<T> renderer) {
        if (list == null || list.isEmpty()) {
            System.out.println("  暂无数据");
            ConsoleUtils.waitEnter();
            return null;
        }
        
        int totalItems = list.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        int currentPage = 1;
        
        while (true) {
            ConsoleUtils.clearScreen();
            renderer.renderHeader();
            
            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, totalItems);
            
            for (int i = start; i < end; i++) {
                renderer.renderItem(list.get(i), i + 1);
            }
            
            renderer.renderFooter();
            
            System.out.printf("\n[第 %d/%d 页，共 %d 条记录]\n", currentPage, totalPages, totalItems);
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("输入数字 1-" + totalItems + " 直接选择对应项");
            System.out.println("P. 上一页 (Previous)");
            System.out.println("N. 下一页 (Next)");
            System.out.println("G. 跳转到指定页 (Go)");
            System.out.println("Q. 返回 (Quit)");
            
            String choice = ConsoleUtils.readString(scanner, "请输入操作选项").toUpperCase();
            try {
                int numChoice = Integer.parseInt(choice);
                if (numChoice >= 1 && numChoice <= totalItems) {
                    return list.get(numChoice - 1);
                } else {
                    ConsoleUtils.printError("无效序号，请输入 1-" + totalItems + " 之间的数字");
                    ConsoleUtils.waitEnter();
                }
            } catch (NumberFormatException e) {
                switch (choice) {
                    case "P":
                        if (currentPage > 1) {
                            currentPage--;
                        } else {
                            ConsoleUtils.printWarning("已经是第一页");
                            ConsoleUtils.waitEnter();
                        }
                        break;
                    case "N":
                        if (currentPage < totalPages) {
                            currentPage++;
                        } else {
                            ConsoleUtils.printWarning("已经是最后一页");
                            ConsoleUtils.waitEnter();
                        }
                        break;
                    case "G":
                        int targetPage = ConsoleUtils.readInt(scanner, "请输入页码");
                        if (targetPage >= 1 && targetPage <= totalPages) {
                            currentPage = targetPage;
                        } else {
                            ConsoleUtils.printError("无效页码");
                            ConsoleUtils.waitEnter();
                        }
                        break;
                    case "Q":
                        return null;
                    default:
                        ConsoleUtils.printError("无效选项，请输入数字选择或 P/N/G/Q");
                        ConsoleUtils.waitEnter();
                }
            }
        }
    }
    
    public interface ItemRenderer<T> {
        void renderHeader();
        void renderItem(T item, int index);
        void renderFooter();
    }
}
