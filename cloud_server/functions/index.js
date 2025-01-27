const functions = require("firebase-functions/v2");
const admin = require("firebase-admin");
const { onValueUpdated } = require("firebase-functions/v2/database");

admin.initializeApp();

let usersInformation = {};
let initializationPromise = null;

// Populate initial data
exports.populateInitialData = functions.https.onRequest(async (req, res) => {
  try {
    const usersSnapshot = await admin.database().ref("Users").once("value");
    const users = usersSnapshot.val();

    for (const userID in users) {
      if (Object.prototype.hasOwnProperty.call(users, userID)) {
        console.log("User ID:", userID);
        const userData = users[userID];
        const followers = userData.followers ? Object.keys(userData.followers) : [];
        const following = userData.following ? Object.keys(userData.following) : [];
        const followersToken = [];
        const followingTokens = [];

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
          const followingToken = followingData?.settings?.notifications?.deviceToken || null;

          if (followingToken) {
            followingTokens.push(followingToken);
          }
        }

        usersInformation[userID] = {
          myToken: userData.settings?.notifications?.deviceToken || null,
          friendsToken: followersToken.concat(followingTokens),
        };
      }
    }

    console.log("Initial data populated:", usersInformation);

    res.status(200).send("Initial data populated successfully.");
  } catch (error) {
    console.error("Error populating initial data:", error);
    res.status(500).send("Error populating initial data.");
  }
});

// Initialize usersInformation
function initializeUsersInformation() {
  if (!initializationPromise) {
    initializationPromise = new Promise(async (resolve, reject) => {
      try {
        const usersSnapshot = await admin.database().ref("Users").once("value");
        const users = usersSnapshot.val();
        for (const userID in users) {
          if (Object.prototype.hasOwnProperty.call(users, userID)) {
            console.log("User ID:", userID);
            const userData = users[userID];
            const followers = userData.followers ? Object.keys(userData.followers) : [];
            const following = userData.following ? Object.keys(userData.following) : [];
            const followersToken = [];
            const followingTokens = [];
    
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
              const followingToken = followingData?.settings?.notifications?.deviceToken || null;
    
              if (followingToken) {
                followingTokens.push(followingToken);
              }
            }
    
            usersInformation[userID] = {
              myToken: userData.settings?.notifications?.deviceToken || null,
              friendsToken: followersToken.concat(followingTokens),
            };
          }
        }
        resolve();
      } catch (error) {
        reject(error);
      }
    });
  }
  return initializationPromise;
}

// Update token and followers
exports.updateTokenAndFollowers = onValueUpdated({
  ref: "Users/{userID}"
}, async (event) => {
  await initializeUsersInformation();
  const userID = event.params.userID;
  const beforeData = event.data.before.val();
  const afterData = event.data.after.val();

  const beforeFollowing = beforeData.following ? Object.keys(beforeData.following) : [];
  const afterFollowing = afterData.following ? Object.keys(afterData.following) : [];
  const userToken = afterData.settings?.notifications?.deviceToken || null;

  const newFollowing = afterFollowing.filter((id) => !beforeFollowing.includes(id));

  const getTokens = async (userIDs) => {
    const promises = userIDs.map((id) =>
      admin.database().ref(`Users/${id}/settings/notifications/deviceToken`).once("value")
    );
    const snapshots = await Promise.all(promises);
    return snapshots.map((snap) => snap.val()).filter((token) => token);
  };
  

  try {
    const followingTokens = await getTokens(afterFollowing);

    usersInformation[userID] = {
      myToken: userToken,
      friendsToken: followingTokens,
    };
    if (newFollowing.length > 0) {
      console.log("New following IDs:", newFollowing);
    } else {
      console.log("No new following IDs were added.");
    }

    console.log(`Updated tokens for user ${userID}:`, usersInformation[userID]);
  } catch (error) {
    console.error(`Error updating tokens for user ${userID}:`, error);
  }

  return null;
});

// Notify message
exports.notifyMessage = onValueUpdated({
  ref: "Messages/{chatListId}"
}, async (event) => {
  await initializeUsersInformation();
  const chatListId = event.params.chatListId;
  const beforeMessages = event.data.before.child("Messages").val() || {};
  const afterMessages = event.data.after.child("Messages").val() || {};

  const beforeMessageKeys = Object.keys(beforeMessages);
  const afterMessageKeys = Object.keys(afterMessages);

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
        const messageBody = message.text;

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

  return null;
});



// Notify meme
exports.notifyMeme = onValueUpdated({
  ref: "Memes/{memeId}"
}, async (event) => {
  await initializeUsersInformation();
  const memeID = event.params.memeId;

  const beforeData = event.data.before.val();
  const afterData = event.data.after.val();
  const userId = afterData.userID || null;
  const beforeLikes = beforeData.likes ? Object.keys(beforeData.likes) : [];
  const afterLikes = afterData.likes ? Object.keys(afterData.likes) : [];
  const beforeComments = beforeData.comments ? Object.keys(beforeData.comments) : [];
  const afterComments = afterData.comments ? Object.keys(afterData.comments) : [];
  const beforeShare = beforeData.totalShare || 0;
  const afterShare = afterData.totalShare || 0;

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

    if (usersInformation[userId]) {
      const token = usersInformation[userId].myToken;

      if (!token) {
        console.warn(`No token found for user ${userId}. Skipping notification.`);
        return null;
      }

      const tokens = [token];

      if (newShares > 0) {
        console.log(`Meme ${memeID} received ${newShares} new shares.`);
        await notifyMemeUser(userId, tokens, memeID, "shares", newShares);
      }

      if (totalNewLikes > 0) {
        console.log(
          `Meme ${memeID} received ${totalNewLikes} new likes from: ${newLikes.join(", ")}`
        );
        await notifyMemeUser(userId, tokens, memeID, "likes", totalNewLikes, newLikes);
      }

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

  return null;
});

async function notifyMemeUser(userId, tokens, memeID, type, count, ids = []) {
  try {
    console.log(`Notifying user about ${count} new ${type} for meme ${memeID}.`);
    if (ids.length > 0) {
      console.log(`New user IDs involved: ${ids.join(", ")}`);
    }

    const notification = {
      memeID: memeID,
      type: type,
      count: count,
      userIds: ids,
      timestamp: Date.now(),
    };

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

    await admin.database().ref(`Notifications/${userId}`).push(notification);

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
async function sendNotification(tokens, notification) {
  const payload = {
    notification: {
      title: notification.title,
      body: notification.body,
      clickAction: "FLUTTER_NOTIFICATION_CLICK",
    },
    data: notification.data,
  };
  try {
    const response = await admin.messaging().sendToDevice(tokens, payload);
    console.log("Notifications sent successfully:", response);
  } catch (error) {
    console.error("Error sending notifications:", error);
  }
}