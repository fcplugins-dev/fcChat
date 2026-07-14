
package fc.plugins.fcchat.api;

import fc.plugins.fcchat.api.FcChatApi;

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

