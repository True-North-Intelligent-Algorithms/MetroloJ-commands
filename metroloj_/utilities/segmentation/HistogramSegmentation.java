/**
 *
 *  HistogramSegmentation v1, 25 juin 2008 
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

package utilities.segmentation;

import ij.*;
import ij.gui.NewImage;
import ij.process.*;

/**
 *
 * @author fab
 */
public class HistogramSegmentation {
    int[] histo;
    int min=0, max=0;
    int[] limits;
    
    /** Creates a new instance of HistogramSegmentation. Only works on 8- and 16-bits images.
     * @param ip specifies the ImagePlus to process.
    */
    public HistogramSegmentation (ImagePlus ip){
        int bitDepth=ip.getBitDepth();
        if (bitDepth!=8 && bitDepth!=16) throw new IllegalArgumentException("Histo_seg expect a 8- or 16-bits images");
        
        //build the histogram on the full stack
        this.max=0;
        this.min=(int) Math.pow(2, bitDepth);
        this.histo=new int[this.min];
        
        for (int z=1; z<=ip.getNSlices(); z++){
            ip.setSlice(z);
            for (int y=0; y<ip.getHeight(); y++){
                for (int x=0; x<ip.getWidth(); x++){
                    int val=ip.getPixel(x, y)[0];
                    this.min=Math.min(this.min, val);
                    this.max=Math.max(this.max, val);
                    this.histo[val]++;
                }
            }
        }
    }
    
    /** Calculates the limits of intensities for the n classes expected on the histogram.
     * First, the limits are initiated to define the n classes with the same width.
     * For each class, the mean value is defined. The half distance between means is defined as a new boundary (i;e; new limit value).
     * The process is iterated as lon as the number of iterations is below maxIt and the absolute sum of differences between limits from one iteration to the other is above epsilon.
     * @param nClasses specifies the number of classes to find within the histogram.
     * @param maxIt specifies the maximum number of search rounds.
     * @param epsilon specifies the convergence factor limit below which the search is stopped.
     * @param log specifies if the search should be performed on the log of the histogram frequencies (suitable for fluorescence images).
     * @return an integer array of size nClasses.
    */
    public int[] calcLimits(int nClasses, int maxIt, int epsilon, boolean log){
        double[] means=new double[nClasses];
        int[] oldLimits;
        this.limits= new int[nClasses+1];
        
        //initialize the limits' array
        this.limits[0]=this.min;
        this.limits[nClasses]=this.max;
        for (int i=1; i<nClasses; i++) this.limits[i]=this.limits[i-1]+(max-min)/nClasses;
        
        int it=0;
        int convFact;
        
        do{
            oldLimits=this.limits.clone();
            
            for (int i=0; i<nClasses; i++){
                double freq=0, mean=0;
                int limLow=this.limits[i], limHigh=(i==nClasses-1)?this.limits[i+1]+1:this.limits[i+1];

                for (int j=limLow; j<limHigh; j++){
                    int val=this.histo[j];
                    freq+=log?(val!=0?Math.log(val):0):val;
                    mean+=log?(val!=0?Math.log(val)*j:0):val*j;
                }
                means[i]=mean/freq;
            }
            
            //Calculate new limits
            for (int i=1; i<nClasses; i++) this.limits[i]=(int) Math.floor((means[i-1]+means[i])/2);
            
            //Calculate the convergence factor
            convFact=0;
            for (int i=0; i<nClasses+1; i++) convFact+=Math.abs(this.limits[i]-oldLimits[i]);
            it++;
        }while(it<maxIt && convFact>epsilon);
        
        return this.limits;
    }
    
    /** Calculates the limits of intensities for the n classes expected on the histogram. Suitable for fluorescence images. Basically, it calls calc limits with the following arguments: maxIt=1000, epsilon=0 and log=true.
     * @param nClasses specifies the number of classes to find within the histogram.
     * @return an integer array containing the limits (has a size of nClasses+1). 
    */
    public int[] getLimitsFluo(int nClasses){
        return this.calcLimits(nClasses, 1000, 0, true);
    }
    
    /** Calculates the limits of intensities for the n classes expected on the histogram. Suitable for transmission images. Basically, it calls calc limits with the following arguments: maxIt=1000, epsilon=0 and log=false.
     * @param nClasses specifies the number of classes to find within the histogram.
     * @return an integer array containing the limits (has a size of nClasses+1). 
    */
    public int[] getLimitsTrans(int nClasses){
        return this.calcLimits(nClasses, 1000, 0, false);
    }
    
    /** Returns the histogram of the imagePlus used to construt the HistogramSegmentation object.
     * @return an integer array containing the histogram (has a size of 256 for 8-bits ImagePlus, 65536 for 16-bits ImagePlus).
    */
    public int[] getHisto(){
        return this.histo;
    }
    
    /** Calculates the mean intensity of the nth class, considering intensities between the [limit(n-1); limit(n)[, except for the last class: [limit(n-1); limit(n)]. .
     * @return the mean value as a double.
    */
    public double getMean(int nClasse){
        nClasse--;
        if (this.limits==null) throw new IllegalArgumentException("calcLimits has not yet been called.");
        if (nClasse<0 || nClasse>this.limits.length-1) throw new IllegalArgumentException("Class number out of the [1-"+(this.limits.length-1)+"] range.");
        double mean=0;
        double freq=0;
        
        int limLow=this.limits[nClasse], limHigh=(nClasse==this.limits.length-1)?this.limits[nClasse+1]+1:this.limits[nClasse+1];
        for (int i=limLow; i<limHigh; i++){
            freq+=this.histo[i];
            mean+=i*this.histo[i];
        }
        return mean/freq;
    }
    
    /** Calculates the mean intensity of all classes, considering intensities between the [limit(n-1); limit(n)[, except for the last class: [limit(n-1); limit(n)]. .
     * @return the mean value as a double array.
    */
    public double[] getMean(){
        if (this.limits==null) throw new IllegalArgumentException("calcLimits has not yet been called.");
        double[] mean=new double[this.limits.length-1];
        
        for (int i=1; i<this.limits.length; i++){
            mean[i-1]=this.getMean(i);
        }
        return mean;
    }
    
    /** Calculates the median intensity of the nth class, considering intensities between the [limit(n-1); limit(n)[, except for the last class: [limit(n-1); limit(n)]. .
     * @return the median value as an integer.
    */
    public int getMedian(int nClasse){
        nClasse--;
        if (this.limits==null) throw new IllegalArgumentException("calcLimits has not yet been called.");
        if (nClasse<0 || nClasse>this.limits.length-1) throw new IllegalArgumentException("Class number out of the [1-"+(this.limits.length-1)+"] range.");
        int median=0, nbVal=0, limLow=this.limits[nClasse], limHigh=(nClasse==this.limits.length-1)?this.limits[nClasse+1]+1:this.limits[nClasse+1];
        
        for (int i=limLow; i<limHigh; i++)nbVal+=this.histo[i];
        nbVal=nbVal/2;
        int currNb=0, i=limLow;
        do{
            currNb+=histo[i];
            median=i;
            i++;
        }while(currNb<nbVal && i<=limHigh);
        
        return median;
    }
    
    /** Calculates the median intensity of all classes, considering intensities between the [limit(n-1); limit(n)[, except for the last class: [limit(n-1); limit(n)]. .
     * @return the median value as an integer array.
    */
    public int[] getMedian(){
        if (this.limits==null) throw new IllegalArgumentException("calcLimits has not yet been called.");
        int[] median=new int[this.limits.length-1];
        
        for (int i=1; i<this.limits.length; i++){
            median[i-1]=this.getMedian(i);
        }
        return median;
    }
    
    /** Retrieves the number of pixels within the nth class, considering intensities between the [limit(n-1); limit(n)[, except for the last class: [limit(n-1); limit(n)]. .
     * @return the number of pixels as an integer.
    */
    public int getNb(int nClasse){
        nClasse--;
        if (this.limits==null) throw new IllegalArgumentException("calcLimits has not yet been called.");
        if (nClasse<0 || nClasse>this.limits.length-1) throw new IllegalArgumentException("Class number out of the [1-"+(this.limits.length-1)+"] range.");
        int nb=0;
        
        int limLow=this.limits[nClasse], limHigh=(nClasse==this.limits.length-1)?this.limits[nClasse+1]+1:this.limits[nClasse+1];
        for (int i=limLow; i<limHigh; i++){
            nb+=this.histo[i];
        }
        return nb;
    }
    
    /** Retrieves the number of pixels within each class, considering intensities between the [limit(n-1); limit(n)[, except for the last class: [limit(n-1); limit(n)]. .
     * @return the number of pixels as an integer array.
    */
    public int[] getNb(){
        if (this.limits==null) throw new IllegalArgumentException("calcLimits has not yet been called.");
        int[] nb=new int[this.limits.length-1];
        
        for (int i=1; i<this.limits.length; i++){
            nb[i-1]=this.getNb(i);
        }
        return nb;
    }
    
    /** Calculates the integrated intensity of pixels within the nth class, considering intensities between the [limit(n-1); limit(n)[, except for the last class: [limit(n-1); limit(n)]. .
     * @return the integrated intensity as an integer.
    */
    public int getIntegratedInt(int nClasse){
        nClasse--;
        if (this.limits==null) throw new IllegalArgumentException("calcLimits has not yet been called.");
        if (nClasse<0 || nClasse>this.limits.length-1) throw new IllegalArgumentException("Class number out of the [1-"+(this.limits.length-1)+"] range.");
        int intInt=0;
        
        int limLow=this.limits[nClasse], limHigh=(nClasse==this.limits.length-1)?this.limits[nClasse+1]+1:this.limits[nClasse+1];
        for (int i=limLow; i<limHigh; i++){
            intInt+=i*this.histo[i];
        }
        return intInt;
    }
    
    /** Calculates the integrated intensity of pixels each class, considering intensities between the [limit(n-1); limit(n)[, except for the last class: [limit(n-1); limit(n)]. .
     * @return the integrated intensity each class as an integer array.
    */
    public int[] getIntegratedInt(){
        if (this.limits==null) throw new IllegalArgumentException("calcLimits has not yet been called.");
        int[] intInt=new int[this.limits.length-1];
        
        for (int i=1; i<this.limits.length; i++){
            intInt[i-1]=this.getIntegratedInt(i);
        }
        return intInt;
    }
    
    /** Returns a segmented version of the input ip ImagePlus, where all pixels from the class n will appear carrying an intensity value of n. The segmentation will take for class limits the one calculated for the current HistogramSegmentation object.
     * @return the segmentation result as an ImagePlus.
    */
    public ImagePlus getsegmentedImage(ImagePlus ip){
        if (this.limits==null) throw new IllegalArgumentException("calcLimits has not yet been called.");
        ImagePlus dest=IJ.createImage("SegImg_"+ip.getTitle(), ip.getBitDepth()+"-bit", ip.getWidth(), ip.getHeight(), ip.getNSlices());
        ImageProcessor oriProc, destProc;
        for (int z=1; z<=ip.getNSlices(); z++){
            ip.setSlice(z);
            dest.setSlice(z);
            oriProc=ip.getProcessor();
            destProc=dest.getProcessor();
            for (int y=0; y<ip.getHeight(); y++){
                for (int x=0; x<ip.getWidth(); x++){
                    int val=oriProc.get(x, y);
                    boolean wasChanged=false;
                    for (int borne=0; borne<this.limits.length-1; borne++){
                        if (val>=this.limits[borne] && val<this.limits[borne+1]){
                            destProc.set(x, y, borne+1);
                            wasChanged=true;
                        }
                    }
                    if (!wasChanged) destProc.set(x, y, this.limits.length-1);
                }
            }
        }
        dest.setSlice(1);
        dest.setDisplayRange(0, this.limits.length-1);
        dest.updateAndDraw();
        return dest;
    }

    /** Returns a segmented version of the input ip ImagePlus, where all pixels from the class n will appear carrying an intensity value of n. The segmentation will take for class limits the one calculated for the current HistogramSegmentation object.
     * @return the segmentation result as an ImagePlus.
    */
    public ImagePlus getsegmentedImage(ImagePlus ip, int nClass){
        if (this.limits==null) throw new IllegalArgumentException("calcLimits has not yet been called.");
        if (nClass<0 || nClass>=this.limits.length) throw new IllegalArgumentException("nClass out of bounds.");
        ImagePlus dest=NewImage.createImage("SegImg_class_"+nClass+"_"+ip.getTitle(), ip.getWidth(), ip.getHeight(), ip.getNSlices(), 8, NewImage.FILL_BLACK);
        ImageProcessor oriProc, destProc;
        for (int z=1; z<=ip.getNSlices(); z++){
            ip.setSlice(z);
            dest.setSlice(z);
            oriProc=ip.getProcessor();
            destProc=dest.getProcessor();
            for (int y=0; y<ip.getHeight(); y++){
                for (int x=0; x<ip.getWidth(); x++){
                    int val=oriProc.get(x, y);
                    boolean wasChanged=false;
                    if (val>=this.limits[nClass]){
                        destProc.set(x, y, 255);
                        wasChanged=true;
                    }
                    if (!wasChanged) destProc.set(x, y, 0);
                }
            }
        }
        dest.setSlice(1);
        dest.setDisplayRange(0, 255);
        dest.updateAndDraw();
        return dest;
    }
    
    /** Applies the segmentaion directly on the input ip ImagePlus. All pixels from the class n will appear carrying an intensity value of n. The segmentation will take for class limits the one calculated for the current HistogramSegmentation object.
    */
    public void doSegmentation(ImagePlus ip){
        ImageProcessor iproc;
        for (int z=1; z<=ip.getNSlices(); z++){
            ip.setSlice(z);
            iproc=ip.getProcessor();
            for (int y=0; y<ip.getHeight(); y++){
                for (int x=0; x<ip.getWidth(); x++){
                    int val=iproc.get(x, y);
                    boolean wasChanged=false;
                    for (int borne=0; borne<this.limits.length-1; borne++){
                        if (val>=this.limits[borne] && val<this.limits[borne+1]){
                            iproc.set(x, y, borne+1);
                            wasChanged=true;
                        }
                    }
                    if (!wasChanged) iproc.set(x, y, this.limits.length-1);
                }
            }
        }
        ip.setSlice(1);
        ip.setDisplayRange(0, this.limits.length-1);
        ip.updateAndDraw();
    }

}
