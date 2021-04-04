package org.ros;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageFactory;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.parameter.ParameterTree;
import org.ros.node.topic.Publisher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Constructs a series of topics that match primitive standard Ros message data types, creates those datatypes,
 * as instance variables, attempts to retrieve values for those those datatypes from the parameter server,
 * and finally publishes those instances to their respective topics, or their default values if no parameter is availble.
 * 
 * @author Jonathan Groff (C) NeoCoreTechs 2021
 */
public class ParameterServerTestNode extends AbstractNodeMain {
  private static final Log log = LogFactory.getLog(ParameterServerTestNode.class);
  private CountDownLatch awaitStart = new CountDownLatch(1);
  NameResolver resolver = null;
  @Override
  public GraphName getDefaultNodeName() {
    return GraphName.of("rosjava/parameter_server_test_node");
  }

  @SuppressWarnings("rawtypes")
  @Override
  public void onStart(ConnectedNode connectedNode) {
    final Publisher<std_msgs.String> pub_tilde =
        connectedNode.newPublisher("tilde", std_msgs.String._TYPE);
    final Publisher<std_msgs.String> pub_string =
        connectedNode.newPublisher("string", std_msgs.String._TYPE);
    final Publisher<std_msgs.Int64> pub_int =
        connectedNode.newPublisher("int", std_msgs.Int64._TYPE);
    final Publisher<std_msgs.Bool> pub_bool =
        connectedNode.newPublisher("bool", std_msgs.Bool._TYPE);
    final Publisher<std_msgs.Float32> pub_float =
        connectedNode.newPublisher("float", std_msgs.Float32._TYPE);
 

    ParameterTree param = connectedNode.getParameterTree();
 
    //Log log = connectedNode.getLog();
    MessageFactory topicMessageFactory = connectedNode.getTopicMessageFactory();

    final std_msgs.String tilde_m = topicMessageFactory.newFromType(std_msgs.String._TYPE);
    
    try {
    tilde_m.setData(String.valueOf(param.get(connectedNode.resolveName("~tilde").toString(),"FAIL")));
    log.info("tilde: " + tilde_m.getData());
    } catch(Exception e){ 
    	e.printStackTrace();
    }
   	GraphName paramNamespace = null;
   
    try {
    	paramNamespace = GraphName.of(String.valueOf(param.get("parameter_namespace","FAIL")));
    } catch(Exception e){
    	e.printStackTrace();
    }
	param.set("parameter_namespace","/param");
   	paramNamespace = GraphName.of(String.valueOf(param.get("parameter_namespace","FAIL")));
    log.info("parameter_namespace: " + paramNamespace);
    resolver = connectedNode.getResolver().newChild(paramNamespace);
    
    NameResolver setResolver = null;
  	GraphName targetNamespace = null;
    try {
    	targetNamespace = GraphName.of((String) param.get("target_namespace","FAIL"));
    } catch(Exception e){ 
    	e.printStackTrace();
    }
	param.set("target_namespace", "/targ");
  	targetNamespace = GraphName.of(String.valueOf(param.get("target_namespace","FAIL")));
  	log.info("target_namespace: " + targetNamespace);
	setResolver = connectedNode.getResolver().newChild(targetNamespace);
	
    final std_msgs.String string_m = topicMessageFactory.newFromType(std_msgs.String._TYPE);
  
    final std_msgs.Int64 int_m = topicMessageFactory.newFromType(std_msgs.Int64._TYPE);

    final std_msgs.Bool bool_m = topicMessageFactory.newFromType(std_msgs.Bool._TYPE);
 
    final std_msgs.Float32 float_m = topicMessageFactory.newFromType(std_msgs.Float32._TYPE);
    
    if(resolver != null) {
    string_m.setData(String.valueOf(param.get(resolver.resolve("string"),"FAIL")));
    log.info("string: " + string_m.getData());
    Object o = param.get(resolver.resolve("int"),-1L);
    if( o instanceof Long)
    	int_m.setData((Long)o);
    else
    	log.error("Return type Long expected, got "+o.getClass().toGenericString());
    log.info("int: " + int_m.getData());
    bool_m.setData((boolean) param.get(resolver.resolve("bool"),false));
    log.info("bool: " + bool_m.getData());
    float_m.setData((float) param.get(resolver.resolve("float"),-1f));
    log.info("float: " + float_m.getData());
 
   /* final test_ros.Composite composite_m =
        topicMessageFactory.newFromType(test_ros.Composite._TYPE);
    Map composite_map = (Map) param.get(resolver.resolve("composite"), new HashMap());
    composite_m.getA().setW((Double) ((Map) composite_map.get("a")).get("w"));
    composite_m.getA().setX((Double) ((Map) composite_map.get("a")).get("x"));
    composite_m.getA().setY((Double) ((Map) composite_map.get("a")).get("y"));
    composite_m.getA().setZ((Double) ((Map) composite_map.get("a")).get("z"));
    composite_m.getB().setX((Double) ((Map) composite_map.get("b")).get("x"));
    composite_m.getB().setY((Double) ((Map) composite_map.get("b")).get("y"));
    composite_m.getB().setZ((Double) ((Map) composite_map.get("b")).get("z"));

    final test_ros.TestArrays list_m = topicMessageFactory.newFromType(test_ros.TestArrays._TYPE);*/
    // only using the integer part for easier (non-float) comparison
    @SuppressWarnings("unchecked")
    List<Integer> list = (List<Integer>) param.get(resolver.resolve("list"), new ArrayList());
    int[] data = new int[list.size()];
    for (int i = 0; i < list.size(); i++) {
      data[i] = list.get(i);
    }
    } else
    	log.error("resolver was null");

    //list_m.setInt32Array(data);

    // Set parameters
    if(setResolver != null) {
    param.set(setResolver.resolve("string"), string_m.getData());
    param.set(setResolver.resolve("int"), (int) int_m.getData());
    param.set(setResolver.resolve("float"), float_m.getData());
    param.set(setResolver.resolve("bool"), bool_m.getData());
    //param.set(setResolver.resolve("composite"), composite_map);
    //param.set(setResolver.resolve("list"), list);
    } else
    	log.error("setResolver was null");
 // tell the waiting constructors that we have registered publishers
 	awaitStart.countDown();
    connectedNode.executeCancellableLoop(new CancellableLoop() {
      @Override
      protected void loop() throws InterruptedException {
		try {
			awaitStart.await();
		} catch (InterruptedException e) {}
		if(resolver != null) {
			pub_tilde.publish(tilde_m);
			pub_string.publish(string_m);
			pub_int.publish(int_m);
			pub_bool.publish(bool_m);
			pub_float.publish(float_m);
		}
        Thread.sleep(100);
      }
    });
  }
}
