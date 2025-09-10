package com.guoshengkai.cloudflarefaker;

import com.guoshengkai.cloudflarefaker.robot.chrome.ChromeLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CloudflareFakerApplication implements ApplicationRunner {

    public static void main(String[] args) {
        SpringApplication.run(CloudflareFakerApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 开始机器人
        ChromeLauncher.openChrome("");
        System.out.println("""
                +--------------------------------------------------------------+
                        CloudflareFakerApplication started successfully.
                +--------------------------------------------------------------+
                 🚀 If the browser did not open automatically,
                    please open it manually.
                
                 🔧 Make sure to enable Developer Mode
                    and load the extension from the
                    'cloudflare_monitor_chrome_plugin' directory
                    in the project root.
                
                 🌐 Access the application at:
                    http://localhost:8080
                """);
    }
}
