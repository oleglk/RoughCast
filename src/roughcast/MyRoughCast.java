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
  

  /** Read integer metrics from string such as 132:100:150 */
  private static double[] readMetrics(String s)
  {
    StringTokenizer st = new StringTokenizer(s, " \t:");
    double[] metrics = new double[3];
    for (int i = 0; i < metrics.length; i++)
    {
      if (!st.hasMoreTokens())
      {
        System.err.println("Ran out of metrics in " + s);
        return null;
      }
      String num = st.nextToken();
      try
      {
        metrics[i] = Double.parseDouble(num.trim());
      }
      catch (NumberFormatException nfe)
      {
        System.err.println("Could not read number in " + num);
        return null;
      }
    }
    return metrics;
  }

  
  /** Format integer metrics into string such as 132:100:150 */
  private static String metricsToString(double[] metric)
  {
    String sep = ":";
    String result = new String();
    for (int j = 0; j < metric.length; j++)
    {
      result += metric[j];
      if ( j < metric.length-1 )
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
    // current metrics values to convert from
    double[] metrics = null;
    boolean scaleEachChannel = false; // scale source-image colors independently
    OKRoughCast.SetMetricsType(MetricsType.MEDIAN); // the default
    // Used in parsing arguments
    int sl1 = s.length-1,  sl2 = s.length-2,  sl3 = s.length-3;
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
      // Scan a file to work out metrics pixel values
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
          metrics = OKRoughCast.analyze_image_colors(filename, verbose);
        else
          metrics = OKRoughCast.analyze_image_colors(filename,
                              xScan, yScan, verbose);
        if (metrics == null)
        {
          System.err.println("Could not get data from file " + filename);
          trouble = true;
          break;
        }
        double[] invMetrics = OKRoughCast.get_inverse_relation_colors(metrics);
        System.out.print("Metrics of " + filename + 
                ((scanSubImage)? " at "+xScan+","+yScan : ""));
        System.out.print(": " + metricsToString(metrics));
        System.out.println("  Inversed: " + metricsToString(invMetrics));
      }
      else if ((i <= sl3) && (metrics != null) && "-convert".equals(s[i]))
      {
        // "-convert" must be the last argument - it triggers the processing
        if (i < sl3)
        {
          System.err.println("'-convert' must be the last argument");
          trouble = true;
          break;
        }
        String inFile = s[++i];
        String outFile = s[++i];
        if (!OKRoughCast.convertFile(inFile, outFile, metrics, scaleEachChannel,
                    quality, gamma, verbose))
        {
          trouble = true;
          break;
        }
      }
      else if ((i < sl1) && "-fixFrom".equals(s[i]))
      {
        i++;
        metrics = readMetrics(s[i]);
        if (metrics == null)
        {
          trouble = true;
          break;
        }
      }
      else if ((i < s.length) && "-scaleEachChannel".equals(s[i]))
      {
        // scale source-image colors independently to match the target measurings
        scaleEachChannel = true;
        OKRoughCast.SetMetricsType(MetricsType.AVERAGE); 
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
