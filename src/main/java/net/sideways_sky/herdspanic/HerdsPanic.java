package net.sideways_sky.herdspanic;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class HerdsPanic extends JavaPlugin implements Listener {

    private final String CRAFTBUKKIT_PACKAGE = Bukkit.getServer().getClass().getPackage().getName();
    public Class<?> cbClass(String clazz) throws ClassNotFoundException {
        return Class.forName(CRAFTBUKKIT_PACKAGE + "." + clazz);
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        radius = getConfig().getInt("radius");
        delay = getConfig().getInt("delay");
        for(String heard : getConfig().getStringList("heards")){
            heards.add(EntityType.valueOf(heard));
        }
        ConfigurationSection complex = getConfig().getConfigurationSection("complex-heards");
        for(String key : complex.getKeys(false)){
            EntityType type = EntityType.valueOf(key);
            heards.add(type);
            speeds.put(type, complex.getDouble(key));
        }
        try {
            CraftEntityGetHandle = cbClass("entity.CraftEntity").getMethod("getHandle");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    public static void consoleSend(String message){
        Bukkit.getConsoleSender().sendMessage("[HeardsPanic] " + message);
    }
    public static int radius = 20;
    public static int delay = 0;
    public static Set<EntityType> heards = new HashSet<>();
    public static HashMap<EntityType, Double> speeds = new HashMap<>();
    static Method CraftEntityGetHandle;
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e){
        if(e.getDamager().getType() != EntityType.PLAYER || !heards.contains(e.getEntityType())){return;}
        List<Entity> entities = e.getEntity().getNearbyEntities(radius, radius, radius);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (Entity entity : entities) {
                if(entity.getType() != e.getEntityType()){continue;}
                try {
                    Mob mob = (Mob) CraftEntityGetHandle.invoke(entity);
                    mob.goalSelector.getAvailableGoals().stream().filter(wrappedGoal -> wrappedGoal.getGoal() instanceof PanicGoal)
                            .findFirst().ifPresentOrElse(WrappedGoal::start, () -> {
                                if(!speeds.containsKey(e.getEntityType())){
                                    consoleSend("Entity type " + e.getEntityType() + " has no panic Goal and no configuration to add one! (Use complex-heards)");
                                    return;
                                }
                                double speed = speeds.get(e.getEntityType());
                                PanicGoal goal = new PanicGoal((PathfinderMob) mob, speed);
                                mob.goalSelector.addGoal(1, goal);
                                goal.start();
                            });
                } catch (InvocationTargetException | IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }, delay);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
