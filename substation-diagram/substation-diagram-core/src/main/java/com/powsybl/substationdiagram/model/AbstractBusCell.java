/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.powsybl.substationdiagram.layout.LayoutParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractBusCell extends AbstractCell implements BusCell {

    private List<LegPrimaryBlock> primaryLegBlocks = new ArrayList<>();
    private Direction direction = Direction.UNDEFINED;

    protected AbstractBusCell(Graph graph, CellType type) {
        super(graph, type);
    }

    @Override
    public void blocksSetting(Block rootBlock, List<LegPrimaryBlock> primaryBlocksConnectedToBus) {
        setRootBlock(rootBlock);
        this.primaryLegBlocks = new ArrayList<>(primaryBlocksConnectedToBus);
    }

    @Override
    public List<BusNode> getBusNodes() {
        return nodes.stream()
                .filter(n -> n.getType() == Node.NodeType.BUS)
                .map(BusNode.class::cast)
                .collect(Collectors.toList());
    }

    @Override
    public List<LegPrimaryBlock> getPrimaryLegBlocks() {
        return new ArrayList<>(primaryLegBlocks);
    }

    @Override
    public Direction getDirection() {
        return direction;
    }

    @Override
    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    @Override
    public Position getMaxBusPosition() {
        return graph.getMaxBusStructuralPosition();
    }

    @Override
    public void calculateCoord(LayoutParameters layoutParam) {
        getRootBlock().calculateRootCoord(layoutParam);
    }

    @Override
    public String toString() {
        return "Cell(type=" + getType() + ", direction=" + direction + ", nodes=" + nodes + ")";
    }
}
