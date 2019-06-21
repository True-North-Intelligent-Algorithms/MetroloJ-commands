/**
 *
 *  PSFprofiler v1, 15 dec. 2008
    Fabrice P Cordelieres, fabrice.cordelieres at gmail.com

    Copyright (C) 2008 Fabrice P. Cordelieres

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

package metroloJ.coalignement;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NewImage;
import ij.measure.*;
import ij.plugin.RGBStackMerge;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ.setup.microscope;
import metroloJ.utilities.findCentre;
import metroloJ.utilities.sideViewGenerator;

/**
 * PSFprofiler allows to retrieve from either a 2D or 3D image x, y and z intensity profiles and calculate the corresponding rsolutions.
 * @author Fabrice Cordelières
 */

public class coAlignement {
    /** Reference to the x dimension **/
    public static final int X=0;

    /** Reference to the y dimension **/
    public static final int Y=1;

    /** Reference to the z dimension **/
    public static final int Z=2;

    /** Red ImagePlus on which coAlignement is built **/
    public ImagePlus red=null;

    /** Green ImagePlus on which coAlignement is built **/
    public ImagePlus green=null;

    /** Blue ImagePlus on which coAlignement is built **/
    public ImagePlus blue=null;

    /**Stores infos on the microscope (important: the wavelength !!!)**/
    public microscope[] micro=null;

    /** Stores the coordinates of the Red centre **/
    public double[] redCentre=null;
    
    /** Stores the coordinates of the Green centre **/
    public double[] greenCentre=null;

    /** Stores the coordinates of the Blue centre **/
    public double[] blueCentre=null;

    /** Stores Red-Green inter-centre distance (uncalibrated)**/
    public double RGDistUnCal;

    /** Stores Red-Blue inter-centre distance (uncalibrated)**/
    public double RBDistUnCal;

    /** Stores Green-Blue inter-centre distance (uncalibrated)**/
    public double GBDistUnCal;

    /** Stores Red-Green inter-centre distance (calibrated)**/
    public double RGDistCal;

    /** Stores Red-Blue inter-centre distance (calibrated)**/
    public double RBDistCal;

    /** Stores Green-Blue inter-centre distance (calibrated)**/
    public double GBDistCal;

    /** Stores Red-Green inter-centre reference distance (calibrated)**/
    public double RGRefDist;

    /** Stores Red-Blue inter-centre reference distance (calibrated)**/
    public double RBRefDist;

    /** Stores Green-Blue inter-centre reference distance (calibrated)**/
    public double GBRefDist;

    /** Stores the calibration of the ImagePlus from which the coAlignement is built**/
    public Calibration cal=new Calibration();

    /** Stores the microscope section ,to be used in the report**/
    public String microSection="";

    /**
     * Builds a new coAlignement object
     * @param ip array containing the 2 or 3 images to analyse
     * @param conditions stores the conditions of acquisition (especially the wavelengths of acquisition)
     */
    public coAlignement(ImagePlus[] ip, microscope[] conditions){
        if (ip.length<2) throw new IllegalArgumentException("coAlignement requieres at least 2 ImagePlus.");
        if (ip.length!=conditions.length) throw new IllegalArgumentException("coAlignement requieres the ImagePlus array to be the same size as the microscope array.");
        red=ip[0];
        green=ip[1];
        blue=null;
        cal=red.getCalibration();

        if (ip.length==3) blue=ip[2];
        if (red.getNSlices()==1)throw new IllegalArgumentException("coAlignement requieres all channels to have the save size and to be of same type.");
        if (red.getWidth()!=green.getWidth() || red.getHeight()!=green.getHeight() || red.getNSlices()!=green.getNSlices() || red.getBitDepth()!=green.getBitDepth()) throw new IllegalArgumentException("coAlignement requieres all channels to have the save size and to be of same type.");
        if (ip[2]!=null) if (ip.length==3 && (red.getWidth()!=blue.getWidth() || red.getHeight()!=blue.getHeight() || red.getNSlices()!=blue.getNSlices() || red.getBitDepth()!=blue.getBitDepth())) throw new IllegalArgumentException("coAlignement requieres all channels to have the save size and to be of same type.");

        micro=conditions;
        microSection="Microscope: "+microscope.MICRO[micro[0].microscope]+"\nWavelengths: "+micro[0].wavelength+", "+micro[1].wavelength;
        if (ip[2]!=null) microSection+=", "+micro[2].wavelength;
        microSection+=" nm\nNA: "+micro[0].NA+"\nSampling rate: "+round(cal.pixelWidth,3)+"x"+round(cal.pixelHeight,3)+"x"+round(cal.pixelDepth,3)+" "+cal.getUnit();
        if (micro[0].microscope==microscope.CONFOCAL) microSection+="\nPinhole: "+micro[0].pinhole+" Airy Units";
        
        getCentresAndDist();
    }

    /**
     * Retrieves the centres for all images and calculates the distances between centres
     */
    private void getCentresAndDist(){
        redCentre=new findCentre().getAllCoordinates(red);
        greenCentre=new findCentre().getAllCoordinates(green);
        RGRefDist=calcRefDist(redCentre, greenCentre, micro[1]);
        RGDistUnCal=dist(redCentre, greenCentre, 1, 1, 1);
        RGDistCal=dist(redCentre, greenCentre, cal.pixelWidth, cal.pixelHeight, cal.pixelDepth);
        if (blue!=null){
            blueCentre=new findCentre().getAllCoordinates(blue);
            RBDistUnCal=dist(redCentre, blueCentre, 1, 1, 1);
            RBDistCal=dist(redCentre, blueCentre, cal.pixelWidth, cal.pixelHeight, cal.pixelDepth);
            RBRefDist=calcRefDist(redCentre, blueCentre, micro[2]);
            GBDistUnCal=dist(greenCentre, blueCentre, 1, 1, 1);
            GBDistCal=dist(greenCentre, blueCentre, cal.pixelWidth, cal.pixelHeight, cal.pixelDepth);
            GBRefDist=calcRefDist(greenCentre, blueCentre, micro[2]);
        }
    }

    /**
     * Calculates a distance (either 2D or 3D) between 2 sets of coordinates
     * @param centre1 coordinates set of the first centre
     * @param centre2 coordinates set of the second centre
     * @param calX x calibration
     * @param calY y calibration
     * @param calZ z calibration
     * @return the calibrated distance between the pair of points
     */
    public double dist(double[] centre1, double[] centre2, double calX, double calY, double calZ){
        if (centre1.length==2){
            return Math.sqrt((centre2[0]-centre1[0])*(centre2[0]-centre1[0])*calX*calX+(centre2[1]-centre1[1])*(centre2[1]-centre1[1])*calY*calY);
        }else{
            return Math.sqrt((centre2[0]-centre1[0])*(centre2[0]-centre1[0])*calX*calX+(centre2[1]-centre1[1])*(centre2[1]-centre1[1])*calY*calY+(centre2[2]-centre1[2])*(centre2[2]-centre1[2])*calZ*calZ);
        }
    }
    
    /**
     * Generates data to be used in the pixel shift array from the report
     * @return a 2D array of strings
     */
    public String[][] getPixShiftArray(){
        if(blue==null){
            String[][] output={{"Shift\n(pix.)", "Red", "Green"},
             {"Red (Ref.)", "0\n0\n0", round(greenCentre[0]-redCentre[0], 3)+"\n"+round(greenCentre[1]-redCentre[1], 3)+"\n"+round(greenCentre[2]-redCentre[2], 3)},
             {"Green (Ref.)", round(redCentre[0]-greenCentre[0], 3)+"\n"+round(redCentre[1]-greenCentre[1], 3)+"\n"+round(redCentre[2]-greenCentre[2], 3), "0\n0\n0"},
             {"Resolutions\n(pix.)", round(micro[0].resolution[0]/cal.pixelWidth, 3)+"\n"+round(micro[0].resolution[1]/cal.pixelHeight, 3)+"\n"+round(micro[0].resolution[2]/cal.pixelDepth, 3), round(micro[1].resolution[0]/cal.pixelWidth, 3)+"\n"+round(micro[1].resolution[1]/cal.pixelHeight, 3)+"\n"+round(micro[1].resolution[2]/cal.pixelDepth, 3)},
             {"Centres' coord.", round(redCentre[0], 1)+"\n"+round(redCentre[1], 1)+"\n"+round(redCentre[2], 1), round(greenCentre[0], 1)+"\n"+round(greenCentre[1], 1)+"\n"+round(greenCentre[2], 1)},
             {"Titles", red.getTitle(), green.getTitle()}};
            return output;
        }else{
            String[][] output={{"Shift\n(pix.)", "Red", "Green", "Blue"},
             {"Red (Ref.)", "0\n0\n0", round(greenCentre[0]-redCentre[0], 3)+"\n"+round(greenCentre[1]-redCentre[1], 3)+"\n"+round(greenCentre[2]-redCentre[2], 3), round(blueCentre[0]-redCentre[0], 3)+"\n"+round(blueCentre[1]-redCentre[1], 3)+"\n"+round(blueCentre[2]-redCentre[2], 3)},
             {"Green (Ref.)", round(redCentre[0]-greenCentre[0], 3)+"\n"+round(redCentre[1]-greenCentre[1], 3)+"\n"+round(redCentre[2]-greenCentre[2], 3), "0\n0\n0", round(blueCentre[0]-greenCentre[0], 3)+"\n"+round(blueCentre[1]-greenCentre[1], 3)+"\n"+round(blueCentre[2]-greenCentre[2], 3)},
             {"Blue (Ref.)", round(redCentre[0]-blueCentre[0], 3)+"\n"+round(redCentre[1]-blueCentre[1], 3)+"\n"+round(redCentre[2]-blueCentre[2], 3), round(greenCentre[0]-blueCentre[0], 3)+"\n"+round(greenCentre[1]-blueCentre[1], 3)+"\n"+round(greenCentre[2]-blueCentre[2], 3), "0\n0\n0"},
             {"Titles", red.getTitle(), green.getTitle(), blue.getTitle()}};
            return output;
        }
    }

    /**
     * Generates data to be used in the uncalibrated array from the report
     * @return a 2D array of strings
     */
    public String[][] getUnCalDistArray(){
        if(blue==null){
            String[][] output={{"Dist.\n(pix.)", "Red", "Green"},
             {"Red", "-", ""+round(RGDistUnCal, 3)},
             {"Green", ""+round(RGDistUnCal, 3), "-"},
             {"Resolutions\n(pix.)", round(micro[0].resolution[0]/cal.pixelWidth, 3)+"\n"+round(micro[0].resolution[1]/cal.pixelHeight, 3)+"\n"+round(micro[0].resolution[2]/cal.pixelDepth, 3), round(micro[1].resolution[0]/cal.pixelWidth, 3)+"\n"+round(micro[1].resolution[1]/cal.pixelHeight, 3)+"\n"+round(micro[1].resolution[2]/cal.pixelDepth, 3)},
             {"Centres' coord.", round(redCentre[0], 1)+"\n"+round(redCentre[1], 1)+"\n"+round(redCentre[2], 1), round(greenCentre[0], 1)+"\n"+round(greenCentre[1], 1)+"\n"+round(greenCentre[2], 1)},
             {"Titles", red.getTitle(), green.getTitle()}};
            return output;
        }else{
            String[][] output={{"Dist.\n(pix.)", "Red", "Green", "Blue"},
             {"Red", "-", ""+round(RGDistUnCal, 3), ""+round(RBDistUnCal, 3)},
             {"Green", ""+round(RGDistUnCal, 3), "-", ""+round(GBDistUnCal, 3)},
             {"Blue", ""+round(RBDistUnCal, 3), ""+round(GBDistUnCal, 3), "-"},
             {"Resolutions\n(pix.)", round(micro[0].resolution[0]/cal.pixelWidth, 3)+"\n"+round(micro[0].resolution[1]/cal.pixelHeight, 3)+"\n"+round(micro[0].resolution[2]/cal.pixelDepth, 3), round(micro[1].resolution[0]/cal.pixelWidth, 3)+"\n"+round(micro[1].resolution[1]/cal.pixelHeight, 3)+"\n"+round(micro[1].resolution[2]/cal.pixelDepth, 3), round(micro[2].resolution[0]/cal.pixelWidth, 3)+"\n"+round(micro[2].resolution[1]/cal.pixelHeight, 3)+"\n"+round(micro[2].resolution[2]/cal.pixelDepth, 3)},
             {"Centres' coord.", round(redCentre[0], 1)+"\n"+round(redCentre[1], 1)+"\n"+round(redCentre[2], 1), +round(greenCentre[0], 1)+"\n"+round(greenCentre[1], 1)+"\n"+round(greenCentre[2], 1), round(blueCentre[0], 1)+"\n"+round(blueCentre[1], 1)+"\n"+round(blueCentre[2], 1)},
             {"Titles", red.getTitle(), green.getTitle(), blue.getTitle()}};
            return output;
        }
    }

    /**
     * Generates data to be used in the calibrated array from the report
     * @return a 2D array of strings
     */
    public String[][] getCalDistArray(){
        if(blue==null){
            String[][] output={{"Dist.\n(Ref. dist.)\n"+cal.getUnit(), "Red", "Green"},
             {"Red", "-", ""+round(RGDistCal, 3)+"\n("+round(RGRefDist, 3)+")"},
             {"Green", ""+round(RGDistCal, 3)+"\n("+round(RGRefDist, 3)+")", "-"},
             {"Resolutions\n("+cal.getUnit()+")", round(micro[0].resolution[0], 3)+"\n"+round(micro[0].resolution[1], 3)+"\n"+round(micro[0].resolution[2], 3), round(micro[1].resolution[0], 3)+"\n"+round(micro[1].resolution[1], 3)+"\n"+round(micro[1].resolution[2], 3)},
             {"Centres' coord.", round(redCentre[0], 1)+"\n"+round(redCentre[1], 1)+"\n"+round(redCentre[2], 1), round(greenCentre[0], 1)+"\n"+round(greenCentre[1], 1)+"\n"+round(greenCentre[2], 1)},
             {"Titles", red.getTitle(), green.getTitle()}};
            return output;
        }else{
            String[][] output={{"Dist.\n(Ref. dist.)\n"+cal.getUnit(), "Red", "Green", "Blue"},
             {"Red", "-", ""+round(RGDistCal, 3)+"\n("+round(RGRefDist, 3)+")", ""+round(RBDistCal, 3)+"\n("+round(RBRefDist, 3)+")"},
             {"Green", ""+round(RGDistCal, 3)+"\n("+round(RGRefDist, 3)+")", "-", ""+round(GBDistCal, 3)+"\n("+round(GBRefDist, 3)+")"},
             {"Blue", ""+round(RBDistCal, 3)+"\n("+round(RBRefDist, 3)+")", ""+round(GBDistCal, 3)+"\n("+round(GBRefDist, 3)+")", "-"},
             {"Resolutions\n("+cal.getUnit()+")", round(micro[0].resolution[0], 3)+"\n"+round(micro[0].resolution[1], 3)+"\n"+round(micro[0].resolution[2], 3), round(micro[1].resolution[0], 3)+"\n"+round(micro[1].resolution[1], 3)+"\n"+round(micro[1].resolution[2], 3), round(micro[2].resolution[0], 3)+"\n"+round(micro[2].resolution[1], 3)+"\n"+round(micro[2].resolution[2], 3)},
             {"Centres' coord.", round(redCentre[0], 1)+"\n"+round(redCentre[1], 1)+"\n"+round(redCentre[2], 1), +round(greenCentre[0], 1)+"\n"+round(greenCentre[1], 1)+"\n"+round(greenCentre[2], 1), round(blueCentre[0], 1)+"\n"+round(blueCentre[1], 1)+"\n"+round(blueCentre[2], 1)},
             {"Titles", red.getTitle(), green.getTitle(), blue.getTitle()}};
            return output;
        }
    }

    /**
     * Saves the data array as a tab-delimited file
     * @param path directory were to save
     * @param filename name of the file (without the extension)
     */
    public void saveData(String path, String filename){
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(path + filename + ".xls"));

            out.write("Pixel shift");
            out.newLine();
            saveArray(getPixShiftArray(), out);

            out.newLine();
            out.write("Uncalibrated distances (in pixels)");
            out.newLine();
            saveArray(getUnCalDistArray(), out);

            out.newLine();
            out.write("Calibrated distances ("+cal.getUnit()+")");
            out.newLine();
            saveArray(getCalDistArray(), out);

            out.close();
        } catch (IOException ex) {
            Logger.getLogger(coAlignement.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Saves the input 2D string array using the provided BufferedWriter object
     * @param array input 2D string array
     * @param out BufferedWriter object to be used to save data
     */
    private void saveArray(String[][] array, BufferedWriter out){
        try {
            for (int j=0; j<array[0].length; j++){
                String line = "";
                for (int i = 0; i < array.length; i++) {
                    line += array[i][j].replaceAll("\n", " ") + "\t";
                }
                out.write(line);
                out.newLine();
            }
        } catch (IOException ex) {
            Logger.getLogger(coAlignement.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Generates a panel of 3 side views (xy, xz and yz), each being a maximu intensity projection overlay of the 2 or 3 input stacks
     * @return the side view as an ImagePlus
     */
    public ImagePlus getSideView(){
        sideViewGenerator svg=new sideViewGenerator();
        ImagePlus redView=svg.getPanelView(red, sideViewGenerator.MAX_METHOD, true, true, 5, true, redCentre, 5);
        ImagePlus greenView=svg.getPanelView(green, sideViewGenerator.MAX_METHOD, true, true, 5, true, greenCentre, 5);
        ImagePlus blueView=null;
        if (blue!=null){
            blueView=svg.getPanelView(blue, sideViewGenerator.MAX_METHOD, true, true, 5, true, blueCentre, 5);
        }else{
            ImagePlus dummyBlue=NewImage.createImage("blue", red.getWidth(), red.getHeight(), red.getNSlices(), red.getBitDepth(), NewImage.FILL_BLACK);
            dummyBlue.setCalibration(cal);
            blueView=svg.getPanelView(dummyBlue, sideViewGenerator.MAX_METHOD, true, true, 5, false, null, 5);
            dummyBlue=null;
        }
        ImageStack is=new RGBStackMerge().mergeStacks(redView.getWidth(), redView.getHeight(), 1, redView.getImageStack(), greenView.getImageStack(), blueView.getImageStack(), false);
        return new ImagePlus("Co-alignement side-view", is);
    }

    private double calcRefDist(double[] coordA, double[] coordB, microscope micro){
        double x=(coordB[0]-coordA[0])*cal.pixelWidth;
        double y=(coordB[1]-coordA[1])*cal.pixelHeight;
        double z=(coordB[2]-coordA[2])*cal.pixelDepth;

        double distXY=Math.sqrt(x*x+y*y);
        double distXYZ=Math.sqrt(distXY*distXY+z*z);

        /*
         *The first Airy disc in 3D is not a sphere but rather egg shaped. Therefore, while the maximimum ditance between two colocalising spots in 2D is equal to the xy optical resolution
         *its hard to figure it out along a xz section as the cross section is an ellipse rather than a sphere. What if this section is not coincident with the equatorial plane ?!!!
         *The only mean is to calculate the distance on the Airy "egg shape"...
         *First, we convert the system: centre A becomes the origin of the spherical space. Then we calculate the two coordinates of B into the new space (phi, theta) ie angles in reference
         *to axis Z and X.
        */

        double theta=0;
        if (distXYZ!=0) theta=Math.acos(z/distXYZ);

        double phi=Math.PI/2;
        if (distXY!=0) phi=Math.acos(x/distXY);

        /*
         *Second, we use the two angles in the equation of the "egg shape" to estimate the coordinates of the pixel on its border. Then, we calculate the distance between the origin and this
         *pixel: it will be used as the reference distance...
        */

        double xRef=micro.resolution[0]*Math.sin(theta)*Math.cos(phi);
        double yRef=micro.resolution[1]*Math.sin(theta)*Math.sin(phi);
        double zRef=micro.resolution[2]*Math.cos(theta);

        return Math.sqrt(xRef*xRef+yRef*yRef+zRef*zRef);
    }
    
    private double round(double nb2round, int nbOfDigits){
        return Math.round(nb2round*Math.pow(10, nbOfDigits))/Math.pow(10, nbOfDigits);
    }
}
