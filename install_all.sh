#!/bin/bash

# Build and install StackShift Debug
echo "Building and installing StackShift Debug variant..."
./gradlew :androidApp:installStackshiftDebug

# Launch StackShift
echo "Launching StackShift..."
adb shell am start -n com.ugurbuga.blockgames/com.ugurbuga.blockgames.app.MainActivity

echo "StackShift Done!"

# Build and install BlockWise Debug
echo "Building and installing BlockWise Debug variant..."
./gradlew :androidApp:installBlockwiseDebug

# Launch BlockWise
echo "Launching BlockWise..."
adb shell am start -n com.ugurbuga.blockwise/com.ugurbuga.blockgames.app.MainActivity

echo "BlockWise Done!"
