package us.drome.cobracorral;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
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
    
    //Horse metadata keys.
    public static final String HORSE_TEST_DRIVE = "CobraCorral.test_drive";
    public static final String HORSE_INFO = "CobraCorral.info";
    public static final String HORSE_LOCK = "CobraCorral.lock";
    public static final String HORSE_UNLOCK = "CobraCorral.unlock";
    
    public void OnDisable() {
        getLogger().info("version " + getDescription().getVersion() + " has been unloaded.");
        config.save();
    }
    
    public void OnEnable() {
        getLogger().info("version " + getDescription().getVersion() + " has begun loading...");
        File configFile = new File(getDataFolder(), "config.yml");
        if(!configFile.exists()) {
            getConfig().options().copyDefaults();
            saveConfig();
        }
        
        config.load();
    }
    
    
    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        switch(cmd.getName().toLowerCase()) {
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
                    List<UUID> horseID = new ArrayList<>();
                    List<String> response = new ArrayList<>();

                    for(UUID key : config.HORSES.keySet()) {
                        if(config.HORSES.containsValue(player)) {
                            horseID.add(key);
                        }
                    }

                    if(horseID.size() > 0) {
                        List<Horse> horses = getHorses(horseID);
                        response.add(ChatColor.GRAY + "Horses locked by " +
                            (player.equalsIgnoreCase(sender.getName()) ? "you" : player) + ":");
                        
                        
                        /**
                         * Iterate through each Horse entity reference and generate a line in the response.
                         * # | Name | Color & Style or Type | Armor | World
                         */
                        for(Horse horse : horses) {
                            response.add(ChatColor.GRAY + String.valueOf(horses.indexOf(horse)) + " | " +
                                    horse.getCustomName() != null ? horse.getCustomName() : "No Name" + " | " +
                                    ((horse.getVariant() == Horse.Variant.HORSE) ? horse.getColor().toString() +
                                        " " + horse.getStyle().toString() : horse.getVariant().toString()) + " | " +
                                    (horse.getInventory().getArmor() != null ? horse.getInventory().getArmor().getType().toString() :
                                        "No Armor") + " | " +
                                    horse.getWorld().getName());
                        }

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
                        int count = 1;
                        
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

                        for(UUID key : config.HORSES.keySet()) {
                            if(config.HORSES.containsValue(pName)) {
                                if(count == target) {
                                    horseID.add(key);
                                    break;
                                }
                                count++;
                            }
                        }

                        if(horseID.size() > 0) {
                            Player player = (Player)sender;
                            List<Horse> horses = getHorses(horseID);
                            Horse horse = horses.get(1);

                            Location playerLoc = player.getLocation();
                            Location horseLoc = horse.getLocation();
                            if(!player.isInsideVehicle() && player.getWorld().equals(horse.getWorld())) {
                                Vector vector = horseLoc.toVector().subtract(playerLoc.toVector());
                                player.getLocation().setDirection(vector);
                            }
                            player.playSound(playerLoc, Sound.ARROW_HIT, 1f , 1f);
                            player.sendMessage(ChatColor.GRAY +
                                horse.getCustomName() != null ? horse.getCustomName() : horse.getVariant().toString() +
                                " Located @ X:" + String.valueOf(horseLoc.getX()) + " Y:" + String.valueOf(horseLoc.getY()) +
                                " Z:" + String.valueOf(horseLoc.getZ() + " World:" + horseLoc.getWorld().getName()));
                        } else {
                            sender.sendMessage(ChatColor.GRAY + "No horse found by that ID.");
                        }
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
                    int count = 1;
                    
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
                    
                    for(UUID key : config.HORSES.keySet()) {
                        if(config.HORSES.containsValue(pName)) {
                            if(count == target) {
                                horseID.add(key);
                                break;
                            }
                            count++;
                        }
                    }

                    if(horseID.size() > 0) {
                        Player player = (Player)sender;
                        List<Horse> horses = getHorses(horseID);
                        Horse horse = horses.get(1);
                        horse.teleport(player);
                        player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1f , 1f);
                        player.sendMessage(ChatColor.GRAY +
                            horse.getCustomName() != null ? horse.getCustomName() : horse.getVariant().toString() +
                            " #" + target + " has been teleported to your location!");
                    } else {
                        sender.sendMessage(ChatColor.GRAY + "No horse found by that ID.");
                    }
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
     * Function iterates through each world's Horse entities and attempts to match the entity UUIDs
     * supplied by the parameter 'id'. It returns a list of Horse entity references that match.
     */
    public List<Horse> getHorses(List<UUID> ids) {
        List<Horse> horses = new ArrayList<>();
        
        for(World world : getServer().getWorlds()) {
            for(Entity horse : world.getEntitiesByClasses(Horse.class)) {
                for(UUID id : ids) {
                    if(horse.getUniqueId().equals(id)) {
                        horses.add((Horse)horse);
                    }
                }
            }
        }
        
        return horses;
    }
    
    public boolean isHorseLocked(Horse horse) {
        for(UUID key : config.HORSES.keySet()) {
           if(horse.getUniqueId().equals(key))
               return true;
        }
        return false;
    }
    
    public boolean maxHorsesLocked(String player) {
        int count = 0;
        
        for(UUID key : config.HORSES.keySet()) {
            if(config.HORSES.get(key).equalsIgnoreCase(player)) {
                count++;
            }
        }
        
        if(count > 1)
            return true;
        else
            return false;
    }
    
    public void lockHorse(UUID id, String player) {
        config.HORSES.put(id, player);
    }
    
    public void unlockHorse(UUID id) {
        config.HORSES.remove(id);
    }
}
