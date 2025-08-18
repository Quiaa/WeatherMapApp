# WeatherMap App

WeatherMap is a modern Android application that provides real-time weather information on an interactive map. It allows users to see weather conditions for any location they select, view their current location's weather, and even see other users' locations on the map in real-time. The app also features an AI-powered assistant and real-time chat between users.

## ✨ Features

- **User Authentication**: Secure sign-up and login using Firebase Authentication (Email & Password).
- **Interactive Map**: Utilizes the Mapbox SDK for a smooth and responsive map interface.
- **Click to See Weather**: Simply tap on any point on the map to get instant weather details for that location.
- **Current Location Weather**: A dedicated button to fetch and display weather for your current device location.
- **Real-time Location Sharing**: See where other users are on the map in real-time. Your location is also shared with others.
- **AI-Powered Assistant**: Chat with an AI assistant powered by a local Large Language Model (Ollama) to get weather-related information and more.
- **Real-time Private Chat**: Engage in one-on-one chats with other users on the map.
- **Weather Data Caching**: Weather data is cached locally using Room, allowing for offline access and a faster user experience.
- **Detailed Weather Information**: Displays current temperature, weather conditions (e.g., "Clear sky"), and a descriptive icon.
- **Modern Architecture**: Built with Kotlin and follows modern Android development practices using Android ViewModel for state management.

## 🛠️ Technologies & Libraries Used

- **Kotlin**: The official language for modern Android development.
- **Firebase**:
  - **Firebase Authentication**: For handling user sign-in and registration.
  - **Cloud Firestore**: As the backend to save and sync user locations and chat messages in real-time.
- **Mapbox SDK**: For displaying the interactive map and handling map gestures.
- **OpenWeatherMap API**: To fetch weather data.
- **Ollama**: For running the local Large Language Model that powers the AI assistant.
- **Retrofit**: For making network requests to the OpenWeatherMap and Ollama APIs.
- **Glide**: For loading and displaying weather icons.
- **Android Architecture Components**:
  - **ViewModel**: To manage UI-related data in a lifecycle-conscious way.
  - **LiveData**: To build data objects that notify views when the underlying data changes.
  - **Hilt**: For dependency injection to manage dependencies and simplify the architecture.
  - **Room**: For local database caching of weather data.
- **Coroutines**: For managing background threads and asynchronous operations.
- **Google Play Services Location**: For fetching the device's current location.

## 🚀 Getting Started

To get a local copy up and running, follow these simple steps.

### Prerequisites

- You need to have Android Studio installed on your machine.
- You need to have Ollama installed and running on your local machine.

### Installation & Setup

1.  **Clone the repo**
    ```sh
    git clone https://github.com/Quiaa/WeatherMapApp.git
    ```
2.  **Setup Firebase**
    - Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project.
    - Enable **Authentication** with the **Email/Password** sign-in provider.
    - Create a **Cloud Firestore** database.
    - From your project's settings, download the `google-services.json` file.
    - Place the downloaded `google-services.json` file in the `app/` directory of the project.

3.  **Setup OpenWeatherMap API Key**
    - Go to [OpenWeatherMap](https://openweathermap.org/api) and create an account to get an API key.
    - In the root directory of the project, create a file named `local.properties`.
    - Add your API key to the `local.properties` file like this:
      ```properties
      OPENWEATHER_API_KEY="YOUR_OPENWEATHERMAP_API_KEY"
      ```

4.  **Setup Mapbox Access Token**
    - Go to [Mapbox](https://www.mapbox.com/) and create an account to get a public access token.
    - Open the `app/src/main/res/values/strings.xml` file.
    - Replace the placeholder `"MAPBOXAPIKEYHERE"` with your actual Mapbox access token:
      ```xml
      <string name="mapbox_access_token" translatable="false">YOUR_MAPBOX_ACCESS_TOKEN</string>
      ```
5.  **Setup Ollama**
    - Install Ollama by following the instructions on the [Ollama website](https://ollama.ai/).
    - Pull the `llama3` model by running the following command in your terminal:
        ```sh
        ollama run llama3
        ```
    - The application is configured to connect to the Ollama server at `http://10.0.2.2:11434/`, which is the default for accessing the host machine's localhost from the Android emulator. Ensure your Ollama server is running and accessible at this address.

6.  **Build and Run**
    - Open the project in Android Studio.
    - Let Gradle sync the dependencies.
    - Build and run the application on an emulator or a physical device.

## 🖼️ Screenshots


<img width="400" height="870" alt="image" src="https://github.com/user-attachments/assets/7a374b5f-5bbe-413f-8b96-beb3ab6f1a5a" />
<img width="400" height="900" alt="image" src="https://github.com/user-attachments/assets/5aa2dae0-d923-47c9-8ed0-ee04f2a8d04f" />
<img width="400" height="900" alt="image" src="https://github.com/user-attachments/assets/554fd03c-be73-4c7f-b22a-65103b065439" />
<img width="400" height="900" alt="image" src="https://github.com/user-attachments/assets/46d3710c-acd5-4807-95b1-1a3382e5a387" />




---
