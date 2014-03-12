package us.drome.cobracorral;

import java.util.HashMap;

public class Configuration {
    private final CobraCorral plugin;
    
    public int MAX_HORSES;
    public boolean IMMORTALITY;
    public boolean AUTO_LOCK;
    public HashMap<String, Integer> HORSES;
    
    public Configuration(CobraCorral instance) {
        plugin = instance;
    }
    
    public void save() {
        plugin.saveConfig();
    }
    
    public void reload() {
        plugin.reloadConfig();
        
        MAX_HORSES = plugin.getConfig().getInt("max-horses", 2);
        IMMORTALITY = plugin.getConfig().getBoolean("immortality", true);
        AUTO_LOCK = plugin.getConfig().getBoolean("auto-lock", true);
        
        //HORSES = plugin.getConfig().getList("horses", new ArrayList<Horse>());
    }
}
