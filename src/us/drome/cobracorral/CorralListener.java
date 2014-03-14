package us.drome.cobracorral;

import org.bukkit.ChatColor;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class CorralListener implements Listener {
    private final CobraCorral plugin;
    
    CorralListener(CobraCorral instance) {
        plugin = instance;
    }
    
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if(event.getRightClicked() instanceof Horse) {
            Horse horse = (Horse)event.getRightClicked();
            
            if(plugin.config.HORSES.containsKey(horse.getUniqueId())) {
                if(!player.getName().equalsIgnoreCase(horse.getOwner().getName())) {
                    
                }
            }
            //check ownership, if now owner and locked, cancel.
        } else {
            if(player.hasMetadata(CobraCorral.HORSE_INFO)) {
                player.sendMessage(ChatColor.GRAY + "You can only perform that action on a horse.");
                player.removeMetadata(CobraCorral.HORSE_INFO, plugin);
            } else if (player.hasMetadata(CobraCorral.HORSE_LOCK)) {
                player.sendMessage(ChatColor.GRAY + "You can only perform that action on a horse.");
                player.removeMetadata(CobraCorral.HORSE_LOCK, plugin);
            } else if (player.hasMetadata(CobraCorral.HORSE_TEST_DRIVE)) {
                player.sendMessage(ChatColor.GRAY + "You can only perform that action on a horse.");
                player.removeMetadata(CobraCorral.HORSE_TEST_DRIVE, plugin);
            } else if (player.hasMetadata(CobraCorral.HORSE_UNLOCK)) {
                player.sendMessage(ChatColor.GRAY + "You can only perform that action on a horse.");
                player.removeMetadata(CobraCorral.HORSE_UNLOCK, plugin);
            }
        }
    }
    
    public void onEntityTame(EntityTameEvent event) {
        Entity entity = event.getEntity();
        AnimalTamer owner = event.getOwner();
        if(entity instanceof Horse && owner instanceof Player) {
            if(plugin.maxHorsesLocked(owner.getName())){
                ((Player)owner).sendMessage("You cannot lock any more horses.");
            } else {
                plugin.lockHorse(entity.getUniqueId(), owner.getName());
                ((Player)owner).sendMessage("This horse has been locked.");
            }
        }
    }
    
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
}
