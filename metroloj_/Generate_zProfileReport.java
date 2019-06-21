
import ij.IJ;
import ij.gui.Roi;
import ij.io.SaveDialog;
import ij.plugin.PlugIn;
import ij.util.Tools;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import metroloJ.report.zProfilerReport;
import metroloJ.setup.metroloJDialog;
import metroloJ.utilities.tricks.fileTricks;
import metroloJ.utilities.tricks.imageTricks;
import metroloJ.utilities.doCheck;

/**
 *
 *  Generate_zProfileReport v1, 18 avr. 2009
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
public class Generate_zProfileReport implements PlugIn, AdjustmentListener, TextListener{
    metroloJDialog mjd;
    
    Vector sliders, values;

    public void run(String arg) {
        if (!(doCheck.isVersionUpToDate() && doCheck.isThereAnImage() && doCheck.isNoMoreThan16bits() && doCheck.isCalibrated())) return;

        mjd=new metroloJDialog("Axial resolution report generator");

        setROI((int) (mjd.ip.getWidth()/2+.5), 5);

        mjd.addSlider("ROI_Position", 0, mjd.ip.getWidth(), (int) (mjd.ip.getWidth()/2+.5));
        mjd.addSlider("ROI_Width", 0, mjd.ip.getWidth(), 5);
        sliders=mjd.getSliders();
        ((Scrollbar)sliders.elementAt(0)).addAdjustmentListener(this);
        ((Scrollbar)sliders.elementAt(1)).addAdjustmentListener(this);
        values=mjd.getNumericFields();
        ((TextField)values.elementAt(0)).addTextListener(this);
        ((TextField)values.elementAt(1)).addTextListener(this);
        mjd.addAll();
        mjd.showDialog();

        if (mjd.wasCanceled()) return;

        mjd.getNextNumber();
        mjd.getNextNumber();
        mjd.getAll();
        mjd.savePrefs();

        SaveDialog sd=new SaveDialog("Save the axial resolution report to...", "Axial resolution report for "+mjd.ip.getTitle(), ".pdf");
        String path=sd.getDirectory()+sd.getFileName();

        if (!path.endsWith("null")) {
            imageTricks.convertCalibration();

            //This part is requiered to avoid a mix-up between local and global calibrations
            imageTricks.tempRemoveGlobalCal(mjd.ip);

            zProfilerReport zpr=new zProfilerReport(mjd);
            zpr.saveReport(path, mjd.save);

            //This part is requiered to go back to the original calibration situation
            imageTricks.restoreOriginalCal(mjd.ip);

            if(!IJ.isMacro()) fileTricks.showPdf(path);
        }else{
            IJ.showStatus("Process canceled by user...");
        }
    }
       
    public void setROI(int x, int width){
        Roi roi=new Roi((int) (x-width/2-.5), 0, width, mjd.ip.getHeight());
        mjd.ip.setRoi(roi);
    }

    public void adjustmentValueChanged(AdjustmentEvent e) {
        int x=((Scrollbar)sliders.elementAt(0)).getValue();
        int width=((Scrollbar)sliders.elementAt(1)).getValue();
        setROI(x, width);
    }

    public void textValueChanged(TextEvent e) {
        int x=(int) Tools.parseDouble(((TextField)values.elementAt(0)).getText());
        int width=(int) Tools.parseDouble(((TextField)values.elementAt(1)).getText());
        setROI(x, width);
    }
}
