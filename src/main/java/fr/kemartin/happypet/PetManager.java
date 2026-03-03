package fr.kemartin.happypet;

import fr.kemartin.happypet.tasks.PatrolTask;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PetManager {

    private final NamespacedKey keyMode;
    private final NamespacedKey keyPatrolX;
    private final NamespacedKey keyPatrolY;
    private final NamespacedKey keyPatrolZ;
    private final NamespacedKey keyPatrolWorld;

    private final Map<UUID, PatrolTask> patrolTasks = new HashMap<>();

    private final HappyPet plugin;

    public PetManager(HappyPet plugin) {
        this.plugin = plugin;
        this.keyMode       = new NamespacedKey(plugin, "mode");
        this.keyPatrolX    = new NamespacedKey(plugin, "patrol_x");
        this.keyPatrolY    = new NamespacedKey(plugin, "patrol_y");
        this.keyPatrolZ    = new NamespacedKey(plugin, "patrol_z");
        this.keyPatrolWorld = new NamespacedKey(plugin, "patrol_world");
    }

    // ---- PDC READ / WRITE ----

    public PetMode getMode(Tameable pet) {
        String raw = pet.getPersistentDataContainer()
                        .get(keyMode, PersistentDataType.STRING);
        if (raw == null) return PetMode.FOLLOWING;
        try {
            return PetMode.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return PetMode.FOLLOWING;
        }
    }

    public void setMode(Tameable pet, PetMode mode) {
        pet.getPersistentDataContainer()
           .set(keyMode, PersistentDataType.STRING, mode.name());
    }

    public Location getPatrolCenter(Tameable pet) {
        PersistentDataContainer pdc = pet.getPersistentDataContainer();
        if (!pdc.has(keyPatrolX, PersistentDataType.DOUBLE)) return null;
        double x = pdc.get(keyPatrolX, PersistentDataType.DOUBLE);
        double y = pdc.get(keyPatrolY, PersistentDataType.DOUBLE);
        double z = pdc.get(keyPatrolZ, PersistentDataType.DOUBLE);
        String worldName = pdc.get(keyPatrolWorld, PersistentDataType.STRING);
        var world = plugin.getServer().getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z);
    }

    public void setPatrolCenter(Tameable pet, Location loc) {
        PersistentDataContainer pdc = pet.getPersistentDataContainer();
        pdc.set(keyPatrolX, PersistentDataType.DOUBLE, loc.getX());
        pdc.set(keyPatrolY, PersistentDataType.DOUBLE, loc.getY());
        pdc.set(keyPatrolZ, PersistentDataType.DOUBLE, loc.getZ());
        pdc.set(keyPatrolWorld, PersistentDataType.STRING, loc.getWorld().getName());
    }

    // ---- MODE APPLICATION ----

    public void applyMode(Tameable pet, PetMode mode) {
        UUID entityId = pet.getUniqueId();
        cancelPatrolTask(entityId);

        switch (mode) {
            case FOLLOWING -> {
                if (pet instanceof Sittable s) s.setSitting(false);
            }
            case SITTING -> {
                if (pet instanceof Sittable s) s.setSitting(true);
            }
            case PATROL -> {
                if (pet instanceof Sittable s) s.setSitting(false);
                if (pet instanceof Mob) {
                    PatrolTask task = new PatrolTask(plugin, pet);
                    task.runTaskTimer(plugin, 0L,
                            plugin.getConfig().getLong("patrol_check_interval", 100L));
                    patrolTasks.put(entityId, task);
                }
            }
        }
    }

    public void cancelPatrolTask(UUID entityId) {
        PatrolTask task = patrolTasks.remove(entityId);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    public void shutdown() {
        patrolTasks.values().forEach(t -> { if (!t.isCancelled()) t.cancel(); });
        patrolTasks.clear();
    }
}
