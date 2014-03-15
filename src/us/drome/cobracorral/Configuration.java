package us.drome.cobracorral;

import java.util.Map;
import java.util.UUID;

public class Configuration {
    private final CobraCorral plugin;
    
    public int MAX_HORSES;
    public boolean IMMORTALITY;
    public boolean AUTO_LOCK;
    public Map<UUID, String> HORSES;
    
    public Configuration(CobraCorral instance) {
        plugin = instance;
    }
    
    public void save() {
        plugin.saveConfig();
    }
    
    public void load() {
        plugin.reloadConfig();
        
        MAX_HORSES = plugin.getConfig().getInt("max-horses", 2);
        IMMORTALITY = plugin.getConfig().getBoolean("immortality", true);
        AUTO_LOCK = plugin.getConfig().getBoolean("auto-lock", true);
        if(!plugin.getConfig().contains("horses")) {
            plugin.getConfig().createSection("horses");
        }
        HORSES = (Map)plugin.getConfig().getConfigurationSection("horses").getValues(false);
    }
}
