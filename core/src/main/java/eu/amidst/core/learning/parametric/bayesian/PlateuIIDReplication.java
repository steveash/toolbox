/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.core.learning.parametric.bayesian;

import eu.amidst.core.inference.messagepassing.Node;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This class extends the abstract class {@link PlateuStructure} and defines Plateu IID Replication.
 */
public class PlateuIIDReplication extends PlateuStructure{

    /**
     * Replicates this model.
     */
    public void replicateModel(){
        nonReplictedNodes = new ArrayList();
        replicatedNodes = new ArrayList<>(nReplications);

        replicatedVarsToNode = new ArrayList<>();
        nonReplicatedVarsToNode = new ConcurrentHashMap<>();
        nonReplictedNodes = ef_learningmodel.getDistributionList().stream()
                .filter(dist -> dist.getVariable().isParameterVariable())
                .map(dist -> {
                    Node node = new Node(dist);
                    nonReplicatedVarsToNode.put(dist.getVariable(), node);
                    return node;
                })
                .collect(Collectors.toList());

        for (int i = 0; i < nReplications; i++) {

            Map<Variable, Node> map = new ConcurrentHashMap<>();
            List<Node> tmpNodes = ef_learningmodel.getDistributionList().stream()
                    .filter(dist -> !dist.getVariable().isParameterVariable())
                    .map(dist -> {
                        Node node = new Node(dist);
                        map.put(dist.getVariable(), node);
                        return node;
                    })
                    .collect(Collectors.toList());
            this.replicatedVarsToNode.add(map);
            replicatedNodes.add(tmpNodes);
        }

        for (int i = 0; i < nReplications; i++) {
            for (Node node : replicatedNodes.get(i)) {
                final int slice = i;
                node.setParents(node.getPDist().getConditioningVariables().stream().map(var -> this.getNodeOfVar(var, slice)).collect(Collectors.toList()));
                node.getPDist().getConditioningVariables().stream().forEach(var -> this.getNodeOfVar(var, slice).getChildren().add(node));
            }
        }

        List<Node> allNodes = new ArrayList();

        allNodes.addAll(this.nonReplictedNodes);

        for (int i = 0; i < nReplications; i++) {
            allNodes.addAll(this.replicatedNodes.get(i));
        }

        this.vmp.setNodes(allNodes);
    }

}