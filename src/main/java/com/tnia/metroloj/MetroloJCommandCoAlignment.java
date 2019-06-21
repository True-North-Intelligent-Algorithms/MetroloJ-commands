
package com.tnia.metroloj;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;
import metroloJ.coalignement.coAlignement;
import metroloJ.resolution.PSFprofiler;
import metroloJ.setup.microscope;

import java.io.IOException;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.Axis;
import net.imagej.space.DefaultCalibratedSpace;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>MetroJCoAlignment")
public class MetroloJCommandCoAlignment<T extends RealType<T> & NativeType<T>> implements Command {

	@Parameter
	ImgPlus<T> img;

	@Parameter(type = ItemIO.OUTPUT)
	Double xres;

	@Parameter(type = ItemIO.OUTPUT)
	Double yres;

	@Parameter(type = ItemIO.OUTPUT)
	Double zres;

	public void run() {
		// @ImagePlus imp

		System.out.println(img.numDimensions());
		System.out.println(img.dimension(2));
		System.out.println(img.axis(0).calibratedValue(1));
		//System.out.println(ip.axis(0).calibratedValue(1));
		
		System.out.println(img.axis(0).getClass());
		
		ImagePlus imp = ImageJFunctions.wrap(img, "wrapped");

		System.out.println(imp.getNFrames());
		System.out.println(imp.getNSlices());
		System.out.println(imp.getNChannels());

		imp.setDimensions(1, (int) img.dimension(2), 1);

		System.out.println(imp.getCalibration().pixelWidth);
		System.out.println(imp.getCalibration().pixelHeight);
		System.out.println(imp.getCalibration().pixelDepth);

		xres = .363;
		yres = .354;
		zres = .344;
		
		Axis ax=img.axis(1);

		System.out.println("axis info ----");
		for (int d = 0; d < img.numDimensions(); d++) {
			System.out.println(img.axis(d));
			System.out.println(img.dimension(d));
			System.out.println("");
		}

		int xIndex = img.dimensionIndex(Axes.X);
		int yIndex = img.dimensionIndex(Axes.Y);
		int zIndex = img.dimensionIndex(Axes.Z);
		int cIndex = img.dimensionIndex(Axes.CHANNEL);
		int tIndex = img.dimensionIndex(Axes.TIME);
	
		long xLen = img.dimension(img.dimensionIndex(Axes.X));
		long yLen = img.dimension(img.dimensionIndex(Axes.Y));
		long zLen = img.dimension(img.dimensionIndex(Axes.Z));
		long cLen = img.dimension(img.dimensionIndex(Axes.CHANNEL));
		long tLen = img.dimension(img.dimensionIndex(Axes.TIME));

		System.out.println("xIndex "+xIndex+" xLen "+xLen);
		System.out.println("yIndex "+yIndex+" yLen "+yLen);
		System.out.println("zIndex "+zIndex+" zLen "+zLen);
		System.out.println("cIndex "+cIndex+" cLen "+cLen);
		System.out.println("tIndex "+tIndex+" tLen "+tLen);
		
		ImagePlus test1 = ImageJFunctions.wrap(Views.hyperSlice(img, cIndex, 0), "channel 1");
	  test1.setCalibration(imp.getCalibration());
	  test1.setDimensions((int)cLen,(int) zLen, (int)tLen);
		ImagePlus test2 = ImageJFunctions.wrap(Views.hyperSlice(img, cIndex, 1), "channel 2");
	  test2.setCalibration(imp.getCalibration());	
	  test2.setDimensions((int)cLen,(int) zLen, (int)tLen);
		
	  int microType=0;
		int Wavelength1=400;
		int Wavelength2=500;
		double NA=1.4;
		double pinhole=1.0;
		String sampleInfos="test";
		String comments="test";
		
		microscope micro1=new microscope(test1.getCalibration(), microType, Wavelength1, NA, pinhole, sampleInfos, comments);
		microscope micro2=new microscope(test2.getCalibration(), microType, Wavelength2, NA, pinhole, sampleInfos, comments);
		
		ImagePlus[] ip= {test1, test2, null};
		microscope[] conditions= {micro1, micro2, null};
		
    coAlignement coa=new coAlignement(ip, conditions);
    
    System.out.println(coa.getPixShiftArray()[0][1]);
    System.out.println(coa.getPixShiftArray()[0][2]);
    
    
		// get all channels
		
		

		/*
				PSFprofiler profiler=new PSFprofiler(imp);
				
				xres=profiler.getResolutions()[0];
				yres=profiler.getResolutions()[1];
				zres=profiler.getResolutions()[2];
		
				System.out.println(profiler.getResolutions()[0]);
				System.out.println(profiler.getResolutions()[1]);
				System.out.println(profiler.getResolutions()[2]);
		*/
	}

	public static <T extends RealType<T> & NativeType<T>> void main(
		final String[] args) throws InterruptedException, IOException
	{

		// create an instance of imagej
		final ImageJ ij = new ImageJ();

		// launch it
		ij.launch(args);
		// load beads
		Dataset datasetBeads = (Dataset) ij.scifio().datasetIO().open(
			"/home/bnorthan/Knime/PSF Project/TestBeads/CHUM_CR_R12802_SDTIRF_coreg_2018_05_04_mai_40X_fovA.czi");
		
		datasetBeads.axis(0).calibratedValue(1);
		
		
		ij.ui().show(datasetBeads);
		
		ij.command().run(MetroloJCommandCoAlignment.class, true, "img", datasetBeads);
	}
}
