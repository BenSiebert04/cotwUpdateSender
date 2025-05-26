package org.sie;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main {
    public static final String COMMAND_SUB = "subscribehcotw";
    public static final String COMMAND_UNSUB = "unsubscribehcotw";
    public static final String COMMAND_HISTORY = "historycotw";

    public static void main(String[] args) throws Exception {
        // Initialisiere Ordner für Steam und Epic Dateien
        DataHelper.initializeFolders();
        
        JDA jda = JDABuilder
                .createLight(System.getenv("BOT_TOKEN"),
                        GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new BotListener())
                .build()
                .awaitReady();

        // Commands registrieren
        jda.updateCommands()
                .addCommands(
                        Commands.slash(COMMAND_SUB, "Registriere dich für Updates"),
                        Commands.slash(COMMAND_UNSUB, "Abmelden von Updates"),
                        Commands.slash(COMMAND_HISTORY, "Zeige die History der gesendeten Nachrichten und aktuellen Dateien an")
                )
                .queue(cmdList -> System.out.println("Commands gesendet an Discord API"));

        System.out.println("Bot läuft");
    }


}