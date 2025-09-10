// Content script for Cloudflare Monitor Chrome Extension

class CloudflareContentScript {
  constructor() {
    this.isCloudflareChallenge = false;
    this.isPageLoading = true;
    this.currentTask = null;
    this.taskTimeout = null;

    this.init();
  }

  init() {
    this.setupEventListeners();
    this.checkPageState();
    this.observePageChanges();
  }

  setupEventListeners() {
    // Listen for messages from background script
    chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
      this.handleMessage(message, sender, sendResponse);
    });

    // Page load events
    document.addEventListener('DOMContentLoaded', () => {
      this.onDOMReady();
    });

    window.addEventListener('load', () => {
      this.onPageFullyLoaded();
    });
  }

  handleMessage(message, sender, sendResponse) {
    const { type } = message;

    switch (type) {
      case 'EXECUTE_FETCH_TASK':
        this.executeFetchTask(message.task, message.taskId);
        sendResponse({ success: true });
        break;
      case 'EXECUTE_SCRIPT_TASK':
      case 'LOAD_HTML':
        try{
          this.executeScriptTask(message.task, message.taskId);
          sendResponse({ success: true });
        }catch(e){
          this.log('error', `Script task execution error:`, e.message);
          this.sendTaskError(message.taskId, 'EXECUTION_ERROR', e.message);
        }
        break;
      default:
        sendResponse({ success: false, error: 'Unknown message type' });
    }
  }

  async executeFetchTask(task, taskId) {
    this.log('info', `Executing fetch task ${taskId} for ${task.fetchUrl}`);
    this.currentTask = { ...task, taskId };

    // Set timeout for the entire operation (excluding fetch execution time)
    this.taskTimeout = setTimeout(() => {
      if (this.currentTask) {
        this.sendTaskError(taskId, 'TIMEOUT', 'Task preparation timeout (20 seconds)');
        this.clearCurrentTask();
      }
    }, 20000);

    try {
      // Wait for page to be ready
      await this.waitForPageReady();

      // Clear timeout - actual fetch execution doesn't count toward 20s limit
      if (this.taskTimeout) {
        clearTimeout(this.taskTimeout);
        this.taskTimeout = null;
      }

      // Execute the fetch
      await this.performFetch(task, taskId);

    } catch (error) {
      // Check if this is a Cloudflare firewall error that should trigger retry
      if (error.message.includes('Cloudflare firewall triggered')) {
        this.log('warn', `Task ${taskId} - Cloudflare firewall detected, will retry after page refresh`);
        this.sendTaskError(taskId, 'CLOUDFLARE_FIREWALL', error.message);
        // Don't clear task - let the retry mechanism handle it after page refresh
        return;
      }

      this.log('error', `Task ${taskId} failed:`, error.message);
      this.sendTaskError(taskId, 'EXECUTION_ERROR', error.message);
    } finally {
      this.clearCurrentTask();
    }
  }

  async executeScriptTask(task, taskId) {
    this.log('info', `Executing script task ${taskId}`);
    this.currentTask = { ...task, taskId };

    // Set timeout for the entire operation (excluding script execution time)
    this.taskTimeout = setTimeout(() => {
      if (this.currentTask) {
        this.sendTaskError(taskId, 'TIMEOUT', 'Task preparation timeout (20 seconds)');
        this.clearCurrentTask();
      }
    }, 20000);

    try {
      // Wait for page to be ready
      await this.waitForPageReady();

      // Clear timeout - actual script execution doesn't count toward 20s limit
      if (this.taskTimeout) {
        clearTimeout(this.taskTimeout);
        this.taskTimeout = null;
      }

      if (task.type === 'LOAD_HTML') {
        // For LOAD_HTML tasks, return the entire HTML content
        const htmlContent = document.documentElement.outerHTML;
        this.sendTaskResponse(taskId, {
          type: 'object',
          data: { html: htmlContent }
        });
        return;
      }

      // Execute the script
      let result;
      try {
        result = eval(task.script); // Caution: using eval has security implications
      } catch (e) {
        throw new Error(`Script execution error: ${e.message}`);
      }

      this.sendTaskResponse(taskId, {
        type: 'object',
        data: result
      });

    } catch (error) {
      this.log('error', `Task ${taskId} failed:`, error.message);
      this.sendTaskError(taskId, 'EXECUTION_ERROR', error.message);
    } finally {
      this.clearCurrentTask();
    }
  }

  async waitForPageReady() {
    this.log('info', 'Waiting for page to be ready...');

    const maxWaitTime = 20000; // 20 seconds
    const startTime = Date.now();

    while (Date.now() - startTime < maxWaitTime) {
      // Check if Cloudflare challenge is active
      if (this.isCloudflareChallenge) {
        this.log('info', 'Waiting for Cloudflare challenge to complete...');
        await this.sleep(500);
        continue;
      }

      // Check if page is still loading
      if (this.isPageLoading) {
        this.log('info', 'Waiting for page to finish loading...');
        await this.sleep(500);
        continue;
      }

      // Page is ready
      this.log('info', 'Page is ready for task execution');
      return;
    }

    throw new Error('Page preparation timeout');
  }

  async performFetch(task, taskId) {
    const { fetchUrl, method, body, stream } = task;

    this.log('info', `Performing ${method} request to ${fetchUrl}`);

    try {
      const requestOptions = {
        method: method || 'GET',
        credentials: 'include', // Include cookies for same-origin requests
        headers: {
          'Content-Type': 'application/json',
          'User-Agent': navigator.userAgent
        }
      };

      if (body && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
        requestOptions.body = typeof body === 'string' ? body : JSON.stringify(body);
      }

      if (stream) {
        await this.performStreamFetch(fetchUrl, requestOptions, taskId);
      } else {
        await this.performRegularFetch(fetchUrl, requestOptions, taskId);
      }

    } catch (error) {
      // Check if this might be a Cloudflare firewall trigger
      if (this.isCloudflareFirewallResponse(error)) {
        this.sendTaskError(taskId, 'CLOUDFLARE_FIREWALL', 'Cloudflare firewall triggered');
      }else if (error.message.includes("Cloudflare firewall triggered")){
        this.sendTaskError(taskId, 'CLOUDFLARE_FIREWALL', 'Cloudflare firewall triggered');
      } else {
        throw error;
      }
    }
  }

  async performRegularFetch(url, options, taskId) {
    const response = await fetch(url, options);

    // Check for Cloudflare firewall response
    if (this.isCloudflareFirewallResponse(response)) {
      throw new Error('Cloudflare firewall triggered');
    }

    const responseData = {
      status: response.status,
      statusText: response.statusText,
      headers: Object.fromEntries(response.headers.entries())
    };

    try {
      const text = await response.text();
      responseData.body = text;

      // Try to parse as JSON if possible
      try {
        responseData.json = JSON.parse(text);
      } catch (e) {
        // Not JSON, that's fine
      }
    } catch (error) {
      responseData.error = error.message;
    }

    this.sendTaskResponse(taskId, {
      type: 'object',
      data: responseData
    });
  }

  async performStreamFetch(url, options, taskId) {
    const response = await fetch(url, options);

    // Check for Cloudflare firewall response
    if (this.isCloudflareFirewallResponse(response)) {
      throw new Error('Cloudflare firewall triggered');
    }

    if (!response.body) {
      this.sendTaskResponse(taskId, {
        type: 'stream',
        data: '[!!DONE!!]'
      });
      return;
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();

    try {
      while (true) {
        const { done, value } = await reader.read();

        if (done) {
          // Send completion signal
          this.sendTaskResponse(taskId, {
            type: 'stream',
            data: '[!!DONE!!]'
          });
          break;
        }

        const chunk = decoder.decode(value, { stream: true });
        const lines = chunk.split('\n');

        for (const line of lines) {
          if (line.trim()) {
            this.sendTaskResponse(taskId, {
              type: 'stream',
              data: line
            });
          }
        }
      }
    } finally {
      reader.releaseLock();
    }
  }

  isCloudflareFirewallResponse(response) {
    if (!response || typeof response.status !== 'number') return false;

    // Check for typical Cloudflare firewall status codes
    if ([403, 429, 503].includes(response.status)) {
      // Additional checks could be added here for specific Cloudflare signatures
      return true;
    }

    return false;
  }

  checkPageState() {
    // Check if this page is a Cloudflare challenge
    const url = window.location.href;
    const title = document.title?.toLowerCase() || '';
    const bodyText = document.body?.textContent?.toLowerCase() || '';

    // URL tokens indicating Cloudflare challenge
    const hasUrlTokens = url.includes('__cf_chl_tk=') ||
        url.includes('cf_chl_jschl_tk=') ||
        url.includes('__cf_chl_captcha_tk=');

    // 固定的Cloudflare标题特征
    const titleIndicators = [
      'please wait...',
      '请稍后...',
      'Just a moment...',
      '稍等片刻...'
    ];

    // 检查标题是否匹配
    let hasChallengeTitle = false;
    for (const indicator of titleIndicators) {
      if (title.includes(indicator)) {
        hasChallengeTitle = true;
        break;
      }
    }

    // 检查内容是否匹配指定的三种情况
    const hasChallengeText =
        bodyText.includes('正在验证您是否是真人。这可能需要几秒钟时间') ||
        bodyText.includes('请完成以下操作，验证您是真人') ||
        (bodyText.includes('继续之前') && bodyText.includes('需要先检查您的连接的安全性'));

    // 英文对应内容检查
    const hasEnglishChallengeText =
        bodyText.includes('checking if you are a real person. this may take a few seconds') ||
        bodyText.includes('please complete the following to verify that you are human') ||
        (bodyText.includes('before you continue') && bodyText.includes('needs to check the security of your connection'));

    const oldChallengeState = this.isCloudflareChallenge;
    this.isCloudflareChallenge = hasUrlTokens || hasChallengeTitle || hasChallengeText || hasEnglishChallengeText;

    // Notify status change
    if (oldChallengeState !== this.isCloudflareChallenge || this.isPageLoading !== document.readyState !== 'complete') {
      this.notifyStatusChange();
    }
  }

  observePageChanges() {
    // Monitor DOM changes to detect challenge state changes
    const observer = new MutationObserver(() => {
      this.checkPageState();
    });

    observer.observe(document.body || document.documentElement, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ['class', 'id']
    });

    // Also monitor for page navigation
    let lastUrl = window.location.href;
    const checkUrlChange = () => {
      if (window.location.href !== lastUrl) {
        lastUrl = window.location.href;
        this.isPageLoading = true;
        this.checkPageState();
      }
      setTimeout(checkUrlChange, 1000);
    };
    checkUrlChange();
  }

  onDOMReady() {
    this.log('info', 'DOM ready');
    this.checkPageState();
  }

  onPageFullyLoaded() {
    this.log('info', 'Page fully loaded');
    this.isPageLoading = false;
    this.checkPageState();
  }

  notifyStatusChange() {
    const status = this.getBusyStatus();
    const url = window.location.href;

    // Provide detailed challenge detection info to background script
    const challengeInfo = this.getDetailedChallengeInfo();

    // Log detailed status information
    this.log('info', `Status: ${status} | Loading: ${this.isPageLoading} | Challenge: ${this.isCloudflareChallenge} | URL: ${url.includes('__cf_chl_tk=') ? 'has challenge token' : 'no token'}`);

    chrome.runtime.sendMessage({
      type: 'PAGE_STATUS_CHANGED',
      status,
      challengeInfo
    });
  }

  getDetailedChallengeInfo() {
    const url = window.location.href;
    const title = document.title?.toLowerCase() || '';
    const bodyText = document.body?.textContent?.toLowerCase() || '';

    // URL tokens indicating Cloudflare challenge
    const hasUrlTokens = url.includes('__cf_chl_tk=') ||
        url.includes('cf_chl_jschl_tk=') ||
        url.includes('__cf_chl_captcha_tk=');

    // 固定的Cloudflare标题特征
    const titleIndicators = [
      'please wait...',
      '请稍后...',
      'Just a moment...',
      '稍等片刻...'
    ];

    // 检查标题是否匹配
    let hasChallengeTitle = false;
    for (const indicator of titleIndicators) {
      if (title.includes(indicator)) {
        hasChallengeTitle = true;
        break;
      }
    }

    // 检查内容是否匹配指定的三种情况
    const hasChallengeText =
        bodyText.includes('正在验证您是否是真人。这可能需要几秒钟时间') ||
        bodyText.includes('请完成以下操作，验证您是真人') ||
        (bodyText.includes('继续之前') && bodyText.includes('需要先检查您的连接的安全性'));

    // 英文对应内容检查
    const hasEnglishChallengeText =
        bodyText.includes('checking if you are a real person. this may take a few seconds') ||
        bodyText.includes('please complete the following to verify that you are human') ||
        (bodyText.includes('before you continue') && bodyText.includes('needs to check the security of your connection'));

    const hasChallengeElements = !!document.querySelector('.cf-browser-verification, .cf-checking-browser, .cf-challenge-running, #challenge-running');

    return {
      hasUrlTokens,
      hasChallengeTitle,
      hasChallengeText: hasChallengeText || hasEnglishChallengeText,
      hasChallengeElements,
      isPageLoading: this.isPageLoading,
      url: url.substring(0, 100) // First 100 chars for debugging
    };
  }

  getBusyStatus() {
    return (this.isCloudflareChallenge || this.isPageLoading) ? 'busy' : 'idle';
  }

  sendTaskResponse(taskId, data) {
    chrome.runtime.sendMessage({
      type: 'TASK_RESPONSE',
      taskId,
      data
    });
  }

  sendTaskError(taskId, errorType, error) {
    chrome.runtime.sendMessage({
      type: 'TASK_ERROR',
      taskId,
      errorType,
      error
    });
  }

  clearCurrentTask() {
    this.currentTask = null;
    if (this.taskTimeout) {
      clearTimeout(this.taskTimeout);
      this.taskTimeout = null;
    }
    this.notifyStatusChange();
  }

  sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  log(level, message, ...args) {
    console[level](`[CloudflareMonitor] ${message}`, ...args);
  }
}

// Initialize content script
if (typeof window !== 'undefined') {
  const contentScript = new CloudflareContentScript();
}
