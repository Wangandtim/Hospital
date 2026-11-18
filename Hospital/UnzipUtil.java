import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

public class UnzipUtil {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java UnzipUtil <zipFile> <outputDir>");
            System.exit(1);
        }
        
        String zipFilePath = args[0];
        String outputDir = args[1];
        
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = Paths.get(outputDir, entry.getName()).normalize();
                if (!entryPath.startsWith(Paths.get(outputDir).normalize())) {
                    throw new SecurityException("Zip Slip detected");
                }
                
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
            System.out.println("Unzip completed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}