const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendNotificationOnNewMessage = functions.database
    .ref("/Chats/{receiverRoom}/messages/{messageId}")
    .onCreate((snapshot, context) => {
      const messageData = snapshot.val();
      const receiverUid = context.params.receiverRoom.split("_")[1];

      return admin
          .database()
          .ref(`/Users/${receiverUid}/fcmToken`)
          .once("value")
          .then((tokenSnapshot) => {
            const fcmToken = tokenSnapshot.val();

            if (!fcmToken) {
              console.log("No FCM token for the recipient");
              return null;
            }

            const payload = {
              notification: {
                title: `New message from ${messageData.senderName}`,
                body: messageData.message || "You received a new message",
                click_action: "OPEN_CHAT_ACTIVITY",
              },
              data: {
                chatId: context.params.receiverRoom,
              },
            };

            return admin
                .messaging()
                .sendToDevice(fcmToken, payload)
                .then((response) => {
                  console.log("Notification sent successfully:", response);
                  return null;
                })
                .catch((error) => {
                  console.error("Error sending notification:", error);
                });
          });
    });
