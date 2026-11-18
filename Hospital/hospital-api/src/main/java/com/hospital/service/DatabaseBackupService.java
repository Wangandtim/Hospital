package com.hospital.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 每日自动 mysqldump 备份，并在启动时补齐当天备份。 */
@Service
public class DatabaseBackupService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Pattern JDBC_PATTERN =
            Pattern.compile("jdbc:mysql://([^:/]+):(\\d+)/([^?]+)");

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @Value("${hospital.backup.enabled:true}")
    private boolean enabled;

    @Value("${hospital.backup.dir:./backups}")
    private String backupDir;

    @Value("${hospital.backup.mysqldump:mysqldump}")
    private String mysqldumpPath;

    @Value("${hospital.backup.retain-days:14}")
    private int retainDays;

    @Autowired
    private EmergencySnapshotService emergencySnapshotService;

    @PostConstruct
    public void backupOnStartupIfMissing() {
        if (!enabled) {
            return;
        }
        Thread thread = new Thread(() -> {
            try {
                File todayFile = new File(backupDir, buildFileName());
                if (!todayFile.exists()) {
                    runBackup();
                }
                emergencySnapshotService.cleanup(30);
            } catch (Exception e) {
                System.out.println("启动备份失败: " + e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledBackup() {
        if (!enabled) {
            return;
        }
        try {
            runBackup();
            emergencySnapshotService.cleanup(30);
        } catch (Exception e) {
            System.out.println("定时备份失败: " + e.getMessage());
        }
    }

    public synchronized File runBackup() throws IOException, InterruptedException {
        Matcher matcher = JDBC_PATTERN.matcher(jdbcUrl);
        if (!matcher.find()) {
            throw new IOException("无法解析数据库连接地址");
        }
        String host = matcher.group(1);
        String port = matcher.group(2);
        String database = matcher.group(3);

        File dir = new File(backupDir).getAbsoluteFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("无法创建备份目录: " + dir);
        }

        File target = new File(dir, buildFileName());

        List<String> command = new ArrayList<>();
        command.add(mysqldumpPath);
        command.add("--host=" + host);
        command.add("--port=" + port);
        command.add("-u" + username);
        command.add("--single-transaction");
        command.add("--quick");
        command.add("--routines");
        command.add("--triggers");
        command.add("--default-character-set=utf8mb4");
        command.add(database);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().put("MYSQL_PWD", password == null ? "" : password);
        builder.redirectErrorStream(true);
        builder.redirectOutput(target);

        Process process = builder.start();
        boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            target.delete();
            throw new IOException("备份超时");
        }
        if (process.exitValue() != 0) {
            target.delete();
            throw new IOException("mysqldump 退出码: " + process.exitValue());
        }

        System.out.println("数据库备份完成: " + target.getAbsolutePath() + " (" + target.length() + " bytes)");
        cleanupOldBackups(dir);
        return target;
    }

    private void cleanupOldBackups(File dir) throws IOException {
        File[] files = dir.listFiles((d, name) -> name.startsWith("hospital_db_") && name.endsWith(".sql"));
        if (files == null) {
            return;
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        for (int i = 0; i < files.length; i++) {
            boolean tooMany = i >= retainDays;
            boolean tooOld = files[i].lastModified() < System.currentTimeMillis()
                    - java.util.concurrent.TimeUnit.DAYS.toMillis(retainDays);
            if (tooMany || tooOld) {
                Files.deleteIfExists(Path.of(files[i].getAbsolutePath()));
            }
        }
    }

    private String buildFileName() {
        return "hospital_db_" + LocalDate.now().format(DATE_FORMAT) + ".sql";
    }
}
