# Short aliases for the Gradle tasks we actually use. Kept narrow on purpose: the dev machine has 8 GB RAM.

ANDROID_HOME ?= $(HOME)/Library/Android/sdk
ADB := $(ANDROID_HOME)/platform-tools/adb
# Android 13 without Play Store, 2 GB RAM: the lightest image that still has the notification permission.
AVD ?= Light_API_33
MSG ?= TL1A2B3C4D Confirmed. Ksh1,100.00 sent to ALICE  MWANGI 0700000001 on 4/12/25 at 8:07 AM. New M-PESA balance is Ksh25,000.00. Transaction cost, Ksh23.00.
.PHONY: help test test-parser coverage apk install stop emulator sms

help:  ## List commands
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | awk -F ':.*## ' '{printf "  make %-12s %s\n", $$1, $$2}'

test:  ## Run all JVM unit tests
	./gradlew :app:testDebugUnitTest

test-parser:  ## Run only the M-Pesa parser tests
	./gradlew :app:testDebugUnitTest --tests "*MpesaParser*"

apk:  ## Build the debug APK (only when about to install it)
	./gradlew :app:assembleDebug

install:  ## Build and install the debug APK on the running emulator or connected phone
	./gradlew :app:installDebug

coverage:  ## Run unit tests with coverage; prints where the HTML report is
	./gradlew :app:createDebugUnitTestCoverageReport
	@echo "Report: app/build/reports/coverage/test/debug/index.html"

stop:  ## Stop the Gradle daemon to free memory
	./gradlew --stop

emulator:  ## Start the light emulator in the background (close Android Studio first to save RAM)
	$(ANDROID_HOME)/emulator/emulator -avd $(AVD) -no-boot-anim -no-audio -no-snapshot-save >/dev/null 2>&1 &

sms:  ## Send an M-Pesa SMS to the emulator: make sms MSG="..."
	$(ADB) emu sms send MPESA "$(MSG)"
