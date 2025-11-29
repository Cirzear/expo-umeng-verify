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

                // ⚠️ IMPORTANT: Use Scheme Secret (NOT AppKey) from Umeng console
                // 1. Go to Umeng+ Console: https://console.umeng.com/
                // 2. Go to 智能认证 → 认证管理 → Create authentication scheme
                // 3. Copy the Scheme Secret (密钥) - it's different from AppKey
                // 4. Make sure app package name and signature match your config
                // 5. Test with cellular data (4G/5G), NOT WiFi
                await ExpoUmengVerify.init('YOUR_SCHEME_SECRET_HERE', 'App Store');
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
                const result = await ExpoUmengVerify.getLoginToken();
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
