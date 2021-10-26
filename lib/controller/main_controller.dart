import 'dart:async';
import 'package:flutter/cupertino.dart';
import 'package:get/get.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:intl/intl.dart';

class MainController extends GetxController {
  Timer? _timer;
  var currDateString = "".obs;
  var currTimeString = "".obs;

  @override
  void onInit() {
    super.onInit();

    var locale = Localizations.localeOf(Get.context!);
    initializeDateFormatting(locale.toLanguageTag(), null);
    String languageTag = locale.toLanguageTag();

    _timer = Timer.periodic(Duration(milliseconds: 1000), (timer) {
      final DateTime now = DateTime.now();
      currDateString(DateFormat.yMMMMd(languageTag).format(now));
      currTimeString(DateFormat.jms(languageTag).format(now));
      // currDateString(DateFormat('yyyy년 MM월 dd일', languageTag).format(now));
      // currTimeString(DateFormat('aaa  hh  :  mm  :  ss', languageTag).format(now));
    });
  }

  @override
  void onClose() {
    super.onClose();
    _timer?.cancel();
  }
}
