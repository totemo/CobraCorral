package us.drome.cobrasql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.util.concurrent.TimeUnit;

/**
 * Class allows for the standardized connection to a SQL database and the ability to send
 * synchronous or asynchronous queries to it directly via SQL or through the <tt>Table</tt> class.
 * 
 * @author TheAcademician
 * @since 0.1
 */
public abstract class SQLEngine {
    protected final Logger logger;
    protected ComboPooledDataSource pool;
    /**
     * ExecutorService is used to run queries asynchronously. It is instantiated
     * as a SingleThreadExecutor to queue all database operations so they are executed in order.
     */
    private final ExecutorService queryExecutor;
    
    protected SQLEngine (Logger logger){
        this.logger = logger;
        this.queryExecutor = Executors.newSingleThreadExecutor();
    }

    public void runAsyncUpdate (final PreparedStatement update) {
        queryExecutor.execute(new Runnable() {
           @Override
           public void run() {
               runUpdate(update);
           }
        });
    }
    
    public void runAsyncBatchUpdate (final PreparedStatement batchUpdate) {
                queryExecutor.execute(new Runnable() {
           @Override
           public void run() {
               runBatchUpdate(batchUpdate);
           }
        });
    }
    
    public void runUpdate(PreparedStatement update) {
        try {
            update.getConnection().setAutoCommit(false);
            update.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage());
             try {
                logger.log(Level.SEVERE, e.getMessage() + " Attempting to roll back update.");
                update.getConnection().rollback();
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, e.getMessage());
            }
        } finally {
            try { update.getConnection().setAutoCommit(true); update.getConnection().close(); update.close(); } catch (SQLException e) { logger.log(Level.SEVERE, e.getMessage()); }
        }
    }
    
    public void runBatchUpdate(PreparedStatement batchUpdate) {
        try {
            batchUpdate.getConnection().setAutoCommit(false);
            batchUpdate.executeBatch();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage());
             try {
                logger.log(Level.SEVERE, e.getMessage() + " Attempting to roll back update.");
                batchUpdate.getConnection().rollback();
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, e.getMessage());
            }
        } finally {
            try { batchUpdate.getConnection().setAutoCommit(true); batchUpdate.getConnection().close(); batchUpdate.close(); } catch (SQLException e) { logger.log(Level.SEVERE, e.getMessage()); }
        }
    }
 
    public abstract Connection getConnection();
    
    /**
     * Properly shuts down this database connection including the query executor that could potentially hang the process it is running on.
     */
    public void shutdown() {
        if(queryExecutor != null) {
            queryExecutor.shutdown();
            try {
                queryExecutor.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        if(pool != null) {
            pool.close();
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
