package com.guoshengkai.cloudflarefaker.robot.screen;

import lombok.Getter;
import lombok.Setter;

/**
 * 坐标类
 * 用于表示屏幕上的一个点的坐标
 * @author gsk
 */
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

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
