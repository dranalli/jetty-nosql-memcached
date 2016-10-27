package org.eclipse.jetty.nosql.kvs.jmx;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Registers mbeans with the default platform MBean Server.
 * 
 * @author David Ranalli
 * Jan 29, 2016
 */
public class JmxMonitorManager {
  private static final Logger log = Log.getLogger( JmxMonitorManager.class.getName() );
  private static final String BEAN_NAME = "jetty-nosql.%s:type=%s,name=%s"; 

  public static void monitor( Object mbean, String category, String type, String name ) {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    try {
      server.registerMBean( mbean, new ObjectName( String.format( BEAN_NAME, category, type, name ) ) );
    }
    catch ( Exception e ) {
      // Throwing runtime exception will not help matters...
      // silently fail and record the problem
      log.info( "Could not add mbean " + mbean + " as " + String.format( BEAN_NAME, category, type, name ), e );
    }
  }
  
  public static void remove( String category, String type, String name ) {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    try {
      server.unregisterMBean( new ObjectName( String.format( BEAN_NAME, category, type, name ) ) );
    }
    catch ( Exception e ) {
      // Throwing runtime exception will not help matters...
      // silently fail and record the problem
      log.info( "Could not remove mbean " + String.format( BEAN_NAME, category, type, name ), e );
    }
  }
  
}
