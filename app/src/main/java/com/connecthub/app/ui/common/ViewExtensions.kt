package com.connecthub.app.ui.common

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

/**
 * Wires a plain (String) -> Unit callback to an EditText/TextInputEditText,
 * so fragments can forward changes straight into a ViewModel intent without
 * boilerplate TextWatcher objects at every call site.
 */
fun EditText.addTextChangedListener(onChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onChanged(s?.toString().orEmpty())
        }
        override fun afterTextChanged(s: Editable?) {}
    })
}
