package fr.kemartin.happypet.commands;

import fr.kemartin.happypet.HappyPet;
import fr.kemartin.happypet.PetManager;
import fr.kemartin.happypet.PetMode;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class HappyPetCommand implements CommandExecutor, TabCompleter {

    private final HappyPet plugin;
    private final PetManager petManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public HappyPetCommand(HappyPet plugin, PetManager petManager) {
        this.plugin = plugin;
        this.petManager = petManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("happypet.admin")) {
            sender.sendMessage(mm.deserialize("<red>Permission refusée."));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "reload"  -> handleReload(sender);
            case "list"    -> handleList(sender, args);
            case "setmode" -> handleSetMode(sender, args);
            default        -> { sendUsage(sender); yield true; }
        };
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage(mm.deserialize(
                plugin.getConfig().getString("messages.reload_success", "<green>Rechargé.")));
        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        OfflinePlayer target;
        if (args.length >= 2) {
            target = Bukkit.getOfflinePlayer(args[1]);
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(mm.deserialize("<red>Précise un nom de joueur."));
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        String name = target.getName() != null ? target.getName() : "Inconnu";
        sender.sendMessage(mm.deserialize(
                plugin.getConfig().getString("messages.list_header", "<gold>Animaux :")
                        .replace("<player>", name)));

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Wolf) && !(entity instanceof Cat)) continue;
                if (!(entity instanceof Tameable pet)) continue;
                if (!pet.isTamed()) continue;
                if (!targetUUID.equals(pet.getOwnerUniqueId())) continue;

                PetMode mode = petManager.getMode(pet);
                String entry = plugin.getConfig()
                        .getString("messages.list_entry", "<gray> - <uuid> [<mode>]")
                        .replace("<uuid>", entity.getUniqueId().toString())
                        .replace("<mode>", mode.name());
                sender.sendMessage(mm.deserialize(entry));
            }
        }
        return true;
    }

    private boolean handleSetMode(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(mm.deserialize(
                    "<red>Usage : /hp setmode <joueur> <petUUID> <mode>"));
            return true;
        }

        PetMode newMode;
        try {
            newMode = PetMode.valueOf(args[3].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(mm.deserialize(
                    plugin.getConfig().getString("messages.setmode_invalid_mode",
                            "<red>Mode invalide.")));
            return true;
        }

        UUID petUUID;
        try {
            petUUID = UUID.fromString(args[2]);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(mm.deserialize("<red>UUID invalide."));
            return true;
        }

        Entity found = null;
        for (World world : Bukkit.getWorlds()) {
            Entity e = world.getEntity(petUUID);
            if (e != null) { found = e; break; }
        }

        if (!(found instanceof Tameable pet)) {
            sender.sendMessage(mm.deserialize(
                    plugin.getConfig().getString("messages.setmode_pet_not_found",
                            "<red>Animal introuvable.")));
            return true;
        }

        if (newMode == PetMode.PATROL) {
            petManager.setPatrolCenter(pet, found.getLocation());
        }
        petManager.setMode(pet, newMode);
        petManager.applyMode(pet, newMode);

        sender.sendMessage(mm.deserialize(
                plugin.getConfig().getString("messages.setmode_success",
                        "<green>Mode modifié.")));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String label, String[] args) {
        if (!sender.hasPermission("happypet.admin")) return List.of();

        if (args.length == 1) {
            return filterStarting(List.of("reload", "list", "setmode"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("list")
                || args[0].equalsIgnoreCase("setmode"))) {
            return null; // Bukkit suggère les joueurs en ligne
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("setmode")) {
            return filterStarting(
                    Arrays.stream(PetMode.values()).map(Enum::name).toList(),
                    args[3]);
        }
        return List.of();
    }

    private List<String> filterStarting(List<String> options, String prefix) {
        return options.stream()
                      .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                      .toList();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(mm.deserialize(
                "<gold>HappyPet — Commandes :\n" +
                "<yellow>/hp reload\n" +
                "/hp list [joueur]\n" +
                "/hp setmode <joueur> <petUUID> <mode>"));
    }
}
