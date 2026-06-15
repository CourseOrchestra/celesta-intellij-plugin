package ru.curs.celesta.ij

import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.intellij.util.PathUtil
import java.io.File

private val libPath = PathUtil.toSystemIndependentName(File("testdata", "libs").absolutePath)!!

/** Light project descriptor that puts the celesta runtime jars and a real JDK on the test module. */
internal class CelestaProjectDescriptor : DefaultLightProjectDescriptor() {

    override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
        listOf("celesta-core-7.2.4.jar", "celesta-sql-7.2.4.jar", "celesta-system-services-7.2.4.jar").forEach {
            PsiTestUtil.addLibrary(model, it.removeSuffix(".jar"), libPath, it)
        }

        super.configureModule(module, model, contentEntry)
    }

    override fun getSdk(): Sdk {
        return JavaSdk.getInstance().createJdk("TEST_JDK", IdeaTestUtil.requireRealJdkHome(), false)
    }
}
