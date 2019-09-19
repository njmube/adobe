package com.tikal.cacao.service.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.tempuri.CancelaCFDIAckResponse;
import org.tempuri.ObtieneCFDIResponse;
import org.tempuri.TimbraCFDIResponse;

import com.finkok.facturacion.cancel.CancelaCFDResult;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfGState;
import com.itextpdf.text.pdf.PdfWriter;
import com.tikal.cacao.dao.BitacoraDAO;
import com.tikal.cacao.dao.ComplementoRenglonDAO;
import com.tikal.cacao.dao.FacturaVttDAO;
import com.tikal.cacao.dao.ImagenDAO;
import com.tikal.cacao.dao.ProveedoresDAO;
import com.tikal.cacao.dao.SerialDAO;
import com.tikal.cacao.dao.sql.RegimenFiscalDAO;
import com.tikal.cacao.dao.sql.SimpleHibernateDAO;
import com.tikal.cacao.factura.Estatus;
import com.tikal.cacao.factura.RespuestaWebServicePersonalizada;
import com.tikal.cacao.factura.ws.WSClientCfdi33;
import com.tikal.cacao.factura.wsfinkok.FinkokClient;
import com.tikal.cacao.model.FacturaVTT;
import com.tikal.cacao.model.Imagen;
import com.tikal.cacao.model.PagosFacturaVTT;
import com.tikal.cacao.model.Proveedores;
import com.tikal.cacao.model.RegistroBitacora;
import com.tikal.cacao.model.Serial;
import com.tikal.cacao.model.orm.FormaDePago;
import com.tikal.cacao.model.orm.RegimenFiscal;
import com.tikal.cacao.model.orm.TipoDeComprobante;
import com.tikal.cacao.reporte.ComplementoRenglon;
import com.tikal.cacao.reporte.ReporteRenglon;
import com.tikal.cacao.sat.cfd.catalogos.dyn.C_MetodoDePago;
import com.tikal.cacao.sat.cfd33.Comprobante;
import com.tikal.cacao.sat.cfd33.Comprobante.Complemento;
import com.tikal.cacao.sat.cfd33.ObjectFactoryComprobante33;
import com.tikal.cacao.service.FacturaVTTService;
import com.tikal.cacao.service.PagosFacturaVTTService;
import com.tikal.cacao.springController.viewObjects.v33.ComprobanteConComplementoPagosVO;
import com.tikal.cacao.util.EmailSender;
import com.tikal.cacao.util.PDFFacturaV33;
import com.tikal.cacao.util.Util;

import localhost.EncodeBase64;
import mx.gob.sat.cancelacfd.Acuse;
import mx.gob.sat.pagos.Pagos;
import mx.gob.sat.pagos.Pagos.Pago;
import mx.gob.sat.timbrefiscaldigital.TimbreFiscalDigital;
import views.core.soap.services.apps.AcuseRecepcionCFDI;
import views.core.soap.services.apps.Folio;
import views.core.soap.services.apps.Incidencia;

@Service
public class PagosFacturaVTTServiceImpl implements PagosFacturaVTTService {

	@Autowired
	private FacturaVttDAO pagosFacturaVttDAO;
	
	@Autowired
	private WSClientCfdi33 wsClienteCFDI33;
	
	@Autowired
	private ImagenDAO imagenDAO;
	
	@Autowired
	private SerialDAO serialDAO;

	@Autowired
	ComplementoRenglonDAO renglonDAO;
	
	@Autowired
	@Qualifier("tipoDeComprobanteDAOH")
	private SimpleHibernateDAO<TipoDeComprobante> tipoDeComprobanteDAO;
	
	@Autowired
	@Qualifier("regimenFiscalDAOH")
	private RegimenFiscalDAO regimenFiscalDAO;
	
	@Autowired
	@Qualifier("formaDePagoDAOH")
	private SimpleHibernateDAO<FormaDePago> formaDePagoDAO;
	
	@Autowired
	private FacturaVTTService f33service;
	
	@Autowired
	BitacoraDAO bitacoradao;
	
	@Autowired
	private ProveedoresDAO proveedoresDAO;
	
	@Autowired
	private FinkokClient serviceFinkok;
	
	@Override
	public String agregar(ComprobanteConComplementoPagosVO comprobanteConComplementoPagos, Comprobante comprobante, String uuidRelacionado) {
//		Comprobante comprobante = comprobanteConComplementoPagos.getComprobante();
		Pagos complementoPagos = comprobanteConComplementoPagos.getComplementoPagos();
		
		ObjectFactoryComprobante33 of = new ObjectFactoryComprobante33();
		Complemento complemento = of.createComprobanteComplemento();
		complemento.getAny().add(complementoPagos);
		comprobante.getComplemento().add(complemento);
		
		String xmlCFDIPago = Util.marshallComprobanteConPagos(comprobante);
		
		Pago pago = complementoPagos.getPago().get(0);
		PagosFacturaVTT pagosFacturaVTT = new PagosFacturaVTT(
				Util.randomString(14), xmlCFDIPago, comprobante.getEmisor().getRfc(),
				comprobante.getReceptor().getNombre(), Util.xmlGregorianAFecha( comprobante.getFecha() ),
				null, null, uuidRelacionado, Util.xmlGregorianAFecha( pago.getFechaPago() ).toString(),
				pago.getFormaDePagoP().getValor(), pago.getMonedaP().getValor(), pago.getMonto().toPlainString());
		
		pagosFacturaVttDAO.guardar(pagosFacturaVTT);
		return "El CFDI con complemento de pago ha sido generado";
	}

	@Override
	public RespuestaWebServicePersonalizada timbrar(ComprobanteConComplementoPagosVO comprobanteConComplementoPagos, Comprobante comprobante, String uuidRelacionado) {
		
		Pagos complementoPagos = comprobanteConComplementoPagos.getComplementoPagos();
		String email = comprobanteConComplementoPagos.getEmail();
		
		ObjectFactoryComprobante33 of = new ObjectFactoryComprobante33();
		Complemento complemento = of.createComprobanteComplemento();
		complemento.getAny().add(complementoPagos);
		comprobante.getComplemento().add(complemento);
		Serial serial=serialDAO.consultar(comprobante.getEmisor().getRfc(), comprobante.getSerie());
		comprobante.setFolio(serial.getFolio()+"");
		String xmlCFDIPago = Util.marshallComprobanteConPagos(comprobante);
		
		
		Proveedores p= proveedoresDAO.getProveedores();
		int rand= this.decision(p);
		
		if (rand!=2) {
		
			TimbraCFDIResponse timbraCFDIResponse = wsClienteCFDI33.getTimbraCFDIResponse(xmlCFDIPago);
			List<Object> respuestaWB = timbraCFDIResponse.getTimbraCFDIResult().getAnyType();
			RespuestaWebServicePersonalizada respPersonalizada = null;
			int codigoRespuesta = -1;
			if (respuestaWB.get(6) instanceof Integer) {
				codigoRespuesta = (int) respuestaWB.get(6);
			
				if (codigoRespuesta == 0) {
					String xmlCFDITimbrado = (String) respuestaWB.get(3);
					Comprobante cfdiTimbrado = Util.unmarshallCFDI33XML(xmlCFDITimbrado);
					this.incrementarFolio(cfdiTimbrado.getEmisor().getRfc(), cfdiTimbrado.getSerie());
					byte[] bytesQRCode = (byte[]) respuestaWB.get(4);
					String selloDigital = (String) respuestaWB.get(5);
					
					TimbreFiscalDigital timbreFD = null;
					List<Object> listaComplemento = cfdiTimbrado.getComplemento().get(0).getAny();
					for (Object objComplemento : listaComplemento) {
						if (objComplemento instanceof TimbreFiscalDigital) {
							timbreFD = (TimbreFiscalDigital) objComplemento;
							break;
						}
					}
					
					Pago pago = complementoPagos.getPago().get(0);
					Date fechaCertificacion = Util.xmlGregorianAFecha(timbreFD.getFechaTimbrado());
					PagosFacturaVTT facturaTimbrada = new PagosFacturaVTT(timbreFD.getUUID(), xmlCFDITimbrado,
							cfdiTimbrado.getEmisor().getRfc(), cfdiTimbrado.getReceptor().getNombre(),
							fechaCertificacion, selloDigital, bytesQRCode, 
							uuidRelacionado, Util.xmlGregorianAFecha( pago.getFechaPago() ).toString(),
							pago.getFormaDePagoP().getValor(), pago.getMonedaP().getValor(), pago.getMonto().toPlainString());
					//facturaTimbrada.setComentarios(comentarios);
					pagosFacturaVttDAO.guardar(facturaTimbrada);
					
					ComplementoRenglon r= new ComplementoRenglon(facturaTimbrada);
					renglonDAO.guardar(r);
					
					
					respPersonalizada = new RespuestaWebServicePersonalizada();
					respPersonalizada.setMensajeRespuesta("¡La factura se timbró con éxito!");
					respPersonalizada.setUuidFactura(timbreFD.getUUID());
					
					RegistroBitacora br = new RegistroBitacora();
					br.setEvento("Se timbró el Complemento de pago");
					br.setFecha(new Date());
					br.setTipo("Operativo");
					br.setUsuario("Usuario");
					bitacoradao.addReg(br);
					
					return respPersonalizada;
				} // FIN TIMBRADO EXITOSO
				
				// CASO DE ERROR EN EL TIMBRADO
				else {
					RespuestaWebServicePersonalizada respuesta=Util.construirMensajeError(respuestaWB);
					RegistroBitacora br = new RegistroBitacora();
					br.setEvento(respuesta.getMensajeRespuesta()+ "xml= "+ xmlCFDIPago);
					br.setFecha(new Date());
					br.setTipo("Operativo");
					br.setUsuario("Usuario 1");
					bitacoradao.addReg(br);
					return respuesta;
				}
			} else {
				RespuestaWebServicePersonalizada respuesta=Util.construirMensajeError(respuestaWB);
				RegistroBitacora br = new RegistroBitacora();
				br.setEvento(respuesta.getMensajeRespuesta()+ "xml= "+ xmlCFDIPago);
				br.setFecha(new Date());
				br.setTipo("Operativo");
				br.setUsuario("Usuario 2");
				bitacoradao.addReg(br);
				return respuesta;
			}
		}else {
			AcuseRecepcionCFDI response= serviceFinkok.getStampResponse(xmlCFDIPago);
			
			if(response.getUUID()!=null){
			
				String xmlCFDITimbrado = response.getXml().getValue();
				Comprobante cfdiTimbrado = Util.unmarshallCFDI33XML(xmlCFDITimbrado);
				String selloDigital=response.getSatSeal().getValue();
				byte[] qr= Util.getQR(selloDigital, response.getUUID().getValue(), cfdiTimbrado.getEmisor().getRfc(), cfdiTimbrado.getReceptor().getRfc(), cfdiTimbrado.getTotal()+"");
				
				System.out.println(response.getUUID().getValue());
				this.incrementarFolio(cfdiTimbrado.getEmisor().getRfc(), cfdiTimbrado.getSerie());
				return this.procesaExitoso(cfdiTimbrado, qr, xmlCFDITimbrado, "", selloDigital, email, "finkok");
			}
			else{
				return construirMensajeError(response);
			}
		}
	}

	@Override
	public List<PagosFacturaVTT> consultarPorUuidRelacionado(String uuidRelacionado) {
		List<PagosFacturaVTT> pagosRelacionados = pagosFacturaVttDAO.consultarPorUuidRelacionado(uuidRelacionado);
		for (PagosFacturaVTT pagosFacturaVTT : pagosRelacionados) {
			pagosFacturaVTT.setCfdiXML(null);
			pagosFacturaVTT.setSelloDigital(null);
			pagosFacturaVTT.setCodigoQR(null);
		}
		return pagosRelacionados;
	}

	@Override
	public PdfWriter obtenerPDF(PagosFacturaVTT factura, OutputStream os)
			throws MalformedURLException, DocumentException, IOException {
		if (factura != null) {
			Comprobante cfdi = Util.unmarshallCFDI33XML(factura.getCfdiXML());
			Imagen imagen = imagenDAO.get(cfdi.getEmisor().getRfc());

			PDFFacturaV33 pdfFactura;
//			UsoDeCFDI usoCFDIHB = usoDeCFDIDAO.consultarPorId(cfdi.getReceptor().getUsoCFDI().getValor());
			RegimenFiscal regimenFiscal = regimenFiscalDAO
					.consultarPorId(cfdi.getEmisor().getRegimenFiscal().getValor());
			Complemento complemento= cfdi.getComplemento().get(0);
			List<Object> lista= complemento.getAny();
			FormaDePago formaDePago= null;
			for(Object element: lista){
				if(element instanceof Pagos){
					Pagos pagos= (Pagos)element;
					formaDePago = formaDePagoDAO.consultar(pagos.getPago().get(0).getFormaDePagoP().getValor());
				}
			}
			
			TipoDeComprobante tipoDeComprobante = tipoDeComprobanteDAO.consultar(cfdi.getTipoDeComprobante().getValor());
			if (regimenFiscal != null && tipoDeComprobante != null) {
				pdfFactura = new PDFFacturaV33("", regimenFiscal.getDescripcion(),
						formaDePago.getDescripcion(), tipoDeComprobante.getDescripcion());
			} else {
				pdfFactura = new PDFFacturaV33("", "", "", "");
			}

			PdfWriter writer = PdfWriter.getInstance(pdfFactura.getDocument(), os);
			pdfFactura.getPieDePagina().setUuid(factura.getUuid());
			if (factura.getEstatus().equals(Estatus.CANCELADO)) {
				pdfFactura.getPieDePagina().setFechaCancel(factura.getFechaCancelacion());
				pdfFactura.getPieDePagina().setSelloCancel(factura.getSelloCancelacion());
			}
			writer.setPageEvent(pdfFactura.getPieDePagina());

			pdfFactura.getDocument().open();
			if (factura.getEstatus().equals(Estatus.TIMBRADO))
				pdfFactura.construirPdf(cfdi, factura.getSelloDigital(), factura.getCodigoQR(), imagen,
						factura.getEstatus(), factura.getComentarios(), factura.getDatosExtra(), factura.getProveedor());
			else if (factura.getEstatus().equals(Estatus.GENERADO)) {
				pdfFactura.construirPdf(cfdi, imagen, factura.getEstatus(), factura.getComentarios());

				PdfContentByte fondo = writer.getDirectContent();
				Font fuente = new Font(FontFamily.HELVETICA, 45);
				Phrase frase = new Phrase("Pre-factura", fuente);
				fondo.saveState();
				PdfGState gs1 = new PdfGState();
				gs1.setFillOpacity(0.5f);
				fondo.setGState(gs1);
				ColumnText.showTextAligned(fondo, Element.ALIGN_CENTER, frase, 297, 650, 45);
				fondo.restoreState();
			}

			else if (factura.getEstatus().equals(Estatus.CANCELADO)) {
				pdfFactura.construirPdfCancelado(cfdi, factura.getSelloDigital(), factura.getCodigoQR(), imagen,
						factura.getEstatus(), factura.getSelloCancelacion(), factura.getFechaCancelacion(),
						factura.getComentarios(), factura.getDatosExtra(), factura.getProveedor());

				pdfFactura.crearMarcaDeAgua("CANCELADO", writer);
			}
			pdfFactura.getDocument().close();
			return writer;
			// pdfFactura.getDocument().close();
		} else {
			return null;
		}
	}
	
	private void incrementarFolio(String rfc, String serie) {
		if (rfc != null && serie != null) {
			Serial serial = serialDAO.consultar(rfc, serie);
			if (serial != null) {
				serial.incrementa();
				serialDAO.guardar(serial);
			}
		}
	}

	//@Override
//	public String cancelarAck(String uuid, String rfcEmisor, HttpSession sesion) {
//		CancelaCFDIAckResponse cancelaCFDIAckResponse = wsClienteCFDI33.getCancelaCFDIAckResponse(uuid, rfcEmisor);
//		List<Object> respuestaWB = cancelaCFDIAckResponse.getCancelaCFDIAckResult().getAnyType();
//		int codigoRespuesta = -1;
//		String strCodigoRespuesta = "";
//		if (respuestaWB.get(6) instanceof String) {
//			//codigoRespuesta = (int) respuestaWB.get(6);
//			strCodigoRespuesta = (String) respuestaWB.get(6);
//			if (strCodigoRespuesta.contentEquals("0")) {
//			//if (codigoRespuesta == 0) {
//				FacturaVTT facturaACancelar = this.pagosFacturaVttDAO.consultar(uuid);
//				ComplementoRenglon repRenglon = renglonDAO.consultar(uuid);
//				
//				String acuseXML = (String) respuestaWB.get(3);
//				StringBuilder stringBuilder = new StringBuilder(acuseXML);
//				stringBuilder.insert(106, " xmlns=\"http://cancelacfd.sat.gob.mx\" ");
//				String acuseXML2 = stringBuilder.toString();
//				facturaACancelar.setAcuseCancelacionXML(acuseXML2);
//				Acuse acuse = Util.unmarshallAcuseXML(acuseXML2);
//				
//				if (acuse != null) {
//					try {
//						EncodeBase64 encodeBase64 = new EncodeBase64();
//						String sello = new String(acuse.getSignature().getSignatureValue(), "ISO-8859-1");
//						String selloBase64 = encodeBase64.encodeStringSelloCancelacion(sello);
//						facturaACancelar.setFechaCancelacion(acuse.getFecha().toGregorianCalendar().getTime());
//						facturaACancelar.setSelloCancelacion(selloBase64);
//					} catch (UnsupportedEncodingException e) {
//						e.printStackTrace();
//						return "";
//					}	
//				} 
//				
//				facturaACancelar.setEstatus(Estatus.CANCELADO);
//				repRenglon.setStatus(Estatus.CANCELADO.toString());
//				pagosFacturaVttDAO.guardar(facturaACancelar);
//				renglonDAO.guardar(repRenglon);
//				
//				String evento = "Se canceló la factura guardada con el id:"+facturaACancelar.getUuid();
//				RegistroBitacora registroBitacora = Util.crearRegistroBitacora(sesion, "Operacional", evento);
//				bitacoradao.addReg(registroBitacora);
//				return (String)respuestaWB.get(2); // regresa "Comprobante cancelado"
//			}
//			
//			// ERROR EN LA CANCELACIÓN DEL CFDI
//			else {
//				RespuestaWebServicePersonalizada respPersonalizada = this.construirMensajeError(respuestaWB);
//				RegistroBitacora registroBitacora = Util.crearRegistroBitacora(sesion, "Operacional", respPersonalizada.getMensajeRespuesta() + "Operación CancelaAck (codigoRespuesta != 0), UUID:"+uuid);
//				bitacoradao.addReg(registroBitacora);
//				return respPersonalizada.getMensajeRespuesta();
//			}
//		} 
//		else {
//			if (respuestaWB.get(6) instanceof  String) {
//				String strRespuesta = (String) respuestaWB.get(6); 
//				if (strRespuesta.contentEquals("0")) {
//					RespuestaWebServicePersonalizada respPersonalizada = this.construirMensaje(respuestaWB);
//					RegistroBitacora registroBitacora = Util.crearRegistroBitacora(sesion, "Operacional", respPersonalizada.getMensajeRespuesta() + " UUID:"+uuid);
//					bitacoradao.addReg(registroBitacora);
//					return respPersonalizada.getMensajeRespuesta();
//				}
//			}
//			String mensaje=(String)respuestaWB.get(2);
//			if(mensaje.contains("UUID Previamente")) {
//				this.corregirFactura(uuid, rfcEmisor, sesion);
//				return "Comprobante cancelado";
//			}
//			
//			RespuestaWebServicePersonalizada respPersonalizada = this.construirMensajeError(respuestaWB);
//			RegistroBitacora registroBitacora = Util.crearRegistroBitacora(sesion, "Operacional", respPersonalizada.getMensajeRespuesta() + "Operación CancelaAck (codigoRespuesta no es Integer) UUID:"+uuid);
//			bitacoradao.addReg(registroBitacora);
//			return respPersonalizada.getMensajeRespuesta();
//		}
//
//	}

	@Override
	public String cancelarAck(String uuid, String rfcEmisor, HttpSession sesion) {
		
		FacturaVTT facturaACancelar = this.pagosFacturaVttDAO.consultar(uuid);
		ComplementoRenglon repRenglon = renglonDAO.consultar(uuid);
		
		if(repRenglon.getProveedor()!=null){
			if(repRenglon.getProveedor().compareTo("finkok")==0){
				CancelaCFDResult result= this.serviceFinkok.cancela(uuid, rfcEmisor,repRenglon.getNoCertificadoSat());
				if(result.getFolios()!=null){
					Folio folio= result.getFolios().getValue().getFolio().get(0);
					String estatusUUID= folio.getEstatusUUID().getValue();
					switch(estatusUUID) {
					case "708":{
						String acuseXML = result.getAcuse().getValue();
						acuseXML= acuseXML.replace("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">", "");
						acuseXML= acuseXML.replace("</s:Envelope>", "");
						acuseXML= acuseXML.replaceAll("<CancelaCFDResponse xmlns=\"http://cancelacfd.sat.gob.mx\">", "");
						acuseXML= acuseXML.replace("</CancelaCFDResult></CancelaCFDResponse></s:Body>", "</Acuse>");
						acuseXML= acuseXML.replace("<CancelaCFDResult", "<Acuse xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"http://cancelacfd.sat.gob.mx\"");
						return this.procesaCancelado(acuseXML, facturaACancelar, repRenglon, sesion, Estatus.CANCELADO);
					}
					case "201":{
						String acuseXML = result.getAcuse().getValue();
						acuseXML= acuseXML.replace("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">", "");
						acuseXML= acuseXML.replace("</s:Envelope>", "");
						acuseXML= acuseXML.replaceAll("<CancelaCFDResponse xmlns=\"http://cancelacfd.sat.gob.mx\">", "");
						acuseXML= acuseXML.replace("</CancelaCFDResult></CancelaCFDResponse></s:Body>", "</Acuse>");
						acuseXML= acuseXML.replace("<CancelaCFDResult", "<Acuse xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"http://cancelacfd.sat.gob.mx\"");
						
						return this.procesaCancelado(acuseXML, facturaACancelar, repRenglon, sesion, Estatus.CANCELADO);
					}
					
					case "202":{
						String acuseXML = result.getAcuse().getValue();
						acuseXML= acuseXML.replace("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">", "");
						acuseXML= acuseXML.replace("</s:Envelope>", "");
						acuseXML= acuseXML.replaceAll("<CancelaCFDResponse xmlns=\"http://cancelacfd.sat.gob.mx\">", "");
						acuseXML= acuseXML.replace("</CancelaCFDResult></CancelaCFDResponse></s:Body>", "</Acuse>");
						acuseXML= acuseXML.replace("<CancelaCFDResult", "<Acuse xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"http://cancelacfd.sat.gob.mx\"");
						
						return this.procesaCancelado(acuseXML, facturaACancelar, repRenglon, sesion, Estatus.CANCELADO);
					}
					}
					
				}else {
					String aux=result.getCodEstatus().getValue();
					String evento =aux; 
					if(aux.contains("version=\"1.0\"")) {
						aux="Error:";
						result.getCodEstatus().setValue(aux);
					}
					RegistroBitacora registroBitacora = Util.crearRegistroBitacora(sesion, "Operacional", evento);
					bitacoradao.addReg(registroBitacora);
				}
				
				return this.construirMensajeError(result).getMensajeRespuesta();
			}
		}
		
		CancelaCFDIAckResponse cancelaCFDIAckResponse = wsClienteCFDI33.getCancelaCFDIAckResponse(uuid, rfcEmisor);
		List<Object> respuestaWB = cancelaCFDIAckResponse.getCancelaCFDIAckResult().getAnyType();
		int codigoRespuesta = -1;
		String strCodigoRespuesta = "";
		if (respuestaWB.get(6) instanceof String) {
			//codigoRespuesta = (int) respuestaWB.get(6);
			strCodigoRespuesta = (String) respuestaWB.get(6);
			if (strCodigoRespuesta.contentEquals("0")) {
			//if (codigoRespuesta == 0) {
				
				
				String acuseXML = (String) respuestaWB.get(3);
				StringBuilder stringBuilder = new StringBuilder(acuseXML);
				stringBuilder.insert(106, " xmlns=\"http://cancelacfd.sat.gob.mx\" ");
				String acuseXML2 = stringBuilder.toString();
				facturaACancelar.setAcuseCancelacionXML(acuseXML2);
				Acuse acuse = Util.unmarshallAcuseXML(acuseXML2);
				
				if (acuse != null) {
					try {
						EncodeBase64 encodeBase64 = new EncodeBase64();
						String sello = new String(acuse.getSignature().getSignatureValue(), "ISO-8859-1");
						String selloBase64 = encodeBase64.encodeStringSelloCancelacion(sello);
						facturaACancelar.setFechaCancelacion(acuse.getFecha().toGregorianCalendar().getTime());
						facturaACancelar.setSelloCancelacion(selloBase64);
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
						return "";
					}	
				} 
				
				facturaACancelar.setEstatus(Estatus.CANCELADO);
				repRenglon.setStatus(Estatus.CANCELADO.toString());
				pagosFacturaVttDAO.guardar(facturaACancelar);
				renglonDAO.guardar(repRenglon);
				
				String evento = "Se canceló la factura guardada con el id:"+facturaACancelar.getUuid();
				RegistroBitacora registroBitacora = Util.crearRegistroBitacora(sesion, "Operacional", evento);
				bitacoradao.addReg(registroBitacora);
				return (String)respuestaWB.get(2); // regresa "Comprobante cancelado"
			}
			
			// ERROR EN LA CANCELACIÓN DEL CFDI
			else {
				RespuestaWebServicePersonalizada respPersonalizada = this.construirMensajeError(respuestaWB);
				RegistroBitacora registroBitacora = Util.crearRegistroBitacora(sesion, "Operacional", respPersonalizada.getMensajeRespuesta() + "Operación CancelaAck (codigoRespuesta != 0), UUID:"+uuid);
				bitacoradao.addReg(registroBitacora);
				return respPersonalizada.getMensajeRespuesta();
			}
		} 
		else {
			if (respuestaWB.get(6) instanceof  String) {
				String strRespuesta = (String) respuestaWB.get(6); 
				if (strRespuesta.contentEquals("0")) {
					RespuestaWebServicePersonalizada respPersonalizada = this.construirMensaje(respuestaWB);
					RegistroBitacora registroBitacora = Util.crearRegistroBitacora(sesion, "Operacional", respPersonalizada.getMensajeRespuesta() + " UUID:"+uuid);
					bitacoradao.addReg(registroBitacora);
					return respPersonalizada.getMensajeRespuesta();
				}
			}
			
			String mensaje=(String)respuestaWB.get(2);
			if(mensaje.contains("UUID Previamente")) {
				this.corregirFactura(uuid, rfcEmisor, sesion);
				return "Comprobante cancelado";
			}
			
			RespuestaWebServicePersonalizada respPersonalizada = this.construirMensajeError(respuestaWB);
			RegistroBitacora registroBitacora = Util.crearRegistroBitacora(sesion, "Operacional", respPersonalizada.getMensajeRespuesta() + "Operación CancelaAck (codigoRespuesta no es Integer) UUID:"+uuid);
			bitacoradao.addReg(registroBitacora);
			return respPersonalizada.getMensajeRespuesta();
		}

	}
	
		
	private RespuestaWebServicePersonalizada construirMensajeError(List<Object> respuestaWB) {
		StringBuilder respuestaError = new StringBuilder("Excepción en caso de error: ");
		respuestaError.append(respuestaWB.get(0) + "\r\n");
		respuestaError.append("Código de error: " + respuestaWB.get(1) + "\r\n");
		respuestaError.append("Mensaje de respuesta: " + respuestaWB.get(2) + "\r\n");
		respuestaError.append(respuestaWB.get(6) + "\r\n");
		respuestaError.append(respuestaWB.get(7) + "\r\n");
		respuestaError.append(respuestaWB.get(8) + "\r\n");

		RespuestaWebServicePersonalizada respPersonalizada = new RespuestaWebServicePersonalizada();
		respPersonalizada.setMensajeRespuesta(respuestaError.toString());
		return respPersonalizada;
	}
	
	private RespuestaWebServicePersonalizada construirMensaje(List<Object> respuestaWS) {
		StringBuilder respuesta = new StringBuilder("Mensaje de respuesta: ");
		respuesta.append(respuestaWS.get(0)+ "\r\n");
		respuesta.append("Código de error: " + respuestaWS.get(1) + "\r\n");
		respuesta.append("Mensaje de respuesta: " + respuestaWS.get(2) + "\r\n");
		respuesta.append("XML : " + respuestaWS.get(3) + "\r\n");
		respuesta.append("QRCode: " + respuestaWS.get(4) + "\r\n");
		respuesta.append("Sello: " + respuestaWS.get(5) + "\r\n");
		respuesta.append( respuestaWS.get(8) + "\r\n");
		
		RespuestaWebServicePersonalizada respPersonalizada = new RespuestaWebServicePersonalizada();
		respPersonalizada.setMensajeRespuesta(respuesta.toString());
		return respPersonalizada;
	}
	
	public String corregirFactura(String uuid, String rfcEmisor, HttpSession sesion) {
		FacturaVTT factura = pagosFacturaVttDAO.consultar(uuid);
		if (factura != null) {
			ObtieneCFDIResponse obtieneCFDIResponse = wsClienteCFDI33.getObtieneCFDIResponse(uuid, rfcEmisor);
			List<Object> respuestaWS = obtieneCFDIResponse.getObtieneCFDIResult().getAnyType();
			int codigoRespuesta = -1;
			if (respuestaWS.get(6) instanceof Integer) {
				codigoRespuesta = (int) respuestaWS.get(6);

				if (codigoRespuesta == 0) {
					String xml = (String) respuestaWS.get(3);
					StringBuilder stringBuilder = new StringBuilder(xml);
					stringBuilder.insert(106, " xmlns=\"http://cancelacfd.sat.gob.mx\" ");
					String acuseXML2 = stringBuilder.toString();
					factura.setAcuseCancelacionXML(acuseXML2);
					Acuse acuse = Util.unmarshallAcuseXML(acuseXML2);

					if (acuse != null) {
						try {
							EncodeBase64 encodeBase64 = new EncodeBase64();
							String sello = new String(acuse.getSignature().getSignatureValue(), "ISO-8859-1");
							String selloBase64 = encodeBase64.encodeStringSelloCancelacion(sello);
							factura.setFechaCancelacion(acuse.getFecha().toGregorianCalendar().getTime());
							factura.setSelloCancelacion(selloBase64);
							factura.setEstatus(Estatus.CANCELADO);
							pagosFacturaVttDAO.guardar(factura);
							ComplementoRenglon reporteRenglon = renglonDAO.consultar(uuid);
							reporteRenglon.setStatus(Estatus.CANCELADO.toString());
							renglonDAO.guardar(reporteRenglon);
							return "Factura " + uuid + " corregida";
						} catch (Exception e) {
							e.printStackTrace();
							return e.getMessage();
						}
					} else {
						stringBuilder = new StringBuilder(xml);
						stringBuilder.insert(107, " xmlns=\"http://cancelacfd.sat.gob.mx\" ");
						acuseXML2 = stringBuilder.toString();
						factura.setAcuseCancelacionXML(acuseXML2);
						acuse = Util.unmarshallAcuseXML(acuseXML2);
						if (acuse != null) {
							try {
								EncodeBase64 encodeBase64 = new EncodeBase64();
								String sello = new String(acuse.getSignature().getSignatureValue(), "ISO-8859-1");
								String selloBase64 = encodeBase64.encodeStringSelloCancelacion(sello);
								factura.setFechaCancelacion(acuse.getFecha().toGregorianCalendar().getTime());
								factura.setSelloCancelacion(selloBase64);
								pagosFacturaVttDAO.guardar(factura);
								ComplementoRenglon reporteRenglon = renglonDAO.consultar(uuid);
								reporteRenglon.setStatus(Estatus.CANCELADO.toString());
								renglonDAO.guardar(reporteRenglon);
								return "Factura " + uuid + " corregida";
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
								return e.getMessage();
							}
						}
						RespuestaWebServicePersonalizada respPersonalizada = this.construirMensaje(respuestaWS);
						RegistroBitacora registroBitacora = Util.crearRegistroBitacora(sesion, "Operacional",
								respPersonalizada.getMensajeRespuesta() + " UUID:" + uuid);
						bitacoradao.addReg(registroBitacora);
						return "Error al obtener el Acuse de Cancelación";

					}
				} else {
					RespuestaWebServicePersonalizada respPersonalizada = this.construirMensajeError(respuestaWS);
					RegistroBitacora registroBitacora = Util.crearRegistroBitacora(sesion, "Operacional",
							respPersonalizada.getMensajeRespuesta() + " UUID:" + uuid);
					bitacoradao.addReg(registroBitacora);
					return respPersonalizada.getMensajeRespuesta();
				}

			} else {
				RespuestaWebServicePersonalizada respPersonalizada = this.construirMensajeError(respuestaWS);
				RegistroBitacora registroBitacora = Util.crearRegistroBitacora(sesion, "Operacional",
						respPersonalizada.getMensajeRespuesta() + " UUID:" + uuid);
				bitacoradao.addReg(registroBitacora);
				return respPersonalizada.getMensajeRespuesta();
			}

		} else {
//			factura= new FacturaVTT();
//			ObtieneCFDIResponse obtieneCFDIResponse = webServiceClient33.getObtieneCFDIResponse(uuid, rfcEmisor);
//			List<Object> respuestaWS = obtieneCFDIResponse.getObtieneCFDIResult().getAnyType();
//			int codigoRespuesta = -1;
//			if (respuestaWS.get(6) instanceof Integer) {
//				codigoRespuesta = (int) respuestaWS.get(6);
//
//				if (codigoRespuesta == 0) {
//					String xmlCFDITimbrado = (String) respuestaWS.get(3);
//					Comprobante cfdiTimbrado = Util.unmarshallCFDI33XML(xmlCFDITimbrado);
//					this.incrementarFolio(cfdiTimbrado.getEmisor().getRfc(), cfdiTimbrado.getSerie());
//					byte[] bytesQRCode = (byte[]) respuestaWS.get(4);
//					String selloDigital = (String) respuestaWS.get(5);
//
//					TimbreFiscalDigital timbreFD = null;
//					List<Object> listaComplemento = cfdiTimbrado.getComplemento().get(0).getAny();
//					for (Object objComplemento : listaComplemento) {
//						if (objComplemento instanceof TimbreFiscalDigital) {
//							timbreFD = (TimbreFiscalDigital) objComplemento;
//							break;
//						}
//					}
//
//					Date fechaCertificacion = Util.xmlGregorianAFecha(timbreFD.getFechaTimbrado(),true);
//					FacturaVTT facturaTimbrada = new FacturaVTT(timbreFD.getUUID(), xmlCFDITimbrado,
//							cfdiTimbrado.getEmisor().getRfc(), cfdiTimbrado.getReceptor().getRfc(), fechaCertificacion,
//							selloDigital, bytesQRCode);
////					facturaTimbrada.setComentarios(comentarios);
//					facturaVTTDAO.guardar(facturaTimbrada);
////					this.crearReporteRenglon(facturaTimbrada, timbreFD.getNoCertificadoSAT());
//					this.crearReporteRenglon(facturaTimbrada, cfdiTimbrado.getMetodoPago(), cfdiTimbrado.getTipoDeComprobante().getValor(),null);
//
//					EmailSender mailero = new EmailSender();
//					Imagen imagen = imagenDAO.get(cfdiTimbrado.getEmisor().getRfc());
////					if (email != null) {
////						try{
////							mailero.enviaFactura(email, facturaTimbrada, "", imagen, cfdiTimbrado);
////						}catch(Exception e){
////							
////						}
////					}
//				 // FIN TIMBRADO EXITOSO
//
//				// CASO DE ERROR EN EL TIMBRADO
//				
//						RespuestaWebServicePersonalizada respPersonalizada = this.construirMensaje(respuestaWS);
//						RegistroBitacora registroBitacora = Util.crearRegistroBitacora(sesion, "Operacional",
//								respPersonalizada.getMensajeRespuesta() + " UUID:" + uuid);
//						bitacoradao.addReg(registroBitacora);
//						return "Error al obtener el Acuse de Cancelación";
//
//					
//			
//				} else {
//					RespuestaWebServicePersonalizada respPersonalizada = this.construirMensajeError(respuestaWS);
//					RegistroBitacora registroBitacora = Util.crearRegistroBitacora(sesion, "Operacional",
//							respPersonalizada.getMensajeRespuesta() + " UUID:" + uuid);
//					bitacoradao.addReg(registroBitacora);
//					return respPersonalizada.getMensajeRespuesta();
//				}
//
//			} else {
//				RespuestaWebServicePersonalizada respPersonalizada = this.construirMensajeError(respuestaWS);
//				RegistroBitacora registroBitacora = Util.crearRegistroBitacora(sesion, "Operacional",
//						respPersonalizada.getMensajeRespuesta() + " UUID:" + uuid);
//				bitacoradao.addReg(registroBitacora);
//				return respPersonalizada.getMensajeRespuesta();
//			}
			
			return "No existe la factura";
		}

	}
	
	private int decision(Proveedores p){
		if(p.isActivo(0) && p.isActivo(1)){
			return ThreadLocalRandom.current().nextInt(0, 3);
		}
		if(p.isActivo(0)){
			return 0;
		}
		return 2;
	}
	
	private RespuestaWebServicePersonalizada procesaExitoso(Comprobante cfdiTimbrado, byte[] bytesQRCode, String xmlCFDITimbrado, String comentarios, String selloDigital, String email, String proveedor){
		RespuestaWebServicePersonalizada respPersonalizada = null;
		TimbreFiscalDigital timbreFD = null;
		List<Object> listaComplemento = cfdiTimbrado.getComplemento().get(0).getAny();
		for (Object objComplemento : listaComplemento) {
			if (objComplemento instanceof TimbreFiscalDigital) {
				timbreFD = (TimbreFiscalDigital) objComplemento;
				break;
			}
		}

		
		Date fechaCertificacion = Util.xmlGregorianAFecha(timbreFD.getFechaTimbrado(),true);
		PagosFacturaVTT facturaTimbrada = new PagosFacturaVTT(timbreFD.getUUID(), xmlCFDITimbrado,
				cfdiTimbrado.getEmisor().getRfc(), cfdiTimbrado.getReceptor().getRfc(), fechaCertificacion,
				selloDigital, bytesQRCode);
		facturaTimbrada.setProveedor(proveedor);
		facturaTimbrada.setComentarios(comentarios);
		facturaTimbrada.setProveedor("finkok");
		pagosFacturaVttDAO.guardar(facturaTimbrada);
//		this.crearReporteRenglon(facturaTimbrada, cfdiTimbrado.getNoCertificado());
		
		this.crearReporteRenglon(facturaTimbrada, cfdiTimbrado.getMetodoPago(), cfdiTimbrado.getTipoDeComprobante().getValor(),cfdiTimbrado.getNoCertificado(),"finkok");
		
		EmailSender mailero = new EmailSender();
		Imagen imagen = imagenDAO.get(cfdiTimbrado.getEmisor().getRfc());
		if (email != null) {
			try{
				mailero.enviaFactura(email, facturaTimbrada, "", imagen, cfdiTimbrado);
			}catch(Exception e){
				
			}
		}
		respPersonalizada = new RespuestaWebServicePersonalizada();
		respPersonalizada.setMensajeRespuesta("¡La factura se timbró con éxito!");
		respPersonalizada.setUuidFactura(timbreFD.getUUID());
		return respPersonalizada;
	}
	
private void crearReporteRenglon(FacturaVTT factura, C_MetodoDePago metodoPago, String tipo,String noCertificadoSat, String proveedor){
		
		switch(tipo){
		case "P":{
			ComplementoRenglon reporterenglon= new ComplementoRenglon(factura);
			reporterenglon.setNoCertificadoSat(noCertificadoSat);
			reporterenglon.setProveedor(proveedor);
			renglonDAO.guardar(reporterenglon);
			break;
		}
		default:{
			ReporteRenglon reporteRenglon = new ReporteRenglon(factura);
			reporteRenglon.setNoCertificadoSat(noCertificadoSat);
			if(metodoPago.getValor().compareTo("PPD")==0){
				reporteRenglon.setTieneComplementoPago(true);
			}
//			repRenglonDAO.guardar(reporteRenglon);
			break;
		}
		}
		
//		ReporteRenglon reporteRenglon = new ReporteRenglon(factura);
//		reporteRenglon.setNoCertificadoSat(noCertificadoSat);
//		repRenglonDAO.guardar(reporteRenglon);
	}

	private RespuestaWebServicePersonalizada construirMensajeError(AcuseRecepcionCFDI respuestaWB) {
		Incidencia incidencia= respuestaWB.getIncidencias().getValue().getIncidencia().get(0);
		StringBuilder respuestaError = new StringBuilder("2 - Excepción en caso de error: ");
	//	respuestaError.append(incidencia.get + "\r\n");
		respuestaError.append("Código de error: " + incidencia.getCodigoError().getValue() + "\r\n");
		respuestaError.append("Mensaje de respuesta: " + incidencia.getMensajeIncidencia().getValue() + "\r\n");
		respuestaError.append(incidencia.getExtraInfo().getValue() + "\r\n");
	//	respuestaError.append(respuestaWB.get(7) + "\r\n");	
	//	respuestaError.append(respuestaWB.get(8) + "\r\n");
	
		RespuestaWebServicePersonalizada respPersonalizada = new RespuestaWebServicePersonalizada();
		respPersonalizada.setMensajeRespuesta(respuestaError.toString());
		return respPersonalizada;
	}
	
//	private String procesaCancelado(String acuseXML, PagosFacturaVTT facturaACancelar, ComplementoRenglon repRenglon, HttpSession sesion, Estatus estatus){
//		String evento="";
//		if(acuseXML!=null) {
//			facturaACancelar.setAcuseCancelacionXML(acuseXML);
//			Acuse acuse = Util.unmarshallAcuseXML(acuseXML);
//	
//			if (acuse != null) {
//				try {
//					EncodeBase64 encodeBase64 = new EncodeBase64();
//					String sello = new String(acuse.getSignature().getSignatureValue(), "ISO-8859-1");
//					String selloBase64 = encodeBase64.encodeStringSelloCancelacion(sello);
//					facturaACancelar.setFechaCancelacion(acuse.getFecha().toGregorianCalendar().getTime());
//					facturaACancelar.setSelloCancelacion(selloBase64);
//					evento = "Se actualizó con acuse la factura guardada con el id:" + facturaACancelar.getUuid()+" Estatus: "+estatus;
//				} catch (UnsupportedEncodingException e) {
//					e.printStackTrace();
//					return "";
//				}
//			}
//		}else {
//			evento = "Se actualizó la factura guardada con el id:" + facturaACancelar.getUuid()+" Estatus: "+estatus;
//		}
//		facturaACancelar.setEstatus(estatus);
//		repRenglon.setStatus(estatus.toString());
//		pagosFacturaVttDAO.guardar(facturaACancelar);
//		renglonDAO.guardar(repRenglon);
//
//		
//		RegistroBitacora registroBitacora = Util.crearRegistroBitacora(sesion, "Operacional", evento);
//		bitacoradao.addReg(registroBitacora);
//		return evento;
//	}
//	
	private String procesaCancelado(String acuseXML, FacturaVTT facturaACancelar, ComplementoRenglon repRenglon, HttpSession sesion, Estatus estatus){
		String evento="";
		if(acuseXML!=null) {
			facturaACancelar.setAcuseCancelacionXML(acuseXML);
			Acuse acuse = Util.unmarshallAcuseXML(acuseXML);
	
			if (acuse != null) {
				try {
					EncodeBase64 encodeBase64 = new EncodeBase64();
					String sello = new String(acuse.getSignature().getSignatureValue(), "ISO-8859-1");
					String selloBase64 = encodeBase64.encodeStringSelloCancelacion(sello);
					facturaACancelar.setFechaCancelacion(acuse.getFecha().toGregorianCalendar().getTime());
					facturaACancelar.setSelloCancelacion(selloBase64);
					evento = "Se actualizó con acuse la factura guardada con el id:" + facturaACancelar.getUuid()+" Estatus: "+estatus;
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					return "";
				}
			}
		}else {
			evento = "Se actualizó la factura guardada con el id:" + facturaACancelar.getUuid()+" Estatus: "+estatus;
		}
		facturaACancelar.setEstatus(estatus);
		repRenglon.setStatus(estatus.toString());
		pagosFacturaVttDAO.guardar(facturaACancelar);
		renglonDAO.guardar(repRenglon);

		
		RegistroBitacora registroBitacora = Util.crearRegistroBitacora(sesion, "Operacional", evento);
		bitacoradao.addReg(registroBitacora);
		return evento;
	}
	private RespuestaWebServicePersonalizada construirMensajeError(CancelaCFDResult respuestaWB) {
		String codigo="";
		String mensaje="";
		if(respuestaWB.getFolios()!=null) {
		Folio f= respuestaWB.getFolios().getValue().getFolio().get(0);
			if(f.getEstatusUUID().getValue().compareTo("708")==0){
				codigo= "708";
				mensaje="Ocurrió un error en la cancelación, por favor contactar a soporte";
			}
		}else{
			codigo= "666";
			mensaje= respuestaWB.getCodEstatus().getValue();
		}
		
		StringBuilder respuestaError = new StringBuilder("Excepción en caso de error: ");
		respuestaError.append("Código de error: " + codigo + "\r\n");
		respuestaError.append("Mensaje de respuesta: " + mensaje + "\r\n");

		RespuestaWebServicePersonalizada respPersonalizada = new RespuestaWebServicePersonalizada();
		respPersonalizada.setMensajeRespuesta(respuestaError.toString());
		return respPersonalizada;
	}
	
}
