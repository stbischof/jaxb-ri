package org.glassfish.jaxb.runtime.test.record;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlRootElement;

public class TestRecord {
	@org.junit.Test
	public void WriteBookTest() throws Throwable {
		Book book = new Book("theName", "theAuthor", "thePublisher", "theIsbn");

		List books = new ArrayList<>();
		books.add(book);
		Bookstore store = new Bookstore("theName", "theLocation", books);
		final Marshaller marshaller = JAXBContext.newInstance(Bookstore.class).createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		StringWriter stringWriter = new StringWriter();
		marshaller.marshal(store, stringWriter);
		String xmlAsString = stringWriter.toString();
		System.out.println(xmlAsString);

	}

	@org.junit.Test
	public void readBookTest() throws Throwable {

		String sBook = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + "<book>\n"
				+ "    <author xsi:type=\"xs:string\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">theAuthor</author>\n"
				+ "    <name xsi:type=\"xs:string\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">theName</name>\n"
				+ "    <publisher xsi:type=\"xs:string\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">thePublisher</publisher>\n"
				+ "    <isbn xsi:type=\"xs:string\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">theIsbn</isbn>\n"
				+ "</book>";
		final Unmarshaller unmarshaller = JAXBContext.newInstance(Book.class).createUnmarshaller();
		Object book = unmarshaller.unmarshal(new ByteArrayInputStream(sBook.getBytes()));
		System.out.println(book);

	}

	@org.junit.Test
	public void readBookStoreTest() throws Throwable {

		String sBookStore = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
				+ "<ns2:bookstore xmlns:ns2=\"example.books\">\n"
				+ "    <name xsi:type=\"xs:string\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">theName</name>\n"
				+ "    <location xsi:type=\"xs:string\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">theLocation</location>\n"
				+ "    <bookList>\n"
				+ "        <author xsi:type=\"xs:string\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">theAuthor</author>\n"
				+ "        <name xsi:type=\"xs:string\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">theName</name>\n"
				+ "        <publisher xsi:type=\"xs:string\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">thePublisher</publisher>\n"
				+ "        <isbn xsi:type=\"xs:string\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">theIsbn</isbn>\n"
				+ "    </bookList>\n" + "</ns2:bookstore>";
		final Unmarshaller unmarshaller = JAXBContext.newInstance(Bookstore.class).createUnmarshaller();
		Object book = unmarshaller.unmarshal(new ByteArrayInputStream(sBookStore.getBytes()));
		System.out.println(book);
		
		//TODO:: LIST OF BOOKS IS MISSING

	}

	@org.junit.Test
	public void helperTest() throws Throwable {
		Foo f = new Foo();

		final Marshaller marshaller = JAXBContext.newInstance(Foo.class).createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		StringWriter stringWriter = new StringWriter();
		marshaller.marshal(f, stringWriter);
		String xmlAsString = stringWriter.toString();
		System.out.println(xmlAsString);

	}

	@XmlRootElement
	static class Foo {
		public String s = "0";
	}
}
