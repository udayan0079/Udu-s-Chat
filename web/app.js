import { initializeApp } from 'https://www.gstatic.com/firebasejs/11.3.1/firebase-app.js';
import {
  getAuth,
  onAuthStateChanged,
  RecaptchaVerifier,
  signInWithPhoneNumber,
  signOut,
} from 'https://www.gstatic.com/firebasejs/11.3.1/firebase-auth.js';
import {
  getFirestore,
  collection,
  doc,
  getDoc,
  getDocs,
  setDoc,
  updateDoc,
  deleteDoc,
  query,
  where,
  orderBy,
  limit,
  onSnapshot,
  arrayUnion,
  arrayRemove,
} from 'https://www.gstatic.com/firebasejs/11.3.1/firebase-firestore.js';
import {
  getStorage,
  ref as storageRef,
  uploadBytes,
  getDownloadURL,
} from 'https://www.gstatic.com/firebasejs/11.3.1/firebase-storage.js';

import { firebaseConfig } from './firebase-config.js';

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);
const storage = getStorage(app);

const $ = (id) => document.getElementById(id);

const state = {
  user: null,
  theme: localStorage.getItem('udus_theme') || 'dark',
  readReceipts: true,
  lastSeen: true,
  activeTab: 'chats',
  activeChatId: null,
  filter: 'all',
  search: '',
  chats: [],
  users: [],
  messages: [],
  statuses: [],
  calls: [],
};

let confirmationResult = null;
let recaptchaVerifier = null;
let pendingPhone = '';
let replyTarget = null;
let mediaRecorder = null;
let audioChunks = [];
let unsubChats = null;
let unsubUsers = null;
let unsubMessages = null;
let unsubStatuses = null;
let unsubCalls = null;
let activeCallId = null;
let callTimer = null;
let callSeconds = 0;

function applyTheme() {
  document.documentElement.setAttribute('data-theme', state.theme);
  $('darkModeSwitch').checked = state.theme === 'dark';
}
applyTheme();

function ensureRecaptcha() {
  if (!recaptchaVerifier) {
    const container = $('recaptcha-container');
    container.innerHTML = '';
    const slot = document.createElement('div');
    container.appendChild(slot);
    recaptchaVerifier = new RecaptchaVerifier(auth, slot, {
      size: 'invisible',
    });
  }
  return recaptchaVerifier;
}

// Listen for real Firebase Auth state
onAuthStateChanged(auth, async (fbUser) => {
  if (!fbUser) {
    cleanupListeners();
    state.user = null;
    $('authScreen').classList.remove('hidden');
    $('appShell').classList.add('hidden');
    $('authStepPhone').classList.remove('hidden');
    $('authStepOtp').classList.add('hidden');
    $('authStepProfile').classList.add('hidden');
    return;
  }

  const userSnap = await getDoc(doc(db, 'users', fbUser.uid));
  if (userSnap.exists() && userSnap.data().displayName) {
    state.user = { uid: fbUser.uid, ...userSnap.data() };
    await setDoc(
      doc(db, 'users', fbUser.uid),
      { isOnline: true, lastSeenAt: Date.now() },
      { merge: true }
    );
    enterMainApp();
  } else {
    pendingPhone = fbUser.phoneNumber || pendingPhone;
    $('authScreen').classList.remove('hidden');
    $('appShell').classList.add('hidden');
    $('authStepPhone').classList.add('hidden');
    $('authStepOtp').classList.add('hidden');
    $('authStepProfile').classList.remove('hidden');
  }
});

function enterMainApp() {
  $('authScreen').classList.add('hidden');
  $('appShell').classList.remove('hidden');
  attachRealtimeListeners();
  renderApp();
}

function cleanupListeners() {
  unsubChats?.();
  unsubUsers?.();
  unsubMessages?.();
  unsubStatuses?.();
  unsubCalls?.();
  unsubChats = unsubUsers = unsubMessages = unsubStatuses = unsubCalls = null;
}

function attachRealtimeListeners() {
  cleanupListeners();
  const uid = state.user.uid;

  // 0. Registered users directory for phone/name search
  unsubUsers = onSnapshot(collection(db, 'users'), (snap) => {
    state.users = snap.docs
      .map((d) => ({ uid: d.id, ...d.data() }))
      .filter((u) => u.uid !== uid && u.phoneNumber)
      .sort((a, b) => (a.displayName || '').localeCompare(b.displayName || ''));
    renderChatList();
  });

  // 1. Chats
  const chatsQ = query(collection(db, 'chats'), where('participantIds', 'array-contains', uid));
  unsubChats = onSnapshot(chatsQ, (snap) => {
    state.chats = snap.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .sort((a, b) => (b.lastMessageTime || 0) - (a.lastMessageTime || 0));
    if (!state.activeChatId && state.chats.length > 0) {
      selectChat(state.chats[0].id);
    } else {
      renderChatList();
      renderActiveChatHeader();
    }
  });

  // 2. Statuses
  const statusQ = query(collection(db, 'statuses'), where('expiresAt', '>', Date.now()));
  unsubStatuses = onSnapshot(statusQ, (snap) => {
    state.statuses = snap.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));
    renderStatuses();
  });

  // 3. Calls
  const callsQ = query(collection(db, 'calls'), where('participants', 'array-contains', uid));
  unsubCalls = onSnapshot(callsQ, (snap) => {
    state.calls = snap.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .sort((a, b) => (b.startedAt || 0) - (a.startedAt || 0));
    renderCalls();

    const ringing = state.calls.find((c) => c.calleeId === uid && c.status === 'RINGING');
    if (ringing && !activeCallId) {
      showIncomingCall(ringing);
    } else if (activeCallId) {
      const current = state.calls.find((c) => c.id === activeCallId);
      if (current && ['ENDED', 'DECLINED', 'MISSED'].includes(current.status)) {
        closeCallOverlay();
      } else if (current && current.status === 'CONNECTED') {
        $('callStatusText').textContent = `Connected • ${formatDuration(callSeconds)}`;
      }
    }
  });
}

function selectChat(chatId) {
  state.activeChatId = chatId;
  unsubMessages?.();

  const msgsQ = query(
    collection(db, 'chats', chatId, 'messages'),
    orderBy('createdAt')
  );
  unsubMessages = onSnapshot(msgsQ, async (snap) => {
    const uid = state.user.uid;
    state.messages = snap.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .filter((m) => !(m.deletedFor || []).includes(uid));
    renderMessages();

    // Mark unread messages as READ
    if (state.readReceipts) {
      for (const m of state.messages) {
        if (m.senderId !== uid && m.status !== 'READ') {
          updateDoc(doc(db, 'chats', chatId, 'messages', m.id), { status: 'READ' }).catch(() => {});
        }
      }
    }
  });

  updateDoc(doc(db, 'chats', chatId), { [`unread_${state.user.uid}`]: 0 }).catch(() => {});
  renderChatList();
  renderActiveChatHeader();
}

// Auth Actions
$('sendOtpBtn').onclick = async () => {
  const code = $('countrySelect').value;
  let num = $('phoneInput').value.replace(/\D/g, '');
  if (code === '+880' && num.startsWith('0')) {
    num = num.slice(1);
  }
  if (num.length < 7) {
    showAuthError('authPhoneError', 'Please enter a valid mobile number.');
    return;
  }
  pendingPhone = `${code}${num}`;
  $('authPhoneError').classList.add('hidden');
  $('sendOtpBtn').disabled = true;
  $('sendOtpBtn').textContent = 'Sending OTP...';

  try {
    const verifier = ensureRecaptcha();
    confirmationResult = await signInWithPhoneNumber(auth, pendingPhone, verifier);
    $('otpPhoneDisplay').textContent = pendingPhone;
    $('authStepPhone').classList.add('hidden');
    $('authStepOtp').classList.remove('hidden');
  } catch (err) {
    showAuthError('authPhoneError', err.message || 'Failed to send OTP. Enable Phone Auth in Firebase Console.');
    if (recaptchaVerifier) {
      recaptchaVerifier.clear();
      recaptchaVerifier = null;
    }
  } finally {
    $('sendOtpBtn').disabled = false;
    $('sendOtpBtn').textContent = 'Send OTP';
  }
};

$('backToPhoneBtn').onclick = () => {
  $('authStepOtp').classList.add('hidden');
  $('authStepPhone').classList.remove('hidden');
};

$('verifyOtpBtn').onclick = async () => {
  const otp = $('otpInput').value.trim();
  if (!/^\d{6}$/.test(otp)) {
    showAuthError('authOtpError', 'Enter the 6-digit OTP code.');
    return;
  }
  $('authOtpError').classList.add('hidden');
  $('verifyOtpBtn').disabled = true;
  $('verifyOtpBtn').textContent = 'Verifying...';
  try {
    await confirmationResult.confirm(otp);
  } catch (err) {
    showAuthError('authOtpError', err.message || 'Invalid OTP code.');
  } finally {
    $('verifyOtpBtn').disabled = false;
    $('verifyOtpBtn').textContent = 'Verify OTP';
  }
};

$('completeProfileBtn').onclick = async () => {
  const fbUser = auth.currentUser;
  if (!fbUser) return;
  const displayName = $('profileNameInput').value.trim() || 'Udu User';
  const about = $('profileAboutInput').value.trim() || "Hey there! I am using udu's chat.";
  const profile = {
    uid: fbUser.uid,
    phoneNumber: fbUser.phoneNumber || pendingPhone,
    displayName,
    about,
    isOnline: true,
    lastSeenAt: Date.now(),
  };
  await setDoc(doc(db, 'users', fbUser.uid), profile, { merge: true });
  state.user = profile;
  enterMainApp();
};

function showAuthError(elId, msg) {
  const el = $(elId);
  el.textContent = msg;
  el.classList.remove('hidden');
}

// Tabs & Filters
document.querySelectorAll('.rail-btn[data-tab]').forEach((btn) => {
  btn.onclick = () => {
    state.activeTab = btn.dataset.tab;
    renderTabs();
  };
});

function renderTabs() {
  document.querySelectorAll('.rail-btn[data-tab]').forEach((b) => {
    b.classList.toggle('active', b.dataset.tab === state.activeTab);
  });
  ['chats', 'updates', 'calls', 'settings'].forEach((t) => {
    $(`tab${t.charAt(0).toUpperCase() + t.slice(1)}`).classList.toggle('hidden', state.activeTab !== t);
  });
  const titles = { chats: "udu's chat", updates: 'Updates', calls: 'Calls', settings: 'Settings' };
  $('sidebarTitle').textContent = titles[state.activeTab] || "udu's chat";
}

$('chatSearchInput').oninput = (e) => {
  state.search = e.target.value.toLowerCase();
  renderChatList();
};

document.querySelectorAll('.chip').forEach((chip) => {
  chip.onclick = () => {
    document.querySelectorAll('.chip').forEach((c) => c.classList.remove('active'));
    chip.classList.add('active');
    state.filter = chip.dataset.filter;
    renderChatList();
  };
});

function chatDisplayTitle(chat) {
  if (chat.type === 'GROUP') return chat.title || 'Group';
  const myUid = state.user.uid;
  const otherUid = (chat.participantIds || []).find((id) => id !== myUid);
  return chat.participantNames?.[otherUid] || chat.participantPhones?.[otherUid] || 'Chat';
}

function renderApp() {
  renderTabs();
  const initial = (state.user.displayName || 'U')[0].toUpperCase();
  $('railAvatar').textContent = initial;
  $('settingsAvatar').textContent = initial;
  $('settingsName').textContent = state.user.displayName;
  $('settingsPhone').textContent = state.user.phoneNumber;
  $('settingsAbout').textContent = state.user.about || '';
  renderChatList();
  renderActiveChatHeader();
  renderMessages();
  renderStatuses();
  renderCalls();
}

function findMatchingUsers(term) {
  const clean = (term || '').trim().toLowerCase();
  if (!clean) return [];
  const digits = clean.replace(/\D/g, '').replace(/^0+/, '');
  return state.users.filter((u) => {
    const name = (u.displayName || '').toLowerCase();
    const phone = (u.phoneNumber || '').toLowerCase();
    const phoneDigits = phone.replace(/\D/g, '');
    return name.includes(clean) || phone.includes(clean) || (digits.length >= 3 && phoneDigits.includes(digits));
  });
}

async function openDirectChatWithUser(other) {
  try {
    const me = state.user;
    const chatId = [me.uid, other.uid].sort().join('_');
    const existing = state.chats.find((c) => c.id === chatId);
    if (!existing) {
      const payload = {
        type: 'DIRECT',
        participantIds: [me.uid, other.uid],
        adminIds: [],
        participantNames: { [me.uid]: me.displayName, [other.uid]: other.displayName || other.phoneNumber },
        participantPhones: { [me.uid]: me.phoneNumber, [other.uid]: other.phoneNumber },
        participantAvatars: { [me.uid]: '', [other.uid]: '' },
        lastMessagePreview: '',
        lastMessageSenderId: '',
        lastMessageType: 'TEXT',
        lastMessageStatus: 'SENT',
        lastMessageTime: Date.now(),
        disappearingSeconds: 0,
      };
      await setDoc(doc(db, 'chats', chatId), payload);
      state.chats.unshift({ id: chatId, ...payload });
    }
    state.search = '';
    $('chatSearchInput').value = '';
    selectChat(chatId);
  } catch (err) {
    alert('Could not open chat: ' + (err.message || err));
  }
}

function renderChatList() {
  const list = $('chatList');
  list.innerHTML = '';
  const myUid = state.user.uid;

  const filtered = state.chats.filter((c) => {
    const title = chatDisplayTitle(c).toLowerCase();
    const unread = c[`unread_${myUid}`] || 0;
    if (state.filter === 'unread' && !unread) return false;
    if (state.filter === 'groups' && c.type !== 'GROUP') return false;
    if (state.search && !title.includes(state.search)) return false;
    return true;
  });

  const matchingUsers = findMatchingUsers(state.search);

  if (filtered.length === 0 && matchingUsers.length === 0) {
    list.innerHTML = `<div class="section-pad muted small">No chats or registered users found. Click ➕ above to start a chat!</div>`;
    return;
  }

  filtered.forEach((chat) => {
    const title = chatDisplayTitle(chat);
    const unread = chat[`unread_${myUid}`] || 0;
    const div = document.createElement('div');
    div.className = `chat-item ${chat.id === state.activeChatId ? 'active' : ''}`;
    div.innerHTML = `
      <div class="avatar-md">${chat.type === 'GROUP' ? '👥' : title[0].toUpperCase()}</div>
      <div class="chat-item-body">
        <div class="chat-item-top">
          <span class="chat-item-title">${escapeHtml(title)}</span>
          <span class="small muted">${chat.lastMessageTime ? formatTime(chat.lastMessageTime) : ''}</span>
        </div>
        <div class="row-between">
          <span class="chat-item-preview">${escapeHtml(chat.lastMessagePreview || 'Tap to start chatting')}</span>
          ${unread ? `<span class="unread-badge">${unread}</span>` : ''}
        </div>
      </div>
    `;
    div.onclick = () => selectChat(chat.id);
    list.appendChild(div);
  });

  if (matchingUsers.length > 0) {
    const hdr = document.createElement('div');
    hdr.className = 'section-pad small';
    hdr.style.color = 'var(--emerald)';
    hdr.style.fontWeight = '600';
    hdr.textContent = "Registered on udu's chat";
    list.appendChild(hdr);

    matchingUsers.forEach((u) => {
      const div = document.createElement('div');
      div.className = 'chat-item';
      const name = u.displayName || u.phoneNumber;
      div.innerHTML = `
        <div class="avatar-md">${name[0].toUpperCase()}</div>
        <div class="chat-item-body">
          <div class="chat-item-top">
            <span class="chat-item-title">${escapeHtml(name)}</span>
          </div>
          <div class="chat-item-preview">${escapeHtml(u.phoneNumber)}</div>
        </div>
      `;
      div.onclick = () => openDirectChatWithUser(u);
      list.appendChild(div);
    });
  }
}

function renderActiveChatHeader() {
  const chat = state.chats.find((c) => c.id === state.activeChatId);
  if (!chat) {
    $('emptyState').classList.remove('hidden');
    $('activeChatView').classList.add('hidden');
    return;
  }
  $('emptyState').classList.add('hidden');
  $('activeChatView').classList.remove('hidden');
  const title = chatDisplayTitle(chat);
  $('activeChatAvatar').textContent = chat.type === 'GROUP' ? '👥' : title[0].toUpperCase();
  $('activeChatTitle').textContent = title;
  const typingOthers = (chat.typingUserIds || []).filter((id) => id !== state.user.uid);
  $('activeChatSubtitle').textContent = typingOthers.length > 0 ? 'typing...' : (chat.type === 'GROUP' ? `${(chat.participantIds || []).length} members` : 'online');
}

function renderMessages() {
  const container = $('messagesContainer');
  container.innerHTML = '';
  const myUid = state.user.uid;

  state.messages.forEach((m) => {
    const isOut = m.senderId === myUid;
    const row = document.createElement('div');
    row.className = `msg-row ${isOut ? 'out' : 'in'}`;

    let mediaHtml = '';
    if (m.mediaUrl && (m.type === 'IMAGE' || m.mediaMimeType?.startsWith('image/'))) {
      mediaHtml = `<img class="msg-media-img" src="${m.mediaUrl}" alt="image" />`;
    } else if (m.mediaUrl && (m.type === 'VOICE' || m.mediaMimeType?.startsWith('audio/'))) {
      mediaHtml = `<audio controls src="${m.mediaUrl}" style="max-width:240px;height:36px;"></audio>`;
    } else if (m.mediaUrl || m.fileName) {
      mediaHtml = `<a href="${m.mediaUrl || '#'}" target="_blank" style="color:var(--emerald);display:block;margin-bottom:4px;">📄 ${escapeHtml(m.fileName || 'Attachment')}</a>`;
    }

    const replyHtml = m.replyToPreview
      ? `<div class="msg-reply-quote"><strong>${escapeHtml(m.replyToSenderName || '')}</strong><div>${escapeHtml(m.replyToPreview)}</div></div>`
      : '';

    const reactionsList = Object.values(m.reactions || {});
    const reactionsHtml = reactionsList.length ? `<div style="margin-top:4px;font-size:13px;">${reactionsList.join(' ')}</div>` : '';

    row.innerHTML = `
      <div class="msg-bubble">
        ${!isOut ? `<div class="small" style="color:var(--emerald);font-weight:600;margin-bottom:2px;">${escapeHtml(m.senderName || '')}</div>` : ''}
        ${replyHtml}
        ${mediaHtml}
        <div>${escapeHtml(m.text || '')}</div>
        ${reactionsHtml}
        <div class="msg-meta">
          <span>${formatTime(m.createdAt)}</span>
          ${isOut ? `<span style="color:${m.status === 'READ' ? '#53bdeb' : 'inherit'}">✓✓</span>` : ''}
        </div>
      </div>
      <div class="msg-actions">
        <button data-act="reply">Reply</button>
        <button data-act="react">❤️</button>
        <button data-act="del">Delete</button>
      </div>
    `;

    row.querySelector('[data-act="reply"]').onclick = () => {
      replyTarget = { id: m.id, senderName: m.senderName || 'User', preview: m.text || m.fileName || 'Media' };
      $('replySender').textContent = replyTarget.senderName;
      $('replyPreviewText').textContent = replyTarget.preview;
      $('replyBar').classList.remove('hidden');
      $('messageInput').focus();
    };
    row.querySelector('[data-act="react"]').onclick = async () => {
      const current = m.reactions?.[myUid];
      await updateDoc(doc(db, 'chats', state.activeChatId, 'messages', m.id), {
        [`reactions.${myUid}`]: current === '❤️' ? null : '❤️',
      });
    };
    row.querySelector('[data-act="del"]').onclick = async () => {
      await updateDoc(doc(db, 'chats', state.activeChatId, 'messages', m.id), {
        deletedFor: arrayUnion(myUid),
      });
    };

    container.appendChild(row);
  });
  container.scrollTop = container.scrollHeight;
}

async function sendCurrentMessage(extra = {}) {
  const chatId = state.activeChatId;
  if (!chatId) return;
  const text = $('messageInput').value.trim();
  if (!text && !extra.mediaUrl) return;

  const msgId = crypto.randomUUID();
  const now = Date.now();
  const me = state.user;
  const payload = {
    id: msgId,
    chatId,
    senderId: me.uid,
    senderName: me.displayName,
    senderPhone: me.phoneNumber,
    type: extra.type || 'TEXT',
    text,
    mediaUrl: extra.mediaUrl || null,
    mediaMimeType: extra.mediaMimeType || null,
    fileName: extra.fileName || null,
    replyToMessageId: replyTarget?.id || null,
    replyToSenderName: replyTarget?.senderName || null,
    replyToPreview: replyTarget?.preview || null,
    reactions: {},
    deletedFor: [],
    status: 'SENT',
    createdAt: now,
  };

  $('messageInput').value = '';
  replyTarget = null;
  $('replyBar').classList.add('hidden');

  await setDoc(doc(db, 'chats', chatId, 'messages', msgId), payload);
  await updateDoc(doc(db, 'chats', chatId), {
    lastMessagePreview: text || extra.fileName || extra.type,
    lastMessageSenderId: me.uid,
    lastMessageType: payload.type,
    lastMessageStatus: 'SENT',
    lastMessageTime: now,
  });
}

$('sendMessageBtn').onclick = () => sendCurrentMessage();
$('messageInput').onkeydown = (e) => {
  if (e.key === 'Enter') sendCurrentMessage();
};
$('cancelReplyBtn').onclick = () => {
  replyTarget = null;
  $('replyBar').classList.add('hidden');
};
$('emojiQuickBtn').onclick = () => {
  $('messageInput').value += '😊';
  $('messageInput').focus();
};

// File / Image upload to Firebase Storage
if ($('fileInput')) $('fileInput').onchange = async (e) => {
  const file = e.target.files?.[0];
  if (!file || !state.activeChatId) return;
  try {
    const sRef = storageRef(storage, `chats/${state.activeChatId}/${Date.now()}_${file.name}`);
    await uploadBytes(sRef, file);
    const url = await getDownloadURL(sRef);
    await sendCurrentMessage({
      type: file.type.startsWith('image/') ? 'IMAGE' : 'DOCUMENT',
      mediaUrl: url,
      mediaMimeType: file.type,
      fileName: file.name,
    });
  } catch (err) {
    alert('Storage upload failed (enable Firebase Storage in Console): ' + err.message);
  }
  e.target.value = '';
};

// Voice Note Recording -> Firebase Storage
$('recordVoiceBtn').onclick = async () => {
  if (mediaRecorder && mediaRecorder.state === 'recording') {
    mediaRecorder.stop();
    $('recordVoiceBtn').classList.remove('recording');
    return;
  }
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    audioChunks = [];
    mediaRecorder = new MediaRecorder(stream);
    mediaRecorder.ondataavailable = (ev) => audioChunks.push(ev.data);
    mediaRecorder.onstop = async () => {
      const blob = new Blob(audioChunks, { type: 'audio/webm' });
      stream.getTracks().forEach((t) => t.stop());
      try {
        const sRef = storageRef(storage, `chats/${state.activeChatId}/${Date.now()}_voice.webm`);
        await uploadBytes(sRef, blob);
        const url = await getDownloadURL(sRef);
        await sendCurrentMessage({ type: 'VOICE', mediaUrl: url, mediaMimeType: 'audio/webm', fileName: 'Voice note' });
      } catch (err) {
        alert('Voice upload failed (enable Firebase Storage in Console): ' + err.message);
      }
    };
    mediaRecorder.start();
    $('recordVoiceBtn').classList.add('recording');
  } catch {
    alert('Microphone permission denied or unavailable.');
  }
};

// New Chat by Real Phone Number
$('newChatBtn').onclick = () => {
  openModal('Start Chat by Phone Number', `
    <input id="findPhoneInput" type="tel" placeholder="Search name or +8801712345678" />
    <p class="muted small" style="margin-top:6px;">Select a registered user below or enter a full international phone number.</p>
    <p id="findPhoneErr" class="error-text hidden" style="margin-top:6px;"></p>
    <button id="findAndChatBtn" class="btn-primary full-width" style="margin-top:10px;">Find & Start Chat</button>
    <div id="modalUsersList" style="max-height:220px;overflow-y:auto;margin-top:12px;"></div>
  `);

  const renderModalUsers = (term) => {
    const box = $('modalUsersList');
    const items = term.trim() ? findMatchingUsers(term) : state.users;
    box.innerHTML = items.length === 0 ? '<div class="muted small">No registered users yet.</div>' : '';
    items.forEach((u) => {
      const row = document.createElement('div');
      row.className = 'chat-item';
      const name = u.displayName || u.phoneNumber;
      row.innerHTML = `
        <div class="avatar-sm">${name[0].toUpperCase()}</div>
        <div class="chat-item-body">
          <div class="chat-item-title">${escapeHtml(name)}</div>
          <div class="chat-item-preview">${escapeHtml(u.phoneNumber)}</div>
        </div>
      `;
      row.onclick = async () => {
        closeModal();
        await openDirectChatWithUser(u);
      };
      box.appendChild(row);
    });
  };
  renderModalUsers('');
  $('findPhoneInput').oninput = (e) => renderModalUsers(e.target.value);

  $('findAndChatBtn').onclick = async () => {
    let phone = $('findPhoneInput').value.trim().replace(/[\s\-()]/g, '');
    if (phone.startsWith('01') && phone.length === 11) phone = `+880${phone.slice(1)}`;
    const errEl = $('findPhoneErr');
    errEl.classList.add('hidden');
    if (!phone.startsWith('+')) {
      errEl.textContent = 'Include country code, e.g. +88017...';
      errEl.classList.remove('hidden');
      return;
    }
    const q = query(collection(db, 'users'), where('phoneNumber', '==', phone), limit(1));
    const snap = await getDocs(q);
    if (snap.empty) {
      errEl.textContent = 'No registered user found with that phone number yet.';
      errEl.classList.remove('hidden');
      return;
    }
    const other = { uid: snap.docs[0].id, ...snap.docs[0].data() };
    closeModal();
    await openDirectChatWithUser(other);
  };
};

// New Group
$('newGroupBtn').onclick = () => {
  openModal('Create Group', `
    <input id="newGroupTitle" type="text" placeholder="Group Name" />
    <button id="createGroupBtn" class="btn-primary full-width" style="margin-top:12px;">Create Group</button>
  `);
  $('createGroupBtn').onclick = async () => {
    const title = $('newGroupTitle').value.trim();
    if (!title) return;
    const chatId = `grp_${crypto.randomUUID()}`;
    const me = state.user;
    await setDoc(doc(db, 'chats', chatId), {
      type: 'GROUP',
      title,
      description: '',
      participantIds: [me.uid],
      adminIds: [me.uid],
      lastMessagePreview: `${me.displayName} created group "${title}"`,
      lastMessageSenderId: me.uid,
      lastMessageType: 'SYSTEM',
      lastMessageStatus: 'SENT',
      lastMessageTime: Date.now(),
      disappearingSeconds: 0,
    });
    closeModal();
    selectChat(chatId);
  };
};

// Calls Signaling (Firestore)
async function startCall(callType) {
  const chat = state.chats.find((c) => c.id === state.activeChatId);
  if (!chat || chat.type === 'GROUP') return;
  const me = state.user;
  const peerUid = (chat.participantIds || []).find((id) => id !== me.uid);
  const peerName = chatDisplayTitle(chat);
  const callId = crypto.randomUUID();
  activeCallId = callId;

  await setDoc(doc(db, 'calls', callId), {
    id: callId,
    callerId: me.uid,
    callerName: me.displayName,
    callerPhone: me.phoneNumber,
    calleeId: peerUid,
    calleeName: peerName,
    calleePhone: chat.participantPhones?.[peerUid] || '',
    participants: [me.uid, peerUid],
    callType,
    status: 'RINGING',
    startedAt: Date.now(),
  });

  $('callAvatar').textContent = peerName[0].toUpperCase();
  $('callContactName').textContent = peerName;
  $('callStatusText').textContent = `${callType} Ringing...`;
  $('callOverlay').classList.remove('hidden');
  startCallTimer();
}

function showIncomingCall(call) {
  activeCallId = call.id;
  $('callAvatar').textContent = (call.callerName || 'U')[0].toUpperCase();
  $('callContactName').textContent = call.callerName || call.callerPhone;
  $('callStatusText').textContent = `Incoming ${call.callType} Call...`;
  $('callOverlay').classList.remove('hidden');
  updateDoc(doc(db, 'calls', call.id), { status: 'CONNECTED' }).catch(() => {});
  startCallTimer();
}

function startCallTimer() {
  callSeconds = 0;
  clearInterval(callTimer);
  callTimer = setInterval(() => {
    callSeconds++;
    $('callStatusText').textContent = `Call • ${formatDuration(callSeconds)}`;
  }, 1000);
}

function closeCallOverlay() {
  clearInterval(callTimer);
  activeCallId = null;
  $('callOverlay').classList.add('hidden');
}

if ($('voiceCallBtn')) $('voiceCallBtn').onclick = () => startCall('VOICE');
if ($('videoCallBtn')) $('videoCallBtn').onclick = () => startCall('VIDEO');
$('endCallBtn').onclick = async () => {
  if (activeCallId) {
    await updateDoc(doc(db, 'calls', activeCallId), { status: 'ENDED', endedAt: Date.now() }).catch(() => {});
  }
  closeCallOverlay();
};

// Statuses
function renderStatuses() {
  const list = $('statusList');
  list.innerHTML = '';
  state.statuses.forEach((s) => {
    const div = document.createElement('div');
    div.className = 'chat-item';
    div.innerHTML = `
      <div class="avatar-md" style="border:2px solid var(--emerald);">${(s.userName || 'U')[0].toUpperCase()}</div>
      <div class="chat-item-body">
        <div class="chat-item-title">${escapeHtml(s.userName || 'User')}</div>
        <div class="chat-item-preview">${formatTime(s.createdAt)}</div>
      </div>
    `;
    div.onclick = () => openStatusViewer(s);
    list.appendChild(div);
  });
}

let statusInterval = null;
function openStatusViewer(s) {
  $('statusViewerName').textContent = s.userName || 'User';
  $('statusViewerBody').textContent = s.text || '';
  $('statusViewerTime').textContent = formatTime(s.createdAt);
  $('statusProgressBar').style.width = '0%';
  $('statusOverlay').classList.remove('hidden');
  let p = 0;
  clearInterval(statusInterval);
  statusInterval = setInterval(() => {
    p += 2;
    $('statusProgressBar').style.width = p + '%';
    if (p >= 100) {
      clearInterval(statusInterval);
      $('statusOverlay').classList.add('hidden');
    }
  }, 80);
}
$('closeStatusBtn').onclick = () => {
  clearInterval(statusInterval);
  $('statusOverlay').classList.add('hidden');
};

$('addStatusBtn').onclick = () => {
  openModal('New Status Update', `
    <input id="newStatusText" type="text" placeholder="Share a status update..." />
    <button id="saveNewStatusBtn" class="btn-primary full-width" style="margin-top:12px;">Post Status</button>
  `);
  $('saveNewStatusBtn').onclick = async () => {
    const text = $('newStatusText').value.trim();
    if (!text) return;
    const id = crypto.randomUUID();
    const now = Date.now();
    await setDoc(doc(db, 'statuses', id), {
      id,
      userId: state.user.uid,
      userName: state.user.displayName,
      userPhone: state.user.phoneNumber,
      text,
      createdAt: now,
      expiresAt: now + 24 * 60 * 60 * 1000,
    });
    closeModal();
  };
};

// Calls List
function renderCalls() {
  const list = $('callsList');
  list.innerHTML = '';
  const uid = state.user.uid;
  state.calls.forEach((c) => {
    const isOut = c.callerId === uid;
    const peer = isOut ? c.calleeName : c.callerName;
    const div = document.createElement('div');
    div.className = 'chat-item';
    div.innerHTML = `
      <div class="avatar-md">${(peer || 'U')[0].toUpperCase()}</div>
      <div class="chat-item-body">
        <div class="chat-item-title">${escapeHtml(peer || 'Unknown')}</div>
        <div class="chat-item-preview">${isOut ? 'Outgoing' : 'Incoming'} ${c.callType} • ${formatTime(c.startedAt)}</div>
      </div>
      <span class="small muted">${c.status}</span>
    `;
    list.appendChild(div);
  });
}

// Settings & Profile
$('editProfileBtn').onclick = () => {
  openModal('Edit Profile', `
    <input id="editNameInput" type="text" value="${escapeHtml(state.user.displayName)}" style="margin-bottom:8px;" />
    <input id="editAboutInput" type="text" value="${escapeHtml(state.user.about || '')}" />
    <button id="saveProfileBtn" class="btn-primary full-width" style="margin-top:12px;">Save</button>
  `);
  $('saveProfileBtn').onclick = async () => {
    const displayName = $('editNameInput').value.trim() || state.user.displayName;
    const about = $('editAboutInput').value.trim();
    await updateDoc(doc(db, 'users', state.user.uid), { displayName, about });
    state.user.displayName = displayName;
    state.user.about = about;
    renderApp();
    closeModal();
  };
};

$('themeToggleBtn').onclick = () => {
  state.theme = state.theme === 'dark' ? 'light' : 'dark';
  localStorage.setItem('udus_theme', state.theme);
  applyTheme();
};
$('darkModeSwitch').onchange = (e) => {
  state.theme = e.target.checked ? 'dark' : 'light';
  localStorage.setItem('udus_theme', state.theme);
  applyTheme();
};
$('readReceiptsSwitch').onchange = (e) => {
  state.readReceipts = e.target.checked;
};
$('signOutBtn').onclick = async () => {
  if (state.user) {
    await updateDoc(doc(db, 'users', state.user.uid), { isOnline: false, lastSeenAt: Date.now() }).catch(() => {});
  }
  await signOut(auth);
};

function openModal(title, html) {
  $('modalTitle').textContent = title;
  $('modalBody').innerHTML = html;
  $('genericModal').classList.remove('hidden');
}
function closeModal() {
  $('genericModal').classList.add('hidden');
}
$('closeModalBtn').onclick = closeModal;

function formatTime(ms) {
  if (!ms) return '';
  return new Date(ms).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}
function formatDuration(sec) {
  return `${String(Math.floor(sec / 60)).padStart(2, '0')}:${String(sec % 60).padStart(2, '0')}`;
}
function escapeHtml(str) {
  return String(str).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
