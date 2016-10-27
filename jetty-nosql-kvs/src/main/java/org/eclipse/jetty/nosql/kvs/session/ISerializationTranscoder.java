package org.eclipse.jetty.nosql.kvs.session;

public interface ISerializationTranscoder {
  
	/**
	 * serialize an object to byte array
	 * If an error occurs a {@link TranscoderException} is thrown
	 * 
	 * @param obj to serialize
	 * @return serialized data
	 */
	public byte[] encode(Object obj);

	/**
	 * deserialize object(s) from byte array
     * If an error occurs a {@link TranscoderException} is thrown
     * 
	 * @param raw data
	 * @param klass of serialized data
	 * @return deserialized object(s)
	 */
	public <T> T decode(byte[] raw, Class<T> klass); 
}
