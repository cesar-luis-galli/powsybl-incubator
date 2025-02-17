/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.layout;

import com.powsybl.substationdiagram.model.Cell;
import com.powsybl.substationdiagram.model.ExternCell;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.InternCell;
import com.powsybl.substationdiagram.model.Node;
import com.powsybl.substationdiagram.model.ShuntCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ImplicitCellDetector implements CellDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImplicitCellDetector.class);
    private boolean removeUnnecessaryFictitiousNodes;
    private boolean substituteSingularFictitiousByFeederNode;

    public ImplicitCellDetector(boolean removeUnnecessaryFictitiousNodes, boolean substituteSingularFictitiousByFeederNode) {
        this.removeUnnecessaryFictitiousNodes = removeUnnecessaryFictitiousNodes;
        this.substituteSingularFictitiousByFeederNode = substituteSingularFictitiousByFeederNode;
    }

    public ImplicitCellDetector() {
        this(true, true);
    }


    /**
     * internCell detection : an internal cell is composed of nodes connecting BUSes without connecting Feeder.
     * genericDetectCell is used to detect cells exploring the graph and scanning exclusionTypes and stopTypes
     * <p>
     * *************INTERN CELL*******************
     * exclusion types = {FEEDER} : if a FEEDER type is reached it is not an INTERN CELL
     * stop the visit if reach a Bus : stopTypes = {BUS}
     * ***************EXTERN AND SHUNT CELLS******
     * detection : nodes connecting buses and departures without being in an internCell (previously allocated nodes)
     * exclusion types = {}
     * stop the visit if reach a Bus : stopTypes = {BUS,FEEDER} * @param graph g
     */
    @Override
    public void detectCells(Graph graph) {
        cleaning(graph);

        LOGGER.info("Detecting cells...");

        List<Node> allocatedNodes = new ArrayList<>();
        // **************INTERN CELL*******************
        List<Node.NodeType> exclusionTypes = new ArrayList<>();
        exclusionTypes.add(Node.NodeType.FEEDER);
        List<Node.NodeType> stopTypes = new ArrayList<>();
        stopTypes.add(Node.NodeType.BUS);
        genericDetectCell(graph, stopTypes, exclusionTypes, true, allocatedNodes);

        // ****************EXTERN AND SHUNT CELLS******
        stopTypes.add(Node.NodeType.FEEDER);
        genericDetectCell(graph, stopTypes, new ArrayList<>(), false, allocatedNodes);
        for (ExternCell cell : graph.getCells().stream()

                .filter(cell -> cell instanceof ExternCell)
                .map(ExternCell.class::cast)
                .collect(Collectors.toList())) {

            //*****************EXTERN CELL
            if (!isPureExternCell(graph, cell)) {
                //*****************SHUNT CELL
                //in that case the cell is splitted into 2 EXTERN Cells and 1 SHUNT CELL
                detectAndTypeShunt(graph, cell);
            }
        }
        graph.getCells().forEach(Cell::getFullId);

        graph.logCellDetectionStatus();
    }

    private void cleaning(Graph graph) {
        graph.substituteFictitiousNodesMirroringBusNodes();
        if (removeUnnecessaryFictitiousNodes) {
            graph.removeUnnecessaryFictitiousNodes();
        }
        graph.extendFeederWithMultipleSwitches();
        if (substituteSingularFictitiousByFeederNode) {
            graph.substituteSingularFictitiousByFeederNode();
        }
        graph.extendFirstOutsideNode();
        graph.extendBreakerConnectedToBus();
        graph.extendFeederConnectedToBus();
    }

    /**
     * @param typeStops      is the types of node that stops the exploration
     * @param exclusionTypes is the types when reached considers the exploration unsuccessful
     * @param isCellIntern   when the exploration is for the identification of internCell enables to instanciate InternCell class instead of Cell
     * @param allocatedNodes is the list of nodes already allocated to a cell.
     **/
    private void genericDetectCell(Graph graph,
                                   List<Node.NodeType> typeStops,
                                   List<Node.NodeType> exclusionTypes,
                                   boolean isCellIntern,
                                   List<Node> allocatedNodes) {
        graph.getNodeBuses().forEach(bus -> bus.getAdjacentNodes().forEach(adj -> {
            List<Node> cellNodes = new ArrayList<>();
            List<Node> visitedNodes = new ArrayList<>(allocatedNodes);
            visitedNodes.add(bus);
            boolean searchOK = rDelimitedExploration(adj, typeStops, exclusionTypes, cellNodes, visitedNodes);
            if (searchOK && !cellNodes.isEmpty()) {
                cellNodes.add(adj);
                cellNodes.add(bus);
                Cell cell = isCellIntern ? new InternCell(graph) : new ExternCell(graph);
                cell.setNodes(cellNodes);
                allocatedNodes.addAll(cellNodes);
                // remove the BusNodes from allocatedNode for a BusNode can be part of many cells
                allocatedNodes.removeAll(
                        cellNodes.stream()
                                .filter(node -> node.getType() == Node.NodeType.BUS)
                                .collect(Collectors.toList()));
            }
        }));
    }

    /**
     * @param node           the starting point for the exploration
     * @param typeStops      is the types of node that stops the exploration
     * @param exclusionTypes is the types when reached considers the exploration unsuccessful
     * @param nodesResult    the resulting list of nodes
     * @param exploredNodes  nodes already visited
     * @return true if no exclusionType found
     **/
    private boolean rDelimitedExploration(Node node,
                                          List<Node.NodeType> typeStops,
                                          List<Node.NodeType> exclusionTypes,
                                          List<Node> nodesResult,
                                          List<Node> exploredNodes) {

        if (exploredNodes.contains(node)) {
            return true;
        }
        exploredNodes.add(node);
        // the node match the pattern if all the branches from its adjacent nodes reaches a typeStop node without reaching an exclusionTypes node
        List<Node> nodesToVisit = new ArrayList<>(node.getAdjacentNodes());
        nodesToVisit.removeAll(exploredNodes);
        if (nodesToVisit.isEmpty()) {
            return true;
        }
        for (Node n : nodesToVisit) {
            if (exclusionTypes.contains(n.getType())) {
                return false;
            } else if (typeStops.contains(n.getType())) {
                nodesResult.add(n);
                exploredNodes.add(n);
            } else if (rDelimitedExploration(n, typeStops, exclusionTypes, nodesResult,
                    exploredNodes)) {
                nodesResult.add(n);
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the cell is a pure extern and return true in that case, else false (suspected shunt)
     *
     * @param cell : the cell to analyse
     **/
    private boolean isPureExternCell(Graph graph, ExternCell cell) {
        /*Explore the graph of the candidate cell. Remove successively one node, assess if it splits the graph into n>1 branches
        if so, then check if each component is exclusively reaching FEEDER or exclusively reaching BUS
        And verify you have at least one of them
        Return true in that case else false meaning there is one shunt
        */
        for (Node n : cell.getNodes()) {
            List<Node> nodes = new ArrayList<>(cell.getNodes());
            nodes.remove(n);
            List<List<Node>> connexComponents = graph.getConnexComponents(nodes);
            if (checkExternComponents(connexComponents)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param connexComponents components partition to analyse
     * @return true if this partition reflects an extern cell
     */
    private boolean checkExternComponents(List<List<Node>> connexComponents) {
        if (connexComponents.size() > 1) {
            boolean hasDepartBranch = false;
            boolean hasBusBranch = false;
            boolean hasMixBranch = false;
            for (List<Node> nodesConnex : connexComponents) {
                List<Node.NodeType> types = nodesConnex.stream()
                        .map(Node::getType)
                        .distinct().filter(t -> t == Node.NodeType.FEEDER || t == Node.NodeType.BUS)
                        .collect(Collectors.toList());
                if (types.size() == 2) {
                    hasMixBranch = true;
                } else if (types.isEmpty()) {
                    return false;
                } else if (types.get(0).equals(Node.NodeType.FEEDER)) {
                    hasDepartBranch = true;
                } else {
                    hasBusBranch = true;
                }
            }
            return hasBusBranch && hasDepartBranch && !hasMixBranch;
        }
        return false;
    }

    /**
     * @param cell the nodes of a cell that is suppected to be a shunt
     **/
    private void detectAndTypeShunt(Graph graph, Cell cell) {

        List<Node> externalNodes = graph.getNodes()
                .stream()
                .filter(node -> !cell.getNodes().contains(node))
                .collect(Collectors.toList());

        for (Node n : cell.getNodes().stream()
                .filter(n -> n.getAdjacentNodes().size() > 2).collect(Collectors.toList())) {
            // optimisation : a Shunt node has necessarily 3 ore more adjacent nodes

            List<Node> cellNodesExtern1 = checkCandidateShuntNode(n, externalNodes);
            if (cellNodesExtern1 != null) {
                // create the 1st new external cell
                cell.removeAllNodes(cellNodesExtern1.stream()
                        .filter(m -> !m.getType().equals(Node.NodeType.BUS))
                        .collect(Collectors.toList()));
                n.setType(Node.NodeType.SHUNT);
                ExternCell newExternCell = new ExternCell(graph);
                cellNodesExtern1.add(n);
                newExternCell.setNodes(cellNodesExtern1);

                //create the shunt cell

                ShuntCell shuntCell = createShuntCell(graph, n, cellNodesExtern1);

                // create the 2nd external cell
                List<Node> cellNodesExtern2 = cell.getNodes().stream()
                        .filter(node -> (!cellNodesExtern1.contains(node) || node.getType() == Node.NodeType.BUS)
                                && (!shuntCell.getNodes().contains(node) || node.getType() == Node.NodeType.SHUNT))
                        .collect(Collectors.toList());

                cellNodesExtern2.removeAll(cellNodesExtern2.stream()
                        .filter(node -> node.getType() == Node.NodeType.BUS
                                && node.getAdjacentNodes().stream().noneMatch(
                                cellNodesExtern2::contains))
                        .collect(Collectors.toList()));

                ExternCell newExternCell2 = new ExternCell(graph);
                newExternCell2.setNodes(cellNodesExtern2);

                graph.removeCell(cell);
// TODO                shuntCell.setBridgingCellsFromShuntNodes();
                break;
            }
        }
    }

    private List<Node> checkCandidateShuntNode(Node n, List<Node> externalNodes) {
        List<Node.NodeType> kindToFilter = Arrays.asList(Node.NodeType.BUS,
                Node.NodeType.FEEDER,
                Node.NodeType.SHUNT);
        /*
        the node n is candidate to be a SHUNT node if there is
        (i) at least one branch exclusively reaching BUSes
        (ii) at least one branch exclusively reaching DEPARTs
        (iii) at least one branch reaching BUSes and DEPARTs (this branch would be a Shunt)
        In that case, the BUSes branches and DEPARTs Branches constitute an EXTERN Cell,
        and returned in the cellNodesExtern
         */

        List<Node> visitedNodes = new ArrayList<>(externalNodes);
        visitedNodes.add(n); //removal of the node to explore branches from it

        List<Node> cellNodesExtern = new ArrayList<>();
        boolean hasFeederBranch = false;
        boolean hasBusBranch = false;
        boolean hasMixBranch = false;

        List<Node> adjList = new ArrayList<>(n.getAdjacentNodes());
        adjList.removeAll(visitedNodes);
        for (Node adj : adjList) {
            if (!visitedNodes.contains(adj)) {
                List<Node> resultNodes = new ArrayList<>();
                rDelimitedExploration(adj,
                        kindToFilter,
                        new ArrayList<>(),
                        resultNodes,
                        visitedNodes);
                resultNodes.add(adj);

                List<Node.NodeType> types = resultNodes.stream() // what are the types of terminal node of the branch
                        .map(Node::getType)
                        .distinct().filter(kindToFilter::contains)
                        .collect(Collectors.toList());

                if (types.size() > 1) {
                    hasMixBranch = true;
                } else {
                    hasBusBranch |= types.get(0).equals(Node.NodeType.BUS);
                    hasFeederBranch |= types.get(0).equals(Node.NodeType.FEEDER);

                    if (types.get(0).equals(Node.NodeType.BUS) || types.get(0).equals(Node.NodeType.FEEDER)) {
                        cellNodesExtern.addAll(resultNodes);
                    }
                }
                visitedNodes.removeAll(resultNodes
                        .stream()
                        .filter(m -> m.getType().equals(Node.NodeType.BUS))
                        .collect(Collectors.toList()));

            }
        }
        return (hasBusBranch && hasFeederBranch && hasMixBranch) ? cellNodesExtern : null;
    }

    private ShuntCell createShuntCell(Graph graph, Node n, List<Node> cellNodesExtern1) {
        List<Node> shuntCellNodes = new ArrayList<>();
        shuntCellNodes.add(n);
        Node currentNode = n.getAdjacentNodes().stream()
                .filter(node -> !cellNodesExtern1.contains(node))
                .findAny().orElse(null);
        if (currentNode != null) {
            while (currentNode.getAdjacentNodes().size() == 2) {
                shuntCellNodes.add(currentNode);
                currentNode = shuntCellNodes.contains(currentNode.getAdjacentNodes().get(0))
                        ? currentNode.getAdjacentNodes().get(1) : currentNode.getAdjacentNodes().get(0);
            }
            shuntCellNodes.add(currentNode);
            currentNode.setType(Node.NodeType.SHUNT);
        }
        ShuntCell shuntCell = new ShuntCell(graph); // the shunt branch is made of the remaining cells + the actual node n
        shuntCell.setNodes(shuntCellNodes);
        return shuntCell;
    }

}

