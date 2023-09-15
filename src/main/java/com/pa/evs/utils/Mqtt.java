package com.pa.evs.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import javax.net.ssl.SSLContext;

import org.apache.http.ssl.SSLContexts;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
<pre>
	<dependency>
	    <groupId>org.eclipse.paho</groupId>
	    <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
	    <version>1.2.0</version>
	</dependency>
</pre>
 *
 */
public class Mqtt {
	
	static {
		try {
			Files.deleteIfExists(new File(System.getProperty("user.dir") + "/mqtt_tmp_dir").toPath());
			Files.deleteIfExists(new File(System.getProperty("user.dir") + "/out_mid").toPath());
			Files.createDirectory(new File(System.getProperty("user.dir") + "/mqtt_tmp_dir").toPath());
		} catch (Exception e) {
			//
		}
	}

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Mqtt.class);
	private static final String MQTT_SERVER_ADDRESS = "ssl://3.1.87.138:8883";//ssl: "ssl://localhost:8883", none-ssl: "tcp://3.1.87.138:8883"
	
	private static final Map<String, Lock> LOCKS = new ConcurrentHashMap<>();
	private static final Map<String, IMqttAsyncClient> INSTANCES = new ConcurrentHashMap<>();
	
	private static final ObjectMapper MAPPER = new ObjectMapper();

	public static IMqttAsyncClient getInstance() {
		return getInstance(null, null);
	}
	
	public static IMqttAsyncClient getInstance(String serverAddress, String clientId) {
		
		if (serverAddress == null) {
			serverAddress = MQTT_SERVER_ADDRESS;
		}
		if (clientId == null) {
			clientId = "dev-be-server";
		}
		
		Lock lock = LOCKS.computeIfAbsent(serverAddress + "." + clientId, k -> new ReentrantLock());
		lock.lock();
		IMqttAsyncClient instance = INSTANCES.get(serverAddress + "." + clientId);
		try {
			if (instance == null) {
				instance = new MqttAsyncClient(serverAddress, clientId + "." + System.currentTimeMillis(), new MqttDefaultFilePersistence(System.getProperty("user.dir") + "/mqtt_tmp_dir"));
				INSTANCES.put(serverAddress + "." + clientId, instance);
			}
			
			if (!instance.isConnected()) {
				instance.connect(getOptions(serverAddress)).waitForCompletion();;
				for (int i = 1; i <= 10; i++) {
					if (instance.isConnected()) {
						break;
					}
					LOG.info(clientId + " waiting for connect... " + i);
					Thread.sleep(2000l);
				}
			}
			if (!instance.isConnected()) {
				instance.connect(getOptions(serverAddress)).waitForCompletion();;
				for (int i = 1; i <= 10; i++) {
					if (instance.isConnected()) {
						break;
					}
					LOG.info(clientId + " waiting for connect... " + i);
					Thread.sleep(2000l);
				}
			}
			if (!instance.isConnected()) {
				instance.connect(getOptions(serverAddress)).waitForCompletion();;
				for (int i = 1; i <= 10; i++) {
					if (instance.isConnected()) {
						break;
					}
					LOG.info(clientId + " waiting for connect... " + i);
					Thread.sleep(2000l);
				}
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		} finally {
			lock.unlock();
		}

		return instance;
	}
	
	private static MqttConnectOptions getOptions(String serverAddress) throws Exception {
		
		MqttConnectOptions options = new MqttConnectOptions();
		options.setAutomaticReconnect(true);
		options.setCleanSession(true);
		options.setConnectionTimeout(50);
		options.setMaxInflight(500000);
		if (serverAddress.startsWith("ssl://")) {
			SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, (a, b) -> true).build();
			options.setSocketFactory(sslContext.getSocketFactory());
		}
		options.setUserName("admin");
		options.setPassword("public".toCharArray());
		
		return options;
	}

	private Mqtt() {

	}

	public static void publish(String topic, Payload<?> messages)
			throws Exception {
		publish(null, topic, messages, 1, true);
	}
	
	public static void publish(IMqttAsyncClient instance, String topic, Payload<?> messages)
			throws Exception {
		publish(instance, topic, messages, 1, true);
	}
	
	public static void publish(String topic, Object messages)
			throws Exception {
		publish(null, topic, messages, 1, true);
	}
	
	public static void publish(IMqttAsyncClient instance, String topic, Object messages)
			throws Exception {
		publish(instance, topic, messages, 1, true);
	}
	
	public static void publish(String topic, Object messages, int qos, boolean retained)
			throws Exception {
		publish(null, topic, MAPPER.writeValueAsBytes(messages), qos, retained);
	}
	
	public static void publish(IMqttAsyncClient instance, String topic, Object messages, int qos, boolean retained)
			throws Exception {
		publish(instance, topic, MAPPER.writeValueAsBytes(messages), qos, retained);
	}
	
	private static void publish(IMqttAsyncClient instance, String topic, byte[] messages, int qos, boolean retained)
			throws InterruptedException, org.eclipse.paho.client.mqttv3.MqttException {
		try {
			MqttMessage mqttMessage = new MqttMessage(messages);
	        mqttMessage.setQos(qos);
	        mqttMessage.setRetained(retained);

	        if (instance == null) {
	        	instance = Mqtt.getInstance();
	        }
	        if (!instance.isConnected()) {
	        	instance.connect(getOptions(instance.getServerURI()));
	        }
	        instance.publish(topic, mqttMessage);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	/**
	 * 
	 * @param instance
	 * @param topic
	 * @param fns
	 * @throws Exception
	 *
	@SafeVarargs
	public static void subscribe(IMqttAsyncClient instance, String topic, Function<Object, Object>... fns) throws Exception {
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
	*/
	
	@SafeVarargs
	public static void subscribe(IMqttAsyncClient instance, String topic, int qos, Function<Object, Object>... fns) throws Exception {
		try {
			if (instance == null) {
				instance = Mqtt.getInstance();
			}
			// AsyncClient
			instance.subscribe(topic, qos, (s, mqttMessage) -> {
				for (Function<Object, Object> fn : fns) {
					fn.apply(mqttMessage);
				}
			});	
			/**
			instance.subscribeWithResponse(topic, qos, (s, mqttMessage) -> {
				for (Function<Object, Object> fn : fns) {
					fn.apply(mqttMessage);
				}
			});
			*/	
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	/**
	 * 
	 * @param topic
	 * @param fns
	 * @throws Exception
	 *
	@SafeVarargs
	public static void subscribe(String topic, Function<Object, Object>... fns) throws Exception {
		subscribe(null, topic, fns);
	}
	*/

	public static void destroy() {
		INSTANCES.forEach((k,v) -> {
			try{
				v.close();
			} catch (Exception e) {
				LOG.error("Error disconnect " + v.getClientId(), e);
			}
		});
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

		/*Mqtt.subscribe("evs/pa/data", o -> {
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
		}*/
		System.out.println("start");
		Mqtt.publish("dev/evs/pa/data", "", 0, false);
		System.out.println("finish");

	}
	
	@SuppressWarnings({ "unchecked", "resource" })
	public static void main(String[] args) throws Exception {
		
		String evsPAMQTTAddress = "tcp://18.142.166.146:1883";
		String mqttClientId = System.currentTimeMillis() + "";
		
		String topic = "dev/evs/pa/data";
		int amount = 20000;
		int midStart = 1;

		int[] counts = new int[] {0};
		ExecutorService ex = Executors.newFixedThreadPool(1);
		
		// EVS -> MCU
		Set<String> midOOid = new HashSet<>();
		FileOutputStream fos = new FileOutputStream(System.getProperty("user.dir") + "/out_mid", true);
		Mqtt.subscribe(Mqtt.getInstance(evsPAMQTTAddress, mqttClientId), "dev/evs/pa/JMETER001", 2, o -> {
			final MqttMessage mqttMessage = (MqttMessage) o;
			LOG.info("dev/evs/pa/JMETER001 -> " + new String(mqttMessage.getPayload()));
			ex.execute(() -> {
				counts[0] = counts[0] + 1;
				try {
					Map<String, Object> rc = new ObjectMapper().readValue(mqttMessage.getPayload(), Map.class);
					Map<String, Object> header = (Map<String, Object>) rc.get("header");
					midOOid.add(header.get("oid") + "");
					fos.write((header.get("oid") + "\n").getBytes());
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			return null;
		});
		
		List<Map<String, Object>> pls = new ArrayList<>();
		String json = "{\"header\":{\"mid\":15246,\"uid\":\"JMETER001\",\"gid\":\"JMETER001\",\"msn\":\"20230906000001\",\"sig\":\"\"},\"payload\":{\"id\":\"JMETER001\",\"type\":\"MDT\",\"data\":[{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"235.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T16:40:13\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"235.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T16:55:14\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T17:10:15\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T17:25:16\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T17:40:17\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"237.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T17:55:18\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T18:10:18\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"237.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T18:25:19\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"237.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T18:40:20\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T18:55:21\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T19:10:21\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T19:25:23\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T19:40:24\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T19:55:24\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"237.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T20:10:26\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"238.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T20:25:27\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T20:40:27\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T20:55:28\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"237.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T21:10:29\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"236.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T21:25:30\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"237.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T21:40:31\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"238.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T21:55:32\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"237.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T22:10:33\"},{\"uid\":\"JMETER001\",\"msn\":\"20230906000001\",\"kwh\":\"8.584\",\"kw\":\"0.0\",\"i\":\"0.0\",\"v\":\"237.0\",\"pf\":\"1.0000\",\"dt\":\"2023-09-07T22:25:33\"}]}}";
		for (int i = 0; i < amount; i++) {
			Map<String, Object> payload = new ObjectMapper().readValue(json, Map.class);
			Map<String, Object> header = (Map<String, Object>) payload.get("header");
			header.put("mid", i + midStart);
			pls.add(payload);
		}
		
		// connect
		// MCU -> EVS
		ExecutorService exPub = Executors.newFixedThreadPool(amount);
		List<Callable<String>> tasks = new ArrayList<>();
		for (int i = 1; i <= amount; i++) {
			Map<String, Object> payload = pls.get(i - 1);
			int idx = i;
			tasks.add(() -> {
				String prefix = "java." + idx;
				IMqttAsyncClient client = Mqtt.getInstance(evsPAMQTTAddress, prefix);
				System.out.println(prefix + " -> " + client.getClientId() + " -> " + client.isConnected());
				Mqtt.publish(client, topic, payload, 2, false);
				client.disconnect();
				return null;
			});
		}
		exPub.invokeAll(tasks);
		
	}
}
