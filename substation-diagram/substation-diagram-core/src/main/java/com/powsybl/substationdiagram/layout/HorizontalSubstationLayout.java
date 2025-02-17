/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.layout;

import com.powsybl.substationdiagram.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public class HorizontalSubstationLayout extends AbstractSubstationLayout {

    public HorizontalSubstationLayout(SubstationGraph graph, VoltageLevelLayoutFactory vLayoutFactory) {
        super(graph, vLayoutFactory);
    }

    /**
     * Calculate relative coordinate of voltageLevel in the substation
     */
    @Override
    protected Coord calculateCoordVoltageLevel(LayoutParameters layoutParam, Graph vlGraph) {
        int maxH = vlGraph.getNodeBuses().stream()
                .mapToInt(nodeBus -> nodeBus.getPosition().getH() + nodeBus.getPosition().getHSpan())
                .max().orElse(0);

        double x = layoutParam.getInitialXBus() + (maxH + 2) * layoutParam.getCellWidth();
        double y = 0;
        return new Coord(x, y);
    }

    /*
     * Calculate polyline points of a snakeLine in the substation graph
     */
    @Override
    protected List<Double> calculatePolylineSnakeLine(LayoutParameters layoutParam,
                                                   Edge edge,
                                                   Map<BusCell.Direction, Integer> nbSnakeLinesTopBottom,
                                                   Map<Side, Integer> nbSnakeLinesLeftRight,
                                                   Map<String, Integer> nbSnakeLinesBetween,
                                                   Map<String, Integer> nbSnakeLinesBottomVL,
                                                   Map<String, Integer> nbSnakeLinesTopVL) {
        Node node1 = edge.getNode1();
        Node node2 = edge.getNode2();

        BusCell.Direction dNode1 = getNodeDirection(node1, 1);
        BusCell.Direction dNode2 = getNodeDirection(node2, 2);

        double xMaxGraph;
        String idMaxGraph;

        if (node1.getGraph().getX() > node2.getGraph().getX()) {
            xMaxGraph = node1.getGraph().getX();
            idMaxGraph = node1.getGraph().getVoltageLevel().getId();
        } else {
            xMaxGraph = node2.getGraph().getX();
            idMaxGraph = node2.getGraph().getVoltageLevel().getId();
        }

        double x1 = node1.getX();
        double y1 = node1.getY();
        double x2 = node2.getX();
        double y2 = node2.getY();

        InfoCalcPoints info = new InfoCalcPoints();
        info.setLayoutParam(layoutParam);
        info.setdNode1(dNode1);
        info.setdNode2(dNode2);
        info.setNbSnakeLinesTopBottom(nbSnakeLinesTopBottom);
        info.setNbSnakeLinesBetween(nbSnakeLinesBetween);
        info.setX1(x1);
        info.setX2(x2);
        info.setY1(y1);
        info.setY2(y2);
        info.setxMaxGraph(xMaxGraph);
        info.setIdMaxGraph(idMaxGraph);

        return calculatePolylinePoints(info);
    }

    public static List<Double> calculatePolylinePoints(InfoCalcPoints info) {
        List<Double> pol = new ArrayList<>();

        LayoutParameters layoutParam = info.getLayoutParam();
        BusCell.Direction dNode1 = info.getdNode1();
        BusCell.Direction dNode2 = info.getdNode2();
        Map<BusCell.Direction, Integer> nbSnakeLinesTopBottom = info.getNbSnakeLinesTopBottom();
        Map<String, Integer> nbSnakeLinesBetween = info.getNbSnakeLinesBetween();
        double x1 = info.getX1();
        double x2 = info.getX2();
        double y1 = info.getY1();
        double y2 = info.getY2();
        double xMaxGraph = info.getxMaxGraph();
        String idMaxGraph = info.getIdMaxGraph();

        switch (dNode1) {
            case BOTTOM:
                if (dNode2 == BusCell.Direction.BOTTOM) {  // BOTTOM to BOTTOM
                    nbSnakeLinesTopBottom.compute(dNode1, (k, v) -> v + 1);
                    double decalV = nbSnakeLinesTopBottom.get(dNode1) * layoutParam.getVerticalSnakeLinePadding();
                    double yDecal = Math.max(y1 + decalV, y2 + decalV);

                    pol.addAll(Arrays.asList(x1, y1,
                            x1, yDecal,
                            x2, yDecal,
                            x2, y2));

                } else {  // BOTTOM to TOP
                    nbSnakeLinesTopBottom.compute(dNode1, (k, v) -> v + 1);
                    nbSnakeLinesTopBottom.compute(dNode2, (k, v) -> v + 1);
                    nbSnakeLinesBetween.compute(idMaxGraph, (k, v) -> v + 1);
                    double decal1V = nbSnakeLinesTopBottom.get(dNode1) * layoutParam.getVerticalSnakeLinePadding();
                    double decal2V = nbSnakeLinesTopBottom.get(dNode2) * layoutParam.getVerticalSnakeLinePadding();
                    double xBetweenGraph = xMaxGraph - (nbSnakeLinesBetween.get(idMaxGraph) * layoutParam.getHorizontalSnakeLinePadding());

                    pol.addAll(Arrays.asList(x1, y1,
                            x1, y1 + decal1V,
                            xBetweenGraph, y1 + decal1V,
                            xBetweenGraph, y2 - decal2V,
                            x2, y2 - decal2V,
                            x2, y2));
                }
                break;

            case TOP:
                if (dNode2 == BusCell.Direction.TOP) {  // TOP to TOP
                    nbSnakeLinesTopBottom.compute(dNode1, (k, v) -> v + 1);
                    double decalV = nbSnakeLinesTopBottom.get(dNode1) * layoutParam.getVerticalSnakeLinePadding();
                    double yDecal = Math.min(y1 - decalV, y2 - decalV);

                    pol.addAll(Arrays.asList(x1, y1,
                            x1, yDecal,
                            x2, yDecal,
                            x2, y2));
                } else {  // TOP to BOTTOM
                    nbSnakeLinesTopBottom.compute(dNode1, (k, v) -> v + 1);
                    nbSnakeLinesTopBottom.compute(dNode2, (k, v) -> v + 1);
                    nbSnakeLinesBetween.compute(idMaxGraph, (k, v) -> v + 1);
                    double decal1V = nbSnakeLinesTopBottom.get(dNode1) * layoutParam.getVerticalSnakeLinePadding();
                    double decal2V = nbSnakeLinesTopBottom.get(dNode2) * layoutParam.getVerticalSnakeLinePadding();

                    double xBetweenGraph = xMaxGraph - (nbSnakeLinesBetween.get(idMaxGraph) * layoutParam.getHorizontalSnakeLinePadding());

                    pol.addAll(Arrays.asList(x1, y1,
                            x1, y1 - decal1V,
                            xBetweenGraph, y1 - decal1V,
                            xBetweenGraph, y2 + decal2V,
                            x2, y2 + decal2V,
                            x2, y2));
                }
                break;
            default:
        }
        return pol;
    }

    @Override
    protected double getHorizontalSubstationPadding(LayoutParameters layoutParameters) {
        return layoutParameters.getHorizontalSubstationPadding();
    }

    @Override
    protected double getVerticalSubstationPadding(LayoutParameters layoutParameters) {
        return 0;
    }

}
