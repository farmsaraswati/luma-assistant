package com.example.lumaassistant.features

import java.util.Calendar
import java.util.regex.Pattern

sealed class Command {
    data class SetReminder(val message: String, val triggerAtMillis: Long) : Command()
    data class AddNote(val text: String) : Command()
    data class OpenApp(val appName: String) : Command()
    data class TypeText(val text: String) : Command()
    data class TapOnScreen(val label: String) : Command()
    data class Scroll(val down: Boolean) : Command()
    data class SetFlashlight(val on: Boolean) : Command()
    data class SetVolume(val percent: Int) : Command()
    object TakeSelfie : Command()
    object AnalyzeScreen : Command()
    data class Chat(val text: String) : Command()
}

/**
 * Decides what to do with the user's spoken/typed text. Everything here is
 * plain, readable pattern matching - there is no remote command channel and
 * no way for anything other than the user's own words to trigger an action.
 */
object CommandRouter {

    private val reminderPattern = Pattern.compile(
        "remind me (?:to )?(.+?) (?:in|at) (\\d+)\\s*(minute|minutes|hour|hours)",
        Pattern.CASE_INSENSITIVE
    )
    private val notePattern = Pattern.compile(
        "(?:take a note|note that|note)[:\\s]+(.+)",
        Pattern.CASE_INSENSITIVE
    )
    private val openAppPattern = Pattern.compile(
        "open\\s+(.+)", Pattern.CASE_INSENSITIVE
    )
    private val typePattern = Pattern.compile(
        "type\\s+(.+)", Pattern.CASE_INSENSITIVE
    )
    private val tapPattern = Pattern.compile(
        "(?:tap|click|press)\\s+(?:on\\s+)?(.+)", Pattern.CASE_INSENSITIVE
    )
    private val volumePattern = Pattern.compile(
        "(?:set )?volume\\s+(?:to\\s+)?(\\d{1,3})", Pattern.CASE_INSENSITIVE
    )

    fun parse(input: String): Command {
        val lower = input.trim().lowercase()

        if (lower == "flashlight on" || lower == "turn on flashlight" || lower == "torch on") {
            return Command.SetFlashlight(true)
        }
        if (lower == "flashlight off" || lower == "turn off flashlight" || lower == "torch off") {
            return Command.SetFlashlight(false)
        }
        if (lower == "take a selfie" || lower == "take selfie" || lower == "take a photo") {
            return Command.TakeSelfie
        }
        if (lower == "what's on my screen" || lower == "what is on my screen" || lower == "analyze screen" || lower == "read screen") {
            return Command.AnalyzeScreen
        }
        if (lower == "scroll down") return Command.Scroll(down = true)
        if (lower == "scroll up") return Command.Scroll(down = false)

        volumePattern.matcher(lower).let {
            if (it.find()) return Command.SetVolume(it.group(1)?.toIntOrNull() ?: 50)
        }
        openAppPattern.matcher(input).let {
            if (it.find()) return Command.OpenApp(it.group(1)?.trim().orEmpty())
        }
        typePattern.matcher(input).let {
            if (it.find()) return Command.TypeText(it.group(1)?.trim().orEmpty())
        }
        tapPattern.matcher(input).let {
            if (it.find()) return Command.TapOnScreen(it.group(1)?.trim().orEmpty())
        }

        val reminderMatcher = reminderPattern.matcher(input)
        if (reminderMatcher.find()) {
            val message = reminderMatcher.group(1)?.trim().orEmpty()
            val amount = reminderMatcher.group(2)?.toLongOrNull() ?: 0L
            val unit = reminderMatcher.group(3)?.lowercase().orEmpty()
            val millis = if (unit.startsWith("hour")) amount * 60 * 60 * 1000
            else amount * 60 * 1000
            val triggerAt = Calendar.getInstance().apply {
                add(Calendar.MILLISECOND, millis.toInt())
            }.timeInMillis
            return Command.SetReminder(message, triggerAt)
        }

        val noteMatcher = notePattern.matcher(input)
        if (noteMatcher.find()) {
            val text = noteMatcher.group(1)?.trim().orEmpty()
            return Command.AddNote(text)
        }

        return Command.Chat(input)
    }
}
