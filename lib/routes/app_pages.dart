import 'package:get/get.dart';
import 'package:nfc_tag_app/bindings/MainBinding.dart';
import 'package:nfc_tag_app/routes/app_routes.dart';
import 'package:nfc_tag_app/ui/screen/main_app.dart';

class AppPages {
  static final pages = [
    GetPage(name: Routes.MAIN, page: () => MainApp(), bindings: [MainBinding()])
  ];
}