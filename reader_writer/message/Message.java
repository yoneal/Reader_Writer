//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.10.09 at 02:37:29 AM PHT 
//


package reader_writer.message;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Message complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Message">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="viewid" type="{}ViewIdentity"/>
 *         &lt;element name="msgid" type="{}MessageIdentity"/>
 *         &lt;element name="timestamp" type="{http://www.w3.org/2001/XMLSchema}unsignedLong"/>
 *         &lt;element name="msgtype" type="{}MessageType"/>
 *         &lt;element name="param" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Message", propOrder = {
    "viewid",
    "msgid",
    "timestamp",
    "msgtype",
    "param"
})
public class Message {

    @XmlElement(required = true)
    protected ViewIdentity viewid;
    @XmlElement(required = true)
    protected MessageIdentity msgid;
    @XmlElement(required = true)
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger timestamp;
    @XmlElement(required = true)
    protected MessageType msgtype;
    protected Object param;

    /**
     * Gets the value of the viewid property.
     * 
     * @return
     *     possible object is
     *     {@link ViewIdentity }
     *     
     */
    public ViewIdentity getViewid() {
        return viewid;
    }

    /**
     * Sets the value of the viewid property.
     * 
     * @param value
     *     allowed object is
     *     {@link ViewIdentity }
     *     
     */
    public void setViewid(ViewIdentity value) {
        this.viewid = value;
    }

    /**
     * Gets the value of the msgid property.
     * 
     * @return
     *     possible object is
     *     {@link MessageIdentity }
     *     
     */
    public MessageIdentity getMsgid() {
        return msgid;
    }

    /**
     * Sets the value of the msgid property.
     * 
     * @param value
     *     allowed object is
     *     {@link MessageIdentity }
     *     
     */
    public void setMsgid(MessageIdentity value) {
        this.msgid = value;
    }

    /**
     * Gets the value of the timestamp property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the value of the timestamp property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setTimestamp(BigInteger value) {
        this.timestamp = value;
    }

    /**
     * Gets the value of the msgtype property.
     * 
     * @return
     *     possible object is
     *     {@link MessageType }
     *     
     */
    public MessageType getMsgtype() {
        return msgtype;
    }

    /**
     * Sets the value of the msgtype property.
     * 
     * @param value
     *     allowed object is
     *     {@link MessageType }
     *     
     */
    public void setMsgtype(MessageType value) {
        this.msgtype = value;
    }

    /**
     * Gets the value of the param property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getParam() {
        return param;
    }

    /**
     * Sets the value of the param property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setParam(Object value) {
        this.param = value;
    }

}
