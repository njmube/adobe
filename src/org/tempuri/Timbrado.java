package org.tempuri;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.Service;


/**
 * This class was generated by Apache CXF 3.1.10
 * 2017-04-20T16:07:30.656-05:00
 * Generated source version: 3.1.10
 * 
 */
@WebServiceClient(name = "Timbrado", 
                  wsdlLocation = "https://cfdi33-pruebas.buzoncfdi.mx:1443/Timbrado.asmx?WSDL",
                  targetNamespace = "http://tempuri.org/") 
public class Timbrado extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://tempuri.org/", "Timbrado");
    public final static QName TimbradoSoap12 = new QName("http://tempuri.org/", "TimbradoSoap12");
    public final static QName TimbradoSoap = new QName("http://tempuri.org/", "TimbradoSoap");
    static {
        URL url = null;
        try {
            url = new URL("https://cfdi33-pruebas.buzoncfdi.mx:1443/Timbrado.asmx?WSDL");
        } catch (MalformedURLException e) {
            java.util.logging.Logger.getLogger(Timbrado.class.getName())
                .log(java.util.logging.Level.INFO, 
                     "Can not initialize the default wsdl from {0}", "https://cfdi33-pruebas.buzoncfdi.mx:1443/Timbrado.asmx?WSDL");
        }
        WSDL_LOCATION = url;
    }

    public Timbrado(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public Timbrado(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public Timbrado() {
        super(WSDL_LOCATION, SERVICE);
    }
    
    public Timbrado(WebServiceFeature ... features) {
        super(WSDL_LOCATION, SERVICE, features);
    }

    public Timbrado(URL wsdlLocation, WebServiceFeature ... features) {
        super(wsdlLocation, SERVICE, features);
    }

    public Timbrado(URL wsdlLocation, QName serviceName, WebServiceFeature ... features) {
        super(wsdlLocation, serviceName, features);
    }    




    /**
     *
     * @return
     *     returns TimbradoSoap
     */
    @WebEndpoint(name = "TimbradoSoap12")
    public TimbradoSoap getTimbradoSoap12() {
        return super.getPort(TimbradoSoap12, TimbradoSoap.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns TimbradoSoap
     */
    @WebEndpoint(name = "TimbradoSoap12")
    public TimbradoSoap getTimbradoSoap12(WebServiceFeature... features) {
        return super.getPort(TimbradoSoap12, TimbradoSoap.class, features);
    }


    /**
     *
     * @return
     *     returns TimbradoSoap
     */
    @WebEndpoint(name = "TimbradoSoap")
    public TimbradoSoap getTimbradoSoap() {
        return super.getPort(TimbradoSoap, TimbradoSoap.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns TimbradoSoap
     */
    @WebEndpoint(name = "TimbradoSoap")
    public TimbradoSoap getTimbradoSoap(WebServiceFeature... features) {
        return super.getPort(TimbradoSoap, TimbradoSoap.class, features);
    }

}
