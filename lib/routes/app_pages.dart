import 'package:get/get.dart';
import 'package:nfc_certification_app/bindings/MainBinding.dart';
import 'package:nfc_certification_app/bindings/ProfileBinding.dart';
import 'package:nfc_certification_app/routes/app_routes.dart';
import 'package:nfc_certification_app/ui/screen/main_app.dart';
import 'package:nfc_certification_app/ui/screen/profile_app.dart';

class AppPages {
  static final pages = [
    GetPage(name: Routes.MAIN, page: () => MainApp(), bindings: [MainBinding()]),
    GetPage(name: Routes.PROFILE, page: () => ProfileApp(), bindings: [ProfileBinding()])
  ];
}