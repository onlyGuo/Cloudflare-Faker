package com.guoshengkai.cloudflarefaker.robot;

import com.guoshengkai.cloudflarefaker.robot.screen.Position;
import com.guoshengkai.cloudflarefaker.robot.screen.ScreenInfo;
import com.guoshengkai.cloudflarefaker.utils.FileUtil;
import com.guoshengkai.cloudflarefaker.websocket.WebSocketMessageSender;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CloudflareRobotManager {

    protected Logger log = LoggerFactory.getLogger(WebSocketMessageSender.class);

    boolean stop = false;

    @PostConstruct
    public void init(){
        ComputerRobot robot = new ComputerRobot();
        Thread.startVirtualThread(() -> {
            while (!stop) {
                try {
                    List<ScreenInfo> allScreenInfo = robot.getAllScreenInfo();
                    for (ScreenInfo screenInfo : allScreenInfo) {
                        Position imgInScreen = screenInfo.findImgInScreen(FileUtil
                                .readImageFromResource("cf-check.png"), 0.8);
                        if (imgInScreen != null) {
                            robot.mouseMove(imgInScreen.getX(), imgInScreen.getY());
                            robot.sleep((int) (100 + Math.random() * 400));
                            robot.mouseClick();
                        }
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        });
    }

    @PreDestroy
    public void stop(){
        this.stop = true;
    }

}
