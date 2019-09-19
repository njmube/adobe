package com.tikal.cacao.model.orm;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "forma_pago")
@AttributeOverride(name = "id", column = @Column(name = "id_forma_pago"))
public class FormaDePago extends EntidadCatalogo{
//TODO throwa Caused by: com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException: 
	//Unknown column 'formadepag0_.id' in 'field list'
	
	
}
