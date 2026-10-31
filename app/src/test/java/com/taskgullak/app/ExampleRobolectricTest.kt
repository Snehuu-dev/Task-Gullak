package com.taskgullak.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.taskgullak.app.data.remote.FirebaseEmailDispatchService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @org.junit.Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runCatching {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(context)
            }
        }
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Task Gullak", appName)
    }

    @Test
    fun `google account session saves user profile and persists login`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        FirebaseEmailDispatchService.clearEmailSession(context)
        assertFalse(FirebaseEmailDispatchService.isEmailRegisteredLocally(context, "newuser@gmail.com"))

        FirebaseEmailDispatchService.saveGoogleAccountSession(
            context = context,
            fullName = "Savvy Earner",
            email = "savvy@gmail.com"
        )

        assertTrue(FirebaseEmailDispatchService.isUserLoggedIn(context))
        assertTrue(FirebaseEmailDispatchService.isEmailRegisteredLocally(context, "savvy@gmail.com"))
        assertEquals("Savvy Earner", FirebaseEmailDispatchService.getSavedUserName(context))
        assertEquals("savvy@gmail.com", FirebaseEmailDispatchService.getSavedUserEmail(context))

        val isLoggedInAfterSave = resolveInitialLoggedInState(context, hasFirebaseUser = false)
        assertTrue(isLoggedInAfterSave)
        assertEquals(AppScreen.Home, resolveInitialScreen(isLoggedIn = isLoggedInAfterSave))

        FirebaseEmailDispatchService.clearEmailSession(context)
        val isLoggedInAfterClear = resolveInitialLoggedInState(context, hasFirebaseUser = false)
        assertFalse(isLoggedInAfterClear)
        assertEquals(AppScreen.Splash, resolveInitialScreen(isLoggedIn = isLoggedInAfterClear))
    }

    @Test
    fun `avatar selection and name edit persist in SharedPreferences`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        FirebaseEmailDispatchService.saveGoogleAccountSession(
            context = context,
            fullName = "Initial Name",
            email = "profiletest@gmail.com"
        )

        FirebaseEmailDispatchService.saveUserAvatarId(
            context = context,
            avatarId = "avatar_3d_astronaut",
            email = "profiletest@gmail.com"
        )
        assertEquals(
            "avatar_3d_astronaut",
            FirebaseEmailDispatchService.getSavedUserAvatarId(context, "profiletest@gmail.com")
        )

        FirebaseEmailDispatchService.saveUpdatedUserName(
            context = context,
            fullName = "Aarav Sharma",
            email = "profiletest@gmail.com"
        )
        assertEquals("Aarav Sharma", FirebaseEmailDispatchService.getSavedUserName(context))
    }

    @Test
    fun `NotificationReceiver handles incoming remote message broadcast and records notification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.taskgullak.app.util.TaskGullakNotificationHelper.clearPushNotificationHistory(context)

        val intent = android.content.Intent(
            context,
            com.taskgullak.app.service.NotificationReceiver::class.java
        ).apply {
            action = com.taskgullak.app.service.NotificationReceiver.ACTION_HANDLE_REMOTE_MESSAGE
            putExtra(com.taskgullak.app.service.NotificationReceiver.EXTRA_TITLE, "FCM Remote Project Alert")
            putExtra(com.taskgullak.app.service.NotificationReceiver.EXTRA_BODY, "New â‚¹75 instant task available now!")
            putExtra(com.taskgullak.app.service.NotificationReceiver.EXTRA_CATEGORY, "fcm_push")
            putExtra(com.taskgullak.app.service.NotificationReceiver.EXTRA_MESSAGE_ID, "test_fcm_msg_101")
        }

        val receiver = com.taskgullak.app.service.NotificationReceiver()
        receiver.onReceive(context, intent)

        val history = com.taskgullak.app.util.TaskGullakNotificationHelper.loadPersistedPushHistory(context)
        assertEquals(1, history.size)
        assertEquals("test_fcm_msg_101", history.first().id)
        assertEquals("FCM Remote Project Alert", history.first().title)
        assertEquals("New â‚¹75 instant task available now!", history.first().message)
    }

    @Test
    fun `taskgullak_space referral link, lifetime 5 percent commission, 50 achievement batches, and notification deletion work properly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Verify referral link uses https://taskgullak.space/dl?ref=
        val referralCode = FirebaseEmailDispatchService.getOrCreateReferralCode(context, "savvy@gmail.com")
        val referralLink = FirebaseEmailDispatchService.getReferralLink(context, "savvy@gmail.com")
        assertTrue(referralLink.startsWith("https://taskgullak.space/?ref="))
        assertTrue(referralLink.endsWith(referralCode))

        // 2. Verify lifetime 5% commission without deducting from referred user's task reward
        FirebaseEmailDispatchService.applyReferralCodeOnSignup(
            context = context,
            enteredCodeOrUrl = "FRIEND99",
            newUserEmail = "referred_earner@gmail.com",
            newUserName = "Referred Earner"
        )
        val commissionCredited = FirebaseEmailDispatchService.creditReferrerLifetimeBonusForTask(
            context = context,
            taskRewardCoins = 10000,
            taskTitle = "App Review Task"
        )
        assertEquals(5.0, commissionCredited, 0.01)

        // 3. Verify 50 achievement batches (10 to 500 tasks)
        val batches = FirebaseEmailDispatchService.getAchievementBatches(completedTasksCount = 25)
        assertEquals(50, batches.size)
        assertEquals("Task Star", batches[0].title)
        assertEquals(10, batches[0].requiredTasks)
        assertTrue(batches[0].isUnlocked)
        assertEquals("Rising Star", batches[1].title)
        assertEquals(20, batches[1].requiredTasks)
        assertTrue(batches[1].isUnlocked)
        assertEquals("Goal Getter", batches[2].title)
        assertEquals(30, batches[2].requiredTasks)
        assertFalse(batches[2].isUnlocked)
        assertEquals(500, batches.last().requiredTasks)

        // 4. Verify single notification deletion
        com.taskgullak.app.util.TaskGullakNotificationHelper.clearPushNotificationHistory(context)
        com.taskgullak.app.util.TaskGullakNotificationHelper.recordReceivedPushNotification(
            context = context,
            id = "ann_1",
            title = "Official Announcement",
            message = "Welcome to Task Gullak!",
            category = "fcm_push"
        )
        assertEquals(1, com.taskgullak.app.util.TaskGullakNotificationHelper.loadPersistedPushHistory(context).size)
        com.taskgullak.app.util.TaskGullakNotificationHelper.deleteNotificationById(context, "push_ann_1")
        assertTrue(com.taskgullak.app.util.TaskGullakNotificationHelper.loadPersistedPushHistory(context).isEmpty())
    }
}
