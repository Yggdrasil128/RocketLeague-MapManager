package de.yggdrasil128.rocketleague.mapmanager.tools;

import org.jetbrains.annotations.NotNull;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicLong;

public class ProgressInputStream extends FilterInputStream {
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#0.0");
	public static final long MEBIBYTE_SIZE = 1024 * 1024;
	private final AtomicLong totalNumBytesRead = new AtomicLong(0);
	private final long totalSize;
	private final ExponentialMovingAverage ema;
	private long lastProgress = 0, lastTimestamp = 0;
	
	public ProgressInputStream(InputStream in, long totalSize) {
		super(in);
		this.totalSize = totalSize;
		ema = new ExponentialMovingAverage(0.3);
	}
	
	public long getTotalNumBytesRead() {
		// for some reason, each byte is counted twice
		// this is the "simple, but it works (tm)" solution
		// ¯\_(ツ)_/¯
		return totalNumBytesRead.get() / 2;
	}
	
	public String getStatusString() {
		final long now = System.nanoTime();
		final long progress = getTotalNumBytesRead();
		final long progressCapped = totalSize > 0 ? Math.min(progress, totalSize) : progress;
		
		String status = DECIMAL_FORMAT.format((double) progressCapped / MEBIBYTE_SIZE);
		status += " / ";
		if(totalSize <= 0) {
			status += "?";
		} else {
			status += DECIMAL_FORMAT.format((double) totalSize / MEBIBYTE_SIZE);
		}
		status += " MiB";
		
		if(totalSize > 0) {
			status += " (";
			status += DECIMAL_FORMAT.format(100d * progressCapped / totalSize);
			status += " %)";
		}
		
		if(lastTimestamp != 0) {
			long timeDiff = now - lastTimestamp;
			double speed;
			if(timeDiff / 1000000 > 400) {
				// require at least 400ms between speed updates
				speed = ((double) (progress - lastProgress)) / (now - lastTimestamp) / MEBIBYTE_SIZE * 1000000000;
				speed = ema.average(speed);
				
				lastTimestamp = now;
				lastProgress = progress;
			} else {
				speed = ema.getCurrent();
			}
			
			status += ", ";
			status += DECIMAL_FORMAT.format(speed);
			status += " MiB/s";
			
			if(totalSize > 0 && speed > 0) {
				long s = Math.round((double) (totalSize - progressCapped) / speed / MEBIBYTE_SIZE);
				long h = s / 3600;
				s -= h * 3600;
				long m = s / 60;
				s -= m * 60;
				
				if(h > 0) {
					status += ", " + h + "h " + m + "m " + s + "s left";
				} else if(m > 0) {
					status += ", " + m + "m " + s + "s left";
				} else {
					status += ", " + s + "s left";
				}
			}
		} else {
			lastTimestamp = now;
			lastProgress = progress;
		}
		
		return status;
	}
	
	@Override
	public int read() throws IOException {
		int b = super.read();
		updateProgress(1);
		return b;
	}
	
	@Override
	public int read(byte @NotNull [] b) throws IOException {
		return (int) updateProgress(super.read(b));
	}
	
	@Override
	public int read(byte @NotNull [] b, int off, int len) throws IOException {
		return (int) updateProgress(super.read(b, off, len));
	}
	
	@Override
	public long skip(long n) throws IOException {
		return updateProgress(super.skip(n));
	}
	
	@Override
	public void mark(int readLimit) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void reset() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean markSupported() {
		return false;
	}
	
	private long updateProgress(long numBytesRead) {
		if(numBytesRead > 0) {
			totalNumBytesRead.addAndGet(numBytesRead);
		}
		
		return numBytesRead;
	}
}
