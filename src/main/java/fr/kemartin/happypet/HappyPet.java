package fr.kemartin.happypet;

import fr.kemartin.happypet.commands.HappyPetCommand;
import fr.kemartin.happypet.listeners.PetInteractListener;
import fr.kemartin.happypet.listeners.PlayerConnectionListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class HappyPet extends JavaPlugin {

    private PetManager petManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        petManager = new PetManager(this);

        getServer().getPluginManager().registerEvents(
                new PetInteractListener(this, petManager), this);
        getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(this, petManager), this);

        HappyPetCommand cmdHandler = new HappyPetCommand(this, petManager);
        PluginCommand cmd = getCommand("happypet");
        cmd.setExecutor(cmdHandler);
        cmd.setTabCompleter(cmdHandler);

        getLogger().info("HappyPet activé.");
    }

    @Override
    public void onDisable() {
        petManager.shutdown();
        getLogger().info("HappyPet désactivé.");
    }

    public PetManager getPetManager() {
        return petManager;
    }
}
