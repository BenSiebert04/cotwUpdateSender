package org.sie;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.sie.model.User;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import static org.sie.Main.COMMAND_SUB;
import static org.sie.Main.COMMAND_UNSUB;

public class ForwardBotListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals(COMMAND_SUB)){
            handleSub(event);
        } else if (event.getName().equals(COMMAND_UNSUB)) {
            handleUnsub(event);
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

        // Lade alle registrierten Benutzer-IDs
        List<String> friendIds = users.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        String content = event.getMessage().getContentDisplay();
        List<Message.Attachment> attachments = event.getMessage().getAttachments();

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