package com.testiot.agent.aws;

import com.amazonaws.services.iot.client.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.testiot.agent.IoTAgent;
import com.testiot.agent.Data;
import com.testiot.agent.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Collection;

/*
    AWS IoT Agent based on the AWS IoT java sdk samples.
    http://www.apache.org/licenses/LICENSE-2.0
 */
public class AwsIoTAgent extends IoTAgent {

    static final String ENDPOINT=Utils.props.getProperty("agent.aws.endpointPrefix")+".iot."
            +Utils.props.getProperty("agent.aws.region")+".amazonaws.com";

    AWSIotMqttClient client;

    void createClient(){
        final String proto = Utils.props.getProperty("agent.aws.protocol");
        switch (proto){
            case "mqtt":
                PrivateKey privateKey=null;
                try (DataInputStream stream = new DataInputStream(ClassLoader.getSystemResourceAsStream(Utils.props.getProperty("agent.aws.privateKey")))) {
                    privateKey = PrivateKeyReader.getPrivateKey(stream, Utils.props.getProperty("agent.aws.algorithm"));
                    if(privateKey == null) throw new IllegalArgumentException("Invalid privateKey");
                } catch (IOException | GeneralSecurityException e) {
                    logger.error("Failed to load private key from file " + Utils.props.getProperty("agent.aws.privateKey"));
                }

                Collection<? extends Certificate> certChain = null;
                try (BufferedInputStream stream = new BufferedInputStream(ClassLoader.getSystemResourceAsStream(Utils.props.getProperty("agent.aws.certificate")))) {
                    final CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                    certChain = certFactory.generateCertificates(stream);
                } catch (IOException | CertificateException e) {
                    logger.error("Failed to load certificate file " + Utils.props.getProperty("agent.aws.certificate"));
                }

                KeyStore keyStore= null;
                String keyPassword = null;
                try {
                    keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    keyStore.load(null);

                    keyPassword = new BigInteger(128, new SecureRandom()).toString(32);

                    keyStore.setKeyEntry("alias", privateKey, keyPassword.toCharArray(), certChain.toArray(new Certificate[0]));
                } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
                    logger.error("Failed to aquire keyStore");
                }

                client = new AWSIotMqttClient(ENDPOINT, Utils.props.getProperty("agent.aws.id"), keyStore, keyPassword);
                break;
            case "wss":
                // set AWS IAM credentials & permission with AWSIoT policy before doing this
                client = new AWSIotMqttClient(ENDPOINT, Utils.props.getProperty("agent.aws.id"),
                        Utils.props.getProperty("agent.aws.accessKey"), Utils.props.getProperty("agent.aws.secret"));
                break;
            default:
                throw new IllegalArgumentException("Invalid protocol "+proto);
        }
    }

    AWSIotDevice device;

    @Override
    protected void once() {
        createClient();
        try {
            logger.info("connecting agent to AWS endpoint: "+ENDPOINT);
            client.setWillMessage(new AWSIotMessage("client/disconnect", AWSIotQos.QOS0, client.getClientId()));

//            device = new AWSIotDevice(Utils.props.getProperty("agent.aws.id"));
//            client.attach(device);
            client.connect();

            AWSIotTopic topic = new AwsAgentMqttListener ("$aws/things/agent/shadow/update", AWSIotQos.QOS0);
            client.subscribe(topic, true);

        } catch (AWSIotException e) {
            logger.info("connection failed", e);
        }
    }

    class  PublishListener extends AWSIotMessage {

        public PublishListener(String topic, AWSIotQos qos, String payload) {
            super(topic, qos, payload);
        }

        @Override
        public void onSuccess() {
            logger.info(System.currentTimeMillis() + ": >>> " + getStringPayload());
        }

        @Override
        public void onFailure() {
            logger.info(System.currentTimeMillis() + ": publish failed for " + getStringPayload());
        }

        @Override
        public void onTimeout() {
            logger.info(System.currentTimeMillis() + ": publish timeout for " + getStringPayload());
        }
    }
    public void exec(){

        try {
            Data thing = new Data();
            long desired = thing.state.desired.counter;
            thing.state.reported.counter = desired;
            thing.state.desired.counter = desired + 1;

            String jsonState = null;

            try {
                jsonState = thing.toJson();
                logger.info(System.currentTimeMillis() + ": >>> " + jsonState);

                AWSIotMessage message = new PublishListener("$aws/things/agent/shadow/update", AWSIotQos.QOS0, jsonState);
                client.publish(message);

                // Send updated document to the shadow
//                device.update(jsonState);
            } catch (AWSIotException e) {
                logger.info(System.currentTimeMillis() + ": update failed for " + jsonState);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            /*try {
                // Retrieve updated document from the shadow
                String shadowState = device.get();
                logger.info(System.currentTimeMillis() + ": <<< " + shadowState);

                thing = Data.fromJason(shadowState);
            } catch (AWSIotException e) {
                logger.info(System.currentTimeMillis() + ": get failed for " + jsonState);
            } catch (IOException e) {
                e.printStackTrace();
            }*/

            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.info("", e);
        }
    }

    class AwsAgentMqttListener extends AWSIotTopic {

        final Logger logger = LoggerFactory.getLogger(AwsAgentMqttListener.class);

        public AwsAgentMqttListener(String topic, AWSIotQos qos) {
            super(topic, qos);
        }

        @Override
        public void onMessage(AWSIotMessage message) {
            logger.info(System.currentTimeMillis() + ": <<< " + message.getStringPayload());
            try {
                Data thing = Data.fromJason(message.getStringPayload());

                logger.info(thing.toJson());
            } catch (IOException e) {
                logger.error("", e);
            }
        }
    }
}
