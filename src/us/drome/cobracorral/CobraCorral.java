package us.drome.cobracorral;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class CobraCorral extends JavaPlugin {
    public final Configuration config = new Configuration(this);
    public final Utils utils = new Utils(this);
    
    //Horse metadata keys.
    public static final String HORSE_TEST_DRIVE = "CobraCorral.test_drive";
    public static final String HORSE_INFO = "CobraCorral.info";
    public static final String HORSE_LOCK = "CobraCorral.lock";
    public static final String HORSE_UNLOCK = "CobraCorral.unlock";
    public static final String HORSE_ACCESS = "CobraCorral.access";
    public static final String HORSE_FREE = "CobraCorral.free";
    
    public void onDisable() {
        getLogger().info("version " + getDescription().getVersion() + " has begun unloading...");
        config.save();
        getLogger().info(" has saved " + config.HORSES.size() + " locked horses.");
        getLogger().info("version " + getDescription().getVersion() + " has finished unloading.");
    }
    
    public void onEnable() {
        getLogger().info("version " + getDescription().getVersion() + " has begun loading...");
        (new CorralListener(this)).registerEvents();
        ConfigurationSerialization.registerClass(LockedHorse.class);
        File configFile = new File(getDataFolder(), "config.yml");
        if(!configFile.exists()) {
            getConfig().options().copyDefaults(true);
            saveConfig();
        }
        config.load();
        getLogger().info(" has loaded " + config.HORSES.size() + " locked horses.");
        getLogger().info("version " + getDescription().getVersion() + " has finished loading.");
    }
    
    
    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        switch(cmd.getName().toLowerCase()) {
            case "ccorral":
                if(args.length > 0) {
                    if(args[0].equalsIgnoreCase("reload") && sender.hasPermission("ccorral.admin")) {
                        config.reload();
                        sender.sendMessage(ChatColor.LIGHT_PURPLE + "[CobraCorral] Config Reloaded");
                    } else
                        utils.helpDisplay(sender);
                } else
                    utils.helpDisplay(sender);
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
                    if(args.length > 1 && sender.hasPermission("ccorral.admin")) {
                        UUID playerID = getServer().getOfflinePlayer(args[0]).getUniqueId();
                        int target;
                        int count = 0;
                        try {
                            target = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.GRAY + "Horse ID provided: " + args[1] + ", is not a valid integer.");
                            return false;
                        }
                        for(String key : config.HORSES.keySet()) {
                            if(config.HORSES.get(key).getOwner().equals(playerID)) {
                                count++;
                                if(count == target) {
                                    LockedHorse lhorse = config.HORSES.get(key);
                                    config.HORSES.remove(key);
                                    ((Player)sender).playSound(((Player)sender).getLocation(), Sound.CLICK, 1f, 1f);
                                    ((Player)sender).playSound(((Player)sender).getLocation(), Sound.ANVIL_USE, 1f, 1f);
                                    getLogger().info(((Player)sender).getName() + " unlocked " + getServer().getOfflinePlayer(lhorse.getOwner()).getName() +
                                        "'s horse " + lhorse.getName() + " with UUID " + key);
                                    sender.sendMessage(ChatColor.GRAY + args[0] + "'s horse " + lhorse.getName() + " has been unlocked.");
                                }
                            }
                        }
                    } else {
                        ((Player)sender).setMetadata(HORSE_UNLOCK, new FixedMetadataValue(this, null));
                        sender.sendMessage(ChatColor.GRAY + "Right click on a Horse that you own.");
                    }
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
            case "horse-access":
                if(sender instanceof Player) {
                    if(args.length > 0) {
                        Character grantRevoke = args[0].charAt(0);
                        if((grantRevoke == '+' || grantRevoke == '-') && args[0].length() > 1) {
                            if(!getServer().getOfflinePlayer(args[0].substring(1)).hasPlayedBefore()) {
                                sender.sendMessage(ChatColor.GRAY + "You must specify a player that has logged on before.");
                                return true;
                            } else {
                                String data = grantRevoke.toString() + getServer().getOfflinePlayer(args[0].substring(1)).getUniqueId().toString();
                                ((Player)sender).setMetadata(HORSE_ACCESS, new FixedMetadataValue(this, data));
                                sender.sendMessage(ChatColor.GRAY + "Right click on a Horse that you own.");
                            }
                        } else {
                            return false;
                        }
                    } else {
                        ((Player)sender).setMetadata(HORSE_ACCESS, new FixedMetadataValue(this, null));
                        sender.sendMessage(ChatColor.GRAY + "Right click on a Horse that you own.");
                    }
                } else {
                    sender.sendMessage("That command can only be ran by a Player.");
                }
            break;
            case "horse-free":
                if(sender instanceof Player) {
                    ((Player)sender).setMetadata(HORSE_FREE, new FixedMetadataValue(this, null));
                    sender.sendMessage(ChatColor.GRAY + "Right click on a Horse that you own.");
                } else {
                    sender.sendMessage("That command can only be ran by a Player.");
                }
                break;
            case "horse-list":
                if(sender instanceof Player) {
                   UUID playerID = ((Player)sender).getUniqueId();
                    if(args.length > 0 && (sender.hasPermission("ccorral.list-all") || sender.hasPermission("ccorral.admin"))) {
                        playerID = getServer().getOfflinePlayer(args[0]).getUniqueId();
                    }
                    List<String> response = new ArrayList<>();

                    int count = 0;
                    for(String key : config.HORSES.keySet()) {
                        if(config.HORSES.get(key).getOwner().equals(playerID)) {
                            count++;
                            LockedHorse lhorse = config.HORSES.get(key);
                            Horse horse = utils.getHorse(lhorse, UUID.fromString(key));
                            if(horse != null) {
                                config.HORSES.put(key, lhorse.updateHorse(horse));
                            }
                            response.add(String.valueOf(count) + ChatColor.GRAY + " | " + lhorse.getName() + " | " + lhorse.getAppearance() +
                                " | " + lhorse.getArmor() + " | " + lhorse.getWorld());
                        }
                    }
                    
                    if(!response.isEmpty()) {
                        response.add(0, ChatColor.GRAY + "Horses locked by " +
                            (playerID.equals(((Player)sender).getUniqueId()) ? "you" : utils.getOwnerName(playerID)) + ":");
                        for(String line : response) {
                            sender.sendMessage(line);
                        }
                    } else {
                        sender.sendMessage(ChatColor.GRAY + "There are no horses locked by " +
                            (playerID.equals(((Player)sender).getUniqueId()) ? "you" : utils.getOwnerName(playerID)) + ".");
                    }                             
                } else {
                    sender.sendMessage("That command can only be ran by a Player.");
                }
                break;
            case "horse-gps":
                if(sender instanceof Player) {
                    if(args.length > 0) {
                        UUID playerID = ((Player)sender).getUniqueId();
                        int target = 0;
                        int count = 0;
                        
                        if(args.length > 1) {
                            if(sender.hasPermission("ccorral.gps-all") || sender.hasPermission("ccorral.admin")) {
                                if(getServer().getOfflinePlayer(args[0]).hasPlayedBefore()) {
                                    playerID = getServer().getOfflinePlayer(args[0]).getUniqueId();
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
                                return true;
                            }   
                        } else {
                            try {
                                target = Integer.parseInt(args[0]);
                            } catch (NumberFormatException e) {
                                sender.sendMessage(ChatColor.GRAY + "Horse ID provided: " + args[0] + ", is not a valid integer.");
                                return false;
                            }
                        }
                        
                        for(String key : config.HORSES.keySet()) {
                            if(config.HORSES.get(key).getOwner().equals(playerID)) {
                                count++;
                                if(count == target) {
                                    LockedHorse lhorse = config.HORSES.get(key);
                                    Horse horse = utils.getHorse(lhorse, UUID.fromString(key));
                                    if(horse != null) {
                                        config.HORSES.put(key, lhorse.updateHorse(horse));
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
                if(sender instanceof Player) {
                    if(args.length > 1) {
                        UUID playerID;
                        int target = 0;
                        int count = 0;

                        if(getServer().getOfflinePlayer(args[0]).hasPlayedBefore()) {
                            playerID = getServer().getOfflinePlayer(args[0]).getUniqueId();
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
                            if(config.HORSES.get(key).getOwner().equals(playerID)) {
                                count++;
                                if(count == target) {
                                    LockedHorse lhorse = config.HORSES.get(key);
                                    Horse horse = utils.getHorse(lhorse, UUID.fromString(key));
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
                                            String passenger = horse.getPassenger() instanceof Player ? ((Player)horse.getPassenger()).getName() : horse.getPassenger().toString();
                                            sender.sendMessage(ChatColor.GRAY + lhorse.getName() + " " + lhorse.getAppearance() +
                                                " is being ridden by " + passenger + " and can't be teleported.");
                                        }

                                    } else {
                                        sender.sendMessage(ChatColor.GRAY + "That horse failed to load, please try again.");
                                        getLogger().info("Failed to load horse " + key + " for player " + utils.getOwnerName(playerID) + ", cancelling teleport.");
                                    }
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
}
