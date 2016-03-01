package org.eclipse.jetty.nosql.mongo;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jetty.nosql.NoSqlSession;
import org.eclipse.jetty.nosql.kvs.KeyValueStoreSessionManager;
import org.eclipse.jetty.nosql.kvs.session.ISerializableSession;
import org.eclipse.jetty.nosql.kvs.session.TranscoderException;
import org.eclipse.jetty.server.session.AbstractSession;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class MongoSessionManager extends KeyValueStoreSessionManager {
	private final static Logger log = Log.getLogger( MongoSessionManager.class.getName() );
	
	/**
	 * Handles execution of clean up tasks of expired local sessions.
	 */
	ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor( 5, new ThreadFactory() {
	    private AtomicLong threadCount = new AtomicLong();
	    
	    @Override public Thread newThread( Runnable r ) {
	      Thread t = new Thread( r, "MongoSession Cleanup Thread " + threadCount.incrementAndGet() );
	      t.setDaemon( true );
	      return t;
	    }
	    
	  } );
	
	private ConcurrentMap<String, SessionHolder> localSessions = new ConcurrentHashMap<>();
	
	private static class SessionHolder {
      AbstractSession session;
	  String hash;
	  
      public SessionHolder( AbstractSession session, String hash ) {
        this.session = session;
        this.hash= hash;
      }
	}
	
	public MongoSessionManager() {
		super();
	}
	
	/**
	 * Schedule a check of the
	 * @param expireTime
	 * @param key
	 */
	private void scheduleExpire( final long expireTime, final String key ) {
	  executor.schedule( new Runnable(){
	    @Override
      public void run() {
	    SessionHolder sh = localSessions.get( key );
	    if ( sh != null ) {
	      NoSqlSession ns = (NoSqlSession)sh.session.getSession();
	      // This will check if the store has expired the session... if so refresh will remove it from the map.
	      // another option is to select version... if not there invalidate and remove(if not done by invalidate)... 
	      // if matches then add 30s, if old, then load and get lastAccess for next 
	      // check time... may need to store last accessed.
	      refresh( ns, ns.getVersion() );
	      if ( localSessions.containsKey( key ) ) {
	        // long newExpire = ( ns.getLastAccessedTime()  + ( getMaxInactiveInterval() * 1000 )) - System.currentTimeMillis();
	        // Give Mongo an extra 30 seconds to purge expired sessions if the session has already expired.
	        scheduleExpire( expireTime , key ); // Math.max( 30000, newExpire ), key );
	      }
	    }
	    
	  } }, expireTime + 1000, TimeUnit.MILLISECONDS ); // Add a second for a bit of a buffer.
	}

	@Override
	public void doStart() throws Exception {
		log.info("starting...");
		super.doStart();
		log.info("started.");
	}

	@Override
	public void doStop() throws Exception {
		log.info("stopping...");
		super.doStop();
		log.info("stopped.");
	}
	
	@Override
	public AbstractSession getSession( String idInCluster ) {
      SessionHolder sessionHolder = localSessions.get( idInCluster );
      AbstractSession session; 
      if ( sessionHolder == null ) {
	    session = super.getSession( idInCluster );
      }
      else 
        session = sessionHolder.session;
      return session;
	}
	
	@Override
	protected void addSession( AbstractSession session ) {
	  
	  localSessions.put( session.getClusterId(), getSessionHolder( session ) );
	  scheduleExpire( getMaxInactiveInterval() * 1000, session.getClusterId() );
	}
	
  /** 
	 * Note: session listeners will be invoked on multiple servers if this session
	 * was loaded in those servers.
	 * 
	 * @param idInCluster
	 * @return
	 */
	@Override
    protected boolean deleteKey(String idInCluster) {
	  super.deleteKey( idInCluster );
      return localSessions.remove( idInCluster ) != null;
	}
	
	@Override
	protected Object refresh( NoSqlSession session, Object version ) {
      log.debug("refresh " + session);
      Long saved = getSessionIdManager().version( session.getClusterId() );
      
      // check if our in memory version is the same as what is on KVS
      if ( version != null && Objects.equals( saved, version ) ) {
        log.debug("refresh not needed");
        return version;
      }

	  return super.refresh( session, version );
	}
	
    @Override
    protected boolean setKey(final String idInCluster, final ISerializableSession data) throws TranscoderException
    {
        SessionHolder sh = localSessions.get( idInCluster );
        String hash = null;
        
        if ( sh != null ) {
          hash = getBareSessionHash( data.getAttributeMap() );
          
          if( hash.equals( sh.hash ) )
            return getSessionIdManager().setKey(getSessionIdManager().mangleKey(idInCluster), data.getVersion(), null,
                getMaxInactiveInterval());
        }
        byte[] raw = getSessionFactory().pack(data);
        if (raw == null)
        {
            return false;
        }
        else
        {
            boolean result =  getSessionIdManager().setKey(getSessionIdManager().mangleKey(idInCluster), data.getVersion(), raw,
                getMaxInactiveInterval());
            if ( sh != null && hash != null )
              sh.hash = hash;
            return result;
        }
    }
    
    @Override
    public MongoSessionIdManager getSessionIdManager() {
      return (MongoSessionIdManager)super.getSessionIdManager();
    }
    
    private SessionHolder getSessionHolder( AbstractSession session ) {
//      String hash = getBareSessionHash( session.getAttributeMap() ); 
//      log.warn( hash );
      return new SessionHolder( session, "" );
    }
    
    private String getBareSessionHash( Map<String, Object> attributes ) {
      byte[] atts = getSessionFactory().pack( new SerializableSession( new HashMap<>( attributes ) ) );
      return Hash.getMD5Hex( atts ); 
    }
    
    private static class SerializableSession implements ISerializableSession {
      Map<String, Object> attributes;
      
      public SerializableSession( Map<String, Object> attributes ) {
        this.attributes = attributes;
      }

      @Override public String getId() { return null; }
      @Override public void setId( String id ) { }
      @Override public long getCreationTime() { return 0; }
      @Override public void setCreationTime( long created ) {}
      @Override public long getAccessed() { return 0; }
      @Override public void setAccessed( long accessed ) {}
      @Override public Map<String, Object> getAttributeMap() { return attributes; }
      @Override public void setAttributeMap( Map<String, Object> attributes ) { this.attributes = attributes; }
      @Override public Object getAttribute( String key ) { return null; }
      @Override public void setAttribute( String key, Object obj ) { }
      @Override public void removeAttribute( String key ) {}
      @Override public Enumeration<String> getAttributeNames() { return null; }
      @Override public boolean isValid() { return false; }
      @Override public void setValid( boolean valid ) {  }
      @Override public long getInvalidated() { return 0; }
      @Override public long getVersion() { return 0; }
      @Override public void setVersion( long version ) {}
      @Override public String getDomain() { return null; }
      @Override public void setDomain( String domain ) { }
      @Override public String getPath() { return null; }
      @Override public void setPath( String path ) {}
    }
    

}