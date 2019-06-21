/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package metroloJ.resolution;

/**
 *
 * @author fab
 */
public class resolutionCalculator {
    public final int WIDEFIELD=0;
    public final int CONFOCAL=1;

    double[] resolutions={0, 0, 0};

    /**
     * Creates a new resolutionCalculator
     * @param microscope microscope type as an Integer (0= widefield, 1= confocal)
     * @param wavelength emission wavelength, as a Double, expressed in nanometers
     * @param NA numerical aperture
     */
    public resolutionCalculator(int microscope, double wavelength, double NA){
        wavelength/=1000;
        switch (microscope){
            case 0: resolutions[0]=0.61*wavelength/NA; resolutions[1]=resolutions[0]; resolutions[2]=2*wavelength/Math.pow(NA,2); break;
            case 1: resolutions[0]=0.4*wavelength/NA; resolutions[1]=resolutions[0]; resolutions[2]=1.4*wavelength/Math.pow(NA,2); break;
        }
    }

    /**
     * Returns the calculated resolutions
     * @return the calculated resolutions as an array of 3 Doubles (x, y and z resolution)
     */
    public double[] getResolutions(){
        return resolutions;
    }

}
