package us.drome.cobracorral;

import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;

public class Utils {
    CobraCorral plugin;
    Configuration config;
    
    public Utils(CobraCorral plugin) {
        this.plugin = plugin;
        config = plugin.config;
    }
    
    /*
    This method attempts to find and return a Horse entity matching the specified UUID.
    Using the cached location it attemps to load the chunk at that location and retrieve the entity.
    If it cannot be found in this chunk it attempts to search every loaded chunk in the world.
    Under the circumstance of no Horse being found, this method will return null.
    */
    public Horse getHorse(LockedHorse lhorse, UUID horseID) {
        Chunk toLoad = lhorse.getLocation(plugin).getChunk();
        Horse horse = null;
        if(!toLoad.isLoaded()) {
            toLoad.load();
        }
        for(Entity entity : toLoad.getEntities()) {
            if(entity instanceof Horse) {
                if(entity.getUniqueId().equals(horseID)) {
                    return (Horse)entity;
                }
            }
        }
        
        
        plugin.getLogger().info("Failed to load horse " + horseID + " from chunk at cached location " + lhorse.getLocation() +
            " for player " + getOwnerName(lhorse.getOwner()) + ", attempting seach of loaded chunks.");
        if(horse == null) {
            for(World world : plugin.getServer().getWorlds()) {
                for(Entity entity : world.getEntitiesByClass(Horse.class)) {
                    if(entity.getUniqueId().equals(horseID)) {
                        return (Horse)entity;
                    }
                }
            }
        }
        plugin.getLogger().info("Failed to load horse " + horseID + " for player " + getOwnerName(lhorse.getOwner()) +
            " from any loaded chunk, will use cached location.");
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
    
    public void lockHorse(Horse horse, UUID ownerID) {
        config.HORSES.put(horse.getUniqueId().toString(), new LockedHorse(horse, ownerID));
    }
    
    public void unlockHorse(UUID horseID) {
        config.HORSES.remove(horseID.toString());
    }
    
    public String getOwnerName(UUID playerID) {
        return plugin.getServer().getOfflinePlayer(playerID).getName();
    }
    
    //Clear all meta keys from the player
    public void clearMetaKeys(Player player) {
        if(player.hasMetadata(CobraCorral.HORSE_INFO)) {
            player.removeMetadata(CobraCorral.HORSE_INFO, plugin);
        } else if (player.hasMetadata(CobraCorral.HORSE_LOCK)) {
            player.removeMetadata(CobraCorral.HORSE_LOCK, plugin);
        } else if (player.hasMetadata(CobraCorral.HORSE_TEST_DRIVE)) {
            player.removeMetadata(CobraCorral.HORSE_TEST_DRIVE, plugin);
        } else if (player.hasMetadata(CobraCorral.HORSE_UNLOCK)) {
            player.removeMetadata(CobraCorral.HORSE_UNLOCK, plugin);
        } else if (player.hasMetadata(CobraCorral.HORSE_ACCESS)) {
            player.removeMetadata(CobraCorral.HORSE_ACCESS, plugin);
        } else if (player.hasMetadata(CobraCorral.HORSE_FREE)) {
            player.removeMetadata(CobraCorral.HORSE_FREE, plugin);
        }
    }
    
    //Check if player has any of the meta keys used for right click interaction.
    public boolean hasMetaKeys(Player player) {
        if(player.hasMetadata(CobraCorral.HORSE_INFO)) {
            return true;
        } else if (player.hasMetadata(CobraCorral.HORSE_LOCK)) {
            return true;
        } else if (player.hasMetadata(CobraCorral.HORSE_TEST_DRIVE)) {
            return true;
        } else if (player.hasMetadata(CobraCorral.HORSE_UNLOCK)) {
            return true;
        } else if (player.hasMetadata(CobraCorral.HORSE_ACCESS)) {
            return true;
        } else if (player.hasMetadata(CobraCorral.HORSE_FREE)) {
            return true;
        } else {
            return false;
        }
    }
    
    
    //Parsed command help to return to the player/console based on permissions.
    public void helpDisplay(CommandSender sender) {
        sender.sendMessage(ChatColor.GRAY + "=======" + ChatColor.WHITE + "CobraCorral v" + plugin.getDescription().getVersion() +
            " Commands" + ChatColor.GRAY + "=======");
        if(sender.hasPermission("ccorral.lock")) {
            sender.sendMessage(ChatColor.WHITE + "/corral" + ChatColor.GRAY + " | Used to lock a horse you have tamed.");
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /horse-lock, /hlock");
            sender.sendMessage(ChatColor.WHITE + "/uncorral" + ChatColor.GRAY + " | Used to unlock a horse you have tamed.");
            if(sender.hasPermission("ccorral.admin")) {
               sender.sendMessage(ChatColor.WHITE + "/uncorral <player> <horseID>" + ChatColor.GRAY + " | Remotely unlock a specific horse."); 
            }
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /horse-unlock, /hunlock");
            sender.sendMessage(ChatColor.WHITE + "/testdrive" + ChatColor.GRAY + " | Temporarily allow others to ride a locked horse.");
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /horse-test, /htest");
            sender.sendMessage(ChatColor.WHITE + "/horse-access: <+/-><player>" + ChatColor.GRAY + " | List, add(+), or remove(-) horse access.");
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /haccess, /hacl");
        }
        if(sender.hasPermission("ccorral.free")) {
            sender.sendMessage(ChatColor.WHITE + "/horse-free" + ChatColor.GRAY + " | Set free any unlocked horse you own.");
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /hfree");
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
            sender.sendMessage(ChatColor.WHITE + "/horse-tp <player> <horseID>" + ChatColor.GRAY + " | Teleport a horse to you.");
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /htp");
        }
        if(sender.hasPermission("ccorral.info")) {
            sender.sendMessage(ChatColor.WHITE + "/horse-info" + ChatColor.GRAY + " | Display owner and lock status of a horse.");
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /hinfo");
        }
    }
}
