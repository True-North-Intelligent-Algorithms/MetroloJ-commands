/**
 *
 *  fieldIllumination v1, 16 oct. 2009
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

package metroloJ.fieldIllumination;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Line;
import ij.gui.NewImage;
import ij.gui.Plot;
import ij.gui.ProfilePlot;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.awt.Color;
import java.awt.Font;
import java.util.Vector;
import metroloJ.utilities.tricks.dataTricks;
import metroloJ.utilities.tricks.fileTricks;
import metroloJ.utilities.tricks.imageTricks;

/**
 * Generates a field illumination homogeneity analysis of an image
 * @author fab
 */
public class fieldIllumination {
    /**ImagePlus on which to work **/
    public ImagePlus ip=null;
    
    /** Current images's calibration **/
    public Calibration cal=null;
    
    /** Current images's idth **/
    public int w=0;
    
    /** Current images's height **/
    public int h=0;

    /** Stores the top-left/bottom-right intensity profile **/
    public double[][] diag_TL_BR=null;

    /** Stores the top-right/bottom-left intensity profile **/
    public double[][] diag_TR_BL=null;

    /** Stores the middle image, horizontal intensity profile **/
    public double[][] horiz=null;
    
    /** Stores the middle image, vertical intensity profile **/
    public double[][] vert=null;

    /** Stores the x coordinate of the image's centre of mass **/
    public double xCent=0.0;

    /** Stores the y coordinate of the image's centre of mass **/
    public double yCent=0.0;

    /** Stores the x coordinate of the image's maximum of intensity **/
    public double xMax=0.0;

    /** Stores the y coordinate of the image's maximum of intensity **/
    public double yMax=0.0;

    /** **/
    public double xCent100=0.0;

    /** **/
    public double yCent100=0.0;

    /** Stores the calibrated distance between the centre of intensity and the image's geometrical centre **/
    public double distInt=0.0;

    /** Stores the calibrated distance between the maximum of intensity and the image's geometrical centre **/
    public double distMax=0.0;

    /** Stores the calibrated distance between the centre of the 100% zone and the image's geometrical centre **/
    public double dist100=0.0;

    public Vector<fieldIlluminationArray> remarkInt=new Vector<fieldIlluminationArray>();
    public static final String[] lineHead={"Top-left corner", "Top-right corner", "Bottom-left corner", "Bottom-right corner",
                                           "Upper bound, middle pixel", "Lower bound, middle pixel", "Left bound, middle pixel", "Right bound, middle pixel"};


    /**
     * Starts the processs of creating a fieldIllumination object on the current image
     */
    public fieldIllumination(){
        ip=WindowManager.getCurrentImage();
        if (ip==null){
           IJ.error("Please, open an image first...");
           return;
        }

        w=ip.getWidth();
        h=ip.getHeight();
        cal=ip.getCalibration();


        ip.setCalibration(null);
        ImageStatistics is=ip.getStatistics(ImageStatistics.CENTER_OF_MASS);
        xCent=is.xCenterOfMass;
        yCent=is.yCenterOfMass;
        ip.setCalibration(cal);

        distInt=dataTricks.dist(new double[]{xCent, yCent}, new double[]{w/2, h/2}, cal);
        
        diag_TL_BR=getProfile(ip, new Line(0, 0, w-1, h-1));
        diag_TR_BL=getProfile(ip, new Line(w-1, 0, 0, h-1));
        horiz=getProfile(ip, new Line(0, h/2-1, w-1, h/2-1));
        vert=getProfile(ip, new Line(w/2-1, 0, w/2-1, h-1));


        int[][] coords={{0,0}, {w-1, 0}, {h-1, 0}, {h-1, w-1}, {w/2, 0}, {w/2, h-1}, {0, h/2}, {w-1, h/2}};

        fieldIlluminationArray fia;
        for (int i=0; i<lineHead.length; i++){
            fia=new fieldIlluminationArray();
            fia.name=lineHead[i];
            fia.coord=coords[i];
            fia.intensity=ip.getPixel(coords[i][0], coords[i][1])[0];
            remarkInt.add(fia);
        }
    }


    /**
     * Creates the isointensity image
     * @param stepWidth intensity interval between two isointensity bands
     * @param barWidth width of the scale bar in microns
     * @return the isointensity image as an ImagePlus
     */
    public ImagePlus getPattern(int stepWidth, int barWidth){
        ImageProcessor iproc= NewImage.createImage("", w, h, 1, 8, NewImage.FILL_BLACK).getProcessor();
        
        double max=ip.getStatistics(ImageStatistics.MIN_MAX).max;
        for (int y=0; y<h; y++){
            for (int x=0; x<w; x++){
                int currInt=ip.getPixel(x, y)[0];
                if (currInt==max){xMax=x; yMax=y;}
                iproc.set(x, y,  (int) ((int)((currInt/max)*100/stepWidth)*stepWidth));
            }
        }

        distMax=dataTricks.dist(new double[]{xMax, yMax}, new double[]{w/2, h/2}, cal);

        iproc.setThreshold(100, 100, ImageProcessor.NO_LUT_UPDATE);
        ImagePlus out=new ImagePlus("Pattern from "+ip.getTitle(), iproc);
        ImageStatistics is=out.getStatistics(ImageStatistics.CENTROID+ImageStatistics.LIMIT);
        iproc.resetThreshold();
        xCent100=is.xCentroid;
        yCent100=is.yCentroid;

        dist100=dataTricks.dist(new double[]{xCent100, yCent100}, new double[]{w/2, h/2}, cal);

        fieldIlluminationArray fia=new fieldIlluminationArray();
        fia.name="Maximum found at ("+(int) xMax+","+(int) yMax+")";
        fia.coord=new int[]{(int) xMax, (int) yMax};
        fia.intensity=(int) max;
        remarkInt.add(0, fia);


        for (int i=0; i<remarkInt.size(); i++) remarkInt.elementAt(i).relativeInt=remarkInt.elementAt(i).intensity/max;
        
        iproc.setFont(new Font(Font.SANS_SERIF, Font.BOLD, w/35));
        iproc.setColor(Color.white);

        double slope=(double) (h-1)/(w-1);
        int prevX=w-1;
        int prevY=h-1;

        int refInt=iproc.get(w-1, h-1);

        for (int i=w-1; i>=0; i=i-w/35){
            int currInt=iproc.get(i, (int) (i*slope));
            if (currInt!=refInt){
                String label=((int) (refInt-stepWidth))+"-"+refInt+"%";
                int x=i;
                int y=(int) (i*slope);
                iproc.drawString(label, (prevX+x-iproc.getStringWidth(label))/2, (prevY+y)/2+iproc.getFont().getSize());
                refInt=currInt;
                prevX=x;
                prevY=y;
            }
        }
        imageTricks.addScaleBar(iproc, ip.getCalibration(), imageTricks.BOTTOM_LEFT, barWidth);
        imageTricks.applyFire(iproc);

        return new ImagePlus("Pattern from "+ip.getTitle(), iproc);
    }

    /**
     * Plots the intensity profile along the two diagonales, the vertical and horizontal lines
     * @return an ImagePlus containing the plot
     */
    public ImagePlus getProfilesImage(){
        double min=Math.min(Math.min(Math.min(dataTricks.min(diag_TL_BR[1]), dataTricks.min(diag_TR_BL[1])), dataTricks.min(horiz[1])), dataTricks.min(vert[1]));
        double max=Math.max(Math.max(Math.max(dataTricks.max(diag_TL_BR[1]), dataTricks.max(diag_TR_BL[1])), dataTricks.max(horiz[1])), dataTricks.max(vert[1]));

        Plot plot= new Plot("Field illumination profiles", "Distance to image center", "Intensity", diag_TL_BR[0], diag_TL_BR[1]);
        plot.setLimits(diag_TL_BR[0][0], diag_TL_BR[0][diag_TL_BR[0].length-1], min, max);
        plot.setSize(600, 400);
        plot.setColor(imageTricks.COLORS[0]);
        plot.draw();

        plot.setColor(imageTricks.COLORS[1]);
        plot.addPoints(diag_TR_BL[0], diag_TR_BL[1], Plot.LINE);

        plot.setColor(imageTricks.COLORS[2]);
        plot.addPoints(horiz[0], horiz[1], Plot.LINE);

        plot.setColor(imageTricks.COLORS[3]);
        plot.addPoints(vert[0], vert[1], Plot.LINE);

        double[][] line={{0, 0},{0, max}};
        plot.setColor(Color.black);
        plot.addPoints(line[0], line[1], Plot.LINE);
        plot.draw();

        String label=   "Top-left/bottom-right: "+imageTricks.COLOR_NAMES[0]+
                        "\nTop-right/bottom-left: "+imageTricks.COLOR_NAMES[1]+
                        "\nHorizontale: "+imageTricks.COLOR_NAMES[2]+
                        "\nVerticale: "+imageTricks.COLOR_NAMES[3];

        plot.setColor(Color.black);
        plot.addLabel(0.05, 0.85, label);

        return plot.getImagePlus();
    }

    /**
     * Generates a String containing all the intensity profiles (along the two diagonales,
     * the vertical and horizontal lines) as tab delimited values
     * @return a String containing the numerical values
     */
    public String getStringProfiles(){
        String out="distance (µm)\tTop-left/bottom-right\tdistance (µm)\tTop-right/bottom-left\tdistance (µm)\tHorizontale\tdistance (µm)\tnVerticale\n";
        for (int i=0; i<diag_TL_BR[0].length; i++){
            out+=diag_TL_BR[0][i]+"\t"+diag_TL_BR[1][i]+"\t"+diag_TR_BL[0][i]+"\t"+diag_TR_BL[1][i];
            if (i<horiz[0].length){
                out+="\t"+horiz[0][i]+"\t"+horiz[1][i];
            }else{
                out+="\t\t";
            }
            if (i<vert[0].length){
                out+="\t"+vert[0][i]+"\t"+vert[1][i];
            }else{
                out+="\t\t";
            }
            out+="\n";
        }
        return out;
    }

    /**
     * Generates a String containing the intensity (raw and normalised) values of
     * the 8 intercepts of the 4 remarkable lines (two diagonales, the vertical
     * and horizontal) and image borders as tab delimited values
     * @return a String containing the numerical values
     */
    public String getStringData(){
        String out="\tImage centre\tCentre of intensity\tCentre of the max intensity\tCentre of the 100% zone\n"+
                   "Coordinates\t("+dataTricks.round(w/2, 3)+", "+dataTricks.round(h/2, 3)+")\t("+dataTricks.round(xCent, 3)+", "+dataTricks.round(yCent, 3)+")\t("+dataTricks.round(xMax, 3)+", "+dataTricks.round(yMax, 3)+")\t("+dataTricks.round(xCent100, 3)+", "+dataTricks.round(yCent100, 3)+")\n"+
                   "Distance to image centre\t\t"+dataTricks.round(distInt, 3)+"µm\t"+dataTricks.round(distMax, 3)+"µm\t"+dataTricks.round(dist100, 3)+"µm\n\n"+
                   "Location\tIntensity\tIntensity relative to max\n";
        for (int i=0; i<remarkInt.size(); i++) out+=remarkInt.elementAt(i).toString()+"\n";
        return out;
    }

    /**
     * Prepares data for the Pdf report generation
     * @return a String 2D array containing the image/illumination centre and the
     * distance of illumination centre to the image's center
     */
    public String[][] getCenterTableForReport(){
        return new String[][]{{"", "Coordinates", "Distance to image centre"},
                              {"Image centre", "("+dataTricks.round(w/2, 3)+", "+dataTricks.round(h/2, 3)+")", ""},
                              {"Centre of intensity", "("+dataTricks.round(xCent, 3)+", "+dataTricks.round(yCent, 3)+")", dataTricks.round(distInt, 3)+"µm"},
                              {"Centre of the max intensity", "("+dataTricks.round(xMax, 3)+", "+dataTricks.round(yMax, 3)+")", dataTricks.round(distMax, 3)+"µm"},
                              {"Centre of the 100% zone", "("+dataTricks.round(xCent100, 3)+", "+dataTricks.round(yCent100, 3)+")", dataTricks.round(dist100, 3)+"µm"}};
    }

    /**
     * Generates a String 2D array containing the intensity (raw and normalised) values of the 8 intercepts of
     * the 4 remarkable lines (two diagonales, the vertical and horizontal) and
     * image borders
     * @return a String 2D array to be used in Pdf report generation
     */
    public String[][] getTableForReport(){
        String[][] out=new String[3][10];

        out[0][0]="Location";
        for (int i=1; i<10; i++) out[0][i]=remarkInt.elementAt(i-1).name;

        out[1][0]="Intensity";
        for (int i=1; i<10; i++) out[1][i]=""+remarkInt.elementAt(i-1).intensity;

        out[2][0]="Intensity relative to max";
        for (int i=1; i<10; i++) out[2][i]=""+dataTricks.round(remarkInt.elementAt(i-1).relativeInt, 3);

        return out;
    }

    /**
     * Retrieves the intensity profile on img along the line. Distances are expressed
     * relative to the image's center
     * @param img ImagePlus from which to retrieve the profile
     * @param line line along which the profile should be retrieved
     * @return a 2D double array: out[0: distance, 1: intensities][]
     */
    private double[][] getProfile(ImagePlus img, Line line){
        double[][] out=new double[2][];
        img.setRoi(line);
        ProfilePlot pp=new ProfilePlot(img);
        
        out[1]=pp.getProfile();

        double length=img.getRoi().getLength();
        int nPoints=out[1].length;

        out[0]=new double[out[1].length];

        for (int i=0; i<nPoints; i++) out[0][i]=(i*length/(nPoints-1))-length/2;

        img.killRoi();

        return out;
    }

    /**
     * Saves all data generated during the fieldIllumination analysis
     * @param path folder where the data should be saved
     * @param filename base name of data files
     * @param stepWidth step's width on the pattern image
     * @param barWidth scale bar's width in microns
     */
    public void saveData(String path, String filename, int stepWidth, int barWidth){
        FileSaver fs=new FileSaver(getPattern(stepWidth, barWidth));
        fs.saveAsJpeg(path+filename+"_pattern.jpg");

        fs=new FileSaver(getProfilesImage());
        fs.saveAsJpeg(path+filename+"_intensityProfiles.jpg");

        fileTricks.save(getStringData(), path+filename+"_stats.xls");
        fileTricks.save(getStringProfiles(), path+filename+"_intensityProfiles.xls");
    }

}
