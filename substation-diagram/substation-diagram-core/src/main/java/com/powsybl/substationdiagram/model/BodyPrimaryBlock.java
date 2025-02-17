/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.powsybl.substationdiagram.layout.LayoutParameters;

import java.util.List;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class BodyPrimaryBlock extends AbstractPrimaryBlock {

    public BodyPrimaryBlock(List<Node> nodes, Cell cell) {
        super(nodes, cell);
        if (getExtremityNode(Extremity.START).getType() == Node.NodeType.FEEDER) {
            reverseBlock();
        }
    }

    public BodyPrimaryBlock(BodyPrimaryBlock bodyPrimaryBlock) {
        this(bodyPrimaryBlock.getNodes(), bodyPrimaryBlock.getCell());
    }

    // TODO : START or END ?
    @Override
    public int getOrder() {
        return getExtremityNode(Block.Extremity.START).getType() == Node.NodeType.FEEDER ?
                ((FeederNode) getExtremityNode(Block.Extremity.START)).getOrder() : 0;
    }

    @Override
    public void sizing() {
        if (getPosition().getOrientation() == Orientation.VERTICAL) {
            getPosition().setHSpan(1);
            // in the case of vertical Blocks the x Spanning is a ratio of the nb of edges of the blocks/overall edges
            getPosition().setVSpan(nodes.size() - 1);
        } else {
            // in the case of horizontal Blocks having 1 switch/1 position => 1 hPos / 2 edges rounded to the superior int
            getPosition().setHSpan(nodes.size() - 2);
            getPosition().setVSpan(1);
        }
    }

    @Override
    public void coordVerticalCase(LayoutParameters layoutParam) {
        int sign = ((BusCell) getCell()).getDirection() == BusCell.Direction.TOP ? 1 : -1;
        double y0 = getCoord().getY() + sign * getCoord().getYSpan() / 2;
        double yPxStep = calcYPxStep(sign);
        int v = 0;
        for (Node node : nodes) {
            node.setX(getCoord().getX());
            node.setY(y0 - yPxStep * v);
            node.setRotationAngle(null);
            v++;
        }
    }

    @Override
    public void coordHorizontalCase(LayoutParameters layoutParam) {
        double x0 = getCoord().getX() - getCoord().getXSpan() / 2;
        if (getCell().getType() == Cell.CellType.INTERN
                && ((BusCell) getCell()).getDirection() != BusCell.Direction.FLAT) {
            x0 += layoutParam.getCellWidth() / 2;
        }
        double xPxStep = getCoord().getXSpan() / (nodes.size() - 1);
        int h = 0;
        for (Node node : nodes) {
            node.setY(getCoord().getY());
            node.setX(x0 + xPxStep * h);
            node.setRotationAngle(90.);
            h++;
        }
    }

    void coordShuntCase() {
        double x0 = getExtremityNode(Block.Extremity.START).getX();
        double x1 = getExtremityNode(Block.Extremity.END).getX();
        double y0 = getExtremityNode(Block.Extremity.START).getY();
        double y1 = getExtremityNode(Block.Extremity.END).getY();
        double dx = (x1 - x0) / (nodes.size() - 1);
        double dy = (y1 - y0) / (nodes.size() - 1);
        for (int i = 1; i < nodes.size() - 1; i++) {
            Node node = nodes.get(i);
            node.setX(x0 + i * dx, false, false);
            node.setY(y0 + i * dy, false, false);
            if (dy == 0) {
                node.setRotationAngle(90.);
            }
        }
    }

    private double calcYPxStep(int sign) {
        if (getPosition().getVSpan() == 0) {
            return 0;
        }
        return sign * getCoord().getYSpan() / (nodes.size() - 1);
    }
}
