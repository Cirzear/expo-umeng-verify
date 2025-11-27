# expo-umeng-verify

An Expo module for Umeng Verify (One-Click Login / 友盟一键登录). This module allows you to integrate Umeng's verification service into your Expo application with support for both Android and iOS.

## Features

- 🚀 **Easy Integration**: Seamlessly works with Expo projects.
- 📱 **Cross-Platform**: Supports both Android and iOS.
- 🔧 **Config Plugin**: Includes an Expo Config Plugin to automatically handle Android network security configurations.
- ⚡ **One-Click Login**: Fast and secure mobile number verification.

## Installation

```bash
npm install expo-umeng-verify
```

## Configuration

Add `expo-umeng-verify` to the `plugins` section of your `app.json` or `app.config.js` file. This is required to configure the necessary Android network security settings.

```json
{
  "expo": {
    "plugins": [
      "expo-umeng-verify"
    ]
  }
}
```

## Usage

### 1. Import the library

```typescript
import * as ExpoUmengVerify from 'expo-umeng-verify';
```

### 2. Initialize the SDK

Initialize the SDK with your Umeng App Key and Channel.

```typescript
// Replace with your actual App Key and Channel
await ExpoUmengVerify.init('YOUR_APP_KEY', 'YOUR_CHANNEL');
```

### 3. Get Login Token

Call `getLoginToken` to start the verification process and retrieve the login token.

```typescript
try {
  const result = await ExpoUmengVerify.getLoginToken();
  console.log('Login Token:', result);
} catch (error) {
  console.error('Verification failed:', error);
}
```

### 4. Other Methods

- **Accelerate Login Page**: Pre-load the login page resources.
  ```typescript
  await ExpoUmengVerify.accelerateLoginPage();
  ```

- **Quit Login Page**: Close the authorization page programmatically.
  ```typescript
  await ExpoUmengVerify.quitLoginPage();
  ```

## API Reference

### `init(appKey: string, channel: string): Promise<boolean>`

Initializes the Umeng Verify SDK.

- `appKey`: Your Umeng App Key.
- `channel`: The distribution channel (e.g., 'App Store', 'Google Play').
- Returns: A promise that resolves to `true` if initialization is successful.

### `getLoginToken(): Promise<any>`

Triggers the one-click login process and returns the token upon success.

- Returns: A promise that resolves with the token object or rejects with an error.

### `accelerateLoginPage(): Promise<any>`

Accelerates the login page loading process.

### `quitLoginPage(): Promise<void>`

Closes the login authorization page.

## Contributing

Contributions are welcome! Please see the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT