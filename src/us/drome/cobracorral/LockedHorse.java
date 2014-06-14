package us.drome.cobracorral;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Horse;

public class LockedHorse implements ConfigurationSerializable {
    private UUID owner;
    private String name;
    private String appearance;
    private String armor;
    private String location;
    
    public LockedHorse(Horse horse) {
        owner = horse.getOwner().getUniqueId();
        name = (horse.getCustomName() != null ? horse.getCustomName() : "NoName");
        appearance = ((horse.getVariant() == Horse.Variant.HORSE) ? horse.getColor().toString() + " " +
                (horse.getStyle().toString().equalsIgnoreCase("none") ? "" : horse.getStyle().toString()) : horse.getVariant().toString());
        armor = (horse.getInventory().getArmor() != null ? horse.getInventory().getArmor().getType().toString() : "No Armor");
        Location horseLoc = horse.getLocation();
        location = horseLoc.getBlockX() + ":" + horseLoc.getBlockY() + ":" + horseLoc.getBlockZ() + ":" + horseLoc.getWorld().getName();
    }
        
    public LockedHorse(Map<String, Object> map) {
        owner = UUID.fromString((String)map.get("owner"));
        name = (String)map.get("name");
        appearance = (String)map.get("appearance");
        armor = (String)map.get("armor");
        location = (String)map.get("location");
    }
    
    public LockedHorse updateHorse(Horse horse) {
        name = (horse.getCustomName() != null ? horse.getCustomName() : "NoName");
        armor = (horse.getInventory().getArmor() != null ? horse.getInventory().getArmor().getType().toString() : "No Armor");
        Location horseLoc = horse.getLocation();
        location = horseLoc.getBlockX() + ":" + horseLoc.getBlockY() + ":" + horseLoc.getBlockZ() + ":" + horseLoc.getWorld().getName();
        return this;
    }
    
    public UUID getOwner() {
        return owner;
    }
    
    public LockedHorse setOwner(UUID owner) {
        this.owner = owner;
        return this;
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
    
    public String getLocation() {
        return location;
    }
    
    public String getX() {
        return location.split(":")[0];
    }
    
    public String getY() {
        return location.split(":")[1];
    }
    
    public String getZ() {
        return location.split(":")[2];
    }
    
    public String getWorld() {
        return location.split(":")[3];
    }
    
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("owner", owner.toString());
        map.put("name", name);
        map.put("appearance", appearance);
        map.put("armor", armor);
        map.put("location", location);
        return map;
    }
    
    public LockedHorse valueOf(Map<String, Object> map) {
        return new LockedHorse(map);
    }
    
    public LockedHorse deserialize(Map<String, Object> map) {
        return new LockedHorse(map);
    }
}
