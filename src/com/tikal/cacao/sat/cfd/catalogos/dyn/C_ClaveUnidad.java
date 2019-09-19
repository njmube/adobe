package com.tikal.cacao.sat.cfd.catalogos.dyn;

import javax.xml.bind.annotation.XmlType;

import com.tikal.cacao.sat.cfd.catalogos.CatalogoCFDI33;

@XmlType(name = "c_ClaveUnidad", namespace = "http://www.sat.gob.mx/sitio_internet/cfd/catalogos")
public class C_ClaveUnidad extends CatalogoCFDI33 {

	public C_ClaveUnidad() {
	}

	public C_ClaveUnidad(String valor) {
		super(valor);
	}
	
}
