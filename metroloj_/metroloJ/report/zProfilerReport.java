/**
 *
 *  zProfilerReport v1, 18 avr. 2009
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

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfWriter;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import metroloJ.report.utilities.ReportSections;
import metroloJ.resolution.zProfiler;
import metroloJ.setup.metroloJDialog;
import metroloJ.setup.microscope;

/**
 * Generates the Z profile report (2 to 3 channels)
 * @author fab
 */
public class zProfilerReport {
    /**Stores a reference to the metroloJDialog**/
    metroloJDialog mjd;

    /**Stores the microscope infos & acquisition conditions **/
    microscope microscope;

    /**Initializes a ReportSections object to be used later while building the report**/
    ReportSections rs=new ReportSections();

    /** Stores a reference to the coAlignement to be used in the report **/
    zProfiler zProf;

    /**Title to be used on the report**/
    String title="";

    /**
     * Starts the process of analysing a Z profile and creating a report
     * @param mjd the metroloJDialog to start working with
     */
    public zProfilerReport(metroloJDialog mjd){
        this.mjd=mjd;
        microscope=mjd.getMicroscope();
        zProf=new zProfiler(mjd.ip, mjd.ip.getRoi());
        title=microscope.date+"\n"+"Axial resolution report on "+mjd.ip.getTitle();
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

            report.add(rs.bigTitle(title));

            report.add(rs.title("Profile view:"));

            ImagePlus img=zProf.getImage(true, true);

            float zoom2scaleTo256pxMax=25600/Math.max(img.getWidth(), img.getHeight());
            report.add(rs.imagePlus(img, zoom2scaleTo256pxMax));


            report.add(rs.title("Microscope infos:"));
            report.add(rs.paragraph(microscope.reportHeader));

            report.add(rs.title("Resolution table:"));
            report.add(rs.paragraph(zProf.getRoiAsString()));
            report.add(rs.table(zProf.getSummary(microscope), 50));
            
            report.newPage();
            
            report.add(rs.title("Z profile & fitting parameters:"));
            Image image=rs.imagePlus(zProf.getProfile().getImagePlus(), 100);
            image.setAlignment(Image.ALIGN_LEFT | Image.TEXTWRAP);
            report.add(image);
            report.add(rs.paragraph(zProf.getParams()));
            report.add(rs.paragraph("\n"));

            if (!microscope.sampleInfos.equals("")){
                report.add(rs.title("Sample infos:"));
                report.add(rs.paragraph(microscope.sampleInfos));
            }

            if (!microscope.comments.equals("")){
                report.add(rs.title("Comments:"));
                report.add(rs.paragraph(microscope.comments));
            }

            report.close();

            if (saveData){
                String filename=path.substring(0, path.lastIndexOf(".pdf"));
                String outPath=filename + File.separator;
                filename=filename.substring(filename.lastIndexOf(File.separator));
                new File(outPath).mkdirs();
                new FileSaver(img).saveAsJpeg(outPath+filename+"_view.jpg");
                zProf.saveProfile(outPath, filename);
                zProf.saveSummary(outPath, filename, microscope);
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
