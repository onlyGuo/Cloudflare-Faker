// Console dashboard JavaScript

class ConsoleManager {
  constructor() {
    this.logs = [];
    this.currentFilter = 'all';
    this.autoRefresh = true;
    this.refreshInterval = null;

    this.init();
  }

  async init() {
    this.setupEventListeners();
    this.startAutoRefresh();
    await this.loadStatus();
    await this.loadLogs();
  }

  setupEventListeners() {
    document.getElementById('clearLogs').addEventListener('click', () => {
      this.clearLogs();
    });

    document.getElementById('refreshLogs').addEventListener('click', () => {
      this.loadLogs();
    });

    document.getElementById('logLevel').addEventListener('change', (e) => {
      this.currentFilter = e.target.value;
      this.renderLogs();
    });

    // Listen for real-time updates from background script
    chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
      if (message.type === 'LOG_UPDATED') {
        this.handleLogUpdate(message.data);
      } else if (message.type === 'STATUS_UPDATED') {
        this.handleStatusUpdate(message.data);
      }
    });
  }

  startAutoRefresh() {
    // Only keep interval for potential future use, status updates are now real-time
    // Remove status polling since we now use real-time updates
    this.refreshInterval = setInterval(() => {
      // Keep alive - no longer polling status
    }, 30000); // Just keep interval alive every 30 seconds
  }

  handleStatusUpdate(status) {
    // Handle real-time status updates from background script
    this.updateStatus(status);
  }

  async loadStatus() {
    try {
      const response = await chrome.runtime.sendMessage({ type: 'GET_STATUS' });
      this.updateStatus(response);
    } catch (error) {
      console.error('Failed to load status:', error);
    }
  }

  handleLogUpdate(data) {
    // Add new log entry to local array
    if (data.logEntry) {
      this.logs.push(data.logEntry);

      // Keep only last 1000 logs in memory
      if (this.logs.length > 1000) {
        this.logs.splice(0, this.logs.length - 1000);
      }

      // Re-render logs immediately
      this.renderLogs();
    }
  }

  async loadLogs() {
    try {
      console.log('Requesting logs from background script...');

      // Try to get logs via message first
      let response;
      try {
        response = await chrome.runtime.sendMessage({ type: 'GET_LOGS' });
        console.log('Received logs response:', response);
      } catch (messageError) {
        console.log('Message failed, trying direct storage access:', messageError);
        response = null;
      }

      if (response && response.logs) {
        this.logs = Array.isArray(response.logs) ? response.logs : [];
        console.log('Loaded logs count via message:', this.logs.length);
      } else {
        // Fallback: read directly from storage
        console.log('Fallback: reading logs directly from storage...');
        try {
          const storageResult = await chrome.storage.local.get(['logs']);
          this.logs = Array.isArray(storageResult.logs) ? storageResult.logs : [];
          console.log('Loaded logs count via storage:', this.logs.length);
        } catch (storageError) {
          console.error('Storage access failed:', storageError);
          this.logs = [];
        }
      }

      this.renderLogs();
    } catch (error) {
      console.error('Failed to load logs:', error);
      this.logs = [];
      this.renderLogs();
    }
  }

  updateStatus(status) {
    // Update connection status
    const indicator = document.getElementById('statusIndicator');
    const statusText = document.getElementById('statusText');

    if (status.connected) {
      indicator.className = 'status-indicator connected';
      statusText.textContent = 'Connected';
    } else {
      indicator.className = 'status-indicator disconnected';
      statusText.textContent = 'Disconnected';
    }

    // Update client ID
    document.getElementById('clientId').textContent = status.clientId || '-';

    // Update page statistics - exclude console pages
    const pageStatuses = status.pageStatuses || {};
    const allPages = Object.values(pageStatuses);
    const pages = allPages.filter(page => {
      const url = page.url || '';
      return !url.includes('chrome-extension://') && !url.includes('cloudflare_monitor:');
    });
    const busyPages = pages.filter(page => page.status === 'busy');

    document.getElementById('activePages').textContent = pages.length;
    document.getElementById('busyPages').textContent = busyPages.length;

    // Update pages list
    this.renderPages(pageStatuses);
  }

  renderPages(pageStatuses) {
    const pagesList = document.getElementById('pagesList');
    const pages = Object.entries(pageStatuses);

    // Filter out console pages
    const filteredPages = pages.filter(([tabId, pageInfo]) => {
      const url = pageInfo.url || '';
      return !url.includes('chrome-extension://') && !url.includes('cloudflare_monitor:');
    });

    if (filteredPages.length === 0) {
      pagesList.innerHTML = '<div class="no-pages">No pages currently monitored</div>';
      return;
    }

    const pagesHtml = filteredPages.map(([tabId, pageInfo]) => {
      const statusClass = pageInfo.status === 'busy' ? 'busy' : 'idle';
      const lastUpdate = new Date(pageInfo.lastUpdate).toLocaleTimeString();

      return `
        <div class="page-item">
          <div class="page-url" title="${pageInfo.url}">${this.truncateUrl(pageInfo.url)}</div>
          <div class="page-status ${statusClass}">${pageInfo.status}</div>
          <div class="page-time">${lastUpdate}</div>
        </div>
      `;
    }).join('');

    pagesList.innerHTML = pagesHtml;
  }

  renderLogs() {
    const container = document.getElementById('logsContainer');

    let filteredLogs = this.logs;
    if (this.currentFilter !== 'all') {
      filteredLogs = this.logs.filter(log => log.level === this.currentFilter);
    }

    if (filteredLogs.length === 0) {
      container.innerHTML = '<div class="loading">No logs available</div>';
      return;
    }

    const logsHtml = filteredLogs
      .slice(-500) // Show last 500 logs
      .map(log => {
        const timestamp = new Date(log.timestamp).toLocaleTimeString();
        const args = log.args && log.args.length > 0 ? ' ' + log.args.join(' ') : '';

        return `
          <div class="log-entry">
            <span class="log-timestamp">${timestamp}</span>
            <span class="log-level ${log.level}">${log.level.toUpperCase()}</span>
            <span class="log-message">${this.escapeHtml(log.message + args)}</span>
          </div>
        `;
      })
      .join('');

    container.innerHTML = logsHtml;

    // Auto-scroll to bottom
    container.scrollTop = container.scrollHeight;
  }

  async clearLogs() {
    try {
      await chrome.storage.local.remove(['logs']);
      this.logs = [];
      this.renderLogs();
    } catch (error) {
      console.error('Failed to clear logs:', error);
    }
  }

  truncateUrl(url) {
    if (!url) return '';
    if (url.length <= 60) return url;
    return url.substring(0, 60) + '...';
  }

  escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  destroy() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }
}

// Handle special console URL
if (window.location.protocol === 'cloudflare_monitor:') {
  // This is the console page
  document.addEventListener('DOMContentLoaded', () => {
    new ConsoleManager();
  });
} else if (window.location.protocol !== 'chrome-extension:' && window.location.protocol !== 'extension:') {
  // This is a regular page (not already on chrome-extension://), redirect to chrome-extension://
  const extensionId = chrome.runtime.id;
  if (extensionId) {
    window.location.href = `chrome-extension://${extensionId}/console.html`;
  }
} else {
  // Already on chrome-extension://, just initialize
  document.addEventListener('DOMContentLoaded', () => {
    new ConsoleManager();
  });
}
