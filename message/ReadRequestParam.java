//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.10.01 at 09:31:05 AM PHT 
//


package reader_writer.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ReadRequestParam complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ReadRequestParam">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="return" type="{}ErrorType"/>
 *         &lt;element name="fileurn" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReadRequestParam", propOrder = {
    "_return",
    "fileurn"
})
public class ReadRequestParam {

    @XmlElement(name = "return", required = true)
    protected ErrorType _return;
    @XmlElement(required = true)
    protected String fileurn;

    /**
     * Gets the value of the return property.
     * 
     * @return
     *     possible object is
     *     {@link ErrorType }
     *     
     */
    public ErrorType getReturn() {
        return _return;
    }

    /**
     * Sets the value of the return property.
     * 
     * @param value
     *     allowed object is
     *     {@link ErrorType }
     *     
     */
    public void setReturn(ErrorType value) {
        this._return = value;
    }

    /**
     * Gets the value of the fileurn property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFileurn() {
        return fileurn;
    }

    /**
     * Sets the value of the fileurn property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFileurn(String value) {
        this.fileurn = value;
    }

}