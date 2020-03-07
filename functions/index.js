const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

// Function called whenever there is a new message added to a Two Person Chat Room
exports.newTwoPersonMessage = functions.firestore
    .document('twoPersonRooms/{rooms}/messages/{message}')
    .onCreate(async (docSnapshot) => {
        const data = docSnapshot.data();
        const message = data['message'];
        const receiverUid = data['receiverUid'];
        const sender = data['sender'] + " sent you a message";

        let tokens = [];
        const userDoc = await admin.firestore().doc('users/' + receiverUid).get();
        userDoc.get('tokenIds').forEach((element) => {
            tokens.push(element);
        });

        console.log("Tokens: " + tokens);

        const payload = {
            notification: {
                title: sender,
                body: message,
                clickAction: "NotificationMessageActivity"
            },
            data: {
                DOCUMENT_ID: docSnapshot.id
            }
        };

        const result = await admin.messaging().sendToDevice(tokens, payload);
        return console.log("Two Person Room notification sent!", result);
    });

// Function called whenever a User send's another User a friend request
exports.newFriendRequest = functions.firestore
    .document('friendList/{uid}/friendRequests/{friendRequest}')
    .onCreate(async (docSnapshot) => {
        const data = docSnapshot.data();
        const name = data['name'] + " sent you a friend request!";
        const receiverUid = data['receiverUid'];

        let tokens = [];
        const userDoc = await admin.firestore().doc('users/' + receiverUid).get();

        userDoc.get('tokenIds').forEach((element) => {
            tokens.push(element);
        });

        console.log("Tokens: " + tokens);

        const payload = {
            notification: {
                title: name
            }
        };

        const result = await admin.messaging().sendToDevice(tokens, payload)
        return console.log("Friend request sent!", result);
    });