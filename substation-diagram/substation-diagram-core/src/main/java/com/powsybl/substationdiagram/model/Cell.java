/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.substationdiagram.layout.LayoutParameters;

import java.io.IOException;
import java.util.*;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface Cell {
    enum CellType {
        INTERN, EXTERN, SHUNT
    }

    void addNodes(Collection<Node> nodesToAdd);

    List<Node> getNodes();

    void removeAllNodes(List<Node> nodeToRemove);

    void setNodes(List<Node> nodes);

    void setType(CellType type);

    CellType getType();

    Block getRootBlock();

    void setRootBlock(Block rootBlock);

    int getNumber();

    void calculateCoord(LayoutParameters layoutParam);

    void writeJson(JsonGenerator generator) throws IOException;

    String getFullId();

    Graph getGraph();
}
