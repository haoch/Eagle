/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.eagle.app;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.*;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.utils.NimbusClient;
import org.apache.eagle.metadata.model.ApplicationEntity;
import org.apache.thrift7.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Int;
import storm.trident.spout.RichSpoutBatchExecutor;

import java.io.Serializable;

public abstract class AbstractApplication implements Application,Serializable {
    private final static Logger LOG = LoggerFactory.getLogger(AbstractApplication.class);
    private static LocalCluster _localCluster;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                if(_localCluster != null) {
                    LOG.info("Shutting down local storm cluster instance");
                    _localCluster.shutdown();
                }
            }
        });
    }

    private static LocalCluster getLocalCluster(){
        if(_localCluster == null){
            _localCluster = new LocalCluster();
        }

        return _localCluster;
    }

    @Override
    public void start(ApplicationContext context){
        ApplicationEntity appEntity = context.getAppEntity();
        String topologyName = context.getAppEntity().getAppId();

        TopologyBuilder builder = new TopologyBuilder();
        buildApp(builder,context);
        StormTopology topology = builder.createTopology();
        Config conf = getClusterStormConfig(context);
        if(appEntity.getMode() == ApplicationEntity.Mode.CLUSTER){
            String jarFile = context.getAppEntity().getDescriptor().getJarPath();
            synchronized (AbstractApplication.class) {
                System.setProperty("storm.jar", jarFile);
                LOG.info("Submitting as cluster mode");
                try {
                    StormSubmitter.submitTopologyWithProgressBar(topologyName, conf, topology);
                } catch (AlreadyAliveException | InvalidTopologyException e) {
                    LOG.error(e.getMessage(), e);
                    throw new RuntimeException(e.getMessage(),e);
                } finally {
                    System.clearProperty("storm.jar");
                }
            }
        }else{
            LOG.info("Submitting as local mode");
            getLocalCluster().submitTopology(topologyName, conf, topology);
        }
    }

    private final static String STORM_NIMBUS_HOST_CONF_PATH = "application.storm.nimbusHost";
    private final static String STORM_NIMBUS_HOST_DEFAULT = "localhost";
    private final static Integer STORM_NIMBUS_THRIFT_DEFAULT = 6627;
    private final static String STORM_NIMBUS_THRIFT_CONF_PATH = "application.storm.nimbusThriftPort";

    private static Config getClusterStormConfig(ApplicationContext context){
        Config conf = new Config();
        conf.put(RichSpoutBatchExecutor.MAX_BATCH_SIZE_CONF, Int.box(64 * 1024));
        conf.put(Config.TOPOLOGY_RECEIVER_BUFFER_SIZE, Int.box(8));
        conf.put(Config.TOPOLOGY_TRANSFER_BUFFER_SIZE, Int.box(32));
        conf.put(Config.TOPOLOGY_EXECUTOR_RECEIVE_BUFFER_SIZE, Int.box(16384));
        conf.put(Config.TOPOLOGY_EXECUTOR_SEND_BUFFER_SIZE, Int.box(16384));
        conf.put(Config.NIMBUS_THRIFT_MAX_BUFFER_SIZE, Int.box(20480000));
        String nimbusHost = STORM_NIMBUS_HOST_DEFAULT;

        if(context.getEnvConfig().hasPath(STORM_NIMBUS_HOST_CONF_PATH)) {
            nimbusHost = context.getEnvConfig().getString(STORM_NIMBUS_HOST_CONF_PATH);
            LOG.info("Overriding {} = {}",STORM_NIMBUS_HOST_CONF_PATH,nimbusHost);
        } else {
            LOG.info("Using default {} = {}",STORM_NIMBUS_HOST_CONF_PATH,STORM_NIMBUS_HOST_DEFAULT);
        }
        Integer nimbusThriftPort =  STORM_NIMBUS_THRIFT_DEFAULT;
        if(context.getEnvConfig().hasPath(STORM_NIMBUS_THRIFT_CONF_PATH)) {
            nimbusThriftPort = context.getEnvConfig().getInt(STORM_NIMBUS_THRIFT_CONF_PATH);
            LOG.info("Overriding {} = {}",STORM_NIMBUS_THRIFT_CONF_PATH,nimbusThriftPort);
        } else {
            LOG.info("Using default {} = {}",STORM_NIMBUS_THRIFT_CONF_PATH,STORM_NIMBUS_THRIFT_DEFAULT);
        }
        conf.put(backtype.storm.Config.NIMBUS_HOST, nimbusHost);
        conf.put(backtype.storm.Config.NIMBUS_THRIFT_PORT, nimbusThriftPort);
        return conf;
    }

    protected abstract void buildApp(TopologyBuilder builder, ApplicationContext context);

    @Override
    public void stop(ApplicationContext context) {
        ApplicationEntity appEntity = context.getAppEntity();
        String appId = appEntity.getAppId();
        if(appEntity.getMode() == ApplicationEntity.Mode.CLUSTER){
            Nimbus.Client stormClient = NimbusClient.getConfiguredClient(getClusterStormConfig(context)).getClient();
            try {
                stormClient.killTopology(appId);
            } catch (NotAliveException | TException e) {
                LOG.error("Failed to kill topology named {}, due to: {}",appId,e.getMessage(),e.getCause());
            }
        } else {
            getLocalCluster().killTopology(appId);
        }
    }

    @Override
    public void status(ApplicationContext context) {
        // TODO: Not implemented yet!
        throw new RuntimeException("TODO: Not implemented yet!");
    }
}