# Short aliases for the Gradle tasks we actually use. Kept narrow on purpose: the dev machine has 8 GB RAM.
.PHONY: help test test-parser apk install stop

help:  ## List commands
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | awk -F ':.*## ' '{printf "  make %-12s %s\n", $$1, $$2}'

test:  ## Run all JVM unit tests
	./gradlew :app:testDebugUnitTest

test-parser:  ## Run only the M-Pesa parser tests
	./gradlew :app:testDebugUnitTest --tests "*MpesaParser*"

apk:  ## Build the debug APK (only when about to install it)
	./gradlew :app:assembleDebug

install:  ## Build and install the debug APK on the connected phone
	./gradlew :app:installDebug

stop:  ## Stop the Gradle daemon to free memory
	./gradlew --stop
