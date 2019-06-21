/**
 *
 *  doCheck v1, 15 oct. 2009
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

package metroloJ.utilities;

import ij.IJ;
import ij.WindowManager;

/**
 * doCheck performs tests an return the result as a boolean
 * @author fab
 */
public class doCheck {
    /**
     * Checks the current ImageJ's version
     * @return true if ImageJ's version is 1.43h or above
     */
    public static boolean isVersionUpToDate(){
        return !IJ.versionLessThan("1.43h");
    }
    
    /**
     * Checks if at least one image is opened
     * @param showMessage if true, when the test returns false, a message will be shown
     * @return true if at least one image is opened
     */
    public static boolean isThereAnImage(boolean showMessage){
        if (WindowManager.getImageCount()!=0){
            return true;
        }else{
            if (showMessage) IJ.error("Please, open an image first...");
            return false;
        }
    }

    /**
     * Checks if at least one image is opened. When the test returns false, a message will be shown
     * @return true if at least one image is opened
     */
    public static boolean isThereAnImage(){
        return isThereAnImage(true);
    }
    
    /**
     * Checks if the current image is a stack
     * @param showMessage if true, when the test returns false, a message will be shown
     * @return true if the current image is a stack
     */
    public static boolean isStack(boolean showMessage){
        if (WindowManager.getImageCount()!=0 && WindowManager.getCurrentImage().getNSlices()!=1){
            return true;
        }else{
            if (showMessage) IJ.error("The image is expected to be a stack...");
            return false;
        }
    }

    /**
     * Checks if the current image is a stack. When the test returns false, a message will be shown
     * @return true if the current image is a stack
     */
    public static boolean isStack(){
        return isStack(true);
    }

    /**
     * Checks if the current image is calibrated
     * @param showMessage if true, when the test returns false, a message will be shown
     * @return true if the current image is calibrated
     */
    public static boolean isCalibrated(boolean showMessage){
        if (WindowManager.getImageCount()!=0 && !WindowManager.getCurrentImage().getCalibration().getUnit().equals("pixel")){
            return true;
        }else{
            if (showMessage) IJ.error("The image is expected to be calibrated...");
            IJ.run("Properties...");
            return false;
        }
    }

    /**
     * Checks if the current image is calibrated. When the test returns false, a message will be shown
     * @return true if the current image is calibrated
     */
    public static boolean isCalibrated(){
        return isCalibrated(true);
    }

    /**
     * Checks if at least n stacks are opened
     * @param n number of stacks that should be found to consider the test successfull
     * @param showMessage if true, when the test returns false, a message will be shown
     * @return true if at least n stacks are opened
     */
    public static boolean atLeastNOpenedStacks(int n, boolean showMessage){
        int nStacks=0;
        int[] idList=WindowManager.getIDList();
        if (idList!=null){
            for (int i=0; i<idList.length; i++){
                if (WindowManager.getImage(idList[i]).getNSlices()!=1) nStacks++;
            }
        }

        if (nStacks>=n){
            return true;
        }else{
            if (showMessage) IJ.error("At least "+n+" images should be opened...");
            return false;
        }
    }

    /**
     * Checks if at least n stacks are opened. When the test returns false, a message will be shown
     * @param n number of stacks that should be found to consider the test successfull
     * @return true if at least n stacks are opened
     */
    public static boolean atLeastNOpenedStacks(int n){
        return atLeastNOpenedStacks(n, true);
    }

    /**
     * Checks if the current image is a 8- or 16-bits image
     * @param showMessage if true, when the test returns false, a message will be shown
     * @return true if the current image is a 8- or 16-bits image
     */
    public static boolean isNoMoreThan16bits(boolean showMessage){
        if (WindowManager.getImageCount()!=0 && WindowManager.getCurrentImage().getBitDepth()<=16){
           return true;
        }else{
            if (showMessage) IJ.error("The image is expected to be 8- or 16-bits...");
            return false;
        }
    }

    /**
     * Checks if the current image is a 8- or 16-bits image. When the test returns false, a message will be shown
     * @return true if the current image is a 8- or 16-bits image
     */
    public static boolean isNoMoreThan16bits(){
        return isNoMoreThan16bits(true);
    }
}