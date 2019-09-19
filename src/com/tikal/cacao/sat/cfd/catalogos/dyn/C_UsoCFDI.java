package com.tikal.cacao.sat.cfd.catalogos.dyn;

import javax.xml.bind.annotation.XmlType;

import com.tikal.cacao.sat.cfd.catalogos.CatalogoCFDI33;

@XmlType(name = "c_UsoCFDI", namespace = "http://www.sat.gob.mx/sitio_internet/cfd/catalogos")
public class C_UsoCFDI extends CatalogoCFDI33 {

	public C_UsoCFDI() {
	}

	public C_UsoCFDI(String valor) {
		super(valor);
	}
	
}
