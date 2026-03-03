package fr.kemartin.happypet.listeners;

import fr.kemartin.happypet.HappyPet;
import fr.kemartin.happypet.PetManager;
import fr.kemartin.happypet.PetMode;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerConnectionListener implements Listener {

    private final HappyPet plugin;
    private final PetManager petManager;

    /** UUID des joueurs actuellement hors ligne, utilisé pour la protection des pets. */
    private final Set<UUID> offlinePlayers = new HashSet<>();

    public PlayerConnectionListener(HappyPet plugin, PetManager petManager) {
        this.plugin = plugin;
        this.petManager = petManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        offlinePlayers.add(playerUUID);

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!isOwnedPet(entity, playerUUID)) continue;
                Tameable pet = (Tameable) entity;

                // Annule la patrouille et force la position assise
                petManager.cancelPatrolTask(entity.getUniqueId());
                if (pet instanceof Sittable s) s.setSitting(true);
                // Le mode PDC est conservé pour être restauré à la reconnexion
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        offlinePlayers.remove(playerUUID);

        // Délai d'1 seconde pour laisser les chunks se charger
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (!isOwnedPet(entity, playerUUID)) continue;
                    Tameable pet = (Tameable) entity;
                    PetMode savedMode = petManager.getMode(pet);
                    petManager.applyMode(pet, savedMode);
                }
            }
        }, 20L);
    }

    /**
     * Rend les pets invincibles quand leur propriétaire est hors ligne.
     * Logue toute tentative d'attaque.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Tameable pet)) return;
        if (!(pet instanceof Wolf) && !(pet instanceof Cat)) return;
        if (!pet.isTamed()) return;

        UUID ownerUUID = pet.getOwnerUniqueId();
        if (ownerUUID == null) return;

        if (!offlinePlayers.contains(ownerUUID)) return;

        // Le propriétaire est hors ligne : annuler les dégâts
        event.setCancelled(true);

        // Logger la tentative d'attaque
        String attackerDesc = "inconnu";
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            Entity attacker = byEntity.getDamager();
            if (attacker instanceof Player p) {
                attackerDesc = p.getName() + " (" + p.getUniqueId() + ")";
            } else {
                attackerDesc = attacker.getType().name();
            }
        } else {
            attackerDesc = event.getCause().name();
        }

        plugin.getLogger().info(
                "[HappyPet] Tentative d'attaque bloquée sur " +
                pet.getType().name() + " [" + pet.getUniqueId() + "]" +
                " (propriétaire hors ligne : " + ownerUUID + ")" +
                " — source : " + attackerDesc);
    }

    /** Retourne true si l'entité est un Wolf ou Cat apprivoisé appartenant au joueur donné. */
    private boolean isOwnedPet(Entity entity, UUID playerUUID) {
        if (!(entity instanceof Wolf) && !(entity instanceof Cat)) return false;
        if (!(entity instanceof Tameable pet)) return false;
        if (!pet.isTamed()) return false;
        return playerUUID.equals(pet.getOwnerUniqueId());
    }
}
