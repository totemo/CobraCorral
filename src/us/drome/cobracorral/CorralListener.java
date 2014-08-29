package us.drome.cobracorral;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class CorralListener implements Listener {
    CobraCorral plugin;
    Configuration config;
    Utils utils;
    
    private List<UUID> onCooldown = new ArrayList<>();
    
    CorralListener(CobraCorral plugin) {
        this.plugin = plugin;
        config = plugin.config;
        utils = plugin.utils;
    }
    
    public void registerEvents() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if(event.getRightClicked() instanceof Horse) {
            Horse horse = (Horse)event.getRightClicked();
            
            //If the horse is a locked horse, update it's cache to be current.
            if(utils.isHorseLocked(horse)) {
                config.HORSES.get(horse.getUniqueId().toString()).updateHorse(horse);
            }
            
            /*
            Auto-unlocker Code
            If the horse has no owner and this horse is still locked, remove it.
            This is to support non-CobraCorral implemented untame commands.
            */
            if(horse.getOwner() == null && utils.isHorseLocked(horse)) {
                utils.unlockHorse(horse.getUniqueId());
                horse.setTamed(false);
                horse.setCustomName(null);
                plugin.getLogger().info("Auto-unlocked " + (horse.getCustomName() != null ?
                    horse.getCustomName() : horse.getVariant().toString()) + " with UUID " + horse.getUniqueId().toString() +
                        " due to the horse having no owner.");
            }
            
            /*
            HORSE_INFO Method
            Display current horse information including owner, lock status, and it's UUID.
            */
            if(player.hasMetadata(CobraCorral.HORSE_INFO)) {
                String owner = (horse.getOwner() != null ? horse.getOwner().getName() : "None");
                String status = (utils.isHorseLocked(horse) ? "Locked" : "Not Locked");
                status = (horse.hasMetadata(CobraCorral.HORSE_TEST_DRIVE) ? "Test Drive" : status);
                String UUID = horse.getUniqueId().toString();
                player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1f);
                player.sendMessage("Owner:" + ChatColor.GRAY + owner + ChatColor.RESET + " Status:" +
                    ChatColor.GRAY + status + ChatColor.RESET + " UUID:" + ChatColor.GRAY + UUID);
                utils.clearMetaKeys(player);
                event.setCancelled(true);
            } else if (horse.isTamed() && horse.getOwner() != null) {
                /*
                HORSE_LOCK Method
                */
                if(player.hasMetadata(CobraCorral.HORSE_LOCK)) {
                    if(player.equals(horse.getOwner()) || player.hasPermission("ccorral.admin")) {
                        if(!utils.maxHorsesLocked(horse.getOwner().getUniqueId())) {
                            if(!utils.isHorseLocked(horse)) {
                                if(!config.PROTECT_CHESTS && horse.getVariant().equals(Horse.Variant.DONKEY) || horse.getVariant().equals(Horse.Variant.MULE) && horse.isCarryingChest()) {
                                    player.sendMessage(ChatColor.GRAY + "You are not allowed to lock a " + horse.getVariant().name().toLowerCase() + " with a chest.");
                                } else {
                                    utils.lockHorse(horse, horse.getOwner().getUniqueId());
                                    player.sendMessage(ChatColor.GRAY + (horse.getCustomName() != null ?
                                        horse.getCustomName() : horse.getVariant().toString()) + " has been locked.");
                                    player.playSound(player.getLocation(), Sound.CLICK, 1f, 1f);
                                    plugin.getLogger().info(player.getName() + " locked " + (horse.getCustomName() != null ?
                                        horse.getCustomName() : horse.getVariant().toString()) + " with UUID " + horse.getUniqueId().toString());
                                }
                            } else {
                                player.sendMessage(ChatColor.GRAY + (horse.getCustomName() != null ?
                                    horse.getCustomName() : horse.getVariant().toString()) + " is already locked.");
                                player.playSound(player.getLocation(), Sound.ITEM_BREAK, 1f, 1f);
                            }
                        } else {
                            player.sendMessage(ChatColor.GRAY + "You cannot lock any more horses.");
                        }
                    } else {
                        player.sendMessage(ChatColor.GRAY + "You do not own that horse.");
                        player.playSound(player.getLocation(), Sound.ITEM_BREAK, 1f, 1f); 
                    }
                    utils.clearMetaKeys(player); //Clear any meta keys off the player so they can interact normally.
                    event.setCancelled(true);
                /*
                HORSE_TEST_DRIVE Method    
                */
                } else if(player.hasMetadata(CobraCorral.HORSE_TEST_DRIVE)) {
                    if(player.equals(horse.getOwner())) {
                        if(utils.isHorseLocked(horse)) {
                            if(horse.hasMetadata(CobraCorral.HORSE_TEST_DRIVE)) {
                                horse.removeMetadata(CobraCorral.HORSE_TEST_DRIVE, plugin);
                                player.sendMessage(ChatColor.GRAY + (horse.getCustomName() != null ?
                                    horse.getCustomName() : horse.getVariant().toString()) + " has been set to locked mode.");
                                player.playSound(player.getLocation(), Sound.CLICK, 1f, 1f);
                                player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1f);
                                plugin.getLogger().info(player.getName() + " disabled testdrive for " + (horse.getCustomName() != null ?
                                    horse.getCustomName() : horse.getVariant().toString()) + " with UUID " + horse.getUniqueId().toString());
                                if(horse.getPassenger() != null) {
                                    if(horse.getPassenger() instanceof Player) {
                                        Player passenger = (Player)horse.getPassenger();
                                        passenger.sendMessage(ChatColor.GRAY + "Your test drive has been cancelled.");
                                        passenger.playSound(passenger.getLocation(), Sound.HORSE_SADDLE, 1f, 1f);
                                    }
                                    horse.eject();
                                }
                            } else {
                                horse.setMetadata(CobraCorral.HORSE_TEST_DRIVE, new FixedMetadataValue(plugin, null));
                                player.sendMessage(ChatColor.GRAY + (horse.getCustomName() != null ?
                                    horse.getCustomName() : horse.getVariant().toString()) + " has been set to test drive mode.");
                                player.playSound(player.getLocation(), Sound.CLICK, 1f, 1f);
                                player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1f);
                                plugin.getLogger().info(player.getName() + " enabled testdrive for " + (horse.getCustomName() != null ?
                                    horse.getCustomName() : horse.getVariant().toString()) + " with UUID " + horse.getUniqueId().toString());
                            }
                        } else {
                            player.sendMessage(ChatColor.GRAY + (horse.getCustomName() != null ?
                                horse.getCustomName() : horse.getVariant().toString()) + " is not locked.");
                            player.playSound(player.getLocation(), Sound.ITEM_BREAK, 1f, 1f);
                        }
                    } else {
                        player.sendMessage(ChatColor.GRAY + "You do not own that horse.");
                        player.playSound(player.getLocation(), Sound.ITEM_BREAK, 1f, 1f); 
                    }
                    utils.clearMetaKeys(player);
                    event.setCancelled(true);
                /*
                HORSE_UNLOCK Method    
                */
                } else if(player.hasMetadata(CobraCorral.HORSE_UNLOCK)) {
                    if(player.equals(horse.getOwner()) || player.hasPermission("ccorral.admin")) {
                        if(utils.isHorseLocked(horse)) {
                            utils.unlockHorse(horse.getUniqueId());
                            player.sendMessage(ChatColor.GRAY + (horse.getCustomName() != null ?
                                horse.getCustomName() : horse.getVariant().toString())+ " has been unlocked.");
                            player.playSound(player.getLocation(), Sound.CLICK, 1f, 1f);
                            player.playSound(player.getLocation(), Sound.ANVIL_USE, 1f, 1f);
                            plugin.getLogger().info(player.getName() + " unlocked " + horse.getOwner().getName() + "'s horse " +
                                (horse.getCustomName() != null ? horse.getCustomName() : horse.getVariant().toString()) +
                                " with UUID " + horse.getUniqueId().toString());
                        } else {
                            player.sendMessage(ChatColor.GRAY + (horse.getCustomName() != null ?
                                horse.getCustomName() : horse.getVariant().toString()) + " is not locked.");
                            player.playSound(player.getLocation(), Sound.ITEM_BREAK, 1f, 1f);
                        }
                    } else {
                        player.sendMessage(ChatColor.GRAY + "You do not own that horse.");
                        player.playSound(player.getLocation(), Sound.ITEM_BREAK, 1f, 1f); 
                    }
                    utils.clearMetaKeys(player);
                    event.setCancelled(true);
                /*
                HORSE_FREE Method    
                */
                } else if (player.hasMetadata(CobraCorral.HORSE_FREE)) {
                    if(player.equals(horse.getOwner()) || player.hasPermission("ccorral.admin")) {
                        if(utils.isHorseLocked(horse)) {
                            player.sendMessage(ChatColor.GRAY + "That horse must be unlocked before it can be set free.");
                        } else {
                            plugin.getLogger().info(player.getName() + " set free " + horse.getOwner().getName() + "'s horse " +
                                horse.getVariant().toString() + " with UUID " + horse.getUniqueId().toString());
                            for(ItemStack item : horse.getInventory().getContents()) {
                                if(item != null) {
                                    horse.getWorld().dropItemNaturally(horse.getLocation(), item);
                                    horse.getInventory().remove(item);
                                }
                            }
                            if(horse.isCarryingChest()) {
                                horse.setCarryingChest(false);
                                horse.getWorld().dropItemNaturally(horse.getLocation(), new ItemStack(Material.CHEST));
                            }
                            horse.setOwner(null);
                            horse.setTamed(false);
                            horse.setCustomName(null);
                            player.sendMessage(ChatColor.GRAY + horse.getVariant().toString() + " has been set free.");
                            player.playSound(player.getLocation(), Sound.CLICK, 1f, 1f);
                            player.playSound(player.getLocation(), Sound.ANVIL_USE, 1f, 1f);
                        }
                    } else {
                        player.sendMessage(ChatColor.GRAY + "You do not own that horse.");
                        player.playSound(player.getLocation(), Sound.ITEM_BREAK, 1f, 1f); 
                    }
                    utils.clearMetaKeys(player);
                    event.setCancelled(true);
                /*
                HORSE_ACCESS Method    
                */
                } else if (player.hasMetadata(CobraCorral.HORSE_ACCESS)) {
                    if(player.equals(horse.getOwner()) || player.hasPermission("ccorral.admin")) {
                        if(utils.isHorseLocked(horse)) {
                            if(player.getMetadata(CobraCorral.HORSE_ACCESS).get(0).asString().isEmpty()) {
                                LockedHorse lhorse = config.HORSES.get(horse.getUniqueId().toString());
                                List<String> response = new ArrayList<>();
                                response.add(ChatColor.GRAY + "=======" + ChatColor.WHITE + "Access List for " + lhorse.getName() + ChatColor.GRAY + "=======");
                                response.add(ChatColor.GOLD + "Owner: " + plugin.getServer().getOfflinePlayer(lhorse.getOwner()).getName());
                                for(UUID playerID : lhorse.getAccessList()) {
                                    if(plugin.getServer().getOfflinePlayer(playerID).hasPlayedBefore()) {
                                        response.add(ChatColor.GRAY + plugin.getServer().getOfflinePlayer(playerID).getName());
                                    } else {
                                        response.add(playerID.toString());
                                    }
                                }
                                for(String line : response) {
                                    player.sendMessage(line);
                                }
                            } else {
                                String accessChange = player.getMetadata(CobraCorral.HORSE_ACCESS).get(0).asString();
                                Character grantRevoke = accessChange.charAt(0);
                                UUID targetID = UUID.fromString(accessChange.substring(1));
                                if(grantRevoke.equals('+')) {
                                    if(config.HORSES.get(horse.getUniqueId().toString()).grantAccess(targetID)) {
                                        player.playSound(player.getLocation(), Sound.PISTON_EXTEND, 1f, 1f);
                                        player.playSound(player.getLocation(), Sound.CLICK, 1f, 1f);
                                        player.sendMessage(ChatColor.GRAY + "Added " + plugin.getServer().getOfflinePlayer(targetID).getName() + " to the access list.");
                                        plugin.getLogger().info(player.getName() + " has added " + plugin.getServer().getOfflinePlayer(targetID).getName() +
                                            " to the access list of horse " + horse.getUniqueId());
                                    } else {
                                        player.sendMessage(ChatColor.GRAY + "That player already has access to this horse.");
                                    }
                                } else {
                                    if(config.HORSES.get(horse.getUniqueId().toString()).revokeAccess(targetID)) {
                                        player.playSound(player.getLocation(), Sound.PISTON_RETRACT, 1f, 1f);
                                        player.playSound(player.getLocation(), Sound.CLICK, 1f, 1f);
                                        player.sendMessage(ChatColor.GRAY + "Removed " + plugin.getServer().getOfflinePlayer(targetID).getName() + " from the access list.");
                                        plugin.getLogger().info(player.getName() + " has removed " + plugin.getServer().getOfflinePlayer(targetID).getName() +
                                            " from the access list of horse " + horse.getUniqueId());
                                    } else {
                                        player.sendMessage(ChatColor.GRAY + "That player already does not have access to this horse.");
                                    }
                                }
                            }
                        } else {
                            player.sendMessage(ChatColor.GRAY + "You must lock the horse before changing the access list.");
                        }
                    } else {
                        player.sendMessage(ChatColor.GRAY + "You do not own that horse.");
                        player.playSound(player.getLocation(), Sound.ITEM_BREAK, 1f, 1f); 
                    }
                    utils.clearMetaKeys(player);
                    event.setCancelled(true);
                /*
                No Meta Keys Method    
                */
                } else if (utils.isHorseLocked(horse)) {
                    if(!config.PROTECT_CHESTS && horse.getVariant().equals(Horse.Variant.DONKEY) || horse.getVariant().equals(Horse.Variant.MULE) && player.getItemInHand().getType().equals(Material.CHEST)) {
                        player.sendMessage(ChatColor.GRAY + "You are not allowed to add a chest to locked " + horse.getVariant().name().toLowerCase() + "s.");
                        event.setCancelled(true);
                    }
                    LockedHorse lhorse = config.HORSES.get(horse.getUniqueId().toString());
                    if(!player.equals(horse.getOwner())) {
                        if(horse.hasMetadata(CobraCorral.HORSE_TEST_DRIVE) || config.HORSES.get(horse.getUniqueId().toString()).hasAccess(player.getUniqueId())){
                            player.sendMessage(ChatColor.GRAY + "You are now riding " + lhorse.getName() + " owned by " + ChatColor.GOLD + utils.getOwnerName(lhorse.getOwner()));
                        } else {
                            player.sendMessage(ChatColor.GRAY + "You do not have access to that horse.");
                            event.setCancelled(true);
                        }
                    } 
                }
            }
        } else {
            if(utils.hasMetaKeys(player)) {
                utils.clearMetaKeys(player);
                player.sendMessage(ChatColor.GRAY + "You can only perform that action on a horse.");
            }
        }
    }
    
    @EventHandler
    public void onEntityTame(EntityTameEvent event) {
        if(plugin.config.AUTO_LOCK) {
            Entity entity = event.getEntity();
            Player owner = (Player)event.getOwner();
            if(entity instanceof Horse && owner instanceof Player) {
                if(utils.maxHorsesLocked(owner.getUniqueId())){
                    owner.sendMessage(ChatColor.GRAY + "You cannot lock any more horses.");
                } else if (!utils.isHorseLocked((Horse)entity)) {
                    utils.lockHorse((Horse)entity, owner.getUniqueId());
                    owner.playSound(owner.getLocation(), Sound.CLICK, 1f, 1f);
                    owner.sendMessage(ChatColor.GRAY + "This horse has been locked.");
                    plugin.getLogger().info(owner.getName() + " tamed and autolocked " + (((Horse)entity).getCustomName() != null ?
                        ((Horse)entity).getCustomName() : ((Horse)entity).getVariant().toString()) + " with UUID " +
                        ((Horse)entity).getUniqueId().toString());
                }
            }
        }
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if(event.getEntity() instanceof Horse && plugin.config.IMMORTALITY) {
            Horse horse = (Horse)event.getEntity();
            if(utils.isHorseLocked(horse)) {
                if(horse.getPassenger() == null && !onCooldown.contains(horse.getUniqueId())) {
                    if(event.getCause() == DamageCause.VOID) {
                        horse.teleport(horse.getWorld().getSpawnLocation()); //If horse is damaged by VOID, teleport them to spawn of that world.
                    }
                    event.setCancelled(true);
                }
            }
        }
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if(event.getEntity() instanceof Horse) {
            Horse horse = (Horse)event.getEntity();
            Player owner = (Player)horse.getOwner();
            String passenger = (horse.getPassenger() == null ? "" : horse.getPassenger() instanceof Player ? ((Player)horse.getPassenger()).getName() : horse.getPassenger().toString());
            String causedBy = (horse.getKiller() == null ? "the environment" : horse.getKiller().getName());
            if(plugin.config.HORSES.containsKey(horse.getUniqueId().toString())) {
                plugin.config.HORSES.remove(horse.getUniqueId().toString());
                
                if(!owner.equals(horse.getPassenger()) && owner.isOnline()) {
                    owner.sendMessage(ChatColor.GRAY + (horse.getCustomName() != null ? horse.getCustomName() : horse.getVariant().toString()) +
                        " has died due to " + causedBy + (passenger.isEmpty() ? "." : " while being ridden by " + passenger + "."));
                }
                
                plugin.getLogger().info(owner.getName() + "'s horse " + (horse.getCustomName() != null ? horse.getCustomName() : horse.getVariant().toString()) + "/" +
                    horse.getUniqueId() + " has died due to " + causedBy + (passenger.isEmpty() ? "." : " while being ridden by " + passenger + "."));
            } else if (horse.isTamed()) {
                plugin.getLogger().info(horse.getOwner().getName() + "'s unlocked horse " + (horse.getCustomName() != null ? horse.getCustomName() : horse.getVariant().toString()) + "/" +
                    horse.getUniqueId() + " has died due to " + causedBy + (passenger.isEmpty() ? "." : " while being ridden by " + passenger + "."));
            }
        }
    }
    
    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if(event.getVehicle() instanceof Horse) {
            Horse horse = (Horse)event.getVehicle();
            final UUID horseID = horse.getUniqueId();
            if(utils.isHorseLocked(horse) && plugin.config.IMMORTAL_COOLDOWN) {
                config.HORSES.get(horse.getUniqueId().toString()).updateHorse(horse);
                onCooldown.add(horseID);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        onCooldown.remove(horseID);
                    }
                }.runTaskLaterAsynchronously(plugin, (plugin.config.COOLDOWN_TIME * 20));
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if(event.getInventory() instanceof HorseInventory && event.getInventory().getHolder() instanceof Horse) {
            Horse horse = (Horse)event.getInventory().getHolder();
            if(utils.isHorseLocked(horse)) {
                LockedHorse lhorse = config.HORSES.get(horse.getUniqueId().toString());
                lhorse.updateHorse(horse);
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if(player.getVehicle() != null) {
            if(player.getVehicle() instanceof Horse) {
                Horse horse = (Horse)player.getVehicle();
                if(utils.isHorseLocked(horse)) {
                    if(!horse.getOwner().getName().equalsIgnoreCase(player.getName())) {
                        horse.eject();
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        List<Entity> entities = Arrays.asList(event.getChunk().getEntities());
        for(Entity entity : entities) {
            if(entity instanceof Horse) {
                if(utils.isHorseLocked((Horse)entity)) {
                    config.HORSES.get(((Horse)entity).getUniqueId().toString()).updateHorse((Horse)entity);
                    LockedHorse lhorse = plugin.config.HORSES.get(entity.getUniqueId().toString());
                    plugin.config.HORSES.put(entity.getUniqueId().toString(), lhorse.updateHorse((Horse)entity));
                    //plugin.getLogger().info("DEBUG: Updated horse " + entity.getUniqueId() + " to Location: " + lhorse.getLocation());
                }
            }
        }
    }
}
