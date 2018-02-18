package com.testiot.agent;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Utils {
    public static final Properties props = new Properties();
    public static final ObjectMapper objectMapper = new ObjectMapper();

    static  {

        try {
            InputStream in = ClassLoader.getSystemResourceAsStream("agent.properties");
            if (in == null) throw new IllegalArgumentException();
            props.load(in);
            String agent_pros = props.getProperty("agent.props");
            if(agent_pros!=null) {
                for (String pf : agent_pros.split(",")) {
                    in = ClassLoader.getSystemResourceAsStream(pf);
                    if (in == null) throw new IllegalArgumentException("Error: missing resource - " + pf);
                    props.load(in);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
