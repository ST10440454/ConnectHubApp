# ConnectHub — POE Prototype
## Presentation:
https://youtu.be/7Px73yH0JKo

A working Android prototype of ConnectHub, built as a real 1:1 messaging app in the
style of WhatsApp: an onboarding screen, a Chats List of contacts, individual
conversations with read receipts and timestamps, registration/login with encrypted
passwords (via Firebase Authentication), multi-language UI support, and a settings screen with
persisted preferences. Architecture follows Clean Architecture + MVI, as described in
the Part 1 planning document.

**UI approach:** built on Android Studio's "Empty View Activity" template — a single
Activity (`MainActivity`, extending `AppCompatActivity`) hosts a `NavHostFragment`,
with one `Fragment` + XML layout per screen (Onboarding, Login, Register, Chats List,
Chat, Settings), wired together via the Jetpack Navigation Component. ViewBinding is
used throughout instead of `findViewById`, and there is no Jetpack Compose anywhere
in this project.

**Screen flow:** Onboarding → Login/Register → **Chats List** (shows the people in your
phone contacts who are on ConnectHub, WhatsApp-style, with a last-message preview) → tap a contact → **Chat**
(a real 1:1 conversation, not a shared room) → Settings (reachable from the Chats List
toolbar).

## 1. Multi-language support (DeepL)

The app ships **English only**. Other languages are produced at runtime by the DeepL API:
when the user picks a language in Settings, the app's English UI text is translated once,
cached on the device (so later launches and offline use need no network), and the screen
is redrawn in the new language.

**DeepL supports only three of South Africa's 11 official languages**, so the Settings
dropdown offers English, Afrikaans, isiXhosa and isiZulu.

How it works:
- `data/remote/DeepLTranslationService.kt` calls `POST /v2/translate` (Free keys end in `:fx`).
- `data/i18n/LanguageManager.kt` caches translations in `filesDir/translations_<code>.json`,
  remembers the chosen language, and rewrites the text of each screen after it is created
  (hooked once in `MainActivity` via fragment lifecycle callbacks).
- `data/i18n/TranslatableStrings.kt` lists the strings that are sent. **When you add a new
  string to `values/strings.xml`, add its id there too.**
- Code that needs text in Kotlin uses `context.tr(R.string.x)` instead of `getString`.

Limitations: the first switch to a language needs internet; translations are machine
generated, so have a native speaker review them; hard-coded English messages in Kotlin
(validation and error messages) are not translated; and the DeepL key is compiled into the
app, so it is extractable from the APK. Fine for a prototype on a free key, but a production
app should call DeepL through your own backend.

## 2. Device contacts (find people you already know)

The **New Chat** screen (tap the floating "+" button on the Chats List) reads the
phone's own contact list and shows which of your phone contacts are already
registered on ConnectHub — tap one to start chatting immediately. Contacts who
haven't registered yet show an **Invite** button that opens the system SMS app with
a pre-filled message instead.

**How matching works:** registration now has an phone number field. When
you register with a phone number, other users' phone-book entries can match against
it. Matching is done by normalizing both numbers (stripping everything but digits and
comparing the last 9 digits — see `normalizePhoneNumber()`), so `+27 71 234 5678`,
`0712345678`, and `71 234 5678` are all recognised as the same number regardless of
formatting differences.

**Permission handling:** the app requests `READ_CONTACTS` the first time you open New
Chat, using Android's standard runtime permission dialog. If denied, a clear
explanation screen appears with a button that deep-links straight to the app's system
Settings page, in case the person wants to reconsider. Nothing about this permission
is requested anywhere else in the app — it's scoped tightly to this one screen.

**Contacts-only messaging:** you can only message people whose phone number is in your
phone book. Opening New Chat (or the Chats List, once the permission is granted) matches
the phone book against registered users and saves the matches as your contacts in
Firestore (`users/{uid}/contacts/{contactUid}`). The Chats List shows only those people
(plus anyone who has messaged you, so incoming messages are never hidden), and
`SendMessageUseCase` / `SendImageMessageUseCase` refuse to send to anyone else. The
Firestore rules also require the receiver to be in the sender's contact list before a
message can be created. Because the phone book lives on the device, the server can't prove
a number is really in it; the rules stop the app and any client that doesn't edit its own
contact list from messaging strangers. After publishing the new `firestore.rules`, open the
app once on each device so the contact list is created before sending.

## 3. Image sharing

Tap the paperclip icon next to the message input field to attach and send a photo.
This uses Android's built-in **Photo Picker** (`ActivityResultContracts.PickVisualMedia`)
the key advantage is that it supports any supported Android version,
since the picker runs in a separate system-owned process and only hands the app the
one image actually selected.

The picked image is read into memory (checked in
`SendImageMessageUseCase` before any network call), uploaded to **Firebase Storage**
under `chat_images/<conversationId>/<uuid>.<ext>`, and the resulting download URL is
sent as a Firestore message with `type = IMAGE`. `MessageAdapter` loads that URL into
the bubble using **Coil** (`ImageView.load(url)`), which was already a dependency
included from the start of this project specifically to demonstrate the "use an
external library" outcome — this is where it's actually put to work now.

The Chats List preview shows "📷 Photo" instead of raw text for a conversation whose
most recent message is an image, so you always know at a glance what kind of message
is waiting without opening the conversation.

## 4. Run the app

You will need **two accounts** to properly test
conversations — needs two physical devices.

1. Select an emulator or physical device (min API 26).
2. Click Run. You should land on the **Onboarding** screen — tap **Get Started**.
3. Tap **Register**, create an account (e.g. "Sarah", `sarah@example.com`), and this
   time also fill in the optional phone number field with a real number reachable
   from your second test device's contacts (needed for step 6). Check the Firebase
   console (Authentication tab and Firestore `users` collection) to see it appear live.
4. Repeat step 3 on a second device/emulator with a different account (e.g. "Michael"),
   also with a phone number.
5. On Sarah's device the **Chats List** is still empty: it only shows people whose phone
   number is in your phone contacts. Save Michael's number as a contact on Sarah's
   device (and Sarah's on Michael's, so replies work both ways).
6. Tap the **+** floating button on the Chats List to open **New Chat**. Grant the
   contacts permission when prompted. If Michael's phone number is saved as a contact
   on Sarah's device (and matches what Michael registered with), his row should show
   "On ConnectHub" with a **Chat** button; other phone contacts show **Invite** instead.
   Michael is now saved as Sarah's contact and appears on her Chats List. Trying to
   message someone who isn't a contact is refused.
7. Tap Michael's row (from either the Chats List or New Chat) to open the conversation.
   Send a text message — check the Firestore `messages` collection to see it appear in
   real time with a `conversationId` field.
8. Tap the paperclip icon, pick a photo from the gallery, and send it — check that it
   uploads (watch Firebase Storage in the console for the new file under
   `chat_images/`) and appears as an image bubble, not text.
9. On Michael's device, open the same conversation — both the text message and the
   image should appear, and opening it marks Sarah's messages as read, which turns her
   sent-message ticks gold (see `ic_check_read.xml`).
10. From the Chats List toolbar, open **Settings**, toggle dark mode / notifications —
    kill and reopen the app to confirm the settings persisted (handled by Jetpack
    DataStore, purely local).
11. Still in Settings, tap the Language dropdown and pick e.g. isiZulu. The screen
    should immediately relabel itself in isiZulu (button text, toggle labels, etc.) —
    go back to the Chats List and Chat screens to see their labels change too. The first
    switch needs internet (it calls DeepL); then switch back to English to confirm it
    reverts cleanly.

## 5. What's implemented 

**Implemented:**
- Onboarding screen (branded welcome, first thing a fresh/logged-out user sees)
- Registration & login with Firebase Auth (password encryption handled by Firebase)
- **Chats List screen** (WhatsApp-style): your contacts (registered users whose phone
  number is in your phone book), with a live last-message preview and timestamp, sorted
  by most recent activity
- **Real 1:1 conversations** — each pair of users gets a deterministic `conversationId`
  (sorted UID pair), not a single shared room; messages are queried per-conversation
- **Read receipts**: opening a conversation marks the other person's unread messages
  as READ; sent-message bubbles show single/double/gold ticks for SENT/DELIVERED/READ
- **Message timestamps** rendered on every bubble, sent/received bubble styling with
  initials-based avatar circles on the Chats List
- Settings screen: dark mode + notification toggles, persisted via DataStore
- **Multi-language UI**: English, Afrikaans, isiXhosa and isiZulu (the South
  African official languages DeepL supports), selectable in Settings and translated at
  runtime through the DeepL API, then cached on the device
- **Device contacts integration**: a "New Chat" screen reads the phone's contact list
  (`READ_CONTACTS` runtime permission) and matches entries against registered
  ConnectHub users by normalized phone number, so you can start a conversation with
  someone already in your phone book without hunting for them in the full user list
- **Contacts-only messaging**: you can only message people whose phone number is in your
  phone book. The matches are saved to Firestore as your contact list, the app refuses
  to send to anyone else, and the Firestore rules require the receiver to be in the
  sender's contact list 
- **Image sharing**: attach and send photos in any conversation via Android's Photo
  Picker, uploaded to Firebase Storage and rendered in
  the chat bubble via Coil
- Input validation across all forms (empty fields, short passwords, blank messages,
  oversized/unreadable images)
- Unit tests for validators, use cases (incl. the conversation ID helper, image-message
  validation, and device-contact matching), and the supported-languages list

## 6. Project structure

```
domain/
  model/      → User (now carries phoneNumber), Message (conversationId/receiverId/
                type/imageUrl), Contact (phoneNumber), ConversationPreview
                (lastMessageIsImage), DeviceContact, DeviceContactMatch,
                Language/SupportedLanguages (en, af, xh, zu), conversationIdFor(),
                normalizePhoneNumber()
  repository/ → AuthRepository, MessageRepository (now incl. sendImageMessage —
                takes a plain ByteArray, not android.net.Uri, to stay Android-free),
                ContactRepository, DeviceContactRepository
                (interfaces only, no Android/Firebase deps)
  usecase/    → Register/LoginUserUseCase, Send/SendImageMessage/ObserveMessagesUseCase,
                ObserveConversationsUseCase (combines contacts + messages into
                Chats List previews), GetDeviceContactMatchesUseCase (matches phone
                book against registered users)

data/
  remote/     → FirebaseAuthService, FirestoreService (conversation-scoped
                queries + observeUsers + markMessagesAsRead), FirebaseStorageService
                (image uploads), DeviceContactsService (ContentResolver phone-book read
                — the one class in this app that touches Android's ContactsContract)
  repository/ → Auth/Message/Contact/DeviceContactRepositoryImpl

presentation/
  onboarding/ → OnboardingFragment (no ViewModel needed — just navigates onward)
  auth/       → AuthViewModel (shared contract, now incl. optional phone field) +
                LoginFragment + RegisterFragment
  chatlist/   → ChatListViewModel + ChatListFragment (now with a New Chat FAB) +
                ConversationAdapter (RecyclerView) — the WhatsApp-style home screen
  newchat/    → NewChatViewModel + NewChatFragment (READ_CONTACTS permission flow,
                SMS-invite intent for non-users) + DeviceContactAdapter
  chat/       → ChatViewModel (conversation-scoped: takes conversationId/
                receiverId/contactName, now also handles SendImage intent) +
                ChatViewModelFactory (dedicated factory, since these args come from
                navigation, not the generic DI container) + ChatFragment (Photo
                Picker integration) + MessageAdapter (RecyclerView, ticks +
                timestamps + Coil-loaded images)
  settings/   → SettingsViewModel (dark mode/notifications only) + SettingsFragment
                (language dropdown calls LanguageManager directly — see
                Section 3)
  common/     → shared UI extensions + AvatarPalette (deterministic initials-avatar
                colours, used on the Chats List and New Chat screen)
  ViewModelFactory.kt → manual DI bridge for Auth/ChatList/NewChat/Settings ViewModels
                        (ChatViewModel is deliberately NOT here — see ChatViewModelFactory)
util/         → Validators, Result wrapper, exception-to-message mapping (incl.
                Firebase Storage error cases)

res/
  layout/       → one XML layout per fragment + message bubble (now with an image
                  slot) + conversation row + device contact row layouts
  navigation/   → nav_graph.xml (Onboarding → Login ↔ Register → ChatList → Chat,
                  ChatList → NewChat → Chat, ChatList → Settings)
  values/       → strings.xml (English, default), colors.xml, themes.xml (light)
  values-night/ → themes.xml (dark mode override)
  drawable/     → bubble backgrounds, tick icons (sent/delivered/read), avatar circle,
                  onboarding gradient, toolbar icons, launcher icon
```

**Navigation flow:** `nav_graph.xml` starts at Onboarding for a fresh/logged-out user
(`MainActivity` overrides the start destination to Chats List if a session already
exists). Register and Login lead to each other and both lead to Chats List on success,
clearing the back stack. Tapping a contact opens Chat with that conversation's ID
passed as a Bundle argument. Chats List's toolbar opens Settings; logging out clears
the back stack back to Login.


**Read receipts:** opening `ChatFragment` triggers `MessageRepository.markMessagesAsRead`,
which batch-updates any Firestore message where the current user is the receiver and
status isn't already READ. `MessageAdapter` reflects this with three tick states:
single (SENT), double white (DELIVERED — not currently set by any code path, see
Section 6), double gold (READ).


## Push notifications (Firebase Cloud Messaging + free Cloudflare Worker)

New chat messages notify the receiver even when the app is in the background,.

- The app stores each device's FCM token on `users/{uid}.fcmToken` (`PushTokenService`,
  called from `ChatListFragment`; cleared on logout).
- After a message is written, `PushRelayService` calls the relay with the new message id and
  the sender's Firebase ID token.
- `relay/` is a Cloudflare Worker. It verifies the token, loads the stored message, checks
  the caller is its sender, looks up the receiver's token and sends a data-only push
  through FCM. `ConnectHubMessagingService` turns that into a notification, honouring
  Settings > Notifications.
