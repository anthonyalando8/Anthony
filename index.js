const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendNotificationOnNewMeme = functions.database
  .ref("Memes/{memeId}")
  .onCreate(async (snapshot, context) => {
    const memeData = snapshot.val(); // Get the meme data
    const title = memeData.title;

    // Get all user device tokens from the database
    const usersSnapshot = await admin.database().ref("Users/{userID}/settings/notifications/deviceToken").once();
    const tokens = [];

    usersSnapshot.forEach((user) => {
      const token = user.val().deviceToken;
      if (token) {
        tokens.push(token); // Collect all device tokens
      }
    });

    if (tokens.length === 0) {
      console.log("No device tokens found.");
      return null;
    }

    // Create a notification payload
    const payload = {
      notification: {
        title: "New Meme Posted!",
        body: `Check out "${title}" now!`,
        clickAction: "FLUTTER_NOTIFICATION_CLICK",
      },
    };

    // Send notifications to all tokens
    try {
      const response = await admin.messaging().sendToDevice(tokens, payload);
      console.log("Notifications sent successfully:", response);
    } catch (error) {
      console.error("Error sending notifications:", error);
    }

    return null;
  });
