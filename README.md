# Task Gullak 🪙 — Task Rewards & Bulk Management Platform

**Task Gullak** is a modern, high-performance Android app and Web Admin Panel designed for real-time task distribution, AI-assisted proof verification, referral commissions, and multi-device Bulk Mode execution.

---

## 🌟 Key Features

### 📱 Android Application
* **Smart Offers & Task Modes (`NORMAL`, `BULK`, `BOTH`):**
  * **Normal Mode:** Single-device task execution with automated proof upload.
  * **Bulk Mode:** Multi-device task execution with customizable device limits for approved bulk members.
  * **Segmented Mode Toggle:** Seamlessly switch between Normal and Bulk modes for `BOTH` category tasks.
* **Strict Daily Limits & Reset Timer:**
  * Enforces a strict 5-task daily completion limit per user (24-hour rolling reset with live backward countdown timer `HH:MM:SS`).
* **Real-Time Payouts & Wallet System:**
  * Live wallet balance tracking, instant referral commission credits (1% lifetime), and direct UPI withdrawal requests.
* **Background Push Notifications & Sync:**
  * Integrated Firebase Cloud Messaging (FCM) and `TaskGullakBackgroundSyncService` for background push notifications (even when the app is completely closed) with user-mode notification filtering (`normal_users`, `bulk_users`, `all_users`).
* **Referrals & Daily Streaks:**
  * Unique referral codes with real-time referral tracking and daily login streak rewards.

### 💻 Web Admin Panel (`admin.html` / `admin-panel/`)
* **Bulk Access & User Management:**
  * Review pending bulk access applications, manage active bulk users, set approved device limits, and process limit increase/exit appeals.
* **Bulk Submissions Dashboard:**
  * Task-wise submission proofs review with image previews, custom wallet credit approvals, and one-click rejection options.
* **Task & Feedback Bank Creation:**
  * Create tasks with custom reward prices, duration, store links, mode types (`NORMAL`, `BULK`, `BOTH`), and private feedback comment pools.
* **Withdrawals & Support Tickets:**
  * Real-time UPI payout processing and in-app support ticket resolution.

---

## 🛠️ Tech Stack & Architecture

### **Android App (`/app`)**
* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material 3)
* **Architecture:** MVVM (Model-View-ViewModel) + Coroutines & Flow
* **Firebase Services:**
  * Firebase Authentication (Email/Password & Credential Manager)
  * Cloud Firestore (Real-time snapshots & ACID transactions)
  * Firebase Cloud Messaging (FCM Remote Messages)
* **Image Loading & Animation:** Coil, Lottie Compose

### **Web Admin Panel (`/admin-panel`)**
* **Frontend:** Modular Vanilla JavaScript (ES6+), HTML5, Modern Glassmorphism CSS
* **Firebase SDK:** Firebase Web SDK v10 (Modular Realtime Listeners)
* **Deployment:** Vercel / Static Web Hosting

---

## 📂 Project Structure

```
├── app/                        # Android App Module
│   └── src/main/java/com/taskgullak/app/
│       ├── data/
│       │   ├── model/          # TaskGullakModels (Data Classes & Firestore Mappings)
│       │   └── repository/     # TaskGullakRepository & Claim Handlers
│       ├── service/            # FCM Messaging & Background Sync Services
│       ├── ui/
│       │   ├── components/     # Reusable Compose Components (Timers, Buttons, Cards)
│       │   └── screens/        # Main App Screens (Home, Offers, Profile, Wallet)
│       └── util/               # TaskGullakNotificationHelper
├── admin.html                  # Monolithic Web Admin Panel Entry Point
├── admin-panel/                # Web Admin Panel Root Folder
│   ├── index.html              # Admin Dashboard HTML
│   ├── css/                    # Admin Panel Stylesheets
│   └── js/                     # Core Modules & Views (bulk-access.js, tasks.js, etc.)
└── build.gradle.kts            # Root Build Script
```

---

## 🚀 How to Run & Build

### **1. Android Application**
1. Open the project directory in **Android Studio**.
2. Ensure `google-services.json` is placed inside the `/app` folder.
3. Sync Gradle project files (`./gradlew sync`).
4. Build & Run the app on an Android Emulator or physical device:
   ```bash
   ./gradlew app:assembleDebug
   ```

### **2. Web Admin Panel**
1. Open `admin.html` or `admin-panel/index.html` in any web browser.
2. Log in using authorized admin credentials configured in Firebase Auth.
3. Alternatively, deploy to Vercel or any static web host:
   ```bash
   npx vercel
   ```

---

## 📄 License & Attribution
© 2026 **TASK GULLAK**. All rights reserved.
