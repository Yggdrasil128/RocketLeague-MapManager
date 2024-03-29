package de.yggdrasil128.rocketleague.mapmanager.tools;

import com.google.gson.JsonObject;
import org.slf4j.Logger;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

import static de.yggdrasil128.rocketleague.mapmanager.config.Config.GSON;

public abstract class Task {
	public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#0.0 %");
	private final Thread thread;
	private final List<Runnable> onFinishRunnables = new LinkedList<>();
	protected String statusMessage = null;
	protected long progress = 0, progressTarget = 0;
	protected boolean showProgress = false, showPercentage = false;
	private State state = State.INITIALIZING;
	private Exception exception = null;
	
	public Task() {
		thread = new Thread(() -> {
			state = State.RUNNING;
			try {
				run();
			} catch(InterruptedException e) {
				state = State.CANCELLED;
				return;
			} catch(Exception e) {
				state = State.ERROR;
				exception = e;
				if(getLogger() != null) {
					getLogger().warn("Uncaught exception", e);
				}
				return;
			} finally {
				cleanup();
				for(Runnable runnable : onFinishRunnables) {
					try {
						runnable.run();
					} catch(Exception e) {
						if(getLogger() != null) {
							getLogger().warn("Uncaught exception in an onFinish runnable", e);
						}
					}
				}
			}
			state = State.DONE;
		});
	}
	
	public void registerOnFinishRunnable(Runnable runnable) {
		if(state != State.INITIALIZING) {
			throw new IllegalStateException();
		}
		if(runnable != null) {
			onFinishRunnables.add(runnable);
		}
	}
	
	public void start() {
		if(state != State.INITIALIZING) {
			throw new IllegalStateException();
		}
		
		thread.start();
	}
	
	public boolean isFinished() {
		return state.isFinished;
	}
	
	public boolean isRunning() {
		return !isFinished();
	}
	
	public void cancel() {
		if(state != State.INITIALIZING && state != State.RUNNING) {
			throw new IllegalStateException();
		}
		state = State.CANCELLING;
		onCancel();
	}
	
	protected void checkIfTaskIsCancelled() throws InterruptedException {
		if(isCancelled()) {
			throw new InterruptedException();
		}
	}
	
	protected void onCancel() {
		thread.interrupt();
	}
	
	protected abstract void run() throws Exception;
	
	protected void cleanup() {
		
	}
	
	protected boolean isCancelled() {
		return state == State.CANCELLING;
	}
	
	protected void resetProgress() {
		showProgress = false;
		showPercentage = false;
		progress = 0;
		progressTarget = 0;
	}
	
	protected void beforeStatusQuery() {
		
	}
	
	protected Logger getLogger() {
		return null;
	}
	
	public String getStatusJson() {
		JsonObject json = new JsonObject();
		String message = getStatus(json);
		
		json.addProperty("isFinished", isFinished());
		json.addProperty("message", message);
		
		return GSON.toJson(json);
	}
	
	public String getStatusMessage() {
		return getStatus(null);
	}
	
	private String getStatus(JsonObject json) {
		String message = "";
		switch(state) {
			case INITIALIZING:
				message = "Initializing...";
				break;
			case RUNNING:
				beforeStatusQuery();
				
				message = statusMessage == null ? "Running..." : statusMessage;
				if(progressTarget > 0) {
					double progressFloat = (double) progress / progressTarget;
					if(progressFloat > 1) {
						progressFloat = 1;
					}
					
					if(json != null) {
						json.addProperty("progress", progress);
						json.addProperty("progressTarget", progressTarget);
						json.addProperty("progressFloat", progressFloat);
					}
					
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
			case CANCELLING:
				message = "Cancelling...";
				break;
			case CANCELLED:
				message = "Cancelled.";
				break;
			case ERROR:
				message = "Error: " + exception.toString();
				break;
			case DONE:
				message = statusMessage == null ? "Done." : statusMessage;
				break;
		}
		
		return message;
	}
	
	private enum State {
		INITIALIZING(false),
		RUNNING(false),
		CANCELLING(false),
		CANCELLED(true),
		ERROR(true),
		DONE(true);
		
		private final boolean isFinished;
		
		State(boolean isFinished) {
			this.isFinished = isFinished;
		}
	}
}
