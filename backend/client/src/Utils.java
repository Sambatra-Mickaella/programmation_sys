import java.io.File;

public class Utils {

    // Vérifier si fichier dépasse quota
    public static boolean checkQuota(long fileSize, long quotaRemaining) {
        return fileSize <= quotaRemaining;
    }

    // Créer dossier utilisateur si inexistant
    public static void createUserFolder(String username) {
        File dir = new File("../shared_storage/users/" + username);
        if (!dir.exists()) dir.mkdirs();
    }
}
