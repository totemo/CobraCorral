package us.drome.cobrasql;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class allows for the standardized connection to a SQL database and the ability to send
 * synchronous or asynchronous queries to it directly via SQL or through the <tt>Table</tt> class.
 * 
 * @author TheAcademician
 * @since 0.1
 */
public abstract class SQLEngine {
    protected final Logger logger;
    protected Connection connection;
    /**
     * ExecutorService is used to run queries asynchronously. It is instantiated
     * as a SingleThreadExecutor to queue all database operations so they are executed in order.
     */
    private final ExecutorService queryExecutor;
    
    protected SQLEngine (Logger logger){
        this.logger = logger;
        this.queryExecutor = Executors.newSingleThreadExecutor();
    }

    public void runAsyncQuery (final PreparedStatement query, final Callback callback){
        queryExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.invoke(runQuery(query));
                } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                    logger.log(Level.SEVERE, e.getMessage());
                }
            }
        });
    }

    public void runAsyncUpdate (final PreparedStatement update) {
        queryExecutor.execute(new Runnable() {
           @Override
           public void run() {
               runUpdate(update);
           }
        });
    }
    
    public Map<String, Object> runQuery(PreparedStatement query) {
        Connection conn = getConnection();
        ResultSet result;
        ResultSetMetaData resultMeta;
        Map<String, Object> resultMap = new HashMap<>();
        PreparedStatement statement;
        
        try {
            conn.setAutoCommit(false);
            result = query.executeQuery();
            resultMeta = result.getMetaData();
            while(result.next()) {
                for(int i = 1 ; i <= resultMeta.getColumnCount() ; i++) {
                    resultMap.put(resultMeta.getColumnName(i), result.getObject(i));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage());
            if(conn != null) {
                try {
                    logger.log(Level.SEVERE, e.getMessage() + " Attempting to roll back query.");
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, e.getMessage());
                }
            }
        } finally {
            try { conn.setAutoCommit(true);} catch (SQLException e) { logger.log(Level.SEVERE, e.getMessage()); }
        }
        return resultMap;
    }
    
    public void runUpdate(PreparedStatement update) {
        Connection conn = getConnection();
        try {
            conn.setAutoCommit(false);
            update.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage());
             try {
                logger.log(Level.SEVERE, e.getMessage() + " Attempting to roll back update.");
                conn.rollback();
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, e.getMessage());
            }
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { logger.log(Level.SEVERE, e.getMessage()); }
        }
        
    }
 
    public abstract Connection getConnection();
    
    /**
     * Closes any open connections to the database.
     */
    public void closeConnection() {
        if(connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, e.getMessage());
            }
        } else {
            logger.log(Level.WARNING, "There are no connections to close.");
        }
    }
    
    /**
     * Properly shuts down this database connection including the query executor that could potentially hang the process it is running on.
     */
    public void shutdown() {
        if(queryExecutor != null) {
            queryExecutor.shutdown();
        }
        if(connection != null) {
            closeConnection();
        }
        logger.log(Level.INFO, "Database engine has been successfully shut down.");
    }
    
    /**
     * Protected method to retrieve the asynchronous executor.
     * @return The executor used to run asynchronous queries.
     */
    public ExecutorService getExecutor() { return this.queryExecutor; }
    
    public abstract String getName();
    
    public Logger getLogger() {
        return logger;
    }
}
