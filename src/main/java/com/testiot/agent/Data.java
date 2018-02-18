package com.testiot.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class Data{

    public State state = new State();

    public static class State {
        public Document reported = new Document();
        public Document desired = new Document();
    }

    public static class Document {
        public long counter = 1;
    }

    public String toJson() throws JsonProcessingException {
        return Utils.objectMapper.writeValueAsString(this);
    }

    public static Data fromJason(String json) throws IOException {
        return Utils.objectMapper.readValue(json, Data.class);
    }

}