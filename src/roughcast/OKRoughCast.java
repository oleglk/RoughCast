/*
 * OKRoughCast.java
 *
 * Created on August 20, 2004, 11:03 AM
 */
package RoughCast;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;

import javax.imageio.*;

import OKUtils.CmdLineHandler;
import OKUtils.ImageFileUtil;

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
public class OKRoughCast
{
  /** width of subimage to scan */
  private static final int SAMPLE_WIDTH = 4;
  /** height of subimage to scan */
  private static final int SAMPLE_HEIGHT = 4;

  /** Creates a new instance of OKRoughCast */
  public OKRoughCast()
  {
  }

  /*************** Main API functions ******************************/
  /** This reads in an image and returns the median values of RGB<
   *  or null, on error.
   */
  public static double[] analyze_image_colors(String imageName, boolean verbose)
  {
	  return  Counter.analyse(imageName, verbose);
  }
  
  /** This reads in an image and returns the median values of RGB
   *  of subimage around {x, y}
   *  or null, on error.
   */
  public static double[] analyze_image_colors(String imageName,
												int x, int y,
												boolean verbose)
  {
	  BufferedImage im = Counter.loadImage(imageName);
	  return  Counter.analyse_subImage(im,
										x-SAMPLE_WIDTH/2, y-SAMPLE_HEIGHT/2,
										SAMPLE_WIDTH, SAMPLE_HEIGHT, verbose);
  }

  /** @return the median values of RGB or null, on error.
   */
  public static double[] analyze_image_colors(BufferedImage img, boolean verbose)
  {
	  return  Counter.analyse(img, verbose);
  }
  
  /** @return the median values of RGB of subimage around {x, y}
   *  or null, on error.
   */
  public static double[] analyze_image_colors(BufferedImage img,
												int x, int y,
												boolean verbose)
  {
	  return  Counter.analyse_subImage(img,
										x-SAMPLE_WIDTH/2, y-SAMPLE_HEIGHT/2,
										SAMPLE_WIDTH, SAMPLE_HEIGHT, verbose);
  }

  public static double[] get_inverse_relation_colors(double[] rgb)
  {
	  double r1=rgb[0], g1=rgb[1], b1=rgb[2];	//orig colors
	  double r2, g2, b2;	// inverse-relation colors
	  if ( r1 == 0 )  r1 = 0.1;
	  if ( g1 == 0 )  g1 = 0.1;
	  if ( b1 == 0 )  b1 = 0.1;
	  r2 = 100.0;
	  g2 = r1*r2/g1;
	  b2 = r1*r2/b1;
	  return	new double[] {r2, g2, b2};
  }

  /**
   *  Convert a file according to the arguments here. Don't overwrite -
   *  it's just too easy to slip up and destroy an image with this
   *  command-line interface otherwise.
   */
  public static boolean convertFile(String inFileName, String outFileName,
    double[] median, double quality, double gamma, boolean verbose)
    throws IOException, InterruptedException
  {
    if (verbose)
    {
      System.out.println("Will convert " + inFileName + " to " + outFileName);
    }
    if (new File(outFileName).exists())
    {
      System.out.println("Stopping conversion as output file " +
						outFileName + " exists");
      return false;
    }
	File inpFile = new File(inFileName);
    if ( !inpFile.exists() )
    {
      System.out.println("Stopping conversion as input file " +
						inFileName + " doesn't exist");
      return false;
    }
	BufferedImage im = OKUtils.ImageFileUtil.readAsBufferedImage(inpFile);
	BufferedImage bi = convertImage(im, median, gamma, verbose);
    if ( bi == null )
    {
		if ( verbose )
			System.out.println("Trouble converting image " + inFileName);
		return  false;
    }
	return	OKUtils.ImageFileUtil.saveImageAsJPEG(bi, 1.0f, outFileName);
  }

  /**
   *  Convert an image according to the arguments here.
   *  @return the new converted image or null on failure.
   */
  public static BufferedImage convertImage(BufferedImage im,
		double[] median, double gamma, boolean verbose)
  {
    if ( verbose && (im == null) )
    {	System.out.println("* convertImage(null) !");
		return	null;
    }
    // First time we fetch the image we want to know its size
    // and the maximum values of each colour to scale the result
    // of conversion
    Counter cwb = new OKRoughCast.Counter();
    cwb.setVerbose(verbose);
    boolean imageAnalysisOK = cwb.countImage(im);
    int width = im.getWidth();
    int height = im.getHeight();
    if ( verbose )
    {
		if ( !imageAnalysisOK )
		{	System.out.println("* convertImage(): Trouble analysing image!");
			return null;
		}
		System.out.println("Image is " + width + " by " + height);
    }
    int[][] counts = new int[3][];
    counts[0] = cwb.getRedCount();
    counts[1] = cwb.getGreenCount();
    counts[2] = cwb.getBlueCount();
	byte[][] tables = Converter.create_lookup_tables(counts, gamma, median);
	
    // Now we can convert the image
    Converter c = new Converter();
    c.setVerbose(verbose);
	BufferedImage bi = c.process(im, tables);
    if ( bi == null )
    {
		if ( verbose )
			System.out.println("* convertImage(): Trouble converting image!");
		return  null;
    }
	return	bi;
  }



  /** This class reads in images, keeping the size of the image
   *  and frequency counts of RGB values.
   */
  public static class Counter
  {
	private final static int MAX_BLOCK_SIZE = 1024 * 1024;	// =1Mpix
    /** slot x holds the number of pixels seen with value x for red */
    private int[] redCounts;
    /** slot x holds the number of pixels seen with value x for green */
    private int[] greenCounts;
    /** slot x holds the number of pixels seen with value x for blue */
    private int[] blueCounts;
    /** The width of the image */
    private int imageWidth;
    /** The height of the image */
    private int imageHeight;
    /** Image to look at */
    private BufferedImage inpImage = null;
    /** Set true to provide more info */
    private boolean verbose;
	
	public Counter()
	{
		inpImage = null;
		redCounts = null;		greenCounts = null;		blueCounts	= null;
	}

	/** Reads in an image and returns the median values of RGB<
	 *  or null, on error.
	 */
	protected static double[] analyse(String imageName, boolean verbose)
	{
		BufferedImage im = loadImage(imageName);
		if ( im == null )
		{	// trouble reading in data
			System.out.println("Trouble loading image " + imageName);
			return null;
		}
		return	analyse(im, verbose);
	}



	/** @return the median values of RGB or null on error.
	 */
	protected static double[] analyse_subImage(BufferedImage im,
								int x, int y, int w, int h, boolean verbose)
	{
		BufferedImage subIm = null;
		if ( im == null )
		{	if ( verbose )
				System.out.println("* analyse_subImage(null)!");
			return null;
		}
		if ( (x < im.getMinX()) || ((x+w) > (im.getMinX()+im.getWidth())) ||
			 (y < im.getMinY()) || ((y+h) > (im.getMinY()+im.getHeight())) )
		{	if ( verbose )
				System.out.println("* analyse_subImage(image, x,y,w,h): " +
								"out of boundaries!");
			return null;
		}
		try
		{
			subIm = im.getSubimage(x, y, w, h);
		} catch (RasterFormatException e)
		{	if ( verbose )
				System.out.println("* analyse_subImage(image, x,y,w,h): " +
								"out of boundaries!");
			return null;
		}
		return	analyse(subIm, verbose);
	}

	/** @return the median values of RGB or null on error.
	 */
	protected static double[] analyse(BufferedImage im, boolean verbose)
	{
		Counter cwb = new Counter();
		cwb.setVerbose(verbose);
		if ( !cwb.countImage(im) )
		{
			System.out.println("* analyse(image): Trouble analyzing image!");
			return null;
		}
		int[] redCount		= cwb.getRedCount();
		int[] greenCount	= cwb.getGreenCount();
	    int[] blueCount		= cwb.getBlueCount();
		if ( verbose )
		{
			System.out.println("Image is " + cwb.getWidth() + " by " +
								cwb.getHeight());
			System.out.println("Red, Green, and Blue counts");
			for (int i = 0; i < 256; i++)
			{
				System.out.println(i + ": " + redCount[i] + ", " +
									greenCount[i] + ", " + blueCount[i]);
			}
		}
		return new double[] {	getMedian(redCount), getMedian(greenCount),
								getMedian(blueCount)};
	}

	/** Work out median from frequency counts */
	protected static double getMedian(int[] counts)
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
				{	firstMedian = i;
				}
				lastMedian = i;
			}
			toLeft += counts[i];
		}
		return (firstMedian + lastMedian) * 0.5;
	}

    /** sets verbose flag to get more info printed out */
    public void setVerbose(boolean x)
    {
		verbose = x;
    }

    /** return the width of the image */
    public int getWidth()
    {
		return (inpImage != null)? inpImage.getWidth() : -1;
    }
    /** return the height of the image */
    public int getHeight()
    {
		return (inpImage != null)? inpImage.getHeight() : -1;
    }
    /** return a copy of the array of counts of red pixels. Slot x
     *  holds the number of such pixels with value x.
     */
    public int[] getRedCount()
    {
		return (int[])redCounts.clone();
    }
    /** return a copy of the array of counts of green pixels. Slot x
     *  holds the number of such pixels with value x.
     */
    public int[] getGreenCount()
    {
		return (int[])greenCounts.clone();
    }
    /** return a copy of the array of counts of blue pixels. Slot x
     *  holds the number of such pixels with value x.
     */
    public int[] getBlueCount()
    {
		return (int[])blueCounts.clone();
    }
    /**
     * return the image
     */
    public BufferedImage getImage()
    {
		return inpImage;
    }
	
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

	protected boolean image_load_failed()
	{
		return  (inpImage == null);
	}
	
    /**
     * Loads in an image and makes frequency counts.
     */
    protected boolean countImage(String name)
    {
		if ( (inpImage =loadImage(name)) != null )
			return  countImage(inpImage);
		else
			return  false;
    }
	
    /**
     * Loads in an image.
	 * @return the loaded image on success, null on failure
     */
    protected static BufferedImage loadImage(String name)
    {
		File imgFile = new File(name);
		// read the image file
		BufferedImage im = ImageFileUtil.readAsBufferedImage(imgFile);
		if ( im == null )
			return  null;
		System.out.println("Source image " + im.getWidth() + "*"
						+ im.getHeight() + "\tread from   "
						+ imgFile.getAbsolutePath());
		return  im;
    }
	
    /**
     * Makes frequency counts on image 'im'.
     */
    protected boolean countImage(BufferedImage im)
    {
	    if ( im == null )
			return	false;
		inpImage = im;
		// obtain and analyze the pixel data in chunks
		redCounts	= new int[256];
		greenCounts = new int[256];
		blueCounts	= new int[256];
		int imgWidth = inpImage.getWidth();
		int maxChunkStep = detect_max_width_of_horz_chunk(inpImage,
														  MAX_BLOCK_SIZE);
		boolean cntResult = true;
		for ( int chunkLeftBound = 0; chunkLeftBound < imgWidth;
				chunkLeftBound += maxChunkStep )
		{
			int chunkRightBound = chunkLeftBound + maxChunkStep - 1;
			if ( chunkRightBound > (imgWidth-1) )
				chunkRightBound = imgWidth - 1;
			int pixels[] = ImageDataReader.getImagePixels(inpImage,
										chunkLeftBound, 0,
										chunkRightBound, inpImage.getHeight()-1);
			if ( pixels == null )
				return  false;
			cntResult &= countPixels(chunkRightBound-chunkLeftBound+1,
									 inpImage.getHeight(),
									 inpImage.getColorModel(), pixels);
		}
		return  cntResult;
    }

	/**
	 * @return size of horizontal part to split image into blocks,
	 *	so that each block is no more than maxBlockSize. This size holds
	 *	for all but the last chunk
	 **/
	protected static int detect_max_width_of_horz_chunk(BufferedImage bimage,
														int maxBlockSize)
	{
		int imgWidth = bimage.getWidth();
		int nChunks = detect_numer_of_horz_chunks(bimage, maxBlockSize);
		// chunk width is maxChunkStep for all but the last chunk
		int maxChunkStep = (int)Math.ceil((double)imgWidth / nChunks);
		return  maxChunkStep;
	}

	/**
	 * @return numer of horizontal parts to split image into,
	 * so that each block is no more than maxBlockSize
	 **/
	protected static int detect_numer_of_horz_chunks(BufferedImage bimage,
													int maxBlockSize)
	{
		int imgWidth = bimage.getWidth();
		int imgHeight = bimage.getHeight();
		double totalPixels = imgWidth * imgHeight;
		int nChunks = (int)Math.ceil(totalPixels / maxBlockSize);
		return  nChunks;
	}
	
    protected boolean countPixels(int w, int h, ColorModel cm, int[] pixels)
    {
		if ( w*h*cm.getNumComponents() != pixels.length )
		{
			System.out.println("* countPixels() got incompatible parameters!");
			return false;
		}
		int pixelStep = cm.getNumComponents();
		for ( int i = 0; i < pixels.length; i+=pixelStep )
		{
			int pix = pixels[i];
			redCounts[pixels[i+0]/*cm.getRed(pix)*/]++;
			greenCounts[pixels[i+1]/*cm.getGreen(pix)*/]++;
			blueCounts[pixels[i+2]/*cm.getBlue(pix)*/]++;
		}
		return  true;
    }

  }//END_OF__class_Counter


  public void read_cmd_line(String[] args)	throws Exception
  {
	CmdLineHandler cmdLine = new CmdLineHandler(args);
	System.out.println(cmdLine);
	// TODO: set "defaults" - as if no arguments
	/*	workDir = new File(".");
		String[] workDirA = cmdLine.params_of_switch("-workdir");
		if ( workDirA != null )
			workDir = new File(workDirA[0]);
	 */
	/*
	String[] paramArr =null;
	if ( (paramArr =cmdLine.params_of_switch("-image_file")) != null )
		imgFile = new File(paramArr[0]);
	 **/
  }
  
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args)
	{
	}

  /**
   * This class converts the image, writing to a BufferedImage.
   */
  public static class Converter
  {
    /** Three 256-entry lookup tables for red, green, and blue */
    private byte[][] lookup;
    /** BufferedImage to write converted results to */
    private BufferedImage ourTarget;
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
		verbose = x;
    }
	
    /**
	 *	Call this to process an image, scaling the values according
     *  to the lookup table in scale.
	 *	@param source image to be processed
	 *	@param scale 3 per-band arrays for lookup table
	 *	@return the resulting image
     */
    public BufferedImage process(BufferedImage source, byte[][] scale)
    {
		//BufferedImage bi = null;
		BufferedImage bi = new BufferedImage(
					source.getWidth(), source.getHeight(), source.getType());
		int compNum = source.getColorModel().getNumComponents();
		//TODO: provide RenderingHints instead of null
        LookupOp lookupop = new LookupOp(new ByteLookupTable(0, scale), null);
		try
		{
			bi = lookupop.filter(source, bi);
		} catch (IllegalArgumentException e)
		{	System.out.println("process(): number of arrays in the LookupTable"
								+ " does not meet the restrictions.");
			return  null;
		}
		ourTarget = bi;
		return  ourTarget;
    }
  
  /**
   * Creates lookup tables doing both color balancing and gamma correction.
   * @param counts 3 per-band arrays of pixel value frequences - counts[0]=array-of-red-frequences.
   */
  protected static byte[][] create_lookup_tables(int[][] counts, double gamma,
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

  /** @return the largest pixel value seen */
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

  }//END_OF__class_Converter
}//END_OF__class_OKRoughCast
