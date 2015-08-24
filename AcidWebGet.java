/******************************************************************************
*                                                                             *
* Dave Mitchell, May 2010 (based on February 2000 routine)                    *
* Dave@zenoshrdlu.com                                                         *
*                                                                             *
* This class gets URL content given a URL                                     *
*                                                                             *
* Three get methods are provided:                                             *
*    getStringContent returns a string (typically the HTML of the web page)   *
*    getPropertyContent returns a Properties object                           *
*    getStream returns a stream (useful for retrieving e.g. zips, images)     *
*                                                                             *
* This version does not allow for the use of a proxy server                   *
*                                                                             *
******************************************************************************/

import java.util.*;
import java.net.*;
import java.io.*;

public class AcidWebGet {
   static final int BUFFSIZE = 1024;
   
   public AcidWebGet() {
   }

   public InputStream getStream(String url) {
      try {
         URLConnection.setDefaultAllowUserInteraction(false);
         URLConnection connection = new URL(url).openConnection();
         return connection.getInputStream();
      } catch (Exception e) {   
         return null;
      }
   }

   public Properties getPropertyContent(String url) {
      Properties props = new Properties();
      try {
         URLConnection.setDefaultAllowUserInteraction(false);
         URLConnection connection = new URL(url).openConnection();
         InputStream ins = connection.getInputStream();
         props.load(ins);
         return props;
      } catch (Exception e) {
         return null;
      }
   }

   public String getStringContent(String url) {
      byte buff[] = new byte[BUFFSIZE];
      StringBuffer sb = new StringBuffer();
      try {
         URLConnection.setDefaultAllowUserInteraction(false);
         URLConnection connection = new URL(url).openConnection();
         InputStream ins = connection.getInputStream();
         DataInputStream dis = new DataInputStream(ins);
         while (true) {
            int l = dis.read(buff, 0, BUFFSIZE);
            if (l == -1)
               break;
            sb = sb.append(new String(buff,0,l));
         }
         return sb.toString();
      } catch (Exception e) {
         return null;
      }
   }
}
