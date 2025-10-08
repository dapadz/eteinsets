package com.dapadz.eteinsets.utils

import androidx.core.view.WindowInsetsCompat


fun WindowInsetsCompat.imeHeight(): Int {
    return getInsets(WindowInsetsCompat.Type.ime()).bottom
}
