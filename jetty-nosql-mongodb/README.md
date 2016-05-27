# jetty-nosql-memcached - mongodb

## Overview

SessionManager implementation for Jetty based on jetty-nosql.  When used with the Kryo serializer, is a fast and efficient implementation, that
allows session clustering that can handle changes even when session.setAttribute is not called, and can handle non-serialized objects, making it easy to 'drop-in' clustering without making changes to application code.
Additionally, it minimizes reads and writes of sessions based on if the session attributes have changed.

## Install

jetty-nosql-memcached is an extension for Jetty.
You have to install jars into jetty's `${jetty.home}/lib/ext`.

*NOTE*

You must install jetty-nosql-memcached into Jetty with all dependent jars, such like jetty-nosql and mongo-java-driver.
If you're not sure, it's better to use all-in-one jar like `jetty-nosql-mongodb-${version}-jar-with-dependencies.jar`.
You don't have to be aware of missing dependencies since all-in-one jar includes all dependencies in single jar file.


## Configuration

You need to configure both "session manager" and "session ID manager".


### Configuring "session ID manager"

SessionIdManagers can be configured in files under `${JETTY_HOME}/etc`.  In following example, using `${JETTY_HOME}/etc/jetty.xml`.

```xml
<?xml version="1.0"?>
<Configure id="Server" class="org.eclipse.jetty.server.Server">
(... snip ...)
    <Set name="sessionIdManager">
        <New id="theSessionIdManager" class="org.eclipse.jetty.nosql.mongo.MongoSessionIdManager">
          <Arg><Ref id="Server" /></Arg>
          <Set name="serverString">session/mongo</Set>
          <Set name="keyPrefix">session:</Set>
        </New>
    </Set>
    <New id="mongodb" class="com.mongodb.Mongo">
        <Arg>
                <New class="java.util.ArrayList">
                      <Call name="add">
                        <Arg>
                          <New class="com.mongodb.ServerAddress">
                            <Arg type="java.lang.String">mongoHost</Arg>
                            <Arg type="int">27017</Arg>
                          </New>
                        </Arg>
                      </Call>
                </New>
        </Arg>
        <Call name="getDB">
                <Arg>HttpSessions</Arg>
                <Call id="sessionDocument" name="getCollection">
                        <Arg>sessions</Arg>
                </Call>
        </Call>
        <!-- If you want to configure Jetty to be able to read through the slaves, call the following: -->
        <!-- Call name="slaveOk"/ -->
   </New>
    <!--
                 Provides a Mongo Client for session management on server and each webapp
    -->
    <New class="org.eclipse.jetty.plus.jndi.Resource">
        <Arg>session/mongo</Arg>
        <Arg><Ref id="sessionDocument" /></Arg>
    </New>
<!-- // equivalent in Java:
  Server server = ...;
  MongoSessionIdManager mongoSessionIdManager = new mongoSessionIdManager(server);
  memcachedSessionIdManager.setServerString("session/mongo");
  memcachedSessionIdManager.setKeyPrefix("session:");
  server.setSessionIdManager(memcachedSessionIdManager);
  List servers = new ArrayList();
  servers.add( new ServerAddress( "mongoHost", 27017 );
  Mongo mongodb = new Mongo( servers );
  DBCollection sessionDocument = mongodb.getDB("HttpSessions").getCollection("sessions");
  new Resource( "sesssion/mongo", sessionDocument );  
  -->
</Configure>
```

#### Extra options for "session ID manager"

You can configure the behavior of session ID manager with following setters.

* setKeyPrefix(String keyPrefix)
  * use keyPrefix for session key prefix on mongodb.
* setKeySuffix(String keySuffix)
  * use keySuffix for session key suffix on mongodb.
* setServerString(String serverString)
  * specify initial context name of mongo DBCollection.


### Configuring "session manager"

SessionManagers can be configured by either `${APP_ROOT}/WEB-INF/jetty-web.xml` or `${JETTY_HOME}/webapps/${APP_NAME}.xml`.

Sample configuration for `${APP_ROOT}/WEB-INF/jetty-web.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configure class="org.eclipse.jetty.webapp.WebAppContext">
(... snip ...)
  <Get name="server">
    <Get id="theSessionIdManager" name="sessionIdManager" />
  </Get>
  <Set name="sessionHandler">
    <New class="org.eclipse.jetty.server.session.SessionHandler">
      <Arg>
        <New class="org.eclipse.jetty.nosql.mongo.MongoSessionManager">
          <Set name="sessionIdManager">
            <Ref id="theSessionIdManager" />
          </Set>
		  <Set name="sessionFactory">
		        <New class="org.eclipse.jetty.nosql.kvs.session.kryo.KryoSessionFactory"/>
		  </Set>
        </New>
      </Arg>
    </New>
  </Set>
</Configure>
```

#### Extra options for "session manager"

You can configure the behavior of session manager with following setters.

* setSessionIdManager(SessionIdManager idManager)
  * session id manager you created.
* setSessionFactory(AbstractSessionFactory sf)
  * set session serializer. org.eclipse.jetty.nosql.kvs.session.serializable.SerializableSessionFactory is used by default.


## Development

### Requirements

All library dependencies can be resolved from Maven.

* [maven](http://maven.apache.org/)
* [jetty](http://eclipse.org/jetty/)
* [spymemcached](http://code.google.com/p/spymemcached/)
* [xmemcached](http://code.google.com/p/xmemcached/)
* [kryo](http://code.google.com/p/kryo/)
* [xstream](http://xstream.codehaus.org/)

### Build

You can build project tree from top of the repository.

```sh
$ git clone git://github.com/yyuu/jetty-nosql-memcached.git
$ cd jetty-nosql-memcached
$ mvn clean package
```

### Release

Use nexus-staging-maven-plugin.

```sh
$ mvn versions:set -DnewVersion="${VERSION}"
$ git commit -a -m "v${VERSION}"
$ mvn clean deploy -DperformRelease -Dgpg.keyname="${GPG_KEYNAME}" -Dgpg.passphrase="${GPG_PASSPHRASE}"
$ mvn nexus-staging:release # may not work
$ git tag "jetty-nosql-memcached-parent-${VERSION}"
```


## License

* Copyright (c) 2011-2015 Yamashita, Yuu

All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Public License v1.0
and Apache License v2.0 which accompanies this distribution.

The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html

The Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php

You may elect to redistribute this code under either of these licenses.
