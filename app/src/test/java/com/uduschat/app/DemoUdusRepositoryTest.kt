package com.uduschat.app

import com.uduschat.app.data.repository.DemoUdusRepository
import com.uduschat.app.domain.model.AuthStep
import com.uduschat.app.domain.model.CallStatus
import com.uduschat.app.domain.model.CallType
import com.uduschat.app.domain.model.MessageDraftAttachment
import com.uduschat.app.domain.model.MessageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DemoUdusRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun signIn(repo: DemoUdusRepository) {
        repo.proceedFromWelcome()
        repo.sendOtp("+8801712345678", null)
        val otp = repo.authState.value.demoOtpHint!!
        repo.verifyOtp(otp)
        assertEquals(AuthStep.PROFILE_SETUP, repo.authState.value.step)
        repo.completeProfileSetup("Udu Tester", "Global tester", null)
        assertEquals(AuthStep.AUTHENTICATED, repo.authState.value.step)
    }

    @Test
    fun authRejectsInvalidOtpThenCompletesProfile() = runTest(testDispatcher) {
        val repo = DemoUdusRepository(null)
        repo.proceedFromWelcome()
        repo.sendOtp("+8801712345678", null)
        repo.verifyOtp("000000")
        assertNotNull(repo.authState.value.errorMessage)
        assertEquals(AuthStep.OTP_VERIFY, repo.authState.value.step)

        repo.verifyOtp(repo.authState.value.demoOtpHint!!)
        assertEquals(AuthStep.PROFILE_SETUP, repo.authState.value.step)
        repo.completeProfileSetup("Mehedi", "Available", null)
        assertEquals("Mehedi", repo.currentUser.value?.displayName)
    }

    @Test
    fun connectGlobalPhoneNumberAndSendEditStarDeleteMessage() = runTest(testDispatcher) {
        val repo = DemoUdusRepository(null)
        signIn(repo)

        val user = repo.findUserByPhone("+4915123456789").getOrNull()
        assertNotNull(user)
        val chatId = repo.startOrOpenDirectChat(user!!).getOrThrow()

        repo.sendMessage(chatId, "Hello Germany from udu's chat!")
        var msgs = repo.observeMessages(chatId).first()
        assertEquals(1, msgs.size)
        val msgId = msgs.first().id

        repo.editMessage(chatId, msgId, "Edited message")
        msgs = repo.observeMessages(chatId).first()
        assertEquals("Edited message", msgs.first().text)
        assertTrue(msgs.first().isEdited)

        repo.toggleReaction(chatId, msgId, "\u2764\uFE0F")
        msgs = repo.observeMessages(chatId).first()
        assertEquals("\u2764\uFE0F", msgs.first().reactions[repo.currentUser.value!!.uid])

        repo.toggleStarMessage(chatId, msgId)
        assertTrue(repo.starredMessages.value.any { it.id == msgId })

        repo.deleteMessage(chatId, msgId, forEveryone = true)
        msgs = repo.observeMessages(chatId).first()
        assertTrue(msgs.first().isDeletedForEveryone)
    }

    @Test
    fun createGroupAndPostStatusAndCallLifecycle() = runTest(testDispatcher) {
        val repo = DemoUdusRepository(null)
        signIn(repo)

        val peerUids = repo.contacts.value.take(2).map { it.uid }
        val groupId = repo.createGroupChat("Test Squad", "Dev talk", peerUids).getOrThrow()
        repo.sendMessage(
            groupId,
            "",
            attachment = MessageDraftAttachment(type = MessageType.LOCATION, locationLabel = "Dhaka", locationLat = 23.81, locationLng = 90.41)
        )
        val groupMsgs = repo.observeMessages(groupId).first()
        assertTrue(groupMsgs.any { it.type == MessageType.LOCATION })

        repo.postTextStatus("Live from Dhaka!", 0xFF008069L)
        val myStatus = repo.statuses.value.first { it.text == "Live from Dhaka!" }
        assertNotNull(myStatus)

        val peer = repo.contacts.value.first()
        repo.startCall(peer.uid, peer.displayName, peer.phoneNumber, null, CallType.VIDEO)
        assertNotNull(repo.activeCall.value)
        repo.acceptIncomingCall()
        assertEquals(CallStatus.CONNECTED, repo.activeCall.value?.status)
        repo.endActiveCall()
        assertNull(repo.activeCall.value)
        assertTrue(repo.callLogs.value.isNotEmpty())
    }
}
