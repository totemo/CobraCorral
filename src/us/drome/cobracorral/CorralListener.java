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
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
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
            
            if(horse.getOwner() == null && utils.isHorseLocked(horse)) {
                utils.unlockHorse(horse.getUniqueId());
                plugin.getLogger().info("Auto-unlocked " + (horse.getCustomName() != null ?
                    horse.getCustomName() : horse.getVariant().toString()) + " with UUID " + horse.getUniqueId().toString() +
                        " due to the horse having no owner.");
            }
            
            if(player.hasMetadata(CobraCorral.HORSE_INFO)) {
                String owner = (horse.getOwner().getName() != null ? horse.getOwner().getName() : "None");
                String status = (utils.isHorseLocked(horse) ? "Locked" : "Not Locked");
                status = (horse.hasMetadata(CobraCorral.HORSE_TEST_DRIVE) ? "Test Drive" : status);
                String UUID = horse.getUniqueId().toString();
                player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1f);
                player.sendMessage("Owner:" + ChatColor.GRAY + owner + ChatColor.RESET + " Status:" +
                    ChatColor.GRAY + status + ChatColor.RESET + " UUID:" + ChatColor.GRAY + UUID);
                clearMetaKeys(player);
                event.setCancelled(true);
            } else if (horse.isTamed()) {
                if(player.hasMetadata(CobraCorral.HORSE_LOCK)) {
                    if(player.getName().equalsIgnoreCase(horse.getOwner().getName())) {
                        if(!utils.maxHorsesLocked(player.getUniqueId())) {
                            if(!utils.isHorseLocked(horse)) {
                                utils.lockHorse(horse.getUniqueId(), horse);
                                player.sendMessage(ChatColor.GRAY + (horse.getCustomName() != null ?
                                    horse.getCustomName() : horse.getVariant().toString()) + " has been locked.");
                                player.playSound(player.getLocation(), Sound.CLICK, 1f, 1f);
                                plugin.getLogger().info(player.getName() + " locked " + (horse.getCustomName() != null ?
                                    horse.getCustomName() : horse.getVariant().toString()) + " with UUID " + horse.getUniqueId().toString());
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
                    clearMetaKeys(player);
                    event.setCancelled(true);
                } else if(player.hasMetadata(CobraCorral.HORSE_TEST_DRIVE)) {
                    if(player.getName().equalsIgnoreCase(horse.getOwner().getName())) {
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
                                    Player passenger = (Player)horse.getPassenger();
                                    passenger.sendMessage(ChatColor.GRAY + "Your test drive has been cancelled.");
                                    passenger.playSound(passenger.getLocation(), Sound.HORSE_SADDLE, 1f, 1f);
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
                    clearMetaKeys(player);
                    event.setCancelled(true);
                } else if(player.hasMetadata(CobraCorral.HORSE_UNLOCK)) {
                    if(player.getName().equalsIgnoreCase(horse.getOwner().getName()) || player.hasPermission("ccorral.admin")) {
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
                    clearMetaKeys(player);
                    event.setCancelled(true);
                } else if (player.hasMetadata(CobraCorral.HORSE_FREE)) {
                    if(player.equals(horse.getOwner()) || player.hasPermission("ccorral.admin")) {
                        if(utils.isHorseLocked(horse)) {
                            player.sendMessage(ChatColor.GRAY + "That horse must be unlocked before it can be set free.");
                        } else {
                            plugin.getLogger().info(player.getName() + " set free " + horse.getOwner().getName() + "'s horse " +
                                horse.getVariant().toString() + " with UUID " + horse.getUniqueId().toString());
                            for(ItemStack item : horse.getInventory().getContents()) {
                                horse.getWorld().dropItemNaturally(horse.getLocation(), item);
                            }
                            horse.getWorld().dropItemNaturally(horse.getLocation(), horse.getInventory().getArmor());
                            horse.getWorld().dropItemNaturally(horse.getLocation(), horse.getInventory().getSaddle());
                            if(horse.isCarryingChest()) {
                                horse.setCarryingChest(false);
                                horse.getWorld().dropItemNaturally(horse.getLocation(), new ItemStack(Material.CHEST));
                            }
                            horse.setOwner(null);
                            horse.setTamed(false);
                            horse.setCustomName(null);
                            player.sendMessage(ChatColor.GRAY + horse.getVariant().toString()+ " has been set free.");
                            player.playSound(player.getLocation(), Sound.CLICK, 1f, 1f);
                            player.playSound(player.getLocation(), Sound.ANVIL_USE, 1f, 1f);
                        }
                    } else {
                        player.sendMessage(ChatColor.GRAY + "You do not own that horse.");
                        player.playSound(player.getLocation(), Sound.ITEM_BREAK, 1f, 1f); 
                    }
                    clearMetaKeys(player);
                    event.setCancelled(true);
                } else if (utils.isHorseLocked(horse)) {
                    if(!player.getName().equalsIgnoreCase(horse.getOwner().getName())) {
                        if(!horse.hasMetadata(CobraCorral.HORSE_TEST_DRIVE)){
                            event.setCancelled(true);
                        }
                    } 
                }
            }
        } else {
            if(hasMetaKeys(player)) {
                clearMetaKeys(player);
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
                    utils.lockHorse(entity.getUniqueId(), (Horse)entity);
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
                    event.setCancelled(true);                    
                }
            }
        }
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if(event.getEntity() instanceof Horse) {
            Horse horse = (Horse)event.getEntity();
            if(plugin.config.HORSES.containsKey(horse.getUniqueId().toString())) {
                Player owner = (Player)horse.getOwner();
                Player causedBy = (Player)horse.getPassenger();
                if(!owner.getName().equalsIgnoreCase(causedBy.getName())) {
                    if(owner.isOnline()) {
                        owner.sendMessage(ChatColor.GRAY + (horse.getCustomName() != null ? horse.getCustomName() : horse.getVariant().toString()) +
                            " has died due to the actions of " + causedBy.getName() + ".");
                        plugin.getLogger().info(owner.getName() + "'s horse, " + (horse.getCustomName() != null ? horse.getCustomName() : horse.getVariant().toString()) +
                            ", has died due to " + causedBy.getName() + "'s actions.");
                    }
                } else {
                    if(owner.isOnline()) {
                        owner.sendMessage(ChatColor.GRAY + (horse.getCustomName() != null ? horse.getCustomName() : horse.getVariant().toString()) +
                            " has died due to your actions.");
                        plugin.getLogger().info(owner.getName() + "'s horse, " + (horse.getCustomName() != null ? horse.getCustomName() : horse.getVariant().toString()) +
                            ", has died due to the owner's actions.");
                    }
                }
                plugin.config.HORSES.remove(horse.getUniqueId().toString());
            }
        }
    }
    
    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if(event.getVehicle() instanceof Horse) {
            Horse horse = (Horse)event.getVehicle();
            final UUID horseID = horse.getUniqueId();
            if(utils.isHorseLocked(horse) && plugin.config.IMMORTAL_COOLDOWN) {
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
                if(plugin.config.HORSES.containsKey(entity.getUniqueId().toString())) {
                    LockedHorse lhorse = plugin.config.HORSES.get(entity.getUniqueId().toString());
                    plugin.config.HORSES.put(entity.getUniqueId().toString(), lhorse.updateHorse((Horse)entity));
                }
            }
        }
    }
    
    public void clearMetaKeys(Player player) {
        if(player.hasMetadata(CobraCorral.HORSE_INFO)) {
            player.removeMetadata(CobraCorral.HORSE_INFO, plugin);
        } else if (player.hasMetadata(CobraCorral.HORSE_LOCK)) {
            player.removeMetadata(CobraCorral.HORSE_LOCK, plugin);
        } else if (player.hasMetadata(CobraCorral.HORSE_TEST_DRIVE)) {
            player.removeMetadata(CobraCorral.HORSE_TEST_DRIVE, plugin);
        } else if (player.hasMetadata(CobraCorral.HORSE_UNLOCK)) {
            player.removeMetadata(CobraCorral.HORSE_UNLOCK, plugin);
        }
    }
    
    public boolean hasMetaKeys(Player player) {
        if(player.hasMetadata(CobraCorral.HORSE_INFO)) {
            return true;
        } else if (player.hasMetadata(CobraCorral.HORSE_LOCK)) {
            return true;
        } else if (player.hasMetadata(CobraCorral.HORSE_TEST_DRIVE)) {
            return true;
        } else if (player.hasMetadata(CobraCorral.HORSE_UNLOCK)) {
            return true;
        } else {
            return false;
        }
    }
}
