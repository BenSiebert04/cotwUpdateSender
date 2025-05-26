package org.sie;

import net.dv8tion.jda.api.entities.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserStateManager {
    private static final Map<String, PendingFileState> pendingFiles = new HashMap<>();

    public static void setPendingFileState(String userId, List<Message.Attachment> attachments) {
        pendingFiles.put(userId, new PendingFileState(attachments));
    }

    public static PendingFileState getPendingFileState(String userId) {
        return pendingFiles.get(userId);
    }

    public static void removePendingFileState(String userId) {
        pendingFiles.remove(userId);
    }

    public static boolean hasPendingFileState(String userId) {
        return pendingFiles.containsKey(userId);
    }

    public static class PendingFileState {
        private final List<Message.Attachment> attachments;

        public PendingFileState(List<Message.Attachment> attachments) {
            this.attachments = attachments;
        }

        public List<Message.Attachment> getAttachments() {
            return attachments;
        }
    }
}