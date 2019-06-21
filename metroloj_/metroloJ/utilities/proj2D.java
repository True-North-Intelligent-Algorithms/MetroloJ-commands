/**
 *
 *  proj2D v1, 17 avr. 2009
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

package metroloJ.utilities;

import ij.ImagePlus;
import ij.gui.Roi;
import java.awt.Rectangle;
import java.util.Arrays;

/**
 * proj2D is to be used to do a 1D projection of a ROI (projection might be AVG_METHOD, MAX_METHOD, MIN_METHOD, SUM_METHOD, SD_METHOD or MEDIAN_METHOD) , either along its X_AXIS or Y_AXIS. The default projection is MAX_METHOD, Y_AXIS.
 * @author fab
 */
public class proj2D {
    public static final int X_AXIS=0;
    public static final int Y_AXIS=1;

    public static final int AVG_METHOD = 0;
    public static final int MAX_METHOD = 1;
    public static final int MIN_METHOD = 2;
    public static final int SUM_METHOD = 3;
    public static final int SD_METHOD = 4;
    public static final int MEDIAN_METHOD = 5;

    public int projType=AVG_METHOD;
    public int projAxis=X_AXIS;


    /**
     *Starts the process of creation of a new proj2D object
     */
    public proj2D(){
    }

    /**
     * Set the projection's type (AVG_METHOD, MAX_METHOD, MIN_METHOD, SUM_METHOD, SD_METHOD or MEDIAN_METHOD) and axis (X_AXIS or Y_AXIS). The default projection is AVG_METHOD, X_AXIS.
     * @param projType
     * @param projAxis
     */
    public void setProjType(int projType, int projAxis){
        if (projType>=0 && projType<6) this.projType=projType;
        if (projAxis>=0 && projAxis<2) this.projAxis=projAxis;
    }

    /**
     * Does the projection using the following arguments:
     * @param ip source ImagePlus.
     * @param x x coordinate of the ROI's upper left corner.
     * @param y y coordinate of the ROI's upper left corner.
     * @param width width of the ROI.
     * @param height height of the ROI.
     * @return the projection as a double array.
     */
    public double[] doProj(ImagePlus ip, int x, int y, int width, int height){
        if (ip.getBitDepth()>16) throw new IllegalArgumentException("proj2D expects a 8- or 16-bits ImagePlus");
        int[][] array=projAxis==X_AXIS?new int[height][width]:new int[width][height];

        for (int j=y; j<y+height; j++){
            for (int i=x; i<x+width; i++){
                if (projAxis==X_AXIS){
                    array[j-y][i-x]=ip.getProcessor().get(i, j);
                }else{
                    array[i-x][j-y]=ip.getProcessor().get(i, j);
                }
            }
        }
        double[] proj=new double[array.length];

        for (int i=0; i<proj.length; i++) proj[i]=proj(array[i]);
        array=null;
        return proj;
    }
    
    /**
     * Does the projection using the following arguments:
     * @param ip source ImagePlus.
     * @param rect a java.awt.Rectangle i.e. the ROI
     * @return the projection as a double array.
     */
    public double[] doProj(ImagePlus ip, Rectangle rect){
        return doProj(ip, rect.x, rect.y, rect.width, rect.height);
    }

    /**
     * Does the projection using the following arguments:
     * @param ip source ImagePlus.
     * @param roi the ROI to be used. NB: in case of non rectangular ROIs, its bounding rectangle will be used.
     * @return the projection as a double array.
     */
    public double[] doProj(ImagePlus ip, Roi roi){
        return doProj(ip, roi.getBounds());
    }

    /**
     * Returns the average, maximum, minimum, sum, SD or median of an input double array.
     * @param array the input array
     * @return the calculated value as a double.
     */
    private double proj(int[] array){
        double projVal=0;
        switch(projType){
            case AVG_METHOD: for (int i=0; i<array.length; i++) projVal+=array[i]; projVal/=array.length; break;
            case MAX_METHOD: Arrays.sort(array); projVal=array[array.length-1]; break;
            case MIN_METHOD: Arrays.sort(array); projVal=array[0]; break;
            case SUM_METHOD: for (int i=0; i<array.length; i++) projVal+=array[i]; break;
            case SD_METHOD: double avg=0; for (int i=0; i<array.length; i++) avg+=array[i]; avg/=array.length; for (int i=0; i<array.length; i++) projVal+=(array[i]-avg)*(array[i]-avg); projVal=Math.sqrt(projVal/array.length); break;
            case MEDIAN_METHOD: Arrays.sort(array); projVal=array[(int) (array.length/2+.5)]; break;
            default: Arrays.sort(array); projVal=array[array.length-1]; break;
        }
        return projVal;
    }

}
