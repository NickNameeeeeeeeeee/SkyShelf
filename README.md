# SkyShelf

SkyShelf is a native Android weather app that turns cities into a personal, visual library. Users can explore locations, save favorite cities, view current conditions and maps, and receive practical weather guidance from an optional NVIDIA NIM integration.

The project is written in Java and focuses on a polished mobile experience with custom draggable controls, card-based city details, local profiles, and responsive bottom-sheet interactions.

## Features

- Search for cities through the Open-Meteo geocoding service
- View current temperature, humidity, conditions, wind speed, and wind direction
- Save and remove cities from a personal library
- Browse suggested cities on the home screen
- Load city photography from Wikimedia page data
- Open an interactive city map powered by Leaflet and web map tiles
- Generate concise, practical weather guidance through NVIDIA NIM
- Fall back to locally generated guidance when NVIDIA NIM is unavailable
- Create local profiles and keep separate saved-city libraries on the same device
- Customize account details and profile imagery
- Use custom draggable segmented controls and sheet-style navigation

## Screens and Navigation

- **Home** — suggested cities and quick access to saved locations
- **Library** — cities saved under the active local profile
- **Search** — Explore and Library search modes with contextual city actions
- **City Details** — hero imagery, live weather, practical insight, and map
- **Settings** — local profile, account details, and app preferences

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Java 20 |
| UI | Android Views, XML layouts, Material Components |
| Networking | OkHttp |
| Weather and geocoding | Open-Meteo APIs |
| City imagery | Wikimedia REST page summaries |
| Maps | Android WebView, Leaflet, OpenStreetMap/CARTO tiles |
| Optional weather insight | NVIDIA NIM chat-completions API |
| Local persistence | SharedPreferences |
| Build system | Gradle 8.10.2, Android Gradle Plugin 8.7.3 |

## Requirements

- Android Studio with Android SDK 35
- JDK 20
- Android device or emulator running Android 10 (API 29) or newer
- Internet access for live weather, imagery, maps, and optional NIM insights

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/NickNameeeeeeeeeee/SkyShelf
cd SkyShelf
```

### 2. Create `local.properties`

Copy the example configuration file:

```bash
cp local.properties.example local.properties
```

On Windows Command Prompt:

```bat
copy local.properties.example local.properties
```

Set the Android SDK path in `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
```

### 3. Configure NVIDIA NIM (optional)

Open-Meteo weather and city search do not require an API key. NVIDIA NIM is only needed for generated weather guidance.

Add your own configuration to `local.properties`:

```properties
NVIDIA_NIM_API_KEY=your_nvidia_nim_api_key
NVIDIA_NIM_MODEL=mistralai/mistral-large-3-675b-instruct-2512
NVIDIA_NIM_BASE_URL=https://integrate.api.nvidia.com/v1
```

Do not commit `local.properties` or any real credentials. The file is excluded by `.gitignore`.

### 4. Build and run

Open the project root in Android Studio, allow Gradle sync to complete, and run the `app` configuration.

You can also build from the command line:

```bash
./gradlew assembleDebug
```

On Windows:

```bat
gradlew.bat assembleDebug
```

## Project Structure

```text
app/src/main/
├── java/com/skyshelf/app/
│   ├── MainActivity.java                 # Home and Library
│   ├── AddCity.java                      # Explore and saved-city search
│   ├── DetailsActivity.java              # Weather, insight, imagery, and map
│   ├── SettingsActivity.java             # Settings and local account UI
│   ├── WeatherRepository.java            # Open-Meteo integration
│   ├── CityImageRepository.java          # Wikimedia image lookup
│   ├── NvidiaNimApiClient.java           # Optional NIM integration
│   ├── DraggableSegmentedControl.java    # Custom segmented control
│   └── ...
└── res/
    ├── layout/                            # Screen and list-item layouts
    ├── drawable/                          # Shapes, icons, and backgrounds
    └── values/                            # Strings, colors, and themes
```

## Data and Privacy

SkyShelf does not include a remote account backend. Profiles, credentials used by the local demo account flow, saved cities, and profile settings are stored on the device through `SharedPreferences`.

The app sends city or weather-related requests to third-party services when their features are used:

- Open-Meteo for city lookup and current weather
- Wikimedia for city imagery
- Web map providers for map rendering
- NVIDIA NIM when optional generated insight is configured

This local account implementation is suitable for a portfolio project or prototype, but it should be replaced with secure authentication and encrypted storage before production use.

## Design Notes

The interface uses a flat purple-and-white visual system with custom interaction details, including:

- Long-press draggable segmented indicators
- Snap and bounce behavior for floating detail/settings sheets
- Card-based city browsing
- Compact contextual menus
- Dynamic hero imagery and responsive loading states

## Current Scope

SkyShelf is a complete portfolio application, but it is not intended to be a production weather-alert service. It currently focuses on current conditions rather than forecasts, severe-weather notifications, or background location monitoring.

## Acknowledgements

SkyShelf uses data and services provided by Open-Meteo, Wikimedia, Leaflet, OpenStreetMap, CARTO, and NVIDIA NIM. Their respective terms and attribution requirements apply.
