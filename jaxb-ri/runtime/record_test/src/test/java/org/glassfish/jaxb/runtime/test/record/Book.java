package org.glassfish.jaxb.runtime.test.record;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "book")
@XmlType(propOrder = { "author", "name", "publisher", "isbn" })
public record Book(@XmlElement(name = "title") String name, String author, String publisher, String isbn) {
}
