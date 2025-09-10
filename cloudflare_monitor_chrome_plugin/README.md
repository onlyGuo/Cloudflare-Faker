# Cloudflare Monitor Chrome Extension

A Chrome extension that allows executing fetch scripts on Cloudflare-proxied pages via WebSocket commands.

## Features

- 🔌 **WebSocket Connection**: Connects to `ws://localhost:8080/ws` with automatic reconnection
- 🛡️ **Cloudflare Detection**: Automatically detects and waits for Cloudflare challenges to complete
- 📊 **Task Management**: Queues and executes fetch tasks with timeout handling
- 📈 **Real-time Dashboard**: Monitor connection status, page states, and logs
- 🔄 **Retry Logic**: Automatically retries on Cloudflare firewall triggers (up to 3 times)
- 📱 **Stream Support**: Supports both regular and streaming fetch responses

## Installation

1. Open Chrome and go to `chrome://extensions/`
2. Enable "Developer mode" in the top right
3. Click "Load unpacked" and select the `cloudflare_monitor_chrome_plugin` directory
4. The extension icon should appear in the toolbar

## Usage

### Basic Setup

1. **Start the server**: Make sure your Java WebSocket server is running on `localhost:8080`
2. **Open target pages**: Navigate to the Cloudflare-proxied pages you want to monitor
3. **Access console**: Click the extension icon and select "Open Console" or navigate to `cloudflare_monitor://console`

### WebSocket Protocol

#### Registration
The extension automatically registers with the server:
```json
{
  "type": "register",
  "clientId": "chrome-xxxxxxxxx-timestamp",
  "timestamp": 1234567890
}
```

#### Fetch Commands
Server sends fetch commands in this format:
```json
{
  "type": "fetch-command",
  "taskId": "task-uuid",
  "data": {
    "pageUrl": "https://example.com",
    "fetchUrl": "https://api.example.com/data",
    "method": "POST",
    "body": {"key": "value"},
    "stream": false
  }
}
```

#### Task Responses
Extension responds with:
```json
{
  "type": "task-response",
  "taskId": "task-uuid",
  "data": {
    "type": "object|stream",
    "data": "response data or stream line"
  }
}
```

#### Error Responses
For errors:
```json
{
  "type": "task-response",
  "taskId": "task-uuid",
  "data": {
    "error": {
      "type": "TIMEOUT|NO_MATCHING_TAB|CLOUDFLARE_FIREWALL|EXECUTION_ERROR",
      "message": "Error description"
    }
  }
}
```

#### Page Status Updates
Extension notifies server of page status changes:
```json
{
  "type": "page-status",
  "data": {
    "pageUrl": "https://example.com",
    "status": "idle|busy"
  }
}
```

## Cloudflare Challenge Handling

The extension automatically:
- Detects Cloudflare challenge pages
- Waits for challenges to complete
- Monitors page loading states
- Handles page refreshes during challenges
- Retries on firewall triggers with deep refresh

## Console Dashboard

Access via `cloudflare_monitor://console` to view:
- **Connection Status**: WebSocket connection state
- **Page Monitor**: List of monitored pages and their status
- **Task Statistics**: Number of executed tasks
- **Live Logs**: Real-time log stream with filtering

## Error Types

- `TIMEOUT`: Task preparation took longer than 20 seconds
- `NO_MATCHING_TAB`: No browser tab matches the specified pageUrl
- `CLOUDFLARE_FIREWALL`: Request triggered Cloudflare protection
- `EXECUTION_ERROR`: General execution error
- `TAB_COMMUNICATION_ERROR`: Failed to communicate with page content script
- `CONTENT_SCRIPT_ERROR`: Content script reported an error

## Reconnection Logic

- **First disconnect**: Immediate reconnection
- **1-3 failures**: 1 second delay
- **4-8 failures**: 3 second delay  
- **9+ failures**: 5 second delay
- **Unlimited retries** as long as browser is open

## Development

To modify the extension:
1. Make changes to the source files
2. Go to `chrome://extensions/`
3. Click the refresh icon on the extension card
4. Test your changes

## Troubleshooting

- **WebSocket won't connect**: Ensure the Java server is running on port 8080
- **Tasks not executing**: Check that target pages are actually open in tabs
- **Cloudflare issues**: Check console logs for challenge detection status
- **Performance**: Monitor the console dashboard for task queue status