package de.uni_koblenz.west.cidre.common.config.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import de.uni_koblenz.west.cidre.common.config.Configurable;
import de.uni_koblenz.west.cidre.common.config.Property;
import de.uni_koblenz.west.cidre.common.config.SerializationException;

/**
 * Serializes a {@link Configurable} instance to an XML configuration file.
 * 
 * @author Daniel Janke &lt;danijankATuni-koblenz.de&gt;
 *
 */
public class XMLSerializer {

	public void serialize(Configurable conf, String fileName) {
		serialize(conf, new File(fileName));
	}

	public void serialize(Configurable conf, File file) {
		try (BufferedWriter bw = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));) {
			serialize(conf, bw);
		} catch (IOException e) {
			e.printStackTrace();
			throw new SerializationException(e);
		}
	}

	public void serialize(Configurable conf, Writer outputWriter) {
		XMLStreamWriter writer = null;
		try {
			writer = XMLOutputFactory.newInstance()
					.createXMLStreamWriter(outputWriter);
			serializeDocumentRoot(conf, writer);
			writer.flush();
			writer.close();
		} catch (XMLStreamException | FactoryConfigurationError
				| IllegalArgumentException | IllegalAccessException
				| NoSuchMethodException | SecurityException
				| InvocationTargetException e) {
			e.printStackTrace();
			if (writer != null) {
				try {
					writer.flush();
					writer.close();
				} catch (XMLStreamException e1) {
					e1.printStackTrace();
					e.addSuppressed(e1);
				}
			}
			throw new SerializationException(e);
		}
	}

	private void serializeDocumentRoot(Configurable conf,
			XMLStreamWriter writer)
					throws XMLStreamException, IllegalArgumentException,
					IllegalAccessException, NoSuchMethodException,
					SecurityException, InvocationTargetException {
		writer.writeStartDocument();
		writer.writeCharacters("\n");
		writer.writeStartElement(XMLConstants.CONFIG_ELEMENT);

		for (Field field : conf.getClass().getDeclaredFields()) {
			Property annotation = field.getAnnotation(Property.class);
			if (annotation != null) {
				serializeProperty(writer, annotation.name(),
						annotation.description(),
						getSerializedValue(conf, annotation.name()));
			}
		}

		writer.writeCharacters("\n");
		writer.writeEndElement();
		writer.writeEndDocument();
	}

	private String getSerializedValue(Configurable conf, String fieldName)
			throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		String methodName = "serialize"
				+ Character.toUpperCase(fieldName.charAt(0))
				+ fieldName.substring(1);
		Method serializeMethod = conf.getSerializer().getClass()
				.getMethod(methodName, conf.getClass());
		return serializeMethod.invoke(conf.getSerializer(), conf).toString();
	}

	private void serializeProperty(XMLStreamWriter writer, String name,
			String description, String value) throws XMLStreamException {
		writer.writeCharacters("\n\t");
		writer.writeStartElement(XMLConstants.PROPERTY_ELEMENT);

		writer.writeCharacters("\n\t\t");
		writer.writeStartElement(XMLConstants.PROPERTY_NAME);
		writer.writeCharacters(name);
		writer.writeEndElement();

		writer.writeCharacters("\n\t\t");
		writer.writeStartElement(XMLConstants.PROPERTY_DESCRIPTION);
		writer.writeCharacters(description);
		writer.writeEndElement();

		writer.writeCharacters("\n\t\t");
		writer.writeStartElement(XMLConstants.PROPERTY_VALUE);
		writer.writeCharacters(value);
		writer.writeEndElement();

		writer.writeCharacters("\n\t");
		writer.writeEndElement();
	}
}
