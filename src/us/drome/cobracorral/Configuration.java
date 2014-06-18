package us.drome.cobracorral;

import java.util.HashMap;
import java.util.Map;

public class Configuration {
    private final CobraCorral plugin;
    
    public int MAX_HORSES;
    public boolean IMMORTALITY;
    public boolean AUTO_LOCK;
    public boolean IMMORTAL_COOLDOWN;
    public int COOLDOWN_TIME;
    public Map<String, LockedHorse> HORSES;
    
    public Configuration(CobraCorral plugin) {
        this.plugin = plugin;
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
        IMMORTAL_COOLDOWN = plugin.getConfig().getBoolean("immortal-cooldown", false);
        COOLDOWN_TIME = plugin.getConfig().getInt("cooldown-time", 0);
        if(!plugin.getConfig().contains("horses")) {
            plugin.getConfig().createSection("horses");
        }
        Map horseList = (Map)plugin.getConfig().getConfigurationSection("horses").getValues(true);
        try {
            HORSES = horseList;
        } catch (IllegalArgumentException e) {
            HORSES = ConvertHORSES(horseList);
        }
    }
    
    public void reload() {
        Map<String, LockedHorse> tempHorses = HORSES;

        load();
        
        HORSES = tempHorses;
    }
    
    public Map<String, LockedHorse> ConvertHORSES(Map<String, String> HORSES) {
        return new HashMap<String, LockedHorse>();
    }
}
