package fr.kemartin.happypet.listeners;

import fr.kemartin.happypet.HappyPet;
import fr.kemartin.happypet.PetManager;
import fr.kemartin.happypet.PetMode;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class PetInteractListener implements Listener {

    private static final Set<Material> VANILLA_INTERACT_ITEMS = Set.of(
            // Renommage
            Material.NAME_TAG,
            // Nourriture chien
            Material.BEEF, Material.COOKED_BEEF,
            Material.CHICKEN, Material.COOKED_CHICKEN,
            Material.MUTTON, Material.COOKED_MUTTON,
            Material.PORKCHOP, Material.COOKED_PORKCHOP,
            Material.RABBIT, Material.COOKED_RABBIT,
            Material.ROTTEN_FLESH,
            // Nourriture chat
            Material.COD, Material.COOKED_COD,
            Material.SALMON, Material.COOKED_SALMON,
            // Armure chien (Wolf Armor)
            Material.WOLF_ARMOR
    );

    private final HappyPet plugin;
    private final PetManager petManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public PetInteractListener(HappyPet plugin, PetManager petManager) {
        this.plugin = plugin;
        this.petManager = petManager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        // Évite le double-déclenchement dû à la main secondaire
        if (event.getHand() != EquipmentSlot.HAND) return;

        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof Wolf) && !(clicked instanceof Cat)) return;

        Tameable pet = (Tameable) clicked;
        Player player = event.getPlayer();

        if (!pet.isTamed()) return; // Animal sauvage : laisser le comportement vanilla

        if (!player.getUniqueId().equals(pet.getOwnerUniqueId())) {
            player.sendActionBar(mm.deserialize(
                    plugin.getConfig().getString("messages.not_owner",
                            "<red>Ce n'est pas ton animal.")));
            event.setCancelled(true);
            return;
        }

        // Laisser passer les actions vanilla : name tag, nourriture, armure
        ItemStack held = player.getInventory().getItemInMainHand();
        if (VANILLA_INTERACT_ITEMS.contains(held.getType())) return;

        PetMode currentMode = petManager.getMode(pet);
        PetMode nextMode = currentMode.next();

        if (nextMode == PetMode.PATROL) {
            petManager.setPatrolCenter(pet, clicked.getLocation());
        }

        petManager.setMode(pet, nextMode);
        petManager.applyMode(pet, nextMode);

        String msgKey = switch (nextMode) {
            case FOLLOWING -> "messages.mode_following";
            case SITTING   -> "messages.mode_sitting";
            case PATROL    -> "messages.mode_patrol";
        };
        player.sendActionBar(mm.deserialize(
                plugin.getConfig().getString(msgKey, "<green>" + nextMode.name())));

        event.setCancelled(true);
    }
}
