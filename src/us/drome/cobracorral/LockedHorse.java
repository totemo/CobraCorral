package us.drome.cobracorral;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Horse;

public class LockedHorse {
    private String owner;
    private String name;
    private String appearance;
    private String armor;
    private String location;
    
    public LockedHorse(Horse horse) {
        owner = horse.getOwner().getName();
        name = (horse.getCustomName() != null ? horse.getCustomName() : "NoName");
        appearance = ((horse.getVariant() == Horse.Variant.HORSE) ? horse.getColor().toString() +
            " " + horse.getStyle().toString() : horse.getVariant().toString());
        armor = (horse.getInventory().getArmor() != null ? horse.getInventory().getArmor().getType().toString() : "No Armor");
        Location horseLoc = horse.getLocation();
        location = horseLoc.getBlockX() + ":" + horseLoc.getBlockY() + ":" + horseLoc.getBlockZ() + ":" + horseLoc.getWorld();
    }
    
    public LockedHorse updateHorse(Horse horse) {
        name = (horse.getCustomName() != null ? horse.getCustomName() : "NoName");
        armor = (horse.getInventory().getArmor() != null ? horse.getInventory().getArmor().getType().toString() : "No Armor");
        Location horseLoc = horse.getLocation();
        location = horseLoc.getBlockX() + ":" + horseLoc.getBlockY() + ":" + horseLoc.getBlockZ() + ":" + horseLoc.getWorld();
        return this;
    }
    
    public String getOwner() {
        return owner;
    }
    
    public String getName() {
        return name;
    }
    
    public String getAppearance() {
        return appearance;
    }
    
    public String getArmor() {
        return armor;
    }
    
    public Location getLocation(CobraCorral plugin) {
        String[] coords = location.split(":");
        double x = Double.valueOf(coords[0]);
        double y = Double.valueOf(coords[1]);
        double z = Double.valueOf(coords[2]);
        World world = plugin.getServer().getWorld(coords[3]);
        return new Location(world, x, y, z);
    }
}
