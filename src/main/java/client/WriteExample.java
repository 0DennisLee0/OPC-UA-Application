/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package client;

import com.google.common.collect.ImmutableList;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.nodes.VariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class WriteExample implements ClientExample {

    public static void main(String[] args) throws Exception {
        WriteExample example = new WriteExample();

        new ClientExampleRunner(example).run();
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void run(OpcUaClient client, CompletableFuture<OpcUaClient> future) throws Exception {
        // synchronous connect
        client.connect().get();

        //修改轴数据
        List<NodeId> nodeIds = ImmutableList.of(new NodeId(2, "CNC/Axis"));

        // synchronous read request via VariableNode
        VariableNode node = client.getAddressSpace().createVariableNode(
                new NodeId(2, "CNC/Axis"));

        NodeId nodeId = new NodeId(2, "CNC/Axis");

        for (int i = 0; i < 10; i++) {
            Variant v = new Variant(i);

            // don't write status or timestamps
            DataValue dv = new DataValue(v, null, null);

            // write asynchronously....
            CompletableFuture<List<StatusCode>> f =
//                    client.writeValue(nodeId, dv);
                client.writeValues(nodeIds, ImmutableList.of(dv));


            // ...but block for the results so we write in order
            List<StatusCode> statusCodes = f.get();
            StatusCode status = statusCodes.get(0);

            //记录写这个动作的记录
            if (status.isGood()) {
                logger.info("Wrote '{}' to nodeId={}", v, nodeIds.get(0));
                logger.info("Read nodeId={}, value = {}", node.getNodeId(), node.getValue());
            }
        }

        future.complete(client);
    }

}
