package org.eclipse.jetty.nosql.kvs.session.kryo;

import java.util.HashMap;
import java.util.Map;

import org.objenesis.strategy.InstantiatorStrategy;

import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.factories.PseudoSerializerFactory;
import com.esotericsoftware.kryo.factories.ReflectionSerializerFactory;
import com.esotericsoftware.kryo.factories.SerializerFactory;

/**
 * Wraps Kryo Configuration, allow custom serializers and InstantiationStrategy.
 *  
 * @author David Ranalli
 * Mar 11, 2016
 */
public class KryoConfig {
  Map<Class<?>, SerializerFactory> serializers = new HashMap<>();
  InstantiatorStrategy instantiatorStrategy;
  SerializerFactory defaultSerializerFactory;
  
  @SuppressWarnings("rawtypes")
  public void addDefaultSerializer(Class type, Serializer serializer) {
    serializers.put( type, new PseudoSerializerFactory(serializer) );
  }
  
  @SuppressWarnings("rawtypes")
  public void addDefaultSerializer(Class type, SerializerFactory serializerFactory) {
    serializers.put( type, serializerFactory );
  }
  
  @SuppressWarnings("rawtypes")
  public void addDefaultSerializer(Class type, Class<? extends Serializer> serializerClass) {
    serializers.put( type, new ReflectionSerializerFactory(serializerClass) );
  }
  
  @SuppressWarnings("rawtypes")
  public void setDefaultSerializer( Class<? extends Serializer> serializerClass ) {
    defaultSerializerFactory = new ReflectionSerializerFactory( serializerClass );
  }
  
  public void setDefaultSerializer( SerializerFactory serializerFactory ) {
    defaultSerializerFactory = serializerFactory;     
  }

  public void setInstantiatorStrategy( InstantiatorStrategy strategy ) {
    this.instantiatorStrategy = strategy;
  }
  
  
}
