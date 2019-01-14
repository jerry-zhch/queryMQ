package com.ucmed.webService;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface MqService {

    @WebResult(name = "response")
    @WebMethod(action = "http://webService.ucmed.com/#hlwyy")
    String sendMq(@WebParam(name = "header") String header,@WebParam(name = "message") String message)
            throws Exception;

    @WebResult(name = "response")
    @WebMethod(action = "http://webService.ucmed.com/#hlwyy")
    String destroyEnvironment(@WebParam(name = "ip") String ip)
            throws Exception;


}
