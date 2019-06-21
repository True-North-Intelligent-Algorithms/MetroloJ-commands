/**
 *
 *  imageTricks v1, 15 oct. 2009
    Fabrice P Cordelieres, fabrice.cordelieres at gmail.com

    Copyright (C) 2009 Fabrice P. Cordelieres

    License:
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package metroloJ.utilities.tricks;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.NewImage;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.LUT;
import java.awt.Color;
import java.awt.Font;

/**
 * imageTricks contains commonly used tools to workd on images (ImageProcessor or ImagePlus)
 * @author fab
 */
public class imageTricks {

    /**Array of colors**/
    public static final Color[] COLORS={Color.red, Color.green, Color.blue, Color.magenta, Color.yellow, Color.cyan};

    /**Array of colors names**/
    public static final String[] COLOR_NAMES={"Red", "Green", "Blue", "Magenta", "Yellow", "Cyan"};

    /**Fraction of the image in percent**/
    public static int fraction =20;

    /**Scale bar thickness in pixels**/
    public static int barHeightInPixels=4;

    /**Posistion the scale bar on the bottom-right corner of the image**/
    public static final int BOTTOM_RIGHT=0;

    /**Posistion the scale bar on the bottom-left corner of the image**/
    public static final int BOTTOM_LEFT=1;

    /**Font size in points**/
    public static int fontSize=12;

    /**Global calibration**/
    public static Calibration globalCal=null;

    /**Local calibration**/
    public static Calibration localCal=null;


    /**
     * If expressed in micron or nm, transforms the calibration of the current image to µm
     */
    public static void convertCalibration(){
        ImagePlus ip=WindowManager.getCurrentImage();
        Calibration cal=ip.getCalibration();
        if (cal.getUnit().equals("micron"))cal.setUnit(IJ.micronSymbol+"m");
        if ((cal.getUnit()).equals("nm")){
            cal.setUnit(IJ.micronSymbol+"m");
            cal.pixelDepth/=1000;
            cal.pixelHeight/=1000;
            cal.pixelWidth/=1000;
            ip.setCalibration(cal);
       }
    }

    /**
     * Removes the current global calibration, stores it, and set the local calibration
     * to the global calibration while storing the local calibration
     * @param ip input ImagePlus
     */
    public static void tempRemoveGlobalCal(ImagePlus ip){
        localCal=ip.getLocalCalibration();
        globalCal=ip.getGlobalCalibration();
        ip.setGlobalCalibration(null);
        ip.setCalibration(localCal==null?globalCal:localCal);
    }

    /**
     * Applies to the ImagePlus the local and global calibration stored using the
     * tempsRemoveGlobalCal method
     * @param ip input ImagePlus
     */
    public static void restoreOriginalCal(ImagePlus ip){
        ip.setGlobalCalibration(globalCal);
        ip.setCalibration(localCal);
    }


    /**
     * Creates a copy of an ImagePlus
     * @param ip the ImagePlus to copy
     * @param title title for the copy
     * @return an ImagePlus
     */
    public static ImagePlus copyCarbon(ImagePlus ip, String title){
        ImagePlus out=NewImage.createImage(title, ip.getWidth(), ip.getHeight(), ip.getNSlices(), ip.getBitDepth(), NewImage.FILL_BLACK);
        for (int i=1; i<=ip.getNSlices(); i++){
            ip.setSlice(i);
            out.setSlice(i);
            out.setProcessor(null, ip.getProcessor().duplicate());
        }
        return out;
    }

    /**
     * Generates a montage from the slices of the provided stack
     * @param ip the stack from which the montage should be made
     * @param nColumns number of colums within the montage
     * @param borderWidth width of the border surrounding each snapshot (in pixels)
     * @return the montage as an ImagePlus
     */
    public static ImagePlus makeMontage(ImagePlus ip, int nColumns, int borderWidth){
        if (ip.getNSlices()==1) return ip;

        int w=ip.getWidth();
        int h=ip.getHeight();
        int d=ip.getNSlices();

        int nRows=(int) ((double) d/nColumns+.5);
        
        int wMontage=w*nColumns+borderWidth*(nColumns-1);
        int hMontage=h*nRows+borderWidth*(nRows-1);

        ImageProcessor out=ip.getProcessor().createProcessor(wMontage, hMontage);
        out.setColorModel(out.getDefaultColorModel());
        out.setColor(Color.white);
        out.fill();

        int counter=1;
        for (int y=0; y<nRows; y++){
            for (int x=0; x<nColumns; x++){
                ip.setSlice(counter++);
                out.insert(ip.getProcessor(), x*(w+borderWidth), y*(h+borderWidth));
                if (counter>d) y=nRows;
            }
        }

        return new ImagePlus("Montage", out);
    }


    /**
      * Draws a scale bar on the ImageProcessor used as argument (adapted from the original ImageJ ScaleBar class)
      * @param ip ImageProcessor on which to draw the scale bar
      * @param cal Calibration of the current ImageProcessor
      * @param barPosition an Integer representing the position of the scale bar
      * @param barWidth width of the scale bar expressed in the current Calibration's units
      */
     public static void addScaleBar(ImageProcessor ip, Calibration cal, int barPosition, int barWidth){
         int barWidthInPixels=(int) (barWidth/cal.pixelWidth);
         int width=ip.getWidth();
         int height=ip.getHeight();
         fontSize=width/35;
         Font oldFont=ip.getFont();
         ip.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
         
         String barString=barWidth+" "+cal.getUnits();
         int stringWidth=ip.getStringWidth(barString);
         
         int x;
         switch(barPosition){
             case BOTTOM_RIGHT: x=width-width/fraction-barWidthInPixels; break;
             case BOTTOM_LEFT: x=width/fraction; break;
             default: x=width-width/fraction-barWidthInPixels; break;
         }


         int y=height-height/fraction-barHeightInPixels-fontSize;
         int xOffset=(int) (barWidthInPixels-stringWidth)/2;
         int yOffset=(int) (barHeightInPixels+fontSize+fontSize/4);

         ip.setColor(Color.white);
         ip.setRoi(x, y, barWidthInPixels, fontSize/3);
         ip.fill();
         ip.drawString(barString, x+xOffset, y+yOffset);
         
         ip.setFont(oldFont);
         fontSize=12;
     }

     /**
      * Draw the string label on the ImageProcessor
      * @param ip the ImageProcessor to label
      * @param string the label to draw
      */
     public static void drawLabel(ImageProcessor ip, String string){
         int width=ip.getWidth();
         int height=ip.getHeight();
         fontSize=width/15;

         int xOffset=(int) fraction*width/500;
         int yOffset=(int) fraction*height/500+fontSize;

         ip.setColor(Color.white);
         Font oldFont=ip.getFont();
         ip.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
         ip.drawString(string, xOffset, yOffset);
         ip.setFont(oldFont);
         fontSize=12;
     }

     /**
      * Draws a cross on the ImageProcessor used as argument
      * @param ip ImageProcessor on which to draw the cross
      * @param coord a 2D array containing the coordinated of the centre of the cross
      * @param radius width/height of the cross
      */
     public static void addCross(ImageProcessor ip, int[] coord, int radius){
         ip.setColor(Color.white);
         ip.setLineWidth((int) Math.max(2, Math.max(ip.getWidth(), ip.getHeight())/500));
         ip.multiply(0.5);
         ip.drawLine(coord[0], (int) (coord[1]-radius), coord[0], (int) (coord[1]+radius));
         ip.drawLine((int) (coord[0]-radius), coord[1], (int) (coord[0]+radius), coord[1]);
     }

     /**
      * Applies the fire LUT to the provided ImageProcessor
      * @param ip the ImageProcessor on which the fire LUT should be applied
      */
     public static void applyFire(ImageProcessor ip){
         int[] red = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 4, 7, 10, 13, 16, 19, 22, 25, 28, 31, 34, 37, 40, 43, 46, 49, 52, 55, 58, 61, 64, 67, 70, 73, 76, 79, 82, 85, 88, 91, 94, 98, 101, 104, 107, 110, 113, 116, 119, 122, 125, 128, 131, 134, 137, 140, 143, 146, 148, 150, 152, 154, 156, 158, 160, 162, 163, 164, 166, 167, 168, 170, 171, 173, 174, 175, 177, 178, 179, 181, 182, 184, 185, 186, 188, 189, 190, 192, 193, 195, 196, 198, 199, 201, 202, 204, 205, 207, 208, 209, 210, 212, 213, 214, 215, 217, 218, 220, 221, 223, 224, 226, 227, 229, 230, 231, 233, 234, 235, 237, 238, 240, 241, 243, 244, 246, 247, 249, 250, 252, 252, 252, 253, 253, 253, 254, 254, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255};
         int[] green = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 3, 5, 7, 8, 10, 12, 14, 16, 19, 21, 24, 27, 29, 32, 35, 37, 40, 43, 46, 48, 51, 54, 57, 59, 62, 65, 68, 70, 73, 76, 79, 81, 84, 87, 90, 92, 95, 98, 101, 103, 105, 107, 109, 111, 113, 115, 117, 119, 121, 123, 125, 127, 129, 131, 133, 134, 136, 138, 140, 141, 143, 145, 147, 148, 150, 152, 154, 155, 157, 159, 161, 162, 164, 166, 168, 169, 171, 173, 175, 176, 178, 180, 182, 184, 186, 188, 190, 191, 193, 195, 197, 199, 201, 203, 205, 206, 208, 210, 212, 213, 215, 217, 219, 220, 222, 224, 226, 228, 230, 232, 234, 235, 237, 239, 241, 242, 244, 246, 248, 248, 249, 250, 251, 252, 253, 254, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255};
         int[] blue = {0, 7, 15, 22, 30, 38, 45, 53, 61, 65, 69, 74, 78, 82, 87, 91, 96, 100, 104, 108, 113, 117, 121, 125, 130, 134, 138, 143, 147, 151, 156, 160, 165, 168, 171, 175, 178, 181, 185, 188, 192, 195, 199, 202, 206, 209, 213, 216, 220, 220, 221, 222, 223, 224, 225, 226, 227, 224, 222, 220, 218, 216, 214, 212, 210, 206, 202, 199, 195, 191, 188, 184, 181, 177, 173, 169, 166, 162, 158, 154, 151, 147, 143, 140, 136, 132, 129, 125, 122, 118, 114, 111, 107, 103, 100, 96, 93, 89, 85, 82, 78, 74, 71, 67, 64, 60, 56, 53, 49, 45, 42, 38, 35, 31, 27, 23, 20, 16, 12, 8, 5, 4, 3, 3, 2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 8, 13, 17, 21, 26, 30, 35, 42, 50, 58, 66, 74, 82, 90, 98, 105, 113, 121, 129, 136, 144, 152, 160, 167, 175, 183, 191, 199, 207, 215, 223, 227, 231, 235, 239, 243, 247, 251, 255, 255, 255, 255, 255, 255, 255, 255};

         byte[] r=new byte[256];
         byte[] g=new byte[256];
         byte[] b=new byte[256];
         for (int i=0; i<256; i++){
             r[i]=(byte) red[i];
             g[i]=(byte) green[i];
             b[i]=(byte) blue[i];
         }

         LUT lut=new LUT(8, 256, r, g, b);
         ip.setColorModel(lut);
     }
}
