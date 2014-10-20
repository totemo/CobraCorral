package us.drome.cobracorral;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World.Environment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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
                utils.updateHorse(config.Database.getHorse(horse.getUniqueId()), horse);
            }
            
            /*
            Auto-unlocker Code
            If the horse has no owner and this horse is still locked, removeHorse it.
            This is to support non-CobraCorral implemented untame commands.
            */
            if(horse.getOwner() == null && utils.isHorseLocked(horse)) {
                utils.unlockHorse(horse.getUniqueId());
                horse.setTamed(false);
                horse.setDomestication(horse.getMaxDomestication() / 2);
                horse.setCustomName(null);
                plugin.getLogger().info("Auto-unlocked " + (horse.getCustomName() != null ?
                    horse.getCustomName() : horse.getVariant().toString()) + " with UUID " + horse.getUniqueId().toString() +
                        " due to the horse having no owner.");
            }
            
            if(player.hasMetadata(CobraCorral.HORSE_INFO)) {
                horseInfo(player, horse);
                utils.clearMetaKeys(player);
                event.setCancelled(true);
            } else if (horse.isTamed() && horse.getOwner() != null) {
                if(player.hasMetadata(CobraCorral.HORSE_LOCK)) {
                    horseLock(player, horse);
                    utils.clearMetaKeys(player); //Clear any meta keys off the player so they can interact normally.
                    event.setCancelled(true);
                } else if(player.hasMetadata(CobraCorral.HORSE_TEST_DRIVE)) {
                    horseTestDrive(player, horse);
                    utils.clearMetaKeys(player);
                    event.setCancelled(true);
                } else if(player.hasMetadata(CobraCorral.HORSE_UNLOCK)) {
                    horseUnlock(player, horse);
                    utils.clearMetaKeys(player);
                    event.setCancelled(true);
                } else if (player.hasMetadata(CobraCorral.HORSE_FREE)) {
                    horseFree(player, horse);
                    utils.clearMetaKeys(player);
                    event.setCancelled(true);
                } else if (player.hasMetadata(CobraCorral.HORSE_ACCESS)) {
                    horseAccess(player, horse);
                    utils.clearMetaKeys(player);
                    event.setCancelled(true);
                } else if(player.hasMetadata(CobraCorral.HORSE_NAME)) {
                    horseName(player, horse);
                    utils.clearMetaKeys(player);
                    event.setCancelled(true);
                /*
                No Meta Keys Detected, but it is a locked horse...
                */
                } else if (utils.isHorseLocked(horse)) {
                    if(!config.PROTECT_CHESTS && (horse.getVariant().equals(Horse.Variant.DONKEY) || horse.getVariant().equals(Horse.Variant.MULE)) && player.getItemInHand().getType().equals(Material.CHEST)) {
                        player.sendMessage(ChatColor.GRAY + "You are not allowed to add a chest to locked " + horse.getVariant().name().toLowerCase() + "s.");
                        event.setCancelled(true);
                    }
                    LockedHorse lhorse = config.Database.getHorse(horse.getUniqueId());
                    if(!(player.equals(horse.getOwner()) || player.hasMetadata(CobraCorral.HORSE_BYPASS))) {
                        if(player.hasMetadata(CobraCorral.HORSE_BYPASS)) {
                            plugin.getLogger().info("[Bypass] " + player.getName() + " used access bypass on horse " + lhorse.getUUID() + "/" + lhorse.getName() + " owned by " + utils.getOwnerName(lhorse.getOwner()));
                        }
                        if(horse.hasMetadata(CobraCorral.HORSE_TEST_DRIVE) || lhorse.hasAccess(player.getUniqueId())){
                            player.sendMessage(ChatColor.GRAY + "You are now riding " + lhorse.getName() + " owned by " + ChatColor.GOLD + utils.getOwnerName(lhorse.getOwner()));
                        } else {
                            if(player.hasPermission("ccorral.bypass")) {
                                player.sendMessage(ChatColor.GRAY + "You have no access to this horse, run " + ChatColor.GOLD + "/horse-bypass" + ChatColor.GRAY + " to toggle the access bypass.");
                            } else {
                                player.sendMessage(ChatColor.GRAY + "You do not have access to that horse.");
                            }
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
    
    /*
    HORSE_INFO Method
    Display detailed information on the horse that was right-clicked.
    */
    private void horseInfo(Player player, Horse horse) {
        player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1f);
        LockedHorse lhorse = null;
        if(utils.isHorseLocked(horse)) {
            lhorse = config.Database.getHorse(horse.getUniqueId());
        }
        if(player.hasPermission("ccorral.admin")) {
            player.sendMessage(ChatColor.GRAY + "UUID: " + ChatColor.GOLD + horse.getUniqueId());
        }
        player.sendMessage(ChatColor.GRAY + "Owner: " + ChatColor.GOLD + (horse.getOwner() != null ? horse.getOwner().getName() : "None"));
        player.sendMessage(ChatColor.GRAY + "Name: " + ChatColor.GOLD + (horse.getCustomName() != null ? horse.getCustomName() : (lhorse != null ? lhorse.getName() : "No Name")));
        player.sendMessage(ChatColor.GRAY + "Status: " + ChatColor.GOLD + (lhorse != null ? (horse.hasMetadata(CobraCorral.HORSE_TEST_DRIVE) ? "Test Drive" : "Locked") : "Unlocked"));
        if(lhorse != null && !player.getUniqueId().equals(lhorse.getOwner())) {
            player.sendMessage(ChatColor.GRAY + "Can I Ride: " + ChatColor.GOLD + (lhorse.hasAccess(player.getUniqueId()) ? "Yes" : (horse.hasMetadata(CobraCorral.HORSE_TEST_DRIVE) ? "Yes" : "No" )));
        }
        player.sendMessage(ChatColor.GRAY + "Variety: " + ChatColor.GOLD + ((horse.getVariant() == Horse.Variant.HORSE) ? horse.getColor().toString() + " " +
                (horse.getStyle().toString().equalsIgnoreCase("none") ? "" : horse.getStyle().toString()) : horse.getVariant().toString()));
        player.sendMessage(ChatColor.GRAY + "Equipment: " + ChatColor.GOLD + (horse.getInventory().getArmor() != null ? horse.getInventory().getArmor().getType().toString() : "No Armor") +
                ((horse.getInventory().contains(Material.SADDLE)) ? ", Saddle" : ", No Saddle") +
                (horse.isCarryingChest() ? ", Chest" : ""));
        player.sendMessage(ChatColor.GRAY + "Health: " + ChatColor.GOLD + (Math.round((horse.getHealth())) / 2.0f) + "♥ / " + Math.floor(horse.getMaxHealth() / 2) + "♥");
        player.sendMessage(ChatColor.GRAY + "Speed: " + ChatColor.GOLD + (Math.round(Utils.getSpeed(horse) * 100.0f) / 100.0f) + "m/s");
        player.sendMessage(ChatColor.GRAY + "Jump Height: " + ChatColor.GOLD + (Math.round((5.5 * (Math.pow(horse.getJumpStrength(), 2))) * 100.0f) / 100.0f) + "m");
    }
    
    /*
    HORSE_LOCK Method
    If the player owns the horse or is an admin, lock the horse to the current horse owner.
    */
    private void horseLock(Player player, Horse horse) {
        if(player.equals(horse.getOwner()) || player.hasPermission("ccorral.admin")) {
            if(!utils.maxHorsesLocked(horse.getOwner().getUniqueId())) {
                if(!utils.isHorseLocked(horse)) {
                    if(!config.PROTECT_CHESTS && (horse.getVariant().equals(Horse.Variant.DONKEY) || horse.getVariant().equals(Horse.Variant.MULE) && horse.isCarryingChest())) {
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
    }
    
    /*
    HORSE_TEST_DRIVE Method
    Toggles the test-drive status of a locked horse, allowing anyone to ride it.
    */
    private void horseTestDrive(Player player, Horse horse) {
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
    }
    
    /*
    HORSE_UNLOCK Method
    If the player owns the horse or is an admin, unlock the horse if it is currently locked.
    */
    private void horseUnlock(Player player, Horse horse) {
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
    }
    
    /*
    HORSE_FREE Method
    If the player owns the horse or is an admin, untame the horse and safely drop its inventory and reset it's stats.
    */
    private void horseFree(Player player, Horse horse) {
        if(player.equals(horse.getOwner()) || player.hasPermission("ccorral.admin")) {
            if(utils.isHorseLocked(horse)) {
                utils.unlockHorse(horse.getUniqueId());
                player.sendMessage(ChatColor.GRAY + horse.getVariant().toString() + " automatically unlocked...");
            }
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
            horse.setDomestication(horse.getMaxDomestication() / 2);
            horse.setCustomName(null);
            player.sendMessage(ChatColor.GRAY + horse.getVariant().toString() + " has been set free.");
            player.playSound(player.getLocation(), Sound.CLICK, 1f, 1f);
            player.playSound(player.getLocation(), Sound.ANVIL_USE, 1f, 1f);
        } else {
            player.sendMessage(ChatColor.GRAY + "You do not own that horse.");
            player.playSound(player.getLocation(), Sound.ITEM_BREAK, 1f, 1f); 
        }
    }
    
    /*
    HORSE_ACCESS Method
    Provide for the ability to add/remove/list the players allowed to ride this horse.
    */
    private void horseAccess(Player player, Horse horse) {
        if(player.equals(horse.getOwner()) || player.hasPermission("ccorral.admin")) {
            if(utils.isHorseLocked(horse)) {
                if(player.getMetadata(CobraCorral.HORSE_ACCESS).get(0).asString().isEmpty()) {
                    LockedHorse lhorse = config.Database.getHorse(horse.getUniqueId());
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
                        if(utils.grantAccess(config.Database.getHorse(horse.getUniqueId()), targetID)) {
                            player.playSound(player.getLocation(), Sound.PISTON_EXTEND, 1f, 1f);
                            player.playSound(player.getLocation(), Sound.CLICK, 1f, 1f);
                            player.sendMessage(ChatColor.GRAY + "Added " + plugin.getServer().getOfflinePlayer(targetID).getName() + " to the access list.");
                            plugin.getLogger().info(player.getName() + " has added " + plugin.getServer().getOfflinePlayer(targetID).getName() +
                                " to the access list of horse " + horse.getUniqueId());
                        } else {
                            player.sendMessage(ChatColor.GRAY + "That player already has access to this horse.");
                        }
                    } else {
                        if(utils.revokeAccess(config.Database.getHorse(horse.getUniqueId()), targetID)) {
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
    }
    
    /*
    HORSE_NAME Method
    Allows a player to give a nickname to their locked horse.
    */
    private void horseName(Player player, Horse horse) {
        if(player.getUniqueId().equals(horse.getOwner().getUniqueId())) {
            if(utils.isHorseLocked(horse)) {
                String nickname = player.getMetadata(CobraCorral.HORSE_NAME).get(0).asString();
                LockedHorse lhorse = config.Database.getHorse(horse.getUniqueId());
                if(lhorse.getName().equals(horse.getCustomName())) {
                    player.sendMessage(ChatColor.GRAY + "You cannot nickname a named horse.");
                } else {
                    player.playSound(player.getLocation(), Sound.DIG_STONE, 1f, 1f);
                    lhorse.setNickname(nickname);
                    utils.updateHorse(lhorse, horse);
                }
            } else {
                player.sendMessage(ChatColor.GRAY + "You must lock the horse before nicknaming it.");
            }
        } else {
            player.sendMessage(ChatColor.GRAY + "You do not own that horse.");
            player.playSound(player.getLocation(), Sound.ITEM_BREAK, 1f, 1f); 
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
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if(event.getEntity() instanceof Horse && plugin.config.STOP_PVP) {
            Horse horse = (Horse)event.getEntity();
            if(utils.isHorseLocked(horse)) {
                if(horse.getPassenger() != null) {
                    Entity damager = event.getDamager();
                    if(damager instanceof Player) {
                        event.setCancelled(true);
                    } else if(damager instanceof Arrow) {
                        if(((Arrow)damager).getShooter() instanceof Player) {
                            event.setCancelled(true);
                        }
                    }
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
            if(utils.isHorseLocked(horse)) {
                utils.unlockHorse(horse.getUniqueId());
                
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
                LockedHorse lhorse = config.Database.getHorse(horseID);
                utils.updateHorse(lhorse, horse);
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
    public void onInventoryOpen(InventoryOpenEvent event) {
        if(event.getInventory() instanceof HorseInventory && event.getInventory().getHolder() instanceof Horse) {
            Horse horse = (Horse)event.getInventory().getHolder();
            if(utils.isHorseLocked(horse)) {
                LockedHorse lhorse = config.Database.getHorse(horse.getUniqueId());
                if(!(lhorse.hasAccess(event.getPlayer().getUniqueId()) || lhorse.getOwner().equals(event.getPlayer().getUniqueId()) || event.getPlayer().hasMetadata(CobraCorral.HORSE_BYPASS))) {
                    event.setCancelled(true);   
                }
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if(event.getInventory() instanceof HorseInventory && event.getInventory().getHolder() instanceof Horse) {
            Horse horse = (Horse)event.getInventory().getHolder();
            if(utils.isHorseLocked(horse)) {
                LockedHorse lhorse = config.Database.getHorse(horse.getUniqueId());
                utils.updateHorse(lhorse, horse);
            }
        }
    }
    
    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if(event.getSource() instanceof HorseInventory && event.getSource().getHolder() instanceof Horse) {
            Horse horse = (Horse)event.getSource().getHolder();
            if(utils.isHorseLocked(horse)) {
                LockedHorse lhorse = config.Database.getHorse(horse.getUniqueId());
                if(event.getInitiator().getHolder() instanceof Player && !((Player)event.getInitiator().getHolder()).getUniqueId().equals(lhorse.getOwner())) {
                    plugin.getLogger().info("[ItemMove]" + ((Player)event.getInitiator().getHolder()).getName() + " removed item " + event.getItem().toString() + " from horse " +
                            lhorse.getUUID() + "/" + lhorse.getName() + " owned by " + utils.getOwnerName(lhorse.getOwner()) );
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        config.Database.clearCache(player.getUniqueId());
        if(player.getVehicle() != null) {
            if(player.getVehicle() instanceof Horse) {
                Horse horse = (Horse)player.getVehicle();
                if(utils.isHorseLocked(horse)) {
                    if(!horse.getOwner().getUniqueId().equals(player.getUniqueId())) {
                        horse.eject();
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        //When a player joins the server, load their horses into the cache.
        Set<LockedHorse> lhorses = config.Database.getHorses(event.getPlayer().getUniqueId());
        for(LockedHorse lhorse : lhorses) {
            plugin.getLogger().info("Cached horse: " + lhorse.getUUID() + ":" + lhorse.getName());
        }
    }
    
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        List<Entity> entities = Arrays.asList(event.getChunk().getEntities());
        for(Entity entity : entities) {
            if(entity instanceof Horse) {
                if(utils.isHorseLocked((Horse)entity)) {
                    LockedHorse lhorse = config.Database.getHorse(((Horse)entity).getUniqueId());
                    utils.updateHorse(lhorse, (Horse)entity);
                }
            }
        }
    }
    
    @EventHandler
    public void onEntityPortal(EntityPortalEvent event) {
        if(event.getFrom().getWorld().getEnvironment().equals(Environment.THE_END) && event.getEntity() instanceof Horse) {
            Horse horse = (Horse)event.getEntity();
            if(horse.getOwner() != null && horse.getPassenger() == null && ((Player)horse.getOwner()).getBedSpawnLocation() != null) {
                Utils.teleportHorse(horse, ((Player)horse.getOwner()).getBedSpawnLocation());
                event.setCancelled(true);
            }
        }
    }
}
