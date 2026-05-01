#!/bin/bash

# Check if multiple devices are connected and target specific device if needed
DEVICE_COUNT=$(adb devices | grep -v "List" | grep "device$" | wc -l)
if [ "$DEVICE_COUNT" -gt 1 ]; then
    TARGET_DEVICE="22101316G"
    echo "Multiple devices detected ($DEVICE_COUNT). Looking for $TARGET_DEVICE..."
    SERIAL=$(adb devices -l | grep "$TARGET_DEVICE" | awk '{print $1}')
    if [ -n "$SERIAL" ]; then
        echo "Targeting device: $SERIAL"
        export ANDROID_SERIAL=$SERIAL
    else
        echo "Target device $TARGET_DEVICE not found. Targeting first device..."
        FIRST_DEVICE=$(adb devices | grep -v "List" | grep "device$" | head -n 1 | awk '{print $1}')
        export ANDROID_SERIAL=$FIRST_DEVICE
        echo "Targeting device: $ANDROID_SERIAL"
    fi
fi

install_ss() {
    # Build and install StackShift Debug
    echo "Building and installing StackShift Debug variant..."
    ./gradlew :androidApp:installStackshiftDebug

    # Launch StackShift
    echo "Launching StackShift..."
    adb shell am start -n com.ugurbuga.blockgames/com.ugurbuga.blockgames.app.MainActivity

    echo "StackShift Done!"
}

install_bw() {
    # Build and install BlockWise Debug
    echo "Building and installing BlockWise Debug variant..."
    ./gradlew :androidApp:installBlockwiseDebug

    # Launch BlockWise
    echo "Launching BlockWise..."
    adb shell am start -n com.ugurbuga.blockwise/com.ugurbuga.blockgames.app.MainActivity

    echo "BlockWise Done!"
}

case "$1" in
    "ss")
        install_ss
        ;;
    "bw")
        install_bw
        ;;
    "all" | "")
        install_ss
        install_bw
        ;;
    *)
        echo "Usage: $0 {ss|bw|all}"
        exit 1
        ;;
esac
