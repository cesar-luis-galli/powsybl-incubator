/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.dl.conversion.importers;

import java.util.Objects;

import com.powsybl.cgmes.iidm.extensions.dl.NetworkDiagramData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.iidm.extensions.dl.DiagramPoint;
import com.powsybl.cgmes.iidm.extensions.dl.LineDiagramData;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class HvdcLineDiagramDataImporter {

    private static final Logger LOG = LoggerFactory.getLogger(HvdcLineDiagramDataImporter.class);

    private Network network;

    public HvdcLineDiagramDataImporter(Network network) {
        this.network = Objects.requireNonNull(network);
    }

    public void importDiagramData(PropertyBag hvdcLineDiagramData) {
        Objects.requireNonNull(hvdcLineDiagramData);
        String hvdcLineId = hvdcLineDiagramData.getId("identifiedObject");
        HvdcLine hvdcLine = network.getHvdcLine(hvdcLineId);
        if (hvdcLine != null) {
            LineDiagramData<HvdcLine> hvdcLineIidmDiagramData = hvdcLine.getExtension(LineDiagramData.class);
            if (hvdcLineIidmDiagramData == null) {
                hvdcLineIidmDiagramData = new LineDiagramData<>(hvdcLine);
            }
            String diagramName = hvdcLineDiagramData.get("diagramName");
            hvdcLineIidmDiagramData.addPoint(diagramName, new DiagramPoint(hvdcLineDiagramData.asDouble("x"), hvdcLineDiagramData.asDouble("y"), hvdcLineDiagramData.asInt("seq")));
            hvdcLine.addExtension(LineDiagramData.class, hvdcLineIidmDiagramData);
            NetworkDiagramData.addDiagramName(network, diagramName);
        } else {
            LOG.warn("Cannot find HVDC line {}, name {} in network {}: skipping HVDC line diagram data", hvdcLineId, hvdcLineDiagramData.get("name"), network.getId());
        }
    }

}
