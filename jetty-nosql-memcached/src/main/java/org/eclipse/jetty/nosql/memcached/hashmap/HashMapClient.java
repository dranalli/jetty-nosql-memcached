package org.eclipse.jetty.nosql.memcached.hashmap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.nosql.kvs.KeyValueStoreClientException;
import org.eclipse.jetty.nosql.memcached.AbstractMemcachedClient;

// intend to use this as test mock
public class HashMapClient extends AbstractMemcachedClient {
	static class Entry {
		private byte[] data = null;
		private long expiry = 0;
		Entry(byte[] raw, long exp) {
			data = raw;
			expiry = exp;
		}
		byte[] getData() {
			return data;
		}
		long getExpiry() {
			return expiry;
		}
	}

	private static final int FOREVER = 0;
	private static Map<String, Entry> data = new ConcurrentHashMap<String, Entry>();

	public HashMapClient() {
		this("127.0.0.1:11211");
	}

	public HashMapClient(String serverString) {
		super(serverString);
	}

	@Override
	public boolean establish() throws KeyValueStoreClientException {
		if (isAlive()) {
			shutdown();
		}
		return true;
	}

	@Override
	public boolean shutdown() throws KeyValueStoreClientException {
		return true;
	}

	@Override
	public boolean isAlive() {
		return data != null;
	}

	@Override
	public byte[] get(String key) throws KeyValueStoreClientException {
		if (!isAlive()) {
			throw(new KeyValueStoreClientException(new IllegalStateException("client not established")));
		}
		byte[] raw = null;
		synchronized(data) {
			Entry entry = data.get(key);
			if ( entry != null) {
				long exp = entry.getExpiry();
				if (System.currentTimeMillis() < exp || exp == FOREVER) {
					raw = entry.getData();
				} else {
					data.remove(key);
				}
			}
		}
		return raw;
	}

	@Override
	public boolean set(String key, long version, byte[] raw) throws KeyValueStoreClientException {
		return this.set(key, 0, raw, FOREVER);
	}

	@Override
	public boolean set(String key, long version, byte[] raw, int exp) throws KeyValueStoreClientException {
		if (!isAlive()) {
			throw(new KeyValueStoreClientException(new IllegalStateException("client not established")));
		}
		data.put(key, new Entry(raw, expiryTimeMillis(exp)));
		return true;
	}

	@Override
	public boolean add(String key, long version, byte[] raw) throws KeyValueStoreClientException {
		return this.add(key, version, raw, FOREVER);
	}

	@Override
	public boolean add(String key, long version, byte[] raw, int exp) throws KeyValueStoreClientException {
		if (!isAlive()) {
			throw(new KeyValueStoreClientException(new IllegalStateException("client not established")));
		}
		boolean notExists = false;
		synchronized(data) {
			notExists = !data.containsKey(key);
			if (notExists) {
				data.put(key, new Entry(raw, expiryTimeMillis(exp)));
			}
		}
		return notExists;
	}

	@Override
  public boolean delete(String key) throws KeyValueStoreClientException {
		if (!isAlive()) {
			throw(new KeyValueStoreClientException(new IllegalStateException("client not established")));
		}
		data.remove(key);
		return true;
	}

	private long expiryTimeMillis(int exp) {
		// the actual value sent may either be Unix time (number of seconds since
		// January 1, 1970, as a 32-bit value), or a number of seconds starting 
		// from current time. In the latter case, this number of seconds may not
		// exceed 60*60*24*30 (number of seconds in 30 days); if the number sent
		// by a client is larger than that, the server will consider it to be real
		// Unix time value rather than an offset from current time.
		// http://code.sixapart.com/svn/memcached/trunk/server/doc/protocol.txt
		long timestamp;
		if (exp < 60*60*24*30) { // relative time
			timestamp = System.currentTimeMillis()+TimeUnit.SECONDS.toMillis(exp);
		} else { // absolute time
			timestamp = TimeUnit.SECONDS.toMillis(exp);
		}
		return timestamp;
	}
  
    @Override
    public boolean exists( String key ) throws KeyValueStoreClientException {
      synchronized(data) {
          return data.containsKey(key);
      }
    }
  
    @Override
    public long version( String key ) {
      return 0;
    }
}
