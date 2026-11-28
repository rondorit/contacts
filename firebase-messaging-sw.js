// Background push for the On IT web app (iPhone: works when added to the Home Screen).
importScripts("https://www.gstatic.com/firebasejs/10.14.1/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/10.14.1/firebase-messaging-compat.js");
firebase.initializeApp({
  apiKey: "AIzaSyBnunFF6hnO7QsvzHJSBqHGMpqLbxs5rG8",
  authDomain: "bekis-e82cd.firebaseapp.com",
  projectId: "bekis-e82cd",
  storageBucket: "bekis-e82cd.firebasestorage.app",
  messagingSenderId: "1065393018022",
  appId: "1:1065393018022:web:8a128da0a08b5154919067"
});
// Messages carry a "notification" part, so the SDK shows them itself.
firebase.messaging();
self.addEventListener("notificationclick", e => {
  e.notification.close();
  e.waitUntil(clients.matchAll({ type: "window", includeUncontrolled: true }).then(ws => {
    for (const w of ws) if (w.url.includes("/contacts/")) return w.focus();
    return clients.openWindow("/contacts/");
  }));
});
