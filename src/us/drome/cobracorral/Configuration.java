package us.drome.cobracorral;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.scheduler.BukkitRunnable;
import us.drome.cobrasql.*;

public class Configuration {
    private final CobraCorral plugin;
    public Database Database;
    
    public int MAX_HORSES;
    public boolean IMMORTALITY;
    public boolean AUTO_LOCK;
    public boolean IMMORTAL_COOLDOWN;
    public int COOLDOWN_TIME;
    public boolean PROTECT_CHESTS;
    public boolean STOP_PVP;
    public boolean EJECT_ON_LOGOFF;
    public String BACKEND;
    
    public Configuration(CobraCorral plugin) {
        this.plugin = plugin;
    }
    
    public void save() {
        plugin.saveConfig();
    }
    
    public void load() {
        reload();
        
        BACKEND = plugin.getConfig().getString("backend.type", "sqlite");
        switch (BACKEND.toLowerCase()) {
            case "mysql":
                String hostname = plugin.getConfig().getString("backend.hostname", "localhost");
                int port = plugin.getConfig().getInt("backend.port", 3306);
                String database = plugin.getConfig().getString("backend.database", "ccorral");
                String username = plugin.getConfig().getString("backend.username", "root");
                String password = plugin.getConfig().getString("backend.password", "password");
                try {
                    Database = new Database(new MySQLEngine(plugin.getLogger(), hostname, port, database, username, password));
                } catch (Exception ex) {
                    plugin.getLogger().severe("Error establishing database connection, disabling plugin.");
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                }
                break;
            case "sqlite":
                String filename = plugin.getConfig().getString("backend.file", "ccorral.db");
                try {
                    Database = new Database(new SQLiteEngine(plugin.getLogger(), filename));
                } catch (Exception ex) {
                    plugin.getLogger().severe("Error establishing database connection, disabling plugin.");
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                }
                break;
            default:
                plugin.getLogger().severe("No valid backend option set in config.yml. Must be: config, mysql, or sqlite. CobraCorral will now disable...");
                plugin.getServer().getPluginManager().disablePlugin(plugin);
                break;
        }
        
        //oldHorses contain items, convert to LockedHorse and update.
        if(plugin.getConfig().contains("horses")) {
            plugin.getLogger().info("Detected horses stored in config file, converting...");
            convertOldHorses();
        }
    }
    
    public void reload() {
        plugin.reloadConfig();
        
        MAX_HORSES = plugin.getConfig().getInt("max-horses", 2);
        IMMORTALITY = plugin.getConfig().getBoolean("immortality", true);
        AUTO_LOCK = plugin.getConfig().getBoolean("auto-lock", true);
        IMMORTAL_COOLDOWN = plugin.getConfig().getBoolean("immortal-cooldown", false);
        COOLDOWN_TIME = plugin.getConfig().getInt("cooldown-time", 0);
        PROTECT_CHESTS = plugin.getConfig().getBoolean("protect-chests", true);
        STOP_PVP = plugin.getConfig().getBoolean("stop-pvp", false);
        EJECT_ON_LOGOFF = plugin.getConfig().getBoolean("eject-on-logoff", true);
    }
    
    public void convertOldHorses() {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Map<String, oldLockedHorse> oldHorses = (Map)plugin.getConfig().getConfigurationSection("horses").getValues(true);
                    List<LockedHorse> convertedHorses = new ArrayList<>();
                    if(!oldHorses.isEmpty()) {
                        Iterator<String> iterator = oldHorses.keySet().iterator();
                        while(iterator.hasNext()) {
                            UUID uuid = UUID.fromString(iterator.next());
                            oldLockedHorse olhorse = oldHorses.get(uuid.toString());
                            LockedHorse lhorse = new LockedHorse(uuid, olhorse.getOwner(), olhorse.getName(), "", olhorse.getAppearance(), olhorse.getAppearance(), "", "", olhorse.getLocation(), 0, 0, 0, olhorse.getAccessList());
                            convertedHorses.add(lhorse);
                        }
                        Database.addHorses((ArrayList<LockedHorse>) convertedHorses);
                        plugin.getConfig().set("horses", null);
                        save();
                    }
                }
            }.runTaskAsynchronously(plugin);
    }
}
