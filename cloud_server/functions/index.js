/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */
// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });
// const {onRequest} = require("firebase-functions/v2/https");
// const logger = require("firebase-functions/logger");


const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

var usersInformation = {};

const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

let usersInformation = {};

exports.populateInitialData = functions.https.onRequest(async (req, res) => {
  try {
    const usersSnapshot = await admin.database().ref("Users").once("value");
    const users = usersSnapshot.val();

    for (const userID in users) {
      const userData = users[userID];
      const followers = userData.followers || [];
      const following = userData.following || [];
      let followersToken = [];
      let followingToken = [];

      for (const followerID of followers) {
        const followerSnapshot = await admin.database().ref(`Users/${followerID}`).once("value");
        const followerData = followerSnapshot.val();
        const followerToken = followerData?.settings?.notifications?.deviceToken || null;

        if (followerToken) {
          followersToken.push(followerToken);
        }
      }

      for (const followingID of following) {
        const followingSnapshot = await admin.database().ref(`Users/${followingID}`).once("value");
        const followingData = followingSnapshot.val();
        const followingToken = followerData?.settings?.notifications?.deviceToken || null;
        
        if(followingToken) {
            followingToken.push(followingToken);
        }
      }

      usersInformation[userID] = {
        myToken: userData.settings.notifications.deviceToken || null,
        friendsToken: followersToken.concat(followingToken),
      };
    }

    console.log("Initial data populated:", usersInformation);

    res.status(200).send("Initial data populated successfully.");

  } catch (error) {
    console.error("Error populating initial data:", error);
    res.status(500).send("Error populating initial data.");
  }
});

exports.updateTokenAndFollowers = functions.database
  .ref("Users/{userID}")
  .onUpdate(async (change, context) => {
    const userID = context.params.userID;
    const beforeData = change.before.val(); // Data before the update
    const afterData = change.after.val(); // Data after the update

    const beforeFollowing = beforeData.following || [];
    const afterFollowing = afterData.following || [];
    const userToken = afterData.settings?.notifications?.deviceToken || null;

    // Find newly added IDs in `following`
    const newFollowing = afterFollowing.filter((id) => !beforeFollowing.includes(id));

    // Helper function to get tokens for a list of user IDs
    const getTokens = async (userIDs) => {
      const tokens = [];
      for (const userID of userIDs) {
        try {
          const userSnapshot = await admin
            .database()
            .ref(`Users/${userID}/settings/notifications/deviceToken`)
            .once("value");
          const token = userSnapshot.val();
          if (token) tokens.push(token);
        } catch (error) {
          console.error(`Error fetching token for user ${userID}:`, error);
        }
      }
      return tokens;
    };

    try {
      // Get tokens for followers and following
      const followers = afterData.followers || [];
      const followingTokens = await getTokens(afterFollowing);
      //const newFollowingTokens = await getTokens(newFollowing); // Tokens for newly added `following`

      // Update usersInformation
      usersInformation[userID] = {
        myToken: userToken,
        friendsToken: followingTokens, // All `following` tokens
        //newFriendsToken: newFollowingTokens, // Only new `following` tokens
      };
      if (newFollowing.length > 0) {
        console.log("New following IDs:", newFollowing);
        // Proceed with further actions, send notifications for new followers, etc.
      } else {
        console.log("No new following IDs were added.");
      }

      console.log(`Updated tokens for user ${userID}:`, usersInformation[userID]);
    } catch (error) {
      console.error(`Error updating tokens for user ${userID}:`, error);
    }

    return null; // Indicate completion of the function
});

exports.notifiyMessage = functions.database
  .ref("Messages/{chatListId}")
  .onUpdate(async (change, context) => {
    const chatListId = context.params.chatListId;
    const beforeMessages = change.before.child("messages").val() || {};
    const afterMessages = change.after.child("messages").val() || {};

    // Get keys for messages before and after the update
    const beforeMessageKeys = Object.keys(beforeMessages);
    const afterMessageKeys = Object.keys(afterMessages);

    // Identify new messages by comparing keys
    const newMessageKeys = afterMessageKeys.filter(
      (key) => !beforeMessageKeys.includes(key)
    );

    if (newMessageKeys.length > 0) {
      try {
        for (const messageID of newMessageKeys) {
          const message = afterMessages[messageID];
          const messageSender = message.sender;
          const messageRecipient = message.recipient;
          const messageSeen = message.seen;
          const messageBody = message.message;

          if (!messageSeen) {
            if (usersInformation[messageRecipient]) {
              const token = usersInformation[messageRecipient].myToken;

              if (token) {
                const tokens = [token];
                const notification = {
                  title: "New message",
                  body: `You have a new message: ${messageBody}`,
                  sender: messageSender,
                  timestamp: Date.now(),
                  data: {
                    chatListId: chatListId,
                    messageID: messageID,
                    type: "message",
                  },
                };

                // Send notification
                await sendNotification(tokens, notification);
                console.log(
                  `Notification sent to user ${messageRecipient} for message ${messageID} from ${messageSender}.`
                );
              } else {
                console.warn(
                  `No token found for recipient ${messageRecipient}. Notification skipped.`
                );
              }
            } else {
              console.warn(
                `User information not found for recipient ${messageRecipient}. Notification skipped.`
              );
            }
          }
        }
      } catch (error) {
        console.error(
          `Error processing new messages for chatListId ${chatListId}:`,
          error
        );
      }
    } else {
      console.log(`No new messages detected for chatListId ${chatListId}.`);
    }

    return null; // Indicate the function has completed
  });

// exports.notifiyMessage = functions.database.ref("Messages/{chatListId}").onUpdate(async (change, context) => {
//     const chatListId = context.params.chatListId;
//     const beforeData = change.before.val(); // Data before the update
//     const afterData = change.after.val(); // Data after the update
//     const beforeMessages = beforeData.messages  || [];
//     const afterMessages = afterData.messages || [];

//     const newMessages = afterMessages.filter((id) => !beforeMessages.includes(id));

//     if(newMessages.length > 0){
//         for(const message of newMessages){
//             const messageSender = message.sender;
//             const messageRecipient = message.recipient;
//             const messageSeen = message.seen;
//             const messageBody = message.message;
//             if(!messageSeen){
//                 if(usersInformation.contains(messageRecipient)){
//                     const token = usersInformation.messageRecipient.myToken;
//                     var tokens = [];
//                     if(token){
//                         tokens.push(token);
//                         const notification = {
//                             title: "New message",
//                             body: `You have a message: ${messageBody}`,
//                             sender: messageSender,
//                             timestamp: Date.now(),
//                             data: {
//                                 chatListId: chatListId,
//                                 message: messageBody,
//                                 type: "message"
//                             },
//                           };
//                         sendNotifitication(tokens, notification);
//                     }
//                 }
//             }
//         }
//     }
// });
async function sendNotifitication(tokens, notification){
      const payload = {
          notification: {
            title: notification.title,
            body: notification.body,
            clickAction: "FLUTTER_NOTIFICATION_CLICK",
          },
          data: notification.data,
        };
      // Send notifications to all tokens
      try {
          const response = await admin.messaging().sendToDevice(tokens, payload);
          console.log("Notifications sent successfully:", response);
        } catch (error) {
          console.error("Error sending notifications:", error);
        }
}
exports.notifiyMeme = functions.database.ref("Memes/{memeId}").onUpdate(async (change, context) => {
    const memeID = context.params.memeId;
  
    const beforeData = change.before.val(); // Data before the update
    const afterData = change.after.val(); // Data after the update
    const userId = afterData.userId || null;
    const beforeLikes = beforeData.likes || [];
    const afterLikes = afterData.likes || [];
    const beforeComments = beforeData.comments || [];
    const afterComments = afterData.comments || [];
    const beforeShare = beforeData.totalShare || 0;
    const afterShare = afterData.totalShare || 0;
  
    // Calculate differences
    const newLikes = afterLikes.filter((id) => !beforeLikes.includes(id));
    const totalNewLikes = newLikes.length;
  
    const newComments = afterComments.filter((id) => !beforeComments.includes(id));
    const totalNewComments = newComments.length;
  
    const newShares = afterShare > beforeShare ? afterShare - beforeShare : 0;
  
    try {
      if (!userId) {
        console.warn(`No userId found for meme ${memeID}. Skipping notification.`);
        return null;
      }
  
      // Check if the user's information exists in the `usersInformation` object
      if (usersInformation[userId]) {
        const token = usersInformation[userId].myToken;
  
        if (!token) {
          console.warn(`No token found for user ${userId}. Skipping notification.`);
          return null;
        }
  
        const tokens = [token]; // Collect user's token
  
        // Handle new shares
        if (newShares > 0) {
          console.log(`Meme ${memeID} received ${newShares} new shares.`);
          await notifyMemeUser(userId, tokens, memeID, "shares", newShares);
        }
  
        // Handle new likes
        if (totalNewLikes > 0) {
          console.log(
            `Meme ${memeID} received ${totalNewLikes} new likes from: ${newLikes.join(", ")}`
          );
          await notifyMemeUser(userId, tokens, memeID, "likes", totalNewLikes, newLikes);
        }
  
        // Handle new comments
        if (totalNewComments > 0) {
          console.log(
            `Meme ${memeID} received ${totalNewComments} new comments from: ${newComments.join(", ")}`
          );
          await notifyMemeUser(userId, tokens, memeID, "comments", totalNewComments, newComments);
        }
      } else {
        console.warn(
          `User information for userId ${userId} not found in usersInformation. Skipping notification.`
        );
      }
    } catch (error) {
      console.error(`Error processing meme update for ${memeID}:`, error);
    }
  
    return null; // Indicate the function has completed
  });
  
  // Helper function for notifications
  async function notifyMemeUser(userId, tokens, memeID, type, count, ids = []) {
    try {
      console.log(`Notifying user about ${count} new ${type} for meme ${memeID}.`);
      if (ids.length > 0) {
        console.log(`New user IDs involved: ${ids.join(", ")}`);
      }
  
      // Construct the notification object
      const notification = {
        memeID: memeID,
        type: type,
        count: count,
        userIds: ids,
        timestamp: Date.now(),
      };
  
      // Determine the notification title and message
      let title = "";
      let message = "";
  
      if (type === "likes") {
        title = "New Likes";
        message = count > 5
          ? `Your post is getting noticed! You have ${count} likes.`
          : "Your meme got liked!";
      } else if (type === "comments") {
        title = "New Comments";
        message = count > 5
          ? `People are commenting on your post! You have ${count} new comments.`
          : "You got new comments on your post.";
      } else if (type === "shares") {
        title = "New Shares";
        message = count > 5
          ? "People are sharing your meme! Check it out again."
          : "Someone shared your meme post. View it.";
      }
  
      // Save the notification to the database
      await admin.database().ref(`Notifications/${userId}`).push(notification);
  
      // Create the notification payload
      const payload = {
        notification: {
          title: title,
          body: message,
          clickAction: "FLUTTER_NOTIFICATION_CLICK",
        },
        data: {
          type: type,
          memeID: memeID,
        },
      };
  
      // Send notifications to all tokens
      if (tokens.length > 0) {
        const response = await admin.messaging().sendToDevice(tokens, payload);
        console.log("Notifications sent successfully:", response);
      } else {
        console.log("No tokens available to send notifications.");
      }
    } catch (error) {
      console.error(`Error notifying user ${userId} about meme ${memeID}:`, error);
    }
  }
  
