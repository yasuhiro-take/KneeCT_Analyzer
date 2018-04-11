package imagejplugin.kneectanalyzer;

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ImageProcessor;
import ij.plugin.frame.RoiManager;

class AnalyzeParticle implements Measurements {
	protected ResultsTable rt;
	protected ParticleAnalyzer analyzer;
	protected RoiManager rois;
	private String rtLabel;
	private int[] rtIndex;
	static final int defaultOption = ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES | ParticleAnalyzer.INCLUDE_HOLES;
	static final int defaultMeasurements =  AREA | CENTROID | RECT | SLICE | STACK_POSITION;
	
	private void commonInit(int option, int measurements, double minSize, double maxSize, double minC, double maxC, Calibration cal, String rtLabel) {
		double minSizePx = minSize / (cal.pixelWidth * cal.pixelHeight);
		double maxSizePx = maxSize / (cal.pixelWidth * cal.pixelHeight);
		
		if ((option & ParticleAnalyzer.ADD_TO_MANAGER) != 0) {
			rois = new RoiManager(false);
			rois.setVisible(false);
			ParticleAnalyzer.setRoiManager(rois);
		} else
			rois = null;
		
		rt = new ResultsTable();
		analyzer = new ParticleAnalyzer(option, measurements, rt, minSizePx, maxSizePx, minC, maxC);
		rtIndex = null;
		this.rtLabel = rtLabel;
	}
	
	public AnalyzeParticle(int measurements, double minSize, Calibration cal, String rtLabel) {
		int o = defaultOption;
		int m = defaultMeasurements | measurements;
		double maxSize = Double.POSITIVE_INFINITY;
		
		commonInit(o, m, minSize, maxSize, 0, 1, cal, rtLabel);
	}
	
	public AnalyzeParticle(int option, int measurements, double minSize, Calibration cal, String rtLabel) {
		int m = defaultMeasurements | measurements;
		
		double maxSize = Double.POSITIVE_INFINITY;
		
		commonInit(option, m, minSize, maxSize, 0, 1, cal, rtLabel);
	}
	
	public AnalyzeParticle(int option, int measurements, double minSize, double maxSize, Calibration cal, String rtLabel) {
		int m = defaultMeasurements | measurements;
		
		commonInit(option, m, minSize, maxSize, 0, 1, cal, rtLabel);
	}
	
	public AnalyzeParticle(int option, int measurements, double minSize, double maxSize, double minC, double maxC, Calibration cal, String rtLabel) {
		int m = defaultMeasurements | measurements;
		
		commonInit(option, m, minSize, maxSize, minC, maxC, cal, rtLabel);
	}
	
	public AnalyzeParticle(double minSize, Calibration cal, String rtLabel) {
		int o = defaultOption;
		int m = defaultMeasurements;
		double maxSize = Double.POSITIVE_INFINITY;
		
		commonInit(o, m, minSize, maxSize, 0, 1, cal, rtLabel);
	}
	
	public AnalyzeParticle(double minSize, double maxSize, Calibration cal, String rtLabel) {
		int o = defaultOption;
		int m = defaultMeasurements;
		
		commonInit(o, m, minSize, maxSize, 0, 1, cal, rtLabel);
	}
	
	public void analyze(ImagePlus imp, ImageProcessor ip) {
		this.analyzer.analyze(imp, ip);
		
		if (this.rt.size() > 0 && this.rtIndex == null && this.rtLabel != null)
			setLabelsForReturn(this.rtLabel);
	}
	
	public void analyze(ImagePlus imp, int slice) {
		ImageProcessor ip = imp.getStack().getProcessor(slice);
		this.analyze(imp, ip);
	}
	
	public void analyze(ImagePlus imp, ImageProcessor ip, double minTh, double maxTh) {
		ip.setThreshold(minTh, maxTh, ImageProcessor.NO_LUT_UPDATE);
		this.analyze(imp, ip);
		
	}
	
	public int newAnalyze(ImagePlus imp, int slice) {
		if (this.rois != null) this.rois.reset();
		this.rt.reset();
		this.analyze(imp, slice);
		return this.rt.size();
	}
	
	public int newAnalyze(ImagePlus imp, ImageProcessor ip) {
		if (this.rois != null) this.rois.reset();
		this.rt.reset();
		this.analyze(imp, ip);
		return this.rt.size();
	}
	
	public int newAnalyze(ImagePlus imp, ImageProcessor ip, double minTh, double maxTh) {
		if (this.rois != null) this.rois.reset();
		this.rt.reset();
		this.analyze(imp, ip, minTh, maxTh);
		return this.rt.size();
	}
	
	public void setLabelsForReturn(String labels) {
		String l[] = labels.split(" ");
		
		rtIndex = new int[l.length];
		for (int i = 0; i < l.length; i++)
			rtIndex[i] = rt.getColumnIndex(l[i]);
	}
	
	public boolean hasLabelsForReturn() {
		return (rtIndex != null);
	}
	
	public double[] getMultiColumnValues(int index_from, int index_to, int row) {
		double ret[] = new double[index_to - index_from + 1];
		for (int i = index_from; i <= index_to; i++)
			ret[i- index_from] = rt.getValueAsDouble(rtIndex[i], row);
			
		return ret;
	}
	public double[] getAllColumnValues(int row) {
		return getMultiColumnValues(0, rtIndex.length - 1, row);
	}
	
	public double getValue(int index, int row) {
		return rt.getValueAsDouble(rtIndex[index], row);
	}
	
	public double[] getMultiRowValues(int index, int row_from, int row_to) {
		double ret[] = new double[row_to - row_from + 1];
		for (int i = row_from; i <= row_to; i++)
			ret[i - row_from] = rt.getValueAsDouble(rtIndex[index], i);
		
		return ret;
	}
	
	public double[] getAllRowValues(int index) {
		return getMultiRowValues(index, 0, rt.size() - 1);
	}
	
	 public void setMultiColumnValues(int index_from, int index_to, int row, int v) {
		 for (int i = index_from; i <= index_to; i++)
			 rt.setValue(i, row, v);
	 }
}
