package us.drome.cobracorral;

import java.util.Arrays;
import java.util.List;
import org.bukkit.ChatColor;
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
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class CorralListener implements Listener {
    private final CobraCorral plugin;
    
    CorralListener(CobraCorral instance) {
        plugin = instance;
    }
    
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if(event.getRightClicked() instanceof Horse) {
            Horse horse = (Horse)event.getRightClicked();
            
            if(player.hasMetadata(CobraCorral.HORSE_INFO)) {
                String owner = horse.getOwner().getName() != null ? horse.getOwner().getName() : "None";
                String status = plugin.isHorseLocked(horse) ? "Locked" : "Not Locked";
                status = horse.hasMetadata(CobraCorral.HORSE_TEST_DRIVE) ? "Test Drive" : status;
                String UUID = horse.getUniqueId().toString();
                player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1f);
                player.sendMessage("Owner:" + ChatColor.GRAY + owner + ChatColor.RESET + " Status:" +
                    ChatColor.GRAY + status + ChatColor.RESET + " UUID:" + ChatColor.GRAY + UUID);
                clearMetaKeys(player);
                event.setCancelled(true);
            } else if (horse.isTamed()) {
                if(player.hasMetadata(CobraCorral.HORSE_LOCK)) {
                    if(player.getName().equalsIgnoreCase(horse.getOwner().getName())) {
                        if(!plugin.isHorseLocked(horse)) {
                            plugin.lockHorse(horse.getUniqueId(), horse);
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
                        player.sendMessage(ChatColor.GRAY + "You do not own that horse.");
                        player.playSound(player.getLocation(), Sound.ITEM_BREAK, 1f, 1f); 
                    }
                    clearMetaKeys(player);
                    event.setCancelled(true);
                } else if(player.hasMetadata(CobraCorral.HORSE_TEST_DRIVE)) {
                    if(player.getName().equalsIgnoreCase(horse.getOwner().getName())) {
                        if(plugin.isHorseLocked(horse)) {
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
                        if(plugin.isHorseLocked(horse)) {
                            plugin.unlockHorse(horse.getUniqueId());
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
                } else if (plugin.isHorseLocked(horse)) {
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
                if(plugin.maxHorsesLocked(owner.getName())){
                    owner.sendMessage(ChatColor.GRAY + "You cannot lock any more horses.");
                } else {
                    ((Horse)entity).setOwner(owner);
                    plugin.lockHorse(entity.getUniqueId(), (Horse)entity);
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
            if(plugin.isHorseLocked(horse)) {
                if(horse.getPassenger() == null) {
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
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if(player.getVehicle() != null) {
            if(player.getVehicle() instanceof Horse) {
                Horse horse = (Horse)player.getVehicle();
                if(plugin.isHorseLocked(horse)) {
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
