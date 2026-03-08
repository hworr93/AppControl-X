package com.appcontrolx.domain

import com.appcontrolx.model.AppAction
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyValidatorTest {

    private val validator = SafetyValidator()

    @Test
    fun acceptsValidPackageName() {
        val result = validator.validatePackageName("com.example.valid_app")

        assertTrue("Expected valid package to pass validation", result.isSuccess)
    }

    @Test
    fun rejectsInjectionAndInvalidPackageNames() {
        val injected = validator.validatePackageName("com.example.app;rm -rf /")
        val invalidFormat = validator.validatePackageName("com..example")

        assertTrue("Expected injection attempt to fail", injected.isFailure)
        assertTrue("Expected invalid format to fail", invalidFormat.isFailure)
    }

    @Test
    fun blocksActionForCriticalPackage() {
        val result = validator.validateAction("android", AppAction.FORCE_STOP)

        assertTrue("Expected critical package action to be blocked", result.isFailure)
    }

    @Test
    fun allowsActionForSafePackage() {
        val result = validator.validateAction("com.example.safe", AppAction.CLEAR_CACHE)

        assertTrue("Expected safe package action to be allowed", result.isSuccess)
    }
}
