package com.guoshengkai.cloudflarefaker.robot;

import com.guoshengkai.cloudflarefaker.robot.screen.ScreenInfo;
import com.guoshengkai.cloudflarefaker.robot.screen.ScreenInfoUtils;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ComputerRobot {
    private Robot robot;

    {
        try {
            System.setProperty("java.awt.headless", "false");
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 鼠标曲线且不规则的移动到指定位置, 高度模拟人类操作
     */
    public void mouseMove(int toX, int toY) {
        try {
            Point point = MouseInfo.getPointerInfo().getLocation();
            int fromX = (int) point.getX();
            int fromY = (int) point.getY();

            double distance = Math.sqrt(Math.pow(toX - fromX, 2) + Math.pow(toY - fromY, 2));

            // 距离太短直接移动
            if (distance < 5) {
                robot.mouseMove(toX, toY);
                Thread.sleep((long)(Math.random() * 50 + 50)); // 等待50-100ms
                return;
            }

            // 根据距离决定总时间（大幅缩短，更接近真人速度）
            long totalTime;
            if (distance < 100) {
                totalTime = (long)(Math.random() * 100 + 100); // 100-200ms
            } else if (distance < 300) {
                totalTime = (long)(Math.random() * 150 + 200); // 200-350ms
            } else if (distance < 800) {
                totalTime = (long)(Math.random() * 200 + 300); // 300-500ms
            } else {
                totalTime = (long)(Math.random() * 200 + 400); // 400-600ms
            }

            // 根据距离决定路径点数（减少点数以提高效率）
            int steps = Math.min(Math.max(15, (int)(distance / 15)), 50);

            // 生成控制点用于贝塞尔曲线（更平滑的垂直偏移）
            double[][] controlPoints = generateSmoothControlPoints(fromX, fromY, toX, toY, distance);

            // 生成贝塞尔曲线路径
            List<Point> path = generateBezierPath(fromX, fromY, toX, toY, controlPoints, steps);

            // 添加轻微噪声
            path = addNoiseToPath(path, distance);

            // 100%概率添加过冲效果（2-10像素）
            path = addOvershoot(path, toX, toY);

            // 确保最后一个点是目标点
            path.set(path.size() - 1, new Point(toX, toY));

            // 计算每个点的延迟时间（优化速度曲线）
            long[] delays = calculateFastSpeedCurve(path, totalTime, distance);

            // 执行移动
            for (int i = 0; i < path.size(); i++) {
                Point p = path.get(i);
                robot.mouseMove(p.x, p.y);

                // 偶尔添加极短停顿（1%概率）
                if (Math.random() < 0.01) {
                    Thread.sleep((long)(Math.random() * 5 + 2));
                }

                if (i < delays.length) {
                    Thread.sleep(delays[i]);
                }
            }

            // 10%概率在最终位置做微小震动
            if (Math.random() < 0.1) {
                int microX = toX + (int)(Math.random() * 2 - 1);
                int microY = toY + (int)(Math.random() * 2 - 1);
                robot.mouseMove(microX, microY);
                Thread.sleep(5);
                robot.mouseMove(toX, toY);
            }

            // 移动完毕后等待50-100ms
            Thread.sleep((long)(Math.random() * 50 + 50));

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 模拟鼠标点击
     * @param button 鼠标按钮
     */
    public void mouseClick(int button) {
        robot.mousePress(button);
        try {
            // 按下持续20-50ms
            Thread.sleep((long)(Math.random() * 50 + 20));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        robot.mouseRelease(button);
        try {
            // 松开后等待20-50ms
            Thread.sleep((long)(Math.random() * 50 + 20));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 模拟鼠标左键点击
     */
    public void mouseClick() {
        mouseClick(InputEvent.BUTTON1_DOWN_MASK);
    }

    /**
     * 模拟鼠标右键点击
     */
    public void mouseRightClick() {
        mouseClick(InputEvent.BUTTON3_DOWN_MASK);
    }

    /**
     * 生成更平滑的贝塞尔曲线控制点
     */
    private double[][] generateSmoothControlPoints(int fromX, int fromY, int toX, int toY, double distance) {
        // 根据距离决定控制点数量
        int numControl;
        if (distance < 50) {
            numControl = 1;
        } else if (distance < 200) {
            numControl = 2;
        } else {
            numControl = 3;
        }

        double[][] controlPoints = new double[numControl][2];

        double angle = Math.atan2(toY - fromY, toX - fromX);
        double perpendicular = angle + Math.PI / 2;

        for (int i = 0; i < numControl; i++) {
            double t = (i + 1.0) / (numControl + 1);
            double baseX = fromX + (toX - fromX) * t;
            double baseY = fromY + (toY - fromY) * t;

            // 垂直偏移按比例，但不超过±10%（小范围移动时偏移更小）
            double maxOffset = Math.min(distance * 0.1, 100); // 最大偏移不超过30像素
            double minOffset = Math.min(distance * 0.05, 10);  // 最小偏移

            // 使用正弦函数创建更平滑的偏移
            double offsetFactor = Math.sin(t * Math.PI); // 中间偏移大，两端偏移小
            double offset = (Math.random() - 0.5) * 2 * (minOffset + (maxOffset - minOffset) * offsetFactor);

            controlPoints[i][0] = baseX + Math.cos(perpendicular) * offset;
            controlPoints[i][1] = baseY + Math.sin(perpendicular) * offset;
        }

        return controlPoints;
    }

    /**
     * 为路径添加轻微的随机噪声
     */
    private List<Point> addNoiseToPath(List<Point> path, double distance) {
        List<Point> noisyPath = new ArrayList<>();

        for (int i = 0; i < path.size(); i++) {
            Point p = path.get(i);
            double progress = (double) i / (path.size() - 1);

            // 根据距离调整噪声强度（距离越小噪声越小）
            double baseIntensity = Math.min(distance * 0.008, 1.5);

            // 开始和结束时噪声更小
            double intensity;
            if (progress < 0.1 || progress > 0.9) {
                intensity = baseIntensity * 0.2;
            } else {
                intensity = baseIntensity;
            }

            // 使用高斯分布生成噪声
            double noiseX = randomGaussian() * intensity;
            double noiseY = randomGaussian() * intensity;

            noisyPath.add(new Point(
                    (int)(p.x + noiseX),
                    (int)(p.y + noiseY)
            ));
        }

        return noisyPath;
    }

    /**
     * 添加过冲效果（100%概率，2-10像素）
     */
    private List<Point> addOvershoot(List<Point> path, int targetX, int targetY) {
        if (path.size() < 2) {
            return path;
        }

        // 计算最后的移动方向
        Point lastPoint = path.get(path.size() - 1);
        Point secondLastPoint = path.get(path.size() - 2);
        double dirX = lastPoint.x - secondLastPoint.x;
        double dirY = lastPoint.y - secondLastPoint.y;
        double dirLength = Math.sqrt(dirX * dirX + dirY * dirY);

        if (dirLength > 0) {
            dirX /= dirLength;
            dirY /= dirLength;

            // 过冲距离（2-10像素）
            double overshootDistance = Math.random() * 8 + 2;
            int overshootX = (int)(targetX + dirX * overshootDistance);
            int overshootY = (int)(targetY + dirY * overshootDistance);

            // 移除最后几个点
            List<Point> newPath = new ArrayList<>(path.subList(0, Math.max(1, path.size() - 2)));

            // 快速过冲（减少点数）
            for (int i = 0; i <= 3; i++) {
                double t = i / 3.0;
                double easedT = t * t * (3 - 2 * t);
                int x = (int)(targetX + (overshootX - targetX) * easedT);
                int y = (int)(targetY + (overshootY - targetY) * easedT);
                newPath.add(new Point(x, y));
            }

            // 快速返回到目标点
            for (int i = 1; i <= 2; i++) {
                double t = i / 2.0;
                double easedT = 1 - Math.pow(1 - t, 2);
                int x = (int)(overshootX + (targetX - overshootX) * easedT);
                int y = (int)(overshootY + (targetY - overshootY) * easedT);
                newPath.add(new Point(x, y));
            }

            return newPath;
        }

        return path;
    }

    /**
     * 计算更快的速度曲线（真人操作速度）
     */
    private long[] calculateFastSpeedCurve(List<Point> path, long totalTime, double totalDistance) {
        long[] delays = new long[path.size()];
        double[] speedFactors = new double[path.size()];

        // 定义减速区域（最后30-100像素）
        double slowdownDistance = Math.min(Math.max(30, totalDistance * 0.15), 100);

        for (int i = 0; i < path.size(); i++) {
            Point currentPoint = path.get(i);
            Point endPoint = path.get(path.size() - 1);
            double distanceToEnd = Math.sqrt(
                    Math.pow(endPoint.x - currentPoint.x, 2) +
                            Math.pow(endPoint.y - currentPoint.y, 2)
            );

            double speedFactor;
            double progress = (double) i / (path.size() - 1);

            if (distanceToEnd <= slowdownDistance) {
                // 在减速区域内，快速线性减速
                double slowdownProgress = 1 - (distanceToEnd / slowdownDistance);
                // 速度从1.0快速降到0.3
                speedFactor = 1.0 - (0.7 * Math.pow(slowdownProgress, 1.5));
            } else if (progress < 0.1) {
                // 开始10%快速加速
                speedFactor = 0.5 + (0.5 * (progress / 0.1));
            } else {
                // 中间大部分路程保持高速
                speedFactor = 1.0 + (Math.random() - 0.5) * 0.2; // 高速且有轻微波动
            }

            // 减少随机变化范围（±5%）
            speedFactor *= (0.95 + Math.random() * 0.1);
            speedFactors[i] = Math.max(0.1, speedFactor);
        }

        // 归一化速度因子以匹配总时间
        double totalFactor = 0;
        for (double factor : speedFactors) {
            totalFactor += factor;
        }

        // 计算延迟并确保最小延迟更短
        for (int i = 0; i < path.size(); i++) {
            delays[i] = Math.max(0, (long)(totalTime * (speedFactors[i] / totalFactor)));
        }

        return delays;
    }

    /**
     * 生成贝塞尔曲线路径
     */
    private List<Point> generateBezierPath(int fromX, int fromY, int toX, int toY,
                                                  double[][] controlPoints, int steps) {
        List<Point> path = new ArrayList<>();

        // 构建完整的控制点列表
        List<double[]> allPoints = new ArrayList<>();
        allPoints.add(new double[]{fromX, fromY});
        for (double[] cp : controlPoints) {
            allPoints.add(cp);
        }
        allPoints.add(new double[]{toX, toY});

        // 生成贝塞尔曲线上的点
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double[] point = calculateBezierPoint(allPoints, t);
            path.add(new Point((int)point[0], (int)point[1]));
        }

        return path;
    }

    /**
     * 计算贝塞尔曲线上的点
     */
    private double[] calculateBezierPoint(List<double[]> points, double t) {
        int n = points.size() - 1;
        double x = 0, y = 0;

        for (int i = 0; i <= n; i++) {
            double coefficient = binomialCoefficient(n, i) * Math.pow(1 - t, n - i) * Math.pow(t, i);
            x += coefficient * points.get(i)[0];
            y += coefficient * points.get(i)[1];
        }

        return new double[]{x, y};
    }

    /**
     * 计算二项式系数
     */
    private long binomialCoefficient(int n, int k) {
        if (k > n - k) {
            k = n - k;
        }
        long result = 1;
        for (int i = 0; i < k; i++) {
            result *= (n - i);
            result /= (i + 1);
        }
        return result;
    }

    /**
     * 生成高斯分布随机数
     */
    private double randomGaussian() {
        double u1 = Math.random();
        double u2 = Math.random();
        return Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
    }

    public List<ScreenInfo> getAllScreenInfo() {
        return ScreenInfoUtils.getAllScreenInfo(true);
    }

    private String imageToBase64Url(BufferedImage image) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
            return "data:image/png;base64," + base64;
        } catch (IOException e) {
            throw new RuntimeException("图片转换Base64失败", e);
        }
    }

    public void sleep(int ms) {
        sleep(ms, false);
    }

    public void sleep(int ms, boolean moveMouse) {
        if (!moveMouse){
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        // 在指定时间内, 鼠标随机线性微动, 模拟人类操作, 横纵正负10~100像素, 每次移动1500~2000ms
        long endTime = System.currentTimeMillis() + ms;
        Point startPoint = MouseInfo.getPointerInfo().getLocation();
        while (System.currentTimeMillis() < endTime) {
            int offsetX = (int) (Math.random() * 200 - 100);
            int offsetY = (int) (Math.random() * 200 - 100);
            int targetX = (int) startPoint.getX() + offsetX;
            int targetY = (int) startPoint.getY() + offsetY;
            mouseMove(targetX, targetY);
            try {
                Thread.sleep((long) (Math.random() * 500 + 1500));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
