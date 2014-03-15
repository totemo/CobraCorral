package us.drome.cobracorral;

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
                String status = plugin.config.HORSES.containsKey(horse.getUniqueId()) ? "Locked" : "Not Locked";
                status = horse.hasMetadata(CobraCorral.HORSE_TEST_DRIVE) ? "Test Drive" : status;
                player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1f);
                player.sendMessage(ChatColor.GRAY + "Owner:" + owner + " Status:" + status);
                clearMetaKeys(player);
                event.setCancelled(true);
            } else if(player.hasMetadata(CobraCorral.HORSE_LOCK)) {
                if(player.getName().equalsIgnoreCase(horse.getOwner().getName())) {
                    if(!plugin.config.HORSES.containsKey(horse.getUniqueId())) {
                        plugin.lockHorse(horse.getUniqueId(), player.getName());
                        player.sendMessage(ChatColor.GRAY + horse.getCustomName() != null ?
                            horse.getCustomName() : horse.getVariant().toString() + " has been locked.");
                        player.playSound(player.getLocation(), Sound.CLICK, 1f, 1f);
                    } else {
                        player.sendMessage(ChatColor.GRAY + horse.getCustomName() != null ?
                            horse.getCustomName() : horse.getVariant().toString() + " is already locked.");
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
                    if(plugin.config.HORSES.containsKey(horse.getUniqueId())) {
                        if(horse.hasMetadata(CobraCorral.HORSE_TEST_DRIVE)) {
                            horse.removeMetadata(CobraCorral.HORSE_TEST_DRIVE, plugin);
                            player.sendMessage(ChatColor.GRAY + horse.getCustomName() != null ?
                                horse.getCustomName() : horse.getVariant().toString() + " has been set to locked mode.");
                            player.playSound(player.getLocation(), Sound.CLICK, 1f, 1f);
                            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1f); 
                        } else {
                            horse.setMetadata(CobraCorral.HORSE_TEST_DRIVE, new FixedMetadataValue(plugin, null));
                            player.sendMessage(ChatColor.GRAY + horse.getCustomName() != null ?
                                horse.getCustomName() : horse.getVariant().toString() + " has been set to test drive mode.");
                            player.playSound(player.getLocation(), Sound.CLICK, 1f, 1f);
                            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1f); 
                        }
                    } else {
                        player.sendMessage(ChatColor.GRAY + horse.getCustomName() != null ?
                            horse.getCustomName() : horse.getVariant().toString() + " is not locked.");
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
                    if(plugin.config.HORSES.containsKey(horse.getUniqueId())) {
                        plugin.unlockHorse(horse.getUniqueId());
                        player.sendMessage(ChatColor.GRAY + horse.getCustomName() != null ?
                            horse.getCustomName() : horse.getVariant().toString() + " has been unlocked.");
                        player.playSound(player.getLocation(), Sound.CLICK, 1f, 1f);
                        player.playSound(player.getLocation(), Sound.CLICK, 1f, 1f);
                    } else {
                        player.sendMessage(ChatColor.GRAY + horse.getCustomName() != null ?
                            horse.getCustomName() : horse.getVariant().toString() + " is not locked.");
                        player.playSound(player.getLocation(), Sound.ITEM_BREAK, 1f, 1f);
                    }
                } else {
                    player.sendMessage(ChatColor.GRAY + "You do not own that horse.");
                    player.playSound(player.getLocation(), Sound.ITEM_BREAK, 1f, 1f); 
                }
                clearMetaKeys(player);
                event.setCancelled(true);
            } else {
                if(!player.getName().equalsIgnoreCase(horse.getOwner().getName())) {
                    if(!horse.hasMetadata(CobraCorral.HORSE_TEST_DRIVE)){
                        event.setCancelled(true);
                    }
                }
            }
        } else {
            clearMetaKeys(player);
            player.sendMessage(ChatColor.GRAY + "You can only perform that action on a horse.");
        }
    }
    
    @EventHandler
    public void onEntityTame(EntityTameEvent event) {
        Entity entity = event.getEntity();
        Player owner = (Player)event.getOwner();
        if(entity instanceof Horse && owner instanceof Player) {
            if(plugin.maxHorsesLocked(owner.getName())){
                owner.sendMessage("You cannot lock any more horses.");
            } else {
                plugin.lockHorse(entity.getUniqueId(), owner.getName());
                owner.playSound(owner.getLocation(), Sound.CLICK, 1f, 1f);
                owner.sendMessage("This horse has been locked.");
            }
        }
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if(event.getEntity() instanceof Horse) {
            Horse horse = (Horse)event.getEntity();
            if(plugin.config.HORSES.containsKey(horse.getUniqueId())) {
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
            if(plugin.config.HORSES.containsKey(horse.getUniqueId())) {
                Player owner = (Player)horse.getOwner();
                Player causedBy = (Player)horse.getPassenger();
                if(!owner.getName().equalsIgnoreCase(causedBy.getName())) {
                    if(owner.isOnline()) {
                        owner.sendMessage(ChatColor.GRAY + horse.getCustomName() != null ? horse.getCustomName() : horse.getVariant().toString() +
                            " has died due to the actions of " + causedBy.getName() + ".");
                    }
                } else {
                    if(owner.isOnline()) {
                        owner.sendMessage(ChatColor.GRAY + horse.getCustomName() != null ? horse.getCustomName() : horse.getVariant().toString() +
                            " has died due to your actions.");
                    }
                }
                plugin.config.HORSES.remove(horse.getUniqueId());
            }
        }
    }
    
    @EventHandler
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
}
