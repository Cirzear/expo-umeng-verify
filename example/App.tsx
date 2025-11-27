import { useEvent } from 'expo';
import * as ExpoUmengVerify from 'expo-umeng-verify';
import { Button, SafeAreaView, ScrollView, Text, View } from 'react-native';

export default function App() {
  return (
    <SafeAreaView style={styles.container}>
      <ScrollView style={styles.container}>
        <Text style={styles.header}>Umeng Verify Example</Text>
        <Group name="Actions">
          <Button
            title="Initialize"
            onPress={async () => {
              try {
                // Replace with your AppKey
                await ExpoUmengVerify.init('YOUR_APP_KEY', 'App Store');
                alert('Initialized');
              } catch (e: any) {
                alert('Error: ' + e.message);
              }
            }}
          />
          <View style={{ height: 10 }} />
          <Button
            title="Get Login Token"
            onPress={async () => {
              try {
                const result = await ExpoUmengVerify.getLoginToken();
                console.log(result);
                alert('Success: ' + JSON.stringify(result));
              } catch (e: any) {
                 console.error(e);
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
