package org.eclipse.jetty.nosql.jmx;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.eclipse.jetty.nosql.mongo.MongoSessionManager;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Registers mbeans with the default platform MBean Server.
 * 
 * @author David Ranalli
 * Jan 29, 2016
 */
public class JmxMonitorManager {
  private static final Logger log = Log.getLogger( MongoSessionManager.class.getName() );
  private static final String PREFIX = "jetty-nosql.";

  public static void monitor( Object mbean, String category, String type, String name ) {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    try {
      server.registerMBean( mbean, new ObjectName( PREFIX + category + ":type=" + type + ",name=" + name ) );
    }
    catch ( Exception e ) {
      // Throwing runtime exception will not help matters...
      // silently fail and record the problem
      log.info( "Could not add mbean " + mbean + " as " + PREFIX + category + ":type=" + type + ",name=" + name, e );
    }
  }
  
  public static void remove( String category, String type, String name ) {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    try {
      server.unregisterMBean( new ObjectName( PREFIX + category + ":type=" + type + ",name=" + name ) );
    }
    catch ( Exception e ) {
      // Throwing runtime exception will not help matters...
      // silently fail and record the problem
      log.info( "Could not remove mbean " + PREFIX + category + ":type=" + type + ",name=" + name, e );
    }
  }
  
}
