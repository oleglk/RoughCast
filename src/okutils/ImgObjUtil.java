/*
 * ImgObjUtil.java
 *
 * Created on July 2, 2004, 5:10 PM
 */

package OKUtils;

import java.awt.image.*;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.Toolkit;

import java.awt.*;
import javax.swing.*;


/**
 *
 * @author  oleg
 */
public class ImgObjUtil
{
    final static int IMAGE_TYPE = BufferedImage.TYPE_INT_RGB;
		
	/** Creates a new instance of ImgObjUtil */
	public ImgObjUtil()
	{
	}
	
	public static Image toAWTImage(BufferedImage bImage)
	{
		AffineTransformOp aop = new AffineTransformOp (new AffineTransform(),
									AffineTransformOp.TYPE_BILINEAR);
		BufferedImageFilter bif = new BufferedImageFilter(aop);
		FilteredImageSource fsource = new FilteredImageSource(
											bImage.getSource(), bif);
		Image img = Toolkit.getDefaultToolkit().createImage(fsource);
		return img;
	}
	
    public static BufferedImage scaleBufferedImage(BufferedImage source,
										double factorX, double factorY)
	{
        BufferedImage smaller = toBufferedImage(
						getScaledInstanceAWT(source, factorX, factorY));
		return  smaller;
	}

    public static Image getScaledInstanceAWT(BufferedImage source,
									double factorX, double factorY)
	{
        int w = (int) (source.getWidth() * factorX);
        int h = (int) (source.getHeight() * factorY);
        return source.getScaledInstance(w, h, Image.SCALE_SMOOTH);
    }

    public static BufferedImage toBufferedImage(Image image)
	{
        new ImageIcon(image); //load image
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        BufferedImage bimage = new BufferedImage(w, h, IMAGE_TYPE);
        //BufferedImage bimage = getDefaultConfiguration().createCompatibleImage(w, h, Transparency.OPAQUE);
        Graphics2D g = bimage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return bimage;
    }

    public static GraphicsConfiguration getDefaultConfiguration()
	{
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        return gd.getDefaultConfiguration();
    }
}
