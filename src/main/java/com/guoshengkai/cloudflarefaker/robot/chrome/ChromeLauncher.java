package com.guoshengkai.cloudflarefaker.robot.chrome;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChromeLauncher {

    /**
     * 跨平台打开Chrome浏览器
     * @param url 要打开的URL（可选）
     * @return true如果成功打开，false否则
     */
    public static boolean openChrome(String url) {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                return openChromeWindows(url);
            } else if (os.contains("mac")) {
                return openChromeMac(url);
            } else if (os.contains("nix") || os.contains("nux")) {
                return openChromeLinux(url);
            } else {
                System.err.println("不支持的操作系统: " + os);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 在Windows上打开Chrome
     */
    private static boolean openChromeWindows(String url) throws IOException {
        // Windows上Chrome的可能路径
        String[] possiblePaths = {
                System.getenv("LOCALAPPDATA") + "\\Google\\Chrome\\Application\\chrome.exe",
                System.getenv("PROGRAMFILES") + "\\Google\\Chrome\\Application\\chrome.exe",
                System.getenv("PROGRAMFILES(X86)") + "\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe"
        };

        // 查找Chrome可执行文件
        String chromePath = findExecutable(possiblePaths);

        if (chromePath != null) {
            List<String> command = new ArrayList<>();
            command.add(chromePath);
            if (url != null && !url.isEmpty()) {
                command.add(url);
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.start();
            return true;
        }

        // 尝试通过注册表启动
        try {
            List<String> command = new ArrayList<>();
            command.add("cmd");
            command.add("/c");
            command.add("start");
            command.add("chrome");
            if (url != null && !url.isEmpty()) {
                command.add(url);
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.start();
            return true;
        } catch (Exception e) {
            System.err.println("无法找到Chrome浏览器");
            return false;
        }
    }

    /**
     * 在macOS上打开Chrome
     */
    private static boolean openChromeMac(String url) throws IOException {
        // macOS上Chrome的可能路径
        String[] possiblePaths = {
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                System.getProperty("user.home") + "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
        };

        // 查找Chrome可执行文件
        String chromePath = findExecutable(possiblePaths);

        if (chromePath != null) {
            List<String> command = new ArrayList<>();
            command.add(chromePath);
            if (url != null && !url.isEmpty()) {
                command.add(url);
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.start();
            return true;
        }

        // 使用open命令
        try {
            List<String> command = new ArrayList<>();
            command.add("open");
            command.add("-a");
            command.add("Google Chrome");
            if (url != null && !url.isEmpty()) {
                command.add(url);
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            System.err.println("无法找到Chrome浏览器");
            return false;
        }
    }

    /**
     * 在Linux上打开Chrome
     */
    private static boolean openChromeLinux(String url) throws IOException {
        // Linux上Chrome的可能路径
        String[] possiblePaths = {
                "/usr/bin/google-chrome",
                "/usr/bin/google-chrome-stable",
                "/usr/bin/chromium",
                "/usr/bin/chromium-browser",
                "/snap/bin/chromium",
                "/opt/google/chrome/chrome",
                "/opt/google/chrome/google-chrome"
        };

        // 查找Chrome可执行文件
        String chromePath = findExecutable(possiblePaths);

        if (chromePath != null) {
            List<String> command = new ArrayList<>();
            command.add(chromePath);
            if (url != null && !url.isEmpty()) {
                command.add(url);
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.start();
            return true;
        }

        // 尝试使用which命令查找
        try {
            String[] browsers = {"google-chrome", "google-chrome-stable", "chromium", "chromium-browser"};
            for (String browser : browsers) {
                Process which = Runtime.getRuntime().exec("which " + browser);
                if (which.waitFor() == 0) {
                    List<String> command = new ArrayList<>();
                    command.add(browser);
                    if (url != null && !url.isEmpty()) {
                        command.add(url);
                    }

                    ProcessBuilder pb = new ProcessBuilder(command);
                    pb.start();
                    return true;
                }
            }
        } catch (Exception e) {
            // 忽略
        }

        System.err.println("无法找到Chrome浏览器");
        return false;
    }

    /**
     * 查找可执行文件
     */
    private static String findExecutable(String[] paths) {
        for (String path : paths) {
            if (path != null) {
                File file = new File(path);
                if (file.exists() && file.canExecute()) {
                    return path;
                }
            }
        }
        return null;
    }

    /**
     * 打开Chrome并传递参数
     * @param url 要打开的URL
     * @param args Chrome启动参数
     * @return true如果成功打开，false否则
     */
    public static boolean openChromeWithArgs(String url, String... args) {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            List<String> command = new ArrayList<>();

            // 获取Chrome路径
            String chromePath = getChromeExecutablePath();
            if (chromePath == null) {
                System.err.println("无法找到Chrome浏览器");
                return false;
            }

            command.add(chromePath);

            // 添加参数
            command.addAll(Arrays.asList(args));

            // 添加URL
            if (url != null && !url.isEmpty()) {
                command.add(url);
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.start();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取Chrome可执行文件路径
     */
    private static String getChromeExecutablePath() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            String[] paths = {
                    System.getenv("LOCALAPPDATA") + "\\Google\\Chrome\\Application\\chrome.exe",
                    System.getenv("PROGRAMFILES") + "\\Google\\Chrome\\Application\\chrome.exe",
                    System.getenv("PROGRAMFILES(X86)") + "\\Google\\Chrome\\Application\\chrome.exe"
            };
            return findExecutable(paths);
        } else if (os.contains("mac")) {
            String[] paths = {
                    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                    System.getProperty("user.home") + "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
            };
            return findExecutable(paths);
        } else if (os.contains("nix") || os.contains("nux")) {
            String[] paths = {
                    "/usr/bin/google-chrome",
                    "/usr/bin/google-chrome-stable",
                    "/usr/bin/chromium",
                    "/usr/bin/chromium-browser"
            };
            return findExecutable(paths);
        }

        return null;
    }

    /**
     * 检查Chrome是否已安装
     */
    public static boolean isChromeInstalled() {
        return getChromeExecutablePath() != null;
    }

    /**
     * 打开Chrome的隐身模式
     */
    public static boolean openChromeIncognito(String url) {
        return openChromeWithArgs(url, "--incognito");
    }

    /**
     * 打开Chrome并指定窗口大小
     */
    public static boolean openChromeWithSize(String url, int width, int height) {
        return openChromeWithArgs(url, "--window-size=" + width + "," + height);
    }

    /**
     * 打开Chrome全屏模式
     */
    public static boolean openChromeFullscreen(String url) {
        return openChromeWithArgs(url, "--start-fullscreen");
    }

    /**
     * 打开Chrome并禁用扩展
     */
    public static boolean openChromeNoExtensions(String url) {
        return openChromeWithArgs(url, "--disable-extensions");
    }

    /**
     * 打开Chrome开发者工具
     */
    public static boolean openChromeWithDevTools(String url) {
        return openChromeWithArgs(url, "--auto-open-devtools-for-tabs");
    }

    /**
     * 使用示例
     */
    public static void main(String[] args) {
        // 检查Chrome是否安装
        if (!isChromeInstalled()) {
            System.out.println("Chrome浏览器未安装");
            return;
        }

        // 1. 打开Chrome（不带URL）
        System.out.println("打开Chrome...");
        openChrome(null);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 2. 打开指定URL
        System.out.println("打开百度...");
        openChrome("https://www.baidu.com");

        // 3. 打开隐身模式
        System.out.println("打开隐身模式...");
        openChromeIncognito("https://www.google.com");

        // 4. 指定窗口大小
        System.out.println("打开指定大小窗口...");
        openChromeWithSize("https://www.github.com", 1200, 800);

        // 5. 全屏模式
        System.out.println("打开全屏模式...");
        openChromeFullscreen("https://www.youtube.com");

        // 6. 带开发者工具
        System.out.println("打开开发者工具...");
        openChromeWithDevTools("https://www.stackoverflow.com");

        // 7. 自定义参数
        System.out.println("自定义参数启动...");
        openChromeWithArgs("https://www.example.com",
                "--disable-gpu",
                "--disable-software-rasterizer",
                "--no-sandbox");
    }
}
