import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ListeningControlScreen extends StatefulWidget {
  const ListeningControlScreen({Key? key}) : super(key: key);

  @override
  State<ListeningControlScreen> createState() => _ListeningControlScreenState();
}

class _ListeningControlScreenState extends State<ListeningControlScreen> {
  static const String _channel = 'pay_speaker/native';
  static const String _storageKey = 'listeningEnabled';

  late final MethodChannel _methodChannel;
  bool _isListeningEnabled = false;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _methodChannel = const MethodChannel(_channel);
    _initializeListeningState();
  }

  /// Initialize listening state from native storage on app start
  Future<void> _initializeListeningState() async {
    try {
      final bool enabled = await _methodChannel.invokeMethod<bool>(
        'getListeningEnabled',
      ) ?? false;

      setState(() {
        _isListeningEnabled = enabled;
        _isLoading = false;
      });
    } catch (e) {
      debugPrint('Error initializing listening state: $e');
      setState(() {
        _isLoading = false;
      });
    }
  }

  /// Handle TURN ON button press
  Future<void> _handleTurnOn() async {
    try {
      // 1. Save to native storage
      await _methodChannel.invokeMethod<void>(
        'setListeningEnabled',
        {'enabled': true},
      );

      // 2. Call setListeningEnabled on native module
      // (already done by setListeningEnabled method)

      // 3. Open notification access settings
      await _methodChannel.invokeMethod<void>(
        'openNotificationAccessSettings',
      );

      setState(() {
        _isListeningEnabled = true;
      });
    } catch (e) {
      debugPrint('Error turning on listening: $e');
      _showErrorSnackBar('Failed to turn on listening');
    }
  }

  /// Handle TURN OFF button press
  Future<void> _handleTurnOff() async {
    try {
      // 1. Save to native storage
      await _methodChannel.invokeMethod<void>(
        'setListeningEnabled',
        {'enabled': false},
      );

      // 2. Call setListeningEnabled on native module
      // (already done by setListeningEnabled method)

      setState(() {
        _isListeningEnabled = false;
      });
    } catch (e) {
      debugPrint('Error turning off listening: $e');
      _showErrorSnackBar('Failed to turn off listening');
    }
  }

  /// Handle OPEN NOTIFICATION ACCESS button press
  Future<void> _handleOpenNotificationAccess() async {
    try {
      await _methodChannel.invokeMethod<void>(
        'openNotificationAccessSettings',
      );
    } catch (e) {
      debugPrint('Error opening notification access: $e');
      _showErrorSnackBar('Failed to open notification settings');
    }
  }

  /// Handle TEST SPEAK button press
  Future<void> _handleTestSpeak() async {
    try {
      await _methodChannel.invokeMethod<void>(
        'testSpeak',
        {'text': 'Pay Speaker is ready'},
      );
    } catch (e) {
      debugPrint('Error calling test speak: $e');
      _showErrorSnackBar('Failed to test speak');
    }
  }

  /// Show error snackbar
  void _showErrorSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red,
        duration: const Duration(seconds: 2),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('PayAlert Listener'),
        centerTitle: true,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              child: Padding(
                padding: const EdgeInsets.all(32.0),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    // Status indicator
                    Container(
                      padding: const EdgeInsets.all(24.0),
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: _isListeningEnabled
                            ? Colors.green.shade100
                            : Colors.red.shade100,
                      ),
                      child: Icon(
                        _isListeningEnabled ? Icons.check_circle : Icons.cancel,
                        size: 80,
                        color: _isListeningEnabled
                            ? Colors.green.shade700
                            : Colors.red.shade700,
                      ),
                    ),
                    const SizedBox(height: 32),

                    // Status text
                    Text(
                      'Listening: ${_isListeningEnabled ? 'ON' : 'OFF'}',
                      style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                            fontWeight: FontWeight.bold,
                            color: _isListeningEnabled
                                ? Colors.green.shade700
                                : Colors.red.shade700,
                          ),
                    ),
                    const SizedBox(height: 48),

                    // TURN ON and TURN OFF buttons
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                      children: [
                        _buildLargeButton(
                          label: 'TURN ON',
                          onPressed: _handleTurnOn,
                          backgroundColor: Colors.green,
                          isActive: _isListeningEnabled,
                        ),
                        _buildLargeButton(
                          label: 'TURN OFF',
                          onPressed: _handleTurnOff,
                          backgroundColor: Colors.red,
                          isActive: !_isListeningEnabled,
                        ),
                      ],
                    ),
                    const SizedBox(height: 48),

                    // OPEN NOTIFICATION ACCESS button
                    _buildSmallButton(
                      label: 'OPEN NOTIFICATION ACCESS',
                      onPressed: _handleOpenNotificationAccess,
                      backgroundColor: Colors.blue,
                    ),
                    const SizedBox(height: 16),

                    // TEST SPEAK button
                    _buildSmallButton(
                      label: 'TEST SPEAK',
                      onPressed: _handleTestSpeak,
                      backgroundColor: Colors.purple,
                    ),
                  ],
                ),
              ),
            ),
    );
  }

  /// Build large button widget
  Widget _buildLargeButton({
    required String label,
    required VoidCallback onPressed,
    required Color backgroundColor,
    required bool isActive,
  }) {
    return Expanded(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12.0),
        child: ElevatedButton(
          onPressed: onPressed,
          style: ElevatedButton.styleFrom(
            backgroundColor: backgroundColor,
            foregroundColor: Colors.white,
            padding: const EdgeInsets.symmetric(vertical: 24.0),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12.0),
              side: BorderSide(
                color: isActive ? Colors.white : Colors.transparent,
                width: 3.0,
              ),
            ),
            elevation: isActive ? 8.0 : 2.0,
          ),
          child: Text(
            label,
            style: const TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              letterSpacing: 1.2,
            ),
          ),
        ),
      ),
    );
  }

  /// Build small button widget
  Widget _buildSmallButton({
    required String label,
    required VoidCallback onPressed,
    required Color backgroundColor,
  }) {
    return SizedBox(
      width: double.infinity,
      child: ElevatedButton(
        onPressed: onPressed,
        style: ElevatedButton.styleFrom(
          backgroundColor: backgroundColor,
          foregroundColor: Colors.white,
          padding: const EdgeInsets.symmetric(vertical: 16.0),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8.0),
          ),
        ),
        child: Text(
          label,
          style: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w600,
            letterSpacing: 0.5,
          ),
        ),
      ),
    );
  }
}
