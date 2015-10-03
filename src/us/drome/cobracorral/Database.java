package us.drome.cobracorral;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
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

/**
* The Database class provides an abstraction for all SQL interactions for the plugin.
*/  
public class Database {
    private SQLEngine database;
    private Set<LockedHorse> horseCache;
    
    /**
     * Constructs a new Database object that provides all SQL interaction for the plugin.
     * @param backend the SQLEngine object that contains the connection to the database.
     * @throws java.lang.Exception Will re-throw exceptions caught during database initialization to halt the plugin.
     */  
    public Database(SQLEngine backend) throws Exception {
        //Set several c3p0 properties to disable additional logging
        System.setProperty("com.mchange.v2.log.MLog", "com.mchange.v2.log.FallbackMLog");
        System.setProperty("com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL", "OFF");
        System.setProperty("com.mchange.v2.c3p0.management.ManagementCoordinator","com.mchange.v2.c3p0.management.NullManagementCoordinator");
        
        this.database = backend;
        horseCache = new LinkedHashSet<>();
        String initHorsesUpdate = "CREATE TABLE IF NOT EXISTS HORSES (" +
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
                    "JumpHeight FLOAT);";
        String initACLUpdate = "CREATE TABLE IF NOT EXISTS ACL (" +
                    "Horse NVARCHAR(36) NOT NULL," +
                    "Player NVARCHAR(36) NOT NULL);";
        
        try {
            PreparedStatement initializeHorses = database.getConnection().prepareStatement(initHorsesUpdate);
            PreparedStatement initializeACL = database.getConnection().prepareStatement(initACLUpdate);
            database.runAsyncUpdate(initializeHorses);
            database.runAsyncUpdate(initializeACL);
            database.getLogger().info("Database successfully initialized.");
        } catch (Exception ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
            throw ex;
        }
    }
    
    /**
     * @return Returns the backend database engine object.
     */  
    public SQLEngine getEngine() {
        return database;
    }
    
    /**
     * Clears all cached LockedHorse objects for the provided player UUID.
     * @param playerID The UUID of the player whose horses will be removed from the local cache.
     */  
    public void clearCache(UUID playerID) {
        for(Iterator<LockedHorse> horseIterator = horseCache.iterator(); horseIterator.hasNext();) {
            LockedHorse lhorse = horseIterator.next();
            if(lhorse.getOwner().equals(playerID)) {
                horseIterator.remove();
            }
        }
    }
     
    /**
     * Retrieves the current size of the database.
     * @return The number of records in the database.
     */
    public int size() {
        String queryString = "SELECT COUNT(*) FROM HORSES";
        try (
            Connection con = database.getConnection();
            PreparedStatement query = con.prepareStatement(queryString);
            ResultSet result = query.executeQuery();
        ){
            return result.getInt(1);
        } catch (Exception ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
        }
        return 0;
    }
    
    /**
     * Returns the amount of LockedHorse that a player has by Player UUID.
     * @param playerID The UUID of the Player to count records for.
     * @return The number of LockedHorse records found.
     */
    public int countLocked(UUID playerID) {
        String queryString = "SELECT COUNT(UUID) FROM HORSES WHERE Owner = ?";
        try (
            Connection con = database.getConnection();
            PreparedStatement query = con.prepareStatement(queryString);
        ){  
            query.setString(1, playerID.toString());
            try (ResultSet result = query.executeQuery(); ){
                if(result.isBeforeFirst()) {
                    result.next();
                    return result.getInt(1);
                }
            }
        } catch (Exception ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
        }
        return 0;
    }    
    
    /**
     * Returns whether or not the live cache or database contains a LochedHorse with the provided Horse UUID.
     * @param horseID The UUID of the Horse to find in the cache or database.
     * @return Whether or not the cache or database contains a matching LockedHorse.
     */
    public boolean contains(UUID horseID) {
        //Iterate through the live cache and attempt to match the UUID.
        for(Iterator<LockedHorse> cacheIterator = horseCache.iterator(); cacheIterator.hasNext();) {
            if(cacheIterator.next().getUUID().equals(horseID)) {
                return true;
            }
        }
        
        //If the Iteration fails to find a match, attempt to find the UUID in the database.
        String queryString = "SELECT 1 FROM HORSES WHERE UUID = ?";
        try (
            Connection con = database.getConnection();
            PreparedStatement query = con.prepareStatement(queryString);
        ){
            boolean contains = false;
            query.setString(1, horseID.toString());
            try (ResultSet result = query.executeQuery()) {
                if(result.isBeforeFirst()) {
                    contains = true;
                }
            }
            return contains;
        } catch (Exception ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    /**
     * Adds a new LockedHorse to the live cache and database.
     * @param horse The Horse to be locked and added to the cache/database.
     * @param owner The UUID of the Player that owns the Horse.
     */    
    public void addHorse(Horse horse, UUID owner) {
        LockedHorse lhorse = new LockedHorse(horse, owner);
        horseCache.add(lhorse);
        
        String updateString = "INSERT INTO HORSES VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement update = database.getConnection().prepareStatement(updateString);
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
        } catch (Exception ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Adds a List of LockedHorse objects to the cache and database.
     * This is only used for the old configuration conversion change.
     * @param lhorses A List of LockedHorse objects to be added to the cache and database.
     * @return Whether or not the batch update was successful.
     */    
    public boolean addHorses(List<LockedHorse> lhorses) {
        String horsesString = "INSERT OR IGNORE INTO HORSES VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        String aclString = "INSERT OR IGNORE INTO ACL VALUES (?,?)";
        try {
            PreparedStatement batchHorses = database.getConnection().prepareStatement(horsesString);
            PreparedStatement batchACL = database.getConnection().prepareStatement(aclString);
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
            database.runAsyncBatchUpdate(batchHorses);
            database.runAsyncBatchUpdate(batchACL);
            return true;
        } catch (Exception ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
        }
        return false;
    }  
    
    /**
     * Returns a LockedHorse object from the database or live cache that match a specified Horse UUID.
     * @param horseID The UUID of the Horse to pull from the cache or database.
     * @return A LockedHorses that matches horseID.
     */
    public LockedHorse getHorse(UUID horseID) {
        LockedHorse lhorse = null;
        
        //Iterate through the live cache of LockedHorse objects and return the object if the UUID matches.
        for(Iterator<LockedHorse> cacheIterator = horseCache.iterator(); cacheIterator.hasNext();) {
            lhorse = cacheIterator.next();
            if(lhorse.getUUID().equals(horseID)) {
                return lhorse;
            }
            lhorse = null;
        }
        
        //If the cache iteration is not successful, run a query against the database to find the LockedHorse data.
        String queryString = "SELECT * FROM HORSES LEFT JOIN ACL ON HORSES.UUID=ACL.Horse WHERE UUID = ?";
        try (
            Connection con = database.getConnection();
            PreparedStatement query = con.prepareStatement(queryString);
        ){
            query.setString(1, horseID.toString());
            try (ResultSet result = query.executeQuery()) {
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
            }
        } catch (Exception ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
        }
        return lhorse;
    }    
    
    /**
     * Returns a Set of LockedHorse objects from the database or live cache that match a specified Player UUID.
     * @param playerID UUID of the player whose LockedHorses to retrieve.
     * @return A Set of LockedHorses that the player owns.
     */
    public Set<LockedHorse> getHorses(UUID playerID) {
        LockedHorse lhorse;
        Set<LockedHorse> horses = new LinkedHashSet<>();
        
        //Iterate through the live cache and if the UUID matches add the LockedHorse to the horses Set.
        for(Iterator<LockedHorse> cacheIterator = horseCache.iterator(); cacheIterator.hasNext();) {
            lhorse = cacheIterator.next();
            if(lhorse.getOwner().equals(playerID)) {
                horses.add(lhorse);
            }
        }

        //If the horses Set is empty or smaller than the amount of horses this player has locked, query the database.
        if(horses.size() < countLocked(playerID)) {
            String queryString = "SELECT * FROM HORSES LEFT JOIN ACL ON HORSES.UUID=ACL.Horse WHERE Owner = ?";
            try (
                Connection con = database.getConnection();
                PreparedStatement query = con.prepareStatement(queryString);
            ){
                query.setString(1, playerID.toString());
                try (ResultSet result = query.executeQuery()) {
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
                }
            } catch (Exception ex) {
                database.getLogger().log(Level.SEVERE, null, ex);
            }
        }
        return horses;
    }
    
    /**
     * Returns a Set of LockedHorse objects from database or live cache that match a Collection of Horse UUIDs.
     * @param horseIDs A Collection of Horse UUID to retrieve from the cache or database.
     * @return The Set of LockedHorse objects that match the provided UUIDs.
     */    
    public Set<LockedHorse> getHorsesByID(Collection<UUID> horseIDs) {
        Set<LockedHorse> horses = new LinkedHashSet<>();
        LockedHorse lhorse;
        
        //Iterate through the live cache and add the LockedHorses to the set, remove those UUIDs from the list.
        for(Iterator<LockedHorse> cacheIterator = horseCache.iterator(); cacheIterator.hasNext();) {
            lhorse = cacheIterator.next();
            for(Iterator<UUID> iterator = horseIDs.iterator(); iterator.hasNext();) {
                UUID id = iterator.next();
                if(lhorse.getUUID().equals(id)) {
                    horses.add(lhorse);
                    iterator.remove();
                }
            }
        }
        
        //Iterate through the remaining UUIDs and attempt to acquire them from the database.
        
        //Construct a PreparedStatement string dynamic to the size of the horseIDs Collection.
        String queryString = "SELECT * FROM HORSES LEFT JOIN ACL ON HORSES.UUID=ACL.Horse WHERE UUID IN (";
        StringBuilder queryBuilder = new StringBuilder(queryString);
        for(int i = 0; i < horseIDs.size(); i++) {
            queryBuilder.append(" ?");
            if(i != horseIDs.size() - 1) {
                queryBuilder.append(",");
            }
        }
        queryBuilder.append(")");
        try (
            Connection con = database.getConnection();
            PreparedStatement query = con.prepareStatement(queryBuilder.toString());
        ){
            int counter = 1;
            for(UUID id : horseIDs) {
                query.setString(counter, id.toString());
                counter++;
            }
            try (ResultSet result = query.executeQuery()) {
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
            }
        } catch (Exception ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
        }
        return horses;
    }
    
      /**
     * Updates the database record for the provided LockedHorse object.
     * @param lhorse The LockedHorse to update in the database.
     */  
    public void updateHorse(LockedHorse lhorse) {
        String updateString = "UPDATE HORSES SET UUID = ?, Owner = ?, Name = ?, Nickname = ?, Appearance = ?, Armor = ?, Saddle = ?,"
                    + "Chest = ?, Location = ?, MaxHealth = ?, MaxSpeed = ?, JumpHeight = ? WHERE UUID = ?";
        try {
            PreparedStatement update = database.getConnection().prepareStatement(updateString);
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
        } catch (Exception ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Updates multiple LockedHorse records on the database by the provided Set of LockedHorse objects.
     * @param lhorses A Set of LockedHorse objects to update in the database.
     * @return Whether or not the batch update was successful.
     */        
    public boolean updateHorses(Set<LockedHorse> lhorses) {
        String updateString = "UPDATE HORSES SET UUID = ?, Owner = ?, Name = ?, Nickname = ?, Appearance = ?, Armor = ?, Saddle = ?,"
                    + "Chest = ?, Location = ?, MaxHealth = ?, MaxSpeed = ?, JumpHeight = ? WHERE UUID = ?";
        try {
            PreparedStatement batchHorses = database.getConnection().prepareStatement(updateString);
            for(LockedHorse lhorse : lhorses) {
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
            }
            database.runAsyncBatchUpdate(batchHorses);
            return true;
        } catch (Exception ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    /**
     * Removes a LockedHorse from the cache and database by the provided Horse UUID.
     * @param horseID The UUID of the Horse to remove from the cache and database.
     */     
    public void removeHorse(UUID horseID) {
        LockedHorse lhorse = getHorse(horseID);
        horseCache.remove(lhorse);
        
        String updateString = "DELETE FROM HORSES WHERE UUID = ?";
        try {
            PreparedStatement update = database.getConnection().prepareStatement(updateString);
            update.setString(1, horseID.toString());
            database.runAsyncUpdate(update);
        } catch (Exception ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Adds access to a LockedHorse by the provided Horse UUID and Player UUID.
     * @param horseID The UUID of the Horse to add access to.
     * @param playerID The UUID of the Player who will receive access.
     */    
    public void addAccess(UUID horseID, UUID playerID) {
        String updateString = "INSERT INTO ACL VALUES (?, ?)";
        try {
            PreparedStatement update = database.getConnection().prepareStatement(updateString);
            update.setString(1, horseID.toString());
            update.setString(2, playerID.toString());
            database.runAsyncUpdate(update);
        } catch (Exception ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Removes access from a LockedHorse by the provided Horse UUID and Player UUID.
     * @param horseID The UUID of the Horse to remove access from.
     * @param playerID The UUID of the Player who will lose access.
     */    
    public void removeAccess(UUID horseID, UUID playerID) {
        String updateString = "DELETE FROM ACL WHERE Horse = ? AND Player = ?";
        try {
            PreparedStatement update = database.getConnection().prepareStatement(updateString);
            update.setString(1, horseID.toString());
            update.setString(2, playerID.toString());
            database.runAsyncUpdate(update);
        } catch (SQLException ex) {
            database.getLogger().log(Level.SEVERE, null, ex);
        }
    }
}
