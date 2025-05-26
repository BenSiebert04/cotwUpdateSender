package org.sie;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sie.model.HistoryEntry;
import org.sie.model.User;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DataHelper {
    private static final String USER_FILE = "users.json";
    private static final String HISTORY_FILE = "history.json";
    private static final String STEAM_FOLDER = "steam";
    private static final String EPIC_FOLDER = "epic";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public static boolean registerUser(User user) {
        try {
            // 1. Lese aktuelle Liste der User
            List<User> users = loadUserList();

            // 2. Prüfe ob User bereits existiert
            String userId = user.getId();
            boolean alreadyExists = users.stream()
                    .anyMatch(u -> u.getId().equals(userId));

            if (alreadyExists) {
                return false;
            }

            // 3. Neuen User hinzufügen
            users.add(user);

            // 4. Speichern
            saveUserList(users);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<User> loadUserList() throws IOException {
        File file = getOrCreateUserFile();
        if (file.length() == 0) {
            return new ArrayList<>();
        }
        return mapper.readValue(file, new TypeReference<>() {
        });
    }

    private static void saveUserList(List<User> users) throws IOException {
        File file = getOrCreateUserFile();
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, users);
    }

    private static File getOrCreateUserFile() throws IOException {
        // Datei im Zielverzeichnis (nicht im gepackten JAR) speichern
        File file = new File("users.json");
        if (!file.exists()) {
            file.createNewFile();
        }
        return file;
    }

    public static User getUserById(String id) {
        try {
            List<User> users = loadUserList();
            return users.stream().filter(user -> user.getId().equals(id)).findFirst().orElseGet(null);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void removeUser(String id) {
        try {
            List<User> users = loadUserList();
            users.removeIf(user -> user.getId().equals(id));
            saveUserList(users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Neue Methoden für History
    public static void addHistoryEntry(String message) {
        try {
            List<HistoryEntry> history = loadHistory();
            HistoryEntry entry = new HistoryEntry(message, LocalDateTime.now().format(formatter));
            history.add(entry);
            saveHistory(history);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<HistoryEntry> loadHistory() throws IOException {
        File file = getOrCreateHistoryFile();
        if (file.length() == 0) {
            return new ArrayList<>();
        }
        return mapper.readValue(file, new TypeReference<>() {
        });
    }

    private static void saveHistory(List<HistoryEntry> history) throws IOException {
        File file = getOrCreateHistoryFile();
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, history);
    }

    private static File getOrCreateHistoryFile() throws IOException {
        File file = new File(HISTORY_FILE);
        if (!file.exists()) {
            file.createNewFile();
        }
        return file;
    }

    // Methoden für Datei-Verwaltung
    public static void initializeFolders() {
        try {
            File steamFolder = new File(STEAM_FOLDER);
            File epicFolder = new File(EPIC_FOLDER);

            if (!steamFolder.exists()) {
                steamFolder.mkdirs();
            }
            if (!epicFolder.exists()) {
                epicFolder.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveFileToFolder(String folderName, String fileName, InputStream inputStream) throws IOException {
        File folder = new File(folderName);

        // Lösche alle existierenden Dateien im Ordner
        File[] existingFiles = folder.listFiles();
        if (existingFiles != null) {
            for (File file : existingFiles) {
                if (file.isFile()) {
                    file.delete();
                }
            }
        }

        // Speichere neue Datei
        File newFile = new File(folder, fileName);
        try (FileOutputStream fos = new FileOutputStream(newFile)) {
            inputStream.transferTo(fos);
        }
    }

    public static String getSteamFolderName() {
        return STEAM_FOLDER;
    }

    public static String getEpicFolderName() {
        return EPIC_FOLDER;
    }

    public static List<String> getFilesInFolder(String folderName) {
        File folder = new File(folderName);
        List<String> fileNames = new ArrayList<>();

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        fileNames.add(file.getName());
                    }
                }
            }
        }

        return fileNames;
    }

    public static List<net.dv8tion.jda.api.utils.FileUpload> getCurrentFilesAsUploads() {
        List<net.dv8tion.jda.api.utils.FileUpload> uploads = new ArrayList<>();

        try {
            // Steam Datei hinzufügen
            File steamFolder = new File(STEAM_FOLDER);
            if (steamFolder.exists()) {
                File[] steamFiles = steamFolder.listFiles();
                if (steamFiles != null && steamFiles.length > 0) {
                    File steamFile = steamFiles[0]; // Nur eine Datei pro Ordner
                    uploads.add(net.dv8tion.jda.api.utils.FileUpload.fromData(steamFile, "STEAM_" + steamFile.getName()));
                }
            }

            // Epic Datei hinzufügen
            File epicFolder = new File(EPIC_FOLDER);
            if (epicFolder.exists()) {
                File[] epicFiles = epicFolder.listFiles();
                if (epicFiles != null && epicFiles.length > 0) {
                    File epicFile = epicFiles[0]; // Nur eine Datei pro Ordner
                    uploads.add(net.dv8tion.jda.api.utils.FileUpload.fromData(epicFile, "EPIC_" + epicFile.getName()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return uploads;
    }
}