package com.ucmed.webService;

import com.ucmed.requestMq.MqSendSample;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.stereotype.Service;

import javax.jws.WebService;
import java.io.StringReader;

@WebService(endpointInterface = "com.ucmed.webService.MqService")
@Service
public class MqServiceImpl implements MqService {
    private static final Logger LOGGER=Logger.getLogger(MqServiceImpl.class);

    @Override
    public String sendMq(String header, String message) throws Exception{
        SAXReader saxReader=new SAXReader();
//        LOGGER.info("request message =====>"+message);
        Document documentHeader=saxReader.read((new StringReader(header)));
        LOGGER.info("request header ====>"+documentHeader);
        Element rootElement=documentHeader.getRootElement();
        String ip=rootElement.elementText("ip");
        String userId=rootElement.elementText("userId");
        String queryName=rootElement.elementText("queryName");
        String hospitalId=rootElement.elementText("hospitalId");
        String serviceId=rootElement.elementText("serviceId");
        String domainId=rootElement.elementText("domainId");
        String applyUnitId=rootElement.elementText("applyUnitId");
        String sendSysId=rootElement.elementText("sendSysId");
        String execUnitId=rootElement.elementText("execUnitId");
        String orderExecId=rootElement.elementText("orderExecId");

        String res;
        try {
            MqSendSample.initEnvironment(ip,userId,queryName);
            res=MqSendSample.testSend(message,hospitalId,serviceId,domainId,applyUnitId,
                    sendSysId,execUnitId,orderExecId);
        }catch (Exception e){
            try {
                //发生异常中断连接并重试
                MqSendSample.destroyEnvironment();
                MqSendSample.initEnvironment(ip,userId,queryName);
                res=MqSendSample.testSend(message,hospitalId,serviceId,domainId,applyUnitId,
                        sendSysId,execUnitId,orderExecId);
            }catch (Exception e2){
                res=e2.getMessage();
            }
//            LOGGER.info("catch =====>"+e.getMessage());
        }finally {
//            mqSendSample.destroyEnvironment();
//            LOGGER.info("finally =====> ");
        }
        LOGGER.info("response =====>"+res);
        return res;
    }

    @Override
    public String destroyEnvironment(String ip) throws Exception {

        try {
//            MqSendSample mqSendSample=new MqSendSample();
//            mqSendSample.destroyEnvironment();
            return "not implemented";
        }catch (Exception e){
            return e.getMessage();
        }
    }
}
