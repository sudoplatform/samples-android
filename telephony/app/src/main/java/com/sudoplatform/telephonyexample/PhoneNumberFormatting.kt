package com.sudoplatform.telephonyexample

fun formatAsUSNumber(number: String) : String {
    var phoneNumber = number

    if (!phoneNumber.contains("+")) {
        phoneNumber = "+1$phoneNumber"
    }

    var s = ""

    for (pos in phoneNumber.indices) {
        if (pos == 2) {
            s += " ("
        }
        if (pos == 5) {
            s += ") "
        }
        if (pos == 8) {
            s += " "
        }
        s += phoneNumber[pos]
    }
    return s
}

