/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.powsybl.substationdiagram.layout.LayoutParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LegParralelBlock extends AbstractParallelBlock implements LegBlock {

    public LegParralelBlock(List<Block> subBlocks, Cell cell, boolean allowMerge) {
        super(subBlocks, cell, allowMerge);
    }

    @Override
    public List<BusNode> getBusNodes() {
        return subBlocks.stream().map(b -> ((LegPrimaryBlock) b).getBusNode()).collect(Collectors.toList());
    }

    @Override
    public void sizing() {
        subBlocks.forEach(Block::sizing);
        if (getPosition().getOrientation() == Orientation.VERTICAL) {
            getPosition().setVSpan(0);
            List<Block> subBlocksCopy = new ArrayList<>(subBlocks);
            int h = 0;
            while (!subBlocksCopy.isEmpty()) {
                Block b = subBlocksCopy.get(0);
                b.getPosition().setHV(h, 0);
                if (!((LegPrimaryBlock) b).getStackableBlocks().isEmpty()) {
                    final int finalH = h;
                    ((LegPrimaryBlock) b).getStackableBlocks().forEach(sb -> sb.getPosition().setHV(finalH, 0));
                    h++;
                    subBlocksCopy.removeAll(((LegPrimaryBlock) b).getStackableBlocks());
                } else {
                    h += b.getPosition().getHSpan();
                }
                subBlocksCopy.remove(b);
            }
            getPosition().setHSpan(h);
        }
        // case HORIZONTAL cannot happen
    }

    @Override
    double initX0() {
        return getCoord().getX()
                + (getPosition().getHSpan() == 1 ? 0 : getCoord().getXSpan() / 2);
    }

    @Override
    double intitXStep() {
        return getPosition().getHSpan() == 1 ? 0 : getCoord().getXSpan() / getPosition().getHSpan();
    }

    @Override
    public void coordHorizontalCase(LayoutParameters layoutParam) {
        // case HORIZONTAL cannot happen
    }

    @Override
    public String toString() {
        return "BodyParallelBlock(subBlocks=" + subBlocks + ")";
    }
}
