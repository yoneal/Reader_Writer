//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.10.09 at 02:37:29 AM PHT 
//


package reader_writer.message;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the reader_writer.message package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Message_QNAME = new QName("", "message");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: reader_writer.message
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link WriteRequestParam }
     * 
     */
    public WriteRequestParam createWriteRequestParam() {
        return new WriteRequestParam();
    }

    /**
     * Create an instance of {@link ViewIdentity }
     * 
     */
    public ViewIdentity createViewIdentity() {
        return new ViewIdentity();
    }

    /**
     * Create an instance of {@link MessageIdentity }
     * 
     */
    public MessageIdentity createMessageIdentity() {
        return new MessageIdentity();
    }

    /**
     * Create an instance of {@link ChangeList }
     * 
     */
    public ChangeList createChangeList() {
        return new ChangeList();
    }

    /**
     * Create an instance of {@link ProcessIdentity }
     * 
     */
    public ProcessIdentity createProcessIdentity() {
        return new ProcessIdentity();
    }

    /**
     * Create an instance of {@link Nack }
     * 
     */
    public Nack createNack() {
        return new Nack();
    }

    /**
     * Create an instance of {@link ReadRequestParam }
     * 
     */
    public ReadRequestParam createReadRequestParam() {
        return new ReadRequestParam();
    }

    /**
     * Create an instance of {@link NewList }
     * 
     */
    public NewList createNewList() {
        return new NewList();
    }

    /**
     * Create an instance of {@link Ack }
     * 
     */
    public Ack createAck() {
        return new Ack();
    }

    /**
     * Create an instance of {@link Confirm }
     * 
     */
    public Confirm createConfirm() {
        return new Confirm();
    }

    /**
     * Create an instance of {@link WriteResponseParam }
     * 
     */
    public WriteResponseParam createWriteResponseParam() {
        return new WriteResponseParam();
    }

    /**
     * Create an instance of {@link Message }
     * 
     */
    public Message createMessage() {
        return new Message();
    }

    /**
     * Create an instance of {@link ParamType }
     * 
     */
    public ParamType createParamType() {
        return new ParamType();
    }

    /**
     * Create an instance of {@link ReadResponseParam }
     * 
     */
    public ReadResponseParam createReadResponseParam() {
        return new ReadResponseParam();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Message }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "message")
    public JAXBElement<Message> createMessage(Message value) {
        return new JAXBElement<Message>(_Message_QNAME, Message.class, null, value);
    }

}
