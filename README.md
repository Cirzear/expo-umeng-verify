# expo-umeng-verify

An Expo module for Umeng Verify (友盟一键登录). This module allows you to integrate Umeng's one-click login service into your Expo application with support for both Android and iOS.

## Features

- 🚀 **Easy Integration**: Seamlessly works with Expo projects
- 📱 **Cross-Platform**: Supports both Android and iOS
- 🔧 **Config Plugin**: Includes an Expo Config Plugin for automatic native configuration
- ⚡ **One-Click Login**: Fast and secure mobile number verification
- 🎨 **Customizable UI**: Full control over authorization page appearance
- 📡 **Event System**: Subscribe to user interactions and UI events
- ⚙️ **Centralized Config**: Configure all settings in `app.config.ts`

## Installation

```bash
npm install expo-umeng-verify
```

## Configuration

### 1. Add Plugin to `app.config.ts`

Add `expo-umeng-verify` to the `plugins` section of your `app.config.ts`:

```typescript
export default {
  plugins: [
    [
      'expo-umeng-verify',
      {
        androidAppKey: 'YOUR_ANDROID_APP_KEY',
        iosAppKey: 'YOUR_IOS_APP_KEY',
        androidSchemeSecret: 'YOUR_ANDROID_SCHEME_SECRET',
        iosSchemeSecret: 'YOUR_IOS_SCHEME_SECRET',
        channel: 'App Store', // Optional: distribution channel
      },
    ],
  ],
};
```

**Important:** You need **two different keys** from Umeng:
- **AppKey**: Get this from 友盟+ Console → My Products → Your App → AppKey
- **Scheme Secret**: Get this from 智能认证 Console → 认证管理 → Create Authentication Scheme → Copy Secret

### 2. Rebuild Your App

After adding the plugin, rebuild your app to apply native configurations:

```bash
# For development builds
npx expo prebuild --clean
npx expo run:android
npx expo run:ios
```

## Usage

### Basic Example

```typescript
import * as ExpoUmengVerify from 'expo-umeng-verify';
import Constants from 'expo-constants';
import { Platform } from 'react-native';

// 1. Get configuration from app.config.ts
const config = Constants.expoConfig?.extra?.umengVerify;
const appKey = Platform.select({
  android: config?.androidAppKey,
  ios: config?.iosAppKey,
});
const schemeSecret = Platform.select({
  android: config?.androidSchemeSecret,
  ios: config?.iosSchemeSecret,
});
const channel = config?.channel || 'default';

// 2. Initialize the SDK
await ExpoUmengVerify.init(appKey, schemeSecret, channel);

// 3. Check if environment supports one-click login
const isAvailable = await ExpoUmengVerify.checkEnvAvailable();
if (!isAvailable) {
  console.log('One-click login not available');
  return;
}

// 4. Optional: Pre-load auth page resources
await ExpoUmengVerify.accelerateLoginPage();

// 5. Get login token
try {
  const result = await ExpoUmengVerify.getLoginToken({
    ui: {
      privacy: {
        privacyOne: {
          name: '用户协议',
          url: 'https://example.com/terms',
        },
        privacyTwo: {
          name: '隐私政策',
          url: 'https://example.com/privacy',
        },
      },
    },
    timeout: 5000,
  });
  
  console.log('Token:', result.token);
  // Send token to your backend for verification
} catch (error) {
  console.error('Login failed:', error);
}
```

### Event Handling

Subscribe to UI events to handle user interactions:

```typescript
import { useEffect } from 'react';

useEffect(() => {
  const subscription = ExpoUmengVerify.addEventListener((event) => {
    switch (event.type) {
      case 'onUserCancel':
        console.log('User cancelled login');
        break;
        
      case 'onSwitchAccount':
        console.log('User wants to switch login method');
        break;
        
      case 'onLoginButtonClick':
        console.log('Login button clicked, checkbox:', event.data?.isChecked);
        break;
        
      case 'onCheckboxChange':
        console.log('Privacy checkbox changed:', event.data?.isChecked);
        break;
        
      case 'onProtocolClick':
        console.log('Privacy policy clicked:', event.data?.name, event.data?.url);
        break;
    }
  });

  // Cleanup
  return () => {
    ExpoUmengVerify.removeEventListener(subscription);
  };
}, []);
```

## API Reference

### `init(appKey: string, schemeSecret: string, channel: string): Promise<boolean>`

Initializes the Umeng Verify SDK.

- `appKey`: Your Umeng App Key
- `schemeSecret`: Your Scheme Secret for verification
- `channel`: Distribution channel (e.g., 'App Store', 'Google Play')
- Returns: `true` if initialization is successful

### `checkEnvAvailable(): Promise<boolean>`

Checks if the current environment supports one-click login.

- Returns: `true` if SIM card and network environment support verification

### `getLoginToken(config?: GetLoginTokenConfig): Promise<TokenResult>`

Triggers the one-click login process and returns the token.

**Parameters:**
- `config.ui`: UI configuration options (see below)
- `config.timeout`: Timeout in milliseconds (default: 5000)

**Returns:**
```typescript
{
  token: string;      // Login token
  code?: string;      // Result code
  operator?: string;  // Operator type (CMCC, CUCC, CTCC)
}
```

### `accelerateLoginPage(): Promise<void>`

Pre-fetches resources to speed up authorization page display.

### `quitLoginPage(): Promise<void>`

Closes the authorization page programmatically.

### `addEventListener(handler: UMVerifyEventHandler): EventSubscription`

Adds an event listener for UI interactions.

### `removeEventListener(subscription: EventSubscription): void`

Removes an event listener.

## UI Configuration

### Full UI Configuration Example

```typescript
const result = await ExpoUmengVerify.getLoginToken({
  ui: {
    // Privacy policies
    privacy: {
      privacyOne: { name: 'Terms', url: 'https://...' },
      privacyTwo: { name: 'Privacy', url: 'https://...' },
      privacyThree: { name: 'Service Agreement', url: 'https://...' },
      privacyColor: {
        normal: '#888888',
        clickable: '#1890ff',
      },
      privacyConnectTexts: ['、', '和', ''],
      privacyState: false, // Checkbox default state
      vendorPrivacyPrefix: '《',
      vendorPrivacySuffix: '》',
    },
    
    // Logo
    logo: {
      imagePath: 'app_logo', // Android: drawable name, iOS: asset name
      width: 60,
      height: 60,
      offsetY: 100,
      hidden: false,
    },
    
    // Phone number display
    phoneNumber: {
      textSize: 18,
      textColor: '#333333',
      offsetY: 200,
    },
    
    // Slogan
    slogan: {
      text: 'Welcome to our app',
      textSize: 12,
      textColor: '#999999',
      offsetY: 180,
      hidden: false,
    },
    
    // Login button
    loginButton: {
      text: 'One-Click Login',
      textSize: 16,
      textColor: '#ffffff',
      width: 300,
      height: 50,
      offsetY: 280,
      backgroundColor: '#1890ff',
      cornerRadius: 25,
    },
    
    // Switch account button
    switchButton: {
      text: 'Use other login method',
      textSize: 14,
      textColor: '#1890ff',
      hidden: false,
      offsetY: 350,
    },
    
    // Checkbox
    checkbox: {
      hidden: false,
      defaultState: false,
    },
    
    // Navigation bar
    navigationBar: {
      title: 'Login',
      titleColor: '#333333',
      backgroundColor: '#ffffff',
      hidden: false,
      returnButtonHidden: false,
    },
    
    // Status bar
    statusBar: {
      color: 'transparent',
      lightColor: true,
    },
    
    // Page
    page: {
      backgroundColor: '#ffffff',
      orientation: 'portrait',
    },
  },
  timeout: 5000,
});
```

### UI Configuration Options

#### Privacy Configuration

| Option | Type | Description |
|--------|------|-------------|
| `privacyOne` | `{name: string, url: string}` | First custom privacy policy |
| `privacyTwo` | `{name: string, url: string}` | Second custom privacy policy |
| `privacyThree` | `{name: string, url: string}` | Third custom privacy policy |
| `privacyColor.normal` | `string` | Normal text color (hex) |
| `privacyColor.clickable` | `string` | Clickable text color (hex) |
| `privacyConnectTexts` | `[string, string, string]` | Connector texts between policies |
| `privacyState` | `boolean` | Default checkbox state |
| `vendorPrivacyPrefix` | `string` | Prefix for operator privacy (e.g., "《") |
| `vendorPrivacySuffix` | `string` | Suffix for operator privacy (e.g., "》") |

#### Logo Configuration

| Option | Type | Description |
|--------|------|-------------|
| `imagePath` | `string` | Logo image (drawable name for Android, asset for iOS) |
| `width` | `number` | Logo width in dp |
| `height` | `number` | Logo height in dp |
| `offsetY` | `number` | Offset from top in dp |
| `hidden` | `boolean` | Hide logo |

#### Login Button Configuration

| Option | Type | Description |
|--------|------|-------------|
| `text` | `string` | Button text |
| `textSize` | `number` | Text size in sp |
| `textColor` | `string` | Text color (hex) |
| `width` | `number` | Button width in dp |
| `height` | `number` | Button height in dp |
| `backgroundColor` | `string` | Background color (hex) |
| `cornerRadius` | `number` | Corner radius in dp |
| `offsetY` | `number` | Offset from top in dp |

#### Status Bar Configuration

| Option | Type | Description |
|--------|------|-------------|
| `color` | `string` | Status bar color (hex) |
| `lightColor` | `boolean` | Use light status bar (white icons) |

## Event Types

### `onUserCancel`

Triggered when user clicks back or close button.

```typescript
{
  type: 'onUserCancel'
}
```

### `onSwitchAccount`

Triggered when user wants to switch to other login methods.

```typescript
{
  type: 'onSwitchAccount'
}
```

### `onLoginButtonClick`

Triggered when user clicks the login button.

```typescript
{
  type: 'onLoginButtonClick',
  data: {
    isChecked: boolean // Privacy checkbox state
  }
}
```

### `onCheckboxChange`

Triggered when privacy checkbox state changes.

```typescript
{
  type: 'onCheckboxChange',
  data: {
    isChecked: boolean
  }
}
```

### `onProtocolClick`

Triggered when user clicks a privacy policy link.

```typescript
{
  type: 'onProtocolClick',
  data: {
    name: string,  // Policy name
    url: string    // Policy URL
  }
}
```

## Platform Support

### Android

- **Minimum SDK**: API 21 (Android 5.0)
- **Required Permissions**: Automatically added by config plugin
  - `INTERNET`
  - `ACCESS_NETWORK_STATE`
  - `ACCESS_WIFI_STATE`
  - `CHANGE_NETWORK_STATE`

### iOS

- **Minimum iOS**: 10.0
- **Required Frameworks**: Automatically linked

## Troubleshooting

### TypeScript Errors After Installation

If you see TypeScript errors in your app after updating the module, rebuild the project:

```bash
# Clear cache and rebuild
rm -rf node_modules
npm install
npx expo prebuild --clean
```

### Token Verification Failed

Make sure to:
1. Use the correct **Scheme Secret** (not App Key) for `setAuthSDKInfo`
2. Send the token to your backend within the validity period
3. Verify the token using Umeng's server-side API

### Authorization Page Not Showing

Check that:
1. Device has an active SIM card
2. Network connection is available
3. `checkEnvAvailable()` returns `true`
4. App Key and Scheme Secret are correct

## Migration from Previous Versions

### Version 0.1.x to Latest

The API has been significantly enhanced. Key changes:

1. **Config Structure**: 
   ```typescript
   // Old
   getLoginToken({
     privacyOneName: 'Terms',
     privacyOneUrl: 'https://...',
   })
   
   // New
   getLoginToken({
     ui: {
       privacy: {
         privacyOne: { name: 'Terms', url: 'https://...' }
       }
     }
   })
   ```

2. **Event Handling**: New `addEventListener` API
3. **Centralized Config**: Use `app.config.ts` for keys

## Contributing

Contributions are welcome! Please see the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

## Links

- [Umeng Official Documentation](https://developer.umeng.com/docs/119267/detail/119275)
- [GitHub Repository](https://github.com/Cirzear/expo-umeng-verify)
- [NPM Package](https://www.npmjs.com/package/expo-umeng-verify)