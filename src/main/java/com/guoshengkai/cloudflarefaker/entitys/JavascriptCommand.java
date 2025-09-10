package com.guoshengkai.cloudflarefaker.entitys;

import lombok.Getter;
import lombok.Setter;

/**
 * JavascriptCommand represents a command to execute JavaScript code on a specified page.
 * It includes details such as the page URL and the JavaScript code to be executed.
 *
 * @author gsk
 */
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

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
