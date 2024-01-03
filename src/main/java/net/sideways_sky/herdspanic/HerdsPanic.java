package net.sideways_sky.herdspanic;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class HerdsPanic extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        radius = getConfig().getInt("radius");
        for(String heard : getConfig().getStringList("heards")){
            heards.add(EntityType.valueOf(heard));
        }
        ConfigurationSection complex = getConfig().getConfigurationSection("complex-heards");
        for(String key : complex.getKeys(false)){
            EntityType type = EntityType.valueOf(key);
            heards.add(type);
            speeds.put(type, complex.getDouble(key));
        }
    }
    public static void consoleSend(String message){
        Bukkit.getConsoleSender().sendMessage("[HeardsPanic] " + message);
    }
    public static int radius = 20;
    public static Set<EntityType> heards = new HashSet<>();
    public static HashMap<EntityType, Double> speeds = new HashMap<>();
    @EventHandler
    public static void onEntityDamageByEntity(EntityDamageByEntityEvent e){
        if(e.getDamager().getType() != EntityType.PLAYER || !heards.contains(e.getEntityType())){return;}
        List<Entity> entities = e.getEntity().getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            if(entity.getType() != e.getEntityType()){continue;}
            Mob mob = (Mob) ((CraftEntity) entity).getHandle();

            mob.goalSelector.getAvailableGoals().stream().filter(wrappedGoal -> wrappedGoal.getGoal() instanceof PanicGoal)
                    .findFirst().ifPresentOrElse(WrappedGoal::start, () -> {
                        if(!speeds.containsKey(e.getEntityType())){
                            consoleSend("Entity type " + e.getEntityType() + " has no panic Goal and no configuration to add one!");
                            return;
                        }
                        double speed = speeds.get(e.getEntityType());
                        PanicGoal goal = new PanicGoal((PathfinderMob) mob, speed);
                        mob.goalSelector.addGoal(1, goal);
                        goal.start();
                    });
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
