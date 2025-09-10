package com.guoshengkai.cloudflarefaker.entitys;

import lombok.Getter;
import lombok.Setter;

/**
 * FetchCommand represents a command to fetch a resource from a specified URL.
 * It includes details such as the page URL, fetch URL, HTTP method, request body, and whether to stream the response.
 *
 * @author gsk
 */
@Getter
@Setter
public class FetchCommand {

    /**
     * 在哪个页面上发起请求
     */
    private String pageUrl;

    /**
     * 请求的URL地址
     */
    private String fetchUrl;

    /**
     * HTTP请求方法，例如GET、POST等
     */
    private String method;

    /**
     * 请求体内容，可以是字符串或其他对象
     */
    private Object body;

    /**
     * 是否以流式方式处理响应
     */
    private boolean stream;

}
