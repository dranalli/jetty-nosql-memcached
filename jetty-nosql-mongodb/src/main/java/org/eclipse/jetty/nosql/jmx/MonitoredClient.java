package org.eclipse.jetty.nosql.jmx;

import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jetty.nosql.kvs.AbstractKeyValueStoreClient;
import org.eclipse.jetty.nosql.kvs.IKeyValueStoreClient;
import org.eclipse.jetty.nosql.kvs.KeyValueStoreClientException;

/**
 * Provides a monitored Implementation of Session Storage the delegates to an actual client.
 * 
 * @author David Ranalli
 * Jan 28, 2016
 */
public class MonitoredClient extends AbstractKeyValueStoreClient implements SessionStorageMBean, IKeyValueStoreClient {
  private final AtomicLong updates = new AtomicLong();
  private final AtomicLong inserts = new AtomicLong();
  private final AtomicLong deletes = new AtomicLong();
  private final AtomicLong reads = new AtomicLong();
  private final AtomicLong errors = new AtomicLong();
  private final AtomicLong updateTime = new AtomicLong();
  private final AtomicLong insertTime = new AtomicLong();
  private final AtomicLong deleteTime = new AtomicLong();
  private final AtomicLong readTime = new AtomicLong();
  private final AtomicLong errorTime = new AtomicLong();
  private final AtomicLong dataIn = new AtomicLong();
  private final AtomicLong dataOut = new AtomicLong();
  private final IKeyValueStoreClient client;
  
  public MonitoredClient( AbstractKeyValueStoreClient client ) {
    super( client.getServerString() );
    this.client = client;
  }

  @Override
  public boolean establish() throws KeyValueStoreClientException {
    client.establish();
    JmxMonitorManager.monitor( new SessionStorage( this ), "SessionClustering", client.getClass().getSimpleName().toLowerCase(), getServerString() );

    return true;
  }
 
//  public static void main( String[] args ) throws InterruptedException {
//    JmxMonitorManager.monitor( new SessionStorage( new MonitoredClient( "dave") ), "SessionClustering", "mongo","dave" );
//    while ( true )  {
//       Thread.sleep( 1000 ); 
//    }
//  }
  
  @Override
  public boolean shutdown() throws KeyValueStoreClientException {
    JmxMonitorManager.remove( "SessionClustering", client.getClass().getSimpleName().toLowerCase(), getServerString() );
    return true;
  }

  @Override
  public boolean isAlive() {
    return client.isAlive();
  }

  @Override
  public byte[] get( String key ) throws KeyValueStoreClientException {
    long start = System.currentTimeMillis();
    try {
      byte[] bytes = client.get( key );
      track( start, bytes, reads, readTime, dataIn );

      return bytes;
    }
    catch( KeyValueStoreClientException e ) {
      track( start, null, errors, errorTime, null );
      throw e;
    }
  }
  

  private void track( long start, byte[] bytes, AtomicLong count, AtomicLong time, AtomicLong data ) {
    long duration = System.currentTimeMillis() - start;
    count.incrementAndGet();
    time.addAndGet( duration );
    if ( bytes != null )
      data.addAndGet( bytes.length );
  }

  @Override
  public boolean delete( String key ) throws KeyValueStoreClientException {
    long start = System.currentTimeMillis();
    
    boolean ret = false;
    try
    {
      ret = client.delete( key );
    }
    catch ( KeyValueStoreClientException e ) {
      track( start, null, errors, errorTime, null );
      throw e;
    }
    
    track( start, null, deletes, deleteTime, null );
    return ret;
  }

  @Override
  public boolean exists( String key ) throws KeyValueStoreClientException {
    long start = System.currentTimeMillis();
    boolean ret = false;
    try
    {
      ret = client.exists( key );
    }
    catch ( Exception e ) {
      track( start, null, errors, errorTime, null );
      throw e;
    }
    
    track( start, null, reads, readTime, null );
    return ret;
  }
  
  @Override
  public long getReads() {
    return reads.get();
  }
  
  @Override
  public long getWrites() {
    return inserts.get() + updates.get() + deletes.get();
  }
  
  @Override
  public long getUpdates() {
    return updates.get();
  }

  @Override
  public long getInserts() {
    return inserts.get();
  }

  @Override
  public long getDeletes() {
    return deletes.get();
  }
  
  @Override
  public long getErrors() {
    return errors.get();
  }

  @Override
  public long getReadTime() {
    return readTime.get();
  }

  @Override
  public long getWriteTime() {
    return updateTime.get() + insertTime.get() + deleteTime.get();
  }

  @Override
  public long getInsertTime() {
    return insertTime.get();
  }

  @Override
  public long getUpdateTime() {
    return updateTime.get();
  }

  @Override
  public long getDeleteTime() {
    return deleteTime.get();
  }

  @Override
  public long getErrorTime() {
    return errorTime.get();
  }

  @Override
  public long getDataInSizeKB() {
    return dataIn.get() / 1024;
  }

  @Override
  public long getDataOutSizeKB() {
    return dataOut.get() / 1024;
  }

  @Override
  public boolean set( String key, long version, byte[] raw ) throws KeyValueStoreClientException {
    return set( key, version, raw, 0 );
  }

  @Override
  public boolean set( String key, long version, byte[] raw, int exp ) throws KeyValueStoreClientException {
    long start = System.currentTimeMillis();

    boolean ret = false;
    try
    {
      ret = client.set( key, version, raw, exp );
    }
    catch (KeyValueStoreClientException e)
    {
        track( start, null, errors, errorTime, null );
        throw e;
    }
    track( start, raw, updates, updateTime, dataOut );
    return ret;
  }

  @Override
  public boolean add( String key, long version, byte[] raw ) throws KeyValueStoreClientException {
    return add( key, version, raw, 0 );
  }

  @Override
  public boolean add( String key, long version, byte[] raw, int exp ) throws KeyValueStoreClientException {
    long start = System.currentTimeMillis();
    boolean ret = false;
    try
    {
      ret = client.add( key, version, raw, exp );
    }
    catch (Exception e)
    {
      track( start, null, errors, errorTime, null );
      throw e;
    }

    track( start, raw, inserts, insertTime, dataOut );
    
    return ret;
  }

  @Override
  public long version( String key ) {
    long start = System.currentTimeMillis();
    try
    {
      long version = client.version( key );
      track( start, null, reads, readTime, null );
      return version;
    }
    catch ( Exception e ) {
      track( start, null, errors, errorTime, null );
      throw e;
    }
    
  }
}
