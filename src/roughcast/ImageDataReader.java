/*
 * ImageDataReader.java
 *
 * Created on August 20, 2004, 2:03 PM
 */

package RoughCast;

import java.awt.*;
import java.awt.image.*;

/**
 *
 * @author  oleg
 */
public class ImageDataReader
{
	
	/** Creates a new instance of ImageDataReader */
	public ImageDataReader()
	{
	}


	/**
	 * @return pixel samples as 2D array [band][data_elements]
	 */
    public static int[][] getPixelSamples(BufferedImage img) {
       WritableRaster wr = img.getRaster();
       Dimension size = new Dimension(img.getWidth(), img.getHeight());
       return getPixelSamples(wr, size);
    }

	/**
	 * @return pixel samples as 2D array [band][data_elements]
	 */
    public static int[][] getPixelSamples(Raster raster, Dimension imageSize){
       if((raster == null) || (imageSize ==null)) return null;
       SampleModel sm = raster.getSampleModel();
       DataBuffer db = raster.getDataBuffer();
       int imageWidth = imageSize.width;
       int imageHeight = imageSize.height;
       int totalPix = imageHeight*imageWidth;
       int sample[][] = new int[totalPix][];
       for(int i=0;i<imageHeight;i++){
           for(int j=0; j<imageWidth;j++) {
               int pix[] = null;
               sample[i*imageWidth+j] =  sm.getPixel(j,i, pix, db);
           }
       }
       int pixel[][] = new int[sample[0].length][ totalPix];
       for(int i=0; i<pixel.length;i++){
           for(int j=0; j<totalPix;j++) {
               pixel[i][j] = sample[j][i];
           }
       }
       return pixel;
    }


	/**
	 * @return pixels as 1D array [data_elements]
	 */
    public static int[] getImagePixels(BufferedImage img)
	{
		return  getImagePixels(img, 0, 0, img.getWidth()-1, img.getHeight()-1);
	}


	/**
	 * @return pixels as 1D array [data_elements]
	 */
    public static int[] getRasterPixels(Raster raster, ColorModel cm)
	{
		return  getRasterPixels(raster, cm, raster.getMinX(), raster.getMinY(),
								raster.getWidth()-1, raster.getHeight()-1);
	}
	
	/**
	 * @return pixels as 1D array [data_elements]
	 */
    public static int[] getImagePixels(BufferedImage img,
										int xMin, int yMin, int xMax, int yMax)
	{
       WritableRaster wr = img.getRaster();
	   ColorModel cm = img.getColorModel();
       Dimension size = new Dimension(img.getWidth(), img.getHeight());
       return getRasterPixels(wr, cm, xMin, yMin, xMax, yMax);
    }

	/**
	 * @return pixels as 1D array [data_elements]
	 */
    public static int[] getRasterPixels(Raster raster, ColorModel cm,
										int xMin, int yMin, int xMax, int yMax)
	{
       if((raster == null) || (cm ==null)) return null;
       int chunkWidth = xMax - xMin + 1;
       int chunkHeight = yMax - yMin + 1;
	   if (	(xMax < xMin) || (yMax < yMin) ||
			(xMin < raster.getMinX()) || (yMin < raster.getMinY()) ||
			(xMax > (raster.getMinX() + raster.getWidth() -1)) ||
			(yMax > (raster.getMinY() + raster.getHeight()-1)) ) 
		   return  null;
       int totalPix = chunkHeight*chunkWidth;
	   int[] pixels = new int[totalPix * cm.getNumComponents()];
	   try
	   {
			raster.getPixels(xMin, yMin, chunkWidth, chunkHeight, pixels);
	   } catch (ArrayIndexOutOfBoundsException e)
	   {
		   System.out.println("* getRasterPixels(" + chunkWidth + "*" + chunkHeight
								+ " caused ArrayIndexOutOfBounds!");
		   return  null;
	   }
       return pixels;
    }

}
