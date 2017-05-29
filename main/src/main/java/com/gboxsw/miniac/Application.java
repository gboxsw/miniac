package com.gboxsw.miniac;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Application {

	/**
	 * Single level wild card.
	 */
	public static final String SINGLE_LEVEL_WILDCARD = "+";

	/**
	 * Multilevel wild card.
	 */
	public static final String MULTI_LEVEL_WILDCARD = "#";

	/**
	 * Identifier of system gateway.
	 */
	public static final String SYSTEM_GATEWAY = "$SYS";

	/**
	 * Recommended name for gateway with data items.
	 */
	public static final String DATA_GATEWAY = "data";

	/**
	 * Recommended name for local (echo) gateway.
	 */
	public static final String LOCAL_GATEWAY = "local";

	/**
	 * Default autosave period in seconds (30 minutes).
	 */
	public static final int DEFAULT_AUTOSAVE_PERIOD = 30 * 60;

	/**
	 * Maximal length of topic or topic filter.
	 */
	private static final int MAX_TOPIC_LENGTH = 65536;

	/**
	 * Logger.
	 */
	private static final Logger logger = Logger.getLogger(Application.class.getName());

	/**
	 * Implementation of subscription.
	 */
	private final class SubscriptionImpl implements Subscription, Comparable<SubscriptionImpl> {

		/**
		 * The topic filter.
		 */
		private final String topicFilter;

		/**
		 * The listener.
		 */
		private final MessageListener messageListener;

		/**
		 * The handling priority.
		 */
		private final int handlingPriority;

		/**
		 * Constructs the subscription.
		 * 
		 * @param topicFilter
		 *            the topic filter.
		 * @param messageListener
		 *            the message listener.
		 * @param handlingPriority
		 *            the handling priority.
		 */
		private SubscriptionImpl(String topicFilter, MessageListener messageListener, int handlingPriority) {
			this.topicFilter = topicFilter;
			this.messageListener = messageListener;
			this.handlingPriority = handlingPriority;
		}

		@Override
		public String getTopicFilter() {
			return topicFilter;
		}

		@Override
		public void close() {
			closeSubscription(this);
		}

		@Override
		public int compareTo(SubscriptionImpl o) {
			return Integer.compare(o.handlingPriority, handlingPriority);
		}
	}

	/**
	 * Topic filter.
	 */
	static final class TopicFilter {

		/**
		 * Subscriptions to the topic filter.
		 */
		private final List<SubscriptionImpl> subscriptions = new ArrayList<>();

		/**
		 * Parsed topic filter.
		 */
		private final String[] topicFilter;

		/**
		 * Indicates whether topic filter ends with the multi-level wild card.
		 */
		private final boolean endsWithMLWildcard;

		/**
		 * Constructs the topic filter.
		 * 
		 * @param topicFilter
		 *            the topic filter.
		 */
		TopicFilter(String topicFilter) {
			String[] parsedTopicFilter = parseTopicHierarchy(topicFilter);

			// check whether topic filter ends with the multilevel wild-card
			endsWithMLWildcard = (MULTI_LEVEL_WILDCARD.equals(parsedTopicFilter[parsedTopicFilter.length - 1]));

			// create parsed topic filter
			if (endsWithMLWildcard) {
				this.topicFilter = Arrays.copyOf(parsedTopicFilter, parsedTopicFilter.length - 1);
			} else {
				this.topicFilter = parsedTopicFilter;
			}

			// replace wild cards with predefined wild-card strings
			for (int i = 0; i < this.topicFilter.length; i++) {
				if (SINGLE_LEVEL_WILDCARD.equals(this.topicFilter[i])) {
					this.topicFilter[i] = SINGLE_LEVEL_WILDCARD;
				}
			}
		}

		/**
		 * Returns whether topic matches the filter.
		 * 
		 * @param parsedTopic
		 *            the parsed topic as an array levels.
		 * @return true, if topic matches the filter, false otherwise.
		 */
		boolean matchTopic(String[] parsedTopic) {
			// check number of levels
			if (endsWithMLWildcard) {
				if (parsedTopic.length < topicFilter.length) {
					return false;
				}
			} else {
				if (parsedTopic.length != topicFilter.length) {
					return false;
				}
			}

			// check levels
			for (int i = topicFilter.length - 1; i >= 0; i--) {
				if (topicFilter[i] == SINGLE_LEVEL_WILDCARD) {
					continue;
				}

				if (!topicFilter[i].equals(parsedTopic[i])) {
					return false;
				}
			}

			return true;
		}
	}

	/**
	 * The extended record related to a messaging gateway.
	 */
	private static final class GatewayHolder {

		/**
		 * The underlying messaging gateway.
		 */
		private final Gateway gateway;

		/**
		 * Active topic filters of the gateway.
		 */
		private final Map<String, TopicFilter> topicFilters = new HashMap<>();

		/**
		 * Constructs the gateway holder.
		 * 
		 * @param gateway
		 *            the underlying messaging gateway.
		 */
		public GatewayHolder(Gateway gateway) {
			this.gateway = gateway;
		}
	}

	/**
	 * Schedule of a scheduled action.
	 */
	private final class Schedule implements Cancellable {

		/**
		 * No repeat.
		 */
		private static final byte NONE = 1;

		/**
		 * Repeat at fixed rate.
		 */
		private static final byte FIXED_RATE = 2;

		/**
		 * Repeat with fixed delay.
		 */
		private static final byte FIXED_DELAY = 3;

		/**
		 * The initial delay in nanoseconds.
		 */
		private final long initialDelay;

		/**
		 * The period between repetitions in nanoseconds.
		 */
		private final long period;

		/**
		 * The repetition mode.
		 */
		private final byte repetitionMode;

		/**
		 * Indicates whether schedule was cancelled.
		 */
		private volatile boolean cancelled;

		/**
		 * Constructs schedule.
		 */
		private Schedule(long initialDelay, long period, byte repetitionMode) {
			this.initialDelay = initialDelay;
			this.period = period;
			this.repetitionMode = repetitionMode;
		}

		@Override
		public void cancel() {
			if (!cancelled) {
				cancelled = true;
				cancelActionWithSchedule(Schedule.this);
			}
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}
	}

	/**
	 * Base class for actions that can be put into a work (action) queue.
	 */
	private static abstract class Action {

	}

	/**
	 * Scheduled action.
	 */
	private final class ScheduledAction implements Comparable<ScheduledAction> {

		/**
		 * The scheduled action.
		 */
		private final Action action;

		/**
		 * The time in milliseconds when the action is scheduled.
		 */
		private final long executionTime;

		/**
		 * The number of non-scheduled actions that precede the scheduled
		 * action.
		 */
		private final long precedingActionCount;

		/**
		 * The schedule of action.
		 */
		private final Schedule schedule;

		/**
		 * Constructs a scheduled action.
		 */
		public ScheduledAction(long executionTime, Action action, Schedule schedule, long precedingActions) {
			this.action = action;
			this.executionTime = executionTime;
			this.precedingActionCount = precedingActions;
			this.schedule = schedule;
		}

		@Override
		public int compareTo(ScheduledAction o) {
			return Long.compare(executionTime, o.executionTime);
		}
	}

	/**
	 * Action representing a receipt of a message.
	 */
	private static final class MessageReceivedAction extends Action {
		/**
		 * Gateway holder of the source gateway.
		 */
		private final GatewayHolder gatewayHolder;

		/**
		 * The received message.
		 */
		private final Message message;

		/**
		 * Constructs action representing a receipt of a message.
		 */
		public MessageReceivedAction(GatewayHolder gatewayHolder, Message message) {
			this.gatewayHolder = gatewayHolder;
			this.message = message;
		}
	}

	/**
	 * Action representing a request to send (publish) a message.
	 */
	private static final class PublishAction extends Action {
		/**
		 * Gateway holder of the target gateway.
		 */
		private final GatewayHolder gatewayHolder;

		/**
		 * Message to be published.
		 */
		private final Message message;

		/**
		 * Constructs action representing a request to send (publish) a message.
		 */
		public PublishAction(GatewayHolder gatewayHolder, Message message) {
			this.gatewayHolder = gatewayHolder;
			this.message = message;
		}
	}

	/**
	 * Subscription change action.
	 */
	private static final class SubscriptionChangeAction extends Action {

		/**
		 * The gateway holder of subscribing/unsubscribing gateway.
		 */
		private final GatewayHolder gatewayHolder;

		/**
		 * The topic filter.
		 */
		private final String topicFilter;

		/**
		 * True for subscribe, false for unsubscribe.
		 */
		private final boolean subscribe;

		/**
		 * Construct the subscription change request.
		 */
		public SubscriptionChangeAction(GatewayHolder gatewayHolder, String topicFilter, boolean subscribe) {
			this.gatewayHolder = gatewayHolder;
			this.topicFilter = topicFilter;
			this.subscribe = subscribe;
		}
	}

	/**
	 * Action representing request for synchronization of a data item.
	 */
	private static final class SynchronizeDataItemAction extends Action {
		/**
		 * The data item to be synchronized.
		 */
		private final DataItem<?> dataItem;

		/**
		 * Constructs request to synchronize a data item.
		 * 
		 * @param dataItem
		 *            the data item.
		 */
		public SynchronizeDataItemAction(DataItem<?> dataItem) {
			this.dataItem = dataItem;
		}
	}

	/**
	 * Action representing request for changing the value of a data item.
	 */
	private static final class RequestChangeAction extends Action {

		/**
		 * The data item requesting change of value.
		 */
		private final DataItem<?> dataItem;

		/**
		 * The desired value.
		 */
		private final Object value;

		/**
		 * Constructs request to change the value of a data item.
		 * 
		 * @param dataItem
		 *            the data item.
		 * @param value
		 *            the desired value.
		 */
		public RequestChangeAction(DataItem<?> dataItem, Object value) {
			this.dataItem = dataItem;
			this.value = value;
		}
	}

	/**
	 * Persistent storage utilized to restore and save states of data items.
	 */
	private PersistentStorage persistentStorage;

	/**
	 * Autosave period in seconds, zero or negative value, if autosave is
	 * disabled.
	 */
	private int autosavePeriodInSeconds = DEFAULT_AUTOSAVE_PERIOD;

	/**
	 * Holders of attached messaging gateways.
	 */
	private final Map<String, GatewayHolder> gatewayHolders = new HashMap<>();

	/**
	 * Gateway for system messages.
	 */
	private final SystemGateway systemGateway;

	/**
	 * Active topic filters that are not related to a particular gateway.
	 */
	private final Map<String, TopicFilter> globalTopicFilters = new HashMap<>();

	/**
	 * Synchronization lock for action queues.
	 */
	private final Object queueLock = new Object();

	/**
	 * The total number of submitted non-scheduled actions.
	 */
	private long totalActionCount = 0;

	/**
	 * Queue of pending actions.
	 */
	private final Queue<Action> actionQueue = new LinkedList<>();

	/**
	 * Queue of scheduled pending actions.
	 */
	private final Queue<ScheduledAction> scheduledActionQueue = new PriorityQueue<>();

	/**
	 * List of modules associated to this application.
	 */
	private final List<Module> modules = new ArrayList<Module>();

	/**
	 * Indicates that initialization of modules started and it is not possible
	 * to add new modules to the application.
	 */
	private boolean immutableModules;

	/**
	 * Indicates whether the application has been launched.
	 */
	private boolean launched;

	/**
	 * The main application thread (used as serialization and event handling
	 * thread).
	 */
	private final Thread applicationThread;

	/**
	 * Start time of a monotonic nano time.
	 */
	private final long clockStartNano;

	/**
	 * Indicates that exit of the application is requested.
	 */
	private volatile boolean exitRequested;

	/**
	 * Synchronization lock.
	 */
	private final Object lock = new Object();

	/**
	 * Constructs the application.
	 */
	public Application() {
		// set start time
		clockStartNano = System.nanoTime();

		// construct the serialization and event handling thread
		applicationThread = new Thread(new Runnable() {
			@Override
			public void run() {
				executeApplication();
				System.exit(0);
			}
		}, "miniAC: main thread");

		// create system gateway
		systemGateway = new SystemGateway();
		systemGateway.attachToApplication(SYSTEM_GATEWAY, this);
		gatewayHolders.put(SYSTEM_GATEWAY, new GatewayHolder(systemGateway));
	}

	/**
	 * Adds a module to the application.
	 * 
	 * @param module
	 *            the module.
	 */
	public void addModule(Module module) {
		if (module == null) {
			throw new NullPointerException("Module cannot be null.");
		}

		synchronized (lock) {
			if ((immutableModules) || (launched)) {
				throw new IllegalStateException("It is not possible to add a module to the launched application.");
			}

			module.attachToApplication(this);
			modules.add(module);
		}
	}

	/**
	 * Adds a gateway to the application.
	 * 
	 * @param id
	 *            the identifier of the gateway.
	 * @param gateway
	 *            the gateway.
	 */
	public void addGateway(String id, Gateway gateway) {
		if (gateway == null) {
			throw new NullPointerException("Gateway cannot be null.");
		}

		if (!Gateway.isValidId(id)) {
			throw new IllegalArgumentException("Malformed/invalid gateway identifier.");
		}

		synchronized (lock) {
			if (launched) {
				throw new IllegalStateException("It is not possible to add a gateway to the launched application.");
			}

			if (gatewayHolders.containsKey(id)) {
				throw new IllegalArgumentException("Duplicated gateway identifier \"" + id + "\".");
			}

			gateway.attachToApplication(id, this);
			gatewayHolders.put(id, new GatewayHolder(gateway));
		}
	}

	/**
	 * Adds a data item to the application.
	 * 
	 * @param gatewayId
	 *            the identifier of a gateway which will manage the data item.
	 *            The gateway must be instance of the class {@link DataGateway
	 *            DataGateway}.
	 * @param id
	 *            the identifier of data item within the gateway.
	 * @param dataItem
	 *            the data item.
	 */
	public void addDataItem(String gatewayId, String id, DataItem<?> dataItem) {
		if (dataItem == null) {
			throw new NullPointerException("Data item cannot be null.");
		}

		if (!Gateway.isValidId(id)) {
			throw new IllegalArgumentException("Malformed/invalid gateway identifier.");
		}

		if (!DataItem.isValidId(id)) {
			throw new IllegalArgumentException("Malformed/invalid identifier of data item.");
		}

		synchronized (lock) {
			if (launched) {
				throw new IllegalStateException("It is not possible to add a data item to the launched application.");
			}

			GatewayHolder gatewayHolder = gatewayHolders.get(gatewayId);
			if (gatewayHolder == null) {
				throw new IllegalArgumentException("Unknown gateway \"" + gatewayId + "\".");
			}

			if (!(gatewayHolder.gateway instanceof DataGateway)) {
				throw new IllegalArgumentException("The \"" + gatewayId + "\" is not a data gateway.");
			}

			DataGateway dataGateway = (DataGateway) gatewayHolder.gateway;
			dataGateway.addDataItem(id, dataItem);
		}
	}

	/**
	 * Returns the data item.
	 * 
	 * @param gatewayId
	 *            the identifier of a gateway which manages the data item.
	 * @param id
	 *            the identifier of data item within the gateway.
	 * @param type
	 *            the class of value type.
	 * @param <T>
	 *            the value type of data item.
	 * @return the data item, or null, if the data item with given name does not
	 *         exist.
	 */
	@SuppressWarnings("unchecked")
	public <T> DataItem<T> getDataItem(String gatewayId, String id, Class<T> type) {
		DataItem<?> result = null;
		synchronized (lock) {
			GatewayHolder gatewayHolder = gatewayHolders.get(gatewayId);
			if ((gatewayHolder != null) && (gatewayHolder.gateway instanceof DataGateway)) {
				result = ((DataGateway) gatewayHolder.gateway).getDataItem(id);
			}
		}

		if (result == null) {
			return null;
		}

		if (result.getType().equals(type)) {
			return (DataItem<T>) result;
		} else {
			throw new IllegalArgumentException("Type of data item " + id + " in gateway \"" + gatewayId
					+ "\" is not compatible with " + type.getName());
		}
	}

	/**
	 * Returns the data item.
	 * 
	 * @param id
	 *            the identifier of data item.
	 * @param type
	 *            the class of value type.
	 * @param <T>
	 *            the value type of data item.
	 * @return the data item, or null, if the data item with given name does not
	 *         exist.
	 */
	public <T> DataItem<T> getDataItem(String id, Class<T> type) {
		int slashIdx = id.indexOf('/');
		if (slashIdx < 0) {
			return null;
		}

		return getDataItem(id.substring(0, slashIdx), id.substring(slashIdx + 1), type);
	}

	/**
	 * Subscribes to a topic.
	 * 
	 * @param topicFilter
	 *            the topic filter.
	 * @param messageListener
	 *            the message listener.
	 * @return the subscription.
	 */
	public Subscription subscribe(String topicFilter, MessageListener messageListener) {
		return subscribe(topicFilter, messageListener, 0);
	}

	/**
	 * Subscribes to a topic.
	 * 
	 * @param topicFilter
	 *            the topic filter.
	 * @param messageListener
	 *            the message listener.
	 * @param handlingPriority
	 *            the handling priority - subscription with greater priority is
	 *            handled first.
	 * @return the subscription.
	 */
	public Subscription subscribe(String topicFilter, MessageListener messageListener, int handlingPriority) {
		// basic checks
		if (messageListener == null) {
			throw new NullPointerException("The message listener cannot be null.");
		}

		if (!isValidTopicFilter(topicFilter)) {
			throw new MessagingException("Malformed topic filter.");
		}

		// create subscription
		SubscriptionImpl subscription = new SubscriptionImpl(topicFilter, messageListener, handlingPriority);

		// retrieve topic head as a gateway id
		String topicHead = getTopicHead(topicFilter);

		// retrieve "gateway" (localized) part of the topic filter
		String localizedTopicFilter = createTopicFilterWithoutHead(topicFilter);
		if (localizedTopicFilter == null) {
			throw new MessagingException("Invalid topic filter: no subtopic after gateway.");
		}

		synchronized (lock) {
			// get topic filters and gateway related to the topic filter
			Map<String, TopicFilter> topicFilters;
			GatewayHolder sourceGatewayHolder;
			if (SINGLE_LEVEL_WILDCARD.equals(topicHead) || MULTI_LEVEL_WILDCARD.equals(topicFilter)) {
				sourceGatewayHolder = null;
				topicFilters = globalTopicFilters;
			} else {
				sourceGatewayHolder = gatewayHolders.get(topicHead);
				if (sourceGatewayHolder == null) {
					throw new MessagingException("Invalid topic filter: Unknown gateway \"" + topicHead + "\".");
				}

				topicFilters = sourceGatewayHolder.topicFilters;
			}

			// get or create topic filter
			TopicFilter filter = topicFilters.get(localizedTopicFilter);

			if (filter == null) {
				// if filter does not exist, create the filter
				filter = new TopicFilter(localizedTopicFilter);
				topicFilters.put(localizedTopicFilter, filter);

				// put appropriate subscription changes to the working queue
				if (sourceGatewayHolder == null) {
					for (GatewayHolder gatewayHolder : gatewayHolders.values()) {
						enqueueAction(new SubscriptionChangeAction(gatewayHolder, localizedTopicFilter, true));
					}
				} else {
					enqueueAction(new SubscriptionChangeAction(sourceGatewayHolder, localizedTopicFilter, true));
				}
			}

			// add subscription to topic filter
			filter.subscriptions.add(subscription);
			return subscription;
		}
	}

	/**
	 * Closes a subscription.
	 * 
	 * @param subscription
	 *            the subscription.
	 */
	private void closeSubscription(SubscriptionImpl subscription) {
		String topicFilter = subscription.topicFilter;

		// retrieve topic head as a gateway id
		String topicHead = getTopicHead(topicFilter);
		// retrieve "gateway" (localized) part of the topic filter
		String localizedTopicFilter = createTopicFilterWithoutHead(topicFilter);

		synchronized (lock) {
			// get topic filters and gateway related to the topic filter
			Map<String, TopicFilter> topicFilters;
			GatewayHolder sourceGatewayHolder;
			if (SINGLE_LEVEL_WILDCARD.equals(topicHead) || MULTI_LEVEL_WILDCARD.equals(topicFilter)) {
				sourceGatewayHolder = null;
				topicFilters = globalTopicFilters;
			} else {
				sourceGatewayHolder = gatewayHolders.get(topicHead);
				if (sourceGatewayHolder == null) {
					return;
				}
				topicFilters = sourceGatewayHolder.topicFilters;
			}

			// get topic filter
			TopicFilter filter = topicFilters.get(localizedTopicFilter);
			if (filter == null) {
				// no active filter for the subscription
				return;
			}

			if (!filter.subscriptions.remove(subscription)) {
				// no subscription to remove
				return;
			}

			if (!filter.subscriptions.isEmpty()) {
				// filter contains other subscriptions
				return;
			}

			// remove empty topic filter
			topicFilters.remove(localizedTopicFilter);

			// put appropriate subscription changes to the working queue
			if (sourceGatewayHolder == null) {
				for (GatewayHolder gatewayHolder : gatewayHolders.values()) {
					enqueueAction(new SubscriptionChangeAction(gatewayHolder, localizedTopicFilter, false));
				}
			} else {
				enqueueAction(new SubscriptionChangeAction(sourceGatewayHolder, localizedTopicFilter, false));
			}
		}
	}

	/**
	 * Publishes a message.
	 * 
	 * @param message
	 *            the message to be published.
	 * 
	 */
	public void publish(Message message) {
		enqueueAction(createPublishAction(message));
	}

	/**
	 * Publishes a message with a delay.
	 * 
	 * @param message
	 *            the message to be published.
	 * @param delay
	 *            the time from now to delay execution.
	 * @param unit
	 *            the time unit of the delay parameter.
	 * 
	 * @return the {@link Cancellable} instance that allows cancellation of the
	 *         pending publication.
	 */
	public Cancellable publishLater(Message message, long delay, TimeUnit unit) {
		return enqueueScheduledAction(createPublishAction(message),
				new Schedule(unit.toNanos(delay), 0, Schedule.NONE));
	}

	/**
	 * Publishes a message at fixed rate analogously to
	 * {@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)}.
	 * 
	 * @param message
	 *            the message to be published
	 * @param initialDelay
	 *            the time to delay first publication.
	 * @param period
	 *            the desired period between successive publications.
	 * @param unit
	 *            the time unit of the initialDelay and period parameters.
	 * 
	 * @return the {@link Cancellable} instance that allows cancellation of the
	 *         pending publications.
	 */
	public Cancellable publishAtFixedRate(Message message, long initialDelay, long period, TimeUnit unit) {
		return enqueueScheduledAction(createPublishAction(message),
				new Schedule(unit.toNanos(initialDelay), unit.toNanos(period), Schedule.FIXED_RATE));
	}

	/**
	 * Publishes a message with a fixed delay analogously to
	 * {@link ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)}.
	 * 
	 * @param message
	 *            the message to be published
	 * @param initialDelay
	 *            the time to delay first publication.
	 * @param delay
	 *            the delay between successive publications.
	 * @param unit
	 *            the time unit of the initialDelay and period parameters.
	 * 
	 * @return the {@link Cancellable} instance that allows cancellation of the
	 *         pending publications.
	 */
	public Cancellable publishWithFixedDelay(Message message, long initialDelay, long delay, TimeUnit unit) {
		return enqueueScheduledAction(createPublishAction(message),
				new Schedule(unit.toNanos(initialDelay), unit.toNanos(delay), Schedule.FIXED_DELAY));
	}

	/**
	 * Creates a publish action for a message to send (publish).
	 * 
	 * @param message
	 *            the message to be published.
	 * @return the instance of {@link PublishAction}.
	 */
	private PublishAction createPublishAction(Message message) {
		String topicName = message.getTopic();
		if (!isValidTopicName(topicName)) {
			throw new MessagingException("Invalid topic.");
		}

		int slashIdx = topicName.indexOf('/');
		if (slashIdx < 0) {
			throw new MessagingException("Invalid topic: missing subtopic of gateway.");
		}

		String gatewayId = topicName.substring(0, slashIdx);
		String localizedTopicName = topicName.substring(slashIdx + 1);

		GatewayHolder targetGatewayHolder = null;
		synchronized (lock) {
			targetGatewayHolder = gatewayHolders.get(gatewayId);
		}

		if (targetGatewayHolder == null) {
			throw new MessagingException("Invalid topic: unknown gateway \"" + gatewayId + "\".");
		}

		if (!targetGatewayHolder.gateway.isValidTopicName(localizedTopicName)) {
			throw new MessagingException("Invalid topic for gateway \"" + gatewayId + "\".");
		}

		Message localizedMessage = new Message(localizedTopicName, message.getPayload());
		return new PublishAction(targetGatewayHolder, localizedMessage);
	}

	/**
	 * Sets the persistent storage utilized to restore and save state of data
	 * items.
	 * 
	 * @param storage
	 *            the persistent storage.
	 */
	public void setPersistentStorage(PersistentStorage storage) {
		synchronized (lock) {
			if (launched) {
				throw new IllegalStateException(
						"It is not possible to set persistent storage of launched application.");
			}

			this.persistentStorage = storage;
		}
	}

	/**
	 * Returns the persistent storage.
	 * 
	 * @see Application#setPersistentStorage(PersistentStorage)
	 * @return the persistent storage.
	 */
	public PersistentStorage getPersistentStorage() {
		synchronized (lock) {
			return persistentStorage;
		}
	}

	/**
	 * Sets the period in seconds after which state of application is
	 * automatically saved.
	 * 
	 * @param seconds
	 *            the period in seconds, zero or negative value indicate that
	 *            state is not saved continuously.
	 */
	public void setAutosavePeriod(int seconds) {
		synchronized (lock) {
			if (launched) {
				throw new IllegalStateException("It is not possible to set autosave period of launched application.");
			}

			this.autosavePeriodInSeconds = Math.max(seconds, 0);
		}
	}

	/**
	 * Returns auto-save period in seconds.
	 * 
	 * @see Application#setAutosavePeriod(int)
	 * @return the auto-save period in seconds.
	 */
	public int getAutosavePeriod() {
		synchronized (lock) {
			return autosavePeriodInSeconds;
		}
	}

	/**
	 * Saves state of application. The method must be executed in the main
	 * application thread.
	 */
	private void saveState() {
		if (persistentStorage == null) {
			return;
		}

		logger.log(Level.INFO, "Saving state of application.");

		// retrieve bundles
		Map<String, Bundle> bundles = new HashMap<>();
		for (GatewayHolder gatewayHolder : gatewayHolders.values()) {
			String gatewayId = gatewayHolder.gateway.getId();
			Map<String, Bundle> gatewayBundles = new HashMap<>();
			try {
				gatewayHolder.gateway.onSaveState(gatewayBundles);
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Saving the state of the gateway \"" + gatewayId + "\" failed.", e);
			}

			String gatewayBundlePrefix = gatewayId + "/";
			for (Map.Entry<String, Bundle> entry : gatewayBundles.entrySet()) {
				if ((entry.getKey() != null) && (entry.getValue() != null)) {
					if (!entry.getKey().startsWith(gatewayBundlePrefix)) {
						logger.log(Level.WARNING, "The gateway \"" + gatewayId + "\" produced a bundle with id (\""
								+ entry.getKey() + "\") whose prefix is not the gateway identifier.");
					}

					bundles.put(entry.getKey(), entry.getValue());
				}
			}
		}

		// persistently store bundles
		try {
			persistentStorage.saveBundles(bundles);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Saving state to persistent storage failed.", e);
		}

		systemGateway.emitSystemMessage("state-saved", null);
		logger.log(Level.INFO, "State of application saved.");
	}

	/**
	 * Launches the application.
	 */
	public void launch() {
		// initialize modules
		List<Module> modulesToInit;
		synchronized (lock) {
			immutableModules = true;
			modulesToInit = new ArrayList<>(modules);
		}

		if (!modulesToInit.isEmpty()) {
			logger.log(Level.INFO, "Initializing modules.");
			for (Module module : modulesToInit) {
				try {
					module.initialize();
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Initialization of module failed.", e);
					throw e;
				}
			}
			logger.log(Level.INFO, "Modules initialized.");
		}

		// start application
		synchronized (lock) {
			if (launched) {
				throw new IllegalStateException("Application has been already launched.");
			}

			launched = true;
		}

		applicationThread.start();

		// add shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				requestApplicationExit();
			}
		});
	}

	/**
	 * Initializes and executes the application. The method is executed in the
	 * main application thread (serialization and event handling thread).
	 */
	private void executeApplication() {
		List<Gateway> startedGateways = null;
		boolean eventLoopStarted = false;
		try {
			// start gateways
			startedGateways = startGateways();

			// if all gateways are started, start event loop
			if (startedGateways.size() == gatewayHolders.size()) {
				logger.log(Level.INFO, "Main thread of the application started.");
				eventLoopStarted = true;
				runEventLoop();
				logger.log(Level.INFO, "Main thread of the application stopped.");
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Main thread of the application failed.", e);
		} finally {
			// save state if the application started normally
			if (eventLoopStarted) {
				saveState();
			}

			// close all started gateways
			if (startedGateways != null) {
				Collections.reverse(startedGateways);
				stopGateways(startedGateways);
			}
		}

		logger.log(Level.INFO, "Application stopped.");
	}

	/**
	 * Returns a list of gateways ordered in an appropriate activation order.
	 * Data gateways are started as the last.
	 * 
	 * @return the list of gateways.
	 */
	private List<Gateway> constructGatewayActivationOrder() {
		List<Gateway> result = new ArrayList<>();

		// add system gateway
		result.add(systemGateway);

		// add all gateways that are not data gateways (ignore system gateway)
		for (GatewayHolder gatewayHolder : gatewayHolders.values()) {
			if (!(gatewayHolder.gateway instanceof DataGateway) && !(gatewayHolder.gateway instanceof SystemGateway)) {
				result.add(gatewayHolder.gateway);
			}
		}

		// add data gateways
		for (GatewayHolder gatewayHolder : gatewayHolders.values()) {
			if (gatewayHolder.gateway instanceof DataGateway) {
				result.add(gatewayHolder.gateway);
			}
		}

		return result;
	}

	/**
	 * Start registered messaging gateways and returns list of started gateways.
	 * 
	 * @return the list of started gateways in order of start.
	 */
	private List<Gateway> startGateways() {
		List<Gateway> startedGateways = new ArrayList<>();

		// load persistent bundles of all gateways
		Map<String, Bundle> persistentBundles = null;
		if (persistentStorage != null) {
			try {
				persistentBundles = persistentStorage.loadBundles();
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Loading data from persistent storage failed.", e);
				return startedGateways;
			}
		}

		if (persistentBundles == null) {
			persistentBundles = Collections.emptyMap();
		} else {
			persistentBundles.remove(null);
		}

		// start gateways
		for (Gateway gateway : constructGatewayActivationOrder()) {
			String gatewayId = gateway.getId();
			String gatewayPrefix = gatewayId + "/";

			Map<String, Bundle> gatewayBundles = new HashMap<>();
			for (Map.Entry<String, Bundle> entry : persistentBundles.entrySet()) {
				if (entry.getKey().startsWith(gatewayPrefix)) {
					gatewayBundles.put(entry.getKey(), entry.getValue());
				}
			}

			try {
				logger.log(Level.INFO, "Gateway \"" + gatewayId + "\" is starting.");
				gateway.onStart(gatewayBundles);
				startedGateways.add(gateway);
				logger.log(Level.INFO, "Gateway \"" + gatewayId + "\" started.");
			} catch (Throwable e) {
				logger.log(Level.SEVERE, "Unable to start the gateway \"" + gatewayId + "\" due to thrown exception.",
						e);
			}
		}

		return startedGateways;
	}

	/**
	 * Executes the main application code (serialization and event loop).
	 */
	private void runEventLoop() {
		// execution loop
		long now = System.nanoTime() - clockStartNano;
		long processedActionCount = 0;

		final boolean autosaveEnabled = (persistentStorage != null) && (autosavePeriodInSeconds > 0);
		final long autosaveNanoPeriod = autosaveEnabled ? autosavePeriodInSeconds * (long) 1_000_000_000 : 0;
		long lastSave = now;

		while (!exitRequested) {
			Action action = null;

			// retrieve an action (work item)
			synchronized (queueLock) {
				// handle scheduled actions first
				if (!scheduledActionQueue.isEmpty()) {
					now = System.nanoTime() - clockStartNano;
					ScheduledAction queueHeadAction = scheduledActionQueue.peek();
					if ((now > queueHeadAction.executionTime)
							&& (queueHeadAction.precedingActionCount <= processedActionCount)) {
						ScheduledAction scheduledAction = scheduledActionQueue.poll();
						Schedule schedule = scheduledAction.schedule;
						if (!schedule.cancelled) {
							action = scheduledAction.action;

							// reschedule if the schedule defines repetitions
							if (schedule.repetitionMode == Schedule.FIXED_DELAY) {
								scheduledActionQueue.add(
										new ScheduledAction(now + schedule.period, action, schedule, totalActionCount));
							} else if (schedule.repetitionMode == Schedule.FIXED_RATE) {
								long nextExecutionTime = scheduledAction.executionTime + schedule.period;
								if (nextExecutionTime <= now) {
									nextExecutionTime = now + schedule.period;
								}
								scheduledActionQueue.add(
										new ScheduledAction(nextExecutionTime, action, schedule, totalActionCount));
							}
						}
					}
				}

				// if there is still no retrieved action, inspect the queue with
				// unscheduled actions
				if (action == null) {
					action = actionQueue.poll();

					// wait for new action (if necessary)
					if (action == null) {
						try {
							if (scheduledActionQueue.isEmpty()) {
								queueLock.wait();
							} else {
								long nanoDelay = scheduledActionQueue.peek().executionTime - now;
								if (nanoDelay > 0) {
									TimeUnit.NANOSECONDS.timedWait(queueLock, nanoDelay);
								}
							}
						} catch (InterruptedException ignore) {
							// nothing to do
						}
					} else {
						processedActionCount++;
					}
				}
			}

			if (action == null) {
				continue;
			}

			if (action.getClass() == SynchronizeDataItemAction.class) {
				// handle synchronization of a data item
				SynchronizeDataItemAction synchronizeAction = (SynchronizeDataItemAction) action;
				synchronizeAction.dataItem.synchronizeValue();
			} else if (action.getClass() == MessageReceivedAction.class) {
				// handle receipt of a message
				handleMessageReceivedAction((MessageReceivedAction) action);
			} else if (action.getClass() == PublishAction.class) {
				// handle request to send message
				PublishAction publishAction = (PublishAction) action;
				publishAction.gatewayHolder.gateway.onPublish(publishAction.message);
			} else if (action.getClass() == RequestChangeAction.class) {
				// handle request to change the value of a data item
				RequestChangeAction changeAction = (RequestChangeAction) action;
				changeAction.dataItem.requestValueChange(changeAction.value);
			} else if (action.getClass() == SubscriptionChangeAction.class) {
				// handle subscription change
				SubscriptionChangeAction changeAction = (SubscriptionChangeAction) action;
				if (changeAction.subscribe) {
					changeAction.gatewayHolder.gateway.onAddTopicFilter(changeAction.topicFilter);
				} else {
					changeAction.gatewayHolder.gateway.onRemoveTopicFilter(changeAction.topicFilter);
				}
			}

			// save state (if necessary)
			if (autosaveEnabled) {
				now = System.nanoTime() - clockStartNano;
				if (now - lastSave > autosaveNanoPeriod) {
					saveState();
					lastSave = now;
				}
			}
		}
	}

	/**
	 * Handles a published message for system.
	 * 
	 * @param message
	 *            the message.
	 */
	void handlePublishedSystemMessage(Message message) {
		String topic = message.getTopic().toLowerCase();

		// handle request to exit
		if ("exit".equals(topic)) {
			requestApplicationExit();
			return;
		}

		// handle request to save state
		if ("save".equals(topic)) {
			saveState();
			return;
		}
	}

	/**
	 * Requests the application to exit.
	 */
	private void requestApplicationExit() {
		exitRequested = true;
		synchronized (queueLock) {
			queueLock.notifyAll();
		}
	}

	/**
	 * Silently stop gateways.
	 * 
	 * @param gatewaysToStop
	 *            the list of gateways that should be stopped.
	 */
	private void stopGateways(List<Gateway> gatewaysToStop) {
		for (Gateway gateway : gatewaysToStop) {
			try {
				gateway.onStop();
				logger.log(Level.INFO, "Gateway \"" + gateway.getId() + "\" stopped.");
			} catch (Exception ignore) {
				logger.log(Level.SEVERE, "Unable to stop the gateway \"" + gateway.getId() + "\".", ignore);
			}
		}
	}

	/**
	 * Adds a new action to the queue of actions to be performed.
	 * 
	 * @param action
	 *            the action.
	 */
	private void enqueueAction(Action action) {
		if (action == null) {
			return;
		}

		synchronized (queueLock) {
			actionQueue.offer(action);
			totalActionCount++;
			queueLock.notifyAll();
		}
	}

	/**
	 * Adds a new action the should be performed in a repeatable manner with
	 * respect to given schedule.
	 * 
	 * @param action
	 *            the action to be scheduled.
	 * @param schedule
	 *            the schedule of action.
	 * @return the {@link Cancellable} instance that allows cancellation of the
	 *         action.
	 */
	private Cancellable enqueueScheduledAction(Action action, Schedule schedule) {
		long firstExecutionTime = (System.nanoTime() - clockStartNano) + schedule.initialDelay;
		synchronized (queueLock) {
			scheduledActionQueue.offer(new ScheduledAction(firstExecutionTime, action, schedule, totalActionCount));
			queueLock.notifyAll();
		}

		return schedule;
	}

	/**
	 * Cancels all scheduled action with given schedule instance.
	 * 
	 * @param schedule
	 *            the schedule.
	 */
	private void cancelActionWithSchedule(Schedule schedule) {
		synchronized (queueLock) {
			Iterator<ScheduledAction> it = scheduledActionQueue.iterator();
			while (it.hasNext()) {
				ScheduledAction action = it.next();
				if (action.schedule == schedule) {
					it.remove();
				}
			}
		}
	}

	/**
	 * Handles the received message.
	 */
	private void handleMessageReceivedAction(MessageReceivedAction action) {
		// find all matching subscriptions
		List<SubscriptionImpl> matchingSubscriptions = new ArrayList<>();
		String[] parsedTopic = parseTopicHierarchy(action.message.getTopic());
		synchronized (lock) {
			// search in gateway specific subscriptions
			for (TopicFilter topicFilter : action.gatewayHolder.topicFilters.values()) {
				if (topicFilter.matchTopic(parsedTopic)) {
					matchingSubscriptions.addAll(topicFilter.subscriptions);
				}
			}

			// search in global subscriptions
			for (TopicFilter topicFilter : globalTopicFilters.values()) {
				if (topicFilter.matchTopic(parsedTopic)) {
					matchingSubscriptions.addAll(topicFilter.subscriptions);
				}
			}
		}

		if (matchingSubscriptions.isEmpty()) {
			return;
		}

		// if there are at least two subscriptions with different handling
		// priorities, sort them according to handling priority
		if (matchingSubscriptions.size() >= 2) {
			boolean sortingRequired = false;

			int handlingPriority = matchingSubscriptions.get(0).handlingPriority;
			for (SubscriptionImpl subscription : matchingSubscriptions) {
				if (subscription.handlingPriority != handlingPriority) {
					sortingRequired = true;
					break;
				}
			}

			if (sortingRequired) {
				Collections.sort(matchingSubscriptions);
			}
		}

		// create message with topic including the source gateway
		Message message = new Message(action.gatewayHolder.gateway.getId() + "/" + action.message.getTopic(),
				action.message.getPayload());

		// send message to all subscriptions
		for (SubscriptionImpl subscription : matchingSubscriptions) {
			try {
				subscription.messageListener.messageReceived(message);
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Message listener threw an exception.");
				throw e;
			}
		}
	}

	/**
	 * Pushes the received message for processing. The method is called from
	 * gateways.
	 * 
	 * @param gatewayId
	 *            the identifier of gateway.
	 * @param message
	 *            the received message.
	 */
	void pushReceivedMessage(String gatewayId, Message message) {
		synchronized (lock) {
			GatewayHolder gatewayHolder = gatewayHolders.get(gatewayId);
			if (gatewayHolder != null) {
				enqueueAction(new MessageReceivedAction(gatewayHolder, message));
			}
		}
	}

	/**
	 * Pushes a request for synchronizing a data item to the handling thread.
	 *
	 * @param dataItem
	 *            the data item.
	 */
	void pushSynchronizationRequest(DataItem<?> dataItem) {
		enqueueAction(new SynchronizeDataItemAction(dataItem));
	}

	/**
	 * Pushes a request to change value of a data item to a new value.
	 * 
	 * @param dataItem
	 *            the data item.
	 * @param newValue
	 *            the desired value.
	 */
	public void pushChangeRequest(DataItem<?> dataItem, Object newValue) {
		enqueueAction(new RequestChangeAction(dataItem, newValue));
	}

	/**
	 * Returns whether the method is executed in the main thread of this
	 * application instance.
	 * 
	 * @return true, if the method is executed in the main thread of the
	 *         application, false otherwise.
	 */
	public boolean isInApplicationThread() {
		return Thread.currentThread() == applicationThread;
	}

	/**
	 * Returns whether a topic filter is valid.
	 * 
	 * @param topicFilter
	 *            the topic filter.
	 * @return true, if the topic filter is valid, false otherwise.
	 */
	public static boolean isValidTopicFilter(String topicFilter) {
		// length of topic must be at least 1
		if ((topicFilter == null) || (topicFilter.isEmpty())) {
			return false;
		}

		// check maximal allowed length of the topic filter
		if (topicFilter.length() > MAX_TOPIC_LENGTH) {
			return false;
		}

		// no topic can contain character with code 0
		if (topicFilter.indexOf(0) >= 0) {
			return false;
		}

		String[] parsedTopicFilter = parseTopicHierarchy(topicFilter);
		int mlWildcardCounter = 0;
		for (String part : parsedTopicFilter) {
			// [MQTT-4.7.1-3]
			if (part.indexOf('+') >= 0) {
				if (!SINGLE_LEVEL_WILDCARD.equals(part)) {
					return false;
				}
			}
			// [MQTT-4.7.1-2]
			if (part.indexOf('#') >= 0) {
				if (!MULTI_LEVEL_WILDCARD.equals(part)) {
					return false;
				}

				mlWildcardCounter++;
			}
		}

		if (mlWildcardCounter >= 2) {
			return false;
		}

		if ((mlWildcardCounter == 1) && !MULTI_LEVEL_WILDCARD.equals(parsedTopicFilter[parsedTopicFilter.length - 1])) {
			return false;
		}

		return true;
	}

	/**
	 * Returns whether a topic name is valid.
	 * 
	 * @param topicName
	 *            the topic name.
	 * @return true, if the topic name is valid, false otherwise.
	 */
	public static boolean isValidTopicName(String topicName) {
		// length of topic must be at least 1
		if ((topicName == null) || (topicName.isEmpty())) {
			return false;
		}

		// check maximal allowed length of the topic filter
		if (topicName.length() > MAX_TOPIC_LENGTH) {
			return false;
		}

		// no topic can contain character with code 0
		if (topicName.indexOf(0) >= 0) {
			return false;
		}

		return true;
	}

	/**
	 * Returns the head (the first part) of a topic name or a topic filter.
	 * 
	 * @param topicFilter
	 *            the topic or the topic filter.
	 * @return the head (the first part) of the topic name or the topic filter.
	 */
	private static String getTopicHead(String topicFilter) {
		int slashPos = topicFilter.indexOf('/');
		if (slashPos < 0) {
			return topicFilter;
		} else {
			return topicFilter.substring(0, slashPos);
		}
	}

	/**
	 * Returns topic filter with removed head and the same semantics.
	 * 
	 * @param topicFilter
	 *            the topic filter.
	 * @return the equivalent topic filter without head.
	 */
	private static String createTopicFilterWithoutHead(String topicFilter) {
		int slashPos = topicFilter.indexOf('/');
		if (slashPos < 0) {
			if (MULTI_LEVEL_WILDCARD.equals(topicFilter)) {
				return MULTI_LEVEL_WILDCARD;
			} else {
				return null;
			}
		} else {
			return topicFilter.substring(slashPos + 1);
		}
	}

	/**
	 * Parses topic name or topic filter to topic hierarchy.
	 * 
	 * @param topic
	 *            the topic name or topic filter to be parsed.
	 * @return the array formed by parts of the topic hierarchy.
	 */
	public static String[] parseTopicHierarchy(String topic) {
		if (topic == null) {
			return null;
		}

		List<String> parts = new ArrayList<>();
		int readIdx = 0, slashIdx = 0;
		while ((slashIdx = topic.indexOf('/', readIdx)) >= 0) {
			parts.add(topic.substring(readIdx, slashIdx));
			readIdx = slashIdx + 1;
		}
		parts.add(topic.substring(readIdx));

		return parts.toArray(new String[parts.size()]);
	}

	/**
	 * Creates an application with attached data and local gateways with
	 * recommended names.
	 * 
	 * @return the application.
	 */
	public static Application createSimpleApplication() {
		Application application = new Application();
		application.addGateway(DATA_GATEWAY, new DataGateway());
		application.addGateway(LOCAL_GATEWAY, new EchoGateway());
		return application;
	}
}
