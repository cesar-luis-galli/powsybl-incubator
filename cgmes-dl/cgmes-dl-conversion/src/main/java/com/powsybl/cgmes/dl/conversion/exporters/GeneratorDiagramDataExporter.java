/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.dl.conversion.exporters;

import java.util.Map;
import java.util.Objects;

import com.powsybl.cgmes.dl.conversion.ExportContext;
import com.powsybl.cgmes.iidm.extensions.dl.InjectionDiagramData;
import com.powsybl.iidm.network.Generator;
import com.powsybl.triplestore.api.TripleStore;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class GeneratorDiagramDataExporter extends AbstractInjectionDiagramDataExporter {

    public GeneratorDiagramDataExporter(TripleStore tripleStore, ExportContext context, Map<String, String> terminals) {
        super(tripleStore, context, terminals);
    }

    public void exportDiagramData(Generator generator) {
        Objects.requireNonNull(generator);
        InjectionDiagramData<Generator> generatorDiagramData = generator.getExtension(InjectionDiagramData.class);
        String diagramObjectStyleId = addDiagramObjectStyle(generator.getTerminal().getVoltageLevel().getTopologyKind());
        addDiagramData(generator.getId(), generator.getName(), generatorDiagramData, diagramObjectStyleId);
    }

}
