import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:zcs_bridge/zcs_bridge.dart';

void main() {
  runApp(const MyApp());
}

/// ZCS Bridge instance
final ZcsBridge _plugin = ZcsBridge();

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    final colorScheme = ColorScheme.fromSeed(
      seedColor: const Color(0xFF6C5CE7),
      brightness: Brightness.light,
    );

    return MaterialApp(
      title: 'ZCS Integrated Demo',
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: colorScheme,
        scaffoldBackgroundColor: colorScheme.surface,
        appBarTheme: AppBarTheme(
          backgroundColor: colorScheme.surface,
          foregroundColor: colorScheme.onSurface,
          elevation: 0,
          centerTitle: false,
        ),
      ),
      home: const DemoPage(),
    );
  }
}

enum _RunStatus { idle, running, success, error }

class _Action {
  final String label;
  final IconData icon;
  final Future<dynamic> Function() call;

  const _Action({required this.label, required this.icon, required this.call});
}

class DemoPage extends StatefulWidget {
  const DemoPage({super.key});

  @override
  State<DemoPage> createState() => _DemoPageState();
}

class _DemoPageState extends State<DemoPage> {
  _RunStatus _status = _RunStatus.idle;
  String _label = '';
  String _message = 'No commands run yet.';
  int _elapsedMs = 0;

  bool get _busy => _status == _RunStatus.running;

  @override
  void initState() {
    super.initState();
    _init();
  }

  Future<void> _init() async {
    try {
      await _plugin.getPlatformVersion();
    } catch (e) {
      setState(() {
        _status = _RunStatus.error;
        _label = 'init';
        _message = '$e';
      });
    }
  }

  Future<void> _run(Future<dynamic> Function() call, String label) async {
    setState(() {
      _status = _RunStatus.running;
      _label = label;
      _message = 'Running…';
    });
    final sw = Stopwatch()..start();
    try {
      final result = await call();
      if (!mounted) return;
      setState(() {
        _status = _RunStatus.success;
        _elapsedMs = sw.elapsedMilliseconds;
        _message = '${result ?? "void"}';
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _status = _RunStatus.error;
        _elapsedMs = sw.elapsedMilliseconds;
        _message = '$e';
      });
    }
  }

  Future<void> _printInvoiceWithLogo() async {
    final logo = (await rootBundle.load('assets/logo.png')).buffer.asUint8List();
    await _run(
      () => _plugin.printDynamic(
        bothCopies: false,
        {
          'logoBytes': logo,
          'businessName': 'YatriKart',
          'header': 'TEST RECEIPT',
          'fields': {
            'Date': DateTime.now().toString(),
            'Message': 'Hello World',
          },
          'footer': 'Thank you for testing!',
        },
      ),
      'printDynamic (Text With Logo)',
    );
  }

  List<_Action> get _deviceActions => [
        _Action(
          label: 'Initialize',
          icon: Icons.power_settings_new_rounded,
          call: () => _plugin.initializeDevice(),
        ),
        _Action(
          label: 'Open Device',
          icon: Icons.link_rounded,
          call: () => _plugin.openDevice(),
        ),
        _Action(
          label: 'Close Device',
          icon: Icons.link_off_rounded,
          call: () => _plugin.closeDevice(),
        ),
        _Action(
          label: 'Device Status',
          icon: Icons.info_outline_rounded,
          call: () => _plugin.getDeviceStatus(),
        ),
        _Action(
          label: 'Serial Number',
          icon: Icons.confirmation_number_outlined,
          call: () => _plugin.getSerialNumber(),
        ),
        _Action(
          label: 'Cash Drawer',
          icon: Icons.point_of_sale_rounded,
          call: () => _plugin.openCashBox(),
        ),
      ];

  List<_Action> get _printActions => [
        _Action(
          label: 'QR Code',
          icon: Icons.qr_code_rounded,
          call: () => _plugin.printDynamic({
            'businessName': 'ZCS Bridge',
            'header': 'QR CODE TEST',
            'qrCodeField': 'url',
            'url': 'https://example.com',
            'footer': 'Scan the QR code above',
          }),
        ),
        _Action(
          label: 'Text Receipt',
          icon: Icons.receipt_long_rounded,
          call: () => _plugin.printDynamic(
            bothCopies: true,
            {
              'businessName': 'ZCS Bridge',
              'header': 'TEST RECEIPT',
              'fields': {
                'Date': DateTime.now().toString(),
                'Message': 'Hello World',
              },
              'footer': 'Thank you for testing!',
            },
          ),
        ),
        _Action(
          label: 'Raw Text',
          icon: Icons.text_snippet_outlined,
          call: () => _plugin.printRawText(
            'Hello, World!\nThis is a test print.\nLine 3\nLine 4\n',
          ),
        ),
      ];

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: colorScheme.primaryContainer,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(Icons.print_rounded, color: colorScheme.onPrimaryContainer, size: 20),
            ),
            const SizedBox(width: 12),
            const Text('ZCS Bridge Demo', style: TextStyle(fontWeight: FontWeight.w700)),
          ],
        ),
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
          children: [
            _SectionHeader(title: 'Device Control'),
            const SizedBox(height: 12),
            _ActionGrid(
              actions: _deviceActions,
              busy: _busy,
              onTap: (a) => _run(a.call, a.label),
            ),
            const SizedBox(height: 28),
            _SectionHeader(title: 'Printing'),
            const SizedBox(height: 12),
            _ActionGrid(
              actions: _printActions,
              busy: _busy,
              onTap: (a) => _run(a.call, a.label),
              trailing: _ActionTile(
                icon: Icons.image_rounded,
                label: 'Invoice + Logo',
                busy: _busy,
                onTap: _printInvoiceWithLogo,
              ),
            ),
            const SizedBox(height: 28),
            _SectionHeader(title: 'Output'),
            const SizedBox(height: 12),
            _ConsoleCard(status: _status, label: _label, message: _message, elapsedMs: _elapsedMs),
          ],
        ),
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  final String title;
  const _SectionHeader({required this.title});

  @override
  Widget build(BuildContext context) {
    return Text(
      title.toUpperCase(),
      style: Theme.of(context).textTheme.labelLarge?.copyWith(
            letterSpacing: 1.1,
            color: Theme.of(context).colorScheme.primary,
            fontWeight: FontWeight.w700,
          ),
    );
  }
}

class _ActionGrid extends StatelessWidget {
  final List<_Action> actions;
  final bool busy;
  final void Function(_Action) onTap;
  final Widget? trailing;

  const _ActionGrid({
    required this.actions,
    required this.busy,
    required this.onTap,
    this.trailing,
  });

  @override
  Widget build(BuildContext context) {
    return GridView.count(
      crossAxisCount: 2,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      mainAxisSpacing: 12,
      crossAxisSpacing: 12,
      childAspectRatio: 2.4,
      children: [
        for (final action in actions)
          _ActionTile(
            icon: action.icon,
            label: action.label,
            busy: busy,
            onTap: () => onTap(action),
          ),
        if (trailing != null) trailing!,
      ],
    );
  }
}

class _ActionTile extends StatelessWidget {
  final IconData icon;
  final String label;
  final bool busy;
  final VoidCallback onTap;

  const _ActionTile({
    required this.icon,
    required this.label,
    required this.busy,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Material(
      color: colorScheme.surfaceContainerHigh,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: busy ? null : onTap,
        child: Opacity(
          opacity: busy ? 0.5 : 1,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 14),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: colorScheme.primary.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Icon(icon, size: 18, color: colorScheme.primary),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    label,
                    style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _ConsoleCard extends StatelessWidget {
  final _RunStatus status;
  final String label;
  final String message;
  final int elapsedMs;

  const _ConsoleCard({
    required this.status,
    required this.label,
    required this.message,
    required this.elapsedMs,
  });

  ({IconData icon, Color color, String text}) _statusInfo() {
    switch (status) {
      case _RunStatus.idle:
        return (icon: Icons.circle_outlined, color: const Color(0xFF9AA0B4), text: 'Idle');
      case _RunStatus.running:
        return (icon: Icons.autorenew_rounded, color: const Color(0xFFF5B84E), text: 'Running');
      case _RunStatus.success:
        return (icon: Icons.check_circle_rounded, color: const Color(0xFF4ADE80), text: 'Success');
      case _RunStatus.error:
        return (icon: Icons.error_rounded, color: const Color(0xFFF87171), text: 'Error');
    }
  }

  @override
  Widget build(BuildContext context) {
    final info = _statusInfo();

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFF14141F),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(info.icon, size: 16, color: info.color),
              const SizedBox(width: 8),
              Text(
                info.text,
                style: TextStyle(color: info.color, fontWeight: FontWeight.w700, fontSize: 12),
              ),
              if (label.isNotEmpty) ...[
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    label,
                    style: const TextStyle(color: Color(0xFF9AA0B4), fontSize: 12),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
              if (status != _RunStatus.idle && status != _RunStatus.running)
                Text(
                  '${elapsedMs}ms',
                  style: const TextStyle(color: Color(0xFF9AA0B4), fontSize: 12),
                ),
            ],
          ),
          const SizedBox(height: 10),
          SelectableText(
            message,
            style: const TextStyle(
              fontFamily: 'monospace',
              color: Color(0xFFE4E6EF),
              fontSize: 13,
              height: 1.4,
            ),
          ),
        ],
      ),
    );
  }
}
