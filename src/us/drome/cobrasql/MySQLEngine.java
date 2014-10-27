package us.drome.cobrasql;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a connection to a MySQL database.
 * 
 * @author TheAcademician
 * @since 0.1
 */
public class MySQLEngine extends SQLEngine {
    private final String url;
    private final String username;
    private final String password;
    
    /**
     * Construct a new <tt>MySQLEngine</tt> by specifying a Logger for output and the necessary connection data.
     * @param logger The output provider for this engine.
     * @param hostname The server hosting the database.
     * @param port The port the server is using.
     * @param database The name of the database instance.
     * @param username The username with access to this database.
     * @param password The password for the provided username.
     */
    public MySQLEngine(Logger logger, String hostname, int port, String database, String username, String password) {
        super(logger);
        this.url = hostname + ":" + String.valueOf(port) + "/" + database;
        this.username = username;
        this.password = password;
    }

    /**
     * @return A <tt>Connection</tt> object to provide connectivity with the database.
     */
    @Override
    public Connection getConnection() {
        try {
            if(pool != null) {
                return pool.getConnection();
            } else {
                return openConnection();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage());
        } 
        return null;
    }
    
    private Connection openConnection() throws SQLException {
        try {
            pool = new ComboPooledDataSource();
            pool.setDriverClass("com.mysql.jdbc.Driver");
            pool.setJdbcUrl("jdbc:mysql://" + url);
            pool.setUser(username);
            pool.setPassword(password);
            pool.setMaxPoolSize(50);
            return pool.getConnection();
        } catch (PropertyVetoException ex) {
            throw new SQLException("Cannot load MySQL. Check your installation and try again.");
        }
    }

    @Override
    public String getName() {
        return "MySQL";
    }
}
