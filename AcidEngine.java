/*******************************************************************************
*                                                                              *
* Dave Mitchell, May 2010                                                      *
* Dave@zenoshrdlu.com                                                          *
*                                                                              *
* Part of ACID (Automatic Camera Indentifier and Downloader)                   *
*                                                                              *
* This class handles requests for EXIF information. It uses the library by     *
* Drew Noakes (http://www.drewnoakes.com/drewnoakes.com/code/exif/) to do      *
* the work.                                                                    *
*                                                                              *
*******************************************************************************/

// for debug, msgout actually needs to do something
                  

   import java.io.*;
   import java.awt.*;
   import java.awt.event.*;
   import java.util.*;
   import java.util.Date;
   import java.util.TimeZone;
   import java.text.MessageFormat;
   import java.text.DateFormat;
   import java.text.SimpleDateFormat;

   import com.drew.imaging.*;
   import com.drew.imaging.jpeg.*;
   import com.drew.metadata.*;
   import com.drew.metadata.exif.*;
   import com.drew.lang.*;

public class AcidEngine {
 
   public static final int NOFINFO  = -1;
   public static final int NOTCANON =  0;
   
   private static final String JPG                 = "JPG";                  
   private static final String TIMESTAMPFORMAT     = "HH:mm:ss";
   private static final String MINTIMESTAMPFORMAT  = "mm:ss";
   private static final String SECTIMESTAMPFORMAT  = "ss";
   private static final String DAYSTAMPFORMAT      = "MMM dd yyyy";
   private static final String CANON               = "Canon";
   
   private static final String EXTRACTERROR        = "EXIF data extraction error ";
   
   private static final int FVOFFSET  = 30;
   private static final int FVSOFFSET = 7;
   private static final int MDOFFSET  = 16;

   static final String CRLF     = "\r\n";
 
   static SimpleDateFormat timeFormatter = null;
   static SimpleDateFormat dayFormatter  = null;
   static SimpleDateFormat minFormatter  = null;
   static SimpleDateFormat secFormatter  = null;
   File file = null;


/*----------------------------------------------------------------------------*/
/*  Constructor                                                               */
/*----------------------------------------------------------------------------*/
   public AcidEngine() {
   }

/*----------------------------------------------------------------------------*/
/*  Some of the get calls rely on this function having beeen called to provide*/
/*  a jpeg file from which to extract EXIF data                               */
/*----------------------------------------------------------------------------*/
   public void setFile(String fname) {
      file = new File(fname);
   }

/*----------------------------------------------------------------------------*/
/*  return the make of camera                                                 */
/*----------------------------------------------------------------------------*/
   public String getCameraMake() {
      String cm = "?";
      try {
         Directory exifDirectory = getDirectory(file);
         if (exifDirectory != null) {
            cm = exifDirectory.getString(ExifDirectory.TAG_MAKE);
         }
      } catch(Exception exiferr) {
         msgOut(EXTRACTERROR + " CameraMake " + exiferr);
      }
      return cm;
   }

/*----------------------------------------------------------------------------*/
/*  return the camera model (as a string)                                     */
/*----------------------------------------------------------------------------*/
   public String getCameraModel() {
      String cm = "?";
      try {
         Directory exifDirectory = getDirectory(file);
         if (exifDirectory != null) {
            cm = exifDirectory.getString(ExifDirectory.TAG_MODEL);
         }
      } catch(Exception exiferr) {
         msgOut(EXTRACTERROR + " CameraModel " + exiferr);
      }
      return cm;
   }
   
/*----------------------------------------------------------------------------*/
/*  return the camera model (as a unique integer)                             */
/*----------------------------------------------------------------------------*/
   public int getIntCameraModel() {
      int icm = NOFINFO;
      try {
         Directory exifDirectory = getDirectory(file);
         if (exifDirectory != null) {
            String cameraMake = exifDirectory.getString(ExifDirectory.TAG_MAKE);
            if (cameraMake.equals(CANON)) {
                  CanonMakernoteDirectory cd = (CanonMakernoteDirectory)getMakerDirectory(file,CanonMakernoteDirectory.class);
                  icm = cd.getInt(MDOFFSET); 
            } else {
                  icm = NOTCANON;
            }
         }
      } catch(Exception exiferr) {
         msgOut(EXTRACTERROR + " IntCameraMake " + exiferr);
      }
      return icm;
   }

/*----------------------------------------------------------------------------*/
/*  return the firmware version (as a String)                                 */
/*----------------------------------------------------------------------------*/
   public String getStringFirmwareVersion() {
      String fm = "?";
      try {
         Directory exifDirectory = getDirectory(file);
         if (exifDirectory != null) {
            String cameraMake = exifDirectory.getString(ExifDirectory.TAG_MAKE);
            if (cameraMake.equals(CANON)) {
                  CanonMakernoteDirectory cd = (CanonMakernoteDirectory)getMakerDirectory(file,CanonMakernoteDirectory.class);
                  fm = cd.getString(FVSOFFSET); 
            } else {
                  fm = null;
            }
         }
      } catch(Exception exiferr) {
         msgOut(EXTRACTERROR + " StringFV " + exiferr);
      }
      return fm;
   }
   


/*----------------------------------------------------------------------------*/
/*  return the firmware version (as an integer code)                          */
/*----------------------------------------------------------------------------*/
   public int getFirmwareVersion() {
      int fm = NOFINFO;
      try {
         Directory exifDirectory = getDirectory(file);
         if (exifDirectory != null) {
            String cameraMake = exifDirectory.getString(ExifDirectory.TAG_MAKE);
            if (cameraMake.equals(CANON)) {
                  CanonMakernoteDirectory cd = (CanonMakernoteDirectory)getMakerDirectory(file,CanonMakernoteDirectory.class);
                  fm = cd.getInt(FVOFFSET); 
            } else {
                  fm = NOTCANON;
            }
         }
      } catch(Exception exiferr) {
         msgOut(EXTRACTERROR + " IntFV " + exiferr);
      }
      return fm;
   }
   
        
/*----------------------------------------------------------------------------*/
/*  get date and time info                                                    */
/*----------------------------------------------------------------------------*/
   public String getDateInfo() {

      dayFormatter = new SimpleDateFormat(DAYSTAMPFORMAT);
      dayFormatter.setTimeZone(TimeZone.getDefault());
          
      timeFormatter = new SimpleDateFormat(TIMESTAMPFORMAT);
      timeFormatter.setTimeZone(TimeZone.getDefault());
                    
      minFormatter = new SimpleDateFormat(MINTIMESTAMPFORMAT);
      minFormatter.setTimeZone(TimeZone.getDefault());
                    
      secFormatter = new SimpleDateFormat(SECTIMESTAMPFORMAT);
      secFormatter.setTimeZone(TimeZone.getDefault());

      Date d = getEXIFDate(file);

      String tt = dayFormatter.format(d);
      String ds = timeFormatter.format(d);
      return tt + " at " + ds;
    }     

/*----------------------------------------------------------------------------*/
/*  get the photo's EXIF timestamp as a Date. If there isn't one then use     */
/*  the date the file was last modified                                       */
/*----------------------------------------------------------------------------*/
   Date getEXIFDate(File jpeg) {
       Date dt = null;
       try {
          Directory exifDirectory = getDirectory(jpeg);
          if (exifDirectory != null) {
              dt = exifDirectory.getDate(ExifDirectory.TAG_DATETIME_ORIGINAL);
              if (dt == null) {
                 msgOut("EXIF no date/time for " + jpeg.getName() + " - using file last modified value");
                 dt = new Date(jpeg.lastModified());
              }
         
              return dt;
          }
       } catch(Exception exiferr) {
          msgOut(EXTRACTERROR + exiferr);
          dt = new Date(jpeg.lastModified());
       }
       return dt;
   }
  
/*----------------------------------------------------------------------------*/
/*  Get EXIF directory                                                        */
/*----------------------------------------------------------------------------*/
   Directory getDirectory(File jpeg) {
       try {
          Metadata metadata = JpegMetadataReader.readMetadata(jpeg);
          return metadata.getDirectory(ExifDirectory.class);
       } catch(Exception exiferr) {
          msgOut(EXTRACTERROR + exiferr);
          return null;                                                
       }
   }

/*----------------------------------------------------------------------------*/
/*  Get EXIF makerdirectory                                                   */
/*----------------------------------------------------------------------------*/
   Directory getMakerDirectory(File jpeg, Class makerclass) {
       try {
          Metadata metadata = JpegMetadataReader.readMetadata(jpeg);
          return metadata.getDirectory(makerclass);
       } catch(Exception exiferr) {
          msgOut(EXTRACTERROR + exiferr);
          return null;                                                
       }
   }

/*----------------------------------------------------------------------------*/
/*  Output a message to system out and to log                                 */
/*----------------------------------------------------------------------------*/
   void msgOut(String l) {
      System.out.println(l);
   }
}
