package fc.plugins.fcchat.api;

public final class FcChatApiProvider {
    private static volatile FcChatApi api;

    private FcChatApiProvider() {
    }

    public static FcChatApi get() {
        return api;
    }

    public static void set(FcChatApi instance) {
        api = instance;
    }
}
