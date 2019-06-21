/**
 *
 *  fieldIlluminationReport v1, 15 oct. 2009
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

package metroloJ.report;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import ij.IJ;
import ij.ImagePlus;
import java.io.*;

import metroloJ.fieldIllumination.fieldIllumination;
import metroloJ.report.utilities.ReportSections;
import metroloJ.setup.microscope;
import metroloJ.setup.metroloJDialog;

/**
 * Generates the field illumination report
 * @author fab
 */
public class fieldIlluminationReport {
    /** Stores a reference to the metroloJDialog**/
    metroloJDialog mjd;

    int stepWidth=10;

    /**Stores the microscope**/
    microscope microscope;

    /**Initializes a ReportSections object to be used later while building the report**/
    ReportSections rs=new ReportSections();

    /** Stores a reference to the fieldIllumination to be used in the report **/
    fieldIllumination fi;

    /** Stores a reference to the views to be used in the report **/
    ImagePlus views;

    /** Stores a reference to the histogram to be used in the report **/
    ImagePlus histogram;

    
    /**
     * Starts the process measuring field illumination homogeneity and creating a report
     * @param mjd the metroloJDialog to start working with
     * @param stepWidth width of the steps on the isointensity image
     */
    public fieldIlluminationReport(metroloJDialog mjd, int stepWidth){
        fi=new fieldIllumination();
        this.mjd=mjd;
        this.stepWidth=stepWidth;
        microscope=mjd.getMicroscope(fi.cal);
    }

    /**
     * Save the results
     * @param path path where to save the file(s)
     * @param saveData true if additional data should be saved (images, excel files...)
     */
    public void saveReport(String path, boolean saveData){
        try {
            Document report = new Document();
            PdfWriter writer = PdfWriter.getInstance(report, new FileOutputStream(path));
            report.open();

            writer.setStrictImageSequence(true);

            report.add(rs.logoRTMFM());

            report.add(rs.bigTitle(microscope.date+"\n"+"Field illumination report on "+fi.ip.getTitle()));

            report.add(rs.title("Normalised intensity profile:"));

            ImagePlus img=fi.getPattern(stepWidth, mjd.scale);

            float zoom2scaleTo350pxMax=35000/Math.max(img.getWidth(), img.getHeight());
            report.add(rs.imagePlus(img, zoom2scaleTo350pxMax));


            report.add(rs.title("Microscope infos:"));
            report.add(rs.paragraph(microscope.reportHeader));

            if (!microscope.sampleInfos.equals("")){
                report.add(rs.title("Sample infos:"));
                report.add(rs.paragraph(microscope.sampleInfos));
            }

            if (!microscope.comments.equals("")){
                report.add(rs.title("Comments:"));
                report.add(rs.paragraph(microscope.comments));
            }

            if (!microscope.sampleInfos.equals("") || !microscope.comments.equals("")) report.newPage();

            report.add(rs.title("Centers' locations:"));
            report.add(rs.table(fi.getCenterTableForReport(), 75));

            if (microscope.sampleInfos.equals("") && microscope.comments.equals("")) report.newPage();

            report.add(rs.title("Intensity profiles:"));
            report.add(rs.imagePlus(fi.getProfilesImage(), 60));
            
            report.add(rs.title("Profiles' statistics:"));
            report.add(rs.table(fi.getTableForReport(), 75));

            report.close();

            if (saveData){
                String filename=path.substring(0, path.lastIndexOf(".pdf"));
                String outPath=filename + File.separator;
                filename=filename.substring(filename.lastIndexOf(File.separator));
                new File(outPath).mkdirs();
                fi.saveData(outPath, filename, stepWidth ,mjd.scale);
            }
        }
        catch (FileNotFoundException ex) {
            IJ.error("Error occured while generating/saving the report");
        }
        catch (DocumentException ex) {
            IJ.error("Error occured while generating/saving the report");
        }
    }
}
