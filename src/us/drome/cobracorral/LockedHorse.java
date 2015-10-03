package us.drome.cobracorral;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Style;
import org.bukkit.entity.Horse.Variant;

public class LockedHorse {
    private UUID uuid;
    private UUID owner;
    private String name;
    private String nickname;
    private String appearance;
    private String armor;
    private String saddle;
    private String chest;
    private String location;
    private int maxHealth = 0;
    private double maxSpeed = 0;
    private double jumpHeight = 0;
    private List<UUID> accessList;
    
    
    public LockedHorse(Horse horse, UUID ownerID) {
        Variant variant = horse.getVariant();
        Style style = horse.getStyle();
        
        uuid = horse.getUniqueId();
        owner = ownerID;
        name = (horse.getCustomName() != null ? horse.getCustomName() : "NoName");
        nickname = "";
        appearance = ((variant == Variant.HORSE) ? horse.getColor().toString() + " " +
                (style.toString().equalsIgnoreCase("none") ? "" : style.toString()) : variant.toString());
        armor = (horse.getInventory().getArmor() != null ? horse.getInventory().getArmor().getType().toString() : "No Armor");
        saddle = (horse.getInventory().contains(Material.SADDLE)) ? "Saddled" : "No Saddle";
        chest = horse.isCarryingChest() ? "Has Chest" : "";
        Location horseLoc = horse.getLocation();
        location = horseLoc.getBlockX() + ":" + horseLoc.getBlockY() + ":" + horseLoc.getBlockZ() + ":" + horseLoc.getWorld().getName();
        maxHealth = (int)Math.floor(horse.getMaxHealth() / 2 );
        jumpHeight = 5.5 * (Math.pow(horse.getJumpStrength(), 2)); //Formula from https://github.com/RedPanda4552/HorseStats
        maxSpeed = Utils.getSpeed(horse);
        accessList = new ArrayList<>();
    }
    
    public LockedHorse(UUID uuid, UUID owner, String name, String nickname, String appearance, String armor, String saddle, String chest, String location, int maxHealth, double maxSpeed, double jumpHeight, List<UUID> accessList) {
        this.uuid = uuid;
        this.owner = owner;
        this.name = name;
        this.nickname = nickname;
        this.appearance = appearance;
        this.armor = armor;
        this.saddle = saddle;
        this.chest = chest;
        this.location = location;
        this.maxHealth = maxHealth;
        this.maxSpeed = maxSpeed;
        this.jumpHeight = jumpHeight;
        this.accessList = accessList;
    }
    
    public LockedHorse updateHorse(Horse horse) {
        name = (horse.getCustomName() != null ? horse.getCustomName() : "NoName");
        armor = (horse.getInventory().getArmor() != null ? horse.getInventory().getArmor().getType().toString() : "No Armor");
        saddle = (horse.getInventory().contains(Material.SADDLE)) ? "Saddled" : "No Saddle";
        chest = ((horse.getVariant().equals(Variant.DONKEY) ||  horse.getVariant().equals(Variant.MULE)) && horse.isCarryingChest() ? "Has Chest" : ""); 
        Location horseLoc = horse.getLocation();
        location = horseLoc.getBlockX() + ":" + horseLoc.getBlockY() + ":" + horseLoc.getBlockZ() + ":" + horseLoc.getWorld().getName();
        if(maxHealth == 0) {
            maxHealth = (int)Math.floor(horse.getMaxHealth() / 2 );
            jumpHeight = 5.5 * (Math.pow(horse.getJumpStrength(), 2));
            maxSpeed = Utils.getSpeed(horse);
        }
        return this;
    }
    
    public LockedHorse updateLocation(Location horseLoc) {
        location = horseLoc.getBlockX() + ":" + horseLoc.getBlockY() + ":" + horseLoc.getBlockZ() + ":" + horseLoc.getWorld().getName();
        return this;
    }
    
    public UUID getUUID() {
        return uuid;
    }
    
    public UUID getOwner() {
        return owner;
    }
    
    public LockedHorse setOwner(UUID owner) {
        this.owner = owner;
        return this;
    }
    
    public String getName() {
        return (name.equalsIgnoreCase("NoName") ? nickname.equalsIgnoreCase("") ? "NoName" : "\"" + nickname + "\"" : name);
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public String getAppearance() {
        return appearance;
    }
    
    public String getArmor() {
        return armor;
    }
    
    public String getSaddle() {
        return saddle;
    }
    
    public String getChest() {
        return chest;
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
    
    public int getMaxHealth() {
        return maxHealth;
    }
    
    public double getMaxSpeed() {
        return maxSpeed;
    }
    
    public double getJumpHeight() {
        return jumpHeight;
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
        }
        return false;
    }
    
    public boolean hasAccess(UUID player) {
        if(accessList.contains(player)) {
            return true;
        }
        return false;
    }
    
    public List<UUID> getAccessList() {
        return accessList;
    }
}
