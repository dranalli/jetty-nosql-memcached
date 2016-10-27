package org.eclipse.jetty.nosql.kvs.jmx;


public interface SessionStorageMBean {
  long getReads();
  long getWrites();
  long getInserts();
  long getUpdates();
  long getDeletes();
  long getErrors();
  long getReadTime();
  long getWriteTime();
  long getInsertTime();
  long getUpdateTime();
  long getDeleteTime();
  long getErrorTime();
  long getDataInSizeKB();
  long getDataOutSizeKB();
}
