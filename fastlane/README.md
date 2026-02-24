# Fastlane Metadata for DeviceInsight

This directory contains metadata for F-Droid and Google Play Store listings.

## Structure

```
fastlane/
└── metadata/
    └── android/
        └── en-US/
            ├── title.txt
            ├── short_description.txt
            ├── full_description.txt
            ├── changelogs/
            │   └── 1.txt
            └── images/
                └── phoneScreenshots/
```

## Usage

### F-Droid
F-Droid automatically uses this metadata when building and publishing the app.

### Google Play Store
For Play Store uploads, use `fastlane supply` to sync metadata to Play Console.
