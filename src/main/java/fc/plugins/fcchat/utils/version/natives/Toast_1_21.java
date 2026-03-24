package fc.plugins.fcchat.utils.version.natives;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Toast_1_21 implements IToastSender {
    
    @Override
    public void sendToast(Player player, String text) {
        try {
            Class<?> craftPlayerClass = player.getClass();
            Object handle = craftPlayerClass.getMethod("getHandle").invoke(player);

            Class<?> packetClass = resolveClass(
                    "net.minecraft.network.protocol.Packet",
                    "net.minecraft.server.Packet"
            );
            Class<?> minecraftKeyClass = resolveClass(
                    "net.minecraft.resources.MinecraftKey",
                    "net.minecraft.resources.ResourceLocation"
            );
            Class<?> itemStackClass = resolveClass("net.minecraft.world.item.ItemStack");
            Class<?> craftItemStackClass = resolveCraftClass(craftPlayerClass, "inventory.CraftItemStack");
            Class<?> chatComponentClass = resolveClass(
                    "net.minecraft.network.chat.IChatBaseComponent",
                    "net.minecraft.network.chat.Component"
            );
            Class<?> advancementDisplayClass = resolveClass("net.minecraft.advancements.AdvancementDisplay");
            Class<?> advancementFrameTypeClass = resolveClass("net.minecraft.advancements.AdvancementFrameType");
            Class<?> advancementClass = resolveClass("net.minecraft.advancements.Advancement");
            Class<?> advancementProgressClass = resolveClass("net.minecraft.advancements.AdvancementProgress");
            Class<?> advancementRewardsClass = resolveClass("net.minecraft.advancements.AdvancementRewards");
            Class<?> packetPlayOutAdvancementsClass = resolveClass(
                    "net.minecraft.network.protocol.game.PacketPlayOutAdvancements",
                    "net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket"
            );

            Object connection = getConnection(handle);
            Method sendPacket = findSendPacketMethod(connection.getClass(), packetClass);
            if (sendPacket == null) {
                return;
            }

            Object advancementKey = createMinecraftKey(minecraftKeyClass);

            Method asNMSCopy = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
            Object bellIcon = asNMSCopy.invoke(null, new ItemStack(Material.BELL));

            Object title = createChatComponent(chatComponentClass, text.replace("&", "§"));
            Object description = createChatComponent(chatComponentClass, "");

            Object frameType = getTaskFrame(advancementFrameTypeClass);
            if (frameType == null) {
                return;
            }

            Constructor<?> displayConstructor = advancementDisplayClass.getConstructor(
                itemStackClass, chatComponentClass, chatComponentClass,
                minecraftKeyClass, advancementFrameTypeClass,
                boolean.class, boolean.class, boolean.class
            );
            Object display = displayConstructor.newInstance(bellIcon, title, description, null, frameType, true, false, true);

            Map<String, Object> criteria = new HashMap<String, Object>();
            Object emptyRewards = getStaticFieldValue(advancementRewardsClass, "a", "EMPTY");

            Object advancement = createAdvancement(
                    advancementClass,
                    advancementKey,
                    display,
                    emptyRewards,
                    criteria
            );
            if (advancement == null) {
                return;
            }

            Constructor<?> progressConstructor = advancementProgressClass.getDeclaredConstructor();
            progressConstructor.setAccessible(true);
            Object progress = progressConstructor.newInstance();

            Map<Object, Object> progressMap = new HashMap<>();
            progressMap.put(advancementKey, progress);

            Constructor<?> packetConstructor = packetPlayOutAdvancementsClass.getConstructor(
                boolean.class, java.util.Collection.class, Set.class, Map.class
            );
            Object addPacket = packetConstructor.newInstance(false, Collections.singleton(advancement), Collections.emptySet(), progressMap);

            sendPacket.invoke(connection, addPacket);

            JavaPlugin plugin = JavaPlugin.getProvidingPlugin(Toast_1_21.class);
            Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> {
                    try {
                        Set<Object> toRemove = new HashSet<>();
                        toRemove.add(advancementKey);
                        Object removePacket = packetConstructor.newInstance(false, Collections.emptySet(), toRemove, Collections.emptyMap());
                        sendPacket.invoke(connection, removePacket);
                    } catch (Exception e) {
                    }
                },
                20L
            );
        } catch (Exception e) {
        }
    }

    private static Class<?> resolveClass(String... names) throws ClassNotFoundException {
        for (String name : names) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassNotFoundException("Unable to resolve class");
    }

    private static Class<?> resolveCraftClass(Class<?> craftPlayerClass, String suffix) throws ClassNotFoundException {
        String packageName = craftPlayerClass.getPackage().getName();
        String basePackage;
        if (packageName.endsWith(".entity")) {
            basePackage = packageName.substring(0, packageName.length() - ".entity".length());
        } else {
            basePackage = "org.bukkit.craftbukkit";
        }
        return Class.forName(basePackage + "." + suffix);
    }

    private static Object getConnection(Object handle) throws IllegalAccessException {
        Field[] fields = handle.getClass().getFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            if ("connection".equals(fieldName) || "c".equals(fieldName) || "b".equals(fieldName) || "f".equals(fieldName)) {
                return field.get(handle);
            }
        }

        for (Field field : fields) {
            String typeName = field.getType().getName();
            if (typeName.contains("Connection") || typeName.contains("PacketListener")) {
                return field.get(handle);
            }
        }

        throw new IllegalStateException("Unable to resolve player connection field");
    }

    private static Method findSendPacketMethod(Class<?> connectionClass, Class<?> packetClass) {
        for (Method method : connectionClass.getMethods()) {
            if (method.getParameterCount() != 1) {
                continue;
            }

            Class<?> parameterType = method.getParameterTypes()[0];
            if (packetClass != null && parameterType.isAssignableFrom(packetClass)) {
                return method;
            }

            String typeName = parameterType.getName();
            if (typeName.contains("Packet") && ("send".equals(method.getName()) || "a".equals(method.getName()) || "b".equals(method.getName()))) {
                return method;
            }
        }
        return null;
    }

    private static Object createMinecraftKey(Class<?> minecraftKeyClass) throws Exception {
        try {
            Constructor<?> constructor = minecraftKeyClass.getConstructor(String.class);
            return constructor.newInstance("fcchat:ping_toast");
        } catch (NoSuchMethodException ignored) {
            Constructor<?> constructor = minecraftKeyClass.getConstructor(String.class, String.class);
            return constructor.newInstance("fcchat", "ping_toast");
        }
    }

    private static Object createChatComponent(Class<?> chatComponentClass, String text) throws Exception {
        String[] methodNames = {"b", "literal", "a"};
        for (String methodName : methodNames) {
            try {
                Method method = chatComponentClass.getMethod(methodName, String.class);
                if ((method.getModifiers() & java.lang.reflect.Modifier.STATIC) != 0) {
                    return method.invoke(null, text);
                }
            } catch (NoSuchMethodException ignored) {
            }
        }

        try {
            Class<?> chatComponentTextClass = resolveClass("net.minecraft.network.chat.ChatComponentText");
            Constructor<?> constructor = chatComponentTextClass.getConstructor(String.class);
            return constructor.newInstance(text);
        } catch (Exception ignored) {
        }

        throw new IllegalStateException("Unable to create chat component");
    }

    private static Object getTaskFrame(Class<?> advancementFrameTypeClass) {
        try {
            Object[] constants = advancementFrameTypeClass.getEnumConstants();
            if (constants != null) {
                for (Object constant : constants) {
                    if ("TASK".equalsIgnoreCase(constant.toString())) {
                        return constant;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        try {
            return advancementFrameTypeClass.getField("a").get(null);
        } catch (Exception ignored) {
        }

        return null;
    }

    private static Object getStaticFieldValue(Class<?> type, String... fieldNames) {
        for (String fieldName : fieldNames) {
            try {
                return type.getField(fieldName).get(null);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static Object createAdvancement(Class<?> advancementClass, Object advancementKey, Object display, Object rewards, Map<String, Object> criteria) {
        Constructor<?>[] constructors = advancementClass.getConstructors();
        for (Constructor<?> constructor : constructors) {
            try {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                Object[] args = new Object[parameterTypes.length];

                for (int i = 0; i < parameterTypes.length; i++) {
                    Class<?> parameterType = parameterTypes[i];
                    if (parameterType.isAssignableFrom(advancementKey.getClass())) {
                        args[i] = advancementKey;
                    } else if (display != null && parameterType.isAssignableFrom(display.getClass())) {
                        args[i] = display;
                    } else if (rewards != null && parameterType.isAssignableFrom(rewards.getClass())) {
                        args[i] = rewards;
                    } else if (Map.class.isAssignableFrom(parameterType)) {
                        args[i] = criteria;
                    } else if (parameterType == String[][].class) {
                        args[i] = new String[0][];
                    } else if (parameterType == boolean.class || parameterType == Boolean.class) {
                        args[i] = false;
                    } else {
                        args[i] = null;
                    }
                }

                return constructor.newInstance(args);
            } catch (Exception ignored) {
            }
        }

        return null;
    }
}
