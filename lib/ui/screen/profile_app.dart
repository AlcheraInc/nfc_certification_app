import 'package:flutter/material.dart';
import 'package:flutter/src/widgets/framework.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:flutter_svg/svg.dart';
import 'package:get/get.dart';
import 'package:intl/intl.dart';
import 'package:nfc_certification_app/controller/profile_controller.dart';
import 'package:nfc_certification_app/routes/app_routes.dart';
import 'package:nfc_certification_app/ui/platform_views/native_camera_view.dart';

class ProfileApp extends GetView<ProfileController> {

  @override
  Widget build(BuildContext context) {
    final headerHeightWeight = 0.15;
    return Scaffold(
      body: Container(
        child: Column(
          children: [
            Image(
              height: Get.height * headerHeightWeight,
              image: AssetImage("assets/images/img_alchera_logo.png")
            ),
            Stack(
                children: [
                  Container(
                    width: Get.width,
                    height: Get.height * (1.0 - headerHeightWeight),
                    child: Column(
                        mainAxisAlignment: MainAxisAlignment.start,
                        crossAxisAlignment: CrossAxisAlignment.center,
                        children: [
                          Expanded(
                            flex: 4,
                            child: Image(
                              width: Get.width,
                              height: Get.height * 0.5,
                              fit: BoxFit.cover,
                              image: AssetImage("assets/images/img_sample_profile.png"),
                            ),
                          ),
                          Expanded(
                            flex: 3,
                            child: Image(
                              width: Get.width,
                              height: Get.height * 0.33,
                              fit: BoxFit.cover,
                              image: AssetImage("assets/images/profile_bg.png"),
                            ),
                          ),
                        ]
                    )
                  ),
                  Container(
                    width: Get.width,
                    height: Get.height * (1.0 - headerHeightWeight),
                    child: Column(
                      children: [
                        const Expanded(flex: 5, child: SizedBox()),
                        const Expanded(flex: 6,
                            child: _ProfileInfoPage(workerId: "AL941014", workerName: "박재훈", workerDepartment: "Product Development Div.", workerType: "CTO",)
                        )
                      ],
                    ),
                  )
                ]
            ),
          ]
        )
      )
    );
  }
}

class _ProfileInfoPage extends StatelessWidget {

  final String workerId;
  final String workerName;
  final String workerDepartment;
  final String workerType;

  const _ProfileInfoPage({Key? key, required this.workerId, required this.workerName, required this.workerDepartment, required this.workerType}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Expanded(flex: 2,
          child: SizedBox(
            width: Get.width * 0.85,
            child:
            GestureDetector(
              onTapUp: (tapUpDetails) {
                Get.toNamed(Routes.MAIN);
              },
              child: Stack(
                children: [
                  SvgPicture.asset(
                    "assets/images/profile_title.svg",
                    fit: BoxFit.fill,
                  ),
                  Center(child: Text(workerName, style: const TextStyle(color: Color(0xff1D1D1D), fontSize: 35, fontWeight: FontWeight.w700),))
                ]
              ),
            )
          ),
        ),
        Expanded(flex: 2,
            child: Container(
              width: Get.width * 0.8,
              child: Row(
                children: [
                  Expanded(flex: 1, child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      Text("사번", style: TextStyle(color: Color(0xffF7F7F7).withOpacity(0.7), fontSize: 15)),
                      Text("소속", style: TextStyle(color: Color(0xffF7F7F7).withOpacity(0.7), fontSize: 15)),
                      Text("직책", style: TextStyle(color: Color(0xffF7F7F7).withOpacity(0.7), fontSize: 15)),
                    ],
                  )),
                  Expanded(flex: 3, child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(workerId, style: TextStyle(color: Color(0xffFFFFFF), fontSize: 15)),
                      Text(workerDepartment, overflow: TextOverflow.ellipsis, style: TextStyle(color: Color(0xffFFFFFF), fontSize: 15)),
                      Text(workerType, style: TextStyle(color: Color(0xffFFFFFF), fontSize: 15)),
                    ],
                  )),
                ],
              ),
            )
        ),
        Expanded(flex: 3,
          child: Container(
            width: Get.width * 0.975,
            child: Stack(
              alignment: Alignment.center,
              children: [
                SvgPicture.asset(
                  "assets/images/profile_barcode_bg.svg",
                  fit: BoxFit.fill,
                ),
                Container(
                  width: Get.width * 0.7,
                  height: Get.height * 0.15,
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      Spacer(),
                      SvgPicture.asset(
                        "assets/images/img_barcode.svg",
                        height: Get.height * 0.1,
                      ),
                      Spacer(),
                      Text("등록일   ${DateFormat("yyyy.MM.dd").format(DateTime.now())}"),
                    ],
                  )
                )
              ]
          ),
        )),
      ],
    );
  }
}