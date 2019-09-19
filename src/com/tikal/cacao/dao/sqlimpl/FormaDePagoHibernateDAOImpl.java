package com.tikal.cacao.dao.sqlimpl;

import java.util.List;

import com.tikal.cacao.dao.sql.SimpleHibernateDAO;
import com.tikal.cacao.model.orm.FormaDePago;

public class FormaDePagoHibernateDAOImpl extends AbstractDAOHibernate implements SimpleHibernateDAO<FormaDePago> {

	@Override
	public void guardar(FormaDePago entity) {
		this.initSessionTx();
		sesion.persist(entity);
		this.cleanSessionTx();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<FormaDePago> consultarTodos() {
		this.sesion = this.sessionFactory.openSession();
		String hql = "select fp.id, fp.descripcion from FormaDePago fp";
		List<FormaDePago> listaFormasPago = this.sesion.createQuery(hql).list();
		this.sesion.close();
		return listaFormasPago;
	}

	@Override
	public FormaDePago consultar(String id) {
		super.sesion = super.sessionFactory.openSession();
		FormaDePago formaDePago = (FormaDePago) super.sesion.get(FormaDePago.class, id);
		if (super.sesion.isConnected()) {
			super.sesion.close();
		}
		return formaDePago;
	}

}
