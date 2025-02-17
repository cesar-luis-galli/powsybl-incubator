/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.substationdiagram.layout.LayoutParameters;

import java.util.Objects;

import static com.powsybl.substationdiagram.library.ComponentTypeName.BUSBAR_SECTION;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class BusNode extends Node {

    private double pxWidth = 1;

    private Position structuralPosition;

    private Position position = new Position(-1, -1);

    protected BusNode(String id, String name, boolean fictitious, Graph graph) {
        super(NodeType.BUS, id, name, BUSBAR_SECTION, fictitious, graph);
    }

    public static BusNode create(Graph graph, BusbarSection busbarSection) {
        Objects.requireNonNull(graph);
        Objects.requireNonNull(busbarSection);
        return new BusNode(busbarSection.getId(), busbarSection.getName(), false, graph);
    }

    public static BusNode create(Graph graph, Bus bus) {
        Objects.requireNonNull(graph);
        Objects.requireNonNull(bus);
        return new BusNode(bus.getId(), bus.getName(), false, graph);
    }

    public static BusNode createFictitious(Graph graph, String id) {
        return new BusNode(id, id, true, graph);
    }

    public void calculateCoord(LayoutParameters layoutParameters) {
        setY(layoutParameters.getInitialYBus() +
                (position.getV() - 1) * layoutParameters.getVerticalSpaceBus());
        setX(layoutParameters.getInitialXBus()
                + position.getH() * layoutParameters.getCellWidth()
                + layoutParameters.getHorizontalBusPadding() / 2);
        setPxWidth(position.getHSpan() * layoutParameters.getCellWidth() - layoutParameters.getHorizontalBusPadding());
    }

    @Override
    public void setCell(Cell cell) {
        if (!(cell instanceof BusCell)) {
            throw new PowsyblException("The Cell of a BusNode shall be a BusCell");
        }
        super.setCell(cell);
    }

    public double getPxWidth() {
        return pxWidth;
    }

    public void setPxWidth(double widthBus) {
        this.pxWidth = widthBus;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Position getStructuralPosition() {
        return structuralPosition;
    }

    public void setStructuralPosition(Position structuralPosition) {
        this.structuralPosition = structuralPosition;
    }
}
