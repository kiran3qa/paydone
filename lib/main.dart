import 'package:flutter/material.dart';
import 'screens/listening_control_screen.dart';

void main() {
  runApp(const PayAlertApp());
}

class PayAlertApp extends StatelessWidget {
  const PayAlertApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'PayAlert',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      home: const ListeningControlScreen(),
    );
  }
}
