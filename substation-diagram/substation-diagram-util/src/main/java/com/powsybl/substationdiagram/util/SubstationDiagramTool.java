/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.util;

import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.substationdiagram.VoltageLevelDiagram;
import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.layout.VoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.library.ComponentLibrary;
import com.powsybl.substationdiagram.library.ResourcesComponentLibrary;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolOptions;
import com.powsybl.tools.ToolRunningContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(Tool.class)
public class SubstationDiagramTool implements Tool {

    private static final String INPUT_FILE = "input-file";
    private static final String OUTPUT_DIR = "output-dir";
    private static final String IDS = "ids";

    @Override
    public Command getCommand() {
        return new Command() {

            @Override
            public String getName() {
                return "generate-substation-diagram";
            }

            @Override
            public String getTheme() {
                return "Substation diagram";
            }

            @Override
            public String getDescription() {
                return "generate substation diagram";
            }

            @Override
            public Options getOptions() {
                Options options = new Options();
                options.addOption(Option.builder().longOpt(INPUT_FILE)
                        .desc("the input file")
                        .hasArg()
                        .argName("INPUT_FILE")
                        .required()
                        .build());
                options.addOption(Option.builder().longOpt(OUTPUT_DIR)
                        .desc("the output directory")
                        .hasArg()
                        .argName("OUTPUT_DIR")
                        .required()
                        .build());
                options.addOption(Option.builder().longOpt(IDS)
                        .desc("voltage level id list")
                        .hasArg()
                        .argName("ID_LIST")
                        .build());
                return options;
            }

            @Override
            public String getUsageFooter() {
                return null;
            }
        };
    }

    private void generateSvg(ToolRunningContext context, Path outputDir, ComponentLibrary componentLibrary,
                             LayoutParameters parameters, VoltageLevelLayoutFactory voltageLevelLayoutFactory, VoltageLevel vl) throws UnsupportedEncodingException {
        Path svgFile = outputDir.resolve(URLEncoder.encode(vl.getId(), StandardCharsets.UTF_8.name()) + ".svg");
        context.getOutputStream().println("Generating '" + svgFile + "' (" + vl.getNominalV() + ")");
        VoltageLevelDiagram.build(vl, voltageLevelLayoutFactory, true)
                .writeSvg(componentLibrary, parameters, svgFile);
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) throws UnsupportedEncodingException {
        ToolOptions toolOptions = new ToolOptions(line, context);
        Path inputFile = toolOptions.getPath(INPUT_FILE).orElseThrow(() -> new PowsyblException(INPUT_FILE  + " option is missing"));
        Path outputDir = toolOptions.getPath(OUTPUT_DIR).orElseThrow(() -> new PowsyblException(OUTPUT_DIR  + " option is missing"));
        Optional<List<String>> ids = toolOptions.getValues(IDS);
        context.getOutputStream().println("Loading network '" + inputFile + "'...");
        Network network = Importers.loadNetwork(inputFile);
        if (network == null) {
            throw new PowsyblException("File '" + inputFile + "' is not importable");
        }
        ComponentLibrary componentLibrary = new ResourcesComponentLibrary("/ConvergenceLibrary");
        LayoutParameters parameters = new LayoutParameters();
        VoltageLevelLayoutFactory voltageLevelLayoutFactory = new SmartVoltageLevelLayoutFactory();
        if (ids.isPresent()) {
            for (String id : ids.get()) {
                VoltageLevel vl = network.getVoltageLevel(id);
                if (vl == null) {
                    throw new PowsyblException("Voltage level '" + id + "'");
                }
                generateSvg(context, outputDir, componentLibrary, parameters, voltageLevelLayoutFactory, vl);
            }
        } else {
            // export all voltage levels
            for (VoltageLevel vl : network.getVoltageLevels()) {
                generateSvg(context, outputDir, componentLibrary, parameters, voltageLevelLayoutFactory, vl);
            }
        }
    }
}
