package fc.plugins.fcchat.chat.automessages;

import java.util.List;

class MessageGroup {
    private final List<String> messages;
    private final String sound;

    public MessageGroup(List<String> messages, String sound) {
        this.messages = messages;
        this.sound = sound;
    }

    public List<String> getMessages() {
        return this.messages;
    }

    public String getSound() {
        return this.sound;
    }
}
