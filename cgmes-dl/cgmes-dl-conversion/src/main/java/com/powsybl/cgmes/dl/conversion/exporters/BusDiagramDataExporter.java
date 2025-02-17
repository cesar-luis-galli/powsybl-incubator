/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.dl.conversion.exporters;

import java.util.Objects;

import com.powsybl.cgmes.dl.conversion.ExportContext;
import com.powsybl.cgmes.iidm.extensions.dl.NodeDiagramData;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.triplestore.api.TripleStore;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class BusDiagramDataExporter extends AbstractNodeDiagramDataExporter {

    public BusDiagramDataExporter(TripleStore tripleStore, ExportContext context) {
        super(tripleStore, context);
    }

    public void exportDiagramData(Bus bus) {
        Objects.requireNonNull(bus);
        NodeDiagramData<Bus> busDiagramData = bus.getExtension(NodeDiagramData.class);
        String diagramObjectStyleId = addDiagramObjectStyle(TopologyKind.BUS_BREAKER);
        addDiagramData(bus.getId(), bus.getName(), busDiagramData, diagramObjectStyleId);
    }

}
