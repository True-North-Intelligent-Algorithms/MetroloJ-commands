/**
 *
 *  PSFprofilerReport v1, 17 déc. 2008
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

package metroloJ.report;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import java.io.*;

import metroloJ.report.utilities.ReportSections;
import metroloJ.resolution.*;
import metroloJ.setup.metroloJDialog;
import metroloJ.setup.microscope;
import metroloJ.utilities.tricks.dataTricks;
import metroloJ.utilities.sideViewGenerator;

/**
 * Generates the PSF (point spread function) report
 * @author fab
 */
public class PSFprofilerReport {
    /**Stores a reference to the metroloJDialog**/
    metroloJDialog mjd;

    /**Stores the microscope**/
    microscope microscope;

    /**Initializes a ReportSections object to be used later while building the report**/
    ReportSections rs=new ReportSections();

    /** Stores a reference to the PSFprofiler to be used in the report **/
    PSFprofiler pp;

    /** Stores a reference to the sideViewGenerator to be used in the report **/
    sideViewGenerator svg;

    
    /**
     * Starts the process of analysing PSF and creating a report
     * @param mjd the metroloJDialog to start working with
     */
    public PSFprofilerReport(metroloJDialog mjd){
        this.mjd=mjd;
        microscope=mjd.getMicroscope();
        pp=new PSFprofiler(mjd.ip);
        svg=new sideViewGenerator();
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

            report.add(rs.bigTitle(microscope.date+"\n"+"PSF profiler report on "+mjd.ip.getTitle()));

            report.add(rs.title("Profile view:"));

            ImagePlus img=svg.getPanelView(mjd.ip, sideViewGenerator.MAX_METHOD, true, true, mjd.scale, false, null, 0);
            
            float zoom2scaleTo256pxMax=25600/Math.max(img.getWidth(), img.getHeight());
            report.add(rs.imagePlus(img, zoom2scaleTo256pxMax));


            report.add(rs.title("Microscope infos:"));
            report.add(rs.paragraph(microscope.reportHeader));

            report.add(rs.title("Resolution table:"));

            String[][] content={{"", "x", "y", "z"},{"FWHM", dataTricks.round(pp.getResolutions()[0], 3)+" "+pp.getUnit(), dataTricks.round(pp.getResolutions()[1], 3)+" "+pp.getUnit(), dataTricks.round(pp.getResolutions()[2], 3)+" "+pp.getUnit()}, {"Theoretical resolution", dataTricks.round(microscope.resolution[0], 3)+" µm", dataTricks.round(microscope.resolution[1], 3)+" µm", dataTricks.round(microscope.resolution[2], 3)+" µm"}};
            report.add(rs.table(content, 50));

            report.newPage();

            report.add(rs.title("X profile & fitting parameters:"));
            Image image=rs.imagePlus(pp.getXplot().getImagePlus(), 100);
            image.setAlignment(Image.ALIGN_LEFT | Image.TEXTWRAP);
            report.add(image);
            report.add(rs.paragraph(pp.getXParams()));
            report.add(rs.paragraph("\n"));

            report.add(rs.title("Y profile & fitting parameters:"));
            image=rs.imagePlus(pp.getYplot().getImagePlus(), 100);
            image.setAlignment(Image.ALIGN_LEFT | Image.TEXTWRAP);
            report.add(image);
            report.add(rs.paragraph(pp.getYParams()));
            report.add(rs.paragraph("\n"));

            report.add(rs.title("Z profile & fitting parameters:"));
            image=rs.imagePlus(pp.getZplot().getImagePlus(), 100);
            image.setAlignment(Image.ALIGN_LEFT | Image.TEXTWRAP);
            report.add(image);
            report.add(rs.paragraph(pp.getZParams()));
            report.add(rs.paragraph("\n"));

            if (!microscope.sampleInfos.equals("") || !microscope.comments.equals("")) report.newPage();

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
                String filename=path.substring(0, path.lastIndexOf(".pdf")!=-1?path.lastIndexOf(".pdf"):path.length());
                String outPath=filename + File.separator;
                filename=filename.substring(filename.lastIndexOf(File.separator));
                new File(outPath).mkdirs();
                pp.saveProfiles(outPath, filename);
                pp.savePlots(outPath, filename);
                pp.saveSummary(outPath, filename, microscope);
                new FileSaver(img).saveAsJpeg(outPath+filename+"_panel-view.jpg");
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
