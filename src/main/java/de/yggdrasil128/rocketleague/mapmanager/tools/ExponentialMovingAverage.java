package de.yggdrasil128.rocketleague.mapmanager.tools;

class ExponentialMovingAverage {
	private final double alpha;
	private Double oldValue;
	
	public ExponentialMovingAverage(double alpha) {
		this.alpha = alpha;
	}
	
	public double average(double value) {
		if(oldValue == null) {
			oldValue = value;
			return value;
		}
		double newValue = oldValue + alpha * (value - oldValue);
		oldValue = newValue;
		return newValue;
	}
	
	public double getCurrent() {
		return oldValue == null ? 0 : oldValue;
	}
}
