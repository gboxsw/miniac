package com.gboxsw.miniac;

/**
 * The {@code Cancellable} interface should be implemented by any class
 * representing a cancellable operation or action.
 */
public interface Cancellable {

	/**
	 * Executes a cancellation.
	 */
	public void cancel();

	/**
	 * Returns whether the operation or action was cancelled.
	 * 
	 * @return true, if the operation or action was cancelled, false otherwise.
	 */
	public boolean isCancelled();
}
