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

package eu.amidst.core.inference.messagepassing;

import eu.amidst.core.exponentialfamily.NaturalParameters;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.Sampler;
import eu.amidst.core.learning.parametric.bayesian.PlateuStructure;
import eu.amidst.core.utils.CompoundVector;

import java.util.Optional;

/**
 *
 * This class extends the class {@link MessagePassingAlgorithm} and implements the interfaces {@link InferenceAlgorithm} and {@link Sampler}.
 * It handles and implements the Variational message passing (VMP) algorithm.
 * Winn, J.M., Bishop, C.M.: Variational message passing. Journal of Machine Learning Research 6 (2005) 661–694.
 *
 * <p> For an example of use follow this link
 * <a href="http://amidst.github.io/toolbox/CodeExamples.html#vmpexample"> http://amidst.github.io/toolbox/CodeExamples.html#vmpexample </a>  </p>
 */
public class VMPParameter extends VMP {

    int maxGlobaIter = 1;
    PlateuStructure plateuStructure;

    public VMPParameter(PlateuStructure plateuStructure) {
        this.plateuStructure = plateuStructure;
    }

    public int getMaxGlobaIter() {
        return maxGlobaIter;
    }

    public void setMaxGlobaIter(int maxGlobaIter) {
        this.maxGlobaIter = maxGlobaIter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runInference() {

        nIter = 0;

        boolean globalconvergence = false;

        boolean convergence = false;
        probOfEvidence = Double.NEGATIVE_INFINITY;
        local_elbo = Double.NEGATIVE_INFINITY;
        local_iter = 0;
        int global_iter = 0;

//        while (!globalconvergence && (global_iter++)<maxGlobaIter) {

        this.testConvergence();

        while (!convergence && (local_iter++) < maxIter) {

                boolean done = true;
                for (Node node : nodes) {
                    if (!node.isActive() || node.isObserved() || plateuStructure.isNonReplicatedVar(node.getMainVariable()))
                        continue;

                    Message<NaturalParameters> selfMessage = newSelfMessage(node);

                    Optional<Message<NaturalParameters>> message = node.getChildren()
                            .stream()
                            .filter(children -> children.isActive())
                            .map(children -> newMessageToParent(children, node))
                            .reduce(Message::combineNonStateless);

                    if (message.isPresent())
                        selfMessage.combine(message.get());

                    //for (Node child: node.getChildren()){
                    //    selfMessage = Message.combine(newMessageToParent(child, node), selfMessage);
                    //}

                    updateCombinedMessage(node, selfMessage);
                    done &= node.isDone();
                }

                convergence = this.testConvergence();

                if (done) {
                    convergence = true;
                }

            }

            CompoundVector posteriorOLD = this.plateuStructure.getPlateauNaturalParameterPosterior();
            CompoundVector posteriorNew = this.plateuStructure.getPlateauNaturalParameterPosterior();

            //Collect messages from active nodes to non-active nodes.
            int count = 0;
            for (Node node : nodes) {
                if (!node.isActive() || node.isObserved() || plateuStructure.isReplicatedVar(node.getMainVariable()))
                    continue;


                Message<NaturalParameters> selfMessage = newSelfMessage(node);

                Optional<Message<NaturalParameters>> message = node.getChildren()
                        .stream()
                        .filter(children -> children.isActive())
                        .map(children -> newMessageToParent(children, node))
                        .reduce(Message::combineNonStateless);

                if (message.isPresent())
                    selfMessage.combine(message.get());

                //for (Node child: node.getChildren()){
                //    selfMessage = Message.combine(newMessageToParent(child, node), selfMessage);
                //}

                updateCombinedMessage(node, selfMessage);


                posteriorNew.setVectorByPosition(count,node.getQDist().getNaturalParameters());
                node.getQDist().setNaturalParameters((NaturalParameters)posteriorOLD.getVectorByPosition(count));
                node.getQDist().fixNumericalInstability();
                node.getQDist().updateMomentFromNaturalParameters();

                count++;
            }

            this.plateuStructure.updateNaturalParameterPosteriors(posteriorNew);

            //probOfEvidence = local_elbo;

            //this.testConvergence();

            /*double percentage = 100 * Math.abs(local_elbo - probOfEvidence) / Math.abs(probOfEvidence);
            if (percentage < threshold) {
                globalconvergence = true;
            }else{
                System.out.println();
            }*/


            probOfEvidence = local_elbo;
 //       }

        probOfEvidence = local_elbo;
        if (output){
            System.out.println("N Iter: "+global_iter +" " +local_iter +", elbo:"+local_elbo);
            logger.info("N Iter: {}, {}, elbo: {}",global_iter, local_iter, local_elbo);
        }
        nIter=local_iter;


    }


}