# ConnectHub — Part 2 Prototype

A working Android prototype of ConnectHub, built as a real 1:1 messaging app in the
style of WhatsApp: an onboarding screen, a Chats List of contacts, individual
conversations with read receipts and timestamps, registration/login with encrypted
passwords (via Firebase Auth), multi-language UI support, and a settings screen with
persisted preferences. Architecture follows Clean Architecture + MVI, as described in
the Part 1 planning document.

**UI approach:** built on Android Studio's "Empty View Activity" template — a single
Activity (`MainActivity`, extending `AppCompatActivity`) hosts a `NavHostFragment`,
with one `Fragment` + XML layout per screen (Onboarding, Login, Register, Chats List,
Chat, Settings), wired together via the Jetpack Navigation Component. ViewBinding is
used throughout instead of `findViewById`, and there is no Jetpack Compose anywhere
in this project.

**Screen flow:** Onboarding → Login/Register → **Chats List** (shows every other
registered user, WhatsApp-style, with a last-message preview) → tap a contact → **Chat**
(a real 1:1 conversation, not a shared room) → Settings (reachable from the Chats List
toolbar).

## 1. Open the project

1. Open Android Studio (Koala/2024.1 or newer recommended).
2. **File → Open** and select this `ConnectHub` folder.
3. Let Gradle sync. It will fail the first time — that's expected until step 2 of
   Firebase setup below.

## 2. Connect Firebase

1. Go to https://console.firebase.google.com and create a new project (e.g. `connecthub-app`).
2. Click **Add app → Android**. Use package name `com.connecthub.app`.
3. Download the generated `google-services.json` file.
4. Place it at: `ConnectHub/app/google-services.json` (same folder as `app/build.gradle.kts`).

   > **Note:** `google-services.json` is deliberately excluded from version control via
   > `.gitignore`, since it contains project-specific Firebase credentials. Each team
   > member downloads their own copy from the Firebase console. A placeholder showing
   > the expected shape is committed as `google-services.json.example`.

5. In the Firebase console, go to **Build → Authentication → Sign-in method** and enable
   **Email/Password**.
6. Go to **Build → Firestore Database → Create database**. Start in **test mode** for
   development, then before your final submission, replace the rules with the contents
   of `firestore.rules` in this repo (Firestore console → Rules tab → paste → Publish).
7. **Composite index (important — see below):** the Chats List and Chat screens query
   Firestore with `where("conversationId", "==", ...).orderBy("timestamp")`, which
   Firestore does not auto-index. The **first time you actually run this query from
   the app** (i.e. open a real conversation), Firestore will throw an error in Logcat
   that looks like `FAILED_PRECONDITION: The query requires an index...` followed by a
   direct link. Click that link (or copy it into a browser while logged into the
   Firebase console) — it pre-fills the index creation form. Click **Create Index** and
   wait 1–2 minutes for it to build. You only need to do this once per Firebase project.
   Skipping this step means messages never load in a conversation.
8. **Enable Firebase Storage** (required for image sharing — see Section 4): go to
   **Build → Storage → Get started**. Choose **Start in test mode** for development,
   then before final submission replace the rules with `storage.rules` from this repo
   (Storage console → Rules tab → paste → Publish). **Good news:** unlike Cloud
   Translation, Firebase Storage's free (Spark) tier requires no billing account or
   credit card at all — this is a much lighter setup step than the Translation API was.
9. Re-sync Gradle in Android Studio. The build should now succeed.

## 3. Multi-language support 

**This app deliberately does NOT use a cloud translation API.** An earlier version of
this project used Google Cloud Translation, but that requires a billing account —
and in testing, Google's billing signup demanded a one-time, refundable-but-real
US$30 prepayment for accounts in some regions. Rather than require that of every
student building this project, multi-language support here is done the way the
original Part 1 design document actually specified: **Android resource qualifiers**.

How it works:
- `res/values/strings.xml` holds the English (default) UI strings.
- `res/values-af/`, `res/values-zu/`, `res/values-xh/`, `res/values-st/`, `res/values-tn/`,
  `res/values-ts/`, `res/values-ss/`, `res/values-ve/`, `res/values-nr/`, and
  `res/values-b+nso/` each hold a translated `strings.xml` with the identical set of
  string keys — one folder per South African official language (all 11, including
  Venda, which a live-translation API wouldn't have supported anyway).
- The `values-b+nso` folder name looks unusual on purpose: Sepedi/Northern Sotho only
  has a 3-letter ISO 639-2 code (`nso`), not a 2-letter ISO 639-1 code, so Android's
  resource-folder-qualifier system requires the BCP-47 `b+` prefix syntax for it. Every
  other language here has a normal 2-letter code and a plain `values-<code>` folder.
- The Settings screen's language dropdown calls
  `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))`,
  Android's standard "per-app language preferences" API. This immediately swaps which
  `values-<code>` folder the app pulls strings from, and persists the choice
  automatically — no DataStore or backend involved. It also shows up in the phone's own
  Settings → Apps → ConnectHub → Language screen on Android 13+, since
  `res/xml/locales_config.xml` declares which locales the app supports.

**Important — translation quality:** the strings in each `values-<code>/strings.xml`
were produced without access to a professional translation service or native-speaker
review. Afrikaans, isiZulu, isiXhosa, Sesotho, and Setswana are reasonably
well-established; Xitsonga, siSwati, isiNdebele, and especially Tshivenda are
lower-confidence best-effort translations. **Before presenting this as a finished
feature (e.g. in your final PoE or to any real isiZulu/Xitsonga/etc. speaker), have
someone who speaks that language check the `values-<code>/strings.xml` files.** This
is flagged directly in the code comment on `SupportedLanguages` too, so it isn't lost
if someone else picks up the project later.

**Note on scope:** this only translates the app's own UI chrome (buttons, labels,
titles) — not user-typed chat message content. Live message translation would need a
cloud API like Cloud Translation, which is exactly what this approach avoids. If your
module specifically requires live message translation rather than UI localization,
that's a deliberate trade-off worth mentioning to your marker/lecturer, along with the
billing obstacle that led to it.

## 4. Device contacts (find people you already know)

The **New Chat** screen (tap the floating "+" button on the Chats List) reads the
phone's own contact list and shows which of your phone contacts are already
registered on ConnectHub — tap one to start chatting immediately. Contacts who
haven't registered yet show an **Invite** button that opens the system SMS app with
a pre-filled message instead.

**How matching works:** registration now has an *optional* phone number field. When
you register with a phone number, other users' phone-book entries can match against
it. Matching is done by normalizing both numbers (stripping everything but digits and
comparing the last 9 digits — see `normalizePhoneNumber()`), so `+27 71 234 5678`,
`0712345678`, and `71 234 5678` are all recognised as the same number regardless of
formatting differences.

**Known limitation:** an account registered *without* a phone number (it's optional,
not required) can never be matched this way — it just won't show up as "on
ConnectHub" to anyone's phone-book contacts, even though it still works fine and
appears normally on the Chats List for direct discovery. This is documented in code
on `GetDeviceContactMatchesUseCase`.

**Permission handling:** the app requests `READ_CONTACTS` the first time you open New
Chat, using Android's standard runtime permission dialog. If denied, a clear
explanation screen appears with a button that deep-links straight to the app's system
Settings page, in case the person wants to reconsider. Nothing about this permission
is requested anywhere else in the app — it's scoped tightly to this one screen.

## 5. Image sharing

Tap the paperclip icon next to the message input field to attach and send a photo.
This uses Android's built-in **Photo Picker** (`ActivityResultContracts.PickVisualMedia`)
rather than the classic gallery-intent approach — the key advantage is that **no
storage or media permission is required at all**, on any supported Android version,
since the picker runs in a separate system-owned process and only hands the app the
one image actually selected.

The picked image is read into memory, capped at 8 MB (checked in
`SendImageMessageUseCase` before any network call), uploaded to **Firebase Storage**
under `chat_images/<conversationId>/<uuid>.<ext>`, and the resulting download URL is
sent as a Firestore message with `type = IMAGE`. `MessageAdapter` loads that URL into
the bubble using **Coil** (`ImageView.load(url)`), which was already a dependency
included from the start of this project specifically to demonstrate the "use an
external library" outcome — this is where it's actually put to work now.

The Chats List preview shows "📷 Photo" instead of raw text for a conversation whose
most recent message is an image, so you always know at a glance what kind of message
is waiting without opening the conversation.

## 6. Run the app

You will need **two accounts** to properly test
conversations — either two emulators, an emulator + a physical device, or two physical
devices.

1. Select an emulator or physical device (min API 26).
2. Click Run. You should land on the **Onboarding** screen — tap **Get Started**.
3. Tap **Register**, create an account (e.g. "Sarah", `sarah@example.com`), and this
   time also fill in the optional phone number field with a real number reachable
   from your second test device's contacts (needed for step 6). Check the Firebase
   console (Authentication tab and Firestore `users` collection) to see it appear live.
4. Repeat step 3 on a second device/emulator with a different account (e.g. "Michael"),
   also with a phone number.
5. On Sarah's device, you should now see Michael listed on the **Chats List** screen
   (and vice versa) — this list is every other registered user, refreshed live.
6. Tap the **+** floating button on the Chats List to open **New Chat**. Grant the
   contacts permission when prompted. If Michael's phone number is saved as a contact
   on Sarah's device (and matches what Michael registered with), his row should show
   "On ConnectHub" with a **Chat** button; other phone contacts show **Invite** instead.
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
    go back to the Chats List and Chat screens to see their labels change too. Switch
    back to English to confirm it reverts cleanly. Note: this changes the app's own UI
    text, not the content of chat messages you've typed or received — see Section 3
    for why.


## 6. What's implemented vs. deferred

**Implemented (Part 2 scope):**
- Onboarding screen (branded welcome, first thing a fresh/logged-out user sees)
- Registration & login with Firebase Auth (password encryption handled by Firebase)
- **Chats List screen** (WhatsApp-style): every other registered user, with a live
  last-message preview and timestamp, sorted by most recent activity
- **Real 1:1 conversations** — each pair of users gets a deterministic `conversationId`
  (sorted UID pair), not a single shared room; messages are queried per-conversation
- **Read receipts**: opening a conversation marks the other person's unread messages
  as READ; sent-message bubbles show single/double/gold ticks for SENT/DELIVERED/READ
- **Message timestamps** rendered on every bubble, sent/received bubble styling with
  initials-based avatar circles on the Chats List
- Settings screen: dark mode + notification toggles, persisted via DataStore
- **Multi-language UI**: all 11 official South African languages, selectable in
  Settings, applied instantly via Android's per-app language preferences
  (`AppCompatDelegate.setApplicationLocales`) and static `values-<code>/strings.xml`
  resource files — no API, no billing, works fully offline
- **Device contacts integration**: a "New Chat" screen reads the phone's contact list
  (`READ_CONTACTS` runtime permission) and matches entries against registered
  ConnectHub users by normalized phone number, so you can start a conversation with
  someone already in your phone book without hunting for them in the full user list
- **Image sharing**: attach and send photos in any conversation via Android's Photo
  Picker (no storage permission needed), uploaded to Firebase Storage and rendered in
  the chat bubble via Coil
- Input validation across all forms (empty fields, short passwords, blank messages,
  oversized/unreadable images)
- Friendly error messages instead of crashes (`Result.kt` maps Firebase and Storage
  exceptions)
- Unit tests for validators, use cases (incl. the conversation ID helper, image-message
  validation, and device-contact matching), and the supported-languages list

## 7. Project structure

```
domain/
  model/      → User (now carries phoneNumber), Message (conversationId/receiverId/
                type/imageUrl), Contact (phoneNumber), ConversationPreview
                (lastMessageIsImage), DeviceContact, DeviceContactMatch,
                Language/SupportedLanguages (11 languages), conversationIdFor(),
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
                (language dropdown handled directly via AppCompatDelegate — see
                Section 3 — since it's a system-level concern, not app state)
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
  values-<code>/→ ten translated strings.xml files, one per supported language
                  (see Section 3)
  values-night/ → themes.xml (dark mode override)
  xml/          → locales_config.xml (declares supported locales for Android 13+
                  system Settings integration)
  drawable/     → bubble backgrounds, tick icons (sent/delivered/read), avatar circle,
                  onboarding gradient, toolbar icons, launcher icon
```

**Navigation flow:** `nav_graph.xml` starts at Onboarding for a fresh/logged-out user
(`MainActivity` overrides the start destination to Chats List if a session already
exists). Register and Login lead to each other and both lead to Chats List on success,
clearing the back stack. Tapping a contact opens Chat with that conversation's ID
passed as a Bundle argument. Chats List's toolbar opens Settings; logging out clears
the back stack back to Login.

**Why ChatViewModel has its own factory:** every other ViewModel in this app only
needs the shared `AppContainer`, so the generic `ViewModelFactory` handles them all
identically. `ChatViewModel` is different — it's scoped to one specific conversation,
so it also needs `conversationId`, `receiverId`, and `contactName` from the Fragment's
navigation arguments. Rather than bloat the generic factory with conversation-specific
parameters that no other ViewModel needs, `ChatViewModelFactory` exists solely to
carry those three extra values alongside the container.

**Conversation ID scheme:** `conversationIdFor(uidA, uidB)` sorts the two UIDs and
joins them with `_`, so both participants always compute the identical ID regardless
of who opened the chat first — no separate "conversation" document needs to be created
or looked up ahead of time.

**Read receipts:** opening `ChatFragment` triggers `MessageRepository.markMessagesAsRead`,
which batch-updates any Firestore message where the current user is the receiver and
status isn't already READ. `MessageAdapter` reflects this with three tick states:
single (SENT), double white (DELIVERED — not currently set by any code path, see
Section 6), double gold (READ).
