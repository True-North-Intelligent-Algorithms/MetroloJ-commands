
import ij.IJ;
import ij.io.SaveDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import metroloJ.report.PSFprofilerReport;
import metroloJ.setup.metroloJDialog;
import metroloJ.utilities.tricks.fileTricks;
import metroloJ.utilities.doCheck;
import metroloJ.utilities.tricks.imageTricks;



/**
 *
 *  Generate_PSFreport v1, 18 déc. 2008
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

/**
 *
 * @author fab
 */
public class Generate_PSFreport implements PlugIn{

    public void run(String arg) {
        
        if (!(doCheck.isVersionUpToDate() && doCheck.isThereAnImage() && doCheck.isStack() && doCheck.isNoMoreThan16bits() && doCheck.isCalibrated())) return;

        metroloJDialog mjd=new metroloJDialog("PSF report generator");
        mjd.addAll();
        mjd.showDialog();

        if (mjd.wasCanceled()) return;

        mjd.getAll();
        mjd.savePrefs();
        
        SaveDialog sd=new SaveDialog("Save the PSF report to...", "PSF report for "+mjd.ip.getTitle(), ".pdf");
        String path=sd.getDirectory()+sd.getFileName();

        if (!path.endsWith("null")) {
            mjd.ip.hide();

            //This part is requiered to avoid a mix-up between local and global calibrations
            imageTricks.tempRemoveGlobalCal(mjd.ip);

            PSFprofilerReport ppr=new PSFprofilerReport(mjd);
            ppr.saveReport(path, mjd.save);

            //This part is requiered to go back to the original calibration situation
            imageTricks.restoreOriginalCal(mjd.ip);

            mjd.ip.show();

            if(!IJ.isMacro()) fileTricks.showPdf(path);
        }else{
            IJ.showStatus("Process canceled by user...");
        }
        /*TODO: désactiver la calibration globale, la stocker avant
         * puis réactiver en fin de process
        */

        
    }
}
