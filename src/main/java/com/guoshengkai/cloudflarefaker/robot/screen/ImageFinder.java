package com.guoshengkai.cloudflarefaker.robot.screen;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacv.Java2DFrameUtils;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_features2d.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_features2d.*;
import static org.bytedeco.opencv.global.opencv_calib3d.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于JavaCV的图片查找工具类
 * @author gsk
 */
public class ImageFinder {

    /**
     * BufferedImage转换为OpenCV的Mat
     */
    private static Mat bufferedImageToMat(BufferedImage image) {
        BufferedImage convertedImg = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_3BYTE_BGR
        );
        convertedImg.getGraphics().drawImage(image, 0, 0, null);

        Mat mat = Java2DFrameUtils.toMat(convertedImg);

        if (mat.depth() != CV_8U) {
            Mat mat8U = new Mat();
            mat.convertTo(mat8U, CV_8U);
            mat.release();
            return mat8U;
        }

        return mat;
    }

    /**
     * 多尺度模板匹配 - 支持大小不一致的情况
     */
    public static Position findImgMultiScale(BufferedImage source, BufferedImage target, double similarityScore) {
        Mat sourceMat = bufferedImageToMat(source);
        Mat targetMat = bufferedImageToMat(target);

        // 转换为灰度图
        Mat sourceGray = new Mat();
        Mat targetGray = new Mat();
        cvtColor(sourceMat, sourceGray, COLOR_BGR2GRAY);
        cvtColor(targetMat, targetGray, COLOR_BGR2GRAY);

        Position bestMatch = null;
        double bestScore = 0;
        double bestScale = 1.0;

        // 尝试不同的缩放比例
        double[] scales = {0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5};

        for (double scale : scales) {
            // 缩放目标图像
            int newWidth = (int)(targetGray.cols() * scale);
            int newHeight = (int)(targetGray.rows() * scale);

            if (newWidth > sourceGray.cols() || newHeight > sourceGray.rows()) {
                continue;
            }

            Mat scaledTarget = new Mat();
            Size newSize = new Size(newWidth, newHeight);
            resize(targetGray, scaledTarget, newSize, 0, 0, INTER_LINEAR);

            // 执行模板匹配
            Mat result = new Mat();
            matchTemplate(sourceGray, scaledTarget, result, CV_TM_CCOEFF_NORMED);

            // 查找最佳匹配
            DoublePointer minVal = new DoublePointer(1);
            DoublePointer maxVal = new DoublePointer(1);
            Point minLoc = new Point();
            Point maxLoc = new Point();

            minMaxLoc(result, minVal, maxVal, minLoc, maxLoc, null);

            double score = maxVal.get();

            if (score > bestScore && score >= similarityScore) {
                bestScore = score;
                bestMatch = new Position(maxLoc.x(), maxLoc.y());
                bestScale = scale;
            }

            // 释放资源
            scaledTarget.release();
            result.release();
            minVal.deallocate();
            maxVal.deallocate();
            minLoc.deallocate();
            maxLoc.deallocate();
        }

        // 释放资源
        sourceMat.release();
        targetMat.release();
        sourceGray.release();
        targetGray.release();

        if (bestMatch != null) {
            System.out.println("找到匹配，缩放比例: " + bestScale + ", 相似度: " + bestScore);
        }

        return bestMatch;
    }

    /**
     * 带旋转的模板匹配 - 支持角度不一致的情况
     */
    public static Position findImgWithRotation(BufferedImage source, BufferedImage target, double similarityScore) {
        Mat sourceMat = bufferedImageToMat(source);
        Mat targetMat = bufferedImageToMat(target);

        // 转换为灰度图
        Mat sourceGray = new Mat();
        Mat targetGray = new Mat();
        cvtColor(sourceMat, sourceGray, COLOR_BGR2GRAY);
        cvtColor(targetMat, targetGray, COLOR_BGR2GRAY);

        Position bestMatch = null;
        double bestScore = 0;
        double bestAngle = 0;

        // 尝试不同的旋转角度（-30到30度，每5度一个步进）
        for (int angle = -30; angle <= 30; angle += 5) {
            // 计算旋转矩阵
            Point2f center = new Point2f(targetGray.cols() / 2f, targetGray.rows() / 2f);
            Mat rotMatrix = getRotationMatrix2D(center, angle, 1.0);

            // 旋转目标图像
            Mat rotatedTarget = new Mat();
            warpAffine(targetGray, rotatedTarget, rotMatrix, targetGray.size());

            // 执行模板匹配
            if (rotatedTarget.cols() <= sourceGray.cols() && rotatedTarget.rows() <= sourceGray.rows()) {
                Mat result = new Mat();
                matchTemplate(sourceGray, rotatedTarget, result, CV_TM_CCOEFF_NORMED);

                // 查找最佳匹配
                DoublePointer minVal = new DoublePointer(1);
                DoublePointer maxVal = new DoublePointer(1);
                Point minLoc = new Point();
                Point maxLoc = new Point();

                minMaxLoc(result, minVal, maxVal, minLoc, maxLoc, null);

                double score = maxVal.get();

                if (score > bestScore && score >= similarityScore) {
                    bestScore = score;
                    bestMatch = new Position(maxLoc.x(), maxLoc.y());
                    bestAngle = angle;
                }

                // 释放资源
                result.release();
                minVal.deallocate();
                maxVal.deallocate();
                minLoc.deallocate();
                maxLoc.deallocate();
            }

            // 释放资源
            rotMatrix.release();
            rotatedTarget.release();
            center.deallocate();
        }

        // 释放资源
        sourceMat.release();
        targetMat.release();
        sourceGray.release();
        targetGray.release();

        if (bestMatch != null) {
            System.out.println("找到匹配，旋转角度: " + bestAngle + "度, 相似度: " + bestScore);
        }

        return bestMatch;
    }

    /**
     * 使用SIFT特征匹配 - 最鲁棒但速度较慢
     */
    public static Position findImgBySIFT(BufferedImage source, BufferedImage target, double minMatches) {
        Mat sourceMat = bufferedImageToMat(source);
        Mat targetMat = bufferedImageToMat(target);

        // 使用SIFT检测器（如果SIFT不可用，可以使用ORB）
        try (SIFT sift = SIFT.create()) {
            // 检测关键点和描述符
            KeyPointVector sourceKeypoints = new KeyPointVector();
            KeyPointVector targetKeypoints = new KeyPointVector();
            Mat sourceDescriptors = new Mat();
            Mat targetDescriptors = new Mat();

            sift.detectAndCompute(sourceMat, new Mat(), sourceKeypoints, sourceDescriptors);
            sift.detectAndCompute(targetMat, new Mat(), targetKeypoints, targetDescriptors);

            // 使用FLANN匹配器
            try (FlannBasedMatcher matcher = new FlannBasedMatcher()) {
                DMatchVectorVector matches = new DMatchVectorVector();
                matcher.knnMatch(targetDescriptors, sourceDescriptors, matches, 2);

                // 使用Lowe's ratio test筛选好的匹配
                List<DMatch> goodMatches = new ArrayList<>();
                for (long i = 0; i < matches.size(); i++) {
                    DMatchVector match = matches.get(i);
                    if (match.size() >= 2) {
                        DMatch first = match.get(0);
                        DMatch second = match.get(1);
                        if (first.distance() < 0.7f * second.distance()) {
                            goodMatches.add(first);
                        }
                    }
                }

                System.out.println("找到 " + goodMatches.size() + " 个好的特征匹配");

                // 如果有足够的匹配点，计算目标位置
                if (goodMatches.size() >= minMatches) {
                    // 计算匹配点的中心位置
                    double sumX = 0, sumY = 0;
                    int count = 0;

                    for (DMatch match : goodMatches) {
                        Point2f sourcePt = sourceKeypoints.get(match.trainIdx()).pt();
                        sumX += sourcePt.x();
                        sumY += sourcePt.y();
                        count++;
                    }

                    if (count > 0) {
                        int x = (int)(sumX / count - target.getWidth() / 2);
                        int y = (int)(sumY / count - target.getHeight() / 2);

                        // 释放资源
                        sourceKeypoints.deallocate();
                        targetKeypoints.deallocate();
                        sourceDescriptors.release();
                        targetDescriptors.release();
                        matches.deallocate();
                        sourceMat.release();
                        targetMat.release();

                        return new Position(x, y);
                    }
                }

                // 释放资源
                sourceKeypoints.deallocate();
                targetKeypoints.deallocate();
                sourceDescriptors.release();
                targetDescriptors.release();
                matches.deallocate();
            }
        } catch (Exception e) {
            System.err.println("SIFT不可用，尝试使用ORB特征");
            return findImgByORB(source, target, minMatches);
        }

        sourceMat.release();
        targetMat.release();

        return null;
    }

    /**
     * 使用ORB特征匹配（SIFT的免费替代）
     */
    public static Position findImgByORB(BufferedImage source, BufferedImage target, double minMatches) {
        Mat sourceMat = bufferedImageToMat(source);
        Mat targetMat = bufferedImageToMat(target);

        try (ORB orb = ORB.create()) {  // 增加特征点数量
            // 检测关键点和描述符
            KeyPointVector sourceKeypoints = new KeyPointVector();
            KeyPointVector targetKeypoints = new KeyPointVector();
            Mat sourceDescriptors = new Mat();
            Mat targetDescriptors = new Mat();

            orb.detectAndCompute(sourceMat, new Mat(), sourceKeypoints, sourceDescriptors);
            orb.detectAndCompute(targetMat, new Mat(), targetKeypoints, targetDescriptors);

            // 使用BFMatcher
            try (BFMatcher matcher = new BFMatcher(NORM_HAMMING, true)) {
                DMatchVector matches = new DMatchVector();
                matcher.match(targetDescriptors, sourceDescriptors, matches);

                // 筛选好的匹配
                List<DMatch> goodMatches = new ArrayList<>();
                double maxDist = 0;
                double minDist = 100;

                for (long i = 0; i < matches.size(); i++) {
                    DMatch match = matches.get(i);
                    double dist = match.distance();
                    if (dist < minDist) {
                        minDist = dist;
                    }
                    if (dist > maxDist) {
                        maxDist = dist;
                    }
                }

                double threshold = Math.max(2 * minDist, 30.0);
                for (long i = 0; i < matches.size(); i++) {
                    DMatch match = matches.get(i);
                    if (match.distance() <= threshold) {
                        goodMatches.add(match);
                    }
                }

                System.out.println("ORB找到 " + goodMatches.size() + " 个好的特征匹配");

                // 如果有足够的匹配点
                if (goodMatches.size() >= minMatches) {
                    // 计算匹配点的中心位置
                    double sumX = 0, sumY = 0;
                    int count = 0;

                    for (DMatch match : goodMatches) {
                        Point2f sourcePt = sourceKeypoints.get(match.trainIdx()).pt();
                        sumX += sourcePt.x();
                        sumY += sourcePt.y();
                        count++;
                    }

                    if (count > 0) {
                        int x = (int)(sumX / count - target.getWidth() / 2);
                        int y = (int)(sumY / count - target.getHeight() / 2);

                        // 释放资源
                        sourceKeypoints.deallocate();
                        targetKeypoints.deallocate();
                        sourceDescriptors.release();
                        targetDescriptors.release();
                        matches.deallocate();
                        sourceMat.release();
                        targetMat.release();

                        return new Position(x, y);
                    }
                }

                // 释放资源
                sourceKeypoints.deallocate();
                targetKeypoints.deallocate();
                sourceDescriptors.release();
                targetDescriptors.release();
                matches.deallocate();
            }
        }

        sourceMat.release();
        targetMat.release();

        return null;
    }

    /**
     * 智能查找 - 依次尝试多种方法
     */
    public static Position findImgSmart(BufferedImage source, BufferedImage target, double threshold) {
        Position result;

        // 1. 先尝试标准模板匹配（最快）
        result = findImgGray(source, target, threshold);
        if (result != null) {
            return result;
        }

        // 2. 尝试多尺度匹配（处理大小不一致）
        result = findImgMultiScale(source, target, threshold * 0.9);
        if (result != null) {
            return result;
        }

        // 3. 尝试带旋转的匹配（处理角度不一致）
//        result = findImgWithRotation(source, target, threshold * 0.85);
//        if (result != null) {
//            return result;
//        }

        // 4. 尝试特征匹配（最鲁棒但最慢）
//        result = findImgByORB(source, target, 4);
//        if (result != null) {
//            return result;
//        }
        return null;
    }

    /**
     * 标准灰度图匹配
     */
    public static Position findImgGray(BufferedImage source, BufferedImage target, double similarityScore) {
        Mat sourceMat = bufferedImageToMat(source);
        Mat targetMat = bufferedImageToMat(target);

        Mat sourceGray = new Mat();
        Mat targetGray = new Mat();
        cvtColor(sourceMat, sourceGray, COLOR_BGR2GRAY);
        cvtColor(targetMat, targetGray, COLOR_BGR2GRAY);

        Mat result = new Mat();
        matchTemplate(sourceGray, targetGray, result, CV_TM_CCOEFF_NORMED);

        DoublePointer minVal = new DoublePointer(1);
        DoublePointer maxVal = new DoublePointer(1);
        Point minLoc = new Point();
        Point maxLoc = new Point();

        minMaxLoc(result, minVal, maxVal, minLoc, maxLoc, null);

        Position position = null;
        if (maxVal.get() >= similarityScore) {
            position = new Position(maxLoc.x(), maxLoc.y());
        }

        // 释放资源
        sourceGray.release();
        targetGray.release();
        sourceMat.release();
        targetMat.release();
        result.release();
        minVal.deallocate();
        maxVal.deallocate();
        minLoc.deallocate();
        maxLoc.deallocate();

        return position;
    }

    /**
     * 使用示例
     */
    public static void main(String[] args) throws IOException {
        BufferedImage source = ImageIO.read(new File("screenshot-1.png"));
        BufferedImage target = ImageIO.read(new File("cf-fuck.png"));

        // 使用智能查找（推荐）
        Position pos = findImgSmart(source, target, 0.7);
        if (pos != null) {
            System.out.println("找到目标位置: " + pos);
        } else {
            System.out.println("未找到目标");
        }

        // 获取position中的内容
        Graphics graphics = source.getGraphics();
        graphics.setColor(Color.BLACK);
        graphics.drawRect(pos.getX() + 27, pos.getY() + 33, 10, 10);

        ImageIO.write(source, "jpg", new File("test.jpg"));
    }
}
