/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram;

import com.google.common.io.ByteStreams;
import com.powsybl.iidm.network.*;
import com.powsybl.substationdiagram.layout.*;
import com.powsybl.substationdiagram.library.ComponentLibrary;
import com.powsybl.substationdiagram.library.ResourcesComponentLibrary;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.svg.DefaultNodeLabelConfiguration;
import com.powsybl.substationdiagram.svg.DefaultSubstationDiagramInitialValueProvider;
import com.powsybl.substationdiagram.svg.DefaultSVGWriter;
import com.powsybl.substationdiagram.util.NominalVoltageSubstationDiagramStyleProvider;
import com.rte_france.powsybl.iidm.network.extensions.cvg.BusbarSectionPosition;
import com.rte_france.powsybl.iidm.network.extensions.cvg.ConnectablePosition;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public class TestCase12GraphWith3WT extends AbstractTestCase {

    private VoltageLevel vl1;
    private VoltageLevel vl2;
    private VoltageLevel vl3;

    @Before
    public void setUp() {
        network = Network.create("testCase11", "test");

        substation = createSubstation(network, "subst", "subst", Country.FR);

        // first voltage level
        //
        vl1 = createVoltageLevel(substation, "vl1", "vl1", TopologyKind.NODE_BREAKER, 400, 50);

        createBusBarSection(vl1, "bbs1", "bbs1", 0, 1, 1);
        createBusBarSection(vl1, "bbs2", "bbs2", 1, 1, 2);
        createBusBarSection(vl1, "bbs3", "bbs3", 2, 2, 1);
        createBusBarSection(vl1, "bbs4", "bbs4", 3, 2, 2);

        createSwitch(vl1, "dsect11", "dsect11", SwitchKind.DISCONNECTOR, false, false, true, 0, 14);
        createSwitch(vl1, "dtrct11", "dtrct11", SwitchKind.BREAKER, true, false, true, 14, 15);
        createSwitch(vl1, "dsect12", "dsect12", SwitchKind.DISCONNECTOR, false, false, true, 15, 1);

        createSwitch(vl1, "dsect21", "dsect21", SwitchKind.DISCONNECTOR, false, false, true, 2, 16);
        createSwitch(vl1, "dtrct21", "dtrct21", SwitchKind.BREAKER, true, false, true, 16, 17);
        createSwitch(vl1, "dsect22", "dsect22", SwitchKind.DISCONNECTOR, false, false, true, 17, 3);

        createLoad(vl1, "load1", "load1", "load1", 0, ConnectablePosition.Direction.TOP, 4, 10, 10);
        createSwitch(vl1, "dload1", "dload1", SwitchKind.DISCONNECTOR, false, false, true, 0, 5);
        createSwitch(vl1, "bload1", "bload1", SwitchKind.BREAKER, true, false, true, 4, 5);

        createGenerator(vl1, "gen1", "gen1", "gen1", 1, ConnectablePosition.Direction.BOTTOM, 6, 0, 20, false, 10, 10);
        createSwitch(vl1, "dgen1", "dgen1", SwitchKind.DISCONNECTOR, false, false, true, 2, 7);
        createSwitch(vl1, "bgen1", "bgen1", SwitchKind.BREAKER, true, false, true, 6, 7);

        createLoad(vl1, "load2", "load2", "load2", 2, ConnectablePosition.Direction.TOP, 8, 10, 10);
        createSwitch(vl1, "dload2", "dload2", SwitchKind.DISCONNECTOR, false, false, true, 1, 9);
        createSwitch(vl1, "bload2", "bload2", SwitchKind.BREAKER, true, false, true, 8, 9);

        createGenerator(vl1, "gen2", "gen2", "gen2", 3, ConnectablePosition.Direction.BOTTOM, 10, 0, 20, false, 10, 10);
        createSwitch(vl1, "dgen2", "dgen2", SwitchKind.DISCONNECTOR, false, false, true, 3, 11);
        createSwitch(vl1, "bgen2", "bgen2", SwitchKind.BREAKER, true, false, true, 10, 11);

        // second voltage level
        //
        vl2 = createVoltageLevel(substation, "vl2", "vl2", TopologyKind.NODE_BREAKER, 225, 50);

        createBusBarSection(vl2, "bbs5", "bbs5", 0, 1, 1);
        createBusBarSection(vl2, "bbs6", "bbs6", 1, 2, 1);

        createSwitch(vl2, "dscpl1", "dscpl1", SwitchKind.DISCONNECTOR, false, false, true, 0, 6);
        createSwitch(vl2, "ddcpl1", "ddcpl1", SwitchKind.BREAKER, true, false, true, 6, 7);
        createSwitch(vl2, "dscpl2", "dscpl2", SwitchKind.DISCONNECTOR, false, false, true, 7, 1);

        createLoad(vl2, "load3", "load3", "load3", 0, ConnectablePosition.Direction.TOP, 2, 10, 10);
        createSwitch(vl2, "dload3", "dload3", SwitchKind.DISCONNECTOR, false, false, true, 0, 3);
        createSwitch(vl2, "bload3", "bload3", SwitchKind.BREAKER, true, false, true, 2, 3);

        createGenerator(vl2, "gen4", "gen4", "gen4", 1, ConnectablePosition.Direction.BOTTOM, 4, 0, 20, false, 10, 10);
        createSwitch(vl2, "dgen4", "dgen4", SwitchKind.DISCONNECTOR, false, false, true, 1, 5);
        createSwitch(vl2, "bgen4", "bgen4", SwitchKind.BREAKER, true, false, true, 4, 5);

        // third voltage level
        //
        vl3 = createVoltageLevel(substation, "vl3", "vl3", TopologyKind.NODE_BREAKER, 63, 50);

        createBusBarSection(vl3, "bbs7", "bbs7", 0, 1, 1);

        createLoad(vl3, "load4", "load4", "load4", 0, ConnectablePosition.Direction.TOP, 1, 10, 10);
        createSwitch(vl3, "dload4", "dload4", SwitchKind.DISCONNECTOR, false, false, true, 0, 2);
        createSwitch(vl3, "bload4", "bload4", SwitchKind.BREAKER, true, false, true, 2, 1);
        createShunt(vl3, "self4", "self4", "self4", 1, ConnectablePosition.Direction.BOTTOM, 3, -1, 1, 1);
        createSwitch(vl3, "dself4", "dself4", SwitchKind.DISCONNECTOR, false, false, true, 0, 4);
        createSwitch(vl3, "bself4", "bself4", SwitchKind.BREAKER, true, false, true, 4, 3);

        // two windings transformers between voltage levels
        //
        createSwitch(vl1, "dtrf11", "dtrf11", SwitchKind.DISCONNECTOR, false, false, true, 0, 18);
        createSwitch(vl1, "btrf11", "btrf11", SwitchKind.BREAKER, true, false, true, 18, 19);
        createSwitch(vl2, "dtrf21", "dtrf21", SwitchKind.DISCONNECTOR, false, false, true, 0, 8);
        createSwitch(vl2, "btrf21", "btrf21", SwitchKind.BREAKER, true, false, true, 8, 9);
        createTwoWindingsTransformer(substation, "trf1", "trf1", 2.0, 14.745, 0.0, 3.2E-5, 400.0, 225.0,
                                     19, 9, vl1.getId(), vl2.getId(),
                                     "trf1", 1, ConnectablePosition.Direction.TOP,
                                     "trf1", 1, ConnectablePosition.Direction.TOP);

        createSwitch(vl1, "dtrf12", "dtrf12", SwitchKind.DISCONNECTOR, false, false, true, 1, 20);
        createSwitch(vl1, "btrf12", "btrf12", SwitchKind.BREAKER, true, false, true, 20, 21);
        createSwitch(vl2, "dtrf22", "dtrf22", SwitchKind.DISCONNECTOR, false, false, true, 1, 10);
        createSwitch(vl2, "btrf22", "btrf22", SwitchKind.BREAKER, true, false, true, 10, 11);
        createTwoWindingsTransformer(substation, "trf2", "trf2", 2.0, 14.745, 0.0, 3.2E-5, 400.0, 225.0,
                21, 11, vl1.getId(), vl2.getId(),
                "trf2", 3, ConnectablePosition.Direction.TOP,
                "trf2", 3, ConnectablePosition.Direction.BOTTOM);

        createSwitch(vl1, "dtrf13", "dtrf13", SwitchKind.DISCONNECTOR, false, false, true, 2, 22);
        createSwitch(vl1, "btrf13", "btrf13", SwitchKind.BREAKER, true, false, true, 22, 23);
        createSwitch(vl2, "dtrf23", "dtrf23", SwitchKind.DISCONNECTOR, false, false, true, 1, 12);
        createSwitch(vl2, "btrf23", "btrf23", SwitchKind.BREAKER, true, false, true, 12, 13);
        createTwoWindingsTransformer(substation, "trf3", "trf3", 2.0, 14.745, 0.0, 3.2E-5, 400.0, 225.0,
                23, 13, vl1.getId(), vl2.getId(),
                "trf3", 1, ConnectablePosition.Direction.BOTTOM,
                "trf3", 4, ConnectablePosition.Direction.BOTTOM);

        createSwitch(vl1, "dtrf14", "dtrf14", SwitchKind.DISCONNECTOR, false, false, true, 3, 24);
        createSwitch(vl1, "btrf14", "btrf14", SwitchKind.BREAKER, true, false, true, 24, 25);
        createSwitch(vl2, "dtrf24", "dtrf24", SwitchKind.DISCONNECTOR, false, false, true, 0, 14);
        createSwitch(vl2, "btrf24", "btrf24", SwitchKind.BREAKER, true, false, true, 14, 15);
        createTwoWindingsTransformer(substation, "trf4", "trf4", 2.0, 14.745, 0.0, 3.2E-5, 400.0, 225.0,
                25, 15, vl1.getId(), vl2.getId(),
                "trf4", 2, ConnectablePosition.Direction.BOTTOM,
                "trf4", 2, ConnectablePosition.Direction.TOP);

        createSwitch(vl1, "dtrf15", "dtrf15", SwitchKind.DISCONNECTOR, false, false, true, 0, 26);
        createSwitch(vl1, "btrf15", "btrf15", SwitchKind.BREAKER, true, false, true, 26, 27);
        createSwitch(vl3, "dtrf25", "dtrf25", SwitchKind.DISCONNECTOR, false, false, true, 0, 5);
        createSwitch(vl3, "btrf25", "btrf25", SwitchKind.BREAKER, true, false, true, 5, 6);
        createTwoWindingsTransformer(substation, "trf5", "trf5", 2.0, 14.745, 0.0, 3.2E-5, 400.0, 225.0,
                27, 6, vl1.getId(), vl3.getId(),
                "trf5", 2, ConnectablePosition.Direction.TOP,
                "trf5", 1, ConnectablePosition.Direction.BOTTOM);

        // three windings transformers between voltage levels
        //
        createSwitch(vl1, "dtrf16", "dtrf16", SwitchKind.DISCONNECTOR, false, false, true, 0, 28);
        createSwitch(vl1, "btrf16", "btrf16", SwitchKind.BREAKER, true, false, true, 28, 29);
        createSwitch(vl2, "dtrf26", "dtrf26", SwitchKind.DISCONNECTOR, false, false, true, 1, 16);
        createSwitch(vl2, "btrf26", "btrf26", SwitchKind.BREAKER, true, false, true, 16, 17);
        createSwitch(vl3, "dtrf36", "dtrf36", SwitchKind.DISCONNECTOR, false, false, true, 0, 7);
        createSwitch(vl3, "btrf36", "btrf36", SwitchKind.BREAKER, true, false, true, 7, 8);

        createThreeWindingsTransformer(substation, "trf6", "trf6", vl1.getId(), vl2.getId(), vl3.getId(),
                                       0.5, 0.5, 0.5, 1., 1., 1., 0.1, 0.1,
                                       400., 225., 225.,
                                       29, 17, 8,
                                       "trf61", 2, ConnectablePosition.Direction.TOP,
                                       "trf62", 2, ConnectablePosition.Direction.TOP,
                                       "trf63", 2, ConnectablePosition.Direction.TOP);

        createSwitch(vl1, "dtrf17", "dtrf17", SwitchKind.DISCONNECTOR, false, false, true, 2, 30);
        createSwitch(vl1, "btrf17", "btrf17", SwitchKind.BREAKER, true, false, true, 30, 31);
        createSwitch(vl2, "dtrf27", "dtrf27", SwitchKind.DISCONNECTOR, false, false, true, 0, 18);
        createSwitch(vl2, "btrf27", "btrf27", SwitchKind.BREAKER, true, false, true, 18, 19);
        createSwitch(vl3, "dtrf37", "dtrf37", SwitchKind.DISCONNECTOR, false, false, true, 0, 9);
        createSwitch(vl3, "btrf37", "btrf37", SwitchKind.BREAKER, true, false, true, 9, 10);

        createThreeWindingsTransformer(substation, "trf7", "trf7", vl1.getId(), vl2.getId(), vl3.getId(),
                0.5, 0.5, 0.5, 1., 1., 1., 0.1, 0.1,
                400., 225., 225.,
                31, 19, 10,
                "trf71", 2, ConnectablePosition.Direction.BOTTOM,
                "trf72", 2, ConnectablePosition.Direction.TOP,
                "trf73", 2, ConnectablePosition.Direction.BOTTOM);

        createSwitch(vl1, "dtrf18", "dtrf18", SwitchKind.DISCONNECTOR, false, false, true, 1, 32);
        createSwitch(vl1, "btrf18", "btrf18", SwitchKind.BREAKER, true, false, true, 32, 33);
        createSwitch(vl2, "dtrf28", "dtrf28", SwitchKind.DISCONNECTOR, false, false, true, 1, 20);
        createSwitch(vl2, "btrf28", "btrf28", SwitchKind.BREAKER, true, false, true, 20, 21);
        createSwitch(vl3, "dtrf38", "dtrf38", SwitchKind.DISCONNECTOR, false, false, true, 0, 11);
        createSwitch(vl3, "btrf38", "btrf38", SwitchKind.BREAKER, true, false, true, 11, 12);

        createThreeWindingsTransformer(substation, "trf8", "trf8", vl1.getId(), vl2.getId(), vl3.getId(),
                0.5, 0.5, 0.5, 1., 1., 1., 0.1, 0.1,
                400., 225., 225.,
                33, 21, 12,
                "trf81", 2, ConnectablePosition.Direction.TOP,
                "trf82", 2, ConnectablePosition.Direction.BOTTOM,
                "trf83", 2, ConnectablePosition.Direction.TOP);

        createShunt(vl3, "self5", "self5", "self5", 2, ConnectablePosition.Direction.BOTTOM, 13, -1, 1, 1);
        createSwitch(vl3, "dself5", "dself5", SwitchKind.DISCONNECTOR, false, false, true, 0, 14);
        createSwitch(vl3, "bself5", "bself5", SwitchKind.BREAKER, true, false, true, 14, 13);

        createBusBarSection(vl3, "bbs8", "bbs8", 15, 1, 2);

        createShunt(vl3, "self6", "self6", "self6", 3, ConnectablePosition.Direction.BOTTOM, 16, 1, 1, 1);
        createSwitch(vl3, "dself6", "dself6", SwitchKind.DISCONNECTOR, false, false, true, 15, 17);
        createSwitch(vl3, "bself6", "bself6", SwitchKind.BREAKER, true, false, true, 17, 16);
    }

    private static Substation createSubstation(Network n, String id, String name, Country country) {
        Substation s = n.newSubstation()
                .setId(id)
                .setName(name)
                .setCountry(country)
                .add();
        return s;
    }

    private static VoltageLevel createVoltageLevel(Substation s, String id, String name,
                                                   TopologyKind topology, double vNom, int nodeCount) {
        VoltageLevel vl = s.newVoltageLevel()
                .setId(id)
                .setName(name)
                .setTopologyKind(topology)
                .setNominalV(vNom)
                .add();
        vl.getNodeBreakerView()
                .setNodeCount(nodeCount);
        return vl;
    }

    private static void createSwitch(VoltageLevel vl, String id, String name, SwitchKind kind, boolean retained, boolean open, boolean fictitious, int node1, int node2) {
        vl.getNodeBreakerView().newSwitch()
                .setId(id)
                .setName(name)
                .setKind(kind)
                .setRetained(retained)
                .setOpen(open)
                .setFictitious(fictitious)
                .setNode1(node1)
                .setNode2(node2)
                .add();
    }

    private static void createBusBarSection(VoltageLevel vl, String id, String name, int node, int busbarIndex, int sectionIndex) {
        BusbarSection bbs = vl.getNodeBreakerView().newBusbarSection()
                .setId(id)
                .setName(name)
                .setNode(node)
                .add();
        bbs.addExtension(BusbarSectionPosition.class, new BusbarSectionPosition(bbs, busbarIndex, sectionIndex));
    }

    private static void createLoad(VoltageLevel vl, String id, String name, String feederName, int feederOrder,
                                   ConnectablePosition.Direction direction, int node, double p0, double q0) {
        Load load = vl.newLoad()
                .setId(id)
                .setName(name)
                .setNode(node)
                .setP0(p0)
                .setQ0(q0)
                .add();
        load.addExtension(ConnectablePosition.class, new ConnectablePosition<>(load, new ConnectablePosition
                .Feeder(feederName, feederOrder, direction), null, null, null));
    }

    private static void createGenerator(VoltageLevel vl, String id, String name, String feederName, int feederOrder,
                                        ConnectablePosition.Direction direction, int node,
                                        double minP, double maxP, boolean voltageRegulator,
                                        double targetP, double targetQ) {
        Generator gen = vl.newGenerator()
                .setId(id)
                .setName(name)
                .setNode(node)
                .setMinP(minP)
                .setMaxP(maxP)
                .setVoltageRegulatorOn(voltageRegulator)
                .setTargetP(targetP)
                .setTargetQ(targetQ)
                .add();
        gen.addExtension(ConnectablePosition.class, new ConnectablePosition<>(gen, new ConnectablePosition
                .Feeder(feederName, feederOrder, direction), null, null, null));
    }

    private static void createShunt(VoltageLevel vl, String id, String name, String feederName, int feederOrder,
                                   ConnectablePosition.Direction direction, int node,
                                    double bPerSection, int maximumSectionCount, int currentSectionCount) {
        ShuntCompensator shunt = vl.newShuntCompensator()
                .setId(id)
                .setName(name)
                .setNode(node)
                .setbPerSection(bPerSection)
                .setMaximumSectionCount(maximumSectionCount)
                .setCurrentSectionCount(currentSectionCount)
                .add();
        shunt.addExtension(ConnectablePosition.class, new ConnectablePosition<>(shunt, new ConnectablePosition
                .Feeder(feederName, feederOrder, direction), null, null, null));
    }

    private static void createTwoWindingsTransformer(Substation s, String id, String name,
                                                     double r, double x, double g, double b,
                                                     double ratedU1, double ratedU2,
                                                     int node1, int node2,
                                                     String idVoltageLevel1, String idVoltageLevel2,
                                                     String feederName1, int feederOrder1, ConnectablePosition.Direction direction1,
                                                     String feederName2, int feederOrder2, ConnectablePosition.Direction direction2) {
        TwoWindingsTransformer t = s.newTwoWindingsTransformer()
                .setId(id)
                .setName(name)
                .setR(r)
                .setX(x)
                .setG(g)
                .setB(b)
                .setRatedU1(ratedU1)
                .setRatedU2(ratedU2)
                .setNode1(node1)
                .setVoltageLevel1(idVoltageLevel1)
                .setNode2(node2)
                .setVoltageLevel2(idVoltageLevel2)
                .add();
        t.addExtension(ConnectablePosition.class,
                       new ConnectablePosition<>(t,
                               null,
                               new ConnectablePosition.Feeder(feederName1, feederOrder1, direction1),
                               new ConnectablePosition.Feeder(feederName2, feederOrder2, direction2),
                               null));
    }

    private static void createThreeWindingsTransformer(Substation s, String id, String name,
                                                       String vl1, String vl2, String vl3,
                                                       double r1, double r2, double r3,
                                                       double x1, double x2, double x3,
                                                       double g1, double b1,
                                                       double ratedU1, double ratedU2, double ratedU3,
                                                       int node1, int node2, int node3,
                                                       String feederName1, int feederOrder1, ConnectablePosition.Direction direction1,
                                                       String feederName2, int feederOrder2, ConnectablePosition.Direction direction2,
                                                       String feederName3, int feederOrder3, ConnectablePosition.Direction direction3) {
        ThreeWindingsTransformer t = s.newThreeWindingsTransformer()
                .setId(id)
                .setName(name)
                .newLeg1()
                .setR(r1)
                .setX(x1)
                .setG(g1)
                .setB(b1)
                .setRatedU(ratedU1)
                .setVoltageLevel(vl1)
                .setNode(node1)
                .add()
                .newLeg2()
                .setR(r2)
                .setX(x2)
                .setRatedU(ratedU2)
                .setVoltageLevel(vl2)
                .setNode(node2)
                .add()
                .newLeg3()
                .setR(r3)
                .setX(x3)
                .setRatedU(ratedU3)
                .setVoltageLevel(vl3)
                .setNode(node3)
                .add()
                .add();

        t.addExtension(ConnectablePosition.class,
                new ConnectablePosition<>(t,
                        null,
                        new ConnectablePosition.Feeder(feederName1, feederOrder1, direction1),
                        new ConnectablePosition.Feeder(feederName2, feederOrder2, direction2),
                        new ConnectablePosition.Feeder(feederName3, feederOrder3, direction3)));
    }

    @Test
    public void test() {
        LayoutParameters layoutParameters = new LayoutParameters()
                .setTranslateX(20)
                .setTranslateY(50)
                .setInitialXBus(0)
                .setInitialYBus(260)
                .setVerticalSpaceBus(25)
                .setHorizontalBusPadding(20)
                .setCellWidth(80)
                .setExternCellHeight(250)
                .setInternCellHeight(40)
                .setStackHeight(30)
                .setShowGrid(true)
                .setShowInternalNodes(false)
                .setScaleFactor(1)
                .setHorizontalSubstationPadding(50)
                .setVerticalSubstationPadding(50)
                .setDrawStraightWires(false)
                .setHorizontalSnakeLinePadding(30)
                .setVerticalSnakeLinePadding(30)
                .setShowInductorFor3WT(true);

        // build voltage level 1 graph
        Graph g1 = Graph.create(vl1, false, true, true);
        new ImplicitCellDetector().detectCells(g1);
        new BlockOrganizer().organize(g1);
        new PositionVoltageLevelLayout(g1).run(layoutParameters);

        Graph g2 = Graph.create(vl2, false, true, true);
        new ImplicitCellDetector().detectCells(g2);
        new BlockOrganizer().organize(g2);
        new PositionVoltageLevelLayout(g2).run(layoutParameters);

        Graph g3 = Graph.create(vl3, false, true, false);
        new ImplicitCellDetector().detectCells(g3);
        new BlockOrganizer().organize(g3);
        new PositionVoltageLevelLayout(g3).run(layoutParameters);

        // write SVG and compare to reference (horizontal layout)
        compareSvg(g1, layoutParameters, "/TestCase12GraphVL1.svg");
        compareSvg(g2, layoutParameters, "/TestCase12GraphVL2.svg");
        compareSvg(g3, layoutParameters, "/TestCase12GraphVL3.svg");

        // write optimized SVG and compare to reference (horizontal layout)
        LayoutParameters layoutParametersOptimized = new LayoutParameters(layoutParameters);
        layoutParametersOptimized.setAvoidSVGComponentsDuplication(true);

        compareSvg(g1, layoutParametersOptimized, "/TestCase12GraphVL1_optimized.svg");
        compareSvg(g2, layoutParametersOptimized, "/TestCase12GraphVL2_optimized.svg");
        compareSvg(g3, layoutParametersOptimized, "/TestCase12GraphVL3_optimized.svg");

        // Create voltageLevel diagram (svg + metadata files)
        VoltageLevelDiagram diagram = VoltageLevelDiagram.build(vl1, new PositionVoltageLevelLayoutFactory(), false, true);
        Path pathSVG = Paths.get(System.getProperty("user.home"), "vlDiag.svg");
        Path pathMetadata = Paths.get(System.getProperty("user.home"), "vlDiag_metadata.json");

        ComponentLibrary componentLibrary = new ResourcesComponentLibrary("/ConvergenceLibrary");
        diagram.writeSvg("", new DefaultSVGWriter(componentLibrary, layoutParameters),
                new DefaultSubstationDiagramInitialValueProvider(network),
                new NominalVoltageSubstationDiagramStyleProvider(),
                new DefaultNodeLabelConfiguration(componentLibrary),
                pathSVG,
                false);
        Assert.assertTrue(Files.exists(pathSVG));
        Assert.assertTrue(Files.exists(pathMetadata));
        try {
            String refSvg = normalizeLineSeparator(new String(ByteStreams.toByteArray(getClass().getResourceAsStream("/vlDiag.svg")), StandardCharsets.UTF_8));
            String svg = normalizeLineSeparator(new String(Files.readAllBytes(pathSVG), StandardCharsets.UTF_8));
            assertEquals(refSvg, svg);
            Files.deleteIfExists(pathSVG);

            String refMetadata = normalizeLineSeparator(new String(ByteStreams.toByteArray(getClass().getResourceAsStream("/vlDiag_metadata.json")), StandardCharsets.UTF_8));
            String metadata = normalizeLineSeparator(new String(Files.readAllBytes(pathMetadata), StandardCharsets.UTF_8));
            assertEquals(refMetadata, metadata);
            Files.deleteIfExists(pathMetadata);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
