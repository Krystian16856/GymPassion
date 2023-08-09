const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Importowanie klucza serwisowego
const serviceAccount = require("./serviceAccountKey.json");
const moment = require('moment-timezone');

// Inicjalizacja Firebase Admin SDK z użyciem klucza serwisowego
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://gympassion-179b2.firebaseio.com",
  storageBucket: "gympassion-179b2.appspot.com"
});

const NotificationTexts = {
    fiveMinutesBefore: [
        "🕔 5 godzin do treningu! Więcej czasu do treningu niż do końca dnia pracy. 💪",
        "🕔 Jeszcze 5 godzin. Na pewno masz wszystko przygotowane? 🎒",
        "🕔 5 godzin do treningu! Pamiętaj o odpowiednim nawodnieniu. 💧",
        "🕔 Masz jeszcze 5 godzin na przemyślenie swojego planu treningowego. 🗒️",
        "🕔 Czy wiesz, że masz jeszcze 5 godzin do treningu? Wykorzystaj ten czas! ⏳",
        "🕔 5 godzin do treningu. Czas na zdrowy posiłek! 🍎",
        "🕔 Jeszcze 5 godzin. Zastanawiałeś się kiedyś, co to jest burpee? 🤔",
        "🕔 5 godzin do treningu. Czy masz już swoją playlistę? 🎵",
        "🕔 Masz jeszcze 5 godzin. Czy pamiętasz o nawodnieniu? 💧",
        "🕔 Jeszcze 5 godzin. Czy jesteś gotowy? 🏋️",
        "🕔 5 godzin do treningu! Planujesz dzisiaj trening? Pamiętaj, że Twój kanapowy tryb życia zawsze może na Ciebie poczekać! 🛋️ 🏃",
        "🕔 5 godzin do treningu! Wszyscy potrzebujemy trochę motywacji. Zacznij dzisiaj i krok po kroku osiągniesz swój cel! 🏆 💪",
        "🕔 Jeszcze 5 godzin. Jak wygląda Twój plan na dzisiaj? Pamiętaj, że ćwiczenia mogą przynieść Ci więcej energii niż kawa! ⚡️ ☕️",
        "🕔 Jeszcze 5 godzin. Czy pamiętasz o dzisiejszym treningu? Mówią, że lepiej jest ćwiczyć dziś, niż przepraszać swoje ciało później! 🏋️‍♀️ ⏰",
        "🕔 Jeszcze 5 godzin. To tylko przypomnienie, że trening czeka! Czy jesteś gotów na wyzwanie? 💥 🥊"
    ],
    fourMinutesBefore: [
        "🕓 4 godziny do treningu! Jesteś bliżej treningu niż do lunchu. 🏋️",
        "🕓 4 godziny i już jesteś bliżej celu! Przygotuj swoje białko! 🥤",
        "🕓 4 godziny do treningu! Pamiętaj o odpowiednim odżywianiu przed treningiem. 🍎",
        "🕓 Jeszcze 4 godziny. Gotowy na trening? 💪",
        "🕓 4 godziny do treningu. Pamiętaj o nawodnieniu. 💧",
        "🕓 Jeszcze 4 godziny. Czy wiesz, co to jest kettlebell? 🏋️",
        "🕓 4 godziny do treningu. Przygotuj swoje białko! 🥤",
        "🕓 4 godziny do treningu. Czas na zdrowy przekąskę! 🍎",
        "🕓 4 godziny do treningu. Pamiętaj o rozciąganiu. 🤸",
        "🕓 Jeszcze 4 godziny. Czy jesteś gotowy? 🏋️",
        "🕓 Jeszcze 4 godziny. Czy już pakujesz torbę na trening? Nie zapomnij o wodzie! 🎒 💧",
        "🕓 Jeszcze 4 godziny. Zbliża się czas treningu. Czy zaczynasz się już podgrzewać? 🔥 🕓",
        "🕓 Jeszcze 4 godziny. Czy pamiętasz o dzisiejszym treningu? Nie zaszkodzi przypomnieć! 💭 🏋️‍♀️",
        "🕓 4 godziny do treningu. Czy Twój strój treningowy jest już gotowy? 4 godziny do treningu! 👟 ⏰",
        "🕓 4 godziny do treningu. Czas na trening za 4 godziny! Czy jesteś już na to gotowy? 🏁 ⌛"
    ],
    threeMinutesBefore: [
        "🕒 3 godziny do treningu! Jeszcze tylko jedno spotkanie. 🚴",
        "🕒 3 godziny do treningu! Czas zacząć się koncentrować. 🧘",
        "🕒 3 godziny do treningu. Masz już swoją playlistę gotową? 🎵",
        "🕒 Jeszcze 3 godziny. Czy Twój strój jest już gotowy? 👕",
        "🕒 3 godziny do treningu. Pamiętaj o odpowiednim odżywianiu. 🍎",
        "🕒 3 godziny do treningu. Czy masz już swoją butelkę na wodę? 💧",
        "🕒 3 godziny do treningu. Pamiętaj o rozgrzewce. 🏃‍♂️",
        "🕒 Jeszcze 3 godziny. Czy jesteś gotowy? 💪",
        "🕒 3 godziny do treningu. Pamiętaj o oddechu. 🧘",
        "🕒 3 godziny do treningu. Czy Twój strój jest już gotowy? 👕",
        "🕒 3 godziny do treningu. Czy już wybrałeś playlistę na dzisiaj? Pamiętaj, że dobre bity mogą podnieść Ci na duchu podczas treningu! 🎵 💪",
        "🕒 3 godziny do treningu.Czas na trening zbliża się nieubłaganie! Czy jesteś gotowy, aby podjąć wyzwanie? 🚀 ⏳",
        "🕒 3 godziny do treningu. Czy jesteś gotowy, aby podbić ten dzień? 👊 ⏰",
        "🕒 Jeszcze 3 godziny. Zbliża się czas treningu. Pamiętaj, że pociąg do celu nie jedzie bez Ciebie! 🚂 🎯",
        "🕒 Jeszcze 3 godziny. Czy masz wszystko, czego potrzebujesz na trening? 🎒 ⏳"
    ],
    twoMinutesBefore: [
        "🕑 2 godziny do treningu! Lepiej przygotować strój treningowy. 🤸",
        "🕑 Jeszcze tylko dwie godziny! Czy Twój shaker jest gotowy? 🥤",
        "🕑 2 godziny do treningu! Czas zacząć się rozgrzewać. 🏃‍♂️",
        "🕑 Jeszcze 2 godziny. Pamiętaj o odpowiednim odżywianiu. 🍌",
        "🕑 2 godziny do treningu. Pamiętaj o odpowiednim nawodnieniu. 💧",
        "🕑 2 godziny do treningu. Czas na rozgrzewkę! 🏃‍♂️",
        "🕑 Jeszcze 2 godziny. Czy jesteś gotowy? 💪",
        "🕑 2 godziny do treningu. Czy Twój strój jest już gotowy? 👕",
        "🕑 Jeszcze 2 godziny. Czy jesteś gotowy na trening? 🏋️",
        "🕑 2 godziny do treningu. Czas na zdrowy posiłek! 🍎",
        "🕑 2 godziny do treningu. Czy Twój strój treningowy jest już na Tobie? 👟 ⏰",
        "🕑 2 godziny do treningu. Czy już zaczynasz się rozgrzewać? 💪 ⏳",
        "🕑 Jeszcze 2 godziny! Pamiętaj, że małe postępy są również postępami! 🐢 🏁",
        "🕑 Jeszcze 2 godziny. Czy już się rozgrzewasz? 🔥 ⌛",
        "🕑 Jeszcze 2 godziny. Zbliża się czas treningu. Czy jesteś już gotowy, aby dać z siebie wszystko? 🥊 🕑"
    ],
    oneMinutesBefore: [
        "🕐 1 godzina do treningu! Czas na rozgrzewkę! 😄",
        "🕐 Jeszcze godzina! Masz już swój ręcznik? 🧖‍♂️",
        "🕐 1 godzina do treningu. Czy jesteś gotowy? 💪",
        "🕐 Jeszcze godzina. Zadbaj o ostatnie przygotowania. 🎒",
        "🕐 1 godzina do treningu. Czy Twój shaker jest gotowy? 🥤",
        "🕐 1 godzina do treningu. Pamiętaj o odpowiednim nawodnieniu. 💧",
        "🕐 Jeszcze godzina. Czy jesteś gotowy? 🏋️",
        "🕐 1 godzina do treningu. Czas na rozgrzewkę! 🏃‍♂️",
        "🕐 1 godzina do treningu. Pamiętaj o rozciąganiu. 🤸",
        "🕐 1 godzina do treningu. Czy masz już swoją playlistę? 🎵",
        "🕐 Jeszcze tylko godzina. Czas na ostatnie przygotowania! ⏰ 💪",
        "🕐 Zbliża się czas treningu. Czy jesteś gotowy na to, aby dać z siebie wszystko? 💥 🕐",
        "🕐 Jeszcze godzina! Czy masz wszystko, czego potrzebujesz? 🎒 🕐",
        "🕐 Pora zacząć się rozgrzewać. Za godzinę startujemy! 🔥 🕐",
        "🕐 Jesteś gotowy na dawkę pozytywnej energii? Trening zaczyna się za godzinę! ⚡️ 🕐"
    ],
    getRandomNotification: function(hoursBefore) {
        switch(hoursBefore) {
            case 5: return this.fiveMinutesBefore[Math.floor(Math.random() * this.fiveMinutesBefore.length)];
            case 4: return this.fourMinutesBefore[Math.floor(Math.random() * this.fourMinutesBefore.length)];
            case 3: return this.threeMinutesBefore[Math.floor(Math.random() * this.threeMinutesBefore.length)];
            case 2: return this.twoMinutesBefore[Math.floor(Math.random() * this.twoMinutesBefore.length)];
            case 1: return this.oneMinutesBefore[Math.floor(Math.random() * this.oneMinutesBefore.length)];
            default: return "Czas na trening! 💪";
        }
    }    
};

exports.sendNotification = functions.region('europe-central2').firestore
    .document('chats/{chatId}/messages/{messageId}')
    .onCreate(async (snapshot, context) => {
        console.log("Function triggered by new message in chat:", context.params.chatId);
        
        const messageData = snapshot.data();
        const senderId = messageData.senderId;

        console.log("Sender ID:", senderId);

        // Pobieranie nickname nadawcy
        const db = admin.firestore();
        const userRef = db.collection('users').doc(senderId);
        const userDoc = await userRef.get();
        const senderNickname = userDoc.data().nickname;
        const imageToken = userDoc.data().imageToken; // Pobieranie tokena obrazu

        console.log("Sender Nickname:", senderNickname);

        const projectId = "gympassion-179b2";
        const imagePath = `profileImages/${senderId}`;
        const encodedImagePath = imagePath.replace("/", "%2F");
        const profileImageUrl = `https://firebasestorage.googleapis.com/v0/b/${projectId}.appspot.com/o/${encodedImagePath}?alt=media&token=${imageToken}`;

        const chatId = context.params.chatId;
        const participants = chatId.split("_");
        const receiverId = participants[0] === senderId ? participants[1] : participants[0];

        // Pobierz token odbiorcy
        const receiverData = await db.collection('users').doc(receiverId).get();
        const receiverToken = receiverData.data().fcmToken;

        console.log('Receiver ID:', receiverId);
        console.log('Receiver Token:', receiverToken);

        const payload = {
            data: {
                type: "message", 
                title: 'Nowa wiadomość!',
                body: `${senderNickname} wysłał Ci wiadomość.`,
                imageUrl: profileImageUrl,
                friendId: senderId,
                priority: 'high'
            }
        };
        
        console.log('Payload:', payload);

        console.log('Attempting to send the message...');
        try {
            const response = await admin.messaging().sendToDevice(receiverToken, payload);
            console.log('Successfully sent message:', response);
            console.log('Message sent successfully with response:', JSON.stringify(response));
        } catch (error) {
            console.error('Error sending message:', error);
            console.error('Detailed error:', JSON.stringify(error));
        }
    });

    exports.sendInvitationNotification = functions.region('europe-central2').firestore
    .document('invitations/{invitationId}')
    .onCreate(async (snapshot, context) => {
        console.log("Function triggered by new invitation:", context.params.invitationId);

        

        const invitationData = snapshot.data();
        const senderId = invitationData.senderId;

        console.log("Sender ID:", senderId);

        // Pobieranie nickname nadawcy
        const db = admin.firestore();
        const userRef = db.collection('users').doc(senderId);
        const userDoc = await userRef.get();
        const senderNickname = userDoc.data().nickname;
        const imageToken = userDoc.data().imageToken; 

        console.log("Sender Nickname:", senderNickname);

        const projectId = "gympassion-179b2";
        const imagePath = `profileImages/${senderId}`;
        const encodedImagePath = imagePath.replace("/", "%2F");
        const profileImageUrl = `https://firebasestorage.googleapis.com/v0/b/${projectId}.appspot.com/o/${encodedImagePath}?alt=media&token=${imageToken}`;

        const receiverId = invitationData.receiverId;

        // Pobierz token odbiorcy
        const receiverData = await db.collection('users').doc(receiverId).get();
        const receiverToken = receiverData.data().fcmToken;

        console.log('Receiver ID:', receiverId);
        console.log('Receiver Token:', receiverToken);

        const payload = {
            data: {
                type: "invitation", 
                title: 'Zaproszenie do znajomych!',
                body: `${senderNickname} wysłał Ci zaproszenie do znajomych.`,
                imageUrl: profileImageUrl,
                friendId: senderId,
                priority: 'high'
            }
        };
        
        console.log('Payload:', payload);

        console.log('Attempting to send the invitation notification...');
        try {
            const response = await admin.messaging().sendToDevice(receiverToken, payload);
            console.log('Successfully sent invitation notification:', response);
            console.log('Invitation notification sent successfully with response:', JSON.stringify(response));
        } catch (error) {
            console.error('Error sending invitation notification:', error);
            console.error('Detailed error:', JSON.stringify(error));
        }
    });

    exports.sendWorkoutNotifications = functions.region('europe-central2').pubsub.schedule('every 60 minutes').timeZone('Europe/Warsaw').onRun(async (context) => {
        const currentTime = moment.tz('Europe/Warsaw');
        const db = admin.firestore();
    
        // Pobierz wszystkich użytkowników i ich planowane treningi
        const usersRef = db.collection('users');
        const usersSnapshot = await usersRef.get();
    
        for (const userDoc of usersSnapshot.docs) {
            const userId = userDoc.id;
            const selectedHoursRef = db.collection('users').doc(userId).collection('selectedHours');
            const selectedHoursDoc = await selectedHoursRef.doc('schedule').get();
    
            if (selectedHoursDoc.exists) {
                const selectedHours = selectedHoursDoc.data();
    
                for (const [day, hour] of Object.entries(selectedHours)) {
                    if (hour !== "Godzina") {
                        console.log(`User ${userId} - Day: ${day}, Hour: ${hour}`);
    
                        const currentDay = currentTime.format('dddd').toLowerCase();
                        console.log(`Current day is: ${currentDay}`);
    
                        const [workoutHour, workoutMinute] = hour.split(':');
                        console.log(`Parsed values for ${day} - Hour: ${workoutHour}, Minute: ${workoutMinute}`);
    
                        if (day === currentDay) {
                            const workoutDateTime = moment.tz({
                                year: currentTime.year(),
                                month: currentTime.month(),
                                day: currentTime.date(),
                                hour: parseInt(workoutHour),
                                minute: parseInt(workoutMinute)
                            }, 'Europe/Warsaw');
    
                            console.log("Workout Date:", workoutDateTime.format());
                            console.log("Current Time:", currentTime.format());
    
                            const minutesDifference = workoutDateTime.diff(currentTime, 'minutes');
                            const hoursDifference = Math.floor(minutesDifference / 60);
    
                            console.log("Minutes difference:", minutesDifference);
                            console.log("Hours difference:", hoursDifference);
                            console.log("Workout Date TimeZone:", workoutDateTime.format('z'));
                            console.log("Current Time TimeZone:", currentTime.format('z'));
    
                            if ([5, 4, 3, 2, 1].includes(hoursDifference)) {
                                const notificationText = NotificationTexts.getRandomNotification(hoursDifference);
                                const userToken = userDoc.data().fcmToken;
    
                                const payload = {
                                    data: {
                                        type: "workoutReminder",
                                        title: 'Przypomnienie o treningu',
                                        body: notificationText,
                                        priority: 'high'
                                    }
                                };
                                
                                try {
                                    const response = await admin.messaging().sendToDevice(userToken, payload);
                                    console.log('Successfully sent workout reminder:', response);
                                } catch (error) {
                                    console.error('Error sending workout reminder:', error);
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    });
