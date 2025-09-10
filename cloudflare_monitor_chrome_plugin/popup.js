// Popup script for Cloudflare Monitor

document.addEventListener('DOMContentLoaded', async () => {
  const statusIndicator = document.getElementById('statusIndicator');
  const statusText = document.getElementById('statusText');
  const clientIdSpan = document.getElementById('clientId');
  const activePagesSpan = document.getElementById('activePages');
  
  // Load current status
  try {
    const response = await chrome.runtime.sendMessage({ type: 'GET_STATUS' });
    
    if (response.connected) {
      statusIndicator.className = 'status-indicator connected';
      statusText.textContent = 'Connected';
    } else {
      statusIndicator.className = 'status-indicator disconnected';
      statusText.textContent = 'Disconnected';
    }
    
    clientIdSpan.textContent = response.clientId || '-';
    
    const pageCount = Object.keys(response.pageStatuses || {}).length;
    activePagesSpan.textContent = pageCount;
    
  } catch (error) {
    statusIndicator.className = 'status-indicator disconnected';
    statusText.textContent = 'Error';
    console.error('Failed to get status:', error);
  }
  
  // Open console button
  document.getElementById('openConsole').addEventListener('click', () => {
    chrome.tabs.create({
      url: chrome.runtime.getURL('console.html')
    });
    window.close();
  });
  
  // Reconnect button
  document.getElementById('reconnect').addEventListener('click', async () => {
    try {
      await chrome.runtime.sendMessage({ type: 'RECONNECT' });
      statusIndicator.className = 'status-indicator connecting';
      statusText.textContent = 'Reconnecting...';
    } catch (error) {
      console.error('Failed to reconnect:', error);
    }
  });
});