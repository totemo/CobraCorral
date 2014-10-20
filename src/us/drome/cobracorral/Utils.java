package us.drome.cobracorral;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.v1_7_R4.EntityHorse;
import net.minecraft.server.v1_7_R4.NBTBase;
import net.minecraft.server.v1_7_R4.NBTTagCompound;
import net.minecraft.server.v1_7_R4.NBTTagList;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftHorse;
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
    public Horse getHorse(LockedHorse lhorse) {
        Chunk toLoad = lhorse.getLocation(plugin).getChunk();
        Horse horse = null;
        if(!toLoad.isLoaded()) {
            toLoad.load();
        }
        for(Entity entity : toLoad.getEntities()) {
            if(entity instanceof Horse) {
                if(entity.getUniqueId().equals(lhorse.getUUID())) {
                    return (Horse)entity;
                }
            }
        }
        
        
        plugin.getLogger().info("Failed to load horse " + lhorse.getUUID() + " from chunk at cached location " + lhorse.getLocation() +
            " for player " + getOwnerName(lhorse.getOwner()) + ", attempting seach of loaded chunks.");
        if(horse == null) {
            for(World world : plugin.getServer().getWorlds()) {
                for(Entity entity : world.getEntitiesByClass(Horse.class)) {
                    if(entity.getUniqueId().equals(lhorse.getUUID())) {
                        return (Horse)entity;
                    }
                }
            }
        }
        plugin.getLogger().info("Failed to load horse " + lhorse.getUUID() + " for player " + getOwnerName(lhorse.getOwner()) +
            " from any loaded chunk, will use cached location.");
        return horse;
    }
    
    public boolean isHorseLocked(Horse horse) {
        return (config.Database.contains(horse.getUniqueId()));
    }
    
    public boolean maxHorsesLocked(UUID playerID) {
        if(config.MAX_HORSES == 0) {
            return false;
        }
        int count = 0;
        Set<LockedHorse> horses = config.Database.getHorses(playerID);
        Iterator<LockedHorse> horseIterator = horses.iterator();
        while(horseIterator.hasNext()) {
            if(horseIterator.next().getOwner().equals(playerID)) {
                count++;
            }
        }
        if(count >= config.MAX_HORSES) //check for > just in case.
            return true;
        else
            return false;
    }
    
    public void lockHorse(Horse horse, UUID ownerID) {
        config.Database.addHorse(horse, ownerID);
    }
    
    public void unlockHorse(UUID horseID) {
        config.Database.removeHorse(horseID);
    }
    
    public void updateHorse(LockedHorse lhorse, Horse horse) {
        lhorse.updateHorse(horse);
        config.Database.updateHorse(lhorse);
    }
    
    public boolean grantAccess(LockedHorse lhorse, UUID playerID) {
        if(lhorse.grantAccess(playerID)) {
            config.Database.addAccess(lhorse.getUUID(), playerID);
            return true;
        }
        return false;
    }
    
    public boolean revokeAccess(LockedHorse lhorse, UUID playerID) {
        if(lhorse.revokeAccess(playerID)) {
            config.Database.removeAccess(lhorse.getUUID(), playerID);
            return true;
        }
        return false;
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
        } else if (player.hasMetadata(CobraCorral.HORSE_NAME)) {
            player.removeMetadata(CobraCorral.HORSE_NAME, plugin);
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
        } else if (player.hasMetadata(CobraCorral.HORSE_NAME)) {
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
               sender.sendMessage(ChatColor.WHITE + "/uncorral <player> <horseID/name>" + ChatColor.GRAY + " | Remotely unlock a specific horse."); 
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
            sender.sendMessage(ChatColor.WHITE + "/horse-list <page>" + ChatColor.GRAY + " | List all horses you have locked.");
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /hlist");
        }
        if(sender.hasPermission("ccorral.list-all")) {
            sender.sendMessage(ChatColor.WHITE + "/horse-list <player>" + ChatColor.GRAY + " | List horses owned by player.");
        }
        if(sender.hasPermission("ccorral.gps")) {
            sender.sendMessage(ChatColor.WHITE + "/horse-gps <horse ID/name>" + ChatColor.GRAY + " | Get the location of a specified horse.");
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /hgps");
        }
        if(sender.hasPermission("ccorral.gps-all")) {
            sender.sendMessage(ChatColor.WHITE + "/horse-gps <player> <horseID/name>" + ChatColor.GRAY + " | Locate a player's horse.");
        }
        if(sender.hasPermission("ccorral.tp")) {
            sender.sendMessage(ChatColor.WHITE + "/horse-tp <player> <horseID/name>" + ChatColor.GRAY + " | Teleport a horse to you.");
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /htp");
        }
        if(sender.hasPermission("ccorral.info")) {
            sender.sendMessage(ChatColor.WHITE + "/horse-info" + ChatColor.GRAY + " | Display horse info & stats.");
            if(sender.hasPermission("ccorral.admin")) {
                sender.sendMessage(ChatColor.WHITE + "/horse-info <player> <horseID/name>" + ChatColor.GRAY + " | Display horse UUID info.");
            }
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /hinfo");
        }
        if(sender.hasPermission("ccorral.bypass")) {
            sender.sendMessage(ChatColor.WHITE + "/horse-bypass" + ChatColor.GRAY + " | Toggle horse access bypass.");
            sender.sendMessage(ChatColor.WHITE + "    aliases:" + ChatColor.GRAY + " /hbypass");
        }
    }
    
    //Parse inputed string list into a page of no more than 10 lines which is the max capacity of MC chat
    public List<String> pagifyOutput(List<String> unparsed, int page) {
        page = (page == 0) ? 1 : page;
        int maxPage = (int)Math.ceil(unparsed.size() / 9d);
        page = (page > maxPage) ? maxPage : page;
        List<String> pagified = new ArrayList<>();
        
        if((unparsed.size() / 9) >= (page - 1)) {
            int firstOnPage = (page - 1) * 9;
            pagified = unparsed.subList(firstOnPage, firstOnPage + ((unparsed.size() / 9 < page) ? (unparsed.size() % 9) : 9));
            if(page > 1) {
                pagified.add(0, unparsed.get(0)); //Add the header.
            }
            pagified.add(ChatColor.GRAY + "Displaying page " + page + " of " + maxPage); //Add the footer.
        }
        
        return pagified;
    }
    
    //Code implimented from https://github.com/RedPanda4552/HorseStats to acquire speed from NMS
    public static double getSpeed(Horse horse) {
        CraftHorse cHorse = (CraftHorse) horse;
        NBTTagCompound compound = new NBTTagCompound();
        cHorse.getHandle().b(compound);
        double speed = -1;
        NBTTagList list = (NBTTagList) compound.get("Attributes");
        for(int i = 0; i < list.size() ; i++) {
            NBTBase base = list.get(i);
            if (base.getTypeId() == 10) {
                NBTTagCompound attrCompound = (NBTTagCompound)base;
                if (base.toString().contains("generic.movementSpeed")) {
                    speed = attrCompound.getDouble("Base");
                }
            }   
        }
        return speed * 43;
    }
    
    //Custom function to teleport Horses, this allows for cross-world teleportation. Passengers do not work with this, but /horse-tp already will not teleport horses with riders anyway.
    public static void teleportHorse(Horse horse, Location toHere) {
        EntityHorse ehorse = ((CraftHorse)horse).getHandle();
        net.minecraft.server.v1_7_R4.World toWorld = ((CraftWorld)toHere.getWorld()).getHandle();
        if(ehorse.world != toWorld) {
            ehorse.world.removeEntity(ehorse);
            ehorse.dead = false;
            ehorse.world = toWorld;
            ehorse.setLocation(toHere.getX(), toHere.getY(), toHere.getZ(), toHere.getYaw(), toHere.getPitch());
            ehorse.world.addEntity(ehorse);
        } else {
            ehorse.getBukkitEntity().teleport(toHere);
        }

    }
    
    public static boolean nameChecker(String name, String check) {
        String tempName = ((name.startsWith("\"") && name.endsWith("\"")) && !(check.startsWith("\"") && check.endsWith("\""))) ? name.substring(1, name.length() -1) : name;
        if(tempName.equalsIgnoreCase(check)) {
            return true;
        } else {
            return false;
        }
    }
}
