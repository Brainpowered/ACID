/******************************************************************************
*                                                                             *
* UI for ACID - OSX Version                                                   *
*                                                                             *
* Dave Mitchell, October 2012                                                 *
* Dave@zenoshrdlu.com                                                         *
*                                                                             *
* ACID is a tool for identifying a camera model from an image taken with it   *
* and then downloading the appropriate SDM, CHDK or CHDK-DE build for it.     *
*                                                                             *
* Drew Noakes EXIF extraction library is used to extract the necessary EXIF   *
* information (see www.drewnoakes.com/drewnoakes.com/code/exif/). Two tables  *
* held at my website (www.zenoshrdlu.com/acid) are then used to construct the *
* appropriate zip file names and then the SDM and CHDK download sites are     *
* searched to locate these. An additional table gives information about       *
* cameras on which CHDK porting is going on.                                  *
*                                                                             *
* To make the program as flexible as possible, values like website addresses  *
* are held as properties which can therefore be adjusted if necessary without *
* the need to modify and recompile the source code.                           *
*                                                                             *
* This OSX version differs slightly from the Windows version - the changes    *
* are marked with the string "// apple only"                                  *
*                                                                             *
* Note: feel free to use this source code as you wish provided that you do    *
* not market or sell a derivative work without my permission.                 *
*                                                                             *
******************************************************************************/

// make any changes to Windows version too


// http://autobuild.akalab.de/CHDK_DE_full.txt
// http://autobuild.akalab.de/CHDK-DE_full.txt

import java.awt.*;
import java.awt.List;

import java.awt.event.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;

import com.apple.eawt.*; // apple only
 
public class Acid extends Frame implements WindowListener,
                                           ItemListener,
                                           DropTargetListener,
                                           ApplicationListener,     // apple only                                        
                                           ActionListener {

   static final String DIRDLG   = "apple.awt.fileDialogForDirectories"; // apple only
   static final String URILIST  = "text/uri-list;class=java.lang.String";

   static final String TITLE    = "Automatic Camera Identifier and Downloader - Dave Mitchell";
   static final String VERSION  = "1.14";
   static final String CRLF     = "\r\n";
   static final String LF       = "\n";
   static final String ALPHA    = " abcdefghijklmnop";
   static final String ZIPFILE  = "tempcvi.zip";
   static final String JPG      = "JPG";
   static final String PORT     = "P";
   
   static final String CHOOSER  = "Choose Download Folder"; // apple version
   static final String DROP1    = "To start, drag and drop a";
   static final String DROP2    = "photo from your camera here";

   static final String IMGLBL   = "Image File";
   static final String DTLBL    = "Date and Time";
   static final String CAMLBL   = "Camera Make";
   static final String MDLLBL   = "Model";
   static final String FRMLBL   = "Firmware Version (Canon only)";
   static final String DIRLBL   = "Download Folder";

   static final String EXITBUT  = "Exit";
   static final String SDMALL   = "download SDM common files?";
   static final String CHDKALL  = "download stable CHDK full build?";
   static final String CHDKALLT = "download trunk CHDK full build?";
   static final String CHDEALL  = "download Stabiles CHDK-DE full build?";
   static final String CHDEALLT = "download Experimentelles CHDK-DE full build?";
   
   static final String LOADING  = "loading tables, please wait...";
   static final String LOADED   = "tables loaded successfully";
   static final String NOLOAD   = " table failed to load";
   static final String NOFINFO  = "No firmware revision information in photo";
   static final String FINFO    = "No firmware revision present, but EXIF says ";
   static final String NOTCANON = "Photo not taken by Canon camera";
   static final String ZIPFAIL  = " failed to download";
   static final String ZIPSAVE  = " has been saved";
   static final String IPROCESS = " is being processed";  
   static final String EPROCESS = "Processing completed"; 
   static final String CHOOSE   = "Click on a file";
   static final String SOME     = "Some builds for this camera are available for download";
   static final String ALL      = "CHDK, CHDK-DE and SDM builds for this camera are available for download";
   static final String NONE     = "No official builds are available for this camera";
   static final String NOTJPG   = " is not a jpg";
   static final String NOTDIR   = "Only images can be dropped here";
   static final String NOTFILE  = "Only folders/directories can be dropped here";
   static final String DROPPED  = " dropped";
   static final String DROPPING = "Handling drop, please wait...";
   static final String EXTRACT  = "Extracting ";
   static final String NOTFOUND = " not found";
   
   static final String SRCHING  = " - searching for build for ";
   static final String BLDFND   = " - build found - ";
   static final String TBLDOWN  = "Downloading build information from ";
   static final String DWNLDING = "Downloading ";
   
   static final String NOBLDFND = " - no build found for ";
   static final String PORTING  = "Work is underway porting CHDK for this camera - test builds may be available";
   static final String DROPFAIL = "Dropping failed - try again";
   
   static final String ZIPDOWN  = " zip file being downloaded, please wait...";
   static final String DOWNLOAD = "Files successfully extracted";
   static final String DSDMBUT  = "Download SDM";
   static final String DCHDKBUT = "Download stable CHDK";
   static final String DCHDKBUTT= "Download trunk CHDK";
   static final String DCHDEBUT = "Download Stabiles CHDK-DE";
   static final String DCHDEBUTT= "Download Experimentelles CHDK-DE";

   static final String BROWBUT  = "Browse Folders";
   static final String BROWBUT2 = "Browse Jpegs";
   static final String CHOOSER2 = "Choose JPG file";
   
   static final String SDM      = "SDM";
   static final String SDMX     = "SDM Additions";
   static final String CHDK     = "Stable CHDK";
   static final String CHDKT    = "Trunk CHDK";
   static final String CHDE     = "Stabiles CHDK-DE";
   static final String CHDET    = "Experimentelles CHDK_DE";
   
   static final String SDMTEST   = "a570-100e";
   static final String SDMXTEST = "s100-";
   static final String CHDKTEST  = "a570-100e";
   static final String CHDETEST  = "a570-100e";
   
   static final int    DOSDM    = 1;
   static final int    DOCHDK   = 2;
   static final int    DOCHDKT  = 3;
//   static final int    DOADD    = 5;
   static final int    DOCHDE   = 4;
   static final int    DOCHDET  = 5;

   AcidEngine acidengine;                         
   FileDialog chooser   = null;
   FileDialog chooser2  = null;
   AcidWebGet webgetter;

   /* UI elements */
   TextField  imagefile;
   TextField  dtinfo;
   TextField  cminfo;
   TextField  mdinfo;
   TextField  fminfo;
   TextField  dlfolder;
   AcidImageCanvas imgcanvas;   
   TextArea   log;
   TextField  message;
   Button     browse;
   Button     browse2;
   Checkbox   sdmall;
   Checkbox   chdkall;
   Checkbox   chdkallt;
   Checkbox   chdeall;
   Checkbox   chdeallt;
   Button     downsdm;
   Button     downchdk;
   Button     downchdkt;
   Button     downchde;
   Button     downchdet;
   Button     exit;


   /* the following values are the defaults for various properties */   
   static final String SDMSITE  = "http://stereo.jpn.org/eng/sdm/";
   static final String SDMPAGE  = "download.htm";
   static final String SDMADD   = "common_files.zip";
   static final String ASSISTSITE = "http://zenoshrdlu.com/assist/";
   static final String SDMXSITE = "http://zenoshrdlu.com/assist/sdm/";
   static final String CHDKSITE = "http://mighty-hoernsche.de/";
   static final String CHDKSITET= "http://mighty-hoernsche.de/trunk/";
   static final String CHDKPAGE = "";
   static final String CHDKPAGET= "";
   static final String CHDKPREF = "bins/";
   static final String CHDESITE = "http://autobuild.akalab.de/";
   static final String CHDESITET= "http://autobuild.akalab.de/";
   static final String CHDEPAGE = "release_full.txt";
   static final String CHDEPAGET= "trunk_full.txt";
   static final String CHDEPREF = "-full";
   static final String ACIDSITE = "http://zenoshrdlu.com/acid/";
 
   static final String SDMPROPS  = "acidsdm.properties";  
   static final String SDMXPROPS = "assistsdmx.properties";  
   static final String CHDKPROPS = "acidchdk.properties";  
   static final String CHDKTPROPS= "acidchdkt.properties";  
   static final String CHDEPROPS = "acidchdes.properties";  
   static final String CHDETPROPS= "acidchdet.properties";  
   static final String PROPS     = "acid.properties";
   static final String PROPT     = "Acid Properties - Dave Mitchell (dave@zenoshrdlu.com)";
   static final String DLPROP    = "downloadfolder";
   static final String DBGPROP   = "debug";
   
   static final String FONTNAME = "fontname";
   static final String DFLTFONT = "SansSerif";
   static final String FONTSIZE = "fontsize";
   static final int    DFLTSIZE = 12;
   static final String FONTBOLD = "fontbold";
   static final String LOGPROP  = "log";
   static final String LOGFILE  = "logfile";
   static final String DFLTLOG  = "acidlog.txt";
   
   
   static final String LOCALPROP = "local";
   static final String YES       = "YES"; 
   static final String NO        = "NO"; 
   static final String USERDIR   = "user.dir";
   
   static final String AURLPROP  = "acidurl";
   static final String SURLPROP  = "sdmurl";
   static final String SXURLPROP = "sdmxurl";
   static final String SADDPROP  = "sdmadd";
   static final String CURLPROP  = "chdkurl";
   static final String CURLPROPT = "chdkurl1";
   static final String CPGPROP   = "chdkpage";
   static final String CPGPROPT  = "chdkpage1";
   static final String CPGAPROP  = "chdkpalt";  
   static final String DURLPROP  = "chdeurl";
   static final String DURLPROPT = "chdeurlt";
   static final String SPGPROP   = "sdmpage";   
   static final String DPGPROP   = "chdepages";  
   static final String DPGPROPT  = "chdepaget";  
   static final String CPRFPROP  = "chdkprefix";   
   static final String DPRFPROP  = "chdeprefix";   
   static final String HPROP     = "height";
   static final String WPROP     = "width";
   static final String NLSPROP   = "langfile";
   
   /* NLS Properties */
   static final String TITLEPROP    = "title";
   static final String CHOOSERPROP  = "chooser";
   static final String CHOOSERPROP2 = "jchooser";
   static final String DROP1PROP    = "droptext1";
   static final String DROP2PROP    = "droptext2";
   static final String IMGLBLPROP   = "imagelabel";
   static final String DTLBLPROP    = "datelabel";
   static final String CAMLBLPROP   = "cameralabel";
   static final String MDLLBLPROP   = "modellabel";
   static final String FRMLBLPROP   = "firmlabel";
   static final String DIRLBLPROP   = "dirlabel";
   static final String EXITBUTPROP  = "exitbutton";
   static final String SDMALLPROP   = "sdmall";
   static final String CHDKALLPROP  = "chdkall";
   static final String CHDKALLPROPT = "chdkall1";
   static final String CHDEALLPROP  = "chdeall";
   static final String CHDEALLPROPT  = "chdeall1";
   static final String LOADINGPROP  = "loading";
   static final String LOADEDPROP   = "loaded";
   static final String NOLOADPROP   = "noload";
   static final String NOFINFOPROP  = "nofinfo";
   static final String FINFOPROP    = "exifinfo";
   static final String NOTCANONPROP = "notcanon";
   static final String ZIPFAILPROP  = "zipfail";
   static final String ZIPSAVEPROP  = "zipsave";
   static final String IPROCESSPROP = "processing";
   static final String EPROCESSPROP = "endprocess";
   static final String CHOOSEPROP   = "choose";
   static final String SOMEPROP     = "some";
   static final String ALLPROP      = "all";
   static final String NONEPROP     = "none";
   static final String NOTJPGPROP   = "notjpg";
   static final String NOTDIRPROP   = "notdir";
   static final String NOTFILEPROP  = "notfile";
   static final String DROPPEDPROP  = "dropped";
   static final String DROPPINGPROP = "dropping";
   static final String EXTRACTPROP  = "extract";
   static final String NOTFOUNDPROP = "notfound";
   static final String SRCHINGPROP  = "searching";
   static final String BLDFNDPROP   = "buildfound";
   static final String TBLDOWNPROP  = "tabledown";
   static final String DWNLDINGPROP = "downlding";
   static final String NOBLDFNDPROP = "nobuildfound";
   static final String PORTINGPROP  = "porting";
   static final String DROPFAILPROP = "dropfail";
   static final String ZIPDOWNPROP  = "zipdown";
   static final String DOWNLOADPROP = "download";
   static final String DSDMBUTPROP  = "dsdmbutton";
   static final String DCHDKBUTPROP = "dchdkbutton";
   static final String DCHDKBUTPROPT= "dchdkbuttont";
   static final String DCHDEBUTPROP = "dchdebutton";
   static final String DCHDEBUTPROPT= "dchdebuttont";
   static final String BROWBUTPROP  = "browbutton";
   static final String BROWBUT2PROP = "jbrowbutton";
  
   
   static final int    DEFH      = 600;
   static final int    DEFW      = 600;

   String  uibuffer    = "";
   boolean uipresent   = false;       
   boolean local       = false;
   boolean loading     = true;
   boolean loaded      = false;
   boolean sdmloaded   = false;
   boolean sdmxloaded  = false;
   boolean chdkloaded  = false;
   boolean chdktloaded = false;
   boolean chdeloaded  = false;
   boolean chdetloaded = false;
   boolean addloaded   = false;
   boolean stabloaded  = false;
   boolean stabxloaded = false;
   boolean ctabloaded  = false;
   boolean ctabtloaded = false;
   boolean dtabloaded  = false;
   boolean dtabtloaded = false;
   boolean nlsloaded   = false;
   boolean downloadx   = false;
   boolean debug       = false;
   
   Properties nls     = new Properties();

   Properties props   = new Properties();
   Properties camsdm  = new Properties();
   Properties camsdmx  = new Properties();
   Properties camchdk = new Properties();
   Properties camchdkt= new Properties();
   Properties camchde = new Properties();
   Properties camchdet= new Properties();
//   Properties camadd  = new Properties();

   String proptitle     =  null;
   String propchooser   =  null;
   String propchooser2  = null;
   String propdrop1     =  null;
   String propdrop2     =  null;
   String propimglbl    =  null;
   String propdtlbl     =  null;
   String propcamlbl    =  null;
   String propmdllbl    =  null;
   String propfrmlbl    =  null;
   String propdirlbl    =  null;
   String propexitbut   =  null;
   String propsdmall    =  null;
   String propchdkall   =  null;
   String propchdkallt  =  null;
   String propchdeall   =  null;
   String propchdeallt  =  null;
   String proploading   =  null;
   String proploaded    =  null;
   String propnoload    =  null;
   String propnofinfo   =  null;
   String propfinfo     =  null;
   String propnotcanon  =  null;
   String propzipfail   =  null;
   String propzipsave   =  null;
   String propiprocess  =  null;
   String propeprocess  =  null;
   String propchoose    =  null;
   String propsome      =  null;
   String propall       =  null;
   String propnone      =  null;
   String propnotjpg    =  null;
   String propnotdir    =  null;
   String propnotfile   =  null;
   String propdropped   =  null;
   String propdropping  =  null;
   String propextract   =  null;
   String propnotfound  =  null;
   String propsrching   =  null;
   String propbldfnd    =  null;
   String proptbldown   =  null;
   String propdwnlding  =  null;
   String propnobldfnd  =  null;
   String propporting   =  null;
   String propdropfail  =  null;
   String propzipdown   =  null;
   String propdownload  =  null;
   String propdsdmbut   =  null;
   String propdchdkbut  =  null;
   String propdchdkbutt =  null;
   String propdchdebut  =  null;
   String propdchdebutt  =  null;
   String propbrowbut   =  null;
   String propbrowbut2  = null;

   String chdkfnd       = null;
   String chdktfnd      = null;
   String chdefnd       = null;
   String chdetfnd       = null;
   String sdmfnd        = null;
   String chdksrch      = null;
   String chdktsrch     = null;
   String chdesrch      = null;
   String chdetsrch      = null;
   String sdmsrch       = null;
   String nochdk        = null;
   String nochdkt       = null;
   String nochde        = null;
   String nochdet        = null;
   String nosdm         = null;
   
   String szip  = null;
   String szip2 = null;
   String czip  = null;
   String czip2 = null;
   String czipt = null;
   String czipt2= null;
   String dzip  = null;
   String dzip2 = null;
   String dzipt = null;
   String dzipt2= null;
   File zipfile = null;

   String sdmsite  = null;
   String sdmxsite = null;
   String chdksite = null;
   String chdksitet = null;
   String chdesite = null;
   String chdesitet = null;
   String sdmpage  = null;
   String chdkpage = null;
   String chdkpaget= null;
//   String chdkpalt = null;
   String chdepage = null;
   String chdepaget= null;
   String sdmadd   = null;
   String assistsite = null;
   String chdkpref = null;
   String chdepref = null;
   String acidsite = null;
   String language = null;
   
   String dlpath;
   String sdmcamname;
   String chdkcamname;
   String chdktcamname;
   String chdecamname;
   String chdetcamname;
   DropTarget droptarget = null;
   DropTarget dropfolder = null;
   String sdmurl   = "";
   String sdmxurl  = "";
   String chdkurl  = "";
   String chdkturl = "";
   String chdeurl  = "";
   String chdeturl = "";

   String     fontprop;
   boolean    boldprop;
   int        fsizeprop;
   boolean    dolog = false;
   String     logfile;

   int		  hprop;
   int		  wprop;

/*----------------------------------------------------------------------------*/
/*  Construct the GUI                                                         */
/*----------------------------------------------------------------------------*/
   public Acid() {
   
   	  Application app = Application.getApplication();
      app.addApplicationListener(this);

      addWindowListener(this);
      acidengine = new AcidEngine();
      webgetter = new AcidWebGet();

      String s = getProperty(DBGPROP);
      debug    =  (s.toUpperCase().equals(YES));

      s = getProperty(LOGPROP);
      dolog = (s.toUpperCase().equals(YES));

      logfile = getProperty(LOGFILE);
      if (logfile.equals(""))
         logfile = DFLTLOG;
            
      fontprop = getProperty(FONTNAME);
      if (fontprop.equals(""))
         fontprop = DFLTFONT;
         
      s = getProperty(FONTBOLD);
      boldprop = s.toUpperCase().equals(YES);
         
      fsizeprop = DFLTSIZE;   
      try {
         s = getProperty(FONTSIZE);
         if (!s.equals("")) {
            fsizeprop = Integer.parseInt(s);
         }
      } catch (Exception e) {
      } 

      s = getProperty(LOCALPROP);
      local    = (s.toUpperCase().equals(YES));
     
      sdmsite  = getProperty(SURLPROP);
      sdmxsite  = getProperty(SXURLPROP);
      chdksite = getProperty(CURLPROP);
      chdksitet= getProperty(CURLPROPT);
      chdesite = getProperty(DURLPROP);
      chdesitet= getProperty(DURLPROPT);
      sdmpage  = getProperty(SPGPROP);
      chdkpage = getProperty(CPGPROP);
      chdkpaget= getProperty(CPGPROPT);
//      chdkpalt = getProperty(CPGAPROP);
      chdepage = getProperty(DPGPROP);
      chdepaget= getProperty(DPGPROPT);
      sdmadd   = getProperty(SADDPROP);
      assistsite = getProperty(AURLPROP);
      chdkpref = getProperty(CPRFPROP);
      chdepref = getProperty(DPRFPROP);
      acidsite = getProperty(AURLPROP);
      language = getProperty(NLSPROP);
      if (sdmsite.length() == 0)  sdmsite  = SDMSITE;
      if (sdmxsite.length() == 0)  sdmxsite  = SDMXSITE;
      if (chdksite.length() == 0) chdksite = CHDKSITE;
      if (chdksitet.length() == 0)chdksitet = CHDKSITET;
      if (chdesite.length() == 0) chdesite = CHDESITE;
      if (chdesitet.length() == 0) chdesitet = CHDESITET;
      if (sdmpage.length() == 0)  sdmpage  = SDMPAGE;
      if (sdmadd.length() == 0)   sdmadd   = SDMADD;
      if (assistsite.length() == 0) assistsite = ASSISTSITE;
      if (chdkpage.length() == 0) chdkpage = CHDKPAGE;
      if (chdkpaget.length() == 0) chdkpaget = CHDKPAGET;
      if (chdkpref.length() == 0) chdkpref = CHDKPREF;
      if (chdepage.length() == 0) chdepage = CHDEPAGE;
      if (chdepaget.length() == 0) chdepaget = CHDEPAGET;
      if (chdepref.length() == 0) chdepref = CHDEPREF;
      if (acidsite.length() == 0) acidsite = ACIDSITE;
      if (language.length() != 0) 
         loadLanguage(language);
      else
         loadLanguage("");
      hprop = DEFH;
      wprop = DEFW;
      try {
         s = getProperty(HPROP);
         if (!s.equals("")) {
            hprop = Integer.parseInt(s);
         }
         s = getProperty(WPROP);
         if (!s.equals("")) {
            wprop = Integer.parseInt(s);
         }
      } catch (Exception e) {
      }
      
      Font f = new Font(fontprop, (boldprop? Font.BOLD: Font.PLAIN), fsizeprop);
      setFont(f);
      
      setTitle(proptitle + " - " + VERSION);     
      setBackground(Color.lightGray);
      GridBagLayout gridbag = new GridBagLayout();
      setLayout(gridbag);

      GridBagConstraints c = new GridBagConstraints();
      c.insets = new Insets(1,2,1,2);
      c.fill = GridBagConstraints.HORIZONTAL;
     
      /* first row of the GUI - image file label */
      c.gridwidth  = 2;
      c.gridheight = 1;
      c.weightx = 1;
      c.weighty = 0;
      Label l0 = new Label(propimglbl);
      c.gridx = 2;
      c.gridy = 0;
      gridbag.setConstraints(l0,c);
      add(l0);
 
      /* second row of the GUI - image file name and browse jpegs button */
      c.gridwidth  = 4;
      imagefile = new TextField("", 40);
      imagefile.setBackground(Color.cyan);
      imagefile.setForeground(Color.black);
      imagefile.setEnabled(false);
      c.gridx = 0;
      c.gridy = 1;
      gridbag.setConstraints(imagefile,c);
      add(imagefile);

      c.fill = GridBagConstraints.NONE;
      c.gridwidth = 1;
      browse2 = new Button(propbrowbut2);
      browse2.addActionListener(this);
      c.gridx = 4;
      c.gridy = 1;
      gridbag.setConstraints(browse2,c);
      add(browse2);
  
      /* third row of the GUI - date/time camera make/model labels */
      c.gridwidth  = 2;
      Label l1 = new Label(propdtlbl);
      c.gridx = 0;
      c.gridy = 2;
      gridbag.setConstraints(l1,c);
      add(l1);

      Label l3 = new Label(propcamlbl);
      c.gridx = 2;
      c.gridy = 2;
      gridbag.setConstraints(l3,c);
      add(l3);

      Label l4 = new Label(propmdllbl);
      c.gridx = 4;
      c.gridy = 2;
      gridbag.setConstraints(l4,c);
      add(l4);

      /* fourth row of the GUI - date/time and camera make/model info */
      dtinfo = new TextField("", 20);
      dtinfo.setBackground(Color.cyan);
      dtinfo.setForeground(Color.black);
      c.gridx = 0;
      c.gridy = 3;
      gridbag.setConstraints(dtinfo,c);
      add(dtinfo);
      dtinfo.setEnabled(false);
    
      cminfo  = new TextField("", 20);
      cminfo.setBackground(Color.cyan);
      cminfo.setForeground(Color.black);
      c.gridx = 2;
      c.gridy = 3;
      gridbag.setConstraints(cminfo,c);
      add(cminfo);
      cminfo.setEnabled(false);
      
      mdinfo  = new TextField("", 20);
      mdinfo.setBackground(Color.cyan);
      mdinfo.setForeground(Color.black);
      c.gridx = 4;
      c.gridy = 3;
      gridbag.setConstraints(mdinfo,c);
      add(mdinfo);
      mdinfo.setEnabled(false);

      /* fifth row of the GUI - firmware version download folder label */
      c.gridwidth  = 2;
      Label l5 = new Label(propfrmlbl);
      c.gridx = 0;
      c.gridy = 4;
      gridbag.setConstraints(l5,c);
      add(l5);

      c.gridwidth  = 2;
      Label l6 = new Label(propdirlbl);
      c.gridx = 2;
      c.gridy = 4;
      gridbag.setConstraints(l6,c);
      add(l6);

      /* sixth row of the GUI - firmware version, download folder and browse button */
      c.gridwidth  = 2;
      fminfo  = new TextField("", 10);
      fminfo.setBackground(Color.cyan);
      fminfo.setForeground(Color.black);
      c.gridx = 0;
      c.gridy = 5;
      gridbag.setConstraints(fminfo,c);
      add(fminfo);
      fminfo.setEnabled(false);
      
      c.gridwidth  = 2;
      dlfolder  = new TextField("", 30);
      dlfolder.setBackground(Color.cyan);
      dlfolder.setForeground(Color.black);
      c.gridx = 2;
      c.gridy = 5;
      gridbag.setConstraints(dlfolder,c);
      add(dlfolder);
      dlfolder.setEnabled(false);  // apple only
      
      String dlf = getProperty(DLPROP);
      if (dlf.length() == 0) {
         dlf = System.getProperty(USERDIR) + File.separator;
      }
      dlfolder.setText(dlf);
      dropfolder = new DropTarget(dlfolder, this);
 
      c.fill = GridBagConstraints.NONE;
      c.gridwidth = 1;
      browse = new Button(propbrowbut);
      browse.addActionListener(this);
      c.gridx = 4;
      c.gridy = 5;
      gridbag.setConstraints(browse,c);
      add(browse);
  
      /* seventh/ninth row of the GUI - photo drop zone (3 rows) */
      c.gridwidth  = 2;
      c.gridheight = 5;
      c.gridx = 0;
      c.gridy = 6;
      c.weighty = 1;
      c.fill = GridBagConstraints.BOTH;
      imgcanvas = new AcidImageCanvas(propdrop1, propdrop2);
      gridbag.setConstraints(imgcanvas,c);
      add(imgcanvas); 
      droptarget = new DropTarget(imgcanvas, this); 
  
      /* seventh row of the GUI - SDM choice and download button */
      c.fill = GridBagConstraints.NONE;
      c.gridwidth = 1;
      c.gridheight = 1;
      c.weighty = 0;
      c.anchor = GridBagConstraints.WEST;
      sdmall  = new Checkbox(propsdmall);
      c.gridx = 2;
      c.gridy = 6;
      gridbag.setConstraints(sdmall,c);
      add(sdmall);
      sdmall.setState(false);

      c.anchor = GridBagConstraints.CENTER;
      downsdm = new Button(propdsdmbut);
      downsdm.addActionListener(this);
      c.gridx = 4;
      c.gridy = 6;
      gridbag.setConstraints(downsdm,c);
      add(downsdm);
      downsdm.setEnabled(false);

      /* eighth row of the GUI - CHDK stable choice and download button */
      c.anchor = GridBagConstraints.WEST;
      chdkall  = new Checkbox(propchdkall);
      c.gridx = 2;
      c.gridy = 7;
      gridbag.setConstraints(chdkall,c);
      add(chdkall);
      chdkall.setState(false);

      c.anchor = GridBagConstraints.CENTER;
      downchdk = new Button(propdchdkbut);
      downchdk.addActionListener(this);
      c.gridx = 4;
      c.gridy = 7;
      gridbag.setConstraints(downchdk,c);
      add(downchdk);
      downchdk.setEnabled(false);

      /* ninth row of the GUI - CHDK unstable choice and download button */
      c.anchor = GridBagConstraints.WEST;
      chdkallt  = new Checkbox(propchdkallt);
      c.gridx = 2;
      c.gridy = 8;
      gridbag.setConstraints(chdkallt,c);
      add(chdkallt);
      chdkallt.setState(false);

      c.anchor = GridBagConstraints.CENTER;
      downchdkt = new Button(propdchdkbutt);
      downchdkt.addActionListener(this);
      c.gridx = 4;
      c.gridy = 8;
      gridbag.setConstraints(downchdkt,c);
      add(downchdkt);
      downchdkt.setEnabled(false);

      /* tenth row of the GUI - CHDKDE stable choice and download button */
      c.anchor = GridBagConstraints.WEST;
      chdeall  = new Checkbox(propchdeall);
      c.gridx = 2;
      c.gridy = 9;
      gridbag.setConstraints(chdeall,c);
      add(chdeall);
      chdeall.setState(false);

      c.anchor = GridBagConstraints.CENTER;
      downchde = new Button(propdchdebut);
      downchde.addActionListener(this);
      c.gridx = 4;
      c.gridy = 9;
      gridbag.setConstraints(downchde,c);
      add(downchde);
      downchde.setEnabled(false);
 
      /* tenth row of the GUI - CHDKDE trunk choice and download button */
      c.anchor = GridBagConstraints.WEST;
      chdeallt  = new Checkbox(propchdeallt);
      c.gridx = 2;
      c.gridy = 10;
      gridbag.setConstraints(chdeallt,c);
      add(chdeallt);
      chdeallt.setState(false);

      c.anchor = GridBagConstraints.CENTER;
      downchdet = new Button(propdchdebutt);
      downchdet.addActionListener(this);
      c.gridx = 4;
      c.gridy = 10;
      gridbag.setConstraints(downchdet,c);
      add(downchdet);
      downchdet.setEnabled(false);
 
      /* twelth row of the GUI - log area */
      c.weightx = 1;
      c.gridwidth = 6;
      c.fill = GridBagConstraints.BOTH;
      c.weighty = 10;
      log = new TextArea(15,60);
      c.gridx = 0;
      c.gridy = 11;
      gridbag.setConstraints(log,c);
      add(log);

      /* thirteenth row of the GUI - message area */
      c.fill = GridBagConstraints.HORIZONTAL;
      c.gridwidth  = 6;
      message = new TextField("", 50);
      message.setBackground(Color.cyan);
      message.setForeground(Color.black);
      c.gridx = 0;
      c.gridy = 12;
      gridbag.setConstraints(message,c);
      add(message);

      /* fourteenth row of the GUI - exit button */
      c.fill = GridBagConstraints.NONE;
      c.gridwidth = 1;
      exit = new Button(propexitbut);
      exit.addActionListener(this);
      c.gridx = 2;
      c.gridy = 13;
      gridbag.setConstraints(exit,c);
      add(exit);

      chooser = new FileDialog(this, propchooser, FileDialog.LOAD);
      chooser2 = new FileDialog(this, propchooser2, FileDialog.LOAD);

      uipresent = true;
      log.append(uibuffer);
//      browse.requestFocusInWindow();
      setSize(wprop, hprop);
//      pack();
      setLocation(50,50); 
      setVisible(true);
      setMessage(proploading);
 
      /* try get the CHDK download webpage */     
      chdkurl = getWebPage(chdksite + chdkpage);
//      if (chdkurl == null || chdkurl.indexOf(chdkpref) == -1) {
//         addDebugMessage("CHDK website failed - trying old CHDK website");  
//         chdkurl = getWebPage(chdksite + chdkpalt);
//      }   
      if (chdkurl == null) {  
         setMessage(CHDK + propnoload);      
         addLogMessage(CHDK + propnoload);
      } else {
         int i = chdkurl.indexOf(CHDKTEST);
         if (i == -1) {
            setMessage(CHDK + propnoload);      
            addLogMessage(CHDK + propnoload);      
         } else {
            ctabloaded = true;
         }
      }
 
      /* and the CHDK trunk page */
      chdkturl = getWebPage(chdksitet + chdkpaget);
//      if (chdkurl1 == null || chdkurl1.indexOf(chdkpref) == -1) {
//         addDebugMessage("CHDK website failed - trying old CHDK website");  
//         chdkurl = getWebPage(chdksite + chdkpalt);
//      }   
      if (chdkturl == null) {  
         setMessage(CHDKT + propnoload);      
         addLogMessage(CHDKT + propnoload);
      } else {
         int i = chdkturl.indexOf(CHDKTEST);
         if (i == -1) {
            setMessage(CHDKT + propnoload);      
            addLogMessage(CHDKT + propnoload);      
         } else {
            ctabtloaded = true;
         }
      }

      /* try get the CHDK-DE download webpage */ 
      chdeurl = getWebPage(chdesite + chdepage);
      if (chdeurl == null) {
         setMessage(CHDE + propnoload);      
         addLogMessage(CHDE + propnoload);      
      } else {
         int i = chdeurl.indexOf(CHDETEST);
         if (i == -1) {
            setMessage(CHDE + propnoload);      
            addLogMessage(CHDE + propnoload);      
         } else {
           dtabloaded = true;
         }
      }

      /* try get the CHDK-DE trunk download webpage */   
       chdeturl = getWebPage(chdesitet + chdepaget);
      if (chdeturl == null) {
         setMessage(CHDET + propnoload);      
         addLogMessage(CHDET + propnoload);      
      } else {
         int i = chdeturl.indexOf(CHDETEST);
         if (i == -1) {
            setMessage(CHDET + propnoload);      
            addLogMessage(CHDET + propnoload);      
         } else {
           dtabtloaded = true;
         }
      }

      /* try to get the SDM download webpage */
      sdmurl = getWebPage(sdmsite + sdmpage);
      if (sdmurl == null) {
         setMessage(SDM + propnoload);      
         addLogMessage(SDM + propnoload);      
      } else {
         int i = sdmurl.indexOf(SDMTEST);
         if (i == -1) {
            setMessage(SDM + propnoload);      
            addLogMessage(SDM + propnoload);      
         } else {
            stabloaded = true;
         }
      }
      
      /* try to get the SDM extra download webpage */
      sdmxurl = getWebPage(sdmxsite + sdmpage);
      if (sdmxurl == null) {
         setMessage(SDMX + NOLOAD);      
         addLogMessage(SDMX + NOLOAD);      
      } else {
         int i = sdmxurl.indexOf(SDMXTEST);
         if (i == -1) {
            setMessage(SDMX + NOLOAD);      
            addLogMessage(SDMX + NOLOAD);      
         } else {
            stabxloaded = true;
         }
      }
      
      loading = false;
     
      if (ctabloaded && ctabtloaded && stabloaded && dtabloaded) { 
         setMessage(proploaded);      
         addLogMessage(proploaded);      
      } else if (!ctabloaded && !ctabtloaded && !stabloaded && !dtabloaded) {
         loading = true;
      }      
   } 
   
/*----------------------------------------------------------------------------*/
/*  load a set of UI strings from language (property) file                    */
/*----------------------------------------------------------------------------*/ 
void loadLanguage(String lang) {
   nls = loadLocalProperties(lang);
   if (lang.length() != 0 && nls.isEmpty()) {
      addLogMessage("Language file '" + lang + "' not found - defaulting to English");
   }
   proptitle      = getNLSProp(TITLEPROP     , TITLE);
   propchooser    = getNLSProp(CHOOSERPROP   ,CHOOSER);
   propchoose     = getNLSProp(CHOOSEPROP    ,CHOOSE);
   propchooser2   = getNLSProp(CHOOSERPROP2  ,CHOOSER2);
   propdrop1      = getNLSProp(DROP1PROP     ,DROP1);
   propdrop2      = getNLSProp(DROP2PROP     ,DROP2);
   propimglbl     = getNLSProp(IMGLBLPROP    ,IMGLBL);
   propdtlbl      = getNLSProp(DTLBLPROP     ,DTLBL);
   propcamlbl     = getNLSProp(CAMLBLPROP    ,CAMLBL);
   propmdllbl     = getNLSProp(MDLLBLPROP    ,MDLLBL);
   propfrmlbl     = getNLSProp(FRMLBLPROP    ,FRMLBL);
   propdirlbl     = getNLSProp(DIRLBLPROP    ,DIRLBL);
   propexitbut    = getNLSProp(EXITBUTPROP   ,EXITBUT);
   propsdmall     = getNLSProp(SDMALLPROP    ,SDMALL);
   propchdkall    = getNLSProp(CHDKALLPROP   ,CHDKALL);
   propchdkallt   = getNLSProp(CHDKALLPROPT  ,CHDKALLT);
   propchdeall    = getNLSProp(CHDEALLPROP   ,CHDEALL);
   propchdeallt   = getNLSProp(CHDEALLPROPT  ,CHDEALLT);
   proploading    = getNLSProp(LOADINGPROP   ,LOADING);
   proploaded     = getNLSProp(LOADEDPROP    ,LOADED);
   propnoload     = getNLSProp(NOLOADPROP    ,NOLOAD);
   propnofinfo    = getNLSProp(NOFINFOPROP   ,NOFINFO);
   propfinfo      = getNLSProp(FINFOPROP     ,FINFO);
   propnotcanon   = getNLSProp(NOTCANONPROP  ,NOTCANON);
   propzipfail    = getNLSProp(ZIPFAILPROP   ,ZIPFAIL);
   propzipsave    = getNLSProp(ZIPSAVEPROP   ,ZIPSAVE);
   propiprocess   = getNLSProp(IPROCESSPROP  ,IPROCESS);
   propeprocess   = getNLSProp(EPROCESSPROP  ,EPROCESS);
   propsome       = getNLSProp(SOMEPROP      ,SOME);
   propall        = getNLSProp(ALLPROP       ,ALL);
   propnone       = getNLSProp(NONEPROP      ,NONE);
   propnotjpg     = getNLSProp(NOTJPGPROP    ,NOTJPG);
   propnotdir     = getNLSProp(NOTDIRPROP    ,NOTDIR);
   propnotfile    = getNLSProp(NOTFILEPROP   ,NOTFILE);
   propdropped    = getNLSProp(DROPPEDPROP   ,DROPPED);
   propdropping   = getNLSProp(DROPPINGPROP  ,DROPPING);
   propextract    = getNLSProp(EXTRACTPROP   ,EXTRACT);
   propnotfound   = getNLSProp(NOTFOUNDPROP  ,NOTFOUND);
   propsrching    = getNLSProp(SRCHINGPROP   ,SRCHING);
   propbldfnd     = getNLSProp(BLDFNDPROP    ,BLDFND);
   proptbldown    = getNLSProp(TBLDOWNPROP   ,TBLDOWN);
   propdwnlding   = getNLSProp(DWNLDINGPROP  ,DWNLDING);
   propnobldfnd   = getNLSProp(NOBLDFNDPROP  ,NOBLDFND);
   propporting    = getNLSProp(PORTINGPROP   ,PORTING);
   propdropfail   = getNLSProp(DROPFAILPROP  ,DROPFAIL);
   propzipdown    = getNLSProp(ZIPDOWNPROP   ,ZIPDOWN);
   propdownload   = getNLSProp(DOWNLOADPROP  ,DOWNLOAD);
   propdsdmbut    = getNLSProp(DSDMBUTPROP   ,DSDMBUT);
   propdchdkbut   = getNLSProp(DCHDKBUTPROP  ,DCHDKBUT);
   propdchdkbutt  = getNLSProp(DCHDKBUTPROPT ,DCHDKBUTT);
   propdchdebut   = getNLSProp(DCHDEBUTPROP  ,DCHDEBUT);
   propdchdebutt  = getNLSProp(DCHDEBUTPROPT ,DCHDEBUTT);
   propbrowbut    = getNLSProp(BROWBUTPROP   ,BROWBUT);
   propbrowbut2   = getNLSProp(BROWBUT2PROP  ,BROWBUT2);
   chdkfnd        = CHDK + propbldfnd;
   chdktfnd       = CHDKT + propbldfnd;
   chdefnd        = CHDE + propbldfnd;
   chdetfnd       = CHDET + propbldfnd;
   sdmfnd         = SDM  + propbldfnd;
   chdksrch       = CHDK + propsrching;
   chdktsrch      = CHDKT + propsrching;
   chdesrch       = CHDE + propsrching;
   chdetsrch      = CHDET + propsrching;
   sdmsrch        = SDM  + propsrching;
   nochdk         = CHDK + propnobldfnd;
   nochdkt        = CHDKT + propnobldfnd;
   nochde         = CHDE + propnobldfnd;
   nochdet        = CHDET + propnobldfnd;
   nosdm          = SDM  + propnobldfnd;
 }

/*----------------------------------------------------------------------------*/
/*  get a string from a language file, or use the default if not found        */
/*----------------------------------------------------------------------------*/
  String getNLSProp(String pname, String deflt) {
     String s = nls.getProperty(pname);
     if (s == null)
         s = deflt;
     else {
         s = s.trim();
         if (s.length() > 2) {
           if (s.charAt(0) == '"')
              s = s.substring(1,s.length()-1);
         }
     }
     return s;
  }

/*----------------------------------------------------------------------------*/
/*  load the given properties file                                            */
/*----------------------------------------------------------------------------*/
   boolean loadProperties(String propfile) {
      if (propfile.equals(PROPS)) {
 		 props = loadLocalProperties(PROPS);
		 loaded = true;
		 return loaded;
	  } else {
		 if (!local) { 
            addLogMessage(propdwnlding + acidsite + propfile);
		    if (propfile.equals(SDMPROPS)) {
			   camsdm = webgetter.getPropertyContent(acidsite+SDMPROPS);
			   sdmloaded = (camsdm != null);
               return sdmloaded;
		    } else if (propfile.equals(SDMXPROPS)) {
			   camsdmx = webgetter.getPropertyContent(assistsite+SDMXPROPS);
			   sdmxloaded = (camsdmx != null);
               return sdmxloaded;
		    } else if (propfile.equals(CHDKPROPS)) {
			   camchdk = webgetter.getPropertyContent(acidsite+CHDKPROPS);
			   chdkloaded = (camchdk != null);
			   return chdkloaded;
		    } else if (propfile.equals(CHDKTPROPS)) {
			   camchdkt = webgetter.getPropertyContent(acidsite+CHDKTPROPS);
			   chdktloaded = (camchdkt != null);
			   return chdktloaded;
		    } else if (propfile.equals(CHDEPROPS)) {
			   camchde = webgetter.getPropertyContent(acidsite+CHDEPROPS);
			   chdeloaded = (camchde != null);
			   return chdeloaded;
		    } else if (propfile.equals(CHDETPROPS)) {
			   camchdet = webgetter.getPropertyContent(acidsite+CHDETPROPS);
			   chdetloaded = (camchdet != null);
			   return chdetloaded;
//		    } else if (propfile.equals(ADDPROPS)) {
//			   camadd = webgetter.getPropertyContent(acidsite+ADDPROPS);
//			   addloaded = (camadd != null);
//			   return addloaded;
		    }
		 } else {
            addDebugMessage("loading " + propfile + " locally");
		    if (propfile.equals(SDMPROPS)) {
			   camsdm = loadLocalProperties(SDMPROPS);
			   sdmloaded = (camsdm != null); 
               return sdmloaded;
		    } else if (propfile.equals(SDMXPROPS)) {
			   camsdmx = loadLocalProperties(SDMXPROPS);
			   sdmxloaded = (camsdmx != null);
               return sdmxloaded;
		    } else if (propfile.equals(CHDKPROPS)) {
			   camchdk = loadLocalProperties(CHDKPROPS);
			   if (camchdk != null) 
			   chdkloaded = (camchdk != null);
               return chdkloaded;
		    } else if (propfile.equals(CHDKTPROPS)) {
			   camchdkt = loadLocalProperties(CHDKTPROPS);
			   if (camchdkt != null) 
			   chdktloaded = (camchdkt != null);
               return chdktloaded;
		    } else if (propfile.equals(CHDEPROPS)) {
			   camchde = loadLocalProperties(CHDEPROPS);
			   if (camchde != null) 
			   chdeloaded = (camchde != null);
               return chdeloaded;
		    } else if (propfile.equals(CHDETPROPS)) {
			   camchdet = loadLocalProperties(CHDETPROPS);
			   if (camchdet != null) 
			   chdetloaded = (camchdet != null);
               return chdetloaded;
//		    } else if (propfile.equals(ADDPROPS)) {
//			   camadd = loadLocalProperties(ADDPROPS);
//			   addloaded = (camadd != null);
//			   return addloaded;
		    }
         }
         return false;
      }   
   }

/*----------------------------------------------------------------------------*/
/*  load the local properties file                                            */
/*----------------------------------------------------------------------------*/
   public Properties loadLocalProperties(String propfile) {
      Properties p = new Properties();
      if (propfile.length() == 0) {
         addDebugMessage("Defaulting to English");
         return p;
      }
      try {
         FileInputStream in = new FileInputStream(propfile);
         BufferedInputStream bis = new BufferedInputStream(in);
         p.load(bis);
         in.close();
      } catch (Exception e) {
         addDebugMessage("error loading " + propfile + " - " + e);
      }
      return p;
   }

/*----------------------------------------------------------------------------*/
/*  save the main properties file                                             */
/*----------------------------------------------------------------------------*/
   boolean saveProperties(String propfile) {
      try {
         putProperty(FONTNAME, fontprop);
         putProperty(FONTBOLD, boldprop?YES:NO);
         putProperty(FONTSIZE, "" + fsizeprop);
         hprop = getHeight();
         wprop = getWidth();
         putProperty(HPROP, "" + hprop);
         putProperty(WPROP, "" + wprop);
         putProperty(DLPROP, dlfolder.getText());

         FileOutputStream out = new FileOutputStream(propfile);
         BufferedOutputStream bos = new BufferedOutputStream(out);
         props.store(bos, PROPT);
         bos.flush();
         bos.close();
         return true;
      } catch (Exception e) {
      }
      return false;
   }

/*----------------------------------------------------------------------------*/
/*  get a named property from the properties file                             */
/*----------------------------------------------------------------------------*/
   public String getProperty(String prop) {
      if (!loaded)
         loadProperties(PROPS);
      if (props == null)
         return "";
      String s = props.getProperty(prop);
      if (s == null)
         s = "";
      return s;
   }
   
/*----------------------------------------------------------------------------*/
/*  put a named property into the properties file                             */
/*----------------------------------------------------------------------------*/
   public void putProperty(String prop, String value) {
      props.put(prop, value);
   }

/*----------------------------------------------------------------------------*/
/*  get a named camera property from a camera file. The SDM and CHDK camera   */
/*  files are stored on my website. Each entry has the form:                  */
/*      0xnnnnnnn-fmv=camera-fmv where:                                       */
/*        nnnnnnn is the camera model id in the table at offset  0x0010 at    */
/*                http://gvsoft.homedns.org/exif/makernote-canon-type1.html   */ 
/*        fmv     is the actual firmware version                              */
/*        camera  is the name of the camera embedded in the zipfile name in   */
/*                the download website html                                   */
/*  This method also searches an additional table for porting information. In */
/*  this case the firmware version is ignored. The table entries either say   */
/*  "D" if a dump exists or "P" if CHDK porting is underway.                  */
/*  Note that if the 'local' property is set to 'yes' then ACID looks for the */
/*  camera property files locally rather than on my website.                  */
/*----------------------------------------------------------------------------*/
   public String getCameraProperty(int type, String prop) {
      if (type == DOSDM) {
         if (!sdmloaded) {
            if (!loadProperties(SDMPROPS)) {
               addLogMessage(SDMPROPS + propnotfound);  
               return null;
            }
         }
         if (!sdmxloaded) {
            if (!loadProperties(SDMXPROPS)) {
               addLogMessage(SDMXPROPS + NOTFOUND);  
               return null;
            }
         }   
      } else if (type == DOCHDK && !chdkloaded) {
         if (!loadProperties(CHDKPROPS)) {
            addLogMessage(CHDKPROPS + propnotfound);
            return null;
         }
      } else if (type == DOCHDKT && !chdktloaded) {
         if (!loadProperties(CHDKTPROPS)) {
            addLogMessage(CHDKTPROPS + propnotfound);
            return null;
         }
      } else if (type == DOCHDE && !chdeloaded) {
         if (!loadProperties(CHDEPROPS)) {
            addLogMessage(CHDEPROPS + propnotfound);
            return null;
         }
      } else if (type == DOCHDET && !chdetloaded) {
         if (!loadProperties(CHDETPROPS)) {
            addLogMessage(CHDETPROPS + propnotfound);
            return null;
         }

      }
      String s = null;
      if (type == DOCHDK) {
         s = camchdk.getProperty(prop);
      } else if (type == DOCHDKT) {
         s = camchdkt.getProperty(prop);
      } else if (type == DOCHDE) {
         s = camchde.getProperty(prop);
      } else if (type == DOCHDET) {
         s = camchdet.getProperty(prop);
      } else if (type == DOSDM) {
         s = camsdm.getProperty(prop);
         if (s == null) {
            s = camsdmx.getProperty(prop);
         }
      }
      return s;
   }

/*----------------------------------------------------------------------------*/
/*  analyse a photo and extract EXIF info                                     */
/*----------------------------------------------------------------------------*/
   public void processImage(String fn) {
      int fm;
      int icm;
      String cam;
      
      setCHDKDownload(false);
      setCHDKDownloadt(false);
      setCHDEDownload(false);
      setCHDEDownloadt(false);
      setSDMDownload(false);
      szip = null;
      czip = null;
      czip2 = null;
      czipt = null;
      czipt2 = null;
	  dzip = null;
      try {
         acidengine.setFile(fn);
         dtinfo.setText(acidengine.getDateInfo());
         cminfo.setText(acidengine.getCameraMake());
         mdinfo.setText(acidengine.getCameraModel());
         fm = acidengine.getFirmwareVersion();
         /* get the model-unique numeric identifier for the camera - we may know the 'model name'  */
         /* but this is not easy to match with the name used in the SDM and CHDK download webpages */
         icm = acidengine.getIntCameraModel();
         String fms = acidengine.getStringFirmwareVersion();
         if (debug)
            addDebugMessage("fm=" + fm + " icm=" + Integer.toHexString(icm) + " fms='" + fms + "'");
         if (fm == AcidEngine.NOFINFO) {
            fminfo.setText(propnofinfo);
            setMessage(propnofinfo);
            if (fms != null)
               addLogMessage(propfinfo + "'" + fms + "'");
            return;
         } else if (fm == AcidEngine.NOTCANON) {
            fminfo.setText(propnotcanon);
            setMessage(propnotcanon);
            return;
         } else {
            /* decode the firmware info */
            fminfo.setText(decode(fm));
         }
         /* at this point we know it's a Canon and have its model name, id and firmware version */
         /* so we can search the CHDK and SDM download sites for suitable builds */
         if (ctabloaded) {
            cam = getCameraProperty(DOCHDK, "0x" + Integer.toHexString(icm) + "-" + decode(fm));
            if (cam != null) {
               addLogMessage(chdksrch + cam);
               czip = clocate(chdkpref + cam);
               if (czip != null) {
                  addLogMessage(chdkfnd + czip);
                  setCHDKDownload(true);
                  chdkcamname = cam;
               }
               czip2 = clocate2(chdkpref + cam);
               if (czip2 != null) {
                  setCHDKDownload(true);
                  addLogMessage(chdkfnd + czip2);
               } else {
                  addLogMessage(nochdk + mdinfo.getText() + " " + decode(fm));
               }   
//            } else {  /* not found, so look in the additions table to see if porting is underway */
//               addLogMessage(nochdk + mdinfo.getText() + " " + decode(fm));
//               cam = getCameraProperty(DOADD, "0x" + Integer.toHexString(icm));
//               if (cam != null && cam.startsWith(PORT))
//                  addLogMessage(propporting);
            } else {  
               addLogMessage(nochdk + mdinfo.getText() + " " + decode(fm));
            }
         }
         if (ctabtloaded) {
            cam = getCameraProperty(DOCHDKT, "0x" + Integer.toHexString(icm) + "-" + decode(fm));
            if (cam != null) {
               addLogMessage(chdktsrch + cam);
               czipt = clocatet(chdkpref + cam);
               if (czipt != null) {
                  addLogMessage(chdktfnd + czipt);
                  setCHDKDownloadt(true);
                  chdktcamname = cam;
               }
               czipt2 = clocatet2(chdkpref + cam);
               if (czipt2 != null) {
                  setCHDKDownloadt(true);
                  addLogMessage(chdktfnd + czipt2);
               } else {
                  addLogMessage(nochdkt + mdinfo.getText() + " " + decode(fm));
               }   
//            } else {  /* not found, so look in the additions table to see if porting is underway */
//               addLogMessage(nochdk + mdinfo.getText() + " " + decode(fm));
//               cam = getCameraProperty(DOADD, "0x" + Integer.toHexString(icm));
//               if (cam != null && cam.startsWith(PORT))
//                  addLogMessage(propporting);
            } else {  
               addLogMessage(nochdkt + mdinfo.getText() + " " + decode(fm));
            }
         }
         if (dtabloaded) {
		    String dz;
            cam = getCameraProperty(DOCHDE, "0x" + Integer.toHexString(icm) + "-" + decode(fm));
            if (cam != null) {
               addLogMessage(chdesrch + cam);
               dz = dlocate(chdeurl, cam);
               if (dz != null) {
                  dzip = dz;
                  dzip2 = removePref(dz, chdepref);
                  addLogMessage(chdefnd + dzip);
                  addLogMessage(chdefnd + dzip2);
                  setCHDEDownload(true);
                  chdecamname = cam;
               }
            } else {  
               addLogMessage(nochde + mdinfo.getText() + " " + decode(fm));
            }
            
         }
         if (dtabtloaded) {
		    String dz;
            cam = getCameraProperty(DOCHDET, "0x" + Integer.toHexString(icm) + "-" + decode(fm));
            if (cam != null) {
               addLogMessage(chdetsrch + cam);
               dz = dlocate(chdeturl, cam);
               if (dz != null) {
                  dzipt = dz;
                  dzipt2 = removePref(dz, chdepref);
                  addLogMessage(chdetfnd + dzipt);
                  addLogMessage(chdetfnd + dzipt2);
                  setCHDEDownloadt(true);
                  chdetcamname = cam;
               }
//               dz = dlocate2(cam);
//               if (dz != null) {
//                  if (dz.indexOf(chdepref) != -1)
//				     dzip=dz;
//				  else
//				  	 dzip2=dz; 
//                  setCHDEDownloadt(true);
//                  addLogMessage(chdetfnd + dz);
//               } else {
//                  addLogMessage(nochdet + mdinfo.getText() + " " + decode(fm));
//               }   
            } else {  
               addLogMessage(nochdet + mdinfo.getText() + " " + decode(fm));
            }
         }
         if (stabloaded) {
            cam = getCameraProperty(DOSDM, "0x" + Integer.toHexString(icm) + "-" + decode(fm));
            if (cam != null) {
               addLogMessage(sdmsrch + cam);
               
               downloadx = true;
               szip = slocatex(cam); // search my site first
               if (szip == null) {
                  szip = slocate(cam);
                  if (szip != null)
                     downloadx = false;
               }      

               if (szip != null) {
                  addLogMessage(sdmfnd + szip);
                  setSDMDownload(true);
                  sdmcamname = cam;
               }   
               szip2 = slocate(sdmadd);  
            } else {
               addLogMessage(nosdm + mdinfo.getText() + " " + decode(fm));
            }
         }
         if (szip!= null && czip != null && czipt != null && dzip != null && dzipt != null)
            setMessage(propall);
         else if (szip == null && czip == null && czipt == null && dzipt == null) 
            setMessage(propnone);
         else
		    setMessage(propsome);
      } catch (Exception e) {
           addDebugMessage("Error " + e);
      }
   }

/*----------------------------------------------------------------------------*/
/*  remove 'full' from zip file string                                       */
/*----------------------------------------------------------------------------*/
   public String removePref(String z, String p) {
      int i, j;
      i = z.indexOf(p);
      if (i!= -1) 
         return z.substring(0,i) + "_" + z.substring(i+p.length()+1);
      return z;   
   }

/*----------------------------------------------------------------------------------------------*/
/* decode the firmware version value                                                            */
/* see Ger Vermeulen's page at http://gvsoft.homedns.org/exif/makernote-canon-type1.html#0x001e */
/* 0x001e 30 	4 bytes 	uLong_32 	Firmware Revision                                       */
/*	        hex number 0xAVvvRRrr                                                               */
/*	                           i1                                                               */
/*	                         i2                                                                 */  
/*	                       i3                                                                   */
/*	                     i4                                                                     */
/*	                     a                                                                      */
/*	                      v                                                                     */
/*      A = a for Alpha, b for Beta, 0 for neither                                              */
/*      Vvv = version V.vv                                                                      */
/*      RRrr = revision RR.rr (1.00=a, 2.00=b, etc)                                             */
/*----------------------------------------------------------------------------------------------*/
   public String decode(int fv) {
     int a, v, r, i1, i2, i3, i4, j;
     String s;
     i1 = fv%256;
     j  = fv/256;
     i2 = j%256;
     j  = j/256;
     i3 = j%256;
     i4 = j/256;
     a  = i4/16;
     v  = i4%16;
     s  = "";
     if (a != 0) {
        s = (a == 10 ? "Alpha " : "Beta ");
     }
     s = "" + (100*v  + i3) + ALPHA.substring(i2,i2+1);
     
     return s;
   }
 
/*----------------------------------------------------------------------------*/
/*  locate a download reference in the SDM webpage                            */
/*----------------------------------------------------------------------------*/
   public String slocate(String s) {
      return locate(sdmurl, s);
   }

/*----------------------------------------------------------------------------*/
/*  locate a download reference in the SDM Additions webpage                  */
/*----------------------------------------------------------------------------*/
   public String slocatex(String s) {
      return locate(sdmxurl, s);
   }
   
/*----------------------------------------------------------------------------*/
/*  locate a full build download reference in the CHDK webpage                */
/*----------------------------------------------------------------------------*/
   public String clocate(String s) {
         return locate(chdkurl, s);
   }
   
/*----------------------------------------------------------------------------*/
/*  locate a full build download reference in the CHDK webpage                */
/*----------------------------------------------------------------------------*/
   public String clocatet(String s) {
         return locate(chdkturl, s);
   }
   
/*----------------------------------------------------------------------------*/
/*  locate a full/simple build download reference in the CHDK-DE webpage      */
/*----------------------------------------------------------------------------*/
   public String dlocate(String page, String s) {
      int i, j, k;
       i = page.indexOf(s);
       if (i == -1) 
          return null;
          
 j = page.indexOf(LF,i);
 k = page.lastIndexOf(LF, i);
 
// System.out.println("i=" + i + " j=" + j + " k=" + k + " len=" + (j-k-1));
// System.out.println("zip='" + page.substring(k+1,j) + "'");         
       return page.substring(k+1,j);
   }
   
/*----------------------------------------------------------------------------*/
/*  locate a simple build download reference in the CHDK webpage              */
/*----------------------------------------------------------------------------*/
   public String clocate2(String s) {
         return locate2(chdkurl, s);
   }   
     
/*----------------------------------------------------------------------------*/
/*  locate a simple build download reference in the CHDK webpage              */
/*----------------------------------------------------------------------------*/
   public String clocatet2(String s) {
         return locate2(chdkturl, s);
   }   
     
/*----------------------------------------------------------------------------*/
/*  locate a full/simple download reference in the CHDK-DE webpage            */
/*----------------------------------------------------------------------------*/
//   public String dlocate2(String s) {
//         return locate3(chdeurl, s);
//   }
   
/*----------------------------------------------------------------------------*/
/*  locate a reference in a webpage                                           */
/*----------------------------------------------------------------------------*/
   public String locate(String page, String s) {
  
       int i, j, k;
       i = page.indexOf(s);
       if (i == -1) 
          return null;
       j = page.indexOf("\"",i);
       k = page.lastIndexOf("\"",i);
       return page.substring(k+1,j);
   }

/*----------------------------------------------------------------------------*/
/*  locate the second reference in a webpage                                  */
/*----------------------------------------------------------------------------*/
   public String locate2(String page, String s) {
       int i, j, k;
       i = page.indexOf(s);
       i = page.indexOf(s, i+1);
       if (i == -1) 
          return null;
       j = page.indexOf("\"",i);
       k = page.lastIndexOf("\"",i);
       return page.substring(k+1,j);
   }
   
/*----------------------------------------------------------------------------*/
/*  locate the third reference in a webpage                                   */
/*----------------------------------------------------------------------------*/
   public String locate3(String page, String s) {
       int i, j, k;
       i = page.indexOf(s);
       i = page.indexOf(s, i+1);
	   i = page.indexOf(s, i+1);
       if (i == -1) 
          return null;
       j = page.indexOf("\"",i);
       k = page.lastIndexOf("\"",i);
       return page.substring(k+1,j);
   }
   
/*----------------------------------------------------------------------------*/
/*  try to download a build and extract the files                             */
/*----------------------------------------------------------------------------*/
   public void tryDownload(int type) {
      dlpath = dlfolder.getText();
      setMessage("");
      try {
         if (type == DOSDM) {

            int okdown = 0;
            addLogMessage(szip + propzipdown);
            if (downloadx) {
               okdown = getZip(sdmxsite + szip, ZIPFILE);
            } else {
               okdown = getZip(sdmsite + szip, ZIPFILE);
            }
            if (okdown != 0) {
               setMessage(szip + propzipfail);
               return;
            } else {
               addLogMessage(szip + propzipsave);
               int rc = extractDB(ZIPFILE, type);
               if (rc == 0)
                  setMessage(propdownload);
            } 
            if (sdmall.getState()) {
               if (getZip(sdmsite + szip2, ZIPFILE) != 0) {
                  setMessage(szip2 + propzipfail);
                  return;
               } else {
                  addLogMessage(szip2 + propzipsave);
                  int rc = extractDB(ZIPFILE, type);
                  if (rc == 0)
                     setMessage(propdownload);
               }
            }
         } else if (type == DOCHDK) {
            String z = (chdkall.getState() ? czip : czip2);
            addLogMessage(z + propzipdown);
            if (getZip(chdksite + z, ZIPFILE) != 0) {
               setMessage(z + propzipfail);
               return;
            } else {
               addLogMessage(z + propzipsave);
               int rc = extractDB(ZIPFILE, type);
               addDebugMessage("extract rc = " + rc);
               if (rc == 0)
                  setMessage(propdownload);
                 
            }
         } else if (type == DOCHDKT) {
            String z = (chdkallt.getState() ? czipt : czipt2);
            addLogMessage(z + propzipdown);
            if (getZip(chdksitet + z, ZIPFILE) != 0) {
               setMessage(z + propzipfail);
               return;
            } else {
               addLogMessage(z + propzipsave);
               int rc = extractDB(ZIPFILE, type);
               addDebugMessage("extract rc = " + rc);
               if (rc == 0)
                  setMessage(propdownload);
                 
            }
         } else if (type == DOCHDE) {
            String z = (chdeall.getState() ? dzip : dzip2);
            addLogMessage(z + propzipdown);
            if (getZip(chdesite + z, ZIPFILE) != 0) {
               setMessage(z + propzipfail);
               return;
            } else {
               addLogMessage(z + propzipsave);
               int rc = extractDB(ZIPFILE, type);
               addDebugMessage("extract rc = " + rc);
               if (rc == 0)
                  setMessage(propdownload);
                 
            }
         } else if (type == DOCHDET) {
            String z = (chdeallt.getState() ? dzipt : dzipt2);
            addLogMessage(z + propzipdown);
            if (getZip(chdesitet + z, ZIPFILE) != 0) {
               setMessage(z + propzipfail);
               return;
            } else {
               addLogMessage(z + propzipsave);
               int rc = extractDB(ZIPFILE, type);
               addDebugMessage("extract rc = " + rc);
               if (rc == 0)
                  setMessage(propdownload);
                 
            }
         }   
      } catch (Exception e) {
         addDebugMessage("Error " + e);
      }
   }
   
/*----------------------------------------------------------------------------*/
/*  download a zipfile                                                        */
/*----------------------------------------------------------------------------*/
   public int getZip(String url, String fn) {
      byte buff[];
      int l;
      zipfile = null;
      try {
         zipfile = new File(fn);
         buff = new byte[1024];
         InputStream ins = webgetter.getStream(url);
         DataInputStream dis = new DataInputStream(ins);
         FileOutputStream outs = new FileOutputStream(new File(fn));
         DataOutputStream dos = new DataOutputStream(outs);
         while ((l=dis.read(buff)) != -1) {
            dos.write(buff, 0, l);
         }
         dis.close();
         dos.flush();
         dos.close();
         return 0;
      } catch(Exception e) {
         addDebugMessage("Error " + e);
         return -1;
      }
   }
   
/*----------------------------------------------------------------------------*/
/*  extract the files from a downloaded zipfile                               */
/*----------------------------------------------------------------------------*/
   public int extractDB(String fn, int type) {
      byte buff[];
      int l;
      ZipEntry ze;
      String dlp = dlpath;
      if (type == DOSDM) {
         dlp = dlp + SDM  + File.separator + sdmcamname + File.separator;
      } else if (type == DOCHDK) {
         dlp = dlp + CHDK + File.separator + chdkcamname + File.separator;
      } else if (type == DOCHDKT) {
         dlp = dlp + CHDKT + File.separator + chdktcamname + File.separator;
      } else if (type == DOCHDE) { 
         dlp = dlp + CHDE + File.separator + chdecamname + File.separator;
      } else if (type == DOCHDET) { 
         dlp = dlp + CHDET + File.separator + chdetcamname + File.separator;
      }  
      
      buff = new byte[1024];
      try {
            File np = new File(dlp);
            np.mkdirs();
            ZipInputStream zipinputstream = null;
            ZipEntry zipentry;
            zipinputstream = new ZipInputStream(new FileInputStream(fn));
            zipentry = zipinputstream.getNextEntry();
            while (zipentry != null) { 
                String entryName = zipentry.getName();
                if (!entryName.endsWith(File.separator)) {
                   addLogMessage(propextract + entryName);
                   FileOutputStream fileoutputstream;
                   File newFile = new File(entryName);
                   String directory = newFile.getParent();
                   if (directory != null) {
                      File parent = new File(dlp + directory);
                      boolean rc = parent.mkdirs();
//                      System.out.println("rc=" + rc);          
                   }
                   fileoutputstream = new FileOutputStream(dlp + entryName);             
                   while ((l = zipinputstream.read(buff, 0, 1024)) > -1)
                      fileoutputstream.write(buff, 0, l);
                   fileoutputstream.close(); 
                   zipinputstream.closeEntry();
                }
                zipentry = zipinputstream.getNextEntry();
            }

            zipinputstream.close();
            return 0;
      } catch(Exception e) {
         addDebugMessage("Error " + e);
         return -1;
      }   
   }
   
/*----------------------------------------------------------------------------*/
/*  get a webpage                                                             */
/*----------------------------------------------------------------------------*/
   public String getWebPage(String url) {
      addLogMessage(proptbldown + url); 
      try {
         return webgetter.getStringContent(url);
      } catch(Exception e1) {
         addDebugMessage("Web page " + url + " not found - " + e1);
      }
      return null;
   }

   public void setSDMDownload(boolean b) {
      downsdm.setEnabled(b);
   }

   public void setCHDKDownload(boolean b) {
      downchdk.setEnabled(b);
   }

   public void setCHDKDownloadt(boolean b) {
      downchdkt.setEnabled(b);
   }

   public void setCHDEDownload(boolean b) {
      downchde.setEnabled(b);
   }

   public void setCHDEDownloadt(boolean b) {
      downchdet.setEnabled(b);
   }

//   public void setAll(boolean b) {
//      downsdm.setEnabled(b);
//      downchdk.setEnabled(b);
//   }

   public void addLogMessage(String m) {
         if (uipresent)
            log.append(CRLF + m);
         else
            uibuffer = uibuffer + CRLF + m;
   }
   
   public void setMessage(String m) {
      message.setText(m);
   }
   
   public void addDebugMessage(String m) {
      if (debug) {
         if (uipresent)
            addLogMessage(m);
         else
            uibuffer = uibuffer + CRLF + m;
      }      
   }

/*----------------------------------------------------------------------------*/
/*  handle a GUI action event                                                 */
/*----------------------------------------------------------------------------*/
   public void actionPerformed(ActionEvent event) {
      Object o = event.getSource();
      if (o == exit) {
          doClose();
      } else if (o == browse) {
      
         /* set the OSX system property that lets a folder (not a file) be selectable */
         System.setProperty(DIRDLG, "true"); // apple only
         chooser.setDirectory(dlfolder.getText());
         chooser.setVisible(true);
         String s = chooser.getFile(); // apple version
         if (s != null) {
            s = chooser.getDirectory() + s + File.separator;
            dlpath = s;
            dlfolder.setText(dlpath);
         }   
         System.setProperty(DIRDLG, "false");
      } else if (o == browse2) {
//                 chooser2.setDirectory(directory.getText());
                 chooser2.setVisible(true);
                 String fn = chooser2.getDirectory()+chooser2.getFile();
                 if (fn != null) {
                    if (fn.toUpperCase().endsWith(JPG))  {
                        setMessage("");
                        imagefile.setText(fn);
                        imgcanvas.viewImage(fn);
                        pack();
                        addLogMessage(fn + propiprocess);
                        processImage(fn);
                        addLogMessage(propeprocess);

                    } else {
                        setMessage(fn + propnotjpg);
                    }
                 }
      } else if (o == downsdm) {
         tryDownload(DOSDM);
         if (zipfile != null)
            zipfile.delete();
      } else if (o == downchdk) {
         tryDownload(DOCHDK);
         if (zipfile != null)
            zipfile.delete();
      } else if (o == downchdkt) {
         tryDownload(DOCHDKT);
         if (zipfile != null)
            zipfile.delete();
      } else if (o == downchde) {
         tryDownload(DOCHDE);
         if (zipfile != null)
            zipfile.delete();
      } else if (o == downchdet) {
         tryDownload(DOCHDET);
         if (zipfile != null)
            zipfile.delete();
      }
   }
   
/*----------------------------------------------------------------------------*/
/*  ignore most GUI item events                                               */
/*----------------------------------------------------------------------------*/
   public void itemStateChanged(ItemEvent event) {
   }  
   public void windowClosed(WindowEvent event) {
   }
   public void windowDeiconified(WindowEvent event) {
   }
   public void windowIconified(WindowEvent event) {
   }
   public void windowActivated(WindowEvent event) {
   }
   public void windowDeactivated(WindowEvent event) {
   }
   public void windowOpened(WindowEvent event) {
   }

/*----------------------------------------------------------------------------*/
/*  Deal with drag and drop                                                   */
/*----------------------------------------------------------------------------*/
   public void 	dragEnter(DropTargetDragEvent dtde) {
       dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
   }
   public void 	dragExit(DropTargetEvent dte) {
   }
         
   public void 	dragOver(DropTargetDragEvent dtde) {
       dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
   }
         
   public void 	drop(DropTargetDropEvent dtde) {  
      if (loading)
         return;
      setMessage(propdropping);
      try {
         Component c = dtde.getDropTargetContext().getComponent();
         java.util.List fl; 
         DataFlavor uriListFlavor = new DataFlavor(URILIST);
         dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
         Transferable t = dtde.getTransferable();
         if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            fl = (java.util.List)t.getTransferData(DataFlavor.javaFileListFlavor);
         } else if (t.isDataFlavorSupported(uriListFlavor)) {
            String data = (String)t.getTransferData(uriListFlavor);
            fl = textURIListToFileList(data);   
         } else {
            setMessage(propdropfail);
            return;
         }  
         if (fl == null) {
            setMessage(propdropfail);
            return;              
         }
         File f = (File)fl.get(0);
         if (c == imgcanvas) {
            if (f.isDirectory()) {
                setMessage(propnotdir);
            } else {
               String fn = f.getAbsolutePath();
               if (fn.toUpperCase().endsWith(JPG))  {
                  setMessage("");
                  imagefile.setText(fn);
                  imgcanvas.viewImage(fn);
                  pack();
                  addLogMessage(f.getName() + propdropped);
                  addLogMessage(fn + propiprocess);
                  processImage(fn);
                  addLogMessage(propeprocess);
               } else {
                  setMessage(fn + propnotjpg);
               }
            }
         } else {
            if (f.isDirectory()) {
                dlfolder.setText(f.getAbsolutePath() + File.separator);
                setMessage("");
            } else {
                setMessage(propnotfile);
            }
         }
      } catch (Exception e) {
         addDebugMessage("drop error " + e);
      }     
   }
/*----------------------------------------------------------------------------*/
/*  handle drops which are not FileListFlavor                                 */
/*----------------------------------------------------------------------------*/
   public java.util.List textURIListToFileList(String data) {
      java.util.List list = new java.util.ArrayList(1);
      for (java.util.StringTokenizer st = new java.util.StringTokenizer(data, CRLF); st.hasMoreTokens();) {
         String s = st.nextToken();
         if (s.startsWith("#")) {
                // the line is a comment (as per the RFC 2483)
            continue;
         }
         try {
            java.net.URI uri = new java.net.URI(s);
            java.io.File file = new java.io.File(uri);
            list.add(file);
         } catch (java.net.URISyntaxException e) {
                // malformed URI
         } catch (IllegalArgumentException e) {
                // the URI is not a valid 'file:' URI
         }
      }
      return list;
   }

          
   public void 	dropActionChanged(DropTargetDragEvent dtde) {
       dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
   }

// apple only
   public void handleAbout(ApplicationEvent event) {
   }
   public void handleOpenApplication(ApplicationEvent event) {
   }
   public void handleOpenFile(ApplicationEvent event) {
   }
   public void handlePreferences(ApplicationEvent event) {
   }
   public void handlePrintFile(ApplicationEvent event) {
   }
 
   public void handleQuit(ApplicationEvent event) {
      doClose();
   }
 
   public void handleReOpenApplication(ApplicationEvent event) {
   }
   
/*----------------------------------------------------------------------------*/
/*  write the log window to a log file                                        */
/*----------------------------------------------------------------------------*/
      public void write(String fname) {

      FileOutputStream out = null;
      BufferedOutputStream bos = null;
      DataOutputStream dataOut = null;
     
      try {
         out = new FileOutputStream(fname);
         bos = new BufferedOutputStream(out);
         dataOut = new DataOutputStream(bos);
      } catch(IOException e) {
//
      }
      try {
         dataOut.writeBytes(log.getText());
         dataOut.flush();
         out.close();
      } catch(IOException e) {
//
      }
   }

/*----------------------------------------------------------------------------*/
/*  we're being shut down, so save the current settings                       */
/*----------------------------------------------------------------------------*/
   public void windowClosing(WindowEvent event) {
      doClose();
   }
   
   public void doClose() {
      saveProperties(PROPS);
      if (dolog) {
          write(logfile);
      }
      System.exit(0);
   }

   public static void main(String arg[]) {
      new Acid();
   }
}
