import 'dart:async';
import 'dart:developer';

import 'package:apklis_license_validator/apklis_license_payment_status.dart';
import 'package:apklis_license_validator/apklis_license_validator.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const ExampleApp());
}

class ExampleApp extends StatefulWidget {
  const ExampleApp({super.key});

  @override
  ExampleAppState createState() => ExampleAppState();
}

class ExampleAppState extends State<ExampleApp> {
  final keyForm = GlobalKey<FormState>();
  final packageIdController = TextEditingController(
    text: 'com.example.app',
  );
  final licenseIdController = TextEditingController(
    // Esta ya esta pagada para alaincj y el device:
    // gRGmIfx53dRcCRDU0MhOlTpZ0rlh4jZEvmRn0+21+KDPBsNHhhNEIohNiyhLdKHGlNMpEAiJDwpaxSNa+fHML/qjso4hyUqkPoPeKwwlQV8GWOWiPcXiECxnIb1aa658
    text: '70dd68bf-c0bf-4c10-b76f-5d906b8bb945',
  );
  ApklisLicensePaymentStatus? status;

  Future<void> buyLicense(
    String licenseId,
  ) async {
    try {
      final status = await ApklisLicenseValidator.purchaseLicense(licenseId);
      setState(() => this.status = status);
    } on PlatformException catch (e) {
      log(e.toString());
    }
  }

  Future<void> verifyCurrentLicense(String packageId) async {
    try {
      final status = await ApklisLicenseValidator.verifyUserLicense(packageId);
      setState(() => this.status = status);
    } on PlatformException catch (e) {
      log(e.toString());
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          centerTitle: true,
          title: const Text('Apklis License Validator'),
        ),
        body: Form(
          key: keyForm,
          autovalidateMode: AutovalidateMode.always,
          child: Column(
            children: [
              Container(
                margin:
                    const EdgeInsets.symmetric(vertical: 15, horizontal: 10),
                child: Row(
                  children: [
                    Expanded(
                      child: TextFormField(
                        controller: packageIdController,
                        decoration: const InputDecoration(
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.all(
                              Radius.circular(10.0),
                            ),
                          ),
                          labelText: 'Package Id',
                          hintText: 'cu.uci.android.apklis',
                        ),
                        keyboardType: TextInputType.text,
                        validator: (value) {
                          if (value == null || value.isEmpty) {
                            return 'Is required';
                          }
                          return null;
                        },
                      ),
                    ),
                  ],
                ),
              ),
              Container(
                margin:
                    const EdgeInsets.symmetric(vertical: 15, horizontal: 10),
                child: Row(
                  children: [
                    Expanded(
                      child: TextFormField(
                        controller: licenseIdController,
                        decoration: const InputDecoration(
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.all(
                              Radius.circular(10.0),
                            ),
                          ),
                          labelText: 'License Id',
                          hintText: '3a640386-7ebf-477a-8231-f627f69536de',
                        ),
                        keyboardType: TextInputType.text,
                        validator: (value) {
                          if (value == null || value.isEmpty) {
                            return 'Is required';
                          }
                          return null;
                        },
                      ),
                    ),
                  ],
                ),
              ),
              if (status != null)
                Column(
                  children: [
                    //Error
                    if (status!.error != null)
                      Column(
                        children: [
                          Container(
                            margin: const EdgeInsets.all(5),
                            child: const Text('Error from License Plugin:'),
                          ),
                          Container(
                            margin: const EdgeInsets.all(5),
                            child: Text(
                              status!.statusCode != null
                                  ? '${status!.statusCode}: ${status!.error!}'
                                  : status!.error!,
                              style: const TextStyle(
                                fontWeight: FontWeight.bold,
                                fontSize: 20,
                              ),
                              textAlign: TextAlign.center,
                            ),
                          ),
                        ],
                      ),

                    //Values
                    Container(
                      margin: const EdgeInsets.all(5),
                      child: const Text('Username registered in Apklis:'),
                    ),
                    Container(
                      margin: const EdgeInsets.all(5),
                      child: Text(
                        status!.username ?? 'Unknown',
                        style: const TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 20,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ),
                    Container(
                      margin: const EdgeInsets.all(5),
                      child: const Text('License payment status:'),
                    ),

                    if (status!.license != null)
                      Container(
                        margin: const EdgeInsets.all(5),
                        child: Text(
                          status!.license ?? '',
                          style: const TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 20,
                          ),
                          textAlign: TextAlign.center,
                        ),
                      ),
                    Container(
                      margin: const EdgeInsets.all(5),
                      child: Text(
                        status!.paid.toString(),
                        style: const TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 20,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ),
                  ],
                ),
            ],
          ),
        ),
        floatingActionButtonLocation: FloatingActionButtonLocation.endFloat,
        floatingActionButton: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            FloatingActionButton(
              onPressed: () {
                if (keyForm.currentState!.validate()) {
                  final packageId = packageIdController.text.trim();
                  verifyCurrentLicense(packageId);
                }
              },
              child: const Icon(Icons.verified_outlined),
            ),
            const SizedBox(
              height: 10,
            ),
            FloatingActionButton(
              onPressed: () {
                if (keyForm.currentState!.validate()) {
                  final licenseId = licenseIdController.text.trim();
                  buyLicense(
                    licenseId,
                  );
                }
              },
              child: const Icon(Icons.shopping_cart_outlined),
            ),
          ],
        ),
      ),
    );
  }
}
