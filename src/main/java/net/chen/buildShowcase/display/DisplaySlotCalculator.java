package net.chen.buildShowcase.display;

import com.sk89q.worldedit.math.BlockVector3;

public class DisplaySlotCalculator {

    /**
     * 根据建筑索引计算展示位置
     * @param origin 起始坐标
     * @param index 第几个建筑
     * @param spacing 间距
     * @param maxPerRow 每行最大数量
     */
    public static BlockVector3 calculate(BlockVector3 origin, int index, int spacing, int maxPerRow) {
        int row = index / maxPerRow;
        int col = index % maxPerRow;

        return origin.add(col * spacing, 0, row * spacing);
    }
}
