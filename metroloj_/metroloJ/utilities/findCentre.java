/**
 *
 *  findCentre v1, 22 mars 2009
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

import utilities.segmentation.HistogramSegmentation;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.process.EllipseFitter;
import ij.process.ImageConverter;
import ij.process.ImageStatistics;

/**
 * findMax is to be used to retrieve the coordinates. Coordinates are uncalibrated.
 * @author fab
 */
public class findCentre {
    public static final int XY=0;
    public static final int XZ=1;
    public static final int YZ=2;

    /**
     *Starts the process of creation of a new findMax object
     */
    public findCentre(){

    }

    /**
     * Retrieves all coordinates for the current's ImagePlus maximum of intensity
     * @param ip ImagePlus on which to find the maximum
     * @return an array of integer of size 2 in 2D, 3 in 3D. x coordinate will be found within the array at index 0, y at index 1 and if applicable z at position 2
     */
    public double[] getAllCoordinates(ImagePlus ip){
        double[] coord;

        if (ip.getNSlices()==1){
            coord=get2DCenter(ip, XY);
        }else{
            double[] coord2D=get2DCenter(ip, XY);
            coord=new double[3];
            coord[0]=coord2D[0];
            coord[1]=coord2D[1];
            coord[2]=get2DCenter(ip, XZ)[1];
        }
        return coord;
    }

    /**
     * Retrieves the x and y coordinates of the geometrical centre on the current ImagePlus, on the current slice
     * @param ip ImagePlus on which to find the centre.
     * @param profileType indicates the profile orientation to be used: 0 for XY, 1 for XZ and 2 for YZ.
     * @return an array of integer of size 2. Depending on profileType, x or y coordinate will be found within the array at index 0, y or z at index 1.
     */
    public double[] get2DCenter(ImagePlus ip, int profileType){
        double[] coord=new double[2];

        ImagePlus proj=null;
        switch (profileType){
            case XY: proj=new sideViewGenerator().getXYview(ip, sideViewGenerator.SUM_METHOD); break;
            case XZ: proj=new sideViewGenerator().getXZview(ip, sideViewGenerator.SUM_METHOD, false); break;
            case YZ: proj=new sideViewGenerator().getYZview(ip, sideViewGenerator.SUM_METHOD, false); break;
            default: proj=new sideViewGenerator().getXYview(ip, sideViewGenerator.SUM_METHOD); break;
        }
        new ImageConverter(proj).convertToGray8();
        proj.updateImage();
        
        HistogramSegmentation hs=new HistogramSegmentation(proj);
        hs.calcLimits(2, 100, 0, true);
        proj=hs.getsegmentedImage(proj, 1);
        proj.updateImage();


        ImageStatistics is=proj.getStatistics(ImageStatistics.CENTER_OF_MASS);
        Wand wand=new Wand(proj.getProcessor());
        coord[0]=is.xCenterOfMass;
        coord[1]=is.yCenterOfMass;


        EllipseFitter ef=new EllipseFitter();
        do{
            wand.autoOutline((int) (coord[0]+.5), (int) (coord[1]+.5), 128, 255);
            proj.setRoi(new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.POLYGON));
            ef.fit(proj.getProcessor(), null);
            coord[0]=ef.xCenter+1;
            coord[1]=ef.yCenter;
        }while(ef.minor<2);
        coord[0]--;
        return coord;
    }
}
