/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.util;

import com.powsybl.basevoltage.BaseVoltageColor;
import com.powsybl.iidm.network.Branch.Side;
import com.powsybl.iidm.network.*;
import com.powsybl.substationdiagram.model.Edge;
import com.powsybl.substationdiagram.model.Node;
import com.powsybl.substationdiagram.model.Node.NodeType;
import com.powsybl.substationdiagram.svg.DefaultSubstationDiagramStyleProvider;
import com.powsybl.substationdiagram.svg.SubstationDiagramStyles;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.powsybl.substationdiagram.library.ComponentTypeName.*;
import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.escapeId;


/**
 * @author Giovanni Ferrari <giovanni.ferrari at techrain.eu>
 */
public class TopologicalStyleProvider extends DefaultSubstationDiagramStyleProvider {

    private BaseVoltageColor baseVoltageColor;
    private HashMap<String, HashMap<String, RGBColor>> voltageLevelColorMap = new HashMap();
    private static final String DEFAULT_COLOR = "#FF0000";
    private static final String DISCONNECTED_COLOR = "#808080";
    private static final double FACTOR = 0.7;
    private String disconnectedColor;

    public TopologicalStyleProvider(Path config) {
        try {
            baseVoltageColor = config != null ? new BaseVoltageColor(config) : new BaseVoltageColor();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        disconnectedColor = getBaseColor(0, "RTE", DISCONNECTED_COLOR);
    }

    private RGBColor getBusColor(Node node) {
        String id = node.getId();
        VoltageLevel vl = node.getGraph().getVoltageLevel();
        return voltageLevelColorMap.computeIfAbsent(vl.getId(), k -> getColorMap(vl)).get(id);
    }

    private HashMap<String, RGBColor> getColorMap(VoltageLevel vl) {
        String basecolor = getBaseColor(vl.getNominalV(), "RTE", DEFAULT_COLOR);

        AtomicInteger idxColor = new AtomicInteger(0);
        long buses = vl.getBusView().getBusStream().count();

        HashMap<String, RGBColor> colorMap = new HashMap();
        HashMap<String, RGBColor> busColorMap = new HashMap();

        RGBColor color = RGBColor.parse(basecolor);

        List<RGBColor> colors = color.getColorGradient((int) buses, FACTOR);

        vl.getBusView().getBuses().forEach(b -> {
            RGBColor c = colors.get(idxColor.getAndIncrement());
            busColorMap.put(b.getId(), c);

            vl.getBusView().getBus(b.getId()).visitConnectedEquipments(new TopologyVisitor() {
                @Override
                public void visitBusbarSection(BusbarSection e) {
                    colorMap.put(e.getId(), c);
                }

                @Override
                public void visitDanglingLine(DanglingLine e) {
                    colorMap.put(e.getId(), c);
                }

                @Override
                public void visitGenerator(Generator e) {
                    colorMap.put(e.getId(), c);
                }

                @Override
                public void visitLine(Line e, Side s) {
                    colorMap.put(e.getId(), c);
                }

                @Override
                public void visitLoad(Load e) {
                    colorMap.put(e.getId(), c);
                }

                @Override
                public void visitShuntCompensator(ShuntCompensator e) {
                    colorMap.put(e.getId(), c);
                }

                @Override
                public void visitStaticVarCompensator(StaticVarCompensator e) {
                    colorMap.put(e.getId(), c);
                }

                @Override
                public void visitThreeWindingsTransformer(ThreeWindingsTransformer e,
                        com.powsybl.iidm.network.ThreeWindingsTransformer.Side s) {
                    colorMap.put(e.getId(), c);
                }

                @Override
                public void visitTwoWindingsTransformer(TwoWindingsTransformer e, Side s) {
                    colorMap.put(e.getId(), c);
                }
            });
        });
        return colorMap;
    }

    private String getBaseColor(double v, String profile, String defaultColor) {
        return baseVoltageColor.getColor(v, profile) != null ? baseVoltageColor.getColor(v, profile) : defaultColor;

    }

    @Override
    public Optional<String> getNodeStyle(Node node, boolean avoidSVGComponentsDuplication) {

        Optional<String> defaultStyle = super.getNodeStyle(node, avoidSVGComponentsDuplication);
        if (node.getType() == NodeType.SWITCH || node.getComponentType().equals(TWO_WINDINGS_TRANSFORMER) || node.getComponentType().equals(THREE_WINDINGS_TRANSFORMER) || node.getComponentType().equals(PHASE_SHIFT_TRANSFORMER)) {
            return defaultStyle;
        }

        try {
            RGBColor c = getBusColor(node);

            String color = c != null ? c.toString() : disconnectedColor;

            return Optional.of(defaultStyle.orElse("") + " #"
                    + escapeId(URLEncoder.encode(node.getId(), StandardCharsets.UTF_8.name())) + " {stroke:"
                    + color + ";}");

        } catch (UnsupportedEncodingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<String> getWireStyle(Edge edge) {

        try {
            String wireId = SubstationDiagramStyles
                    .escapeId(URLEncoder.encode(
                            edge.getNode1().getGraph().getVoltageLevel().getId() + "_Wire"
                                    + edge.getNode1().getGraph().getEdges().indexOf(edge),
                            StandardCharsets.UTF_8.name()));
            Node bus = findConnectedBus(edge);
            String color = disconnectedColor;
            if (bus != null) {
                RGBColor c = getBusColor(bus);
                if (c != null) {
                    color = c.toString();
                }
            }

            return Optional.of(" #" + wireId + " {stroke:" + color + ";}");
        } catch (UnsupportedEncodingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Node findConnectedBus(Edge edge) {
        Node n1 = edge.getNode1();
        if (n1.getType() == NodeType.BUS) {
            return n1;
        }
        Node n2 = edge.getNode2();
        if (n1.getType() == NodeType.BUS) {
            return n2;
        }
        Node n11 = findConnectedBus(n1, new ArrayList<Node>());
        if (n11 != null) {
            return n11;
        } else {
            return findConnectedBus(n2, new ArrayList<Node>());
        }
    }

    private Node findConnectedBus(Node node, List<Node> visitedNodes) {
        List<Node> nodesToVisit = new ArrayList<>(node.getAdjacentNodes());
        if (!visitedNodes.contains(node)) {
            visitedNodes.add(node);
            if (node.getType().equals(NodeType.SWITCH) && node.isOpen()) {
                return null;
            }
            for (Node n : nodesToVisit) {
                if (n.getType().equals(NodeType.BUS)) {
                    return n;
                } else {
                    Node n1 = findConnectedBus(n, visitedNodes);
                    if (n1 != null) {
                        return n1;
                    }
                }
            }
        }
        return null;
    }

}
