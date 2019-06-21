/**
 *
 *  CVReport v1, 15 oct. 2009
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

import metroloJ.cv.cv;
import metroloJ.report.utilities.ReportSections;
import metroloJ.setup.microscope;
import metroloJ.setup.metroloJDialog;

/**
 * Generates the CV (coefficient of variation) report
 * @author fab
 */
public class CVReport {
    /** Stores a reference to the metroloJDialog**/
    metroloJDialog mjd;

    /**Stores the microscope**/
    microscope microscope;

    /**Initializes a ReportSections object to be used later while building the report**/
    ReportSections rs=new ReportSections();

    /** Stores a reference to the cv to be used in the report **/
    cv cv;

    /** Stores a reference to the views to be used in the report **/
    ImagePlus views;

    /** Stores a reference to the histogram to be used in the report **/
    ImagePlus histogram;

    
    /**
     * Starts the process measuring CV and creating a report
     * @param mjd the metroloJDialog to start working with
     */
    public CVReport(metroloJDialog mjd){
        cv=new cv();
        this.mjd=mjd;
        microscope=mjd.getMicroscope(cv.cal);
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

            report.add(rs.bigTitle(microscope.date+"\n"+"CV report on "+cv.ip.getTitle()));

            report.add(rs.title("ROIs used for measures:"));

            ImagePlus img=cv.getPanel(mjd.scale);

            float zoom2scale=40000/Math.max(img.getWidth(), img.getHeight());
            if (mjd.ip.getNSlices()==1) zoom2scale=20000/Math.max(img.getWidth(), img.getHeight());
            report.add(rs.imagePlus(img, zoom2scale));


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

            report.add(rs.title(cv.nImg==1?"Histogram:":"Histograms:"));
            zoom2scale=50;
            if (mjd.ip.getNSlices()==1) zoom2scale=40;
            report.add(rs.imagePlus(cv.getHistograms(), zoom2scale));
            
            report.add(rs.title(cv.nImg==1?"CV table:":"CVs table:"));
            report.add(rs.table(cv.tableForReport(), 75));

            report.close();

            if (saveData){
                String filename=path.substring(0, path.lastIndexOf(".pdf"));
                String outPath=filename + File.separator;
                filename=filename.substring(filename.lastIndexOf(File.separator));
                new File(outPath).mkdirs();
                cv.saveData(outPath, filename, mjd.scale);
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
