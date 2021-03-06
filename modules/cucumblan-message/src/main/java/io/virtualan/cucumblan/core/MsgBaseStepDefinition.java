/*
 *
 *
 *    Copyright (c) 2021.  Virtualan Contributors (https://virtualan.io)
 *
 *     Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *     in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software distributed under the License
 *     is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *     or implied. See the License for the specific language governing permissions and limitations under
 *     the License.
 *
 *
 *
 */

package io.virtualan.cucumblan.core;


import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.virtualan.csvson.Csvson;
import io.virtualan.cucumblan.core.msg.MQClient;
import io.virtualan.cucumblan.message.exception.UnableToProcessException;
import io.virtualan.cucumblan.props.util.EventRequest;
import io.virtualan.cucumblan.core.msg.kafka.KafkaConsumerClient;
import io.virtualan.cucumblan.core.msg.kafka.KafkaProducerClient;
import io.virtualan.cucumblan.core.msg.kafka.MessageContext;
import io.virtualan.cucumblan.message.exception.MessageNotDefinedException;
import io.virtualan.cucumblan.message.type.MessageType;
import io.virtualan.cucumblan.props.TopicConfiguration;
import io.virtualan.cucumblan.props.util.MsgHelper;
import io.virtualan.cucumblan.props.util.ScenarioContext;
import io.virtualan.cucumblan.props.util.StepDefinitionHelper;
import io.virtualan.mapson.exception.BadInputDataException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.jms.JMSException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;


/**
 * The type Message base step definition.
 *
 * @author Elan Thangamani
 */
@Slf4j
public class MsgBaseStepDefinition {

  private Scenario scenario;

  /**
   * Before.
   *
   * @param scenario the scenario
   */
  @Before
  public void before(Scenario scenario) {
    this.scenario = scenario;
  }


  /**
   * given sql.
   *
   * @param dummy    the dummy
   * @throws Exception the exception
   */
  @Given("As a user perform message (.*) action$")
  public void dummyGiven(String dummy) throws Exception {
  }

  /**
   * Produce message with partition.
   *
   * @param eventName the event name
   * @param partition partition
   * @param resource  the resource
   * @param type      the type
   * @param messages  the messages
   * @throws MessageNotDefinedException the message not defined exception
   */
  @Given("Send message (.*) for event (.*) in partition (.*) on (.*) with type (.*)$")
  public void produceMessageWithPartition(String dummy, String eventName, Integer partition, String resource,
      String type, Object messages) throws MessageNotDefinedException {
    String eventNameInput = StepDefinitionHelper.getActualValue(eventName);
    String typeInput = StepDefinitionHelper.getActualValue(type);
    String topic = StepDefinitionHelper.getActualValue(TopicConfiguration.getProperty(eventNameInput));
    MessageType messageType = MessageContext.getMessageTypes().get(typeInput);
    if (topic != null && messageType != null) {
      MessageType builtMessage = messageType.buildProducerMessage(messages);
      scenario.log(builtMessage.toString());

      KafkaProducerClient
          .sendMessage(resource, topic, builtMessage.getKey(), builtMessage.getMessage(),
              partition, builtMessage.getHeaders());
    } else {
      Assertions.assertTrue(false, eventName + " is not configured for any topic. or " + type +" is not configured");
    }
  }

  /**
   * Produce message.
   *
   * @param sleep the sleep
   * @throws InterruptedException the interrupted exception
   */
  @Given("Pause message (.*) for process for (.*) milliseconds$")
  public void produceMessage(String dummy, long sleep) throws InterruptedException {
    Thread.sleep(sleep);
  }

  /**
   * eventName to Produce message.
   *
   * @param eventName the eventName
   * @throws InterruptedException the interrupted exception
   */
  @Given("Clear the consumed message (.*) for the event (.*)$")
  public void clearMessage(String dummy, String eventName) throws InterruptedException {
    String eventNameInput = StepDefinitionHelper.getActualValue(eventName);
    MessageContext.removeEventContextMap(eventNameInput);
  }

  /**
   * Produce message.
   *
   * @param eventName the event name
   * @param resource  the resource
   * @param type      the type
   * @param messages  the messages
   * @throws MessageNotDefinedException the message not defined exception
   */
  @Given("Send message (.*) for event (.*) on (.*) with type (.*)$")
  public void produceMessage(String dummy, String eventName, String resource, String type,
      DataTable messages) throws MessageNotDefinedException {
    String eventNameInput = StepDefinitionHelper.getActualValue(eventName);
    String typeInput = StepDefinitionHelper.getActualValue(type);
    String topic = StepDefinitionHelper.getActualValue(TopicConfiguration.getProperty(eventNameInput));
    MessageType messageType = MessageContext.getMessageTypes().get(typeInput);
    if (topic != null && messageType != null) {
      MessageType builtMessage = messageType.buildProducerMessage(messages);
      scenario.log(builtMessage.toString());
      if (builtMessage.getKey() != null) {
        KafkaProducerClient
            .sendMessage(resource, topic, builtMessage.getKey(), builtMessage.getMessage(),
                null, builtMessage.getHeaders());
      } else {
        KafkaProducerClient
            .sendMessage(resource, topic, null, builtMessage.getMessage(),
                null, builtMessage.getHeaders());
      }
    } else {
      Assertions.assertTrue(false, eventName + " is not configured for any topic. or " + type +" is not configured");
    }
  }

  /**
   * Produce message.
   *
   * @param eventName the event name
   * @param resource  the resource
   * @param type      the type
   * @param messages  the messages
   * @throws MessageNotDefinedException the message not defined exception
   */
  @Given("Send inline message (.*) for event (.*) on (.*) with type (.*)$")
  public void produceMessage(String dummy, String eventName, String resource, String type,
      List<String> messages) throws MessageNotDefinedException {
    String eventNameInput = StepDefinitionHelper.getActualValue(eventName);
    String typeInput = StepDefinitionHelper.getActualValue(type);
    String topic = StepDefinitionHelper.getActualValue(TopicConfiguration.getProperty(eventNameInput));
    MessageType messageType = MessageContext.getMessageTypes().get(typeInput);
    if (topic != null && messageType != null) {
      MessageType builtMessage = messageType.buildProducerMessage(messages);
      scenario.log(builtMessage.toString());
      if (builtMessage.getKey() != null) {
        KafkaProducerClient
            .sendMessage(resource, topic, builtMessage.getKey(), builtMessage.getMessage(),
                null, builtMessage.getHeaders());
      } else {
        KafkaProducerClient
            .sendMessage(resource, topic, null, builtMessage.getMessage(),
                null, builtMessage.getHeaders());
      }
    } else {
      Assertions.assertTrue(false, eventName + " is not configured for any topic. or " + type +" is not configured");
    }
  }


  /**
   * Produce message.
   *
   * @param dummy     the dummy
   * @param queueName the queue name
   * @param resource  the resource
   * @param queueType the queue type
   * @param messages  the messages
   * @throws UnableToProcessException the message not defined exception
   */
  @Given("Send inline message (.*) for messageQ (.*) on (.*) with type (.*)$")
  public void produceJMSMessage(String dummy, String queueName, String resource, String queueType, List<String> messages)
      throws UnableToProcessException {
    String eventNameInput = StepDefinitionHelper.getActualValue(queueName);
    if (eventNameInput != null) {
      boolean message = false;
        message = MQClient.postMessage(scenario,resource,eventNameInput,
            StepDefinitionHelper.getActualValue(messages.stream().map(x -> x).collect(Collectors.joining())));
      Assertions.assertTrue(message, "message posting status");
    } else {
      Assertions.assertTrue(false, queueName + " is not configured.");
    }
  }



  /**
   * Produce message.
   *
   * @param eventName the event name
   * @param resource  the resource
   * @param type      the type
   * @param messages  the messages
   * @throws MessageNotDefinedException the message not defined exception
   */
  @Given("Send mapson message (.*) for event (.*) on (.*) with type (.*)$")
  public void produceMessageMapson(String dummy, String eventName, String resource, String type,
      Map<String, String> messages) throws MessageNotDefinedException {
    String eventNameInput = StepDefinitionHelper.getActualValue(eventName);
    String typeInput = StepDefinitionHelper.getActualValue(type);
    String topic = StepDefinitionHelper.getActualValue(TopicConfiguration.getProperty(eventNameInput));
    MessageType messageType = MessageContext.getMessageTypes().get(typeInput);
    if (topic != null && messageType != null) {
      MessageType builtMessage = messageType.buildProducerMessage(messages);
      scenario.log(builtMessage.toString());
      if (builtMessage.getKey() != null) {
        KafkaProducerClient
            .sendMessage(resource, topic, builtMessage.getKey(), builtMessage.getMessage(),
                null, builtMessage.getHeaders());
      } else {
        KafkaProducerClient
            .sendMessage(resource, topic, null, builtMessage.getMessage(),
                null, builtMessage.getHeaders());
      }
    } else {
      Assertions.assertTrue(false, eventName + " is not configured for any topic. or " + type +" is not configured");
    }

  }


  /**
   * Verify consumed json object.
   *
   * @param receiveQ the event name
   * @param id        the id
   * @param resource  the resource
   * @param type      the type
   * @param csvson    the csvson
   * @throws InterruptedException       the interrupted exception
   * @throws BadInputDataException      bad input data exception
   * @throws MessageNotDefinedException the message not defined exception
   */
  @Given("Verify (.*) for receiveQ (.*) contains (.*) on (.*) with type (.*)$")
  public void verifyConsumedJMSJSONObject(String dummy, String receiveQ, String id, String resource, String type,
      List<String> csvson)
      throws InterruptedException, BadInputDataException, MessageNotDefinedException, IOException, JMSException {
    String eventNameInput = StepDefinitionHelper.getActualValue(receiveQ);
    String idInput = StepDefinitionHelper.getActualValue(id);

    String expectedJson = MQClient.readMessage(scenario,resource,eventNameInput, idInput);
    if (expectedJson != null) {
      JSONArray csvobject = Csvson.buildCSVson(csvson, ScenarioContext.getContext(String.valueOf(Thread.currentThread().getId())));
      scenario.attach(csvobject.toString(4), "application/json",
          "ExpectedResponse:");
      Object expectedJsonObj = StepDefinitionHelper.getJSON(expectedJson);
      if (expectedJsonObj instanceof JSONObject) {
        scenario.attach(((JSONObject)expectedJsonObj).toString(4), "application/json",
            "ActualResponse:");
        JSONAssert
            .assertEquals(csvobject.getJSONObject(0), (JSONObject) expectedJsonObj,
                JSONCompareMode.LENIENT);
      } else {
        scenario.attach(((JSONArray)expectedJsonObj).toString(4), "application/json",
            "ActualResponse:");
        JSONAssert.assertEquals(csvobject, (JSONArray) expectedJsonObj, JSONCompareMode.LENIENT);
      }
    } else {
      Assertions.assertTrue(false,
          " Unable to read message name (" + eventNameInput + ") with identifier : " + id);
    }
  }

  /**
   * Verify consumed json object.
   *
   * @param receiveQ the event name
   * @param resource  the resource
   * @param type      the type
   * @param csvson    the csvson
   * @throws InterruptedException       the interrupted exception
   * @throws BadInputDataException      bad input data exception
   * @throws MessageNotDefinedException the message not defined exception
   */
  @Given("Verify (.*) for receiveQ (.*) find (.*) message on (.*) with type (.*)$")
  public void verifyConsumedJMSJSONObjectWithOutId(String dummy, String receiveQ, String jsonpath, String resource, String type,
      List<String> csvson)
      throws InterruptedException, BadInputDataException, MessageNotDefinedException, IOException, JMSException {
    String eventNameInput = StepDefinitionHelper.getActualValue(receiveQ);

    String expectedJson = MQClient.findMessage(scenario,resource,eventNameInput, jsonpath, type);
    if (expectedJson != null) {
      JSONArray csvobject = Csvson.buildCSVson(csvson, ScenarioContext.getContext(String.valueOf(Thread.currentThread().getId())));
      scenario.attach(csvobject.toString(4), "application/json",
          "ExpectedResponse:");
      Object expectedJsonObj = StepDefinitionHelper.getJSON(expectedJson);
      if (expectedJsonObj instanceof JSONObject) {
        scenario.attach(((JSONObject)expectedJsonObj).toString(4), "application/json",
            "ActualResponse:");
        JSONAssert
            .assertEquals(csvobject.getJSONObject(0), (JSONObject) expectedJsonObj,
                JSONCompareMode.LENIENT);
      } else {
        scenario.attach(((JSONArray)expectedJsonObj).toString(4), "application/json",
            "ActualResponse:");
        JSONAssert.assertEquals(csvobject, (JSONArray) expectedJsonObj, JSONCompareMode.LENIENT);
      }
    } else {
      Assertions.assertTrue(false,
          " Unable to read message name (" + eventNameInput + ") with identifier : " + jsonpath);
    }
  }




  /**
   * Verify consumed json object.
   *
   * @param eventName the event name
   * @param id        the id
   * @param resource  the resource
   * @param type      the type
   * @param csvson    the csvson
   * @throws InterruptedException       the interrupted exception
   * @throws BadInputDataException      bad input data exception
   * @throws MessageNotDefinedException the message not defined exception
   */
  @Given("Verify (.*) for event (.*) contains (.*) on (.*) with type (.*)$")
  public void verifyConsumedJSONObject(String dummy, String eventName, String id, String resource, String type,
      List<String> csvson)
      throws InterruptedException, BadInputDataException, MessageNotDefinedException {
    int recheck = 0;
    String eventNameInput = StepDefinitionHelper.getActualValue(eventName);
    String typeInput = StepDefinitionHelper.getActualValue(type);
    String idInput = StepDefinitionHelper.getActualValue(id);
    EventRequest eventRequest = new EventRequest();
    eventRequest.setRecheck(recheck);
    eventRequest.setEventName(eventNameInput);
    eventRequest.setType(typeInput);
    eventRequest.setId(idInput);
    eventRequest.setResource(resource);

    MessageType expectedJson = KafkaConsumerClient.getEvent(eventRequest);
    if (expectedJson != null) {
      JSONArray csvobject = Csvson.buildCSVson(csvson, ScenarioContext.getContext(String.valueOf(Thread.currentThread().getId())));
      scenario.attach(csvobject.toString(4), "application/json",
          "ExpectedResponse:");
      if (expectedJson.getMessageAsJson() instanceof JSONObject) {
        scenario.attach(((JSONObject)expectedJson.getMessageAsJson()).toString(4), "application/json",
            "ActualResponse:");
        JSONAssert
            .assertEquals(csvobject.getJSONObject(0), (JSONObject) expectedJson.getMessageAsJson(),
                JSONCompareMode.LENIENT);
      } else {
        scenario.attach(((JSONArray)expectedJson.getMessageAsJson()).toString(4), "application/json",
            "ActualResponse:");
        JSONAssert.assertEquals(csvobject, (JSONArray) expectedJson, JSONCompareMode.LENIENT);
      }
    } else {
      Assertions.assertTrue(false,
          " Unable to read event name (" + eventName + ") with identifier : " + id);
    }
  }

  /**
   * Consume message.
   *
   * @param eventName the event name
   * @param id        the id
   * @param resource  the resource
   * @param type      the type
   * @param keyValue  the key value
   * @throws InterruptedException       interrupted exception
   * @throws MessageNotDefinedException the message not defined exception
   */
  @Given("Verify-by-elements (.*) for event (.*) contains (.*) on (.*) with type (.*)$")
  public void consumeMessage(String dummy, String eventName, String id, String resource, String type,
      Map<String, String> keyValue)
      throws InterruptedException, MessageNotDefinedException {
    int recheck = 0;
    String eventNameInput = StepDefinitionHelper.getActualValue(eventName);
    String typeInput = StepDefinitionHelper.getActualValue(type);
    String idInput = StepDefinitionHelper.getActualValue(id);
    EventRequest eventRequest = new EventRequest();
    eventRequest.setRecheck(recheck);
    eventRequest.setEventName(eventNameInput);
    eventRequest.setType(typeInput);
    eventRequest.setId(idInput);
    eventRequest.setResource(resource);

    MessageType expectedJson = KafkaConsumerClient.getEvent(eventRequest);
    if (expectedJson != null) {
      scenario.attach(expectedJson.getMessage().toString(), "application/json",
          "ActualResponse");
      MessageType finalExpectedJson = expectedJson;
      keyValue.forEach((k, v) -> {
        Object value = MsgHelper.getJSON(finalExpectedJson.getMessageAsJson().toString(), k);
        Assertions.assertEquals(
            StepDefinitionHelper.getObjectValue(StepDefinitionHelper.getActualValue((String) v)),
            value, k + " is not failed.");
      });
    } else {
      Assertions.assertTrue(false,
          " Unable to read event name (" + eventName + ") with identifier : " + id);
    }
  }


}