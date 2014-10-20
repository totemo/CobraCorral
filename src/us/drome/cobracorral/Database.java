package us.drome.cobracorral;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.entity.Horse;
import us.drome.cobrasql.*;

/*
Schema for table HORSES:
    * UUID - Pkey
    * Owner
    * Name
    * Nickname
    * Appearance
    * Armor
    * Saddle
    * Chest
    * Location
    * MaxHealth
    * MaxSpeed
    * JumpHeight
    
Schema for table ACL:
    * Horse - LEFT JOIN HORSES.UUID
    * Player
*/

public class Database {
    private SQLEngine database;
    private Set<LockedHorse> horseCache;
    
    public Database(SQLEngine backend) throws Exception {
        this.database = backend;
        horseCache = new LinkedHashSet<>();
        
        try {
            PreparedStatement initializeHorses = database.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS HORSES (" +
                    "UUID NVARCHAR(36) PRIMARY KEY NOT NULL," +
                    "Owner NVARCHAR(36)," +
                    "Name NVARCHAR(64)," +
                    "Nickname NVARCHAR(16)," +
                    "Appearance NVARCHAR(32)," +
                    "Armor NVARCHAR(16)," +
                    "Saddle NVARCHAR(16)," +
                    "Chest NVARCHAR(16)," +
                    "Location NVARCHAR(32)," +
                    "MaxHealth INTEGER(2)," +
                    "MaxSpeed FLOAT," +
                    "JumpHeight FLOAT);");

            PreparedStatement initializeACL = database.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS ACL (" +
                    "Horse NVARCHAR(36) NOT NULL," +
                    "Player NVARCHAR(36) NOT NULL);");
                    
            database.runAsyncUpdate(initializeHorses);
            database.runAsyncUpdate(initializeACL);
            database.getLogger().info("Database successfully initialized.");
        } catch (Exception ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
            throw ex;
        }
    }
    
    public SQLEngine getEngine() {
        return database;
    }
    
    public void clearCache(UUID playerID) {
        Iterator<LockedHorse> horseIterator = horseCache.iterator();
        while(horseIterator.hasNext()) {
            LockedHorse lhorse = horseIterator.next();
            if(lhorse.getOwner().equals(playerID)) {
                horseIterator.remove();
            }
        }
    }
    
    
    public void updateHorse(LockedHorse lhorse) {
        try {
            PreparedStatement update = database.getConnection().prepareStatement("UPDATE HORSES SET UUID = ?, Owner = ?, Name = ?, Nickname = ?, Appearance = ?, Armor = ?, Saddle = ?,"
                    + "Chest = ?, Location = ?, MaxHealth = ?, MaxSpeed = ?, JumpHeight = ? WHERE UUID = ?");
            update.setString(1, lhorse.getUUID().toString());
            update.setString(2, lhorse.getOwner().toString());
            update.setString(3, lhorse.getName());
            update.setString(4, lhorse.getNickname());
            update.setString(5, lhorse.getAppearance());
            update.setString(6, lhorse.getArmor());
            update.setString(7, lhorse.getSaddle());
            update.setString(8, lhorse.getChest());
            update.setString(9, lhorse.getLocation());
            update.setInt(10, lhorse.getMaxHealth());
            update.setDouble(11, lhorse.getMaxSpeed());
            update.setDouble(12, lhorse.getJumpHeight());
            update.setString(13, lhorse.getUUID().toString());
            database.runAsyncUpdate(update);
        } catch (SQLException ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Returns a Set of LockedHorse objects from the database/cache matching a specified player UUID.
     * @param playerID UUID of the player.
     * @return A Set of LockedHorses that the player owns.
     */
    public Set<LockedHorse> getHorses(UUID playerID) {
        LockedHorse lhorse = null;
        Set<LockedHorse> horses = new LinkedHashSet<>();
        
        //Iterate through the live cache and addHorse LockedHorses owned by the playerID to the result.
        Iterator<LockedHorse> cacheIterator = horseCache.iterator();
        while(cacheIterator.hasNext()) {
            lhorse = cacheIterator.next();
            if(lhorse.getOwner().equals(playerID)) {
                horses.add(lhorse);
            }
        }
        
        //If no locked horses were found in the cache, check the database.
        if(horses.isEmpty()) {
            try {
                PreparedStatement query = database.getConnection().prepareStatement("SELECT * FROM HORSES LEFT JOIN ACL ON HORSES.UUID=ACL.Horse WHERE Owner = ?");
                query.setString(1, playerID.toString());
                ResultSet result = query.executeQuery();
                
                if(result.isBeforeFirst()) {
                    UUID uuid = null;
                    UUID owner = null;
                    String name = "";
                    String nickname = "";
                    String appearance = "";
                    String armor = "";
                    String saddle = "";
                    String chest = "";
                    String location = "";
                    int maxHealth = 0;
                    double maxSpeed = 0;
                    double jumpHeight = 0;
                    List<UUID> accessList = new ArrayList<>();
                    Map<UUID, LockedHorse> tempCache = new HashMap<>();
                    while(result.next()) {
                        uuid = UUID.fromString(result.getString("UUID"));
                        if(tempCache.containsKey(uuid)) {
                            tempCache.get(uuid).getAccessList().add(UUID.fromString(result.getString("Player")));
                        } else {
                            owner = UUID.fromString(result.getString("Owner"));
                            name = result.getString("Name");
                            nickname = result.getString("Nickname");
                            appearance = result.getString("Appearance");
                            armor = result.getString("Armor");
                            saddle = result.getString("Saddle");
                            chest = result.getString("Chest");
                            location = result.getString("Location");
                            maxHealth = result.getInt("MaxHealth");
                            maxSpeed = result.getDouble("MaxSpeed");
                            jumpHeight = result.getDouble("JumpHeight");
                            if(result.getString("Player") != null) {
                                accessList.add(UUID.fromString(result.getString("Player")));
                            }
                            tempCache.put(uuid, new LockedHorse(uuid, owner, name, nickname, appearance, armor, saddle, chest, location, maxHealth, maxSpeed, jumpHeight, accessList));
                        }
                    }
                    if(!tempCache.isEmpty()) {
                        for(LockedHorse tempLhorse : tempCache.values()) {
                            horseCache.add(tempLhorse);
                            horses.add(tempLhorse);
                        }
                    }
                }
            } catch (SQLException ex) {
                database.getLogger().log(Level.SEVERE, null, ex);
            }
        }
        return horses;
    }
    
    public LockedHorse getHorse(UUID horseID) {
        LockedHorse lhorse = null;
        Iterator<LockedHorse> cacheIterator = horseCache.iterator();
        while(cacheIterator.hasNext()) {
            lhorse = cacheIterator.next();
            if(lhorse.getUUID().equals(horseID)) {
                return lhorse;
            }
        }
        
        try {
            PreparedStatement query = database.getConnection().prepareStatement("SELECT * FROM HORSES LEFT JOIN ACL ON HORSES.UUID=ACL.Horse WHERE UUID = ?");
            query.setString(1, horseID.toString());
            ResultSet result = query.executeQuery();
            
            if(result.isBeforeFirst()) {
                result.next();
                UUID uuid = UUID.fromString(result.getString("UUID"));
                UUID owner = UUID.fromString(result.getString("Owner"));
                String name = result.getString("Name");
                String nickname = result.getString("Nickname");
                String appearance = result.getString("Appearance");
                String armor = result.getString("Armor");
                String saddle = result.getString("Saddle");
                String chest = result.getString("Chest");
                String location = result.getString("Location");
                int maxHealth = result.getInt("MaxHealth");
                double maxSpeed = result.getDouble("MaxSpeed");
                double jumpHeight = result.getDouble("JumpHeight");
                List<UUID> accessList = new ArrayList<>();
                if(result.getString("Player") != null) {
                    accessList.add(UUID.fromString(result.getString("Player")));
                }
                while(result.next()) {
                    accessList.add(UUID.fromString(result.getString("Player")));
                }
                
                lhorse = new LockedHorse(uuid, owner, name, nickname, appearance, armor, saddle, chest, location, maxHealth, maxSpeed, jumpHeight, accessList);
                horseCache.add(lhorse);
                return lhorse;
            }
        } catch (SQLException ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
        }
        return lhorse;
    }
    
    public boolean contains(UUID horseID) {
        Iterator<LockedHorse> cacheIterator = horseCache.iterator();
        while(cacheIterator.hasNext()) {
            if(cacheIterator.next().getUUID().equals(horseID)) {
                return true;
            }
        }
        
        try {
            PreparedStatement query = database.getConnection().prepareStatement("SELECT 1 FROM HORSES WHERE UUID = ?");
            query.setString(1, horseID.toString());
            ResultSet result = query.executeQuery();
            database.closeConnection();
            if(result.isBeforeFirst()) {
                return true; 
            } else {
                return false;
            }
        } catch (SQLException ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    public void addHorse(Horse horse, UUID owner) {
        LockedHorse lhorse = new LockedHorse(horse, owner);
        horseCache.add(lhorse);
        try {
            PreparedStatement update = database.getConnection().prepareStatement("INSERT INTO HORSES VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
            update.setString(1, lhorse.getUUID().toString());
            update.setString(2, lhorse.getOwner().toString());
            update.setString(3, lhorse.getName());
            update.setString(4, lhorse.getNickname());
            update.setString(5, lhorse.getAppearance());
            update.setString(6, lhorse.getArmor());
            update.setString(7, lhorse.getSaddle());
            update.setString(8, lhorse.getChest());
            update.setString(9, lhorse.getLocation());
            update.setInt(10, lhorse.getMaxHealth());
            update.setDouble(11, lhorse.getMaxSpeed());
            update.setDouble(12, lhorse.getJumpHeight());
            database.runAsyncUpdate(update);
        } catch (SQLException ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
        }
    }
    
    public boolean batchAddLockedHorses(ArrayList<LockedHorse> lhorses) {
        try {
            PreparedStatement batchHorses = database.getConnection().prepareStatement("INSERT INTO HORSES VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
            PreparedStatement batchACL = database.getConnection().prepareStatement("INSERT INTO ACL VALUES (?,?)");
            for(LockedHorse lhorse : lhorses) {
                horseCache.add(lhorse);
                batchHorses.setString(1, lhorse.getUUID().toString());
                batchHorses.setString(2, lhorse.getOwner().toString());
                batchHorses.setString(3, lhorse.getName());
                batchHorses.setString(4, lhorse.getNickname());
                batchHorses.setString(5, lhorse.getAppearance());
                batchHorses.setString(6, lhorse.getArmor());
                batchHorses.setString(7, lhorse.getSaddle());
                batchHorses.setString(8, lhorse.getChest());
                batchHorses.setString(9, lhorse.getLocation());
                batchHorses.setInt(10, lhorse.getMaxHealth());
                batchHorses.setDouble(11, lhorse.getMaxSpeed());
                batchHorses.setDouble(12, lhorse.getJumpHeight());
                batchHorses.addBatch();
                for(UUID uuid : lhorse.getAccessList()) {
                    batchACL.setString(1, lhorse.getUUID().toString());
                    batchACL.setString(2, uuid.toString());
                    batchACL.addBatch();
                }
            }
            batchHorses.executeBatch();
            batchACL.executeBatch();
            return true;
        } catch (SQLException ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    public void removeHorse(UUID horseID) {
        LockedHorse lhorse = getHorse(horseID);
        horseCache.remove(lhorse);
        try {
            PreparedStatement update = database.getConnection().prepareStatement("DELETE FROM HORSES WHERE UUID = ?");
            update.setString(1, horseID.toString());
            database.runAsyncUpdate(update);
        } catch (SQLException ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
        }
    }
    
    public void addAccess(UUID horseID, UUID playerID) {
        try {
            PreparedStatement update = database.getConnection().prepareStatement("INSERT INTO ACL VALUES (?, ?)");
            update.setString(1, horseID.toString());
            update.setString(2, playerID.toString());
            database.runAsyncUpdate(update);
        } catch (SQLException ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
        }
    }
    
    public void removeAccess(UUID horseID, UUID playerID) {
        try {
            PreparedStatement update = database.getConnection().prepareStatement("DELETE FROM ACL WHERE UUID = ? AND Player = ?");
            update.setString(1, horseID.toString());
            update.setString(2, playerID.toString());
            database.runAsyncUpdate(update);
        } catch (SQLException ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
        }
    }
    
    public int size() {
        try {
            PreparedStatement query = database.getConnection().prepareStatement("SELECT COUNT(*) FROM HORSES");
            ResultSet result = query.executeQuery();
            return result.getInt(1);
        } catch (SQLException ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
        }
        return 0;
    }
}
