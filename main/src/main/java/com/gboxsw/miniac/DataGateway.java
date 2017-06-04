package com.gboxsw.miniac;

import java.util.*;
import java.util.logging.*;

import com.gboxsw.miniac.Application.TopicFilter;

/**
 * Gateway managing a collection of data items.
 */
public final class DataGateway extends Gateway {

	/**
	 * Logger.
	 */
	private static final Logger logger = Logger.getLogger(DataGateway.class.getName());

	/**
	 * Extended record of a data item.
	 */
	private static final class DataItemHolder {
		/**
		 * The underlying data item.
		 */
		private final DataItem<?> dataItem;

		/**
		 * Parsed identifier of the data item.
		 */
		private final String[] parsedId;

		/**
		 * The number of active subscriptions.
		 */
		private int subscriptionCount = 0;

		/**
		 * Constructs the record of a data item.
		 * 
		 * @param dataItem
		 */
		private DataItemHolder(String id, DataItem<?> dataItem) {
			this.dataItem = dataItem;
			this.parsedId = Application.parseTopicHierarchy(id);
		}
	}

	/**
	 * Data item holders.
	 */
	private final Map<String, DataItemHolder> dataItemHolders = new HashMap<>();

	/**
	 * Data items with subscription.
	 */
	private final Set<String> subscribedDataItems = new HashSet<>();

	/**
	 * Set of active (subscribed) topic filters.
	 */
	private final Set<String> subscribedTopicFilters = new HashSet<>();

	/**
	 * The data item that is currently activated.
	 */
	private volatile DataItem<?> activatingDataItem;

	/**
	 * Adds the data item to the gateway. The method invocation is synchronized
	 * by the application lock and the method is invoked before the application
	 * starts.
	 * 
	 * @param id
	 *            the identifier of the data item.
	 * @param dataItem
	 *            the data item.
	 */
	void addDataItem(String id, DataItem<?> dataItem) {
		if (dataItemHolders.containsKey(id)) {
			throw new IllegalArgumentException(
					"Duplicated identifier \"" + id + "\" of data item in the gateway \"" + getId() + "\".");
		}

		dataItem.attachToGateway(getId() + "/" + id, id, this);
		dataItemHolders.put(id, new DataItemHolder(id, dataItem));
	}

	/**
	 * Returns data item. The method invocation is synchronized by the
	 * application lock.
	 * 
	 * @param id
	 *            the identifier of the data item.
	 * @return the data item.
	 */
	DataItem<?> getDataItem(String id) {
		return dataItemHolders.get(id).dataItem;
	}

	/**
	 * Notifies that value of a data item is changed. The method is invoked in
	 * the handling thread of the application.
	 * 
	 * @param id
	 *            the identifier (within the gateway) of the changed data item.
	 */
	void notifyValueChanged(String id) {
		if (subscribedDataItems.contains(id)) {
			handleReceivedMessage(new Message(id));
		}
	}

	/**
	 * Returns whether the method is invoked inside the method
	 * {@link DataItem#onActivate()} of a given data item.
	 * 
	 * @param dataItem
	 *            the data item.
	 * @return true, if the method is invoked inside the method
	 *         {@link DataItem#onActivate()}, false otherwise.
	 */
	boolean isInsideActivationCodeOfDataItem(DataItem<?> dataItem) {
		return (getApplication() != null) && (getApplication().isInApplicationThread())
				&& (activatingDataItem == dataItem);
	}

	@Override
	protected void onStart(Map<String, Bundle> bundles) {
		List<DataItem<?>> activatedDataItems = new ArrayList<>();

		for (DataItemHolder holder : dataItemHolders.values()) {
			try {
				DataItem<?> dataItem = holder.dataItem;
				Bundle bundle = bundles.get(dataItem.getId());
				activatingDataItem = dataItem;
				dataItem.activate((bundle != null) ? bundle : new Bundle());
				activatedDataItems.add(dataItem);
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Activation of data item \"" + holder.dataItem.getId() + "\" failed.", e);
				throw e;
			} finally {
				activatingDataItem = null;
			}
		}
	}

	@Override
	protected void onAddTopicFilter(String topicFilter) {
		if (!subscribedTopicFilters.add(topicFilter)) {
			logger.log(Level.SEVERE, "Invalid attempt to create a new subscription with topic filter \"" + topicFilter
					+ "\" in the gateway \"" + getId() + "\".");

			return;
		}

		Set<String> matchedDataItems = getMatchingDataItems(topicFilter);
		subscribedDataItems.addAll(matchedDataItems);
		for (String dataItemId : matchedDataItems) {
			dataItemHolders.get(dataItemId).subscriptionCount++;
		}
	}

	@Override
	protected void onRemoveTopicFilter(String topicFilter) {
		if (!subscribedTopicFilters.remove(topicFilter)) {
			logger.log(Level.SEVERE, "Invalid attempt to unsubscribe the topic filter \"" + topicFilter
					+ "\" from the gateway \"" + getId() + "\".");

			return;
		}

		Set<String> matchedDataItems = getMatchingDataItems(topicFilter);
		for (String dataItemId : matchedDataItems) {
			DataItemHolder holder = dataItemHolders.get(dataItemId);
			holder.subscriptionCount--;
			if (holder.subscriptionCount == 0) {
				subscribedDataItems.remove(dataItemId);
			}
		}
	}

	@Override
	protected void onPublish(Message message) {
		// nothing to do (the gateway does not support publications)
	}

	@Override
	protected void onSaveState(Map<String, Bundle> outBundles) {
		Bundle outBundle = new Bundle();

		for (DataItemHolder holder : dataItemHolders.values()) {
			DataItem<?> dataItem = holder.dataItem;
			try {
				dataItem.onSaveState(outBundle);
			} catch (Exception e) {
				logger.log(Level.SEVERE, "onSaveState method of the data item \"" + dataItem.getId() + "\" failed.", e);
			}

			if (!outBundle.isEmpty()) {
				outBundles.put(dataItem.getId(), outBundle);
				outBundle = new Bundle();
			}
		}
	}

	@Override
	protected void onStop() {
		subscribedDataItems.clear();
		subscribedTopicFilters.clear();

		for (DataItemHolder holder : dataItemHolders.values()) {
			holder.subscriptionCount = 0;
			DataItem<?> dataItem = holder.dataItem;
			try {
				dataItem.deactivate();
			} catch (Exception ignore) {
				logger.log(Level.SEVERE, "Unable to deactivate the data item \"" + dataItem.getId() + "\".", ignore);
			}
		}
	}

	@Override
	protected boolean isValidTopicName(String topic) {
		// no topic is accepted for publication
		return false;
	}

	/**
	 * Returns a set of data item identifiers that match a given topic filter.
	 * 
	 * @param topicFilter
	 *            the topic filter.
	 * @return the set of date item identifiers that match the topic filter.
	 */
	private Set<String> getMatchingDataItems(String topicFilter) {
		TopicFilter filter = new TopicFilter(topicFilter);

		Set<String> result = new HashSet<>();
		for (Map.Entry<String, DataItemHolder> entry : dataItemHolders.entrySet()) {
			if (filter.matchTopic(entry.getValue().parsedId)) {
				result.add(entry.getKey());
			}
		}

		return result;
	}
}
