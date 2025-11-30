import { useEvent } from 'expo';
import * as ExpoUmengVerify from 'expo-umeng-verify';
import { Button, SafeAreaView, ScrollView, Text, View, PermissionsAndroid, Platform } from 'react-native';

export default function App() {
  return (
    <SafeAreaView style={styles.container}>
      <ScrollView style={styles.container}>
        <Text style={styles.header}>Umeng Verify Example</Text>
        <Group name="Actions">
          <Button
            title="Initialize"
            onPress={async () => {
              console.log('Initialize pressed');
              try {
                if (Platform.OS === 'android') {
                  const granted = await PermissionsAndroid.request(
                    PermissionsAndroid.PERMISSIONS.READ_PHONE_STATE
                  );
                  console.log('Permission status:', granted);
                }

                // ⚠️ IMPORTANT: You need TWO keys from Umeng console
                // 1. AppKey: From 友盟+ Console → My Products → Your App → AppKey
                // 2. Scheme Secret: From 智能认证 Console → 认证管理 → Create/View Scheme → Copy Secret
                // 
                // Steps to get Scheme Secret:
                // - Go to https://console.umeng.com/
                // - Navigate to 智能认证 → 认证管理
                // - Create authentication scheme (if not exists)
                // - Configure app package name and signature
                // - Copy the Scheme Secret (密钥)
                //
                // Test Requirements:
                // - Use cellular data (4G/5G), NOT WiFi
                // - Grant READ_PHONE_STATE permission
                await ExpoUmengVerify.init(
                  'YOUR_APP_KEY_HERE',      // AppKey from Umeng console
                  'YOUR_SCHEME_SECRET_HERE', // Scheme Secret from authentication scheme
                  'App Store'                // Channel name
                );
                console.log('Initialized success');
                alert('Initialized');
              } catch (e: any) {
                console.error('Initialize error:', e);
                alert('Error: ' + e.message);
              }
            }}
          />
          <View style={{ height: 10 }} />
          <Button
            title="Check Env"
            onPress={async () => {
              console.log('Check Env pressed');
              try {
                const result = await ExpoUmengVerify.checkEnvAvailable();
                console.log('Check Env result:', result);
                alert('Env Available: ' + result);
              } catch (e: any) {
                console.error('Check Env error:', e);
                alert('Error: ' + e.message);
              }
            }}
          />
          <View style={{ height: 10 }} />
          <Button
            title="Get Login Token"
            onPress={async () => {
              console.log('Get Login Token pressed');
              try {
                const result = await ExpoUmengVerify.getLoginToken({
                  privacyOneName: '《Privacy Policy》',
                  privacyOneUrl: 'https://www.example.com'
                });
                console.log('Get Login Token result:', result);
                alert('Success: ' + JSON.stringify(result));
              } catch (e: any) {
                 console.error('Get Login Token error:', e);
                alert('Error: ' + e.message);
              }
            }}
          />
        </Group>
      </ScrollView>
    </SafeAreaView>
  );
}

function Group(props: { name: string; children: React.ReactNode }) {
  return (
    <View style={styles.group}>
      <Text style={styles.groupHeader}>{props.name}</Text>
      {props.children}
    </View>
  );
}

const styles = {
  header: {
    fontSize: 30,
    margin: 20,
  },
  groupHeader: {
    fontSize: 20,
    marginBottom: 20,
  },
  group: {
    margin: 20,
    backgroundColor: '#fff',
    borderRadius: 10,
    padding: 20,
  },
  container: {
    flex: 1,
    backgroundColor: '#eee',
  },
  view: {
    flex: 1,
    height: 200,
  },
};
