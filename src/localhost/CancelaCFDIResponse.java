
package localhost;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Clase Java para anonymous complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="CancelaCFDIResult" type="{http://localhost/}ArrayOfAnyType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "cancelaCFDIResult"
})
@XmlRootElement(name = "CancelaCFDIResponse")
public class CancelaCFDIResponse {

    @XmlElement(name = "CancelaCFDIResult")
    protected ArrayOfAnyType cancelaCFDIResult;

    /**
     * Obtiene el valor de la propiedad cancelaCFDIResult.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfAnyType }
     *     
     */
    public ArrayOfAnyType getCancelaCFDIResult() {
        return cancelaCFDIResult;
    }

    /**
     * Define el valor de la propiedad cancelaCFDIResult.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfAnyType }
     *     
     */
    public void setCancelaCFDIResult(ArrayOfAnyType value) {
        this.cancelaCFDIResult = value;
    }

}
