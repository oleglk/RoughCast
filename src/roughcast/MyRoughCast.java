// A.G.McDowell
// This code has not been tested to professional standards and is
// provided without warranty of any kind: in fact, given the lack
// of testing, it probably has bugs I don't know about. On the
// other hand, you are free to do with it as you will - though an
// acknowledgement would be nice.

package RoughCast;

import java.util.Arrays;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Hashtable;
import java.awt.Image;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.io.IOException;
// See JDC Tech Tips October 21, 1999 for description of these classes
//(deprecated) import com.sun.image.codec.jpeg.JPEGCodec;
//(deprecated) import com.sun.image.codec.jpeg.JPEGEncodeParam;
//(deprecated) import com.sun.image.codec.jpeg.JPEGImageEncoder;
import java.io.OutputStream;
import java.util.StringTokenizer;
import java.awt.Toolkit;

/**
 * This class hosts a little utility to provide a post-camera
 * white balance. It can work out the median values of the red,
 * green, and blue pixels, and it can scale all the pixels of an
 * image so as to restore a previous ratio of red:green:blue to
 * 1:1:1. So you can take a shot (perhaps at lowest resolution)
 * of a card with a reasonably large area of white or uniform grey,
 * and use the values from this image to restore the white balance of
 * other shots taken under the same conditions. More realistically,
 * the result may not be perfect, but it may provide a more
 * convenient starting point for correction with a GUI image editing
 * tool than the original.
 * <br>
 * If you don't have a grey card you trust, you can use a card that
 * you don't trust, calibrating it by a third shot, taken under
 * better lighting conditions. Both calibration shots and the image
 * you wish to corect should be taken with a manual white balance
 * setting (e.g. cloudy). Work out the median values as the
 * values from the card under the conditions you wish to correct
 * from, divided by the values from this card under the conditions
 * for which your camera's manual white balance setting was intended.
 * I find that a photographic 18% grey card is cheap, and that cheap
 * photocopier paper is slightly blue.
 * <br>
 * It is not obvious that it makes sense to scale pixel values that
 * may not be linear functions of light intensity, due to gamma
 * correction. However, gamma correction can be at least approximated
 * by assuming that the pixel value is the light intensity raised to
 * some unknown power. This means that converting down to light
 * intensity, scaling, and then converting back up amounts to scaling
 * by some power of the original scaling factor. Since our scaling
 * factor is determined by the median of a calibration image this all
 * comes out in the wash. However, after I have scaled the pixel values
 * to adjust the colour, I may have values outside the range 0..255.
 * I scale the result (scaling all colours the same) so that the
 * highest pixel value in the image is 255. Here I am merely trying
 * to preserve as much information as possible, but the choice of
 * the best possible quantisation to use here probably does depend on
 * gamma. Hopefully this pragmatic choice won't be to bad.
 * <br>
 * My main reason to use the median as the basis
 * of compensation is that it would be unaffected if your calibration
 * image (typically of a grey card) was not quite properly framed, or
 * contained a little specular reflection, but it also has the advantage
 * that (unlike the mean) it gives the same result no matter whether you
 * apply it to gamma-corrected values and transform the result back
 * to the original scale, or just apply it to the original values and
 * ignore gamma-correction.
 * <br>
 * This program will also apply gamma-correction, just before the
 * final scaling step. I do this just because
 * it is easy and saves you one step going in and out of JPEG. I
 * don't attempt to work out a gamma correction or even advise this
 * particularly - it just seemed too easy to leave out.
 */
public class MyRoughCast
{
  /** This class reads in images, keeping the size of the image
   *  and frequency counts of RGB values.
   */
  //~ private static class Counter implements ImageConsumer
  //~ {
    //~ /** slot x holds the number of pixels seen with value x for red */
    //~ private int[] redCounts;
    //~ /** slot x holds the number of pixels seen with value x for green */
    //~ private int[] greenCounts;
    //~ /** slot x holds the number of pixels seen with value x for blue */
    //~ private int[] blueCounts;
    //~ /** The width of the image */
    //~ private int imageWidth;
    //~ /** The height of the image */
    //~ private int imageHeight;
    //~ /** The status flag returned by the ImageProducer */
    //~ private int imageStatus = -1;
    //~ /** ImageProducer to use to get the image */
    //~ private ImageProducer producer;
    //~ /** Image to look at */
    //~ private Image image;
    //~ /** Set true to provide more info */
    //~ private boolean verbose;
    //~ /** release resources */
    //~ public void dispose()
    //~ {
     //~ synchronized(lock)
     //~ {
       //~ if (producer != null)
       //~ {
         //~ producer = null;
	 //~ image.flush();
	 //~ image = null;
       //~ }
     //~ }
    //~ }
    //~ /** sets verbose flag to get more info printed out */
    //~ public void setVerbose(boolean x)
    //~ {
      //~ synchronized(lock)
      //~ {
	//~ verbose = x;
      //~ }
    //~ }
    //~ /** return the status flag from the ImageProducer */
    //~ public int getStatus()
    //~ {
      //~ synchronized(lock)
      //~ {
	//~ return imageStatus;
      //~ }
    //~ }
    //~ /** return the width of the image */
    //~ public int getWidth()
    //~ {
      //~ synchronized(lock)
      //~ {
	//~ return imageWidth;
      //~ }
    //~ }
    //~ /** return the height of the image */
    //~ public int getHeight()
    //~ {
      //~ synchronized(lock)
      //~ {
	//~ return imageHeight;
      //~ }
    //~ }
    //~ /** return a copy of the array of counts of red pixels. Slot x
     //~ *  holds the number of such pixels with value x.
     //~ */
    //~ public int[] getRedCount()
    //~ {
      //~ synchronized(lock)
      //~ {
	//~ return (int[])redCounts.clone();
      //~ }
    //~ }
    //~ /** return a copy of the array of counts of green pixels. Slot x
     //~ *  holds the number of such pixels with value x.
     //~ */
    //~ public int[] getGreenCount()
    //~ {
      //~ synchronized(lock)
      //~ {
	//~ return (int[])greenCounts.clone();
      //~ }
    //~ }
    //~ /** return a copy of the array of counts of blue pixels. Slot x
     //~ *  holds the number of such pixels with value x.
     //~ */
    //~ public int[] getBlueCount()
    //~ {
      //~ synchronized(lock)
      //~ {
	//~ return (int[])blueCounts.clone();
      //~ }
    //~ }
    //~ /**
     //~ * return the image
     //~ */
    //~ public Image getImage()
    //~ {
      //~ synchronized(lock)
      //~ {
	//~ return image;
      //~ }
    //~ }
    //~ /**
     //~ * Used for synchronization (don't use this for synchronization
     //~ * in case the ImageProducer does)
     //~ */
    //~ private Object lock = new Object();
    //~ /**
     //~ * Set true when imageComplete() has been called, which may
     //~ * be when the first frame is seen, or when an error is
     //~ * detected.
     //~ */
    //~ private boolean complete = false;
    //~ /**
     //~ * Loads in an image and makes frequency counts.
     //~ */
    //~ public void countImage(String name) throws InterruptedException
    //~ {
      //~ synchronized(lock)
      //~ {
	//~ imageWidth = imageHeight = 0;
	//~ redCounts = new int[256];
	//~ greenCounts = new int[256];
	//~ blueCounts = new int[256];
	//~ Toolkit toolkit = Toolkit.getDefaultToolkit();
	//~ image = toolkit.createImage(name);
	//~ producer = image.getSource();
	//~ producer.startProduction(this);
	//~ while (!complete)
	//~ {
	  //~ lock.wait();
	//~ }
      //~ }
    //~ }
    //~ /**
     //~ * Called back by ImageProducer to receive image dimensions
     //~ */
    //~ public void setDimensions(int width, int height)
    //~ {
      //~ synchronized(lock)
      //~ {
	//~ imageWidth = width;
	//~ imageHeight = height;
      //~ }
    //~ }
    //~ /**
     //~ * Called back by ImageProducer to receive image properties
     //~ */
    //~ public void setProperties(Hashtable ht)
    //~ {
      //~ if (verbose)
      //~ {
	//~ System.out.println("Properties " + ht);
      //~ }
    //~ }
    //~ /**
     //~ * Called back by ImageProducer to receive color model
     //~ */
    //~ public void setColorModel(ColorModel model)
    //~ {
    //~ }
    //~ /**
     //~ * Called back by ImageProducer to receive hints.
     //~ */
    //~ public void setHints(int hintflags)
    //~ {
      //~ if (verbose && 
        //~ ((hintflags & ImageConsumer.SINGLEPASS) != 0))
          
      //~ {
	//~ // If here, the ImageProducer is giving us sucessive
	//~ // views of the same image to build it up in greater
	//~ // and greater resolution. If JPEG is doing its job
	//~ // the colours of the lower resolution versions should
	//~ // be reasonably accurate so this won't be too bad, but
	//~ // it isn't the best way to estimate the median pixel
	//~ // values.
	//~ System.out.println(
	  //~ "Not single pass image: results will be inaccurate");
      //~ }
    //~ }
    //~ /**
     //~ * Called back by ImageProducer to recieve pixel values
     //~ */
    //~ public void setPixels(int x, int y, int w, int h, ColorModel cm,
      //~ byte[] pixels, int off, int scansize)
    //~ {
      //~ synchronized(lock)
      //~ {
	//~ for (int i = 0; i < h; i++)
	//~ {
	  //~ for (int j = 0; j < w; j++)
	  //~ {
	    //~ int pix = pixels[j + i * scansize + off] & 0xff;
	    //~ redCounts[cm.getRed(pix)]++;
	    //~ greenCounts[cm.getGreen(pix)]++;
	    //~ blueCounts[cm.getBlue(pix)]++;
	  //~ }
	//~ }
      //~ }
    //~ }
    //~ /**
     //~ * Called back by ImageProducer to recieve pixel values
     //~ */
    //~ public void setPixels(int x, int y, int w, int h, ColorModel cm,
      //~ int[] pixels, int off, int scansize)
    //~ {
      //~ synchronized(lock)
      //~ {
	//~ for (int i = 0; i < h; i++)
	//~ {
	  //~ for (int j = 0; j < w; j++)
	  //~ {
	    //~ int pix = pixels[j + i * scansize + off];
	    //~ redCounts[cm.getRed(pix)]++;
	    //~ greenCounts[cm.getGreen(pix)]++;
	    //~ blueCounts[cm.getBlue(pix)]++;
	  //~ }
	//~ }
      //~ }
    //~ }
    //~ /**
     //~ * Called back by ImageProducer to receive notification when
     //~ * the first frame has been seen or on error
     //~ */
    //~ public void imageComplete(int x)
    //~ {
      //~ if (verbose)
      //~ {
	//~ System.out.println("Complete: " + x);
      //~ }
      //~ producer.removeConsumer(this);
      //~ synchronized(lock)
      //~ {
	//~ complete = true;
	//~ imageStatus = x;
	//~ lock.notifyAll();
      //~ }
    //~ }
  //~ }
  /**
   * This class converts the image, writing to a BufferedImage.
   */
  private static class Converter implements ImageConsumer
  {
    /** Three 256-entry lookup tables for red, green, and blue */
    private byte[][] lookup;
    /** BufferedImage to write converted results to */
    private BufferedImage ourTarget;
    /** Used for synchronization. Don't use this in case ImageProducer
     *  does.
     */
    private Object lock = new Object();
    /** Set to true when ImageProducer says complete */
    private boolean complete = false;
    /** ImageProducer with the data */
    private ImageProducer producer;
    /** status provided by ImageProducer */
    private int imageStatus;
    /** Set to true to get more info printed out */
    private boolean verbose;
    /** set verbose flag */
    public void setVerbose(boolean x)
    {
      synchronized(lock)
      {
	verbose = x;
      }
    }
    /** return status according to ImageProducer */
    public int getStatus()
    {
      synchronized(lock)
      {
	return imageStatus;
      }
    }
    /** Call this to process an image, scaling the values according
     *  to the lookup table in scale
     */
    public void process(Image source, BufferedImage target,
      byte[][] scale)
      throws InterruptedException
    {
      synchronized(lock)
      {
	// Make our own copy of the lookup tables
	lookup = new byte[3][];
	for (int i = 0; i < 3; i++)
	{
	  lookup[i] = new byte[256];
	  for (int j = 0; j < lookup[i].length; j++)
	  {
	    lookup[i][j] = scale[i][j];
	  }
	}
	ourTarget = target;
	producer = source.getSource();
	// Ask ImageProducer to start sending us image values
	producer.startProduction(this);
	while (!complete)
	{
	  lock.wait();
	}
      }
    }
    /** Receive dimension callback from ImageProducer. We can
     *  ignore it because our caller is supposed to have given
     *  us the correct dimensions
     */
    public void setDimensions(int width, int height)
    {
    }
    /** Receive properties callback from ImageProducer. */
    public void setProperties(Hashtable ht)
    {
    }
    /** Receive color model callback from ImageProducer */
    public void setColorModel(ColorModel model)
    {
    }
    /** Receive hints callback from ImageProducer */
    public void setHints(int hintflags)
    {
    }
    /** 
     * Recieve data callback from ImageProducer. Pass data through
     *  lookup table and write to BufferedImage.
     */
    public void setPixels(int x, int y, int w, int h, ColorModel cm,
      byte[] pixels, int off, int scansize)
    {
      synchronized(lock)
      {
	for (int i = 0; i < h; i++)
	{
	  for (int j = 0; j < w; j++)
	  {
	    int pixelValue = pixels[j + i * scansize + off];
	    int red = lookup[0][cm.getRed(pixelValue)] & 255;
	    int green = lookup[1][cm.getGreen(pixelValue)] & 255;
	    int blue = lookup[2][cm.getBlue(pixelValue)] & 255;
	    int value = (red << 16) + (green << 8) + blue;
	    ourTarget.setRGB(x + j, y + i, value);
	  }
	}
      }
    }
    /** 
     * Recieve data callback from ImageProducer. Pass data through
     *  lookup table and write to BufferedImage.
     */
    public void setPixels(int x, int y, int w, int h, ColorModel cm,
      int[] pixels, int off, int scansize)
    {
      synchronized(lock)
      {
	for (int i = 0; i < h; i++)
	{
	  for (int j = 0; j < w; j++)
	  {
	    int pixelValue = pixels[j + i * scansize + off];
	    int red = lookup[0][cm.getRed(pixelValue)] & 255;
	    int green = lookup[1][cm.getGreen(pixelValue)] & 255;
	    int blue = lookup[2][cm.getBlue(pixelValue)] & 255;
	    int value = (red << 16) + (green << 8) + blue;
	    ourTarget.setRGB(x + j, y + i, value);
	  }
	}
      }
    }
    /** Receive completion callback from ImageProducer */
    public void imageComplete(int x)
    {
      if (verbose)
      {
	System.out.println("Complete: " + x);
      }
      producer.removeConsumer(this);
      synchronized(lock)
      {
	complete = true;
	imageStatus = x;
	lock.notifyAll();
      }
    }
  }
  /** Work out median from frequency counts */
  public static double getMedian(int[] counts)
  {
    // First work out the total number of samples
    int sum = 0;
    for (int i = 0; i < counts.length; i++)
    {
      sum += counts[i];
    }
    // A value is a median if <= half of the values
    // lie to its left and also <= half of the values
    // lie to its right. We may have many such values. For instance
    // in the counts {1, 0, 0, 1} ALL of the data points are medians.
    // In such a case we take the middle value of the range of
    // medians.
    int toLeft = 0;
    int toRight = sum;
    // This truncation is safe even if there are an odd number of
    // values - there will still be a median: e.g. in {1, 0, 0, 1, 0, 1}
    // the central 1 is a median.
    int half = sum / 2;
    int firstMedian = -1;
    int lastMedian = -1;
    for (int i = 0; i < counts.length; i++)
    {
      toRight -= counts[i];
      if ((toLeft <= half) && (toRight <= half))
      {
        if (firstMedian < 0)
	{
	  firstMedian = i;
	}
	lastMedian = i;
      }
      toLeft += counts[i];
    }
    return (firstMedian + lastMedian) * 0.5;
  }
  /** This reads in an image and returns the median values of RGB<
   *  or null, on error.
   */
  //~ public static double[] analyse_(String imageName, boolean verbose)
    //~ throws InterruptedException
  //~ {
    //~ Counter cwb = new Counter();
    //~ cwb.setVerbose(verbose);
    //~ cwb.countImage(imageName);
    //~ cwb.dispose();
    //~ if ((cwb.getWidth() <= 0) ||
        //~ (cwb.getHeight() <= 0))
    //~ { // trouble reading in data
      //~ return null;
    //~ }
    //~ int status = cwb.getStatus();
    //~ int[] redCount = cwb.getRedCount();
    //~ int[] greenCount = cwb.getGreenCount();
    //~ int[] blueCount = cwb.getBlueCount();
    //~ if (verbose)
    //~ {
      //~ if ((status & (ImageConsumer.IMAGEABORTED |
		       //~ ImageConsumer.IMAGEERROR)) != 0)
      //~ {
	//~ System.out.println("Trouble loading image " + imageName +
	  //~ " for analysis");
	//~ System.out.println("CWB status is " + status);
      //~ }
      //~ System.out.println("File is " + cwb.getWidth() + " by " +
	//~ cwb.getHeight());
      //~ System.out.println("Red, Green, and Blue counts");
      //~ for (int i = 0; i < 256; i++)
      //~ {
	//~ System.out.println(i + ": " + redCount[i] + ", " +
	  //~ greenCount[i] + ", " + blueCount[i]);
      //~ }
    //~ }
    //~ return new double[] {getMedian(redCount), getMedian(greenCount),
      //~ getMedian(blueCount)};
  //~ }
  
  /** Return the largest pixel value seen */
  private static int getMax(int[] counts)
  {
    for (int i = counts.length - 1; i >= 0; i--)
    {
      if (counts[i] != 0)
      {
        return i;
      }
    }
    return 0;
  }

  //~ /**
   //~ *  Convert a file according to the arguments here. Don't overwrite -
   //~ *  it's just too easy to slip up and destroy an image with this
   //~ *  command-line interface otherwise.
   //~ */
  //~ public static boolean HIDDEN__convertFile(String inFile, String outFile,
    //~ double[] median, boolean verbose, double quality, double gamma)
    //~ throws IOException, InterruptedException
  //~ {
    //~ if (verbose)
    //~ {
      //~ System.out.println("Will convert " + inFile + " to " +
	//~ outFile);
    //~ }
    //~ if (new File(outFile).exists())
    //~ {
      //~ System.err.println("Stopping conversion as output file " +
        //~ outFile + " exists");
      //~ return false;
    //~ }
    //~ // First time we fetch the image we want to know its size
    //~ // and the maximum values of each colour to scale the result
    //~ // of conversion
    //~ OKRoughCast.Counter cwb = new OKRoughCast.Counter();
    //~ cwb.setVerbose(verbose);
    //~ boolean imageAnalysisOK = cwb.countImage(inFile);
    //~ int width = cwb.getWidth();
    //~ int height = cwb.getHeight();
    //~ if (verbose)
    //~ {
      //~ if ( !imageAnalysisOK )
      //~ {
		//~ System.out.println("Trouble analysing image " + inFile);
		//~ return false;
      //~ }
      //~ System.out.println("File is " + width + " by " + height);
    //~ }
    //~ BufferedImage im = cwb.getImage();
    //~ int[][] counts = new int[3][];
    //~ counts[0] = cwb.getRedCount();
    //~ counts[1] = cwb.getGreenCount();
    //~ counts[2] = cwb.getBlueCount();
	//~ byte[][] tables = OKRoughCast.Converter.create_lookup_tables(counts, gamma, median);
	
    //~ // Now we can convert the image
    //~ OKRoughCast.Converter c = new OKRoughCast.Converter();
    //~ c.setVerbose(verbose);
	//~ BufferedImage bi = c.process(im, tables);
//~ /*//OK_TEMP
//~ java.awt.image.AffineTransformOp aop = new java.awt.image.AffineTransformOp(new java.awt.geom.AffineTransform(), java.awt.image.AffineTransformOp.TYPE_BILINEAR);
//~ BufferedImage bi = new BufferedImage(im.getWidth(), im.getHeight(), im.getType());
//~ ColorModel cm1 = im.getColorModel(), cm2 = bi.getColorModel();
//~ if ( !cm1.equals(cm2) )
	//~ System.out.println("Src cm: "+cm1 + " Dst cm: " +cm2);
//~ bi = aop.filter(im, bi);
//~ */
    //~ if ( bi == null )
    //~ {
		//~ if ( verbose )
			//~ System.out.println("Trouble converting image " + inFile);
		//~ return  false;
    //~ }
	//~ return	OKUtils.ImageFileUtil.saveImageAsJPEG(bi, 1.0f, outFile);
  //~ }


  
  /**
   * Creates lookup tables doing both color balancing and gamma correction.
   * @param counts 3 per-band arrays of pixel value frequences.
   */
  protected static byte[][] HIDDEN__create_lookup_tables(int[][] counts, double gamma,
													double[] median)
  {
    final double ZERO_SUBSTITUTE = 0.5;
    // Want to divide by respective medians, add optional
    // gamma correction, and then scale
    // result so that the maximum pixel value produced is 255
    double max = 0.0;
    for (int i = 0; i < counts.length; i++)
    {
      double v = median[i];
      if (v <= 0.0)
      { // Shouldn't be here, but if we are avoid division by zero
        v = ZERO_SUBSTITUTE;
      }
      double x = getMax(counts[i]) / v;
      if (gamma != 1.0)
      {
        x = Math.pow(x, gamma);
      }
      // x is what the corrected value would be without scaling to
      // fit everything into the range 0..255
      if (x > max)
      {
        max = x;
      }
    }
    // Now create lookup tables for conversion
    byte[][] tables = new byte[3][];
    for (int i = 0; i < tables.length; i++)
    {
      double v = median[i];
      if (v <= 0.0)
      {
        v = ZERO_SUBSTITUTE;
      }
      byte[] lookup = new byte[256];
      tables[i] = lookup;
      for (int j = 0; j < lookup.length; j++)
      {
		double x = j / v;
		if (gamma != 1.0)
		{
			x = Math.pow(x, gamma);
		}
		// Apply scaling to fit in range 0..255
		lookup[j] = (byte)Math.round(x * 255.0 / max);
      }
    }
	return  tables;
  }


  /** Read medians from string such as 132:100:150 */
  private static double[] readMedians(String s)
  {
    StringTokenizer st = new StringTokenizer(s, " \t:");
    double[] medians = new double[3];
    for (int i = 0; i < medians.length; i++)
    {
      if (!st.hasMoreTokens())
      {
        System.err.println("Ran out of medians in " + s);
		return null;
      }
      String num = st.nextToken();
      try
      {
        medians[i] = Double.parseDouble(num.trim());
      }
      catch (NumberFormatException nfe)
      {
        System.err.println("Could not read number in " + num);
	return null;
      }
    }
    return medians;
  }

  private static String mediansToString(double[] median)
  {
	String sep = ":";
	String result = new String();
	for (int j = 0; j < median.length; j++)
	{
		result += median[j];
		if ( j < median.length-1 )
			result += sep;
	}
	return	result;
  }
  
  /** Main program. Reads args to know what to do as it goes
   *  along, so you can convert multiple files with different
   *  conversions in one run
   */
  public static void main(String[] s) throws Exception
  {
    // current median value to convert from
    double[] median = null;
    boolean scaleEachChannel = false; // scale source-image colors independently
    // Used in parsing arguments
    int sl1 = s.length - 1;
    int sl2 = s.length - 2;
    // set on error
    boolean trouble = false;
    // default quality is JPEG normal
    double quality = 0.75;
    // default gamma conversion is none
    double gamma = 1.0;
    int xScan=0, yScan=0;	// where to scan the [sub]image
    boolean scanSubImage = false;
    // set true to get more output to standard output
    boolean verbose = false;
    for (int i = 0; i < s.length; i++)
    {
      // Scan a file to work out median pixel values
      if ((i < sl1) && "-scan".equals(s[i]))
      {
        i++;
        String filename = s[i];
        if (((i+1) < sl2) && "-scanAtCoord".equals(s[i+1]))
        {
          i++;  // s[i]=="-scanAtCoord"  s[i+1]==X  s[i+2]==Y
          try
          {
            xScan = Integer.parseInt(s[++i].trim());
            yScan = Integer.parseInt(s[++i].trim());
            scanSubImage = true;
          }
          catch (NumberFormatException nfe)
          {
            System.err.println("Cannot read coord value in " + s[i]);
            trouble = true;
            break;
          }
          if (verbose)
          {
            System.out.println("Scanning image at {"+xScan+","+yScan+"}");
          }
        }
        else
          scanSubImage = false;
        if ( !scanSubImage )
          median = OKRoughCast.analyze_image_colors(filename, verbose);
        else
          median = OKRoughCast.analyze_image_colors(filename,
                              xScan, yScan, verbose);
        if (median == null)
        {
          System.err.println("Could not get data from file " + filename);
          trouble = true;
          break;
        }
        double[] invMedian = OKRoughCast.get_inverse_relation_colors(median);
        System.out.print("Medians of " + filename + 
                ((scanSubImage)? " at "+xScan+","+yScan : ""));
        System.out.print(": " + mediansToString(median));
        System.out.println("  Inversed: " + mediansToString(invMedian));
      }
      else if ((i < sl2) && (median != null) && "-convert".equals(s[i]))
      {
        String inFile = s[++i];
        String outFile = s[++i];
        if (!OKRoughCast.convertFile(inFile, outFile, median, scaleEachChannel,
                    quality, gamma, verbose))
        {
          trouble = true;
          break;
        }
      }
      else if ((i < sl1) && "-fixFrom".equals(s[i]))
      {
        i++;
        median = readMedians(s[i]);
        if (median == null)
        {
          trouble = true;
          break;
        }
      }
      else if ((i < s.length) && "-scaleEachChannel".equals(s[i]))
      {
        // scale source-image colors independently to match the target measurings
        scaleEachChannel = true;
      }
      else if ((i < sl1) && "-gamma".equals(s[i]))
      {
        i++;
        try
        {
          gamma = Double.parseDouble(s[i].trim());
        }
        catch (NumberFormatException nfe)
        {
          System.err.println("Cannot read gamma value in " + s[i]);
          trouble = true;
          break;
        }
        if (verbose)
        {
          System.out.println("Set gamma to " + gamma);
        }
      }
      else if ((i < sl1) && "-quality".equals(s[i]))
      {
            i++;
        try
        {
          quality = Double.parseDouble(s[i].trim());
        }
        catch (NumberFormatException nfe)
        {
          System.err.println("Cannot read quality value in " + s[i]);
          trouble = true;
          break;
        }
        if (verbose)
        {
          System.out.println("Set quality to " + quality);
        }
      }
      else if ("-v".equals(s[i]))
      {
        verbose = true;
      }
      else if ("-nv".equals(s[i]))
      {
        verbose = false;
      }
      else
      {
        System.err.println("Could not understand flag " + s[i]);
        trouble = true;
        break;
      }
    }
    if (trouble)
    {
		System.err.println(
			"Args are [-scan <file>]* [-convert <in> <out>]* " +
			"[-fixFrom #:#:#] [-scaleEachChannel] [-gamma #] [-nv] [-quality #] [-v]");
		System.exit(1);
    }
    System.exit(0);
  }
}
