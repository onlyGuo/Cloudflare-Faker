package com.guoshengkai.cloudflarefaker.robot;


import com.guoshengkai.cloudflarefaker.robot.chrome.ChromeLauncher;
import com.guoshengkai.cloudflarefaker.robot.screen.Position;
import com.guoshengkai.cloudflarefaker.robot.screen.ScreenInfo;
import com.guoshengkai.cloudflarefaker.utils.FileUtil;

import java.io.File;
import java.util.List;

public class FuckCloudflare {

    private ComputerRobot robot = new ComputerRobot();

    private String url;

    public FuckCloudflare(String url){
        this.url = url;
    }

    /**
     * 开始过CF
     * @return Cookies
     */
    public String fuck() {
        ChromeLauncher.openChrome(url);
        Position position = null;
        int tryCount = 0;
        while (true) {
            long startTime = System.currentTimeMillis();
            System.out.println("第" + tryCount + "轮尝试, 开始寻找Cloudflare质询框");
            position = findCloudflareFuckBtn();
            System.out.println("第" + tryCount + "轮尝试, 耗时:" +
                    (System.currentTimeMillis() - startTime) + "ms, 质询框搜寻结果:" + position);
            if (position != null){
                robot.mouseMove(position.getX() + 30, position.getY() + 40);
                robot.mouseClick();
            }
            tryCount ++;
        }
    }

    private Position findCloudflareFuckBtn(){
        List<ScreenInfo> allScreenInfo = robot.getAllScreenInfo();
        for (ScreenInfo screenInfo : allScreenInfo) {
            Position imgInScreen = screenInfo.findImgInScreen(FileUtil.readImageFromResource("cf-fuck.png"), 0.8);
            if (imgInScreen != null && imgInScreen.getX() > 0 && imgInScreen.getY() > 0) {
                return imgInScreen;
            }
        }
        return null;
    }
}
