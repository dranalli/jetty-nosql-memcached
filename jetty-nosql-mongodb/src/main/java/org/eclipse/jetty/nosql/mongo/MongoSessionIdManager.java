package org.eclipse.jetty.nosql.mongo;


import java.io.IOException;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.nosql.kvs.AbstractKeyValueStoreClient;
import org.eclipse.jetty.nosql.kvs.KeyValueStoreClientException;
import org.eclipse.jetty.nosql.kvs.KeyValueStoreSessionIdManager;
import org.eclipse.jetty.nosql.kvs.jmx.MonitoredClient;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class MongoSessionIdManager extends KeyValueStoreSessionIdManager {
    private static final String DEFAULT_JNDI_NAME = "session/mongo";
	private static final Logger log = Log.getLogger( MongoSessionIdManager.class.getName() );
    private static final String NEW_SESSION_ID = "org.eclipse.jetty.server.newSessionId";
    
	public MongoSessionIdManager(Server server) throws IOException {
		this(server, DEFAULT_JNDI_NAME);
	}

	public MongoSessionIdManager(Server server, String serverString) throws IOException {
		super(server, serverString);
	}

	@Override
	protected void doStart() throws Exception {
		log.info( getClass().getSimpleName() + " starting");
		super.doStart();
		log.info( getClass().getSimpleName() + " started");
	}

	@Override
	protected void doStop() throws Exception {
		log.info( getClass().getSimpleName() + " stopping" );
		super.doStop();
		log.info( getClass().getSimpleName() + " stopped" );
	}

	@Override
	protected AbstractKeyValueStoreClient newClient(String serverString) {		
		AbstractKeyValueStoreClient client = new MonitoredClient( new MongoClient( serverString ) );
		client.setTimeoutInMs(getTimeoutInMs());
		return client;
	}
	
  @Override
  public String newSessionId( HttpServletRequest request, long created ) {
    if ( request == null ) 
      return newSessionId( created );

    // A requested session ID can only be used if it is in use already.
    String requestedId = request.getRequestedSessionId();
    if ( requestedId != null ) {
      String clusterId = getClusterId( requestedId );
      if ( idInUse( clusterId ) ) 
        return clusterId;
    }

    // Else reuse any new session ID already defined for this request.
    String newId = (String)request.getAttribute( NEW_SESSION_ID );
    if ( newId != null && idInUse( newId ) ) 
      return newId;

    // pick a new unique ID!
    String id = newSessionId( request.hashCode() );

    request.setAttribute( NEW_SESSION_ID, id );
    return id;
  }

  @Override
  public String newSessionId( long seedTerm ) {
    return UUID.randomUUID().toString().toLowerCase();
  }

  @Override
  public boolean idInUse( final String idInCluster ) {
    return exists( idInCluster ); // note "null" may also mean be caused by connection problem. Anyway: Treating this as "not in use"
    // do not check the validity of the session since
    // we do not save invalidated sessions anymore.
  }

  protected boolean exists(final String idInCluster)
  {
      log.debug("get: id=" + idInCluster);
      boolean exists = false;
      try
      {
          exists = _client.exists(mangleKey(idInCluster));
      }
      catch (KeyValueStoreClientException error)
      {
          log.warn("unable to check key exists: id=" + idInCluster, error);
      }
      return exists;
  }
  
  protected long version( final String idInCluster ) {
    log.debug("version: id=" + idInCluster);
    return _client.version(mangleKey(idInCluster));
  }

  protected boolean setKey( String mangleKey, long version, byte[] raw, int maxInactiveInterval ) {
    log.debug("setKey: id=" + mangleKey );
    boolean exists = false;
    try
    {
        exists = _client.set( mangleKey, version, raw, maxInactiveInterval );
    }
    catch (KeyValueStoreClientException error)
    {
        log.warn("unable save session: id=" + mangleKey, error);
    }
    return exists;
  }

  /**
   * Added here to give access to SessionManager
   */
  @Override
  protected String mangleKey( String key ) {
    return super.mangleKey( key );
  }
  
  
}
