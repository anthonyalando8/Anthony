const functions = require("firebase-functions/v2");
const admin = require("firebase-admin");
const { onValueUpdated } = require("firebase-functions/v2/database");

admin.initializeApp();

let usersInformation = {};

async function getUserInformation(ID) {
  try {
    const userSnapshot = await admin.database().ref(`Users/${ID}`).once("value");
    const userData = userSnapshot.val();
    if (!userData) {
      console.error(`User data for ID ${ID} is null or undefined.`);
      return { myToken: null, friendsToken: [] };
    }
    
    const userID = userSnapshot.key;

    console.log("User ID:", userID);
    const userName = userData.username || null;

    // const followers = userData.followers ? Object.keys(userData.followers) : [];
    // const following = userData.following ? Object.keys(userData.following) : [];

    // TODO: Fetch tokens concurrently using the helper function
    // const followersToken = await getTokens(followers);
    // const followingTokens = await getTokens(following);
    const followersToken = [];
    const followingTokens = [];

    // Combine and deduplicate tokens
    const friendsToken = [...new Set([...followersToken, ...followingTokens])];
    const myToken = userData.settings?.notifications?.deviceToken || null;

    // Assuming `usersInformation` is defined elsewhere
    if (typeof usersInformation === "object") {
      usersInformation[userID] = {
        myToken: myToken,
        username: userName,
        friendsToken: friendsToken,
      };
    }

    return {
      myToken:myToken,
      username:userName,
      friendsToken:friendsToken,
    };
  } catch (error) {
    console.error("Error fetching user information:", error);
    throw error;
  }
}
// TODO: Fetch followers' tokens and implement notifications for them.
async function getTokens(userIDs) {
  const promises = userIDs.map((id) =>
    admin.database().ref(`Users/${id}/settings/notifications/deviceToken`).once("value")
  );
  const results = await Promise.allSettled(promises);
  const successfulTokens = results
    .filter((result) => result.status === "fulfilled")
    .map((result) => result.value.val());
  return successfulTokens;
}


// Update token and followers
exports.updateTokenAndFollowers = onValueUpdated({
  ref: "Users/{userID}"
}, async (event) => {

  if (!event.data || !event.data.before.exists() || !event.data.after.exists()) {
    console.warn("No data exists for the event trigger.");
    return null;
  }
  
  const userID = event.params.userID;
  const beforeData = event.data.before.val();
  const afterData = event.data.after.val();
  const userName = afterData.username || null;

  const beforeFollowing = beforeData.following ? Object.keys(beforeData.following) : [];
  const afterFollowing = afterData.following ? Object.keys(afterData.following) : [];
  const beforeFollowers = beforeData.followers ? Object.keys(beforeData.followers) : [];
  const afterFollowers = afterData.followers ? Object.keys(afterData.followers) : [];
  const userToken = afterData.settings?.notifications?.deviceToken || null;

  const newFollowing = afterFollowing.filter((id) => !beforeFollowing.includes(id));
  // const newFollowers = afterFollowers.filter((id) => !beforeFollowers.includes(id));

  try {
    let newFollowingTokens = await getTokens(newFollowing);
    // let newFollowersTokens = await getTokens(afterFollowers);
    const newFollowersTokens = [];

    
    if (newFollowing.length > 0) {
      console.log("New following IDs:", newFollowing);
      const notification = {
        title: "New Follower",
        body: `${userName} is now following you.`,
      
        data: {
          type: "follow",
          userId: userID,
          timestamp: Date.now().toString(),
        },
      };
      try {
        await sendNotification(newFollowingTokens, notification);
        console.log(`Notification sent to user ${userID} for new followers.`);
      } catch (error) {
        console.error("Notification failed:", error);
      }
      
    }
    const friendsToken = [...new Set([...newFollowersTokens, ...newFollowingTokens])];

    usersInformation[userID] = {
      myToken: userToken,
      friendsToken: friendsToken,
    };
    console.log(`Updated tokens for user ${userID}:`, usersInformation[userID]);
  } catch (error) {
    console.error(`Error updating tokens for user ${userID}:`, error);
  }

  return null;
});

// Notify message
exports.notifyMessage = onValueUpdated({
  ref: "Chats/{chatListId}"
}, async (event) => {
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
          let token = null;
          // const token = usersInformation[userID]?.myToken || (await getUserInformation(userID)).myToken;

          if (usersInformation[messageRecipient]) {
            token = usersInformation[messageRecipient].myToken;
          } else {
            console.warn(
              `User information not found for recipient ${messageRecipient} in userInformation`
            );
            token = (await getUserInformation(messageRecipient)).myToken; 
          }
          if (!token) {
            console.warn(`No token found for user ${messageRecipient}. Skipping notification.`);
            return null;
          }
      
          const tokens = [token];
          const senderInfo = await getUserInformation(messageSender);
          const senderName = senderInfo.username;
          const notification = {
            title: senderName,
            body: `Message: ${messageBody}`,
            data: {
              chatListId: chatListId,
              messageID: messageID,
              type: "message",
              sender: messageSender,
              recipient: messageRecipient,
              timestamp: Date.now().toString(),
            },
          };

          try {
            await sendNotification(tokens, notification);
            console.log(
              `Notification sent to user ${messageRecipient} for message ${messageID} from ${messageSender}.`
            );
          } catch (error) {
            console.error("Notification failed:", error);
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
    let token = null;
    if (usersInformation[userId]) {
      token = usersInformation[userId].myToken;
    } else {
      console.warn(
        `User information not found for memer ${userId} in userInformation`
      );
      token = (await getUserInformation(userId)).myToken;
    }

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
      timestamp: Date.now().toString(),
    };

    let title = "";
    let message = "";

    const senderInfo = await getUserInformation(ids[0]);
    const senderName = senderInfo.username;

    if (type === "likes") {
      title = "New Like";
      message = count > 5
        ? `Your post is getting noticed! You have ${count} likes.`
        : `${senderName} liked your meme. Click to open.`;
    } else if (type === "comments") {
      title = "New Comments";
      message = count > 5
        ? `People are commenting on your post! You have ${count} new comments.`
        : `${senderName} commented on your meme.`;
    } else if (type === "shares") {
      title = "New Shares";
      message = count > 5
        ? "People are sharing your meme! Check it out again."
        : `${senderName} shared your meme. View it.`;
    }

    await admin.database().ref(`Notifications/${userId}`).push(notification);

    const notify = {
      title: title,
      body: message,
      data: {
        type: type,
        memeID: memeID,
      },
    }

    await sendNotification(tokens, notify);

  } catch (error) {
    console.error(`Error notifying user ${userId} about meme ${memeID}:`, error);
  }
}
async function sendNotification(tokens, notification) {
  const message = {
    notification: {
      title: notification.title,
      body: notification.body,
    },
    data: notification.data, // Custom data (optional)
  };

  try {
    // Check if there are tokens to send notifications to
    if (tokens.length > 0) {
      // Using sendEach() to send individual messages
      try {
        const responses = await admin.messaging().sendEach(tokens.map(token => ({
          ...message,
          token,  // Assigning each token to the notification payload
        })));

        // Logging the responses to check success/failure
        console.log("Notifications sent successfully:", responses.responses);

        responses.responses.forEach((response, idx) => {
          if (!response.success) {
            console.error(`Failed to send notification to token: ${tokens[idx]}`);
          }
        });
      } catch (error) {
        console.error("Error sending notifications:", error);
      }
    } else {
      console.log("No tokens available to send notifications.");
    }
  } catch (error) {
    console.error("Error sending notifications:", error);
  }
}
