Setup
=====

The Thales TSH Pay SDK libraries need to be placed in directory ./libs
Then ./app/build.gradle file needs to be updated with particular SDK version and variant used.
For example:
    def SDK_VERSION="6.5.0"
    def SDK_QUALIFIER="rc01"

	=> Will look for TSHPaySDK-dev-6.5.0.rc01.aar or TSHPaySDK-release-6.5.0.rc01.aar dependencies

Build
=====

The app project is designed to cover various use cases and targets several Thales test environments.
The exact configuration is available through build variants which is composed of tree parts:
    * Payment experience (as described at https://thales-dis-dbp.stoplight.io/docs/tsh-hce-android/ZG9jOjI5NDIzNzAz-payment-experience-api)
    * Target environment (Sandbox, E2E, QA1)
    * Debug/Release

We recommend to start with variant "oneTapEnabledSandboxDebug".


References
==========

* Thales TSH Pay Documentation: https://thales-dis-dbp.stoplight.io/docs/tsh-hce-android/ZG9jOjE-introduction
