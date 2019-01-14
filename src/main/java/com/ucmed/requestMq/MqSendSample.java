package com.ucmed.requestMq;

import java.util.Hashtable;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQPutMessageOptions;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.MQConstants;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class MqSendSample {
	private static final Logger logger=Logger.getLogger(MqSendSample.class);
	@SuppressWarnings("rawtypes")
	private static Hashtable<String, Comparable> env = new Hashtable<String, Comparable>();

	// 队列管理器名
	private static String queueManagerName;
	// 队列管理器引用
	private static MQQueueManager queueManager;
	// 队列名
	private static String queueName;
	// 队列引用
	private static MQQueue queue;

	/**
	 * <b>应用启动时初始化队列管理器连�?/b> <br>
	 * 由于连接队列管理器如同连接数据一样，建立时需要资源较多， 连接时间较长�?因此不要每次创建关闭�?
	 * 建议应用程序保持�?�� 或多个队列管理器连接�? 但应用关闭时注意关闭连接，释放资源！
	 */
	public static void initEnvironment(String ip,String userId,String queryName) throws Exception {

		if (queueManager == null || env==null || !env.get(CMQC.HOST_NAME_PROPERTY).equals(ip) ||
				!env.get(CMQC.USER_ID_PROPERTY).equals(userId) || !queueName.equals(queryName)) {
			//重连之前最好断开之前的连接？？？
//            destroyEnvironment();
			logger.info("initEnvironment====>");

			// 服务器地id、名称
			env.put(CMQC.HOST_NAME_PROPERTY, ip);
			// 连接通道
			env.put(CMQC.CHANNEL_PROPERTY, "IE.SVRCONN");
			// 服务器MQ服务使用的编1381代表GBK,1208代表UTF(Coded Character Set Identifier:CCSID) ,1383
			env.put(CMQC.CCSID_PROPERTY, 1208);
			// 端口号
			env.put(CMQC.PORT_PROPERTY, 5000);
			// 传输类型
			env.put(CMQC.TRANSPORT_PROPERTY, CMQC.TRANSPORT_MQSERIES);
			// 用户标识
			env.put(CMQC.USER_ID_PROPERTY, userId);
			// 设置目标队列管理器
			queueManagerName = "GWI.QM";
			// 设置目标队列
			queueName = queryName;
			// 建立队列管理器连接
			connectQM();
		}
	}

	/**
	 * 程序结束时释放队列管理连接资源
	 *
	 * @throws Exception
	 */
	public static void destroyEnvironment() throws Exception {
		logger.info("destroyEnvironment=====>");
		disconnectQM();
	}

	public static String testSend(String message,String hospital_id,String service_id,String domain_id,String
			apply_unit_id,String send_sys_id,String exec_unit_id,String order_exec_id) throws Exception {
//		logger.info("testSend=====>");
		// 队列打开参数
		int openOptions = MQConstants.MQOO_BIND_AS_Q_DEF| MQConstants.MQOO_OUTPUT;
		// 打开队列(同一线程内，同时只能打开该队列一次)
		queue = queueManager.accessQueue(queueName, openOptions);
		// 设置发送消息参数为：具有同步性，及支持事务
		MQPutMessageOptions pmo = new MQPutMessageOptions();
		pmo.options = MQConstants.MQPMO_SYNCPOINT;
		try {

			MQMessage msg = null;
			// 设置消息格式为字符串类型
			msg = new MQMessage();
			msg.format = MQConstants.MQFMT_STRING;
			// 设置自定义消息头（厂商接入时根据自己的业务设定消息头）
			// 医疗机构代码
			msg.setStringProperty("hospital_id", hospital_id);
			// 消息ID
			msg.setStringProperty("service_id", service_id);
			// 就诊类别ID(01 门诊,2 急诊,0201 普通急诊,0202 急诊留观,03 住院,04 体检,0401 普通体检,0402 干保体检,
			// 05 转院)
			msg.setStringProperty("domain_id", domain_id);
			// 申请科室ID
			msg.setStringProperty("apply_unit_id", apply_unit_id);
			// 发送系统ID
			msg.setStringProperty("send_sys_id", send_sys_id);
			// 执行科室ID
			msg.setStringProperty("exec_unit_id", exec_unit_id);
			// 医嘱执行分类编码
			msg.setStringProperty("order_exec_id", order_exec_id);
			// 扩展码（empi使用时放入域ID，其它系统标0）
			msg.setStringProperty("extend_sub_id", "0");

			// 消息内容编码(1208:utf-8)
			msg.characterSet = 1208;

			// 消息内容
//			String message=Sample.medical;
//			String message = "<msg>你好123<msg>";
//			 设置消息内容
			msg.writeString(message);

			// 消息放入隊列
			queue.put(msg, pmo);

			// 提交事务
			queueManager.commit();

			String str = SimpleStringUtil2.Bytes2HexString(msg.messageId).toLowerCase();
//			logger.info("send mq message success =====> MsgId.len:" + str.length() + "~~~~MsgId:" + str);
			return "<<ok>>:MsgId="+str;

		} catch (MQException e) {

			// 事务回滚
			queueManager.backout();
			// e.printStackTrace();
			if (e.reasonCode == MQConstants.MQRC_CHANNEL_NOT_AVAILABLE
					|| e.reasonCode == MQConstants.MQRC_CONNECTION_BROKEN
					|| e.reasonCode == MQConstants.MQRC_CONNECTION_ERROR
					|| e.reasonCode == MQConstants.MQRC_CONNECTION_STOPPED
					|| e.reasonCode == MQConstants.MQRC_Q_MGR_QUIESCING) {
				// 设定重新连接（接入厂商考虑重新连接设定）
				connectQM();
				logger.info("send mq message error1==>队列管理器连接不可用，需要重新创建队列管理器!");
			} else {
				logger.info("send mq message error2==>"+e.getMessage());
//				throw e;
			}
			return "<<fail>>:"+e.getMessage();
		} finally {
			// 关闭队列
			if (queue != null) {
				queue.close();
			}
			// 断开连接（此为Demo，厂商接入时要用长连接，不要发一次消息建立一个连接）
//			disconnectQM();
		}
	}

	/**
	 * 建立连接
	 * @throws Exception
	 */
	private static void connectQM() throws Exception {
//		logger.info("connectQM=====>");
		queueManager = new MQQueueManager(queueManagerName, env);
	}

	/**
	 * 释放连接
	 * @throws Exception
	 */
	private static void disconnectQM() throws Exception {
		logger.info("disconnectQM=====>");
		if (queueManager != null) {
			queueManager.disconnect();
		}
	}
}
