package fr.kemartin.happypet.tasks;

import fr.kemartin.happypet.HappyPet;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Tameable;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class PatrolTask extends BukkitRunnable {

    private static final int TARGET_TIMEOUT_TICKS = 200; // 200 ticks = 10s à intervalle 20t

    private final HappyPet plugin;
    private final Tameable pet;
    private final Random random = new Random();

    private Location currentTarget = null;
    private int ticksOnTarget = 0;

    public PatrolTask(HappyPet plugin, Tameable pet) {
        this.plugin = plugin;
        this.pet = pet;
    }

    @Override
    public void run() {
        if (!pet.isValid() || pet.isDead()) {
            cancel();
            plugin.getPetManager().cancelPatrolTask(pet.getUniqueId());
            return;
        }

        if (!(pet instanceof Mob mob)) {
            cancel();
            return;
        }

        Location center = plugin.getPetManager().getPatrolCenter(pet);
        if (center == null) {
            cancel();
            plugin.getPetManager().cancelPatrolTask(pet.getUniqueId());
            return;
        }

        if (!mob.getWorld().equals(center.getWorld())) {
            cancel();
            return;
        }

        double radius = plugin.getConfig().getDouble("patrol_radius", 15.0);

        // Si le pet sort de la zone (téléportation vanilla ou autre), le ramener
        if (mob.getLocation().distanceSquared(center) > (radius + 3) * (radius + 3)) {
            Location safeSpot = pointNearCenter(center, radius * 0.4);
            mob.teleport(safeSpot);
            currentTarget = null;
            return;
        }

        // Vérifier si le waypoint actuel est atteint ou expiré
        if (currentTarget != null) {
            ticksOnTarget++;
            boolean reached = mob.getLocation().distanceSquared(currentTarget) < 4.0;
            boolean timedOut = ticksOnTarget > TARGET_TIMEOUT_TICKS;

            if (!reached && !timedOut) {
                // Re-donner l'ordre à chaque tick pour contrer les vanilla goals
                mob.getPathfinder().moveTo(currentTarget, 1.0);
                return;
            }
            currentTarget = null;
        }

        // Choisir un nouveau waypoint depuis la position actuelle du pet
        currentTarget = pickNextWaypoint(mob.getLocation(), center, radius);
        ticksOnTarget = 0;
        mob.getPathfinder().moveTo(currentTarget, 1.0);
    }

    /**
     * Choisit un prochain waypoint en déambulant depuis la position actuelle du pet,
     * dans un rayon de 3–10 blocs, tout en restant dans la zone de patrouille.
     */
    private Location pickNextWaypoint(Location from, Location center, double radius) {
        double wanderDist = 3 + random.nextDouble() * 7;

        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double tx = from.getX() + wanderDist * Math.cos(angle);
            double tz = from.getZ() + wanderDist * Math.sin(angle);

            double dx = tx - center.getX();
            double dz = tz - center.getZ();
            if (dx * dx + dz * dz <= radius * radius) {
                double ty = center.getWorld().getHighestBlockYAt((int) tx, (int) tz) + 1.0;
                return new Location(center.getWorld(), tx, ty, tz);
            }
        }

        // Fallback : revenir près du centre si tous les essais sont hors zone
        return pointNearCenter(center, radius * 0.3);
    }

    private Location pointNearCenter(Location center, double maxDist) {
        double angle = random.nextDouble() * 2 * Math.PI;
        double dist = random.nextDouble() * maxDist;
        double tx = center.getX() + dist * Math.cos(angle);
        double tz = center.getZ() + dist * Math.sin(angle);
        double ty = center.getWorld().getHighestBlockYAt((int) tx, (int) tz) + 1.0;
        return new Location(center.getWorld(), tx, ty, tz);
    }
}
