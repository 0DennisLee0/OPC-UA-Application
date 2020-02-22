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

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.l;

public class HistoryReadExampleProsys implements ClientExample {

    public static void main(String[] args) throws Exception {
        HistoryReadExampleProsys example = new HistoryReadExampleProsys();

        new ClientExampleRunner(example, false).run();
    }

    @Override
    public void run(OpcUaClient client, CompletableFuture<OpcUaClient> future) throws Exception {
        client.connect().get();

        HistoryReadDetails historyReadDetails = new ReadRawModifiedDetails(
            false,
            DateTime.MIN_VALUE,
            DateTime.now(),
            uint(0),
            true
        );

        HistoryReadValueId historyReadValueId = new HistoryReadValueId(
            new NodeId(5, "Counter1"),
            null,
            QualifiedName.NULL_VALUE,
            ByteString.NULL_VALUE
        );

        List<HistoryReadValueId> nodesToRead = new ArrayList<>();
        nodesToRead.add(historyReadValueId);

        HistoryReadResponse historyReadResponse = client.historyRead(
            historyReadDetails,
            TimestampsToReturn.Both,
            false,
            nodesToRead
        ).get();


        HistoryReadResult[] historyReadResults = historyReadResponse.getResults();

        if (historyReadResults != null) {
            HistoryReadResult historyReadResult = historyReadResults[0];
            HistoryData historyData = (HistoryData) historyReadResult.getHistoryData().decode(
                client.getSerializationContext()
            );

            List<DataValue> dataValues = l(historyData.getDataValues());

            dataValues.forEach(v -> System.out.println("value=" + v));
        }

        future.complete(client);
    }

    @Override
    public String getEndpointUrl() {
        return "opc.tcp://localhost:53530/OPCUA/SimulationServer";
    }

}
