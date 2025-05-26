package org.sie;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.sie.model.HistoryEntry;
import org.sie.model.User;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import static org.sie.Main.*;

public class ForwardBotListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals(COMMAND_SUB)){
            handleSub(event);
        } else if (event.getName().equals(COMMAND_UNSUB)) {
            handleUnsub(event);
        } else if (event.getName().equals(COMMAND_HISTORY)) {
            handleHistory(event);
        }
    }

    private void handleHistory(SlashCommandInteractionEvent event) {
        // Sofort antworten um das 3-Sekunden-Limit zu vermeiden
        event.deferReply().queue();

        try {
            List<HistoryEntry> history = DataHelper.loadHistory();
            StringBuilder response = new StringBuilder();

            response.append("**📜 Nachrichten History:**\n\n");

            if (history.isEmpty()) {
                response.append("Keine Nachrichten in der History vorhanden.");
            } else {
                for (HistoryEntry entry : history) {
                    response.append("**").append(entry.getTimestamp()).append(":**\n");
                    response.append(entry.getMessage()).append("\n\n");
                }
            }

            response.append("\n**📁 Aktuelle Dateien werden als Anhang gesendet:**");

            // Discord hat ein Limit von 2000 Zeichen pro Nachricht
            String finalResponse = response.toString();
            if (finalResponse.length() > 2000) {
                finalResponse = finalResponse.substring(0, 1997) + "...";
            }

            // Sammle alle verfügbaren Dateien
            List<FileUpload> fileUploads = DataHelper.getCurrentFilesAsUploads();

            if (fileUploads.isEmpty()) {
                event.getHook().editOriginal(finalResponse + "\n\n*Keine aktuellen Dateien vorhanden.*").queue();
            } else {
                event.getHook().editOriginal(finalResponse)
                        .setFiles(fileUploads)
                        .queue();
            }

        } catch (Exception e) {
            e.printStackTrace();
            event.getHook().editOriginal("Ein Fehler ist beim Abrufen der History aufgetreten.").queue();
        }
    }

    private void handleUnsub(SlashCommandInteractionEvent event) {
        if(DataHelper.getUserById(event.getUser().getId()) == null){
            event.reply("Du bist nicht für die neusten Hunter Updates per DM registriert!").queue();
            return;
        }
        try {
            DataHelper.removeUser(event.getUser().getId());
            event.reply("Du hast dich erfolgreich von den neusten Hunter Updates per DM abgemeldet!").queue();
        } catch (Exception e) {
            e.printStackTrace();
            event.reply("Ein Fehler ist aufgetreten beim Abmelden. Bitte versuche es später erneut.").queue();
            return;
        }
    }

    private static void handleSub(SlashCommandInteractionEvent event) {
        if(!DataHelper.registerUser(new User(event.getUser().getId(), false))){
            event.reply("Du bist bereits für die neusten Hunter Updates per DM registriert!").queue();
            return;
        }

        event.reply("Du hast dich erfolgreich für die neusten Hunter Updates per DM registriert!").queue();
        event.getUser().openPrivateChannel()
                .flatMap(channel -> channel.sendMessage("🚀 Du hast dich erfolgreich registriert!"))
                .queue();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getChannelType() != ChannelType.PRIVATE) return;
        if (event.getAuthor().isBot()) return;

        List<User> users;
        try {
            users = DataHelper.loadUserList();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Prüfe, ob der Absender autorisiert ist
        String authorId = event.getAuthor().getId();
        boolean isAuthorized = users.stream()
                .anyMatch(u -> u.getId().equals(authorId) && u.isAuthorized());
        if (!isAuthorized) return;

        String content = event.getMessage().getContentDisplay();
        List<Message.Attachment> attachments = event.getMessage().getAttachments();

        // Prüfe ob der User auf eine Datei-Frage antwortet
        if (UserStateManager.hasPendingFileState(authorId)) {
            handleFileResponse(event, content);
            return;
        }

        // Verarbeite neue Nachricht mit Dateien
        if (!attachments.isEmpty()) {
            handleNewFilesMessage(event, content, attachments);
            return;
        }

        // Normale Nachricht ohne Dateien - nur weiterleiten
        if (!content.isBlank()) {
            DataHelper.addHistoryEntry(content);
            forwardMessageToUsers(event, content, attachments, users);
        }
    }

    private void handleFileResponse(MessageReceivedEvent event, String content) {
        String authorId = event.getAuthor().getId();
        UserStateManager.PendingFileState state = UserStateManager.getPendingFileState(authorId);

        if (state == null) return;

        List<Message.Attachment> attachments = state.getAttachments();

        try {
            if (attachments.size() == 1) {
                // Antwort auf "steam oder epic"
                String response = content.toLowerCase().trim();
                if (response.equals("steam") || response.equals("epic")) {
                    String folderName = response.equals("steam") ? DataHelper.getSteamFolderName() : DataHelper.getEpicFolderName();
                    InputStream in = attachments.get(0).getProxy().download().get();
                    DataHelper.saveFileToFolder(folderName, attachments.get(0).getFileName(), in);

                    event.getChannel().sendMessage("✅ Datei wurde erfolgreich im " + response.toUpperCase() + " Ordner gespeichert!").queue();
                } else {
                    event.getChannel().sendMessage("❌ Bitte antworte mit 'steam' oder 'epic'.").queue();
                    return; // Zustand nicht löschen, da ungültige Antwort
                }
            } else if (attachments.size() == 2) {
                // Antwort auf "Ist die erste Datei Steam? (ja/nein)"
                String response = content.toLowerCase().trim();
                if (response.equals("ja") || response.equals("nein")) {
                    boolean firstIsSteam = response.equals("ja");

                    String firstFolder = firstIsSteam ? DataHelper.getSteamFolderName() : DataHelper.getEpicFolderName();
                    String secondFolder = firstIsSteam ? DataHelper.getEpicFolderName() : DataHelper.getSteamFolderName();

                    InputStream in1 = attachments.get(0).getProxy().download().get();
                    InputStream in2 = attachments.get(1).getProxy().download().get();

                    DataHelper.saveFileToFolder(firstFolder, attachments.get(0).getFileName(), in1);
                    DataHelper.saveFileToFolder(secondFolder, attachments.get(1).getFileName(), in2);

                    event.getChannel().sendMessage("✅ Dateien wurden erfolgreich gespeichert!\n" +
                            "- " + attachments.get(0).getFileName() + " → " + (firstIsSteam ? "STEAM" : "EPIC") + "\n" +
                            "- " + attachments.get(1).getFileName() + " → " + (firstIsSteam ? "EPIC" : "STEAM")).queue();
                } else {
                    event.getChannel().sendMessage("❌ Bitte antworte mit 'ja' oder 'nein'.").queue();
                    return; // Zustand nicht löschen, da ungültige Antwort
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            event.getChannel().sendMessage("❌ Fehler beim Speichern der Datei(en).").queue();
        }

        UserStateManager.removePendingFileState(authorId);
    }

    private void handleNewFilesMessage(MessageReceivedEvent event, String content, List<Message.Attachment> attachments) {
        String authorId = event.getAuthor().getId();

        if (attachments.size() == 1) {
            UserStateManager.setPendingFileState(authorId, attachments);
            event.getChannel().sendMessage("📁 Eine Datei empfangen. Für welche Plattform ist diese Datei?\nAntworte mit: **steam** oder **epic**").queue();
        } else if (attachments.size() == 2) {
            UserStateManager.setPendingFileState(authorId, attachments);
            event.getChannel().sendMessage("📁📁 Zwei Dateien empfangen.\nIst die erste Datei (" + attachments.get(0).getFileName() + ") für Steam?\nAntworte mit: **ja** oder **nein**").queue();
        } else {
            event.getChannel().sendMessage("❌ Bitte sende nur 1 oder 2 Dateien gleichzeitig.").queue();
            return;
        }

        // Nachricht trotzdem zur History hinzufügen wenn Text vorhanden
        if (!content.isBlank()) {
            DataHelper.addHistoryEntry(content);
        }
    }

    private void forwardMessageToUsers(MessageReceivedEvent event, String content, List<Message.Attachment> attachments, List<User> users) {
        // Lade alle registrierten Benutzer-IDs
        List<String> friendIds = users.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        for (String friendId : friendIds) {
            event.getJDA().retrieveUserById(friendId)
                    .flatMap(user -> user.openPrivateChannel())
                    .queue(channel -> {
                        try {
                            for (Message.Attachment att : attachments) {
                                InputStream in = att.getProxy().download().get();
                                channel.sendFiles(FileUpload.fromData(in, att.getFileName())).queue();
                            }
                            if (!content.isBlank()) {
                                channel.sendMessage(content).queue();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        }
    }
}