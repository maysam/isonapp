package net.sensmaster;

public class StoredData {
  private ByteArray data;
  private long datetime;
  private int interval;

  public StoredData(ByteArray storing_data, long l, int log_interval) {
    data = storing_data;
    datetime = l;
    interval = log_interval;
  }

  public boolean isEqual(ByteArray storing_data, long l, int log_interval) {
    if (datetime != l)
      return false;
    if (interval != log_interval)
      return false;
    if (storing_data.length() == data.length())
      return storing_data.isEqual(data);
    if (storing_data.length() > data.length()) {
      if (storing_data.startsWith(data)) {
        data = storing_data;
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }
}
