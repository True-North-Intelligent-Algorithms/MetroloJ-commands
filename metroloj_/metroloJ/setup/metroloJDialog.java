/**
 *
 *  metroloJDialog v1, 12 oct. 2009
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

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.Calibration;

/**
 * metroloJDialog extends the GenericDialog class from ImageJ in order to create
 * the default interface of all the metroloJ tools. It stores the preferences, detects
 * the current image and stores a reference to it.
 * @author fab
 */
public class metroloJDialog extends GenericDialog{

    /** Reference to the ImagePlus opened when the metroloJDialog object is initiated **/
    public ImagePlus ip=null;

    /** Microscope type, as an Integer **/
    public int microType=(int) Prefs.get("MetroloJ_micro.double", 0);

    /** Wavelegth in nm **/
    public double Wavelength=Prefs.get("MetroloJ_wave.double", 500);

    /** Numerical aperture **/
    public double NA=Prefs.get("MetroloJ_NA.double", 1.4);

    /** Pinhole aperture (Airy units) **/
    public double pinhole=Prefs.get("MetroloJ_pinhole.double", 1.0);

    /** Sample informations as a String **/
    public String sampleInfos=Prefs.get("MetroloJ_sampleInfos.string", "");

    /**Comments as a String **/
    public String comments=Prefs.get("MetroloJ_comments.string", "");

    /**Scale bar's width in microns **/
    public int scale=(int) Prefs.get("MetroloJ_scale.double", 5);

    /**Stores weither the data/images shoudl be saved or not **/
    public boolean save=Prefs.get("MetroloJ_save.boolean", false);

    /**
     * Creates a new metroloJDialog
     * @param title the title of the metroloJDialog
     */
    public metroloJDialog(String title){
        super(title);
        ip=WindowManager.getCurrentImage();
    }

    /**
     * Adds a drop down menus containing a microscope system list
     */
    public void addMicroscopeType(){
        this.addChoice("Microscope type", microscope.MICRO, microscope.MICRO[microType]);
    }

    /**
     * Adds a numeric field allowing to retrieve a wavelength
     */
    public void addWaveField(){
        this.addNumericField("Wavelength (nm)", Wavelength, 1);
    }

    /**
     * Adds a numeric field allowing to retrieve a numerical aperture
     */
    public void addNA(){
       this.addNumericField("NA", NA, 2);
    }

    /**
     * Adds a numeric field allowing to retrieve the pinhole aperture expressed in Airy units
     */
    public void addPinhole(){
       this.addNumericField("Pinhole (AU)", pinhole, 2);
    }

    /**
     * Adds a text field allowing to retrieve sample informations and comments
     */
    public void addInfos(){
       this.addTextAreas("Sample infos:\n"+sampleInfos, "Comments:\n"+comments, 10, 20);
    }

    /**
     * Adds a numericc field allowing to retrieve the size of the scale bar, expressed in microns
     */
    public void addScale(){
        this.addNumericField("Scale bar ("+IJ.micronSymbol+"m)", scale, 0);
    }

    /**
     * Adds check box allowing to retrieve weither the numerical data and images
     * generated by the tool should be saved
     */
    public void addSaveData(){
       this.addCheckbox("Save image/plots/data", save);
    }

    /**
     * Adds all default fields to the metroloJDialog
     */
    public void addAll(){
        addMicroscopeType();
        addWaveField();
        addNA();
        addPinhole();
        addInfos();
        addScale();
        addSaveData();
    }

    /**
     * Retrieves all data from the default fields on the metroloJDialog
     */
    public void getAll(){
        microType=getNextChoiceIndex();
        Wavelength=getNextNumber();
        NA=getNextNumber();
        pinhole=getNextNumber();
        sampleInfos=getNextText();
        sampleInfos=sampleInfos.replace("Sample infos:\n", "");
        comments=getNextText();
        comments=comments.replace("Comments:\n", "");
        scale=(int) getNextNumber();
        save=getNextBoolean();
    }


    /**
     * Generates a microscope object based on the current image
     * @return a microscope object
     */
    public microscope getMicroscope(){
        return getMicroscope(ip.getCalibration());
    }

    /**
     * Generates a microscope object based on the provided calibration
     * @param cal the calibration to use while generating the microscope object
     * @return a microscope object
     */
    public microscope getMicroscope(Calibration cal){
        if (cal.getUnit().equals("micron"))cal.setUnit(IJ.micronSymbol+"m");
        if ((cal.getUnit()).equals("nm")){
            cal.setUnit(IJ.micronSymbol+"m");
            cal.pixelDepth/=1000;
            cal.pixelHeight/=1000;
            cal.pixelWidth/=1000;
        }

        return new microscope(cal, microType, Wavelength, NA, pinhole, sampleInfos, comments);
    }


    /**
     * Saves all the metroloJDialog fields values to IJPrefs
     */
    public void savePrefs(){
       Prefs.set("MetroloJ_micro.double", microType);
       Prefs.set("MetroloJ_wave.double", Wavelength);
       Prefs.set("MetroloJ_NA.double", NA);
       Prefs.set("MetroloJ_pinhole.double", pinhole);
       Prefs.set("MetroloJ_sampleInfos.string", sampleInfos);
       Prefs.set("MetroloJ_comments.string", comments);
       Prefs.set("MetroloJ_scale.double", scale);
       Prefs.set("MetroloJ_save.boolean", save);
    }
}
