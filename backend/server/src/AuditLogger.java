import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

final class AuditLogger {
    private AuditLogger() {}

    private static Path resolveAuditPath() {
        try {
            return StoragePaths.resolveServerResourcePath(
                    "audit.log",
                    "SMARTDRIVE_AUDIT_PATH",
                    "smartdrive.auditPath",
                    "resources/audit.log"
            );
        } catch (Exception e) {
            return Path.of("resources", "audit.log");
        }
    }

    static void log(String actor, String action, String details) {
        long ts = System.currentTimeMillis();
        String safeActor = actor == null ? "" : actor.replace(";", ",");
        String safeAction = action == null ? "" : action.replace(";", ",");
        String safeDetails = details == null ? "" : details.replace(";", ",");
        String line = ts + ";" + safeActor + ";" + safeAction + ";" + safeDetails;

        try {
            Path p = resolveAuditPath();
            Path parent = p.getParent();
            if (parent != null) Files.createDirectories(parent);
            try (BufferedWriter w = Files.newBufferedWriter(
                    p,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            )) {
                w.write(line);
                w.newLine();
            }
        } catch (Exception ignored) {
        }
    }
}
