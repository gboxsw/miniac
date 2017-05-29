package com.gboxsw.miniac;

import java.util.Map;

/**
 * Interface of a persistent storage providing methods to save and load named
 * bundles of values.
 */
public interface PersistentStorage {

	/**
	 * Loads named bundles as a map from the storage.
	 * 
	 * @return the loaded bundles or null, if the loading fails.
	 */
	public Map<String, Bundle> loadBundles();

	/**
	 * Saves named bundles in the persistent storage.
	 * 
	 * @param bundles
	 *            the map containing named bundles.
	 */
	public void saveBundles(Map<String, Bundle> bundles);

}
