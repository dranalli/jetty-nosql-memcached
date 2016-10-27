package org.eclipse.jetty.nosql.kvs.jmx;

public class SessionStorage implements SessionStorageMBean {

  private SessionStorageMBean client;
  
  
  public SessionStorage( SessionStorageMBean client ) {
    this.client = client;
  }
  
  @Override
  public long getReads() {
    return client.getReads();
  }

  @Override
  public long getWrites() {
    return client.getWrites();
  }

  @Override
  public long getInserts() {
    return client.getInserts();
  }

  @Override
  public long getUpdates() {
    return client.getUpdates();
  }

  @Override
  public long getDeletes() {
    return client.getDeletes();
  }
  
  @Override
  public long getErrors() {
    return client.getErrors();
  }

  @Override
  public long getReadTime() {
    return client.getReadTime();
  }

  @Override
  public long getWriteTime() {
    return client.getWriteTime();
  }

  @Override
  public long getInsertTime() {
    return client.getInsertTime();
  }

  @Override
  public long getErrorTime() {
    return client.getErrorTime();
  }

  @Override
  public long getDataInSizeKB() {
    return client.getDataInSizeKB();
  }

  @Override
  public long getDataOutSizeKB() {
    return client.getDataOutSizeKB();
  }

  @Override
  public long getUpdateTime() {
    return client.getUpdateTime();
  }

  @Override
  public long getDeleteTime() {
    return client.getDeleteTime();
  }

}
