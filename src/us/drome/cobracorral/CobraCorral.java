package us.drome.cobracorral;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class CobraCorral extends JavaPlugin {
    public final Configuration config = new Configuration(this);
    private CorralListener listener = new CorralListener(this);
    
    //Horse metadata keys.
    public static final String HORSE_TEST_DRIVE = "CobraCorral.test_drive";
    public static final String HORSE_INFO = "CobraCorral.info";
    public static final String HORSE_LOCK = "CobraCorral.lock";
    public static final String HORSE_UNLOCK = "CobraCorral.unlock";
    
    public void onDisable() {
        getLogger().info("version " + getDescription().getVersion() + " has begun unloading...");
        config.save();
        getLogger().info(" has saved " + config.HORSES.size() + " locked horses.");
        getLogger().info("version " + getDescription().getVersion() + " has finished unloading.");
    }
    
    public void onEnable() {
        getLogger().info("version " + getDescription().getVersion() + " has begun loading...");
        File configFile = new File(getDataFolder(), "config.yml");
        if(!configFile.exists()) {
            getConfig().options().copyDefaults(true);
            saveConfig();
        }
        config.load();
        getLogger().info(" has loaded " + config.HORSES.size() + " locked horses.");
        getServer().getPluginManager().registerEvents(listener, this);
        getLogger().info("version " + getDescription().getVersion() + " has finished loading.");
    }
    
    
    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        switch(cmd.getName().toLowerCase()) {
            case "ccorral":
                helpDisplay(sender);
                break;
            case "corral":
                if(sender instanceof Player) {
                    ((Player)sender).setMetadata(HORSE_LOCK, new FixedMetadataValue(this, null));
                    sender.sendMessage(ChatColor.GRAY + "Right click on a Horse that you own.");
                } else {
                    sender.sendMessage("That command can only be ran by a Player.");
                }
                break;
            case "uncorral":
                if(sender instanceof Player) {
                    ((Player)sender).setMetadata(HORSE_UNLOCK, new FixedMetadataValue(this, null));
                    sender.sendMessage(ChatColor.GRAY + "Right click on a Horse that you own.");
                } else {
                    sender.sendMessage("That command can only be ran by a Player.");
                }
                break;
            case "testdrive":
                if(sender instanceof Player) {
                    ((Player)sender).setMetadata(HORSE_TEST_DRIVE, new FixedMetadataValue(this, null));
                    sender.sendMessage(ChatColor.GRAY + "Right click on a Horse that you own.");
                } else {
                    sender.sendMessage("That command can only be ran by a Player.");
                }
                break;
            case "horse-list":
                if(sender instanceof Player) {
                    String player = sender.getName();
                    if(args.length > 0 && (sender.hasPermission("ccorral.list-all") || sender.hasPermission("ccorral.admin"))) {
                        if(getServer().getOfflinePlayer(args[0]).hasPlayedBefore()) {
                            player = args[0];
                        }
                    }
                    List<UUID> horseIDs = new ArrayList<>();
                    List<String> response = new ArrayList<>();
                    int count = 0;

                    for(String key : config.HORSES.keySet()) {
                        if(config.HORSES.get(key).getOwner().equalsIgnoreCase(player)) {
                            count++;
                            LockedHorse lhorse = config.HORSES.get(key);
                            Horse horse = getHorse(lhorse.getLocation(this), UUID.fromString(key));
                            if(horse != null) {
                                config.HORSES.put(key, lhorse.updateHorse(horse));
                            } else {
                                getLogger().info("Failed to load horse " + key.toString() + " from chunk at location " + lhorse.getLocation() +
                                    " for player " + lhorse.getOwner() + ", using cache.");
                            }
                            response.add(String.valueOf(count) + ChatColor.GRAY + " | " + lhorse.getName() + " | " + lhorse.getAppearance() +
                                " | " + lhorse.getArmor() + " | " + lhorse.getWorld());
                        }
                    }
                    
                    if(!response.isEmpty()) {
                        response.add(0, ChatColor.GRAY + "Horses locked by " +
                            (player.equalsIgnoreCase(sender.getName()) ? "you" : player) + ":");
                        for(String line : response) {
                            sender.sendMessage(line);
                        }
                    } else {
                        sender.sendMessage(ChatColor.GRAY + "There are no horses locked by " +
                            (player.equalsIgnoreCase(sender.getName()) ? "you" : player) + ".");
                    }                             
                } else {
                    sender.sendMessage("That command can only be ran by a Player.");
                }
                break;
            case "horse-gps":
                if(sender instanceof Player) {
                    if(args.length > 0) {
                        String pName = sender.getName();
                        List<UUID> horseID = new ArrayList<>();
                        int target = 0;
                        int count = 0;
                        
                        if(args.length > 1) {
                            if(sender.hasPermission("ccorral.gps-all") || sender.hasPermission("ccorral.admin")) {
                                if(getServer().getOfflinePlayer(args[0]).hasPlayedBefore()) {
                                    pName = args[0];
                                    try {
                                        target = Integer.parseInt(args[1]);
                                    } catch (NumberFormatException e) {
                                        sender.sendMessage(ChatColor.GRAY + "Horse ID provided: " + args[1] + ", is not a valid integer.");
                                        return false;
                                    }
                                } else {
                                    sender.sendMessage(ChatColor.GRAY + args[0] + " is not a valid player.");
                                    return false;
                                }
                            } else { 
                                sender.sendMessage(ChatColor.RED + "You do not have permission to do that.");
                                break;
                            }   
                        } else {
                            try {
                                target = Integer.parseInt(args[0]);
                            } catch (NumberFormatException e) {
                                sender.sendMessage(ChatColor.GRAY + "Horse ID provided: " + args[1] + ", is not a valid integer.");
                                return false;
                            }
                        }
                        
                        for(String key : config.HORSES.keySet()) {
                            if(config.HORSES.get(key).getOwner().equalsIgnoreCase(pName)) {
                                count++;
                                if(count == target) {
                                    LockedHorse lhorse = config.HORSES.get(key);
                                    Horse horse = getHorse(lhorse.getLocation(this), UUID.fromString(key));
                                    if(horse != null) {
                                        config.HORSES.put(key, lhorse.updateHorse(horse));
                                    } else {
                                        getLogger().info("Failed to load horse " + key + " from chunk at location " +
                                            lhorse.getLocation() + " for player " + lhorse.getOwner() + ", using cache.");
                                    }
                                    
                                    Player player = (Player)sender;
                                    
                                    Vector pVector = player.getLocation().toVector();
                                    Vector hVector = lhorse.getLocation(this).toVector();
                                    
                                    if(!player.isInsideVehicle() && player.getWorld().equals(lhorse.getLocation(this).getWorld())) {
                                        Vector vector = hVector.subtract(pVector);
                                        player.teleport(player.getLocation().setDirection(vector));
                                    }
                                    player.playSound(player.getLocation(), Sound.SUCCESSFUL_HIT, 1f , 1f);
                                    player.sendMessage(ChatColor.GRAY + lhorse.getName() + " Located @ X:" + lhorse.getX() +
                                        " Y:" + lhorse.getY() + " Z:" + lhorse.getZ() + " World:" + lhorse.getWorld());
                                    return true;
                                }
                            }
                        }
                        
                        sender.sendMessage(ChatColor.GRAY + "No horse found by that ID.");
                    } else {
                        return false;
                    }
                } else {
                    sender.sendMessage("That command can only be ran by a Player.");
                }   
                break;
            case "horse-tp":
                if(args.length > 1) {
                    String pName = "";
                    List<UUID> horseID = new ArrayList<>();
                    int target = 0;
                    int count = 0;
                    
                    if(getServer().getOfflinePlayer(args[0]).hasPlayedBefore()) {
                        pName = args[0];
                        try {
                            target = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.GRAY + "Horse ID provided: " + args[1] + ", is not a valid integer.");
                            return false;
                        }
                    } else {
                        sender.sendMessage(ChatColor.GRAY + args[0] + " is not a valid player.");
                        return false;
                    }
                    
                    for(String key : config.HORSES.keySet()) {
                        if(config.HORSES.get(key).getOwner().equalsIgnoreCase(pName)) {
                            count++;
                            LockedHorse lhorse = config.HORSES.get(key);
                            Horse horse = getHorse(lhorse.getLocation(this), UUID.fromString(key));
                            if(horse != null) {
                                config.HORSES.put(key, lhorse.updateHorse(horse));
                                if (!horse.getLocation().getWorld().equals(((Player)sender).getWorld())) {
                                    sender.sendMessage(ChatColor.GRAY + "Cannot teleport horses across worlds. Enter world \"" +
                                        lhorse.getWorld() + "\" to teleport this horse.");
                                    return true;
                                }
                                if(horse.getPassenger() == null) {
                                    ((Player)sender).playSound(((Player)sender).getLocation(), Sound.ENDERMAN_TELEPORT, 1f , 1f);
                                    sender.sendMessage(ChatColor.GRAY + lhorse.getName() + " " + lhorse.getAppearance() +
                                        " has been teleported to your location!");
                                    horse.teleport(((Player)sender).getLocation());
                                    
                                } else {
                                    sender.sendMessage(ChatColor.GRAY + lhorse.getName() + " " + lhorse.getAppearance() +
                                        " is being ridden by " + ((Player)horse.getPassenger()).getName() + " and can't be teleported.");
                                }

                            } else {
                                sender.sendMessage(ChatColor.GRAY + "That horse failed to load, please try again.");
                                getLogger().info("Failed to load horse " + key + " from chunk at location " +
                                    lhorse.getLocation() + " for player " + lhorse.getOwner() + ", cancelling teleport.");
                            }
                            return true;
                        }
                    }
                    
                    sender.sendMessage(ChatColor.GRAY + "No horse found by that ID.");
                } else {
                    return false;
                }
                break;
            case "horse-info":
                if(sender instanceof Player) {
                    ((Player)sender).setMetadata(HORSE_INFO, new FixedMetadataValue(this, null));
                    sender.sendMessage(ChatColor.GRAY + "Right click on a Horse to retrieve it's information.");
                } else {
                    sender.sendMessage("That command can only be ran by a Player.");
                }
                break;
        }
        return true;
    }
    
    /**
     * Function to locate a horse in an unloaded chunk and return the horse entity.
     */
    public Horse getHorse(Location horseLoc, UUID id) {
        Chunk toLoad = horseLoc.getChunk();
        Horse horse = null;
        if(!toLoad.isLoaded()) {
            toLoad.load();
        }
        for(Entity entity : toLoad.getEntities()) {
            if(entity instanceof Horse) {
                if(entity.getUniqueId().equals(id)) {
                    horse = (Horse)entity;
                }
            }
        }
        if(horse == null) {
            for(World world : getServer().getWorlds()) {
                for(Entity entity : world.getEntitiesByClass(Horse.class)) {
                    if(entity.getUniqueId().equals(id)) {
                        horse = (Horse)entity;
                    }
                }
            }
        }
        return horse;
    }
    
    public boolean isHorseLocked(Horse horse) {
        if(config.HORSES.containsKey(horse.getUniqueId().toString())) {
            return true;
        } else {
            return false;
        }
    }
    
    public boolean maxHorsesLocked(String player) {
        int count = 0;
        
        for(String key : config.HORSES.keySet()) {
            if(config.HORSES.get(key).getOwner().equalsIgnoreCase(player)) {
                count++;
            }
        }
        
        if(count >= config.MAX_HORSES) //check for > just in case.
            return true;
        else
            return false;
    }
    
    public void lockHorse(UUID id, Horse horse) {
        config.HORSES.put(id.toString(), new LockedHorse(horse));
    }
    
    public void unlockHorse(UUID id) {
        config.HORSES.remove(id.toString());
    }
    
    public void helpDisplay(CommandSender sender) {
        sender.sendMessage(ChatColor.GRAY + "=======" + ChatColor.WHITE + "CobraCorral v" + getDescription().getVersion() +
            " Commands" + ChatColor.GRAY + "=======");
        if(sender.hasPermission("ccorral.lock")) {
            sender.sendMessage(ChatColor.WHITE + "/corral" + ChatColor.GRAY + " | Used to lock a horse you have tamed.");
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /horse-lock");
            sender.sendMessage(ChatColor.WHITE + "/uncorral" + ChatColor.GRAY + " | Used to unlock a horse you have tamed.");
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /horse-unlock");
            sender.sendMessage(ChatColor.WHITE + "/testdrive" + ChatColor.GRAY + " | Temporarily allow others to ride a locked horse.");
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /horse-test");
        }
        if(sender.hasPermission("ccorral.list")) {
            sender.sendMessage(ChatColor.WHITE + "/horse-list" + ChatColor.GRAY + " | List all horses you have locked.");
        }
        if(sender.hasPermission("ccorral.list-all")) {
            sender.sendMessage(ChatColor.WHITE + "/horse-list <player>" + ChatColor.GRAY + " | List horses owned by player.");
        }
        if(sender.hasPermission("ccorral.gps")) {
            sender.sendMessage(ChatColor.WHITE + "/horse-gps <horseID>" + ChatColor.GRAY + " | Get the location of a specified horse.");
        }
        if(sender.hasPermission("ccorral.gps-all")) {
            sender.sendMessage(ChatColor.WHITE + "/horse-gps <player> <horseID>" + ChatColor.GRAY + " | Locate a player's horse.");
        }
        if(sender.hasPermission("ccorral.tp")) {
            sender.sendMessage(ChatColor.WHITE + "/horse-tp <player> <horseID>" + ChatColor.GRAY + " | Telelport a horse to you.");
        }
        if(sender.hasPermission("ccorral.info")) {
            sender.sendMessage(ChatColor.WHITE + "/horse-info" + ChatColor.GRAY + " | Display owner and lock status of a horse.");
        }
    }
}
