package org.eclipse.jetty.nosql.mongo;

import java.util.Calendar;

import javax.naming.InitialContext;

import org.eclipse.jetty.nosql.ext.IExtKeyValueStoreClient;
import org.eclipse.jetty.nosql.kvs.AbstractKeyValueStoreClient;
import org.eclipse.jetty.nosql.kvs.KeyValueStoreClientException;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

/**
 * Provides the Implementation of Session Storage to a MongoDB.
 * 
 * @author David Ranalli
 * Jan 28, 2016
 */
public class MongoClient extends AbstractKeyValueStoreClient implements IExtKeyValueStoreClient {
  private final static Logger log = Log.getLogger( MongoClient.class.getName() );
  /* Jetty session id - key for session */
  private static final String KEY_ID = "id";
  /* Field used by MongoDb to expire entry at that exact time */
  private static final String KEY_EXPIRY = "expiry";
  /* Current version of session ( increased as changes occur ) */
  private static final String KEY_VERSION = "version";
  /*  Raw session data */
  private static final String KEY_SESSION = "session";
  
  /* MongoDb accessor */
  private DBCollection collection;
  
  /**
   * Create a MongoClient with the context string key of the DbCollection to use
   * @param serverString
   */
  public MongoClient( String serverString ) {
    super( serverString );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean establish() throws KeyValueStoreClientException {
    try {
      // Look up the DbCollection by serverString
      InitialContext ic = new InitialContext();
      collection = (DBCollection) ic.lookup( getServerString() );

    }
    catch( Exception e ) {
      throw new KeyValueStoreClientException( e.toString() );
    }
    
    // Ensure primary key index is created
    collection.ensureIndex(
        BasicDBObjectBuilder.start().add(KEY_ID,1).get(),
        BasicDBObjectBuilder.start().add("unique",true).add("sparse",false).get());
    
    // Create an index that tells Mongo to expire any entries based on the exipre field.
    collection.ensureIndex(
        BasicDBObjectBuilder.start().add(KEY_EXPIRY,1).get(),
        BasicDBObjectBuilder.start().add( "expireAfterSeconds", 0 ).add("unique",false).add("sparse",false).get());
    
    return true;
  }
 
  @Override
  public boolean shutdown() throws KeyValueStoreClientException {
    return true;
  }

  @Override
  public boolean isAlive() {
    return true;
  }

  @Override
  public byte[] get( String key ) throws KeyValueStoreClientException {
    try {
      DBObject found = find( key );
      return found != null ? (byte[])found.get( KEY_SESSION ) : null;
    }
    catch( RuntimeException e ) {
      throw new KeyValueStoreClientException( e.getMessage() );
    }
  }
  

  @Override
  public boolean set( String key, byte[] raw ) throws KeyValueStoreClientException {
    // there are no references to this method
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean set( String key, byte[] raw, int exp ) throws KeyValueStoreClientException {
    return set( key, 0, raw, exp );
  }

  @Override
  public boolean add( String key, byte[] raw ) throws KeyValueStoreClientException {
    return add( key, raw, 0 );
  }

  @Override
  public boolean add( String key, byte[] raw, int exp ) throws KeyValueStoreClientException {
    return add( key, 0, raw, exp );
  }

  @Override
  public boolean delete( String key ) throws KeyValueStoreClientException {
    WriteResult result = null;
    try
    {
      log.debug("MongoClient: delete session {}", key);
      result = collection.remove( new BasicDBObject( KEY_ID, key ), WriteConcern.SAFE );
    }
    catch ( Exception e ) {
      log.warn(e);
      throw new KeyValueStoreClientException( e.getMessage() );
    }
    
    return result != null && result.getN() > 0;
  }

  @Override
  public boolean exists( String key ) throws KeyValueStoreClientException {
    try {
      return collection.count(  new BasicDBObject( KEY_ID, key )  ) > 0;
    }
    catch ( Exception e ) {
      log.warn(e);
      throw new KeyValueStoreClientException( e.getMessage() );
    }    
  }
  
  private DBObject find( String key ) {
    return collection.findOne( new BasicDBObject( KEY_ID, key ) );
  }

  @Override
  public boolean set( String key, long version, byte[] raw ) throws KeyValueStoreClientException {
    return set( key, version, raw, 0 );
  }

  @Override
  public boolean set( String key, long version, byte[] raw, int exp ) throws KeyValueStoreClientException {
    try {
        log.debug("MongoClient: save session {}", key );
        
        // Form query for upsert
        BasicDBObject dbKey = new BasicDBObject(KEY_ID, key );

        // Form updates
        BasicDBObject update = new BasicDBObject();
        boolean upsert = true;
        update.put( KEY_ID, key );
        
        // Allow saving meta data for session only, without updating session data
        if ( raw != null )
          update.put( KEY_SESSION, raw );
        
        update.put( KEY_VERSION, version );
        
        if ( exp > 0 ) {
          Calendar c = Calendar.getInstance();
          c.add( Calendar.SECOND, exp );
          update.put( KEY_EXPIRY, c.getTime() );
        }

        collection.update(dbKey,update,upsert,false,WriteConcern.SAFE);

        log.debug("MongoClient: save:db.sessions.update( {}, {} )", key, update);
       
    }
    catch (Exception e)
    {
        log.warn(e);
        throw new KeyValueStoreClientException( e.getMessage() );
    }
    return true;
  }

  @Override
  public boolean add( String key, long version, byte[] raw ) throws KeyValueStoreClientException {
    return add( key, version, raw, 0 );
  }

  @Override
  public boolean add( String key, long version, byte[] raw, int exp ) throws KeyValueStoreClientException {
    WriteResult result = null;
    try
    {
        log.debug("MongoClient: add session {}", key );

        // Form insert
        BasicDBObject insert = new BasicDBObject();
        
        insert.put( KEY_ID, key );
        insert.put( KEY_SESSION, raw );
        insert.put( KEY_VERSION, version );
        if ( exp > 0 ) {
          Calendar c = Calendar.getInstance();
          c.add( Calendar.SECOND, exp );
          insert.put( KEY_EXPIRY, c.getTime() );
        }

        result = collection.insert(insert,WriteConcern.SAFE);

        log.debug("MongoClient: add:db.sessions.insert( {}, {} )", key, insert);
    }
    catch (Exception e)
    {
      log.warn(e);
      throw new KeyValueStoreClientException( e.getMessage() );
    }

    return result != null && result.getN() > 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long version( String key ) {
    DBObject versionObj = collection.findOne( new BasicDBObject( KEY_ID, key ), new BasicDBObject( KEY_VERSION, 1 ) );
    if ( versionObj == null || !(versionObj.get( KEY_VERSION ) instanceof Number) )
      return 0l; // TODO log error...
    return ((Number)versionObj.get( KEY_VERSION )).longValue();
  }

}
