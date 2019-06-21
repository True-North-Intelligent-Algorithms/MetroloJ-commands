
import ij.*;
import ij.gui.GenericDialog;
import ij.io.SaveDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import metroloJ.report.coAlignementReport;
import metroloJ.setup.microscope;
import metroloJ.utilities.tricks.fileTricks;
import metroloJ.utilities.doCheck;
import metroloJ.utilities.tricks.imageTricks;



/**
 *
 *  Generate_coAlignementReport v1, 25 mars 2009
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

/**
 *
 * @author fab
 */
public class Generate_coAlignementReport implements PlugIn{
    int img1=-1;
    int img2=-1;
    int img3=-1;
    String title=Prefs.get("coAlignementReport_title.string", "");
    int microType=(int) Prefs.get("coAlignementReport_micro.double", 0);
    double Wavelength1=Prefs.get("coAlignementReport_wave1.double", 400);
    double Wavelength2=Prefs.get("coAlignementReport_wave2.double", 500);
    double Wavelength3=Prefs.get("coAlignementReport_wave3.double", 600);
    double NA=Prefs.get("coAlignementReport_NA.double", 1.4);
    double pinhole=Prefs.get("coAlignementReport_pinhole.double", 1.0);
    boolean save=Prefs.get("coAlignementReport_save.boolean", false);

    public void run(String arg) {
       if (!(doCheck.isVersionUpToDate() && doCheck.isThereAnImage() && doCheck.atLeastNOpenedStacks(2))) return;

       int nbStacks=0;
       int[] idList=WindowManager.getIDList();
       if (idList!=null){
           for (int i=0; i<idList.length; i++){
               if (WindowManager.getImage(idList[i]).getNSlices()!=1) nbStacks++;
           }
       }

       String[] stackList=new String[nbStacks];
       String[] stackListNone=new String[nbStacks+1];
       int index=0;
       for (int i=0; i<WindowManager.getImageCount(); i++){
           if (WindowManager.getImage(idList[i]).getNSlices()!=1){
               stackList[index]=WindowManager.getImage(idList[i]).getTitle();
               stackListNone[index]=stackList[index++];
           }
       }

       stackListNone[nbStacks]="None";

       GenericDialog gd=new GenericDialog("Co-alignement report generator");
       gd.addStringField("Title_of_report", title);
       gd.addChoice("Stack_1", stackList, stackList[0]);
       gd.addChoice("Stack_2", stackList, stackList[1]);
       if (nbStacks>2) gd.addChoice("Stack_3", stackListNone, stackListNone[nbStacks==2?nbStacks:nbStacks-1]);
       gd.addChoice("Microscope type", microscope.MICRO, microscope.MICRO[microType]);
       gd.addNumericField("Wavelength_1 (nm)", Wavelength1, 1);
       gd.addNumericField("Wavelength_2 (nm)", Wavelength2, 1);
       if (nbStacks>2) gd.addNumericField("Wavelength_3 (nm)", Wavelength3, 1);
       gd.addNumericField("NA", NA, 2);
       gd.addNumericField("Pinhole (AU)", pinhole, 2);
       gd.addTextAreas("Sample infos", "Comments", 10, 20);
       gd.addCheckbox("Save image/plots/data", save);
       gd.showDialog();

       if (gd.wasCanceled()) return;

       title=gd.getNextString();
       img1=gd.getNextChoiceIndex();
       img2=gd.getNextChoiceIndex();
       if (nbStacks>2) img3=gd.getNextChoiceIndex();
       microType=gd.getNextChoiceIndex();
       Wavelength1=gd.getNextNumber();
       Wavelength2=gd.getNextNumber();
       if (nbStacks>2) Wavelength3=gd.getNextNumber();
       NA=gd.getNextNumber();
       pinhole=gd.getNextNumber();
       String sampleInfos=gd.getNextText();
       sampleInfos=sampleInfos.replace("Sample infos", "");
       String comments=gd.getNextText();
       comments=comments.replace("Comments", "");
       save=gd.getNextBoolean();

       Prefs.set("coAlignementReport_title.string", title);
       Prefs.set("coAlignementReport_micro.double", microType);
       Prefs.set("coAlignementReport_wave1.double", Wavelength1);
       Prefs.set("coAlignementReport_wave2.double", Wavelength2);
       Prefs.set("coAlignementReport_wave3.double", Wavelength3);
       Prefs.set("coAlignementReport_NA.double", NA);
       Prefs.set("coAlignementReport_pinhole.double", pinhole);
       Prefs.set("coAlignementReport_save.boolean", save);

       SaveDialog sd=new SaveDialog("Save the co-alignement report to...", "coAlignement report"+(title.equals("")?title:" "+title), ".pdf");
       String path=sd.getDirectory()+sd.getFileName();

       if (!path.endsWith("null")) {
           ImagePlus ip1=WindowManager.getImage(idList[img1]);
           ImagePlus ip2=WindowManager.getImage(idList[img2]);
           ImagePlus ip3=null;
           if (nbStacks>2 && img3!=nbStacks) ip3=WindowManager.getImage(idList[img3]);

           Calibration cal=ip1.getCalibration();
           if (cal.getUnit().equals("pixel")){
               IJ.error("Please, calibrate the image first...");
               IJ.run("Properties...");
               return;
           }

           if (cal.getUnit().equals("micron"))cal.setUnit(IJ.micronSymbol+"m");
           if ((cal.getUnit()).equals("nm")){
               cal.setUnit(IJ.micronSymbol+"m");
               cal.pixelDepth/=1000;
               cal.pixelHeight/=1000;
               cal.pixelWidth/=1000;
               ip1.setCalibration(cal);
               ip2.setCalibration(cal);
               if (nbStacks>2 && img3!=nbStacks) ip3.setCalibration(cal);
           }

           microscope micro1=new microscope(ip1.getCalibration(), microType, Wavelength1, NA, pinhole, sampleInfos, comments);
           microscope micro2=new microscope(ip1.getCalibration(), microType, Wavelength2, NA, pinhole, sampleInfos, comments);
           microscope micro3=null;
           if (nbStacks>2 && img3!=nbStacks) micro3=new microscope(ip1.getCalibration(), microType, Wavelength3, NA, pinhole, sampleInfos, comments);

           ImagePlus[] ipArray={ip1, ip2, ip3};
           microscope[] microArray={micro1, micro2, micro3};

           ip1.hide();
           ip2.hide();
           if (nbStacks>2 && img3!=nbStacks) ip3.hide();

           //This part is requiered to avoid a mix-up between local and global calibrations
           imageTricks.tempRemoveGlobalCal(ip1);

           coAlignementReport coAR=new coAlignementReport(ipArray, microArray, title);
           coAR.saveReport(path, save);

           //This part is requiered to go back to the original calibration situation
           imageTricks.restoreOriginalCal(ip1);

           ip1.show();
           ip2.show();
           if (nbStacks>2 && img3!=nbStacks) ip3.show();

           if(!IJ.isMacro()) fileTricks.showPdf(path);
        }else{
            IJ.showStatus("Process canceled by user...");
        }










       
    }
    

}
