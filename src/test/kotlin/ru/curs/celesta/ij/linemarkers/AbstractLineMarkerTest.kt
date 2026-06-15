package ru.curs.celesta.ij.linemarkers

import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.daemon.LineMarkerProviders
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.lang.LanguageExtensionPoint
import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.util.registry.Registry
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import junit.framework.TestCase
import ru.curs.celesta.ij.CelestaProjectDescriptor

abstract class AbstractLineMarkerTest : LightJavaCodeInsightFixtureTestCase() {
    protected abstract val lineMarkerProviderClass: Class<out LineMarkerProvider>
    protected abstract val language: String

    protected abstract val myTestDataRelativePath: String

    override fun getTestDataPath(): String = "testdata/$myTestDataRelativePath"

    override fun getProjectDescriptor(): LightProjectDescriptor = CelestaProjectDescriptor()

    protected val notifications: MutableList<Notification> = mutableListOf()

    override fun setUp() {
        super.setUp()

        project.messageBus.connect(myFixture.projectDisposable).subscribe(Notifications.TOPIC, object : Notifications {
            override fun notify(notification: Notification) {
                notifications.add(notification)
            }
        })

        Registry.get("ide.jvm.run.marker").setValue(true, myFixture.projectDisposable)

        val markerProviderEP = LanguageExtensionPoint<LineMarkerProvider>(
            language, lineMarkerProviderClass.canonicalName, PluginManagerCore.getPlugin(
                PluginManagerCore.CORE_ID
            )!!
        )

        ExtensionTestUtil.maskExtensions(
            LineMarkerProviders.EP_NAME,
            listOf(markerProviderEP),
            myFixture.projectDisposable
        )
    }

    protected fun assertNoNotifications() {
        assertTrue(notifications.isEmpty())
    }

    protected fun assertHasNotification(predicate: (Notification) -> Boolean) {
        TestCase.assertTrue(notifications.any(predicate))
    }
}
