package com.guoshengkai.cloudflarefaker.robot.screen;

import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.List;
/**
 * 颜色信息类
 * 用于存储和处理颜色相关的信息
 * @author gsk
 */
public class ColorInfo {
    @Setter
    @Getter
    private Color averageColor;
    private final java.util.Map<Integer, Integer> colorCount = new java.util.HashMap<>();

    public void addColor(Color color) {
        int rgb = color.getRGB();
        colorCount.put(rgb, colorCount.getOrDefault(rgb, 0) + 1);
    }

    public List<Color> getDominantColors(int count) {
        return colorCount.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(count)
                .map(e -> new Color(e.getKey()))
                .collect(java.util.stream.Collectors.toList());
    }
}
