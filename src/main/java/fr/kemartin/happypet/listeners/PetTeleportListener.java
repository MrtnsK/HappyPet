package fr.kemartin.happypet.listeners;

import fr.kemartin.happypet.PetManager;
import fr.kemartin.happypet.PetMode;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;

public class PetTeleportListener implements Listener {

    private final PetManager petManager;

    public PetTeleportListener(PetManager petManager) {
        this.petManager = petManager;
    }

    /**
     * Bloque les téléportations vanilla (FollowOwnerGoal) pour les pets en mode PATROL.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPetTeleport(EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof Tameable pet)) return;
        if (petManager.getMode(pet) == PetMode.PATROL) {
            event.setCancelled(true);
        }
    }
}
