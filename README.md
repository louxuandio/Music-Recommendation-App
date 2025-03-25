# 🎵 Mood Melody – Personalized Music Recommendation Based on Mood & Weather

Mood Melody is a personalized music recommendation app that suggests daily playlists based on a user's current mood and real-time weather conditions. By delivering music tailored to emotional states, the app aims to enhance users’ mental well-being and foster emotional self-awareness.

---

## 🌟 App Concept & Purpose

Music has the power to influence and reflect how we feel. Mood Melody helps users connect with their emotions by recommending music that resonates with both their **inner state** and the **outer world**.

### 🔁 How it works:
1. **Detect mood** through text input, voice input, or facial expressions.
2. **Fetch local weather** data via GPS and external weather APIs.
3. **Recommend music** using a custom algorithm that matches mood + weather context.
4. **Promote emotional awareness** by helping users reflect on their mood and music preferences.
5. **Encourage intentional listening** to support mood improvement and mindfulness.

---

## 🧠 Tentatively Selected APIs, Databases, and Sensors

### 🎭 Mood Capturing
- **CameraX API**: Capture user facial expressions.
- **ML Kit API**: Perform on-device face analysis and emotion detection.
- **Microphone**: Capture voice tone patterns as a secondary mood input.
- **Text Input**: Optional mood journaling analyzed via AI.
- **Activity Sensor (Accelerometer)**: Infer user activity level as a supporting mood signal.

### 🌤️ Weather Integration
- **GPS**: Detect user location.
- **OpenWeatherMap API**: Retrieve real-time weather data (temperature, condition, etc.).

### 📊 Mood Analyzing
- **OpenAI GPT API**: Perform sentiment/emotion analysis on user inputs.
- **NLP**: Enhance multi-dimensional emotion classification (joy, sadness, anger, etc.).

### 🗄️ Database (Room)
- Store mood entries with timestamps
- Save music preferences and listening history
- Cache recommended playlists for offline access
- Track mood-music response data for personalization

### 🎧 Music API
- **Spotify Web API**:
  - Access Spotify’s music library categorized by mood, energy, and tempo
  - Retrieve audio features (e.g., valence, energy, tempo)
  - Generate playlists that align with detected moods

---

## 📱 Target Devices

### 📲 Primary: Smartphone
- Main interface for daily interaction
- Quick mood check-ins and music recommendations
- On-the-go music playback
- Camera-based mood detection

### 💻 Secondary: Tablet (Expanded Experience)
- Mood analytics and visualization dashboard
- Enhanced playlist creation tools
- Historical mood-music pattern exploration
- Guided mindfulness/meditation exercises with music

---

## 🔧 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Database**: Room
- **Networking**: Retrofit / OkHttp
- **Permissions**: Camera, Microphone, Location
- **Sensors**: Camera, Microphone, Accelerometer

---

## 🚧 Current Status

> This project is currently under development as a final course project.  
We have tentatively selected the technologies and tools listed above, which may evolve during implementation.

---

## 🤝 Contributors

- Xuan Lou
- Zhengkai Li

---

## 📌 License

[MIT License](LICENSE) 

