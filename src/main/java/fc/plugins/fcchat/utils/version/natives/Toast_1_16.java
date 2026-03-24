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

public class Toast_1_16 implements IToastSender {
    private final String serverVersion;
    
    public Toast_1_16(String serverVersion) {
        this.serverVersion = serverVersion;
    }
    
    @Override
    public void sendToast(Player player, String text) {
        try {
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + serverVersion + ".entity.CraftPlayer");
            Class<?> entityPlayerClass = Class.forName("net.minecraft.server." + serverVersion + ".EntityPlayer");
            Class<?> playerConnectionClass = Class.forName("net.minecraft.server." + serverVersion + ".PlayerConnection");
            Class<?> packetClass = Class.forName("net.minecraft.server." + serverVersion + ".Packet");
            Class<?> minecraftKeyClass = Class.forName("net.minecraft.server." + serverVersion + ".MinecraftKey");
            Class<?> itemStackClass = Class.forName("net.minecraft.server." + serverVersion + ".ItemStack");
            Class<?> craftItemStackClass = Class.forName("org.bukkit.craftbukkit." + serverVersion + ".inventory.CraftItemStack");
            Class<?> chatComponentClass = Class.forName("net.minecraft.server." + serverVersion + ".IChatBaseComponent");
            Class<?> chatComponentTextClass = Class.forName("net.minecraft.server." + serverVersion + ".ChatComponentText");
            Class<?> advancementDisplayClass = Class.forName("net.minecraft.server." + serverVersion + ".AdvancementDisplay");
            Class<?> advancementFrameTypeClass = Class.forName("net.minecraft.server." + serverVersion + ".AdvancementFrameType");
            Class<?> advancementClass = Class.forName("net.minecraft.server." + serverVersion + ".Advancement");
            Class<?> advancementProgressClass = Class.forName("net.minecraft.server." + serverVersion + ".AdvancementProgress");
            Class<?> advancementRewardsClass = Class.forName("net.minecraft.server." + serverVersion + ".AdvancementRewards");
            Class<?> criterionClass = Class.forName("net.minecraft.server." + serverVersion + ".Criterion");
            Class<?> criterionTriggerImpossibleClass = Class.forName("net.minecraft.server." + serverVersion + ".CriterionTriggerImpossible");
            Class<?> packetPlayOutAdvancementsClass = Class.forName("net.minecraft.server." + serverVersion + ".PacketPlayOutAdvancements");
            
            Object handle = craftPlayerClass.getMethod("getHandle").invoke(player);
            Field connectionField = entityPlayerClass.getField("playerConnection");
            Object connection = connectionField.get(handle);
            
            Constructor<?> minecraftKeyConstructor = minecraftKeyClass.getConstructor(String.class, String.class);
            Object advancementKey = minecraftKeyConstructor.newInstance("fcchat", "ping_toast");
            
            Method asNMSCopy = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
            Object bellIcon = asNMSCopy.invoke(null, new ItemStack(Material.BELL));
            
            Constructor<?> chatComponentTextConstructor = chatComponentTextClass.getConstructor(String.class);
            Object title = chatComponentTextConstructor.newInstance(text.replace("&", "§"));
            Object description = chatComponentTextConstructor.newInstance("");
            
            Field taskField = advancementFrameTypeClass.getField("TASK");
            Object frameType = taskField.get(null);
            
            Constructor<?> displayConstructor = advancementDisplayClass.getConstructor(
                itemStackClass, chatComponentClass, chatComponentClass, 
                minecraftKeyClass, advancementFrameTypeClass, 
                boolean.class, boolean.class, boolean.class
            );
            Object display = displayConstructor.newInstance(bellIcon, title, description, null, frameType, true, false, true);
            
            Method setPositionMethod = advancementDisplayClass.getMethod("a", float.class, float.class);
            setPositionMethod.invoke(display, 0f, 0f);
            
            Map<String, Object> criteria = new HashMap<>();
            Class<?> criterionTriggerImpossibleAClass = Class.forName("net.minecraft.server." + serverVersion + ".CriterionTriggerImpossible$a");
            Constructor<?> impossibleAConstructor = criterionTriggerImpossibleAClass.getDeclaredConstructor();
            impossibleAConstructor.setAccessible(true);
            Object impossibleA = impossibleAConstructor.newInstance();
            
            Constructor<?> criterionConstructor = criterionClass.getConstructor(criterionTriggerImpossibleAClass);
            Object criterion = criterionConstructor.newInstance(impossibleA);
            criteria.put("impossible", criterion);
            
            String[][] requirements = {{"impossible"}};
            
            Field rewardsField = advancementRewardsClass.getField("a");
            Object rewards = rewardsField.get(null);
            
            Constructor<?> advancementConstructor = advancementClass.getConstructor(
                minecraftKeyClass, advancementClass, advancementDisplayClass,
                advancementRewardsClass, Map.class, String[][].class
            );
            Object advancement = advancementConstructor.newInstance(advancementKey, null, display, rewards, criteria, requirements);
            
            Constructor<?> progressConstructor = advancementProgressClass.getDeclaredConstructor();
            progressConstructor.setAccessible(true);
            Object progress = progressConstructor.newInstance();
            
            Method initProgressMethod = advancementProgressClass.getMethod("a", Map.class, String[][].class);
            initProgressMethod.invoke(progress, criteria, requirements);
            
            Method grantProgressMethod = advancementProgressClass.getMethod("a", String.class);
            grantProgressMethod.invoke(progress, "impossible");
            
            Map<Object, Object> progressMap = new HashMap<>();
            progressMap.put(advancementKey, progress);
            
            Constructor<?> packetConstructor = packetPlayOutAdvancementsClass.getConstructor(
                boolean.class, java.util.Collection.class, Set.class, Map.class
            );
            Object addPacket = packetConstructor.newInstance(false, Collections.singleton(advancement), Collections.emptySet(), progressMap);
            
            Method sendPacket = playerConnectionClass.getMethod("sendPacket", packetClass);
            sendPacket.invoke(connection, addPacket);
            
            JavaPlugin plugin = JavaPlugin.getProvidingPlugin(Toast_1_16.class);
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
}
