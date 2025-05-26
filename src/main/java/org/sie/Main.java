package org.sie;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.sie.model.User;

import java.io.IOException;
import java.util.List;

public class Main {
    public static final String COMMAND_SUB = "subscribehcotw";
    public static final String COMMAND_UNSUB = "unsubscribehcotw";

    public static void main(String[] args) throws Exception {
        JDA jda = JDABuilder
                .createLight(System.getenv("BOT_TOKEN"),
                        GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new ForwardBotListener())
                .build()
                .awaitReady();

        // 1) Command registrieren
        jda.updateCommands()
                .addCommands(Commands.slash(COMMAND_SUB, "Registriere dich für Updates"),
                        Commands.slash(COMMAND_UNSUB, "Abmelden von Updates"))
                .queue(cmdList -> System.out.println("Command gesendet an Discord API"));



        System.out.println("Bot läuft");
    }

    private static void notifyUsers(JDA jda) throws IOException {
        List<User> users = DataHelper.loadUserList();
        List<String> authorizedUserIds = users.stream()
                .filter(User::isAuthorized)
                .map(User::getId)
                .toList();

        for (String userId : authorizedUserIds) {
            jda.retrieveUserById(userId)
                    .queue(user -> user.openPrivateChannel()
                            .flatMap(ch -> ch.sendMessage("✅ Dein Befehl **/subscribehcotw** ist jetzt verfügbar!"))
                            .queue());
        }
    }
}
