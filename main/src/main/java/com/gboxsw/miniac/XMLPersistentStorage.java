package com.gboxsw.miniac;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;
import javax.xml.stream.*;

/**
 * File and xml based persistent storage.
 */
public class XMLPersistentStorage implements PersistentStorage {

	/**
	 * Root element of the storage.
	 */
	private static final String ROOT_ELEMENT = "Storage";

	/**
	 * The element representing a bundle.
	 */
	private static final String BUNDLE_ELEMENT = "Bundle";

	/**
	 * The element representing an item.
	 */
	private static final String ITEM_ELEMENT = "Item";

	/**
	 * Logger.
	 */
	private static final Logger logger = Logger.getLogger(XMLPersistentStorage.class.getName());

	/**
	 * The file where data are stored.
	 */
	private final File xmlFile;

	/**
	 * Constructs the storage.
	 * 
	 * @param xmlFile
	 *            the xml file where persistent data are stored.
	 */
	public XMLPersistentStorage(File xmlFile) {
		this.xmlFile = xmlFile;
	}

	@Override
	public Map<String, Bundle> loadBundles() {
		try {
			if (!xmlFile.exists() || !xmlFile.isFile()) {
				return Collections.emptyMap();
			}

			XMLInputFactory xif = XMLInputFactory.newInstance();
			try (Reader fileReader = new FileReader(xmlFile)) {
				XMLStreamReader xmlReader = xif.createXMLStreamReader(fileReader);
				if (xmlReader.nextTag() == XMLStreamConstants.START_ELEMENT) {
					return readXmlStorage(xmlReader);
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Loading of xml formatted data from the file \"" + xmlFile + "\" failed.", e);
		}

		return Collections.emptyMap();
	}

	/**
	 * Reads map of bundles from an xml stream reader.
	 * 
	 * @param xmlReader
	 *            the xml stream reader at opening tag of the root element.
	 * @return the loaded map of bundles.
	 */
	private Map<String, Bundle> readXmlStorage(XMLStreamReader xmlReader) throws XMLStreamException {
		xmlReader.require(XMLStreamConstants.START_ELEMENT, null, ROOT_ELEMENT);

		Map<String, Bundle> result = new HashMap<>();
		while (xmlReader.next() != XMLStreamReader.END_ELEMENT) {
			if (xmlReader.isStartElement()) {
				if (BUNDLE_ELEMENT.equals(xmlReader.getLocalName())) {
					String bundleId = xmlReader.getAttributeValue(null, "id");
					if ((bundleId == null) || bundleId.isEmpty()) {
						logger.log(Level.WARNING, "Found a bundle without identifier.");
						skipElement(xmlReader);
					} else {
						if (result.containsKey(bundleId)) {
							logger.log(Level.WARNING,
									"Found a bundle with non-unique identifier \"" + bundleId + "\".");
						}

						result.put(bundleId, readXmlBundle(xmlReader));
					}
				} else {
					skipElement(xmlReader);
				}
			}
		}

		return result;
	}

	/**
	 * Reads a bundle from an xml stream reader.
	 * 
	 * @param xmlReader
	 *            the xml stream reader at opening tag of a bundle.
	 * @return the loaded bundle.
	 */
	private Bundle readXmlBundle(XMLStreamReader xmlReader) throws XMLStreamException {
		xmlReader.require(XMLStreamConstants.START_ELEMENT, null, BUNDLE_ELEMENT);
		String bundleId = xmlReader.getAttributeValue(null, "id");

		Bundle result = new Bundle();
		while (xmlReader.next() != XMLStreamReader.END_ELEMENT) {
			if (xmlReader.isStartElement()) {
				if (ITEM_ELEMENT.equals(xmlReader.getLocalName())) {
					String itemKey = xmlReader.getAttributeValue(null, "key");
					if ((itemKey == null) || itemKey.isEmpty()) {
						logger.log(Level.WARNING, "Found an item without key in the bundle \"" + bundleId + "\".");
						skipElement(xmlReader);
					} else {
						if (result.containsKey(itemKey)) {
							logger.log(Level.WARNING,
									"Found duplicated items (\"" + itemKey + "\") in the bundle \"" + bundleId + "\".");
						}

						try {
							result.put(itemKey, (Serializable) readXmlItem(xmlReader));
						} catch (Exception e) {
							logger.log(Level.WARNING, "Parsing of the item \"" + itemKey + "\" in the bundle \""
									+ bundleId + "\" failed.", e);
						}
					}
				} else {
					skipElement(xmlReader);
				}
			}
		}

		return result;
	}

	/**
	 * Reads an item from an xml stream reader.
	 * 
	 * @param xmlReader
	 *            the xml stream reader at an item element.
	 * @return the loaded value of an item.
	 */
	private Object readXmlItem(XMLStreamReader xmlReader) throws XMLStreamException {
		xmlReader.require(XMLStreamConstants.START_ELEMENT, null, ITEM_ELEMENT);
		String itemType = xmlReader.getAttributeValue(null, "type");
		String encodedValue = null;
		try {
			encodedValue = xmlReader.getElementText();
		} catch (XMLStreamException e) {
			xmlReader.nextTag();
			throw e;
		}

		if (itemType == null) {
			return deserialize(encodedValue);
		}

		switch (itemType) {
		case "bool":
			return Boolean.parseBoolean(encodedValue);
		case "int":
			return Integer.parseInt(encodedValue);
		case "long":
			return Long.parseLong(encodedValue);
		case "double":
			return Double.parseDouble(encodedValue);
		case "string":
			return encodedValue;
		default:
			throw new IllegalStateException("Unsupported item type: " + itemType);
		}
	}

	@Override
	public void saveBundles(Map<String, Bundle> bundles) {
		if (bundles == null) {
			bundles = Collections.emptyMap();
		}

		try {
			XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
			try (FileWriter writer = new FileWriter(xmlFile)) {
				XMLStreamWriter xmlWriter = outputFactory.createXMLStreamWriter(writer);
				xmlWriter.writeStartDocument();
				xmlWriter.writeStartElement(ROOT_ELEMENT);

				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
				dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				xmlWriter.writeAttribute("saved", dateFormat.format(new Date()));

				for (Map.Entry<String, Bundle> entry : bundles.entrySet()) {
					xmlWriter.writeStartElement(BUNDLE_ELEMENT);
					xmlWriter.writeAttribute("id", entry.getKey());

					Bundle bundle = entry.getValue();
					for (String key : bundle.getKeys()) {
						writeItemToXml(xmlWriter, key, bundle.get(key));
					}

					xmlWriter.writeEndElement();
				}

				xmlWriter.writeEndElement();
				xmlWriter.writeEndDocument();
				xmlWriter.flush();
				xmlWriter.close();
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Saving xml formatted data to the file \"" + xmlFile + "\" failed.", e);
		}
	}

	/**
	 * Writes a key-value pairs as an item to an xml writer.
	 * 
	 * @param xmlWriter
	 *            the xml writer.
	 * @param key
	 *            the key.
	 * @param value
	 *            the value.
	 * @throws XMLStreamException
	 *             when writing to provided xml stream failed.
	 */
	private void writeItemToXml(XMLStreamWriter xmlWriter, String key, Object value) throws XMLStreamException {
		if (value == null) {
			return;
		}

		String valueType = null;
		String encodedValue = null;

		if (value instanceof String) {
			valueType = "string";
			encodedValue = value.toString();
		} else if (value instanceof Boolean) {
			valueType = "bool";
			encodedValue = Boolean.toString((boolean) value);
		} else if (value instanceof Integer) {
			valueType = "int";
			encodedValue = Integer.toString((int) value);
		} else if (value instanceof Long) {
			valueType = "long";
			encodedValue = Long.toString((long) value);
		} else if (value instanceof Double) {
			valueType = "double";
			encodedValue = Double.toString((double) value);
		} else if (value instanceof Serializable) {
			encodedValue = serialize(value);
		} else {
			logger.log(Level.WARNING, "Unsupported serialization for key \"" + key + "\" failed.");
			return;
		}

		xmlWriter.writeStartElement(ITEM_ELEMENT);
		xmlWriter.writeAttribute("key", key);
		if (valueType != null) {
			xmlWriter.writeAttribute("type", valueType);
		}
		xmlWriter.writeCharacters(encodedValue);
		xmlWriter.writeEndElement();
	}

	/**
	 * Serializes a serializable object to a base-64 encoded string.
	 * 
	 * @param o
	 *            the object to be serialized.
	 * @return the base64 serialized object.
	 */
	private static String serialize(Object o) {
		try {
			ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
			ObjectOutputStream output = new ObjectOutputStream(byteOutput);
			output.writeObject(o);
			output.close();
			return Base64.getEncoder().encodeToString(byteOutput.toByteArray());
		} catch (Exception e) {
			throw new RuntimeException("Serialization of an object failed.");
		}
	}

	/**
	 * Deserializes a serialized object.
	 * 
	 * @param encodedObject
	 *            the base-64 encoded serialization of an object.
	 * @return the deserialized object.
	 */
	private static Object deserialize(String encodedObject) {
		try {
			byte[] inputData = Base64.getDecoder().decode(encodedObject);
			ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(inputData));
			Object o = input.readObject();
			input.close();
			return o;
		} catch (Exception e) {
			throw new RuntimeException("Deserialization of an object failed.");
		}
	}

	/**
	 * Skips an xml element.
	 * 
	 * @param xmlReader
	 *            the xml stream reader.
	 */
	private static void skipElement(XMLStreamReader xmlReader) throws XMLStreamException {
		xmlReader.require(XMLStreamConstants.START_ELEMENT, null, null);
		while (xmlReader.next() != XMLStreamConstants.END_ELEMENT) {
			if (xmlReader.isStartElement()) {
				skipElement(xmlReader);
			}
		}
	}
}
