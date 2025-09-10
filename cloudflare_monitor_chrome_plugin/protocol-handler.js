// Override page script to handle  cloudflare_monitor://console URL

// Check if this is the special console URL
if (window.location.href.startsWith(' cloudflare_monitor://console')) {
  // Redirect to the extension's console page
  window.location.href = chrome.runtime.getURL('console.html');
}
