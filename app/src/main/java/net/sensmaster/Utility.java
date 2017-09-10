package net.sensmaster;

import java.util.Calendar;
import java.util.TimeZone;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

class Utility {

  public static boolean hasInternetConnection(Context context)
  {
      ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
      if (wifiNetwork != null && wifiNetwork.isConnected())
      {
          return true;
      }
      NetworkInfo mobileNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
      if (mobileNetwork != null && mobileNetwork.isConnected())
      {
          return true;
      }
      NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    return activeNetwork != null && activeNetwork.isConnected();
  }

  static long[] CRC16Table = { 0x0000, 0x1189, 0x2312, 0x329B, 0x4624, 0x57AD, 0x6536,
    0x74BF, 0x8C48, 0x9DC1, 0xAF5A, 0xBED3, 0xCA6C, 0xDBE5, 0xE97E, 0xF8F7,
    0x1081, 0x0108, 0x3393, 0x221A, 0x56A5, 0x472C, 0x75B7, 0x643E, 0x9CC9,
    0x8D40, 0xBFDB, 0xAE52, 0xDAED, 0xCB64, 0xF9FF, 0xE876, 0x2102, 0x308B,
    0x0210, 0x1399, 0x6726, 0x76AF, 0x4434, 0x55BD, 0xAD4A, 0xBCC3, 0x8E58,
    0x9FD1, 0xEB6E, 0xFAE7, 0xC87C, 0xD9F5, 0x3183, 0x200A, 0x1291, 0x0318,
    0x77A7, 0x662E, 0x54B5, 0x453C, 0xBDCB, 0xAC42, 0x9ED9, 0x8F50, 0xFBEF,
    0xEA66, 0xD8FD, 0xC974, 0x4204, 0x538D, 0x6116, 0x709F, 0x0420, 0x15A9,
    0x2732, 0x36BB, 0xCE4C, 0xDFC5, 0xED5E, 0xFCD7, 0x8868, 0x99E1, 0xAB7A,
    0xBAF3, 0x5285, 0x430C, 0x7197, 0x601E, 0x14A1, 0x0528, 0x37B3, 0x263A,
    0xDECD, 0xCF44, 0xFDDF, 0xEC56, 0x98E9, 0x8960, 0xBBFB, 0xAA72, 0x6306,
    0x728F, 0x4014, 0x519D, 0x2522, 0x34AB, 0x0630, 0x17B9, 0xEF4E, 0xFEC7,
    0xCC5C, 0xDDD5, 0xA96A, 0xB8E3, 0x8A78, 0x9BF1, 0x7387, 0x620E, 0x5095,
    0x411C, 0x35A3, 0x242A, 0x16B1, 0x0738, 0xFFCF, 0xEE46, 0xDCDD, 0xCD54,
    0xB9EB, 0xA862, 0x9AF9, 0x8B70, 0x8408, 0x9581, 0xA71A, 0xB693, 0xC22C,
    0xD3A5, 0xE13E, 0xF0B7, 0x0840, 0x19C9, 0x2B52, 0x3ADB, 0x4E64, 0x5FED,
    0x6D76, 0x7CFF, 0x9489, 0x8500, 0xB79B, 0xA612, 0xD2AD, 0xC324, 0xF1BF,
    0xE036, 0x18C1, 0x0948, 0x3BD3, 0x2A5A, 0x5EE5, 0x4F6C, 0x7DF7, 0x6C7E,
    0xA50A, 0xB483, 0x8618, 0x9791, 0xE32E, 0xF2A7, 0xC03C, 0xD1B5, 0x2942,
    0x38CB, 0x0A50, 0x1BD9, 0x6F66, 0x7EEF, 0x4C74, 0x5DFD, 0xB58B, 0xA402,
    0x9699, 0x8710, 0xF3AF, 0xE226, 0xD0BD, 0xC134, 0x39C3, 0x284A, 0x1AD1,
    0x0B58, 0x7FE7, 0x6E6E, 0x5CF5, 0x4D7C, 0xC60C, 0xD785, 0xE51E, 0xF497,
    0x8028, 0x91A1, 0xA33A, 0xB2B3, 0x4A44, 0x5BCD, 0x6956, 0x78DF, 0x0C60,
    0x1DE9, 0x2F72, 0x3EFB, 0xD68D, 0xC704, 0xF59F, 0xE416, 0x90A9, 0x8120,
    0xB3BB, 0xA232, 0x5AC5, 0x4B4C, 0x79D7, 0x685E, 0x1CE1, 0x0D68, 0x3FF3,
    0x2E7A, 0xE70E, 0xF687, 0xC41C, 0xD595, 0xA12A, 0xB0A3, 0x8238, 0x93B1,
    0x6B46, 0x7ACF, 0x4854, 0x59DD, 0x2D62, 0x3CEB, 0x0E70, 0x1FF9, 0xF78F,
    0xE606, 0xD49D, 0xC514, 0xB1AB, 0xA022, 0x92B9, 0x8330, 0x7BC7, 0x6A4E,
    0x58D5, 0x495C, 0x3DE3, 0x2C6A, 0x1EF1, 0x0F78 };

  static long checksum(byte[] bytes) {
    long crc = 0x0000;
    long temp1, temp3;
    int temp2;

    for (byte tempdata : bytes) {

      temp1 = (crc >> 8);
      temp2 = (int) ((crc ^ tempdata) & 0x00ff);
      temp3 = CRC16Table[temp2];

      crc = (temp1 ^ temp3);
    }
    return crc;
  }

  static Calendar toTime(byte[] bytes) {
    int yearmonth = ((bytes[0] & 0xff) << 8) + (bytes[1] & 0xff);
    int year = yearmonth >> 4;
    int month = yearmonth & 0x0f;
    int monthDay = ((bytes[2] & 0xff) >> 3);
    int hour = ((bytes[3] & 0xff) >> 6) | ((bytes[2] & 0x07) << 2);
    int minute = bytes[3] & 0x3f;
    int second = bytes[4] & 0xff;
    Calendar cal = Calendar.getInstance();
    cal.set(year, month-1, monthDay, hour, minute, second);
    return cal;
  }

  static byte[] getTime() {
    byte[] timeArray = new byte[5];
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    int year = cal.get(Calendar.YEAR);
    timeArray[0] = (byte) (year  >> 4);
    int month = cal.get(Calendar.MONTH)+1;
    timeArray[1] = (byte) ((year << 4) | month);
    int day = cal.get(Calendar.DAY_OF_MONTH); // - 1;
    int hour = cal.get(Calendar.HOUR_OF_DAY);
    timeArray[2] = (byte) ((day << 3) | (hour  >> 2));
    timeArray[3] = (byte) ((hour << 6) | (cal.get(Calendar.MINUTE)));
    timeArray[4] = (byte) cal.get(Calendar.SECOND);
    return timeArray;
  }

  private static final char[] BYTE2HEX=(
    "000102030405060708090A0B0C0D0E0F"+
    "101112131415161718191A1B1C1D1E1F"+
    "202122232425262728292A2B2C2D2E2F"+
    "303132333435363738393A3B3C3D3E3F"+
    "404142434445464748494A4B4C4D4E4F"+
    "505152535455565758595A5B5C5D5E5F"+
    "606162636465666768696A6B6C6D6E6F"+
    "707172737475767778797A7B7C7D7E7F"+
    "808182838485868788898A8B8C8D8E8F"+
    "909192939495969798999A9B9C9D9E9F"+
    "A0A1A2A3A4A5A6A7A8A9AAABACADAEAF"+
    "B0B1B2B3B4B5B6B7B8B9BABBBCBDBEBF"+
    "C0C1C2C3C4C5C6C7C8C9CACBCCCDCECF"+
    "D0D1D2D3D4D5D6D7D8D9DADBDCDDDEDF"+
    "E0E1E2E3E4E5E6E7E8E9EAEBECEDEEEF"+
    "F0F1F2F3F4F5F6F7F8F9FAFBFCFDFEFF").toCharArray();

  public static String getHexString(byte[] bytes) {
    final int len=bytes.length;
    final char[] chars=new char[len<<1];
    int hexIndex;
    int idx=0;
    int ofs=0;
    while (ofs<len) {
      hexIndex=(bytes[ofs++] & 0xFF)<<1;
      chars[idx++]=BYTE2HEX[hexIndex++];
      chars[idx++]=BYTE2HEX[hexIndex];
    }
    return new String(chars);
  }
  public static double calcTemperature(byte b1, byte b2) {
    long temp_val = ((b1 & 0xff) << 8) + (b2 & 0xff);
    double temp = 175.72*(temp_val/65536f) - 46.85;
    return temp;
  }
  public static double calcHumidity(byte b1, byte b2) {
    long hum_val = ((b1 & 0xff) << 8) + (b2 & 0xff);
    double rh = 125 * (hum_val/65536f) - 6;
    return rh;
  }
}
