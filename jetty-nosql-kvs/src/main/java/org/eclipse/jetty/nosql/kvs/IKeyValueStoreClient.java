package org.eclipse.jetty.nosql.kvs;

public interface IKeyValueStoreClient {
	public boolean establish() throws KeyValueStoreClientException;

	public boolean shutdown() throws KeyValueStoreClientException;

	public boolean isAlive();

	public byte[] get(String key) throws KeyValueStoreClientException;


	public boolean delete(String key) throws KeyValueStoreClientException;
	
	  /**
	   * Allow checking existence without pulling back all the session data.
	   * @param key
	   * @return
	   * @throws KeyValueStoreClientException
	   */
	  public boolean exists(String key) throws KeyValueStoreClientException;

	  /**
	   * Store session data with version as session meta data.
	   * 
	   * @param key
	   * @param version
	   * @param raw
	   * @return
	   * @throws KeyValueStoreClientException
	   */
	  public boolean set(String key, long version, byte[] raw ) throws KeyValueStoreClientException;

	  public boolean set(String key, long version, byte[] raw, int exp) throws KeyValueStoreClientException;

	  public boolean add(String key, long version, byte[] raw) throws KeyValueStoreClientException;

	  public boolean add(String key, long version, byte[] raw, int exp) throws KeyValueStoreClientException;

	  /**
	   * Allow querying version without pulling back all the session data.
	   * @param key
	   * @return
	   */
	  public long version( String key );

}
