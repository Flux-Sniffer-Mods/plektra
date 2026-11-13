package rocks.pkb.plektra.otp

import android.app.Notification
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Reads new notifications for one-time codes, only while "One-time codes" is on and the user has
 * given the app Android's notification access. Only the code itself is kept (see [OneTimeCodes]).
 */
class OneTimeCodeListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        if (sbn.packageName == packageName || !OneTimeCodeSettings.isEnabled(this)) return
        val code = codeIn(notification) ?: return
        OneTimeCodes.offer(code)
    }

    companion object {
        /** The code a notification carries, if any: its copy button, else its newest text. */
        fun codeIn(notification: Notification): String? {
            val extras = notification.extras ?: return null
            // Newest first: a conversation's notification (SMS, chat) keeps its older messages,
            // with their older codes, in its big text and message list
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val parts = listOfNotNull(
                latestMessage(notification),
                extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
                extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.lastOrNull()?.toString()
            ).filter { it.isNotBlank() }
            // A "Copy 123456" button (Google Messages and others add one) names the code outright
            return notification.actions.orEmpty().firstNotNullOfOrNull { action ->
                OneTimeCodes.fromCopyAction(action.title?.toString().orEmpty())
            } ?: parts.firstNotNullOfOrNull { part ->
                OneTimeCodes.extract(part) ?: OneTimeCodes.extract("$title $part")
            }
        }

        /** The newest message of a conversation-style notification, if it is one. */
        private fun latestMessage(notification: Notification): String? = runCatching {
            @Suppress("DEPRECATION")
            val messages = notification.extras.getParcelableArray(Notification.EXTRA_MESSAGES) ?: return null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Notification.MessagingStyle.Message.getMessagesFromBundleArray(messages)
                    .maxByOrNull { it.timestamp }?.text?.toString()
            } else {
                messages.filterIsInstance<Bundle>()
                    .maxByOrNull { it.getLong("time") }?.getCharSequence("text")?.toString()
            }
        }.getOrNull()
    }
}
