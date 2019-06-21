/**
 *
 *  PSFprofilerReport v1, 23 mars 2009
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
import ij.io.FileSaver;
import java.io.*;

import metroloJ.coalignement.coAlignement;
import metroloJ.report.utilities.ReportSections;
import metroloJ.setup.microscope;
import metroloJ.utilities.sideViewGenerator;

/**
 * Generates the co-alignement report (2 to 3 channels)
 * @author fab
 */
public class coAlignementReport {
    /**Stores the microscope infos & acquisition conditions for the 2 or 3 channels**/
    microscope[] micro;

    /**Initializes a ReportSections object to be used later while building the report**/
    ReportSections rs=new ReportSections();

    /** Stores a reference to the coAlignement to be used in the report **/
    coAlignement coa;

    /** Stores a reference to the sideViewGenerator to be used in the report **/
    sideViewGenerator svg;

    /**Title to be used on the report**/
    String title="";

    
    /**
     * Starts the process of analysing co-alignement and creating a report
     * @param ip ImagePlus to analyse
     * @param conditions microscope conditions (usufull to store the wavelengths)
     * @param title title to be used on the report
     */
    public coAlignementReport(ImagePlus[] ip, microscope[] conditions, String title){
        micro=conditions;
        coa=new coAlignement(ip, conditions);
        svg=new sideViewGenerator();
        this.title=micro[0].date+"\n"+"Co-Alignement report";
        if (!title.equals("")) this.title+="\n"+title;
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

            report.add(rs.bigTitle(this.title));

            report.add(rs.title("Profile view:"));

            ImagePlus img=coa.getSideView();
            
            float zoom2scaleTo256pxMax=25600/Math.max(img.getWidth(), img.getHeight());
            report.add(rs.imagePlus(img, zoom2scaleTo256pxMax));

            report.add(rs.title("Microscope infos:"));
            report.add(rs.paragraph(coa.microSection));

            report.newPage();

            report.add(rs.title("Pixel shift table:"));
            report.add(rs.table(coa.getPixShiftArray(), 85));
            
            report.add(rs.title("Distances table (uncalibrated):"));
            report.add(rs.table(coa.getUnCalDistArray(), 85));

            report.add(rs.title("Distances table (calibrated):"));
            report.add(rs.table(coa.getCalDistArray(), 85));
            
            if (!micro[0].sampleInfos.equals("") || !micro[0].comments.equals("")) report.newPage();

            if (!micro[0].sampleInfos.equals("")){
                report.add(rs.title("Sample infos:"));
                report.add(rs.paragraph(micro[0].sampleInfos));
            }

            if (!micro[0].comments.equals("")){
                report.add(rs.title("Comments:"));
                report.add(rs.paragraph(micro[0].comments));
            }

            report.close();

            if (saveData){
                String filename=path.substring(0, path.lastIndexOf(".pdf"));
                String outPath=filename + File.separator;
                filename=filename.substring(filename.lastIndexOf(File.separator));
                new File(outPath).mkdirs();
                new FileSaver(img).saveAsJpeg(outPath+filename+"_panel-view.jpg");
                coa.saveData(outPath, filename);
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
