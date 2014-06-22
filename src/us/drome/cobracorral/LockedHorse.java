package us.drome.cobracorral;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private List<UUID> accessList;
    public String ownerName; //Only here for UUID conversion, never saved to file and will be removed from future versions.
    
    public LockedHorse(Horse horse, UUID ownerID) {
        owner = ownerID;
        name = (horse.getCustomName() != null ? horse.getCustomName() : "NoName");
        appearance = ((horse.getVariant() == Horse.Variant.HORSE) ? horse.getColor().toString() + " " +
                (horse.getStyle().toString().equalsIgnoreCase("none") ? "" : horse.getStyle().toString()) : horse.getVariant().toString());
        armor = (horse.getInventory().getArmor() != null ? horse.getInventory().getArmor().getType().toString() : "No Armor");
        Location horseLoc = horse.getLocation();
        location = horseLoc.getBlockX() + ":" + horseLoc.getBlockY() + ":" + horseLoc.getBlockZ() + ":" + horseLoc.getWorld().getName();
        accessList = new ArrayList<>();
    }
    
    //Constructor for de-serialization on loading from config.yml    
    public LockedHorse(Map<String, Object> map) {
        try {
            owner = UUID.fromString((String)map.get("owner"));
        } catch (IllegalArgumentException e) {
            ownerName = (String)map.get("owner");
        }
        name = (String)map.get("name");
        appearance = (String)map.get("appearance");
        armor = (String)map.get("armor");
        location = (String)map.get("location");
        List<String> aclTemp = (List<String>)map.get("accessList");
        accessList = new ArrayList<>();
        if(aclTemp != null && !aclTemp.isEmpty()) {
            for(String value : aclTemp) {
                accessList.add(UUID.fromString(value));
            }
        }
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
    
    /*
    Access List methods.
    */
    public boolean grantAccess(UUID player) {
        if(accessList.contains(player)) {
            return false;
        } else {
            accessList.add(player);
            return true;
        }
    }
    
    public boolean revokeAccess(UUID player) {
        if(accessList.contains(player)) {
            accessList.remove(player);
            return true;
        } else {
            return false;
        }
    }
    
    public boolean hasAccess(UUID player) {
        if(accessList.contains(player)) {
            return true;
        } else {
            return false;
        }
    }
    
    public List<UUID> getAccessList() {
        return accessList;
    }
    
    /*
    Methods below are used for ConfigurationSerializable support
    */
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("owner", owner.toString());
        map.put("name", name);
        map.put("appearance", appearance);
        map.put("armor", armor);
        map.put("location", location);
        List<String> aclTemp = new ArrayList<>();
        if(accessList != null && !accessList.isEmpty()) {
            for(UUID value : accessList) {
                aclTemp.add(value.toString());
            }
        }
        map.put("accessList", aclTemp);
        return map;
    }
    
    public LockedHorse valueOf(Map<String, Object> map) {
        return new LockedHorse(map);
    }
    
    public LockedHorse deserialize(Map<String, Object> map) {
        return new LockedHorse(map);
    }
}
