/**
 *
 *  microscope v1, 11 févr. 2009
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

package metroloJ.setup;

import ij.measure.Calibration;
import java.text.DateFormat;
import java.util.Calendar;
import metroloJ.resolution.resolutionCalculator;

public class microscope {
    /** Stores the default microscope types **/
    public static final String[] MICRO={"WideField", "Confocal"};

    public static final int WIDEFIELD=0;
    public static final int CONFOCAL=1;

    /**Stores the type of microscope**/
    public int microscope=0;

    /**Stores the emission wavelength (nm)**/
    public double wavelength=0;

    /**Stores the numerical aperture**/
    public double NA=0;

    /**Stores the pinhole aperture (Airy units)**/
    public double pinhole=0;

    /**Stores the calibration**/
    public Calibration cal=null;

    /**Stores the calculated resolutions**/
    public double[] resolution;

    /**Stores the header of the report, containing the informations about the microscope setup**/
    public String reportHeader="";

    /**Stores the date of creation of the report **/
    public String date="";

    /**Stores the informations about the sample**/
    public String sampleInfos="";

    /**Stores the comment field**/
    public String comments="";

    /**
     * Creates a new microscope object, used to store informations about the microscope and the sample
     * @param microscope microscope type (0: wide-field, 1: confocal)
     * @param wavelength emission wavelength (nm)
     * @param NA numerical aperture
     * @param pinhole pinhole aperture in Airy units
     * @param sampleInfos sample informations to be displayed on the report
     * @param comments comments to be displayed on the report
     */
    public microscope(Calibration cal, int microscope, double wavelength, double NA, double pinhole, String sampleInfos, String comments){
        this.cal=cal;
        this.microscope=microscope;
        this.wavelength=wavelength;
        this.NA=NA;
        this.pinhole=pinhole;
        this.sampleInfos=sampleInfos;
        this.comments=comments;

        resolution=new resolutionCalculator(microscope, wavelength, NA).getResolutions();

        reportHeader="Microscope: "+MICRO[microscope]+"\nWavelength: "+wavelength+" nm\nNA: "+NA+"\nSampling rate: "+round(cal.pixelWidth,3)+"x"+round(cal.pixelHeight,3)+"x"+round(cal.pixelDepth,3)+" "+cal.getUnit();
        if (microscope==CONFOCAL) reportHeader+="\nPinhole: "+pinhole+" Airy Units";
        
        DateFormat df=DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
        date=df.format(Calendar.getInstance().getTime()).toString();
    }

    /**
     * Rounds any double to the user provided number of digits
     * @param nb2round number to round
     * @param nbOfDigits number of digits
     * @return a rounded double
     */
    private double round(double nb2round, int nbOfDigits){
        return Math.round(nb2round*Math.pow(10, nbOfDigits))/Math.pow(10, nbOfDigits);
    }

}
