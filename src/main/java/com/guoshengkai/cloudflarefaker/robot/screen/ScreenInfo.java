package com.guoshengkai.cloudflarefaker.robot.screen;

import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 屏幕信息类
 * 用于存储和管理显示器的各种属性和信息
 * @author gsk
 */
@Getter
@Setter
public class ScreenInfo {
    /**
     * 显示器序号
     */
    private int index;
    /**
     * 显示器ID
     */
    private String id;
    /**
     * 显示器名称
     */
    private String name;
    /**
     * 位置和尺寸
     */
    private Rectangle bounds;
    /**
     * 宽度（分辨率）
     */
    private int width;
    /**
     *  高度（分辨率）
     */
    private int height;
    /**
     * 刷新率
     */
    private int refreshRate;
    /**
     * 色彩深度
     */
    private int bitDepth;
    /**
     * 截图
     */
    private BufferedImage screenshot;
    /**
     * 是否主显示器
     */
    private boolean isPrimary;
    /**
     * 缩放因子（DPI）
     */
    private double scaleFactor;

    @Override
    public String toString() {
        return String.format(
                "显示器 %d:\n" +
                        "  ID: %s\n" +
                        "  名称: %s\n" +
                        "  位置: (%d, %d)\n" +
                        "  分辨率: %d x %d\n" +
                        "  刷新率: %d Hz\n" +
                        "  色彩深度: %d bit\n" +
                        "  缩放因子: %.2f\n" +
                        "  主显示器: %s\n",
                index, id, name, bounds.x, bounds.y, width, height,
                refreshRate, bitDepth, scaleFactor, isPrimary ? "是" : "否"
        );
    }

    /**
     * 在当前屏幕截图中查找子图像
     * @param subImg 子图像
     * @param similarity 相似度（0.0 - 1.0）
     * @return 位置对象，如果未找到则返回null
     */
    public Position findImgInScreen(BufferedImage subImg, double similarity) {
        return ImageFinder.findImgSmart(this.screenshot, subImg, similarity);
    }

    /**
     * 在当前屏幕截图中快速查找子图像
     * @param subImg 子图像
     * @param similarity 相似度（0.0 - 1.0）
     * @return 位置对象，如果未找到则返回null
     */
    public Position findImgInScreenFast(BufferedImage subImg, double similarity) {
        return ImageFinder.findImgGray(this.screenshot, subImg, similarity);
    }

}
