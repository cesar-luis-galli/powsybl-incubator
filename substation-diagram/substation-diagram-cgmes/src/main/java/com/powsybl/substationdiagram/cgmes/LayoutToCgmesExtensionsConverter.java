/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.cgmes;

import com.powsybl.cgmes.dl.conversion.CgmesDLUtils;
import com.powsybl.cgmes.iidm.extensions.dl.*;
import com.powsybl.iidm.network.*;
import com.powsybl.substationdiagram.layout.*;
import com.powsybl.substationdiagram.model.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.powsybl.substationdiagram.library.ComponentTypeName.DANGLING_LINE;
import static com.powsybl.substationdiagram.library.ComponentTypeName.LINE;
import static com.powsybl.substationdiagram.library.ComponentTypeName.THREE_WINDINGS_TRANSFORMER;
import static com.powsybl.substationdiagram.library.ComponentTypeName.TWO_WINDINGS_TRANSFORMER;
import static com.powsybl.substationdiagram.library.ComponentTypeName.VSC_CONVERTER_STATION;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
public class LayoutToCgmesExtensionsConverter {

    private static final Logger LOG = LoggerFactory.getLogger(LayoutToCgmesExtensionsConverter.class);

    private static final double OFFSET_MULTIPLIER_X = 2.0;

    private final LayoutParameters lparams;
    private final SubstationLayoutFactory sFactory;
    private final VoltageLevelLayoutFactory vFactory;
    private final boolean showNames;

    public LayoutToCgmesExtensionsConverter(SubstationLayoutFactory sFactory, VoltageLevelLayoutFactory vFactory, LayoutParameters lparams, boolean showNames) {
        this.sFactory = Objects.requireNonNull(sFactory);
        this.vFactory = Objects.requireNonNull(vFactory);
        this.lparams = Objects.requireNonNull(lparams);
        this.showNames = showNames;
    }

    public LayoutToCgmesExtensionsConverter() {
        this(new HorizontalSubstationLayoutFactory(), new PositionVoltageLevelLayoutFactory(new PositionFree()), new LayoutParameters(), true);
    }

    private boolean isLineNode(Node node) {
        return Arrays.asList(LINE, DANGLING_LINE, VSC_CONVERTER_STATION).contains(node.getComponentType());
    }

    private String getBranchId(String branchNodeId) {
        return branchNodeId.substring(0, branchNodeId.lastIndexOf('_'));
    }

    private int getMaxSeq(List<DiagramPoint> diagramPoints) {
        Objects.requireNonNull(diagramPoints);
        return diagramPoints.stream().sorted(Comparator.reverseOrder()).findFirst().orElse(new DiagramPoint(0, 0, 0)).getSeq();
    }

    private NodeDiagramData setNodeDiagramPoints(NodeDiagramData diagramData, BusNode busNode, OffsetPoint offsetPoint, String diagramName) {
        double x1 = busNode.getX();
        double y1 = busNode.getY();
        double x2 = x1;
        double y2 = y1;
        double pxWidth = busNode.getPxWidth();
        boolean rotatedBus = busNode.isRotated();
        if (!rotatedBus) {
            x2 = x1 + pxWidth;
        } else {
            y2 = y1 + pxWidth;
        }

        NodeDiagramData.NodeDiagramDataDetails diagramDetails = diagramData.new NodeDiagramDataDetails();
        DiagramPoint p1 = offsetPoint.newDiagramPoint(x1, y1, 1);
        DiagramPoint p2 = offsetPoint.newDiagramPoint(x2, y2, 2);
        diagramDetails.setPoint1(p1);
        diagramDetails.setPoint2(p2);
        diagramData.addData(diagramName, diagramDetails);
        return diagramData;
    }

    private LayoutInfo applyLayout(Substation substation, double xoffset, double yoffset, String diagramName) {
        OffsetPoint offsetPoint = new OffsetPoint(xoffset, yoffset);

        //apply the specified layout
        SubstationGraph sgraph = SubstationGraph.create(substation, showNames);
        SubstationLayout sLayout = sFactory.create(sgraph, vFactory);
        sLayout.run(lparams);

        LayoutInfo subsBoundary = new LayoutInfo(0.0, 0.0);
        substation.getVoltageLevelStream().forEach(voltageLevel -> {
            Graph vlGraph = sgraph.getNode(voltageLevel.getId());

            // remove fictitious nodes&switches (no CGMES DL data available for them)
            vlGraph.removeUnnecessaryFictitiousNodes();
            vlGraph.removeFictitiousSwitchNodes();

            double vlNodeMaxX = vlGraph.getNodes().stream().map(Node::getX).sorted(Collections.reverseOrder()).findFirst().orElse(0.0);
            double vlNodeMaxY = vlGraph.getNodes().stream().map(Node::getY).sorted(Collections.reverseOrder()).findFirst().orElse(0.0);
            subsBoundary.update(vlNodeMaxX, vlNodeMaxY);

            List<String> componentTypeList = vlGraph.getNodes().stream().map(Node::getComponentType).collect(Collectors.toList());
            LOG.debug("Voltage level id: {} ({}); {} ;component types: {}; max x,y: {}, {}", voltageLevel.getId(), voltageLevel.getName(), voltageLevel.getTopologyKind(), componentTypeList, vlNodeMaxX, vlNodeMaxY);

            //iterate over the voltage level's equipments, and fill the IIDM CGMES DL extensions with the computed layout info
            voltageLevel.getLoadStream().filter(load -> vlGraph.getNode(load.getId()) != null).forEach(load -> {
                Node node = vlGraph.getNode(load.getId());
                DiagramPoint lDiagramPoint = offsetPoint.newDiagramPoint(node.getX(), node.getY(), 0);
                InjectionDiagramData<Load> loadIidmDiagramData = new InjectionDiagramData<>(load);
                InjectionDiagramData.InjectionDiagramDetails diagramDetails = loadIidmDiagramData.new InjectionDiagramDetails(lDiagramPoint, 0);
                loadIidmDiagramData.addData(diagramName, diagramDetails);
                LOG.debug("setting CGMES DL IIDM extensions for Load: {}, {}", load.getId(), lDiagramPoint);
                load.addExtension(InjectionDiagramData.class, loadIidmDiagramData);
            });

            voltageLevel.getGeneratorStream().filter(generator -> vlGraph.getNode(generator.getId()) != null).forEach(generator -> {
                Node node = vlGraph.getNode(generator.getId());
                DiagramPoint gDiagramPoint = offsetPoint.newDiagramPoint(node.getX(), node.getY(), 0);
                InjectionDiagramData<Generator> gIidmDiagramData = new InjectionDiagramData<>(generator);
                InjectionDiagramData.InjectionDiagramDetails diagramDetails = gIidmDiagramData.new InjectionDiagramDetails(gDiagramPoint, 0);
                gIidmDiagramData.addData(diagramName, diagramDetails);
                LOG.debug("setting CGMES DL IIDM extensions for Generator: {}, {}", generator.getId(), gDiagramPoint);
                generator.addExtension(InjectionDiagramData.class, gIidmDiagramData);
            });

            voltageLevel.getShuntCompensatorStream().filter(shuntCompensator -> vlGraph.getNode(shuntCompensator.getId()) != null).forEach(shuntCompensator -> {
                Node node = vlGraph.getNode(shuntCompensator.getId());
                DiagramPoint scDiagramPoint = offsetPoint.newDiagramPoint(node.getX(), node.getY(), 0);
                InjectionDiagramData<ShuntCompensator> scDiagramData = new InjectionDiagramData<>(shuntCompensator);
                InjectionDiagramData.InjectionDiagramDetails diagramDetails = scDiagramData.new InjectionDiagramDetails(scDiagramPoint, 0);
                scDiagramData.addData(diagramName, diagramDetails);
                LOG.debug("setting CGMES DL IIDM extensions for ShuntCompensator: {}, {}", shuntCompensator.getId(), scDiagramPoint);
                shuntCompensator.addExtension(InjectionDiagramData.class, scDiagramData);
            });

            voltageLevel.getStaticVarCompensatorStream().filter(staticVarCompensator -> vlGraph.getNode(staticVarCompensator.getId()) != null).forEach(staticVarCompensator -> {
                Node node = vlGraph.getNode(staticVarCompensator.getId());
                DiagramPoint svcDiagramPoint = offsetPoint.newDiagramPoint(node.getX(), node.getY(), 0);
                InjectionDiagramData<StaticVarCompensator> svcDiagramData = new InjectionDiagramData<>(staticVarCompensator);
                InjectionDiagramData.InjectionDiagramDetails diagramDetails = svcDiagramData.new InjectionDiagramDetails(svcDiagramPoint, 0);
                svcDiagramData.addData(diagramName, diagramDetails);
                LOG.debug("setting CGMES DL IIDM extensions for StaticVarCompensator: {}, {}", staticVarCompensator.getId(), svcDiagramPoint);
                staticVarCompensator.addExtension(InjectionDiagramData.class, svcDiagramData);
            });

            substation.getTwoWindingsTransformerStream().forEach(twoWindingsTransformer -> vlGraph.getNodes().stream()
                    .filter(node -> checkNode(twoWindingsTransformer, node)).findFirst().ifPresent(node -> {
                        FeederNode transformerNode = (FeederNode) node;
                        DiagramPoint tDiagramPoint = offsetPoint.newDiagramPoint(transformerNode.getX(), transformerNode.getY(), transformerNode.getOrder());
                        CouplingDeviceDiagramData<TwoWindingsTransformer> transformerIidmDiagramData = new CouplingDeviceDiagramData<>(twoWindingsTransformer);
                        CouplingDeviceDiagramData.CouplingDeviceDiagramDetails diagramDetails = transformerIidmDiagramData.new CouplingDeviceDiagramDetails(tDiagramPoint, rotationValue(transformerNode));
                        transformerIidmDiagramData.addData(diagramName, diagramDetails);
                        LOG.debug("setting CGMES DL IIDM extensions for TwoWindingTransformer: {}, {}", twoWindingsTransformer.getId(), tDiagramPoint);
                        twoWindingsTransformer.addExtension(CouplingDeviceDiagramData.class, transformerIidmDiagramData);
                    })
            );

            substation.getThreeWindingsTransformerStream().forEach(threeWindingsTransformer -> vlGraph.getNodes().stream()
                    .filter(node -> checkNode(threeWindingsTransformer, node)).findFirst().ifPresent(node -> {
                        DiagramPoint tDiagramPoint = offsetPoint.newDiagramPoint(node.getX(), node.getY(), 0);
                        ThreeWindingsTransformerDiagramData transformerIidmDiagramData = new ThreeWindingsTransformerDiagramData(threeWindingsTransformer);
                        ThreeWindingsTransformerDiagramData.ThreeWindingsTransformerDiagramDataDetails diagramDetails = transformerIidmDiagramData.new ThreeWindingsTransformerDiagramDataDetails(tDiagramPoint, rotationValue(node));
                        transformerIidmDiagramData.addData(diagramName, diagramDetails);
                        LOG.debug("setting CGMES DL IIDM extensions for ThreeWindingTransformer: {}, {}", threeWindingsTransformer.getId(), tDiagramPoint);
                        threeWindingsTransformer.addExtension(ThreeWindingsTransformerDiagramData.class, transformerIidmDiagramData);
                    })
            );

            vlGraph.getNodes().stream().filter(this::isLineNode).forEach(node -> {
                switch (node.getComponentType()) {
                    case LINE:
                        FeederNode lineNode = (FeederNode) node;
                        Line line = vlGraph.getVoltageLevel().getConnectable(getBranchId(lineNode.getId()), Line.class);
                        if (line != null) {
                            LineDiagramData<Line> lineDiagramData = LineDiagramData.getOrCreateDiagramData(line);
                            int lineSeq = getMaxSeq(lineDiagramData.getPoints(diagramName)) + 1;
                            DiagramPoint linePoint = offsetPoint.newDiagramPoint(lineNode.getX(), lineNode.getY(), lineSeq);
                            lineDiagramData.addPoint(diagramName, linePoint);

                            LOG.debug("setting CGMES DL IIDM extensions for Line {} ({}), new point {}", line.getId(), line.getName(), linePoint);
                            line.addExtension(LineDiagramData.class, lineDiagramData);
                        }
                        break;
                    case DANGLING_LINE:
                        FeederNode danglingLineNode = (FeederNode) node;
                        DanglingLine danglingLine = vlGraph.getVoltageLevel().getConnectable(danglingLineNode.getId(), DanglingLine.class);
                        if (danglingLine != null) {
                            LineDiagramData<DanglingLine> danglingLineDiagramData = LineDiagramData.getOrCreateDiagramData(danglingLine);
                            int danglingLineSeq = getMaxSeq(danglingLineDiagramData.getPoints(diagramName)) + 1;
                            DiagramPoint danglingLinePoint = offsetPoint.newDiagramPoint(danglingLineNode.getX(), danglingLineNode.getY(), danglingLineSeq);
                            danglingLineDiagramData.addPoint(diagramName, danglingLinePoint);

                            LOG.debug("setting CGMES DL IIDM extensions for Dangling line {} ({}),  point {}", danglingLine.getId(), danglingLine.getName(), danglingLinePoint);
                            danglingLine.addExtension(LineDiagramData.class, danglingLineDiagramData);
                        }
                        break;
                    default:
                        break;
                }
            });

            if (TopologyKind.BUS_BREAKER.equals(voltageLevel.getTopologyKind())) {
                voltageLevel.getBusBreakerView().getBusStream().forEach(bus ->
                    vlGraph.getNodeBuses().stream().filter(busNode -> busNode.getId().equals(bus.getId())).findFirst().ifPresent(busNode -> {
                        NodeDiagramData<Bus> busDiagramData = NodeDiagramData.getOrCreateDiagramData(bus);
                        setNodeDiagramPoints(busDiagramData, busNode, offsetPoint, diagramName);
                        LOG.debug("setting CGMES DL IIDM extensions for Bus {}, {} - {}", bus.getId(), busDiagramData.getData(diagramName).getPoint1(), busDiagramData.getData(diagramName).getPoint2());
                        bus.addExtension(NodeDiagramData.class, busDiagramData);
                    })
                );

            } else {
                voltageLevel.getNodeBreakerView().getBusbarSectionStream().forEach(busbarSection ->
                    vlGraph.getNodeBuses().stream().filter(busNode -> busNode.getId().equals(busbarSection.getId())).findFirst().ifPresent(busNode -> {
                        NodeDiagramData<BusbarSection> busbarSectionDiagramData = NodeDiagramData.getOrCreateDiagramData(busbarSection);
                        setNodeDiagramPoints(busbarSectionDiagramData, busNode, offsetPoint, diagramName);
                        LOG.debug("setting CGMES DL IIDM extensions for BusbarSection {}, {} - {}", busbarSection.getId(), busbarSectionDiagramData.getData(diagramName).getPoint1(), busbarSectionDiagramData.getData(diagramName).getPoint2());
                        busbarSection.addExtension(NodeDiagramData.class, busbarSectionDiagramData);
                    })
                );

                voltageLevel.getNodeBreakerView().getSwitchStream().filter(Objects::nonNull).forEach(sw -> {
                    Node swNode = vlGraph.getNode(sw.getId());
                    if (checkSwitchNode(swNode)) {
                        CouplingDeviceDiagramData<Switch> switchIidmDiagramData = new CouplingDeviceDiagramData<>(sw);
                        CouplingDeviceDiagramData.CouplingDeviceDiagramDetails diagramDetails = switchIidmDiagramData.new CouplingDeviceDiagramDetails(offsetPoint.newDiagramPoint(swNode.getX(), swNode.getY(), 0), switchRotationValue(swNode));
                        switchIidmDiagramData.addData(diagramName, diagramDetails);
                        LOG.debug("setting CGMES DL IIDM extensions for Switch {}, {}", sw.getId(), switchIidmDiagramData);
                        sw.addExtension(CouplingDeviceDiagramData.class, switchIidmDiagramData);
                    }
                });
            }
        });

        return subsBoundary;
    }

    private boolean checkSwitchNode(Node swNode) {
        return (swNode != null) && swNode.getType().equals(Node.NodeType.SWITCH);
    }

    private boolean checkNode(ThreeWindingsTransformer threeWindingsTransformer, Node node) {
        return node.getComponentType().equals(THREE_WINDINGS_TRANSFORMER) &&
            (((node instanceof Fictitious3WTNode) && ((Fictitious3WTNode) node).getTransformer().getId().equals(threeWindingsTransformer.getId()))
                || ((node instanceof Feeder3WTNode) && ((Feeder3WTNode) node).getTransformer().getId().equals(threeWindingsTransformer.getId())));
    }

    private boolean checkNode(TwoWindingsTransformer twoWindingsTransformer, Node node) {
        return node.getComponentType().equals(TWO_WINDINGS_TRANSFORMER) && node.getId().startsWith(twoWindingsTransformer.getId());
    }

    private double rotationValue(Node node) {
        return node.isRotated() ? 0.0 : 180.0;
    }

    private double switchRotationValue(Node node) {
        return node.isRotated() ? 90.0 : 0.0;
    }

    private void convertLayoutSingleDiagram(Network network, Stream<Substation> subsStream, String diagramName) {
        //creates a single CGMES-DL diagram (named diagramName), where each substation
        NetworkDiagramData.addDiagramName(network, diagramName);
        final double[] xoffset = {0.0};
        subsStream.forEach(s -> {
            LOG.debug("Substation {}({} offset: {})", s.getId(), s.getName(), xoffset[0]);
            LayoutInfo li = applyLayout(s, xoffset[0], 0.0, diagramName);
            xoffset[0] += OFFSET_MULTIPLIER_X * li.getMaxX();
        });
    }

    private void convertLayoutMultipleDiagrams(Network network, Stream<Substation> subsStream) {
        // creates one CGMES-DL diagram for each substation (where each diagram name is the substation's name)
        subsStream.forEach(s -> {
            String subDiagramName = StringUtils.isEmpty(s.getName()) ? s.getId() : s.getName();
            NetworkDiagramData.addDiagramName(network, subDiagramName);
            LOG.debug("Substation {}", subDiagramName);
            applyLayout(s, 0.0, 0.0, subDiagramName);
        });
    }

    /**
     * Apply the layout to the network, creating one or more CGMES-DL diagrams.
     * Note that a CGMES-DL diagram refers to a global coordinate system and can include all the network equipments,
     * whereas layouts are currently created per-substation (or per-voltage), using a coordinate system that is local to
     * the specific substation/voltage.
     *
     * This method creates either a single CGMES-DL diagram (where each substation is placed on a single row, one next to the other),
     * or multiple CGMES_DL diagrams, one per substation.
     *
     * @param network
     * @param diagramName the diagram's name, if <code>null</code> it creates one CGMES-DL diagram for each substation
     *                    (where each diagram name is the substation's name). Otherwise it creates a single CGMES-DL diagram
     *                    (named diagramName).
     */
    public void convertLayout(Network network, String diagramName) {
        Objects.requireNonNull(network);
        LOG.info("Converting layout {} to IIDM CGMES DL extensions for network: {}", sFactory.getClass(), network.getId());

        //Network could have already defined a set of iidm cgmes extensions, as loaded via the cgmes importer/cgmesDLImport postprocessor.
        //Also associated to the network, we have the triplestore with the DL related triples
        //clear the  CGMES DL profile data from the network's CGMES tiplestore, if it already exists
        //and remove any exising IIDM CGMES equipments' extensions
        CgmesDLUtils.clearCgmesDl(network);
        CgmesDLUtils.removeIidmCgmesExtensions(network);

        if (diagramName != null) {
            convertLayoutSingleDiagram(network, network.getSubstationStream(), diagramName);
        } else {
            convertLayoutMultipleDiagrams(network, network.getSubstationStream());
        }
    }

    /**
     * Apply the layout to the network, creating one CGMES-DL diagrams per substation.
     *
     * @param network
     */

    public void convertLayout(Network network) {
        convertLayout(network, null);
    }

    class LayoutInfo {
        double maxX;
        double maxY;

        LayoutInfo(double maxNodeX, double maxNodeY) {
            this.maxX = maxNodeX;
            this.maxY = maxNodeY;
        }

        double getMaxX() {
            return maxX;
        }

        double getMaxY() {
            return maxY;
        }

        void update(double maxX, double maxY) {
            if (maxX > this.maxX) {
                this.maxX = maxX;
            }
            if (maxY > this.maxY) {
                this.maxY = maxY;
            }
        }
    }

    class OffsetPoint {
        private final double dx;
        private final double dy;

        OffsetPoint(double dx, double dy) {
            this.dx = dx;
            this.dy = dy;
        }

        DiagramPoint newDiagramPoint(double x, double y, int seq) {
            return new DiagramPoint(x + dx, y + dy, seq);
        }
    }
}
