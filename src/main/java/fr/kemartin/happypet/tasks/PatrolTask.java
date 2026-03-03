package fr.kemartin.happypet.tasks;

import fr.kemartin.happypet.HappyPet;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Tameable;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class PatrolTask extends BukkitRunnable {

    private final HappyPet plugin;
    private final Tameable pet;
    private final Random random = new Random();

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

        double radius = plugin.getConfig().getDouble("patrol_radius", 30.0);
        double distFromCenter = mob.getLocation().distance(center);

        if (distFromCenter > radius) {
            // Hors du rayon : retour direct au centre
            mob.getPathfinder().moveTo(center, 1.0);
        } else {
            // Dans le rayon : cible aléatoire sur la surface
            double angle = random.nextDouble() * 2 * Math.PI;
            double dist  = random.nextDouble() * radius;
            double tx = center.getX() + dist * Math.cos(angle);
            double tz = center.getZ() + dist * Math.sin(angle);
            double ty = center.getWorld().getHighestBlockYAt((int) tx, (int) tz) + 1.0;

            Location target = new Location(center.getWorld(), tx, ty, tz);
            mob.getPathfinder().moveTo(target, 1.0);
        }
    }
}
