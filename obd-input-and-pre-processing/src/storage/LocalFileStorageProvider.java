package com.ignitionai.obdinput.storage;

import com.ignitionai.obdinput.schema.ObdMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class LocalFileStorageProvider implements StorageProvider {
    
    private final Path rootDir;

    public LocalFileStorageProvider(Path rootDir) {
        this.rootDir = rootDir;
        try {
            Files.createDirectories(rootDir.resolve("raw"));
            Files.createDirectories(rootDir.resolve("normalized"));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize local file storage", e);
        }
    }

    @Override
    public void persistRawRecord(String sessionId, String rawPayload) {
        // Enforces retention logic conceptually.
        // Files older than 90 days would be cleaned up in a background task.
        Path sessionDir = rootDir.resolve("raw").resolve(sessionId);
        try {
            Files.createDirectories(sessionDir);
            Path filePath = sessionDir.resolve(Instant.now().toEpochMilli() + ".json");
            Files.writeString(filePath, rawPayload);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist raw record", e);
        }
    }

    @Override
    public void persistNormalizedMessage(ObdMessage message) {
        // Files older than 12 months would be cleaned up in a background task.
        Path sessionDir = rootDir.resolve("normalized").resolve(message.getSessionId());
        try {
            Files.createDirectories(sessionDir);
            Path filePath = sessionDir.resolve(message.getMessageId() + ".json");
            // Basic persistence (In real system, serialize object to JSON properly)
            Files.writeString(filePath, "{ \"message_id\": \"" + message.getMessageId() + "\" }"); 
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist normalized record", e);
        }
    }
}
