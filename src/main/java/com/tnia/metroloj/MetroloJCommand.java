package com.tnia.metroloj;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;
import metroloJ.resolution.PSFprofiler;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>MetroJ")
public class MetroloJCommand implements Command {
	
	@Parameter
	ImgPlus img;
	
	@Parameter(type = ItemIO.OUTPUT)
	Double xres;
	
	@Parameter(type = ItemIO.OUTPUT)
	Double yres;

	@Parameter(type = ItemIO.OUTPUT)
	Double zres;

	public void run() {
		// @ImagePlus imp

		System.out.println(img.dimension(2));
				
		ImagePlus imp=ImageJFunctions.wrap(img, "wrapped");
		
		System.out.println(imp.getNFrames());
		System.out.println(imp.getNSlices());
		System.out.println(imp.getNChannels());
		
		imp.setDimensions(1, (int)img.dimension(2), 1);
		
		System.out.println(imp.getCalibration().pixelWidth);
		System.out.println(imp.getCalibration().pixelHeight);
		System.out.println(imp.getCalibration().pixelDepth);
		

		PSFprofiler profiler=new PSFprofiler(imp);
		
		xres=profiler.getResolutions()[0];
		yres=profiler.getResolutions()[1];
		zres=profiler.getResolutions()[2];

		System.out.println(profiler.getResolutions()[0]);
		System.out.println(profiler.getResolutions()[1]);
		System.out.println(profiler.getResolutions()[2]);
		
	}
	
	public static <T extends RealType<T> & NativeType<T>> void main(final String[] args) throws InterruptedException {

		// create an instance of imagej
		final ImageJ ij = new ImageJ();

		// launch it
		ij.launch(args);
	}
}
