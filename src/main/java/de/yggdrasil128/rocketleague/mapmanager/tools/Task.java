package de.yggdrasil128.rocketleague.mapmanager.tools;

import com.google.gson.JsonObject;

import java.text.DecimalFormat;

import static de.yggdrasil128.rocketleague.mapmanager.config.Config.GSON;

public abstract class Task {
	public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#0.0 %");
	private final Thread thread;
	protected String statusMessage = null;
	protected int progress = 0, progressTarget = 0;
	protected boolean showProgress = false, showPercentage = false;
	private Status status = Status.INITIALIZING;
	private Exception exception = null;
	
	public Task() {
		thread = new Thread(() -> {
			status = Status.RUNNING;
			try {
				run();
			} catch(InterruptedException e) {
				status = Status.CANCELLED;
				return;
			} catch(Exception e) {
				status = Status.ERROR;
				exception = e;
				return;
			}
			status = Status.DONE;
		});
	}
	
	public void start() {
		if(status != Status.INITIALIZING) {
			throw new IllegalStateException();
		}
		
		thread.start();
	}
	
	public boolean isRunning() {
		return status == Status.INITIALIZING || status == Status.RUNNING;
	}
	
	public boolean isFinished() {
		return !isRunning();
	}
	
	public void cancel() {
		thread.interrupt();
	}
	
	protected abstract void run() throws Exception;
	
	protected void resetProgress() {
		progress = 0;
		progressTarget = 0;
	}
	
	public String getStatusJson() {
		JsonObject json = new JsonObject();
		
		boolean isFinished = isFinished();
		String message = "";
		switch(status) {
			case INITIALIZING:
				message = "Initializing...";
				break;
			case RUNNING:
				message = statusMessage == null ? "Running..." : statusMessage;
				if(progressTarget > 0) {
					double progressFloat = (double) progress / progressTarget;
					
					json.addProperty("progress", progress);
					json.addProperty("progressTarget", progressTarget);
					json.addProperty("progressFloat", progressFloat);
					
					if(showProgress) {
						message += " " + progress + " / " + progressTarget;
						
						if(showPercentage) {
							message += " (" + DECIMAL_FORMAT.format(progressFloat) + ")";
						}
					} else if(showPercentage) {
						message += " " + DECIMAL_FORMAT.format(progressFloat);
					}
				}
				break;
			case CANCELLED:
				message = "Cancelled.";
				break;
			case ERROR:
				message = "Error: ";
				if(exception.getMessage() != null) {
					message += exception.getMessage();
				} else {
					message += exception.toString();
				}
				break;
			case DONE:
				message = "Done.";
				if(statusMessage != null) {
					message += " " + statusMessage;
				}
				break;
		}
		
		json.addProperty("isFinished", isFinished);
		json.addProperty("message", message);
		
		return GSON.toJson(json);
	}
	
	private enum Status {
		INITIALIZING,
		RUNNING,
		CANCELLED,
		ERROR,
		DONE;
	}
}
