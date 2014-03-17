package us.drome.cobracorral;

import java.util.Map;

public class Configuration {
    private final CobraCorral plugin;
    
    public int MAX_HORSES;
    public boolean IMMORTALITY;
    public boolean AUTO_LOCK;
    public Map<String, LockedHorse> HORSES;
    
    public Configuration(CobraCorral instance) {
        plugin = instance;
    }
    
    public void save() {
        plugin.getConfig().set("horses", HORSES);
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
        HORSES = (Map)plugin.getConfig().getConfigurationSection("horses").getValues(true);
    }
}
