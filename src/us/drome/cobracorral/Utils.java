package us.drome.cobracorral;

import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;

public class Utils {
    CobraCorral plugin;
    Configuration config;
    
    public Utils(CobraCorral plugin) {
        this.plugin = plugin;
        config = plugin.config;
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
            for(World world : plugin.getServer().getWorlds()) {
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
        return (config.HORSES.containsKey(horse.getUniqueId().toString()) ? true : false);
    }
    
    public boolean maxHorsesLocked(UUID playerID) {
        if(config.MAX_HORSES == 0) {
            return false;
        }
        int count = 0;
        for(String key : config.HORSES.keySet()) {
            if(config.HORSES.get(key).getOwner().equals(playerID)) {
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
    
    public String getOwnerName(String id) {
        return plugin.getServer().getOfflinePlayer(UUID.fromString(id)).getName();
    }
    
    public void helpDisplay(CommandSender sender) {
        sender.sendMessage(ChatColor.GRAY + "=======" + ChatColor.WHITE + "CobraCorral v" + plugin.getDescription().getVersion() +
            " Commands" + ChatColor.GRAY + "=======");
        if(sender.hasPermission("ccorral.lock")) {
            sender.sendMessage(ChatColor.WHITE + "/corral" + ChatColor.GRAY + " | Used to lock a horse you have tamed.");
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /horse-lock, /hlock");
            sender.sendMessage(ChatColor.WHITE + "/uncorral" + ChatColor.GRAY + " | Used to unlock a horse you have tamed.");
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /horse-unlock, /hunlock");
            sender.sendMessage(ChatColor.WHITE + "/testdrive" + ChatColor.GRAY + " | Temporarily allow others to ride a locked horse.");
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /horse-test, /htest");
        }
        if(sender.hasPermission("ccorral.list")) {
            sender.sendMessage(ChatColor.WHITE + "/horse-list" + ChatColor.GRAY + " | List all horses you have locked.");
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /hlist");
        }
        if(sender.hasPermission("ccorral.list-all")) {
            sender.sendMessage(ChatColor.WHITE + "/horse-list <player>" + ChatColor.GRAY + " | List horses owned by player.");
        }
        if(sender.hasPermission("ccorral.gps")) {
            sender.sendMessage(ChatColor.WHITE + "/horse-gps <horseID>" + ChatColor.GRAY + " | Get the location of a specified horse.");
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /hgps");
        }
        if(sender.hasPermission("ccorral.gps-all")) {
            sender.sendMessage(ChatColor.WHITE + "/horse-gps <player> <horseID>" + ChatColor.GRAY + " | Locate a player's horse.");
        }
        if(sender.hasPermission("ccorral.tp")) {
            sender.sendMessage(ChatColor.WHITE + "/horse-tp <player> <horseID>" + ChatColor.GRAY + " | Telelport a horse to you.");
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /htp");
        }
        if(sender.hasPermission("ccorral.info")) {
            sender.sendMessage(ChatColor.WHITE + "/horse-info" + ChatColor.GRAY + " | Display owner and lock status of a horse.");
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /hinfo");
        }
    }
}
