package com.guoshengkai.cloudflarefaker.robot.screen;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

/**
 * 屏幕工具类
 */
public class ScreenInfoUtils {

    /**
     * 获取所有屏幕的详细信息
     * @param captureScreenshot 是否截取屏幕截图
     * @return 屏幕信息列表
     */
    public static List<ScreenInfo> getAllScreenInfo(boolean captureScreenshot) {
        List<ScreenInfo> screenInfoList = new ArrayList<>();

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screens = ge.getScreenDevices();
        GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();

        for (int i = 0; i < screens.length; i++) {
            GraphicsDevice screen = screens[i];
            ScreenInfo info = new ScreenInfo();

            // 基本信息
            info.setIndex(i);
            info.setId(screen.getIDstring());
            info.setName(getScreenName(screen));
            info.setPrimary(screen.equals(defaultScreen));

            // 获取配置
            GraphicsConfiguration gc = screen.getDefaultConfiguration();
            Rectangle bounds = gc.getBounds();
            info.setBounds(bounds);
            info.setWidth(bounds.width);
            info.setHeight(bounds.height);

            // 显示模式信息
            DisplayMode dm = screen.getDisplayMode();
            info.setRefreshRate(dm.getRefreshRate());
            info.setBitDepth(dm.getBitDepth());

            // 获取缩放因子
            info.setScaleFactor(getScaleFactor(gc));

            // 截图（如果需要）
            if (captureScreenshot) {
                try {
                    BufferedImage screenshot = captureScreen(screen);
                    info.setScreenshot(screenshot);
                } catch (AWTException e) {
                    e.printStackTrace();
                }
            }

            screenInfoList.add(info);
        }

        return screenInfoList;
    }

    /**
     * 获取屏幕名称（尝试获取更友好的名称）
     */
    private static String getScreenName(GraphicsDevice screen) {
        String id = screen.getIDstring();
        // 尝试解析更友好的名称
        if (id.contains("\\")) {
            String[] parts = id.split("\\\\");
            if (parts.length > 0) {
                return parts[parts.length - 1];
            }
        }
        return id;
    }

    /**
     * 获取屏幕缩放因子（DPI相关）
     */
    private static double getScaleFactor(GraphicsConfiguration gc) {
        AffineTransform transform = gc.getDefaultTransform();
        return transform.getScaleX(); // 通常X和Y缩放相同
    }

    /**
     * 截取指定屏幕的截图
     */
    private static BufferedImage captureScreen(GraphicsDevice screen) throws AWTException {
        Rectangle bounds = screen.getDefaultConfiguration().getBounds();
        Robot robot = new Robot(screen);
        return robot.createScreenCapture(bounds);
    }

    /**
     * 保存屏幕截图到文件
     */
    public static void saveScreenshot(BufferedImage image, String filename) {
        try {
            File file = new File(filename);
            ImageIO.write(image, "png", file);
            System.out.println("截图已保存: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取屏幕上指定位置的颜色
     */
    public static Color getPixelColor(int screenIndex, int x, int y) {
        try {
            GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            if (screenIndex >= 0 && screenIndex < screens.length) {
                Robot robot = new Robot(screens[screenIndex]);
                return robot.getPixelColor(x, y);
            }
        } catch (AWTException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取屏幕指定区域的颜色信息
     */
    public static ColorInfo analyzeColors(BufferedImage image, Rectangle region) {
        if (region == null) {
            region = new Rectangle(0, 0, image.getWidth(), image.getHeight());
        }

        ColorInfo colorInfo = new ColorInfo();
        int totalPixels = 0;
        long totalR = 0, totalG = 0, totalB = 0;

        int minX = Math.max(0, region.x);
        int minY = Math.max(0, region.y);
        int maxX = Math.min(image.getWidth(), region.x + region.width);
        int maxY = Math.min(image.getHeight(), region.y + region.height);

        for (int x = minX; x < maxX; x += 10) { // 采样，每10个像素取一个
            for (int y = minY; y < maxY; y += 10) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb);

                totalR += color.getRed();
                totalG += color.getGreen();
                totalB += color.getBlue();
                totalPixels++;

                // 统计主要颜色
                colorInfo.addColor(color);
            }
        }

        // 计算平均颜色
        if (totalPixels > 0) {
            colorInfo.setAverageColor(new Color(
                    (int)(totalR / totalPixels),
                    (int)(totalG / totalPixels),
                    (int)(totalB / totalPixels)
            ));
        }

        return colorInfo;
    }

    /**
     * 获取所有支持的显示模式
     */
    public static List<DisplayMode> getSupportedDisplayModes(int screenIndex) {
        List<DisplayMode> modes = new ArrayList<>();
        GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

        if (screenIndex >= 0 && screenIndex < screens.length) {
            DisplayMode[] supportedModes = screens[screenIndex].getDisplayModes();
            for (DisplayMode mode : supportedModes) {
                modes.add(mode);
            }
        }

        return modes;
    }

    /**
     * 打印所有屏幕的详细信息
     */
    public static void printAllScreenInfo() {
        List<ScreenInfo> screens = getAllScreenInfo(false);

        System.out.println("===== 系统屏幕信息 =====");
        System.out.println("显示器总数: " + screens.size());
        System.out.println();

        for (ScreenInfo screen : screens) {
            System.out.println(screen);

            // 打印支持的显示模式
            List<DisplayMode> modes = getSupportedDisplayModes(screen.getIndex());
            System.out.println("  支持的显示模式:");
            for (DisplayMode mode : modes) {
                System.out.printf("    %d x %d @ %d Hz, %d bit\n",
                        mode.getWidth(), mode.getHeight(),
                        mode.getRefreshRate(), mode.getBitDepth());
            }
            System.out.println();
        }
    }

    /**
     * 使用示例
     */
    public static void main(String[] args) {
        // 1. 获取所有屏幕信息（包括截图）
        List<ScreenInfo> screens = getAllScreenInfo(true);

        // 2. 打印详细信息
        printAllScreenInfo();

        // 3. 保存每个屏幕的截图
        for (ScreenInfo screen : screens) {
            if (screen.getScreenshot() != null) {
                String filename = String.format("screen_%d_%dx%d.png",
                        screen.getIndex(), screen.getWidth(), screen.getHeight());
                saveScreenshot(screen.getScreenshot(), filename);

                // 分析屏幕颜色
                ColorInfo colorInfo = analyzeColors(screen.getScreenshot(), null);
                System.out.println("屏幕 " + screen.getIndex() + " 颜色分析:");
                System.out.println("  平均颜色: " + colorInfo.getAverageColor());
                System.out.println("  主要颜色: " + colorInfo.getDominantColors(5));
            }
        }

        // 4. 获取特定位置的颜色
        Color pixelColor = getPixelColor(0, 100, 100);
        if (pixelColor != null) {
            System.out.println("坐标(100,100)的颜色: " + pixelColor);
        }
    }
}
