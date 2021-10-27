import 'package:get/get.dart';
import 'package:nfc_certification_app/controller/main_controller.dart';

class MainBinding extends Bindings {
  @override
  void dependencies() {
    // TODO: implement dependencies
    Get.lazyPut(() => MainController());
  }
}