package fc.plugins.fcchat.utils.concurrent;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class CompatScheduler {
    private final Plugin plugin;
    private final Object asyncScheduler;
    private final Object globalRegionScheduler;
    private final Method asyncRunNow;
    private final Method asyncRunDelayed;
    private final Method asyncRunAtFixedRate;
    private final Method globalRun;
    private final Method globalRunDelayed;
    private final Method globalRunAtFixedRate;
    private final Method entityGetScheduler;
    private final Method entityRun;
    private final Method entityRunDelayed;
    private final Method entityRunAtFixedRate;
    private final boolean folia;

    public CompatScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.asyncScheduler = resolve(Bukkit.getServer(), "getAsyncScheduler");
        this.globalRegionScheduler = resolve(Bukkit.getServer(), "getGlobalRegionScheduler");
        this.asyncRunNow = find(asyncScheduler, "runNow", Plugin.class, java.util.function.Consumer.class);
        this.asyncRunDelayed = find(asyncScheduler, "runDelayed", Plugin.class, java.util.function.Consumer.class, long.class, TimeUnit.class);
        this.asyncRunAtFixedRate = find(asyncScheduler, "runAtFixedRate", Plugin.class, java.util.function.Consumer.class, long.class, long.class, TimeUnit.class);
        this.globalRun = find(globalRegionScheduler, "run", Plugin.class, java.util.function.Consumer.class);
        this.globalRunDelayed = find(globalRegionScheduler, "runDelayed", Plugin.class, java.util.function.Consumer.class, long.class);
        this.globalRunAtFixedRate = find(globalRegionScheduler, "runAtFixedRate", Plugin.class, java.util.function.Consumer.class, long.class, long.class);
        this.entityGetScheduler = find(Entity.class, "getScheduler");
        Method entityRunLocal = null;
        Method entityRunDelayedLocal = null;
        Method entityRunAtFixedRateLocal = null;
        if (this.entityGetScheduler != null) {
            try {
                Class<?> schedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
                entityRunLocal = find(schedulerClass, "run", Plugin.class, java.util.function.Consumer.class, Runnable.class);
                entityRunDelayedLocal = find(schedulerClass, "runDelayed", Plugin.class, java.util.function.Consumer.class, Runnable.class, long.class);
                entityRunAtFixedRateLocal = find(schedulerClass, "runAtFixedRate", Plugin.class, java.util.function.Consumer.class, Runnable.class, long.class, long.class);
            } catch (ClassNotFoundException ignored) {
            }
        }
        this.entityRun = entityRunLocal;
        this.entityRunDelayed = entityRunDelayedLocal;
        this.entityRunAtFixedRate = entityRunAtFixedRateLocal;
        this.folia = this.asyncScheduler != null && this.globalRegionScheduler != null;
    }

    public boolean isFolia() {
        return folia;
    }

    public ScheduledTask runAsync(Runnable runnable) {
        if (folia && asyncRunNow != null) {
            Object task = invoke(asyncScheduler, asyncRunNow, plugin, (java.util.function.Consumer<Object>) ignored -> runnable.run());
            return new ReflectTask(task);
        }
        BukkitTask task = Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        return new BukkitTaskWrapper(task);
    }

    public ScheduledTask runGlobal(Runnable runnable) {
        if (folia && globalRun != null) {
            Object task = invoke(globalRegionScheduler, globalRun, plugin, (java.util.function.Consumer<Object>) ignored -> runnable.run());
            return new ReflectTask(task);
        }
        BukkitTask task = Bukkit.getScheduler().runTask(plugin, runnable);
        return new BukkitTaskWrapper(task);
    }

    public ScheduledTask runGlobalLater(long delayTicks, Runnable runnable) {
        if (folia && globalRunDelayed != null) {
            Object task = invoke(globalRegionScheduler, globalRunDelayed, plugin, (java.util.function.Consumer<Object>) ignored -> runnable.run(), delayTicks);
            return new ReflectTask(task);
        }
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        return new BukkitTaskWrapper(task);
    }

    public ScheduledTask runGlobalTimer(long delayTicks, long periodTicks, Runnable runnable) {
        if (folia && globalRunAtFixedRate != null) {
            Object task = invoke(globalRegionScheduler, globalRunAtFixedRate, plugin, (java.util.function.Consumer<Object>) ignored -> runnable.run(), delayTicks, periodTicks);
            return new ReflectTask(task);
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
        return new BukkitTaskWrapper(task);
    }

    public ScheduledTask runAsyncTimer(long delayTicks, long periodTicks, Runnable runnable) {
        if (folia && asyncRunAtFixedRate != null) {
            long delayMillis = ticksToMillis(delayTicks);
            long periodMillis = ticksToMillis(periodTicks);
            Object task = invoke(asyncScheduler, asyncRunAtFixedRate, plugin, (java.util.function.Consumer<Object>) ignored -> runnable.run(), delayMillis, periodMillis, TimeUnit.MILLISECONDS);
            return new ReflectTask(task);
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delayTicks, periodTicks);
        return new BukkitTaskWrapper(task);
    }

    public ScheduledTask runEntity(Entity entity, Runnable runnable) {
        if (folia && entityGetScheduler != null && entityRun != null) {
            Object entityScheduler = invoke(entity, entityGetScheduler);
            if (entityScheduler != null) {
                Object task = invoke(entityScheduler, entityRun, plugin, (java.util.function.Consumer<Object>) ignored -> runnable.run(), (Runnable) () -> {
                });
                return new ReflectTask(task);
            }
        }
        BukkitTask task = Bukkit.getScheduler().runTask(plugin, runnable);
        return new BukkitTaskWrapper(task);
    }

    public ScheduledTask runEntityLater(Entity entity, long delayTicks, Runnable runnable) {
        if (folia && entityGetScheduler != null && entityRunDelayed != null) {
            Object entityScheduler = invoke(entity, entityGetScheduler);
            if (entityScheduler != null) {
                Object task = invoke(entityScheduler, entityRunDelayed, plugin, (java.util.function.Consumer<Object>) ignored -> runnable.run(), (Runnable) () -> {
                }, delayTicks);
                return new ReflectTask(task);
            }
        }
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        return new BukkitTaskWrapper(task);
    }

    public ScheduledTask runEntityTimer(Entity entity, long delayTicks, long periodTicks, Runnable runnable) {
        if (folia && entityGetScheduler != null && entityRunAtFixedRate != null) {
            Object entityScheduler = invoke(entity, entityGetScheduler);
            if (entityScheduler != null) {
                Object task = invoke(entityScheduler, entityRunAtFixedRate, plugin, (java.util.function.Consumer<Object>) ignored -> runnable.run(), (Runnable) () -> {
                }, delayTicks, periodTicks);
                return new ReflectTask(task);
            }
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
        return new BukkitTaskWrapper(task);
    }

    private static Object resolve(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Method find(Object target, String name, Class<?>... parameters) {
        if (target == null) {
            return null;
        }
        return find(target.getClass(), name, parameters);
    }

    private static Method find(Class<?> owner, String name, Class<?>... parameters) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getMethod(name, parameters);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object invoke(Object target, Method method, Object... args) {
        if (target == null || method == null) {
            return null;
        }
        try {
            return method.invoke(target, args);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static long ticksToMillis(long ticks) {
        return ticks * 50L;
    }

    public interface ScheduledTask {
        void cancel();
    }

    private static final class BukkitTaskWrapper implements ScheduledTask {
        private final BukkitTask task;

        private BukkitTaskWrapper(BukkitTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            if (task != null) {
                task.cancel();
            }
        }
    }

    private static final class ReflectTask implements ScheduledTask {
        private final Object task;
        private final Method cancelMethod;

        private ReflectTask(Object task) {
            this.task = task;
            this.cancelMethod = task == null ? null : find(task.getClass(), "cancel");
        }

        @Override
        public void cancel() {
            invoke(task, cancelMethod);
        }
    }
}
