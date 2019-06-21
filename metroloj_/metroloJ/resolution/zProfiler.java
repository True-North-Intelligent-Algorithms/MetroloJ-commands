/**
 *
 *  zProfiler v1, 17 avr. 2009
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

package metroloJ.resolution;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import ij.process.ImageProcessor;
import java.awt.Color;
import java.awt.Rectangle;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ.setup.microscope;
import metroloJ.utilities.tricks.dataTricks;
import metroloJ.utilities.proj2D;
import metroloJ.utilities.sideViewGenerator;

/**
 * zProfiler allows to retrieve from either a 2D XZ or YZ image the z intensity profiles and calculate the corresponding resolution.
 * @author fab
 */
public class zProfiler {

    /** Equals sqrt(2*ln(2)), used to calculate FWHM of Gaussians **/
    public final static double SQRT2LN2=Math.sqrt(2*Math.log(2));
    
    /** Input ImagePlus **/
    public ImagePlus ip=null;

    /** Input ROI **/
    public Roi roi=null;

    /** Input ImagePlus's calibration **/
    public Calibration cal=new Calibration();

    /** Calibrated distances (x axis of the plot) **/
    public double[] dist=null;
    
    /** Raw intensity profile **/
    public double[] rawProfile=null;

    /** Fitted intensity profile **/
    public double[] fitProfile=null;
    
    /** Fitting parameters **/
    public double[] params=null;

    /** Fitting parameters for the profile as a string**/
    String paramString="Fitted on y = a + (b-a)*exp(-(x-c)^2/(2*d^2))";

    /** Stores the calculated resolution (FWHM) from the profile **/
    double resol=0;


    /**
     * Builds a new zProfiler object.
     * @param ip ImagePlus on which the zProfiler object is built.
     * @param roi the roi where quantification should be done.
     */
    public zProfiler(ImagePlus ip, Roi roi){
        this.ip=ip;
        this.roi=roi;
        cal=ip.getGlobalCalibration()==null?ip.getCalibration():ip.getGlobalCalibration();
        
        
        rawProfile=new proj2D().doProj(ip, roi);
        fitProfile();
    }

    /**
     * Performs the fitting on the retrieved profile.
     */
    private void fitProfile(){
        double max=rawProfile[0];
        params=new double[4];
        params[0]=max;
        params[1]=max;
        params[2]=0;
        
        dist=new double[rawProfile.length];

        for (int i=0; i<rawProfile.length; i++){
            dist[i]=i*cal.pixelHeight;
            double currVal=rawProfile[i];
            params[0]=Math.min(params[0], currVal);
            if (currVal>max){
                params[1]=currVal;
                params[2]=dist[i];
                max=currVal;
            }
        }
        params[3]=ip.getHeight()*cal.pixelHeight/2;

        CurveFitter cv=new CurveFitter(dist, rawProfile);
        cv.setInitialParameters(params);
        cv.doFit(CurveFitter.GAUSSIAN);
        params=cv.getParams();
        String resultString=cv.getResultString();
        paramString=paramString+resultString.substring(resultString.lastIndexOf("ms")+2);
        
        fitProfile=new double[rawProfile.length];
        for (int i=0; i<rawProfile.length; i++) fitProfile[i]=CurveFitter.f(CurveFitter.GAUSSIAN, params, dist[i]);
        resol=2*SQRT2LN2*params[3];
    }

    /**
     * Returns the fitting parameters as a String.
     * @return the fitting parameters as a String.
     */
    public String getParams(){
        return paramString;
    }

    /**
     * Returns a plot object based on the profile of the current ImagePlus.
     * @return a plot object.
     */
    public Plot getProfile(){
        Plot plot=new Plot("Profile plot along the z axis", "z ("+cal.getUnit()+")", "Intensity (AU)", dist, fitProfile);
        plot.setSize(300, 200);
        plot.setColor(Color.red);
        plot.addPoints(dist, rawProfile, Plot.CIRCLE);
        plot.setColor(Color.black);
        plot.addLabel(0.6, 0.13, "Dots: measured\nLine: fitted");
        return plot;
    }

    /**
     * Returns the FWHM of the fitted profile.
     * @return the FWHM of the fitted profile as a double.
     */
    public double getResolution(){
        return resol;
    }

    /**
     * Returns an ImagePlus of the input image, where the ROI and scale bar might have been drawn.
     * @param drawRoi true is the Roi should be drawn.
     * @param addScaleBar true is the scale bar should be drawn.
     * @return an ImagePlus
     */
    public ImagePlus getImage(boolean drawRoi, boolean addScaleBar){
        ImageProcessor iproc=ip.getProcessor().duplicate();
        iproc.setColorModel(iproc.getDefaultColorModel());
        if (drawRoi){
            iproc.setLineWidth(2);
            iproc.setColor(Color.white);
            iproc.draw(roi);
        }
        if(addScaleBar){
            new sideViewGenerator().addScaleBar(iproc, cal, 1);
        }
        return new ImagePlus("", iproc);
    }

    /**
     * Save the numerical data retrieved from the image as well as the fitted data as a tabulation separated data file
     * @param path folder where the file will be saved
     * @param filename the final file will have for name filename_-profile.xls
     */
    public void saveProfile(String path, String filename){
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(path + filename + ".xls"));
            out.write("Distance ("+cal.getUnit()+")\tRaw_data\tFitted_data\n");
            for (int i = 0; i < dist.length; i++) {
                String line = dist[i]+"\t"+rawProfile[i]+"\t"+fitProfile[i];
                out.write(line);
                out.newLine();
            }
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(zProfiler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Generates a array of string containing both the calculated resolution from the image and the theoretical resolution, based on the input microscope setup
     * @param microscope describes the microscope used
     * @return an array of strings
     */
    public String[][] getSummary(microscope microscope){
        String[][] output={{"", "z"},{"FWHM", dataTricks.round(resol, 3)+" "+cal.getUnit()}, {"Theoretical resolution", dataTricks.round(microscope.resolution[2], 3)+" "+IJ.micronSymbol+"m"}};
        return output;
    }

    /**
     * Returns the ROI's coordinates as a string
     * @return the ROI's coordinates as a string
     */
    public String getRoiAsString(){
        Rectangle rect=roi.getBoundingRect();
        return "ROI: from ("+rect.x+", "+rect.y+") to ("+(rect.x+rect.width)+", "+(rect.y+rect.height)+")";
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
            out.write(getRoiAsString());
            out.newLine();
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
            Logger.getLogger(zProfiler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Saves the image of the plot as jpg files
     * @param path folder where the file will be saved
     * @param filename the final file will have for name filename_-plot.jpg
     */
    public void savePlot(String path, String filename){
        FileSaver fs=new FileSaver(getProfile().getImagePlus());
        fs.saveAsJpeg(path+filename+"_-plot.jpg");
    }
}
