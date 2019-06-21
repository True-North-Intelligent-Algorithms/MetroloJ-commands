/**
 *
 *  fileTricks v1, 15 oct. 2009
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

package metroloJ.utilities.tricks;

import ij.IJ;
import ij.gui.Roi;
import ij.io.RoiEncoder;
import java.io.*;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * fileTricks contains commonly used tools to work on files
 * @author fab
 */
public class fileTricks {
    /**
     * Saves a String to a text file
     * @param content the String to save
     * @param path the file to which the String will be saved
     */
    public static void save(String content, String path){
        try {
            BufferedWriter file = new BufferedWriter(new FileWriter(path));
            file.write(content, 0, content.length());
            file.close();
        } catch (IOException ex) {
            Logger.getLogger(fileTricks.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Saves the ROI to the provided path
     * @param roi the ROI to save
     * @param path where the file should be saved
     */
    public static void saveRoi(Roi roi, String path){
        try {
            RoiEncoder re = new RoiEncoder(path);
            re.write(roi);
        }
        catch (IOException e) {
            System.out.println("Can't save roi");
        }
    }


    /**
     * Saves the ROIs contained in the array a zip file
     * @param rois the ROI array to save
     * @param path where the file should be saved
     */
    public static void saveRois(Roi[] rois, String path){
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(path));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
            RoiEncoder re = new RoiEncoder(out);
            for (int i=0; i<rois.length; i++) {
                Roi roi = rois[i];
                if (roi!=null){
                    String label = roi.getName();
                    if (!label.endsWith(".roi")) label += ".roi";
                    zos.putNextEntry(new ZipEntry(label));
                    re.write(roi);
                    out.flush();
                }
            }
            out.close();
        }
        catch (IOException e) {
            System.out.println("Can't save rois");
        }
    }

    /**
     * Opens a file a return it content as a Vector of String arrays. Each Vector
     * element corresponds to a line within the file, each String array corresponding
     * to a "tabulation" splited version of the line.
     * @param path path to the file to be opened
     * @return a Vector of String arrays filled with the file's content
     */
    public static Vector<String[]> load(String path){
        Vector<String[]> out = new Vector<String[]>();
        try {
           BufferedReader file = new BufferedReader(new FileReader(path));
           String line=file.readLine();
           while (line!=null){
               out.add(line.split("\t"));
               line=file.readLine();
           }
           file.close();
        } catch (IOException ex) {
                Logger.getLogger(fileTricks.class.getName()).log(Level.SEVERE, null, ex);
        }
        return out;
    }

    /**
     * Opens the pdf file located at the indicated path
     * @param path path to the pdf file to display
     */
    public static void showPdf(String path){
        if (IJ.isWindows()) try {
            String cmd="rundll32 url.dll,FileProtocolHandler "+"\""+path+"\"";
            Runtime.getRuntime().exec(cmd);
        } catch (IOException ex) {
            Logger.getLogger(fileTricks.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (IJ.isMacintosh() || IJ.isLinux()) try {
            String[] cmd={"open", path};
            Runtime.getRuntime().exec(cmd);
        } catch (IOException ex) {
            Logger.getLogger(fileTricks.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
