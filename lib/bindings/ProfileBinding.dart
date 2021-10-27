import 'package:get/get.dart';
import 'package:nfc_certification_app/controller/profile_controller.dart';

class ProfileBinding extends Bindings {
  @override
  void dependencies() {
    // TODO: implement dependencies
    Get.lazyPut(() => ProfileController());
  }
}