/**
 * 
 */
package com.tikal.cacao.model;

import com.google.gson.annotations.SerializedName;

/**
 * @author Tikal
 *
 */
public enum Estado {
	@SerializedName("Aguascalientes")
	AGUASCALIENTES("Aguascalientes"),
	
	@SerializedName("Baja California")
	BAJA_CALIFORNIA("Baja California"),
	
	@SerializedName("Baja California Sur")
	BAJA_CALIFORNIA_SUR("Baja California Sur"),
	
	@SerializedName("Campeche")
	CAMPECHE("Campeche"),
	
	@SerializedName("Chiapas")
	CHIAPAS("Chiapas"),
	
	@SerializedName("Chihuahua")
	CHIHUAHUA("Chihuahua"),
	
	@SerializedName("Coahuila")
	COAHUILA("Coahuila"),
	
	@SerializedName("Colima")
	COLIMA("Colima"),
	
	@SerializedName("Ciudad de M�xico")
	CIUDAD_DE_MEXICO("Ciudad de M�xico"),
	
	@SerializedName("Durango")
	DURANGO("Durango"),
	
	@SerializedName("Estado de M�xico")
	ESTADO_DE_MEXICO("Estado de M�xico"),
	
	@SerializedName("Guanajuato")
	GUANAJUATO("Guanajuato"),
	
	@SerializedName("Guerrero")
	GUERRERO("Guerrero"),
	
	@SerializedName("Hidalgo")
	HIDALGO("Hidalgo"),
	
	@SerializedName("Jalisco")
	JALISCO("Jalisco"),
	
	@SerializedName("Michoac�n")
	MICHOACAN("Michoac�n"),
	
	@SerializedName("Morelos")
	MORELOS("Morelos"),
	
	@SerializedName("Nayarit")
	NAYARIT("Nayarit"),
	
	@SerializedName("Nuevo Le�n")
	NUEVO_LEON("Nuevo Le�n"),
	
	@SerializedName("Oaxaca")
	OAXACA("Oaxaca"),
	
	@SerializedName("Puebla")
	PUEBLA("Puebla"),
	
	@SerializedName("Quer�taro")
	QUERETARO("Quer�taro"),
	
	@SerializedName("Quintana Roo")
	QUINTANA_ROO("Quintana Roo"),
	
	@SerializedName("San Luis Potos�")
	SAN_LUIS_POTOSI("San Luis Potos�"),
	
	@SerializedName("Sinaloa")
	SINALOA("Sinaloa"),
	
	@SerializedName("Sonora")
	SONORA("Sonora"),
	
	@SerializedName("Tabasco")
	TABASCO("Tabasco"),
	
	@SerializedName("Tamaulipas")
	TAMAULIPAS("Tamaulipas"),
	
	@SerializedName("Tlaxcala")
	TLAXCALA("Tlaxcala"),
	
	@SerializedName("Veracruz")
	VERACRUZ("Veracruz"),
	
	@SerializedName("Yucat�n")
	YUCATAN("Yucat�n"),
	
	@SerializedName("Zacatecas")
	ZACATECAS("Zacatecas");
	
	private String brandname;
	
	private Estado(String brandname) {
		this.brandname = brandname;
	}

	/* (non-Javadoc)
	 * @see java.lang.Enum#toString()
	 */
	@Override
	public String toString() {
		return this.brandname;
	}
	

}
