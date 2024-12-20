package io.github.asephermann.plugins.mocklocationchecker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.scottyab.rootbeer.RootBeer
import io.github.asephermann.plugins.mocklocationchecker.checkRoot.Constants
import io.github.asephermann.plugins.mocklocationchecker.checkRoot.RootDetectionActions
import io.github.asephermann.plugins.mocklocationchecker.checkRoot.RootJailBreakDetector
import io.github.asephermann.plugins.mocklocationchecker.checkRoot.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@CapacitorPlugin(name = "MockLocationChecker")
class MockLocationCheckerPlugin : Plugin() {
    private val implementation = MockLocationChecker()

    @PluginMethod
    fun checkMock(call: PluginCall)  {
        val whiteList = call.getArray("whiteList")
        val ret = JSObject()

        val result = implementation.checkMock(activity, whiteList.toList())
        Log.d(Constants.LOG_TAG, "[checkMock] result: $result")

        ret.put("isRooted", result.isRooted)
        ret.put("isMock", result.isMock)
        ret.put("messages", result.messages)
        ret.put("indicated", result.indicated)
        call.resolve(ret)
    }

    @PluginMethod
    fun isLocationFromMockProvider(call: PluginCall)  {
        val ret = JSObject()

        val result = implementation.isLocationFromMockProvider(activity)

        ret.put("value", result)
        call.resolve(ret)
    }

    @PluginMethod
    fun goToMockLocationAppDetail(call: PluginCall)  {
        val packageName = call.getString("packageName")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse(String.format("package:%s", packageName))
            activity.startActivity(intent)
        }
    }

//    @PluginMethod
//    fun checkMockGeoLocation(call: PluginCall)  {
//        val ret = JSObject()
//
//        val result = implementation.checkMockGeoLocation(activity)
//
//        ret.put("isMock", result.isMock)
//        ret.put("messages", result.messages)
//        ret.put("indicated", result.indicated)
//        call.resolve(ret)
//    }

    @PluginMethod
    suspend fun checkMockGeoLocation(call: PluginCall) {
        val ret = JSObject()

        if (activity != null) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val result = implementation.checkMockGeoLocation(activity)
                    ret.put("isMock", result.isMock)
                    ret.put("messages", result.messages)
                    ret.put("indicated", result.indicated)
                    call.resolve(ret)
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.reject("An error occurred")
                }
            }
        } else {
            call.reject("Activity is null")
        }
    }
  
    //////////////////////////////////////////////////////////
    // check Root and Emulator
    //////////////////////////////////////////////////////////

    private val implementationCheckRoot = RootJailBreakDetector()

    /**
     * Generic method to check rooted status with various configurations.
     */
    private fun checkRootStatus(
        context: Context,
        useBusyBox: Boolean,
        checkEmulator: Boolean
    ): Boolean {
        val rootBeer = RootBeer(context)
        val rootBeerCheck = if (useBusyBox) rootBeer.isRootedWithBusyBoxCheck else rootBeer.isRooted
        val internalCheck = if (checkEmulator) {
            implementationCheckRoot.checkIsRootedWithEmulator(activity)
        } else {
            implementationCheckRoot.checkIsRooted(activity)
        }

        Log.d(Constants.LOG_TAG, "[checkRootStatus] RootBeerCheck: $rootBeerCheck, InternalCheck: $internalCheck")
        return rootBeerCheck || internalCheck
    }

    /**
     * Check root status using RootBeer and internal checks.
     */
    @PluginMethod
    fun isRooted(call: PluginCall) {
        val ret = JSObject()
        try {
            val context = activity.applicationContext
            val isRooted = checkRootStatus(context, useBusyBox = false, checkEmulator = false)
            ret.put("isRooted", isRooted)
            call.resolve(ret)
        } catch (error: Exception) {
            handlePluginError(call, "isRooted", error)
        }
    }

    /**
     * Check root status using RootBeer with BusyBox and internal checks.
     */
    @PluginMethod
    fun isRootedWithBusyBox(call: PluginCall) {
        val ret = JSObject()
        try {
            val context = activity.applicationContext
            val isRooted = checkRootStatus(context, useBusyBox = true, checkEmulator = false)
            ret.put("isRooted", isRooted)
            call.resolve(ret)
        } catch (error: Exception) {
            handlePluginError(call, "isRootedWithBusyBox", error)
        }
    }

    /**
     * Check root status using RootBeer, internal checks, and emulator detection.
     */
    @PluginMethod
    fun isRootedWithEmulator(call: PluginCall) {
        val ret = JSObject()
        try {
            val context = activity.applicationContext
            val isRooted = checkRootStatus(context, useBusyBox = false, checkEmulator = true)
            ret.put("isRooted", isRooted)
            call.resolve(ret)
        } catch (error: Exception) {
            handlePluginError(call, "isRootedWithEmulator", error)
        }
    }

    /**
     * Check root status using RootBeer with BusyBox, internal checks, and emulator detection.
     */
    @PluginMethod
    fun isRootedWithBusyBoxWithEmulator(call: PluginCall) {
        val ret = JSObject()
        try {
            val context = activity.applicationContext
            val isRooted = checkRootStatus(context, useBusyBox = true, checkEmulator = true)
            ret.put("isRooted", isRooted)
            call.resolve(ret)
        } catch (error: Exception) {
            handlePluginError(call, "isRootedWithBusyBoxWithEmulator", error)
        }
    }

    /**
     * Perform a specific root detection action.
     */
    @PluginMethod
    fun whatIsRooted(call: PluginCall) {
        val ret = JSObject()
        try {
            val action = call.getString("action") ?: throw IllegalArgumentException("Action parameter is missing")
            val context = activity.applicationContext
            val rootBeer = RootBeer(context)

            val isRooted = when (action) {
                RootDetectionActions.ACTION_DETECT_ROOT_MANAGEMENT_APPS -> rootBeer.detectRootManagementApps()
                RootDetectionActions.ACTION_DETECT_POTENTIALLY_DANGEROUS_APPS -> rootBeer.detectPotentiallyDangerousApps()
                RootDetectionActions.ACTION_DETECT_TEST_KEYS -> rootBeer.detectTestKeys()
                RootDetectionActions.ACTION_CHECK_FOR_BUSY_BOX_BINARY -> rootBeer.checkForBusyBoxBinary()
                RootDetectionActions.ACTION_CHECK_FOR_SU_BINARY -> rootBeer.checkForSuBinary()
                RootDetectionActions.ACTION_CHECK_SU_EXISTS -> rootBeer.checkSuExists()
                RootDetectionActions.ACTION_CHECK_FOR_RW_PATHS -> rootBeer.checkForRWPaths()
                RootDetectionActions.ACTION_CHECK_FOR_DANGEROUS_PROPS -> rootBeer.checkForDangerousProps()
                RootDetectionActions.ACTION_CHECK_FOR_ROOT_NATIVE -> rootBeer.checkForRootNative()
                RootDetectionActions.ACTION_DETECT_ROOT_CLOAKING_APPS -> rootBeer.detectRootCloakingApps()
                RootDetectionActions.ACTION_IS_SELINUX_FLAG_ENABLED -> Utils.isSelinuxFlagInEnabled
                else -> implementationCheckRoot.whatIsRooted(action, context)
            }

            Log.d(Constants.LOG_TAG, "[whatIsRooted] Action: $action, Result: $isRooted")
            ret.put("isRooted", isRooted)
            call.resolve(ret)
        } catch (error: Exception) {
            handlePluginError(call, "whatIsRooted", error)
        }
    }

    /**
     * Retrieve detailed device information.
     */
    @PluginMethod
    fun getDeviceInfo(call: PluginCall) {
        try {
            val ret = implementationCheckRoot.getDeviceInfo()
            Log.d(Constants.LOG_TAG, "[getDeviceInfo] Result: $ret")
            call.resolve(ret)
        } catch (error: Exception) {
            handlePluginError(call, "getDeviceInfo", error)
        }
    }

    /**
     * Utility method for handling errors in plugin methods.
     */
    private fun handlePluginError(call: PluginCall, methodName: String, error: Exception) {
        Log.e(Constants.LOG_TAG, "[$methodName] Error: ${error.localizedMessage}", error)
        call.reject(error.message, error)
    }
}