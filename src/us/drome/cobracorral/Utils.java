package us.drome.cobracorral;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.v1_8_R3.NBTBase;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftHorse;
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
    
    //Method to return command information to the player, based on their access to the commands via permissions.
    public void helpDisplay(CommandSender sender, String command) {
        //Attempt to parse a # or command name out of the provided command argument.
        int topic = 0;
        try {
            topic = Integer.parseInt(command);
        } catch (NumberFormatException e) {
            if(!command.startsWith("/") && !command.isEmpty()) {
                command = "/" + command;
            }
        }
        
        //A command list is built each time the help display is called, based on current player permissions and stored in this Map.
        Map<Integer,String> commands = new HashMap();
        int order = 1;
        if(sender.hasPermission("ccorral.lock")) {
            commands.put(order,"/corral"); order++;
            commands.put(order,"/uncorral"); order++;
            commands.put(order,"/horse-name"); order++;
            commands.put(order,"/testdrive"); order++;
            commands.put(order,"/horse-access"); order++;
        }
        if(sender.hasPermission("ccorral.free")) {
            commands.put(order,"/horse-free"); order++;
        }
        if(sender.hasPermission("ccorall.list")) {
            commands.put(order,"/horse-list"); order++;
        }
        if(sender.hasPermission("ccorall.gps")) {
            commands.put(order,"/horse-gps"); order++;
        }
        if(sender.hasPermission("ccorall.tp")) {
            commands.put(order,"/horse-tp"); order++;
        }
        if(sender.hasPermission("ccorall.info")) {
            commands.put(order,"/horse-info"); order++;
        }
        if(sender.hasPermission("ccorall.bypass")) {
            commands.put(order,"/horse-bypass"); order++;
        }
        
        //If the topic is 0 and command argument is empty, display the command list in 2 columns of 8 lines.
        if(topic == 0 && command.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "=======" + ChatColor.GOLD + "CobraCorral v" + plugin.getDescription().getVersion() +
                " Commands" + ChatColor.GRAY + "=======");
            ArrayList<String> output = new ArrayList();
            for(Integer index:commands.keySet()) {
                if(index > 8) {
                    String current = output.get(index - 9);
                    while(current.length() < 25) {
                        current = current.concat(" ");
                    }
                    output.set((index - 9),current + ChatColor.GRAY + index + ":" + ChatColor.GOLD + commands.get(index));
                } else {
                    output.add(ChatColor.GRAY + index.toString() + ":" + ChatColor.GOLD + commands.get(index));
                }
            }
            sender.sendMessage(output.toArray(new String[output.size()]));
            sender.sendMessage(ChatColor.GRAY + "Use " + ChatColor.GOLD + "/ccorral <#>" + ChatColor.GRAY + " to see detailed command help.");
        } else {
            //If a number was parsed from the command argument, replace the command argument with the command from the Map that matches the number.
            if(topic > 0) {
                command = commands.get(topic);
            }
            switch(command) {
                case "/corral":
                    sender.sendMessage(ChatColor.GRAY + "Command: " + ChatColor.GOLD + "/corral");
                    sender.sendMessage(ChatColor.GRAY + "Parameters: " + ChatColor.GOLD + "None.");
                    sender.sendMessage(ChatColor.WHITE + "Lock a horse that you have tamed, up to " + config.MAX_HORSES + " on this server.");
                    sender.sendMessage(ChatColor.GRAY + "Aliases: " + ChatColor.GOLD + "/horse-lock" + ChatColor.GRAY + "," + ChatColor.GOLD + "/hlock");
                    break;
                case "/uncorral":
                    sender.sendMessage(ChatColor.GRAY + "Command: " + ChatColor.GOLD + "/uncorral");
                    if(sender.hasPermission("ccorral.admin")) {
                        sender.sendMessage(ChatColor.DARK_GRAY + "Admin Parameters: " + ChatColor.GOLD + "<player> <horseID/name>");
                        sender.sendMessage(ChatColor.DARK_GRAY + "Remotely unlock any player's horse.");
                    } else {
                        sender.sendMessage(ChatColor.GRAY + "Parameters: " + ChatColor.GOLD + "None.");
                    }
                    sender.sendMessage(ChatColor.WHITE + "Unlock one of your horses. This does not untame the horse, but it can now be ridden by anyone.");
                    sender.sendMessage(ChatColor.GRAY + "Aliases: " + ChatColor.GOLD + "/horse-unlock" + ChatColor.GRAY + "," + ChatColor.GOLD + "/hunlock");
                    break;
                case "/horse-name":
                    sender.sendMessage(ChatColor.GRAY + "Command: " + ChatColor.GOLD + "/horse-name");
                    sender.sendMessage(ChatColor.GRAY + "Parameters: " + ChatColor.GOLD + "<nickname>");
                    sender.sendMessage(ChatColor.WHITE + "Sets a nickname on the horse. This can be used in place of the horse ID in other commands. It is overwritten if you name the horse with a nametag.");
                    sender.sendMessage(ChatColor.GRAY + "Aliases: " + ChatColor.GOLD + "/hname");
                    break;
                case "/testdrive":
                    sender.sendMessage(ChatColor.GRAY + "Command: " + ChatColor.GOLD + "/testdrive");
                    sender.sendMessage(ChatColor.GRAY + "Parameters: " + ChatColor.GOLD + "None.");
                    sender.sendMessage(ChatColor.WHITE + "Allows anyone to ride this locked horse until server restart or this command is used to turn it off.");
                    sender.sendMessage(ChatColor.GRAY + "Aliases: " + ChatColor.GOLD + "/horse-test" + ChatColor.GRAY + "," + ChatColor.GOLD + "/htest");
                    break;
                case "/horse-access":
                    sender.sendMessage(ChatColor.GRAY + "Command: " + ChatColor.GOLD + "/horse-access");
                    sender.sendMessage(ChatColor.GRAY + "Optional Parameters: " + ChatColor.GOLD + "<+/-><player>");
                    sender.sendMessage(ChatColor.WHITE + "With no parameters, this will list everyone who can ride the locked horse. To add a rider use the" +
                        ChatColor.GOLD + " + " + ChatColor.WHITE + "symbol followed by the player's name, or remove by using the" + ChatColor.GOLD + " - " + ChatColor.WHITE + "symbol instead.");
                    sender.sendMessage(ChatColor.GRAY + "Aliases: " + ChatColor.GOLD + "/haccess" + ChatColor.GRAY + "," + ChatColor.GOLD + "/hacl");
                    break;
                case "/horse-free":
                    sender.sendMessage(ChatColor.GRAY + "Command: " + ChatColor.GOLD + "/horse-free");
                    sender.sendMessage(ChatColor.GRAY + "Parameters: " + ChatColor.GOLD + "None.");
                    sender.sendMessage(ChatColor.WHITE + "Untames a horse that you own. This will also unlock the horse if necessary.");
                    sender.sendMessage(ChatColor.GRAY + "Aliases: " + ChatColor.GOLD + "/hfree");
                    break;
                case "/horse-list":
                    sender.sendMessage(ChatColor.GRAY + "Command: " + ChatColor.GOLD + "/horse-list");
                    if(sender.hasPermission("ccorral.listall")) {
                        sender.sendMessage(ChatColor.DARK_GRAY + "Admin Parameters: " + ChatColor.GOLD + "<player> <horseID/name>");
                        sender.sendMessage(ChatColor.DARK_GRAY + "List all horses locked by a specific player.");
                    }
                    sender.sendMessage(ChatColor.GRAY + "Optional Parameters: " + ChatColor.GOLD + "<page>");
                    sender.sendMessage(ChatColor.WHITE + "List all of the horses you have currently locked.");
                    sender.sendMessage(ChatColor.GRAY + "Aliases: " + ChatColor.GOLD + "/hlist");
                    break;
                case "/horse-gps":
                    sender.sendMessage(ChatColor.GRAY + "Command: " + ChatColor.GOLD + "/horse-gps");
                    if(sender.hasPermission("ccorral.gpsall")) {
                        sender.sendMessage(ChatColor.DARK_GRAY + "Admin Parameters: <player> <horseID/name>" );
                        sender.sendMessage(ChatColor.DARK_GRAY + "Locate any player's horse.");
                    }
                    sender.sendMessage(ChatColor.GRAY + "Parameters: " + ChatColor.GOLD + "<horseID/name>");
                    sender.sendMessage(ChatColor.WHITE + "Locate a locked horse by ID, nickname, or name as found in /horse-list.");
                    sender.sendMessage(ChatColor.GRAY + "Aliases:" + ChatColor.GOLD + " /hgps");
                    break;
                case "/horse-tp":
                    sender.sendMessage(ChatColor.GRAY + "Command: " + ChatColor.GOLD + "/horse-tp");
                    sender.sendMessage(ChatColor.GRAY + "Parameters: " + ChatColor.GOLD + "<player> <horseID/name>");
                    sender.sendMessage(ChatColor.WHITE + "Teleport any player's horse to your current location.");
                    sender.sendMessage(ChatColor.GRAY + "Aliases: " + ChatColor.GOLD + "/htp");
                    break;
                case "/horse-info":
                    sender.sendMessage(ChatColor.GRAY + "Command: " + ChatColor.GOLD + "/horse-info");
                    if(sender.hasPermission("ccorral.gpsall")) {
                        sender.sendMessage(ChatColor.DARK_GRAY + "Admin Parameters: <player> <horseID/name>" );
                        sender.sendMessage(ChatColor.DARK_GRAY + "Remotely show info for a specific horse.");
                    }
                    sender.sendMessage(ChatColor.WHITE + "Display information about the horse including ownership and stats.");
                    sender.sendMessage(ChatColor.GRAY + "Aliases: " + ChatColor.GOLD + "/hinfo");
                    break;
                case "/horse-bypass":
                    sender.sendMessage(ChatColor.GRAY + "Command: " + ChatColor.GOLD + "/horse-bypass");
                    sender.sendMessage(ChatColor.GRAY + "Parameters: " + ChatColor.GOLD + "None.");
                    sender.sendMessage(ChatColor.WHITE + "Bypass the lock on a horse for moderation purposes. Command toggles on/off.");
                    sender.sendMessage(ChatColor.GRAY + "Aliases: " + ChatColor.GOLD + "/hbypass");
                    break;
                default:
                    sender.sendMessage(ChatColor.GRAY + "Command Not Found.");
                    break;
            }
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
    
    //Implimented algorithm from Zyin's HUD for jump height calculations.
    public static double getJumpHeight(Horse horse) {
        //simulate gravity and air resistance to determine the jump height
    	double yVelocity = horse.getJumpStrength();	//horses's jump strength attribute
    	double jumpHeight = 0;
    	while (yVelocity > 0)
    	{
    		jumpHeight += yVelocity;
    		yVelocity -= 0.08;
    		yVelocity *= 0.98;
    	}
    	return jumpHeight;
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
