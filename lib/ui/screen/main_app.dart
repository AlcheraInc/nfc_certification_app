import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/src/widgets/framework.dart';
import 'package:get/get.dart';
import 'package:nfc_tag_app/controller/main_controller.dart';

const double logoSizeWidth = 100;
const double logoSizeHeight = 83.67;

const double logoTitleSizeWidth = 209.79;
const double logoTitleSizeHeight = 32.68;

const double logoTextSizeWidth = 218;
const double logoTextSizeHeight = 66;

class MainApp extends GetView<MainController> {

  @override
  Widget build(BuildContext context) {

    return Scaffold(
      body: Container(
        decoration: BoxDecoration(
          color: Color(0xffF7F7F7),
          image: DecorationImage(
              image: AssetImage(
                  "assets/images/main_bg.png"
              ),
              fit: BoxFit.cover
          ),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
                width: Get.width * 0.8,
                height: Get.height * 0.85,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    SizedBox(height: Get.height * 0.075),
                    SizedBox(
                      child: Text('정부청사\n출입관리시스템', style: const TextStyle(fontSize: 35),),
                    ),

                    SizedBox(height: Get.height * 0.075),
                    SizedBox(
                      child: Obx(() => Text(
                        '${controller.currDateString.value}',
                        style: const TextStyle(fontSize: 25, color: Color(0xff003668)),)) ,
                    ),

                    SizedBox(height: Get.height * 0.02),
                    SizedBox(
                      child: Container(
                        alignment: Alignment.center,
                        width: Get.width,
                        height: Get.height * 0.1,
                        decoration: BoxDecoration(
                          color: Color(0xff003668),
                          boxShadow: [
                            BoxShadow(
                              color: Color(0x40000000),
                              offset: Offset(0, 10),
                              blurRadius: 50
                            )
                          ]
                        ),
                        child: Obx(() => Text(
                          '${controller.currTimeString.value}',
                          style: const TextStyle(fontSize: 30, color: Colors.white,),)) ,
                      )
                    ),

                    SizedBox(height: Get.height * 0.02),
                    SizedBox(
                      child: RichText(
                        text: TextSpan(
                          style: const TextStyle(fontSize: 20, color: Color(0xff575757), fontWeight: FontWeight.w500),
                          children: <TextSpan>[
                            TextSpan(text: '얼굴인식 출입관리 모드 '),
                            TextSpan(text: 'OFF', style: const TextStyle(color: Colors.red),),
                          ]
                        )
                      )
                    ),
                  ]
                )
            ),
            Container(
              height: Get.height * 0.15,
              decoration: BoxDecoration(color: Colors.white),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  SizedBox(),
                  SizedBox(width: Get.width * 0.25, child: Image(image: AssetImage("assets/images/img_gbmo_logo.png"))),
                  SizedBox(width: Get.width * 0.25, child: Image(image: AssetImage("assets/images/img_alchera_logo.png"))),
                  SizedBox(),
                ],
              ),
            )
          ],
        ),
      )
    );
  }
}