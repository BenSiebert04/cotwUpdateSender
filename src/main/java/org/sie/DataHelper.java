package org.sie;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sie.model.User;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DataHelper {
    private static final String USER_FILE = "users.json";
    private static final ObjectMapper mapper = new ObjectMapper();

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
}
