import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';

class NativeCameraView extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    const viewType = 'scan_layout';
    final Map<String, dynamic> creationParams = <String, dynamic>{};

    return PlatformViewLink(
      viewType: viewType,
      surfaceFactory: (BuildContext context, PlatformViewController controller) {
        return AndroidViewSurface(
            controller: controller as AndroidViewController,
            hitTestBehavior: PlatformViewHitTestBehavior.opaque,
            gestureRecognizers: const <Factory<OneSequenceGestureRecognizer>>{}
            );
      },
      onCreatePlatformView: (PlatformViewCreationParams params) {
        return PlatformViewsService.initSurfaceAndroidView(
          id: params.id,
          viewType: viewType,
          layoutDirection: TextDirection.ltr,
          creationParams: creationParams,
          creationParamsCodec: StandardMessageCodec(),
          onFocus: () {
            params.onFocusChanged(true);
          },
        )
            ..addOnPlatformViewCreatedListener(params.onPlatformViewCreated)
            ..create();
      },
    );
  }
}