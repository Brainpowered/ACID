/******************************************************************************
*                                                                             *
* Image Display for ACID                                                      *
*                                                                             *
* Dave Mitchell, July 2010                                                    *
*                                                                             *
******************************************************************************/


import java.awt.*;
import java.awt.image.*;

public class AcidImageCanvas extends Canvas {
   static final Color BGCOLOR  = Color.pink; 
   static final Color TEXTCOLOR= Color.black;
   
   Image image = null;
   String info1;
   String info2;
   Color bg;
   Color fg;
        
   public AcidImageCanvas(String t1, String t2) {
      info1 = t1;
      info2 = t2;
      setMinimumSize(new Dimension(160,120));
      Font f = new Font("SansSerif", Font.BOLD, 14);
      setFont(f);
      bg = BGCOLOR;
      fg = TEXTCOLOR;
   }
   
   public AcidImageCanvas(Color fgc, Color bgc, int h, int w) {
      bg = bgc;
      fg = fgc;
      setMinimumSize(new Dimension(h,w));
   }
   
   public void paint(Graphics g) {
      g.setPaintMode();
      Dimension d = getSize();                  
      g.setColor(BGCOLOR);                      
      g.fillRect(0, 0, d.width + 1, d.height + 1);
      if (image != null) {
         int w = image.getWidth(this);
         int h = image.getHeight(this);
         if (h > w) {
            w = (w * d.height)/h;
            h = d.height;
         } else {
            h = (h * d.width)/w;
            w = d.width;
         }
         g.drawImage(image, 0, 0, w, h, this);
      } else {   
         g.translate(d.width, d.height - 1);          
         g.setColor(TEXTCOLOR);    
         g.drawString(info1, 5 - d.width, 30-d.height);    
         g.drawString(info2, 5 - d.width, 50-d.height);    
      }
   }

   public void update(Graphics g) {
      paint(g);
   }

   public void viewImage(String fname) {
      MediaTracker mt = new MediaTracker(this);
      try {
         if (image != null) {
            image.flush();
            System.gc();
         }  
         image = Toolkit.getDefaultToolkit().createImage(fname);
         mt.addImage(image, 0);
         mt.waitForID(0);
      } catch(Exception e) {
         image = null;
      }
      repaint();
   }
}
