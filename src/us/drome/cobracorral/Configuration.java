package us.drome.cobracorral;

import com.evilmidget38.UUIDFetcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.scheduler.BukkitRunnable;

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
        HORSES = (Map)plugin.getConfig().getConfigurationSection("horses").getValues(true);
        if(!HORSES.isEmpty() && !HORSES.get(HORSES.keySet().iterator().next()).ownerName.isEmpty()) {
            plugin.getLogger().info("Detected non-UUID based horse entries in config. Converting now...");
            convertHORSES();
        }
    }
    
    public void reload() {
        Map<String, LockedHorse> tempHorses = HORSES;

        load();
        
        HORSES = tempHorses;
    }
    
    public void convertHORSES() {
        List<String> usernames = new ArrayList<>();
        
        for(String key : HORSES.keySet()) {
            if(!usernames.contains(HORSES.get(key).ownerName))
                usernames.add(HORSES.get(key).ownerName);
        }
        
        final UUIDFetcher fetcher = new UUIDFetcher(usernames);
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    final Map<String, UUID> response = fetcher.call();
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            finishConvertHORSES(response);
                        }
                    }.runTask(plugin);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Error while attempting to fetch UUIDs, disabling plugin.");
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
    }
    
    public void finishConvertHORSES(Map<String, UUID> response) {
        for(String key : HORSES.keySet()) {
            if(response.containsKey(HORSES.get(key).ownerName)) {
                HORSES.get(key).setOwner(response.get(HORSES.get(key).ownerName));
                HORSES.get(key).ownerName = null;
            }
        }
        save();
        plugin.getLogger().info("Successfully converted " + response.size() + " names to UUID in the config.");
    }
}
