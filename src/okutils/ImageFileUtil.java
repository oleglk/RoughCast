package OKUtils;
/* *************** Image file handler ****************************************/

import java.util.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import javax.imageio.plugins.jpeg.*;

public class ImageFileUtil
{
  
  public static BufferedImage readAsBufferedImage(File iFile)
  {
      try
	  {	BufferedImage img = ImageIO.read(iFile);
		return img;
      } catch (Exception e)
	  {	  e.printStackTrace();		return null;
      }
  }
  
  // compressionQuality ranges between 0 and 1,
  // 0-lowest, 1-highest.
  public static boolean saveImageAsJPEG(BufferedImage bi,
                                      float compressionQuality,
                                      File outFile)
  {
	   RenderedImage rendImage = (RenderedImage)bi;
	   try
	   {
			// Find a jpeg writer
            ImageWriter writer = null;
            Iterator iter = ImageIO.getImageWritersByFormatName("jpg");
            if (iter.hasNext()) {
                writer = (ImageWriter)iter.next();
            }
    
            // Prepare output file
            ImageOutputStream ios = ImageIO.createImageOutputStream(outFile);
            writer.setOutput(ios);
    
            // Set the compression quality
            ImageWriteParam iwparam = new MyImageWriteParam();
            iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT) ;
            iwparam.setCompressionQuality(compressionQuality);
    
            // Write the image
            writer.write(null, new IIOImage(rendImage, null, null), iwparam);
    
            // Cleanup
            ios.flush();
            writer.dispose();
            ios.close();
			return  true;
		}
		catch (Exception e)
		{  System.out.println(e);	  return  false;

		}
   }
   public static boolean saveImageAsJPEG(BufferedImage bi,
                                      float compressionQuality,
                                      String filename)
   {
	   return saveImageAsJPEG(bi, compressionQuality, new File(filename));
   }
}

    
    // This class overrides the setCompressionQuality() method to workaround
    // a problem in compressing JPEG images using the javax.imageio package.
    class MyImageWriteParam extends JPEGImageWriteParam {
        public MyImageWriteParam() {
            super(Locale.getDefault());
        }
    
        // This method accepts quality levels between 0 (lowest) and 1 (highest) and simply converts
        // it to a range between 0 and 256; this is not a correct conversion algorithm.
        // However, a proper alternative is a lot more complicated.
        // This should do until the bug is fixed.
        public void setCompressionQuality(float quality) {
            if (quality < 0.0F || quality > 1.0F) {
                throw new IllegalArgumentException("Quality out-of-bounds!");
            }
            this.compressionQuality = quality;
        }
    }



