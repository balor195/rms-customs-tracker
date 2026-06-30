package com.rms.customs.domain.model.enums

enum class Department(val labelAr: String, val labelEn: String) {
    PHARMACY("شعبة الدواء", "Pharmacy Division"),
    MEDICAL_CONSUMABLES("شعبة المستهلكات الطبية", "Medical Consumables Division"),
    MEDICAL_DEVICES("شعبة الأجهزة الطبية", "Medical Devices Division");
}
