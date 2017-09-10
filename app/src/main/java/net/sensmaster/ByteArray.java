package net.sensmaster;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

class ByteArray {
  private ByteArrayOutputStream stream;
  private int ignore_bytes = 0;
  public ByteArray() {
    stream = new ByteArrayOutputStream( );
  }
  
  public ByteArray(byte[] data) {
    stream = new ByteArrayOutputStream( );
    try {
      stream.write(data);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public void append(byte[] data, int offset, int len) {
    stream.write(data, offset+ignore_bytes, len-ignore_bytes);
    ignore_bytes = 0;
  }
  
  public byte[] toBytes() {
    return stream.toByteArray();
  }

  @Override
  public String toString() {
    return Utility.getHexString(toBytes());
  }
  
  public boolean isEqual(ByteArray tag) {
    byte[] tag_bytes = tag.toBytes();
    byte[] bytes = toBytes();
    if(bytes.length != tag_bytes.length)
      return false;
    for (int i = 0; i < bytes.length; i++) {
      if(bytes[i] != tag_bytes[i])
        return false;
    }
    return true;
  }

  public boolean startsWith(ByteArray tag) {
    byte[] tag_bytes = tag.toBytes();
    byte[] bytes = toBytes();
    if(bytes.length < tag_bytes.length)
      return false;
    for (int i = 0; i < tag_bytes.length; i++) {
      if(bytes[i] != tag_bytes[i])
        return false;
    }
    return true;
  }

  public int length() {
    return stream.size();
  }

  public void removeFromEnd(int mod) {
    ignore_bytes  = mod;
  }

  public static ByteArray parse(String uid_str) {
    int len = uid_str.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i/2] = (byte) Integer.parseInt(uid_str.substring(i, i + 2), 16);
    }
    return new ByteArray(data);
  }

  public boolean isEmpty() {
    return stream.size() == 0;
  }
}
