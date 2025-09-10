package com.guoshengkai.cloudflarefaker.robot.screen;

import lombok.Getter;
import lombok.Setter;

/**
 * 坐标类
 * 用于表示屏幕上的一个点的坐标
 * @author gsk
 */
@Getter
@Setter
public class Position {
    private int x;
    private int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return String.format("Position(%d, %d)", x, y);
    }
}
