package com.guoshengkai.cloudflarefaker.entitys;

import lombok.Getter;
import lombok.Setter;

/**
 * JavascriptCommand represents a command to execute JavaScript code on a specified page.
 * It includes details such as the page URL and the JavaScript code to be executed.
 *
 * @author gsk
 */
@Getter
@Setter
public class JavascriptCommand {
    /**
     * 在哪个页面上执行脚本
     */
    private String pageUrl;

    /**
     * 要执行的JavaScript代码
     */
    private String script;

    /**
     * 任务类型，默认为 "EXECUTE_SCRIPT_TASK"
     */
    private String type = "EXECUTE_SCRIPT_TASK";
}
