package com.ignitionai.obdinput.adapter;

import com.ignitionai.obdinput.schema.ObdMessage;
import java.util.Optional;
import java.util.Iterator;
import java.util.List;

public class ReplayAdapter implements ObdAdapter {
    
    private final Iterator<ObdMessage> replayStream;
    private boolean sessionActive = false;

    public ReplayAdapter(List<ObdMessage> loadedMessages) {
        this.replayStream = loadedMessages.iterator();
    }

    @Override
    public void startSession(String sessionId) {
        this.sessionActive = true;
    }

    @Override
    public Optional<ObdMessage> pollNextMessage() {
        if (!sessionActive) return Optional.empty();
        if (replayStream.hasNext()) {
            return Optional.of(replayStream.next());
        }
        return Optional.empty();
    }

    @Override
    public void endSession() {
        this.sessionActive = false;
    }
}
