//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.03.24 at 10:04:15 AM CST 
//

package com.tikal.cacao.sat.cfd.catalogos;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

import com.google.gson.annotations.SerializedName;


/**
 * <p>Java class for c_TipoRelacion.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="c_TipoRelacion">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;whiteSpace value="collapse"/>
 *     &lt;enumeration value="01"/>
 *     &lt;enumeration value="02"/>
 *     &lt;enumeration value="03"/>
 *     &lt;enumeration value="04"/>
 *     &lt;enumeration value="05"/>
 *     &lt;enumeration value="06"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "c_TipoRelacion", namespace = "http://www.sat.gob.mx/sitio_internet/cfd/catalogos")
@XmlEnum
public enum C_TipoRelacion {

	/** Nota de cr&eacute;dito de los documentos relacionados */
	@SerializedName("01")
    @XmlEnumValue("01")
    VALUE_1("01"),
    
    /** Nota de d&eacute;bito de los documentos relacionados */
    @SerializedName("02")
    @XmlEnumValue("02")
    VALUE_2("02"),
    
    /** Devoluci&oacute;n de mercanc�a sobre facturas o traslados previos */
    @SerializedName("03")
    @XmlEnumValue("03")
    VALUE_3("03"),
    
    /** Sustituci&oacute;n de los CFDI previos */
    @SerializedName("04")
    @XmlEnumValue("04")
    VALUE_4("04"),
    
    /** Traslados de mercancias facturados previamente */
    @SerializedName("05")
    @XmlEnumValue("05")
    VALUE_5("05"),
    
    /** Factura generada por los traslados previos */
    @SerializedName("06")
    @XmlEnumValue("06")
    VALUE_6("06"),
    
    @SerializedName("07")
    @XmlEnumValue("07")
    CFDI_POR_APLICACION_DE_ANTICIPO("07"),
    
    @SerializedName("08")
    @XmlEnumValue("08")
    FACTURA_GENERADA_POR_PAGOS_EN_PARCIALIDADES("08"),
    
    @SerializedName("09")
    @XmlEnumValue("09")
    FACTURA_GENERADA_POR_PAGOS_DIFERIDOS("09");
    
    private final String value;

    C_TipoRelacion(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static C_TipoRelacion fromValue(String v) {
        for (C_TipoRelacion c: C_TipoRelacion.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
