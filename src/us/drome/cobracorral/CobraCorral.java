package us.drome.cobracorral;

import java.io.File;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class CobraCorral extends JavaPlugin {
    public final Configuration config = new Configuration(this);
    
    //Horse metadata key to temporarily unlock a horse for other riders.
    public static final String HORSE_TEST_DRIVE = "CobraCorral.test_drive";
    
    public void OnDisable() {
        getLogger().info("version " + getDescription().getVersion() + " has been unloaded.");
        config.save();
    }
    
    public void OnEnable() {
        File configFile = new File(getDataFolder(), "config.yml");
        if(!configFile.exists()) {
            getConfig().options().copyDefaults();
            saveConfig();
        }
        
        config.load();
    }
    
    
    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        return true;
    }
    
    public boolean maxHorsesLocked(String player) {
        return true;
    }
    
    public void lockHorse(UUID id, String player) {
        config.HORSES.put(id, player);
    }
    
    public void unlockHorse(UUID id) {
        config.HORSES.remove(id);
    }
}
