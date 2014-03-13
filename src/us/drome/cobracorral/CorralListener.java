package us.drome.cobracorral;

import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.entity.EntityTameEvent;

public class CorralListener implements Listener {
    private final CobraCorral plugin;
    
    CorralListener(CobraCorral instance) {
        plugin = instance;
    }
    
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if(entity instanceof Horse) {
            
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
}
