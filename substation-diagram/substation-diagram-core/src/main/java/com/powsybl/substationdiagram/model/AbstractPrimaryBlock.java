/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.commons.PowsyblException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractPrimaryBlock extends AbstractBlock implements PrimaryBlock {

    protected final List<Node> nodes;

    protected final List<PrimaryBlock> stackableBlocks;

    /**
     * Constructor.
     * A layout.block primary is oriented in order to have :
     * <ul>
     * <li>BUS - when in the layout.block - as starting node
     * <li>FEEDER - when in the layout.block - as ending node
     * </ul>
     *
     * @param nodes nodes
     */

    AbstractPrimaryBlock(List<Node> nodes, Cell cell) {
        super(Type.PRIMARY);
        if (nodes.isEmpty()) {
            throw new PowsyblException("Empty node list");
        }
        this.stackableBlocks = new ArrayList<>();
        this.nodes = new ArrayList<>(nodes);
        setCardinality(Extremity.START, 1);
        setCardinality(Extremity.END, 1);
        setCell(cell);
    }

    @Override
    public Graph getGraph() {
        return nodes.get(0).getGraph();
    }

    @Override
    public boolean isEmbedingNodeType(Node.NodeType type) {
        return nodes.stream().anyMatch(n -> n.getType() == type);
    }

    public List<Node> getNodes() {
        return new ArrayList<>(nodes);
    }

    @Override
    public void reverseBlock() {
        Collections.reverse(nodes);
    }

    @Override
    public Node getExtremityNode(Extremity extremity) {
        if (extremity == Extremity.START) {
            return nodes.get(0);
        }
        if (extremity == Extremity.END) {
            return nodes.get(nodes.size() - 1);
        }
        return null;
    }

    @Override
    public int getOrder() {
        return getExtremityNode(Extremity.START).getType() == Node.NodeType.FEEDER ?
                ((FeederNode) getExtremityNode(Extremity.START)).getOrder() : 0;
    }

    // TODO : this should be LegPrimaryBlock
    public void addStackableBlock(PrimaryBlock block) {
        stackableBlocks.add(block);
    }

    public List<PrimaryBlock> getStackableBlocks() {
        return new ArrayList<>(stackableBlocks);
    }

    @Override
    protected void writeJsonContent(JsonGenerator generator) throws IOException {
        generator.writeFieldName("nodes");
        generator.writeStartArray();
        for (Node node : nodes) {
            node.writeJson(generator);
        }
        generator.writeEndArray();
    }

    @Override
    public String toString() {
        return "PrimaryBlock(nodes=" + nodes + ")";
    }
}
