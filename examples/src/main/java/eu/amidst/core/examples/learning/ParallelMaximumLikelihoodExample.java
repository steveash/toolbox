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
package eu.amidst.core.examples.learning;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.models.BayesianNetwork;

/**
 *
 * This example shows how to learn in parallel the parameters of a Bayesian network from a stream of data using maximum
 * likelihood.
 *
 * Created by andresmasegosa on 18/6/15.
 */
public class ParallelMaximumLikelihoodExample {


    public static void main(String[] args) throws Exception {

        //We can open the data stream using the static class DataStreamLoader
        DataStream<DataInstance> data = DataStreamLoader.openFromFile("datasets/simulated/WasteIncineratorSample.arff");

        //We create a ParallelMaximumLikelihood object with the MaximumLikehood builder
        ParallelMaximumLikelihood parameterLearningAlgorithm = new ParallelMaximumLikelihood();

        //We activate the parallel mode.
        parameterLearningAlgorithm.setParallelMode(true);

        //We fix the DAG structure
        parameterLearningAlgorithm.setDAG(MaximimumLikelihoodByBatchExample.getNaiveBayesStructure(data, 0));

        //We set the batch size which will be employed to learn the model in parallel
        parameterLearningAlgorithm.setBatchSize(100);

        //We set the data which is going to be used for leaning the parameters
        parameterLearningAlgorithm.setDataStream(data);

        //We perform the learning
        parameterLearningAlgorithm.runLearning();

        //And we get the model
        BayesianNetwork bnModel = parameterLearningAlgorithm.getLearntBayesianNetwork();

        //We print the model
        System.out.println(bnModel.toString());

    }

}
