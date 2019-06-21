/**
 *
 *  PSFprofiler v1, 15 déc. 2008
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

package metroloJ.resolution;

import ij.ImagePlus;
import ij.gui.*;
import ij.io.FileSaver;
import ij.measure.*;
import ij.plugin.Slicer;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ.setup.microscope;
import metroloJ.utilities.findMax;
import metroloJ.utilities.tricks.dataTricks;

/**
 * PSFprofiler allows to retrieve from either a 2D or 3D image x, y and z intensity profiles and calculate the corresponding resolutions.
 * @author Fabrice Cordelières
 */

public class PSFprofiler {

    /** Equals sqrt(2*ln(2)), used to calculate FWHM of Gaussians **/
    public final static double SQRT2LN2=Math.sqrt(2*Math.log(2));

    /** Reference to the x dimension **/
    public static final int X=0;

    /** Reference to the y dimension **/
    public static final int Y=1;

    /** Reference to the z dimension **/
    public static final int Z=2;

    /** ImagePlus on which PSFprofiler is built **/
    ImagePlus ip;

    /** Stores the coordinates of the center **/
    int[] center;
    
    /** Stores the x profile, [0=x as a physical distance ,1=raw intensity, 2=fitted data][pixel nb, from 0 to width-1]**/
    double[][] xProfile=null;

    /** Stores the fitting parameters for the x profile **/
    double[] xParams=null;

    /** Stores the fitting goodness for the x profile **/
    double xR2=Double.NaN;

    /** Stores the fitting parameters for the x profile as a string**/
    String xParamString="Fitted on y = a + (b-a)*exp(-(x-c)^2/(2*d^2))";

    /** Stores the y profile, [0=y as a physical distance ,1=raw intensity, 2=fitted data][pixel nb, from 0 to height-1]**/
    double[][] yProfile=null;

    /** Stores the fitting parameters for the y profile **/
    double[] yParams=null;

    /** Stores the fitting goodness for the y profile **/
    double yR2=Double.NaN;
    
    /** Stores the fitting parameters for the y profile as a string**/
    String yParamString="Fitted on y = a + (b-a)*exp(-(x-c)^2/(2*d^2))";

    /** Stores the z profile, [0=z as a physical distance ,1=raw intensity, 2=fitted data][pixel nb, from 0 to nSlices-1]**/
    double[][] zProfile=null;

    /** Stores the fitting parameters for the z profile **/
    double[] zParams=null;

    /** Stores the fitting goodness for the z profile **/
    double zR2=Double.NaN;

    /** Stores the fitting parameters for the z profile as a string**/
    String zParamString="Fitted on y = a + (b-a)*exp(-(x-c)^2/(2*d^2))";

    /** Stores the calibration of the ImagePlus from which the PSFprofiler is built**/
    Calibration cal=new Calibration();

    /** Stores the calculated resolutions (FWHM) in all the dimensions of the image **/
    double[] resol={0, 0, 0};

    /**
     * Builds a new PSFprofiler object
     * @param ip ImagePlus on which the PSFprofiler object is built
     */
    public PSFprofiler(ImagePlus ip){
        if (ip.getNSlices()==1) throw new IllegalArgumentException("PSFprofiler requieres a stack");

        this.ip=ip;
        center=new findMax().getAllCoordinates(ip);
        cal=ip.getCalibration();
        ip.setSlice(center[2]);

        getXprofileAndFit();
        getYprofileAndFit();
        getZprofileAndFit();

    }
    
    public PSFprofiler(String path){
        this(new ImagePlus(path));
    }

    /**
     * Retrieves data and fills xProfile the x profile through the centre of the bead retrieved from the current ImageProcessor
     * @return a double[][] [0=x as a physical distance ,1=raw intensity, 2=fitted data][pixel nb, from 0 to width-1]
     */
    private void getXprofileAndFit(){
        xProfile=new double[3][ip.getWidth()];
        xProfile[1]=ip.getProcessor().getLine(0, center[1], ip.getWidth()-1, center[1]);
        fitProfile(xProfile, xParams, X);
     }

    /**
     * Retrieves data and fills yProfile the y profile through the centre of the bead retrieved from the current ImageProcessor
     * @return a double[][] [0=y as a physical distance ,1=raw intensity, 2=fitted data][pixel nb, from 0 to width-1]
     */
    private void getYprofileAndFit(){
        yProfile=new double[3][ip.getHeight()];
        yProfile[1]=ip.getProcessor().getLine(center[0], 0, center[0], ip.getHeight()-1);
        fitProfile(yProfile, yParams, Y);
    }

    /**
     * Retrieves data and fills zProfile the z profile through the centre of the bead retrieved from the current ImageProcessor
     * @return a double[][] [0=z as a physical distance ,1=raw intensity, 2=fitted data][pixel nb, from 0 to width-1]
     */
    private void getZprofileAndFit(){
        ip.setCalibration(new Calibration());
        ip.setRoi(new Line(0, center[1], ip.getWidth()-1, center[1]));
        ImagePlus crossX=new Slicer().reslice(ip);
        ip.killRoi();
        ip.setCalibration(cal);

        zProfile=new double[3][ip.getNSlices()];
        zProfile[1]=crossX.getProcessor().getLine(center[0], 0, center[0], crossX.getHeight()-1);
        fitProfile(zProfile, zParams, Z);
    }

    private void fitProfile(double [][] profile, double[] params, int dimension){
        double max=profile[1][0];
        double pixelSize=1;
        int resolIndex=0;

        switch (dimension){
            case X: pixelSize=cal.pixelWidth; break;
            case Y: pixelSize=cal.pixelHeight; resolIndex=1; break;
            case Z: pixelSize=cal.pixelDepth; resolIndex=2; break;
        }

        params=new double[4];
        params[0]=max;
        params[1]=max;
        params[2]=0;
        params[3]=2*pixelSize;
        
        for (int i=0; i<profile[0].length; i++){
            profile[0][i]=i*pixelSize;
            double currVal=profile[1][i];
            params[0]=Math.min(params[0], currVal);
            if (currVal>max){
                params[1]=currVal;
                params[2]=profile[0][i];
                max=currVal;
            }
        }
        CurveFitter cv=new CurveFitter(profile[0], profile[1]);
        cv.setInitialParameters(params);
        cv.doFit(CurveFitter.GAUSSIAN);
        params=cv.getParams();
        String paramString=cv.getResultString();
        paramString=paramString.substring(paramString.lastIndexOf("ms")+2);
        
        switch (dimension){
            case X: xParamString+=paramString; xR2=cv.getFitGoodness(); break;
            case Y: yParamString+=paramString; yR2=cv.getFitGoodness(); break;
            case Z: zParamString+=paramString; zR2=cv.getFitGoodness(); break;
        }

        for (int i=0; i<profile[0].length; i++) profile[2][i]=CurveFitter.f(CurveFitter.GAUSSIAN, params, profile[0][i]);
        resol[resolIndex]=2*SQRT2LN2*params[3];
    }

    /**
     * Returns a plot object based on the x profile of the current ImagePlus
     * @return a plot object
     */
    public Plot getXplot(){
        Plot plot=new Plot("Profile plot along the x axis", "x ("+cal.getUnit()+")", "Intensity (AU)", xProfile[0], xProfile[2]);
        plot.setSize(300, 200);
        plot.setColor(Color.red);
        plot.addPoints(xProfile[0], xProfile[1], Plot.CIRCLE);
        plot.setColor(Color.black);
        plot.addLabel(0.6, 0.13, "Dots: measured\nLine: fitted");
        return plot;
    }

    /**
     * Returns a plot object based on the y profile of the current ImagePlus
     * @return a plot object
     */
    public Plot getYplot(){
        Plot plot=new Plot("Profile plot along the y axis", "y ("+cal.getUnit()+")", "Intensity (AU)", yProfile[0], yProfile[2]);
        plot.setSize(300, 200);
        plot.setColor(Color.red);
        plot.addPoints(yProfile[0], yProfile[1], Plot.CIRCLE);
        plot.setColor(Color.black);
        plot.addLabel(0.6, 0.13, "Dots: measured\nLine: fitted");
        return plot;
    }

    /**
     * Returns a plot object based on the z profile of the current ImagePlus
     * @return a plot object
     */
    public Plot getZplot(){
        Plot plot=new Plot("Profile plot along the z axis", "z ("+cal.getUnit()+")", "Intensity (AU)", zProfile[0], zProfile[2]);
        plot.setSize(300, 200);
        plot.setColor(Color.red);
        plot.addPoints(zProfile[0], zProfile[1], Plot.CIRCLE);
        plot.setColor(Color.black);
        plot.addLabel(0.6, 0.13, "Dots: measured\nLine: fitted");
        return plot;
    }

    /**
     * Returns the calculated resolutions in all available dimensions, i.e. FWHM after fitting the 2 or 3 profiles
     * @return the x, y and z (if applicable) resolutions as a double array of size 2 (or 3).
     */
    public double[] getResolutions(){
        return resol;
    }

    /**
     * Returns the unit of length used for the resolutions
     * @return a String containing the unit of length
     */
    public String getUnit(){
        return cal.getUnit();
    }

    public String getXParams(){
        return xParamString;
    }

    public String getYParams(){
        return yParamString;
    }

    public String getZParams(){
        return zParamString;
    }

    /**
     * Save the numerical data retrieved from the image as well as the fitted data as a tabulation separated data file
     * @param path folder where the file will be saved
     * @param filename the final file will have for name filename_x-profile.xls
     */
    public void saveProfiles(String path, String filename){
        saveProfile(path, filename+"_x-profile", xProfile);
        saveProfile(path, filename+"_y-profile", yProfile);
        saveProfile(path, filename+"_z-profile", zProfile);
    }

    /**
     * Save the numerical data retrieved from the image as well as the fitted data as a tabulation separated data file
     * @param path folder where the file will be saved
     * @param filename the final file will have for name filename_x-profile.xls
     * @param data the double array containing the data
     */
    private void saveProfile(String path, String filename, double[][] data){
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(path + filename + ".xls"));
            out.write("Distance ("+cal.getUnit()+")\tRaw_data\tFitted_data\n");
            for (int j = 0; j < data[0].length; j++) {
                String line = "";
                for (int i = 0; i < 3; i++) line += data[i][j] + "\t";
                out.write(line);
                out.newLine();
            }
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(PSFprofiler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Generates a array of string containing both the calculated resolutions from the image and the theoretical resolutions, based on the input microscope setup.
     * The goodness of fitting is also logged.
     * @param microscope describes the microscope used
     * @return an array of strings
     */
    public String[][] getSummary(microscope microscope){
        String[][] output={{"", "x", "y", "z"},
        {"FWHM", dataTricks.round(getResolutions()[0], 3)+" "+getUnit(), dataTricks.round(getResolutions()[1], 3)+" "+getUnit(), dataTricks.round(getResolutions()[2], 3)+" "+getUnit()},
        {"Theoretical resolution", dataTricks.round(microscope.resolution[0], 3)+" µm", dataTricks.round(microscope.resolution[1], 3)+" µm", dataTricks.round(microscope.resolution[2], 3)+" µm"},
        {"Fit goodness", dataTricks.round(xR2, 3)+"", dataTricks.round(yR2, 3)+"", dataTricks.round(zR2, 3)+""}};
        return output;
    }

    /**
     * Saves the summary array as a tab-delimited file
     * @param path directory were to save
     * @param filename name of the file (without the extension)
     */
    public void saveSummary(String path, String filename, microscope microscope){
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(path + filename + "_summary.xls"));
            String[][] array=getSummary(microscope);
            for (int j=0; j<array[0].length; j++){
                String line="";
                for (int i=0; i<array.length; i++){
                    line+=array[i][j].replaceAll("\n", " ")+"\t";
                }
                out.write(line);
                out.newLine();
            }
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(PSFprofiler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Saves the images of the plots as jpg files
     * @param path folder where the file will be saved
     * @param filename the final file will have for name filename_x-plot.jpg
     */
    public void savePlots(String path, String filename){
        FileSaver fs=new FileSaver(getXplot().getImagePlus());
        fs.saveAsJpeg(path+filename+"_x-plot.jpg");
        fs=new FileSaver(getYplot().getImagePlus());
        fs.saveAsJpeg(path+filename+"_y-plot.jpg");
        fs=new FileSaver(getZplot().getImagePlus());
        fs.saveAsJpeg(path+filename+"_z-plot.jpg");
    }
}
