package org.eclipse.jetty.nosql.kvs.session.kryo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jetty.nosql.kvs.session.ISerializationTranscoder;
import org.eclipse.jetty.nosql.kvs.session.TranscoderException;
import org.objenesis.strategy.SerializingInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.factories.SerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class KryoTranscoder implements ISerializationTranscoder {
  KryoConfig config;
  
  private ThreadLocal<Kryo> kryos = new ThreadLocal<Kryo>() {
    @Override
    protected Kryo initialValue() {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        kryo.setClassLoader( Thread.currentThread().getContextClassLoader() );
        applyConfig( kryo );
        return kryo;
    }

  };

  public KryoTranscoder() {
    this(null, Thread.currentThread().getContextClassLoader());
  }

  public KryoTranscoder(ClassLoader cl) {
    this( null, cl );
  }

  public KryoTranscoder( KryoConfig config ) {
    this(config, Thread.currentThread().getContextClassLoader());
  }
  
  public KryoTranscoder( KryoConfig config, ClassLoader cl ) {
    this.config = config;
    kryos.get().setClassLoader( cl );
  }
  
  @Override
  public byte[] encode(Object obj) throws TranscoderException {
    byte[] raw = null;
    try {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      Output output = new Output(stream);
      kryos.get().writeObject(output, obj);
      output.close();
      raw = stream.toByteArray();
    } catch (Exception error) {
      throw(new TranscoderException(error));
    }
    return raw;
  }

  @Override
  public <T> T decode(byte[] raw, Class<T> klass) throws TranscoderException {
    T obj = null;
    try {
      ByteArrayInputStream stream = new ByteArrayInputStream(raw);
      Input input = new Input(stream);
      obj = kryos.get().readObject(input, klass);
    } catch (Exception error) {
      throw(new TranscoderException(error));
    }
    return obj;
  }
  
  private void applyConfig( Kryo kryo ) {
    
    if ( config != null && config.instantiatorStrategy != null )
      kryo.setInstantiatorStrategy( config.instantiatorStrategy );
    else { 
      // Use a more robust default instantiation strategy... 
      // so that serializable objects without no-arg constructors still work.
      kryo.setInstantiatorStrategy( new Kryo.DefaultInstantiatorStrategy(new SerializingInstantiatorStrategy()) );
    }

    if ( config != null && config.defaultSerializerFactory != null ) 
      kryo.setDefaultSerializer( config.defaultSerializerFactory );
    
    if ( config != null && !config.serializers.isEmpty() ) 
      for( Map.Entry<Class<?>, SerializerFactory> entry : config.serializers.entrySet() ) 
          kryo.addDefaultSerializer( entry.getKey(), entry.getValue() );
    
  }
  
}
