/*
 *  Copyright (c) 2021 David Allison <davidallisongithub@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation; either version 3 of the License, or (at your option) any later
 *  version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.ichi2.anki.reviewer

import android.content.Context
import android.text.TextUtils
import android.util.Pair
import android.view.KeyEvent
import androidx.annotation.VisibleForTesting
import com.ichi2.anki.cardviewer.Gesture
import com.ichi2.utils.StringUtil
import timber.log.Timber
import java.util.*

class Binding private constructor(private val mModifierKeys: ModifierKeys?, private val mKeycode: Int?, private val mUnicodeCharacter: Char?, private val mGesture: Gesture?) {
    fun toDisplayString(context: Context?): String {
        val string = StringBuilder()
        when {
            mKeycode != null -> {
                string.append(KEY_PREFIX)
                string.append(' ')
                string.append(Objects.requireNonNull(mModifierKeys).toString())
                val keyCodeString = KeyEvent.keyCodeToString(mKeycode)
                string.append(StringUtil.toTitleCase(keyCodeString.replace("KEYCODE_", "").replace('_', ' ')))
            }
            mUnicodeCharacter != null -> {
                string.append(KEY_PREFIX)
                string.append(' ')
                string.append(Objects.requireNonNull(mModifierKeys).toString())
                string.append(mUnicodeCharacter)
            }
            mGesture != null -> {
                string.append(mGesture.toDisplayString(context!!))
            }
        }
        return string.toString()
    }

    fun getKeycode() = mKeycode
    fun getModifierKeys() = mModifierKeys
    fun getUnicodeCharacter() = mUnicodeCharacter
    fun getGesture() = mGesture

    override fun toString(): String {
        val string = StringBuilder()
        when {
            mKeycode != null -> {
                string.append(KEY_PREFIX)
                string.append(Objects.requireNonNull(mModifierKeys).toString())
                string.append(mKeycode)
            }
            mUnicodeCharacter != null -> {
                string.append(UNICODE_PREFIX)
                string.append(Objects.requireNonNull(mModifierKeys).toString())
                string.append(mUnicodeCharacter)
            }
            mGesture != null -> {
                string.append(GESTURE_PREFIX)
                string.append(mGesture)
            }
        }
        return string.toString()
    }

    val isKeyCode: Boolean get() = mKeycode != null

    val isKey: Boolean
        get() = isKeyCode || mUnicodeCharacter != null

    fun isGesture(): Boolean = mGesture != null

    fun matchesModifier(event: KeyEvent): Boolean {
        return mModifierKeys == null || mModifierKeys.matches(event)
    }

    open class ModifierKeys internal constructor(private val mShift: Boolean, private val mCtrl: Boolean, private val mAlt: Boolean) {
        fun matches(event: KeyEvent): Boolean {
            // return false if Ctrl+1 is pressed and 1 is expected
            return shiftMatches(event) && ctrlMatches(event) && altMatches(event)
        }

        private fun shiftMatches(event: KeyEvent): Boolean = mShift == event.isShiftPressed

        private fun ctrlMatches(event: KeyEvent): Boolean = mCtrl == event.isCtrlPressed

        private fun altMatches(event: KeyEvent): Boolean = altMatches(event.isAltPressed)

        open fun shiftMatches(shiftPressed: Boolean): Boolean = mShift == shiftPressed

        fun ctrlMatches(ctrlPressed: Boolean): Boolean = mCtrl == ctrlPressed

        fun altMatches(altPressed: Boolean): Boolean = mAlt == altPressed

        override fun toString(): String {
            val string = StringBuilder()
            if (mCtrl) {
                string.append("Ctrl+")
            }
            if (mAlt) {
                string.append("Alt+")
            }
            if (mShift) {
                string.append("Shift+")
            }
            return string.toString()
        }

        companion object {
            fun none(): ModifierKeys = ModifierKeys(mShift = false, mCtrl = false, mAlt = false)

            @JvmStatic
            fun ctrl(): ModifierKeys = ModifierKeys(mShift = false, mCtrl = true, mAlt = false)

            @JvmStatic
            fun shift(): ModifierKeys = ModifierKeys(mShift = true, mCtrl = false, mAlt = false)

            @JvmStatic
            fun alt(): ModifierKeys = ModifierKeys(mShift = false, mCtrl = false, mAlt = true)

            /**
             * Parses a [ModifierKeys] from a string.
             * @param s The string to parse
             * @return The [ModifierKeys], and the remainder of the string
             */
            fun parse(s: String): Pair<ModifierKeys, String> {
                var modifiers = none()
                val plus = s.lastIndexOf("+")
                if (plus == -1) {
                    return Pair(modifiers, s)
                }
                modifiers = fromString(s.substring(0, plus + 1))
                return Pair(modifiers, s.substring(plus + 1))
            }

            fun fromString(from: String): ModifierKeys =
                ModifierKeys(from.contains("Shift"), from.contains("Ctrl"), from.contains("Alt"))
        }
    }

    /** Modifier keys which cannot be defined by a binding  */
    private class AppDefinedModifierKeys private constructor() : ModifierKeys(false, false, false) {
        override fun shiftMatches(shiftPressed: Boolean): Boolean = true

        companion object {
            /**
             * Specifies a keycode combination binding from an unknown input device
             * Should be due to the "default" key bindings and never from user input
             *
             * If we do not know what the device is, "*" could be a key on the keyboard or Shift + 8
             *
             * So we need to ignore shift, rather than match it to a value
             *
             * If we have bindings in the app, then we know whether we need shift or not (in actual fact, we should
             * be fine to use keycodes).
             */
            fun allowShift(): ModifierKeys = AppDefinedModifierKeys()
        }
    }

    companion object {
        const val FORBIDDEN_UNICODE_CHAR = MappableBinding.PREF_SEPARATOR

        /** https://www.fileformat.info/info/unicode/char/2328/index.htm (Keyboard)  */
        const val KEY_PREFIX = '\u2328'

        /** https://www.fileformat.info/info/unicode/char/235d/index.htm (similar to a finger)  */
        const val GESTURE_PREFIX = '\u235D'

        /** https://www.fileformat.info/info/unicode/char/2705/index.htm - checkmark (often used in URLs for unicode)
         * Only used for serialisation. [.KEY_PREFIX] is used for display.
         */
        const val UNICODE_PREFIX = '\u2705'

        /** This returns multiple bindings due to the "default" implementation not knowing what the keycode for a button is  */
        @JvmStatic
        fun key(event: KeyEvent): List<Binding> {
            val modifiers = ModifierKeys(event.isShiftPressed, event.isCtrlPressed, event.isAltPressed)
            val ret: MutableList<Binding> = ArrayList()
            val keyCode = event.keyCode
            if (keyCode != 0) {
                ret.add(keyCode(modifiers, keyCode))
            }

            // passing in metaState: 0 means that Ctrl+1 returns '1' instead of '\0'
            // NOTE: We do not differentiate on upper/lower case via KeyEvent.META_CAPS_LOCK_ON
            val unicodeChar = event.getUnicodeChar(event.metaState and (KeyEvent.META_SHIFT_ON or KeyEvent.META_NUM_LOCK_ON))
            if (unicodeChar != 0) {
                try {
                    ret.add(unicode(modifiers, unicodeChar.toChar()))
                } catch (e: Exception) {
                    Timber.w(e)
                }
            }

            return ret
        }

        /**
         * Specifies a unicode binding from an unknown input device
         * See [AppDefinedModifierKeys]
         */
        @JvmStatic
        fun unicode(unicodeChar: Char): Binding =
            unicode(AppDefinedModifierKeys.allowShift(), unicodeChar)

        fun unicode(modifierKeys: ModifierKeys?, unicodeChar: Char): Binding {
            if (unicodeChar == FORBIDDEN_UNICODE_CHAR) return unknown()
            return Binding(modifierKeys, null, unicodeChar, null)
        }

        @JvmStatic
        fun keyCode(keyCode: Int): Binding = keyCode(ModifierKeys.none(), keyCode)

        @JvmStatic
        fun keyCode(modifiers: ModifierKeys?, keyCode: Int): Binding =
            Binding(modifiers, keyCode, null, null)

        fun gesture(gesture: Gesture?): Binding = Binding(null, null, null, gesture)

        @VisibleForTesting
        fun unknown(): Binding = Binding(ModifierKeys.none(), null, null, null)

        fun fromString(from: String): Binding {
            if (TextUtils.isEmpty(from)) return unknown()
            try {
                return when (from[0]) {
                    GESTURE_PREFIX -> {
                        gesture(Gesture.valueOf(from.substring(1)))
                    }
                    UNICODE_PREFIX -> {
                        val parsed = ModifierKeys.parse(from.substring(1))
                        unicode(parsed.first, parsed.second[0])
                    }
                    KEY_PREFIX -> {
                        val parsed = ModifierKeys.parse(from.substring(1))
                        val keyCode = parsed.second.toInt()
                        keyCode(parsed.first, keyCode)
                    }
                    else -> unknown()
                }
            } catch (ex: Exception) {
                Timber.w(ex)
            }
            return unknown()
        }
    }
}
