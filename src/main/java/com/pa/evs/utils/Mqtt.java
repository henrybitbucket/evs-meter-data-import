package com.pa.evs.utils;
import java.io.Serializable;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.ssl.SSLContexts;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
<pre>
	<dependency>
	    <groupId>org.eclipse.paho</groupId>
	    <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
	    <version>1.2.0</version>
	</dependency>
</pre>
 * @author thanh
 *
 */
public class Mqtt {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Mqtt.class);
	private static final String MQTT_PUBLISHER_ID = "be-server";
	private static final String MQTT_SERVER_ADDRES = "ssl://localhost:7773";//ssl: "ssl://localhost:8883", none-ssl: "tcp://3.1.87.138:8883"
	
	private static final Map<String, Lock> LOCKS = new ConcurrentHashMap<>();
	private static final Map<String, IMqttClient> INSTANCES = new ConcurrentHashMap<>();
	
	private static final ObjectMapper MAPPER = new ObjectMapper();

	public static IMqttClient getInstance() {
		return getInstance(null);
	}
	
	public static IMqttClient getInstance(String serverAddres) {
		
		if (serverAddres == null) {
			serverAddres = MQTT_SERVER_ADDRES;
		}
		
		Lock lock = LOCKS.computeIfAbsent(serverAddres, k -> new ReentrantLock());
		lock.lock();
		IMqttClient instance = INSTANCES.get(serverAddres);
		try {
			
			if (instance == null) {
				instance = new MqttClient(serverAddres, MQTT_PUBLISHER_ID + "." + System.currentTimeMillis());
				INSTANCES.put(serverAddres, instance);
			}
			
			MqttConnectOptions options = new MqttConnectOptions();
			options.setAutomaticReconnect(true);
			options.setCleanSession(true);
			options.setConnectionTimeout(10);
			if (serverAddres.startsWith("ssl://")) {
				TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
					public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
						return true;
					}
				};
				SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
				options.setSocketFactory(sslContext.getSocketFactory());
			}
			options.setUserName("admin");
			options.setPassword("1234567".toCharArray());
			if (!instance.isConnected()) {
				instance.connect(options);
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		} finally {
			lock.unlock();
		}

		return instance;
	}

	private Mqtt() {

	}

	public static void publish(String topic, Payload<?> messages)
			throws Exception {
		publish(null, topic, messages, 2, true);
	}
	
	public static void publish(IMqttClient instance, String topic, Payload<?> messages)
			throws Exception {
		publish(instance, topic, messages, 2, true);
	}
	
	public static void publish(String topic, Object messages)
			throws Exception {
		publish(null, topic, messages, 2, true);
	}
	
	public static void publish(IMqttClient instance, String topic, Object messages)
			throws Exception {
		publish(instance, topic, messages, 2, true);
	}
	
	public static void publish(String topic, Object messages, int qos, boolean retained)
			throws Exception {
		publish(null, topic, MAPPER.writeValueAsBytes(messages), qos, retained);
	}
	
	public static void publish(IMqttClient instance, String topic, Object messages, int qos, boolean retained)
			throws Exception {
		publish(instance, topic, MAPPER.writeValueAsBytes(messages), qos, retained);
	}
	
	private static void publish(IMqttClient instance, String topic, byte[] messages, int qos, boolean retained)
			throws InterruptedException, org.eclipse.paho.client.mqttv3.MqttException {

		try {
			MqttMessage mqttMessage = new MqttMessage(messages);
	        mqttMessage.setQos(qos);
	        mqttMessage.setRetained(retained);

	        if (instance == null) {
	        	instance = Mqtt.getInstance();
	        }
	        Mqtt.getInstance().publish(topic, mqttMessage);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	@SafeVarargs
	public static void subscribe(IMqttClient instance, String topic, Function<Object, Object>... fns) throws Exception {
		try {
			if (instance == null) {
				instance = Mqtt.getInstance();
			}
			instance.subscribe(topic, (s, mqttMessage) -> {
				for (Function<Object, Object> fn : fns) {
					fn.apply(mqttMessage);
				}
			});	
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	@SafeVarargs
	public static void subscribe(String topic, Function<Object, Object>... fns) throws Exception {
		subscribe(null, topic, fns);
	}
	
	public static class Payload<T> implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6467330612675756901L;
		
		private final String key;
		
		private final T data;

		public Payload(String key, T data) {
			this.key = key;
			this.data = data;
		}

		public String getKey() {
			return key;
		}

		public T getData() {
			return data;
		}

		@Override
		public String toString() {
			return "Payload [key=" + key + "]";
		}
	}
	
	public static void main1(String[] args) throws Exception {

		Mqtt.subscribe("evs/pa/data", o -> {
			MqttMessage mqttMessage = (MqttMessage) o;
			LOG.info("1 -> " + new String(mqttMessage.getPayload()));
			return null;
		});

		Mqtt.subscribe("test2", o -> {
			MqttMessage mqttMessage = (MqttMessage) o;
			LOG.info("2 -> " + new String(mqttMessage.getPayload()));
			return null;
		});

		Mqtt.subscribe("test", o -> {
			MqttMessage mqttMessage = (MqttMessage) o;
			LOG.info(new String(mqttMessage.getPayload()));
			return null;
		});

		while (true) {
			Mqtt.publish("test", new Payload<>("Key1", new ArrayList<>()), 2, true);
			Mqtt.publish("test2", new Payload<>("Key2", new ArrayList<>()), 2, true);
			Thread.sleep(1000l);
		}

	}
	
	public static void main(String[] args) throws Exception {
		String json = "{\"header\":{\"mid\":8985,\"uid\":\"BIERWXAABMADGAFHAA\",\"msn\":\"201906000021\",\"sig\":\"3065023100E63D9474849F426557A5367E208B093B3510003B395A0EEBD49FC44EA32F45C582E3B3D55B22FE001B45EFDB6FCFCA7802307D5709A727AB075667FBFAFB5EE8FEC1BADEF6872FE0D811A1900AB86F71DACCBC64CF1ECAFA0F21ABEA197F5BC124B1\"},\"payload\":{\"id\":\"BIERWXAABMADGAFHAA\",\"type\":\"MDT\",\"data\":[{\"uid\":\"BIERWXAABMADGAFHAA\",\"msn\":\"201906000021\",\"kwh\":\"0.0\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"244.6\",\"pf\":\"10.0\",\"dt\":\"2021-05-31T13:26:51\"}]}}";
		Mqtt.publish("evs/pa/data",  new ObjectMapper().readValue(json, Map.class), 2, true);
	}
}