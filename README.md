# Solar Tracker

Solar Tracker is an Android application designed to manage and monitor solar panel installations. It provides a comprehensive dashboard for tracking real-time panel status, weather conditions, and performance forecasts, with a specific focus on solar sites in the Rajkot region.

## Key Features

*   **Live Dashboard**: Monitor panel status, current energy output, and system health at a glance.
*   **Weather Integration**: Real-time weather updates and forecasts powered by Open-Meteo and Tomorrow.io to help predict energy generation.
*   **AI-Powered Insights**: Uses Gemini AI to analyze performance data and provide actionable recommendations.
*   **Multi-Site Management**: Easily switch between different solar installation sites.
*   **Offline Support**: Local data persistence using Room ensures your data is available even without an internet connection.
*   **Modern UI**: Built entirely with Jetpack Compose for a smooth, responsive user experience.

## Getting Started

### Prerequisites

*   [Android Studio](https://developer.android.com/studio) (Ladybug or newer recommended)
*   An Android device or emulator running API 24+

### Setup

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/Yanshu04/solar-tracker.git
    ```

2.  **Configure API Keys**:
    The app requires a Gemini API key to function.
    *   Create a file named `.env` in the root directory.
    *   Add your key: `GEMINI_API_KEY=your_actual_key_here`
    *   (Optional) Add `TOMORROW_API_KEY` if you have one; otherwise, the app will use mock data for weather.

3.  **Build and Run**:
    *   Open the project in Android Studio.
    *   Sync Gradle and wait for the build to finish.
    *   Click **Run** to deploy to your device.

## Tech Stack

*   **UI**: Jetpack Compose
*   **Asynchronous Programming**: Kotlin Coroutines & Flow
*   **Networking**: Retrofit & OkHttp
*   **Local Database**: Room
*   **Dependency Injection**: ViewModelProvider (Manual DI)
*   **AI**: Google AI SDK for Android (Gemini)

---
*Developed as a smart solution for solar energy management.*
