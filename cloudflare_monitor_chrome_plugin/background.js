// Background script for Cloudflare Monitor Chrome Extension

class CloudflareMonitor {
  constructor() {
    this.websocket = null;
    this.clientId = this.generateClientId();
    this.reconnectAttempts = 0;
    this.reconnectTimeouts = [0, 1000, 1000, 1000, 3000, 3000, 3000, 3000, 3000, 5000];
    this.taskQueue = new Map(); // taskId -> task info
    this.pageStatuses = new Map(); // tabId -> {url, status, lastUpdate}
    this.isConnecting = false;
    this.heartbeatInterval = null;

    this.init();
  }

  generateClientId() {
    return 'chrome-' + Math.random().toString(36).substr(2, 9) + '-' + Date.now();
  }

  init() {
    this.log('info', 'CloudflareMonitor initializing...');
    this.connectWebSocket();
    this.setupEventListeners();
    this.loadStoredState();
    this.log('info', 'CloudflareMonitor initialization complete');
  }

  setupEventListeners() {
    // Tab events
    chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
      if (changeInfo.status === 'complete' && tab.url) {
        this.onTabLoaded(tabId, tab.url);
      }
    });

    chrome.tabs.onRemoved.addListener((tabId) => {
      this.pageStatuses.delete(tabId);
      this.saveState(); // Save state after removing tab
      // Notify console pages of status change
      this.notifyStatusChange();
    });

    // Message handling between content scripts
    chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
      this.handleMessage(message, sender, sendResponse);
    });

    // Handle extension startup
    chrome.runtime.onStartup.addListener(() => {
      this.connectWebSocket();
    });
  }

  async loadStoredState() {
    try {
      const result = await chrome.storage.local.get(['pageStatuses']);
      if (result.pageStatuses) {
        this.pageStatuses = new Map(Object.entries(result.pageStatuses));

        // Clean up stale tabs that no longer exist
        await this.cleanupStaleTabs();
      }
    } catch (error) {
      console.error('Failed to load stored state:', error);
    }
  }

  async cleanupStaleTabs() {
    try {
      // Get all current tabs
      const currentTabs = await chrome.tabs.query({});
      const currentTabIds = new Set(currentTabs.map(tab => tab.id.toString()));

      // Remove any stored page statuses for tabs that no longer exist
      let removedCount = 0;
      for (const [tabId] of this.pageStatuses) {
        if (!currentTabIds.has(tabId)) {
          this.pageStatuses.delete(tabId);
          removedCount++;
        }
      }

      if (removedCount > 0) {
        this.log('info', `Cleaned up ${removedCount} stale tab entries`);
        await this.saveState();
        // Notify console pages of status change after cleanup
        this.notifyStatusChange();
      }
    } catch (error) {
      console.error('Failed to cleanup stale tabs:', error);
    }
  }

  async saveState() {
    try {
      await chrome.storage.local.set({
        pageStatuses: Object.fromEntries(this.pageStatuses)
      });
    } catch (error) {
      console.error('Failed to save state:', error);
    }
  }

  connectWebSocket() {
    if (this.isConnecting || (this.websocket && this.websocket.readyState === WebSocket.OPEN)) {
      return;
    }

    this.isConnecting = true;
    this.log('info', `Connecting to WebSocket... (attempt ${this.reconnectAttempts + 1})`);

    try {
      this.websocket = new WebSocket('ws://localhost:8080/ws');

      this.websocket.onopen = () => {
        this.isConnecting = false;
        this.reconnectAttempts = 0;
        this.log('info', 'WebSocket connected');
        this.register();
        // Notify console pages of connection status change
        this.notifyStatusChange();
        // 开始发送心跳
        if (this.heartbeatInterval) {
          clearInterval(this.heartbeatInterval);
        }
        this.heartbeatInterval = setInterval(() => {
          if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
            this.websocket.send(JSON.stringify({ type: 'ping' }));
          }
        }, 10000); // 每10秒发送一次心跳
      };

      this.websocket.onmessage = (event) => {
        this.handleWebSocketMessage(event.data);
      };

      this.websocket.onclose = () => {
        // 停止心跳
        if (this.heartbeatInterval) {
          clearInterval(this.heartbeatInterval);
          this.heartbeatInterval = null;
        }
        this.isConnecting = false;
        this.log('warn', 'WebSocket disconnected');
        this.scheduleReconnect();
        // Notify console pages of connection status change
        this.notifyStatusChange();
      };

      this.websocket.onerror = (error) => {
        this.isConnecting = false;
        this.log('error', 'WebSocket error: ' + error.message);
        // Notify console pages of connection status change
        this.notifyStatusChange();
      };

    } catch (error) {
      this.isConnecting = false;
      this.log('error', 'Failed to create WebSocket: ' + error.message);
      this.scheduleReconnect();
      // Notify console pages of connection status change
      this.notifyStatusChange();
    }
  }

  scheduleReconnect() {
    const timeoutIndex = Math.min(this.reconnectAttempts, this.reconnectTimeouts.length - 1);
    const timeout = this.reconnectTimeouts[timeoutIndex];

    this.log('info', `Scheduling reconnect in ${timeout}ms`);

    setTimeout(() => {
      this.reconnectAttempts++;
      this.connectWebSocket();
    }, timeout);
  }

  register() {
    const message = {
      type: 'register',
      clientId: this.clientId,
      timestamp: Date.now()
    };
    this.sendWebSocketMessage(message);
    this.log('info', `Registered with clientId: ${this.clientId}`);
  }

  sendWebSocketMessage(message) {
    if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
      this.websocket.send(JSON.stringify(message));
      return true;
    }
    return false;
  }

  handleWebSocketMessage(data) {
    try {
      const message = JSON.parse(data);
      this.log('info', 'Received message:', message);

      switch (message.type) {
        case 'fetch-command':
          this.handleFetchCommand(message);
          break;
        case 'register_ack':
          this.log('info', 'Registration acknowledged');
          break;
        case 'execute-script':
          this.handleScriptExecution(message);
        default:
          this.log('warn', 'Unknown message type:', message.type);
      }
    } catch (error) {
      this.log('error', 'Failed to parse WebSocket message:', error.message);
    }
  }

  async handleFetchCommand(message) {
    const { taskId, data } = message;
    const { pageUrl, fetchUrl, method, body, stream } = data;

    this.log('info', `Received fetch command for ${pageUrl}, taskId: ${taskId}`);

    // Store task
    this.taskQueue.set(taskId, {
      taskId,
      pageUrl,
      fetchUrl,
      method: method || 'GET',
      body,
      stream: stream || false,
      timestamp: Date.now(),
      retryCount: 0,
      maxRetries: 5
    });

    // Find matching tab
    const tabs = await chrome.tabs.query({ url: pageUrl });
    let tabId;

    if (tabs.length === 0) {
      // No matching tab found, create a new one
      try {
        this.log('info', `No matching tab found for ${pageUrl}, creating new tab`);
        const newTab = await chrome.tabs.create({ url: pageUrl });
        tabId = newTab.id;

        // Wait for tab to load before executing task
        this.waitForTabLoadAndExecute(taskId, tabId);
      } catch (createError) {
        this.sendTaskError(taskId, 'TAB_CREATE_ERROR', `Failed to create tab: ${createError.message}`);
        return;
      }
    } else {
      tabId = tabs[0].id;
      this.executeTaskInTab(taskId, tabId);
    }
  }

  async handleScriptExecution(message) {
    const { taskId, data } = message;
    const { pageUrl, script, type } = data;
    this.log('info', `Received script execution command for ${pageUrl}, taskId: ${taskId}`);
    // Store task
    this.taskQueue.set(taskId, {
      taskId,
      pageUrl,
      script,
      timestamp: Date.now(),
      retryCount: 0,
      maxRetries: 5,
      isScript: true,
      type: type ? type : 'EXECUTE_SCRIPT_TASK'
    });
    // Find matching tab
    const tabs = await chrome.tabs.query({ url: pageUrl });
    let tabId;
    if (tabs.length === 0) {
      // No matching tab found, create a new one
      try {
        this.log('info', `No matching tab found for ${pageUrl}, creating new tab`);
        const newTab = await chrome.tabs.create({ url: pageUrl });
        tabId = newTab.id;
        // Wait for tab to load before executing task
        this.waitForTabLoadAndExecute(taskId, tabId);
      } catch (createError) {
        this.sendTaskError(taskId, 'TAB_CREATE_ERROR', `Failed to create tab: ${createError.message}`);
        return;
      }
    } else {
      tabId = tabs[0].id;
      this.executeTaskInTab(taskId, tabId);
    }
  }

  async waitForTabLoadAndExecute(taskId, tabId) {
    const task = this.taskQueue.get(taskId);
    if (!task) return;

    this.log('info', `Waiting for newly created tab ${tabId} to load before executing task ${taskId}`);

    // Listen for tab update to know when it's loaded
    const tabUpdateListener = (updatedTabId, changeInfo, tab) => {
      if (updatedTabId === tabId && changeInfo.status === 'complete') {
        chrome.tabs.onUpdated.removeListener(tabUpdateListener);
        this.log('info', `Tab ${tabId} loaded, executing task ${taskId}`);
        this.executeTaskInTab(taskId, tabId);
      }
    };

    chrome.tabs.onUpdated.addListener(tabUpdateListener);

    // Set timeout in case tab never loads
    setTimeout(() => {
      chrome.tabs.onUpdated.removeListener(tabUpdateListener);
      this.sendTaskError(taskId, 'TAB_LOAD_TIMEOUT', 'New tab failed to load within timeout');
    }, 30000);
  }


  async executeTaskInTab(taskId, tabId) {
    const task = this.taskQueue.get(taskId);
    if (!task) return;

    // Send task to content script directly - allow parallel execution
    try {
      const response = await chrome.tabs.sendMessage(tabId, {
        type: task.isScript ? task.type: 'EXECUTE_FETCH_TASK',
        taskId,
        task
      });

      if (response && response.success) {
        this.log('info', `Task ${taskId} sent to content script successfully`);
      } else {
        this.sendTaskError(taskId, 'CONTENT_SCRIPT_ERROR', response?.error || 'Failed to send task to content script');
      }
    } catch (error) {
      this.log('error', `Failed to send task to tab ${tabId}:`, error.message);
      this.sendTaskError(taskId, 'TAB_COMMUNICATION_ERROR', error.message);
    }
  }

  handleMessage(message, sender, sendResponse) {
    const { type } = message;
    console.log('Background received message:', type);

    switch (type) {
      case 'TASK_RESPONSE':
        this.handleTaskResponse(message, sender);
        break;
      case 'TASK_ERROR':
        this.handleTaskError(message, sender);
        break;
      case 'PAGE_STATUS_CHANGED':
        this.handlePageStatusChanged(message, sender);
        break;
      case 'GET_LOGS':
        console.log('Handling GET_LOGS request...');
        (async () => {
          try {
            const logs = await this.getLogs();
            console.log('Sending logs response, count:', logs.length);
            sendResponse({ logs });
          } catch (error) {
            console.error('Error in GET_LOGS handler:', error);
            sendResponse({ logs: [] });
          }
        })();
        return true; // Keep message channel open for async response
      case 'GET_STATUS':
        sendResponse({
          connected: this.websocket?.readyState === WebSocket.OPEN,
          clientId: this.clientId,
          pageStatuses: Object.fromEntries(this.pageStatuses)
        });
        break;
      case 'RECONNECT':
        this.reconnectAttempts = 0;
        this.connectWebSocket();
        sendResponse({ success: true });
        break;
    }
  }

  handleTaskResponse(message, sender) {
    const { taskId, data } = message;
    this.log('info', `Task ${taskId} completed successfully`);

    const responseMessage = {
      type: 'task-response',
      taskId,
      data
    };

    this.sendWebSocketMessage(responseMessage);
    this.taskQueue.delete(taskId);
  }

  handleTaskError(message, sender) {
    const { taskId, error, errorType } = message;

    // Handle Cloudflare firewall error with retry logic
    if (errorType === 'CLOUDFLARE_FIREWALL') {
      this.handleCloudflareFirewallError(taskId, sender.tab?.id, error);
      return;
    }

    this.log('error', `Task ${taskId} failed: ${error}`);
    this.sendTaskError(taskId, errorType, error);
  }

  async handleCloudflareFirewallError(taskId, tabId, error) {
    const task = this.taskQueue.get(taskId);
    if (!task) return;

    task.retryCount++;
    this.log('warn', `Cloudflare firewall detected for task ${taskId}, retry ${task.retryCount}/${task.maxRetries}`);

    if (task.retryCount >= task.maxRetries) {
      this.log('error', `Max retries reached for task ${taskId}`);
      this.sendTaskError(taskId, 'CLOUDFLARE_FIREWALL', `Max retries (${task.maxRetries}) reached for Cloudflare firewall`);
      return;
    }

    // Refresh the tab and retry after a delay
    if (tabId) {
      try {
        this.log('info', `Refreshing tab ${tabId} for Cloudflare firewall retry`);
        await chrome.tabs.reload(tabId, { bypassCache: true });

        // Wait for page to become idle (Cloudflare challenge completed), then retry
        this.waitForPageIdleAndRetry(taskId, tabId);

      } catch (refreshError) {
        this.log('error', `Failed to refresh tab ${tabId}:`, refreshError.message);
        this.sendTaskError(taskId, 'TAB_REFRESH_ERROR', refreshError.message);
      }
    } else {
      this.sendTaskError(taskId, 'NO_TAB_ID', 'Cannot refresh tab - no tab ID provided');
    }
  }

  async waitForPageIdleAndRetry(taskId, tabId) {
    const task = this.taskQueue.get(taskId);
    if (!task) return;

    // Store the pending retry info
    task.pendingRetry = { taskId, tabId, startTime: Date.now() };
    this.log('info', `Waiting for tab ${tabId} to become idle before retrying task ${taskId}`);

    // Start polling for page status
    this.checkPageStatusForRetry(taskId, tabId);
  }

  checkPageStatusForRetry(taskId, tabId) {
    const task = this.taskQueue.get(taskId);
    if (!task || !task.pendingRetry) return;

    // Check timeout (30 seconds max wait - allow more time for challenge completion)
    const elapsed = Date.now() - task.pendingRetry.startTime;
    if (elapsed > 30000) {
      this.log('error', `Timeout waiting for Cloudflare challenge to complete for task ${taskId}`);
      this.sendTaskError(taskId, 'RETRY_TIMEOUT', 'Timeout waiting for Cloudflare challenge to complete');
      return;
    }

    const pageStatus = this.pageStatuses.get(tabId);

    if (pageStatus && pageStatus.status === 'idle') {
      // Page claims to be idle, but we need comprehensive challenge completion detection
      const timeSinceIdle = Date.now() - pageStatus.lastUpdate;
      const challengeInfo = pageStatus.challengeInfo || {};

      // Check all challenge indicators are clear
      const hasUrlTokens = challengeInfo.hasUrlTokens || (pageStatus.url && (pageStatus.url.includes('__cf_chl_tk=') || pageStatus.url.includes('cf_chl_jschl_tk=')));
      const hasChallengeTitle = challengeInfo.hasChallengeTitle;
      const hasChallengeText = challengeInfo.hasChallengeText;
      const hasChallengeElements = challengeInfo.hasChallengeElements;
      const isPageLoading = challengeInfo.isPageLoading;

      const challengeActive = hasUrlTokens || hasChallengeTitle || hasChallengeText || hasChallengeElements || isPageLoading;

      if (timeSinceIdle >= 3000 && !challengeActive) {
        // Page has been idle for 5+ seconds AND all challenge indicators are clear
        this.log('info', `Cloudflare challenge completed - page idle for ${timeSinceIdle}ms with no challenge indicators, retrying task ${taskId}`);
        delete task.pendingRetry;
        this.retryTaskAfterRefresh(taskId, tabId);
      } else {
        // Still in challenge or not stable enough
        const reasons = [];
        if (timeSinceIdle < 3000) reasons.push(`only idle for ${timeSinceIdle}ms`);
        if (hasUrlTokens) reasons.push('URL has challenge tokens');
        if (hasChallengeTitle) reasons.push('title indicates challenge');
        if (hasChallengeText) reasons.push('body text indicates challenge');
        if (hasChallengeElements) reasons.push('challenge DOM elements present');
        if (isPageLoading) reasons.push('page still loading');

        this.log('info', `Waiting for challenge completion - ${reasons.join(', ')}`);
        setTimeout(() => {
          this.checkPageStatusForRetry(taskId, tabId);
        }, 1000);
      }
    } else {
      // Page still busy with challenge, check again
      this.log('info', `Page still busy, continuing to wait for challenge completion...`);
      setTimeout(() => {
        this.checkPageStatusForRetry(taskId, tabId);
      }, 1000);
    }
  }

  async retryTaskAfterRefresh(taskId, tabId) {
    const task = this.taskQueue.get(taskId);
    if (!task) return;

    this.log('info', `Retrying task ${taskId} after tab refresh`);

    // Wait for tab to be ready, then execute task
    try {
      // Check if tab is still valid
      const tab = await chrome.tabs.get(tabId);
      if (tab) {
        this.executeTaskInTab(taskId, tabId);
      } else {
        this.sendTaskError(taskId, 'TAB_NOT_FOUND', 'Tab no longer exists after refresh');
      }
    } catch (error) {
      this.sendTaskError(taskId, 'TAB_ACCESS_ERROR', error.message);
    }
  }

  handlePageStatusChanged(message, sender) {
    const { status, challengeInfo } = message;
    if (sender.tab) {
      this.setPageStatus(sender.tab.id, sender.tab.url, status, challengeInfo);
    }
  }

  sendTaskError(taskId, errorType, error) {
    const responseMessage = {
      type: 'task-response',
      taskId,
      data: {
        error: {
          type: errorType,
          message: error
        }
      }
    };

    this.sendWebSocketMessage(responseMessage);
    this.taskQueue.delete(taskId);
  }

  setPageStatus(tabId, url, status, challengeInfo = null) {
    const oldStatus = this.pageStatuses.get(tabId);
    const newStatus = {
      url,
      status,
      lastUpdate: Date.now(),
      challengeInfo // Store detailed challenge information
    };

    this.pageStatuses.set(tabId, newStatus);

    // Notify server if status changed
    if (!oldStatus || oldStatus.status !== status) {
      const message = {
        type: 'page-status',
        data: {
          pageUrl: url,
          status
        }
      };
      this.sendWebSocketMessage(message);
      this.log('info', `Page status changed: ${url} -> ${status}`);
    }

    this.saveState();
    // Notify console pages of status change
    this.notifyStatusChange();
  }

  onTabLoaded(tabId, url) {
    // Set initial status as idle when tab loads
    this.setPageStatus(tabId, url, 'idle');
  }

  // Notify console pages of status changes
  notifyStatusChange() {
    const status = {
      connected: this.websocket?.readyState === WebSocket.OPEN,
      clientId: this.clientId,
      pageStatuses: Object.fromEntries(this.pageStatuses)
    };

    this.notifyConsolePages('STATUS_UPDATED', status);
  }

  log(level, message, ...args) {
    const timestamp = new Date().toISOString();
    const logEntry = {
      timestamp,
      level,
      message: typeof message === 'string' ? message : JSON.stringify(message),
      args: args.map(arg => typeof arg === 'string' ? arg : JSON.stringify(arg))
    };

    console[level](timestamp, message, ...args);

    // Store log for console page
    this.storeLogs(logEntry).catch(error => {
      console.error('Failed to store log:', error);
    });
  }

  async storeLogs(logEntry) {
    try {
      const result = await chrome.storage.local.get(['logs']);
      const logs = result.logs || [];
      logs.push(logEntry);

      // Keep only last 1000 log entries
      if (logs.length > 1000) {
        logs.splice(0, logs.length - 1000);
      }

      await chrome.storage.local.set({ logs });
      console.log('Stored log entry, total logs:', logs.length);

      // Notify console pages of new log entry
      this.notifyConsolePages('LOG_UPDATED', { logEntry, totalLogs: logs.length });
    } catch (error) {
      console.error('Failed to store log:', error);
    }
  }

  async notifyConsolePages(type, data) {
    try {
      // Send message to all tabs (console pages will listen for it)
      const tabs = await chrome.tabs.query({});
      for (const tab of tabs) {
        if (tab.url && tab.url.includes('chrome-extension://')) {
          chrome.tabs.sendMessage(tab.id, {
            type,
            data
          }).catch(() => {
            // Ignore errors for tabs that don't have content scripts
          });
        }
      }
    } catch (error) {
      // Ignore errors - not critical
    }
  }

  async getLogs() {
    try {
      const result = await chrome.storage.local.get(['logs']);
      return result.logs || [];
    } catch (error) {
      console.error('Failed to get logs:', error);
      return [];
    }
  }
}

// Initialize the monitor
const monitor = new CloudflareMonitor();
