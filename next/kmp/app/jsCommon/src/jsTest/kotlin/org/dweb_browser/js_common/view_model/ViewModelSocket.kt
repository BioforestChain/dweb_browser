import kotlinx.serialization.SealedClassSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.js_common.view_model.SyncData
import org.dweb_browser.js_common.view_model.SyncType

class TestSyncData{
    @Test
    fun testSerializedSyncData(){
        val syncData = SyncData(
            key = "key",
            value = "value",
            type = SyncType.REPLACE
        )

        val jsonString = Json.encodeToString(syncData)
        console.log("jsonString: ", jsonString)
        val d = Json.decodeFromString<SyncData>(jsonString)
        console.log("d: ", d)
        assertEquals("REPLACE", d.type.value)
    }
}

class TestSyncType{
    @Test
    fun testSerializedSyncType(){
        val replace = SyncType.REPLACE
        val jsonString = Json.encodeToString(replace)
        console.log("jsonString", jsonString)
        val replace2 = Json.decodeFromString<SyncType>(jsonString)
        console.log("replace2: ", replace2)
        assertEquals(replace.value, replace2.value)
    }

    @Test
    fun testEquals(){
        val replace = SyncType.REPLACE
        val jsonString = Json.encodeToString(replace)
        val replace2 = Json.decodeFromString<SyncType>(jsonString)
        val value = replace == replace2
        console.log("replace == replace2", replace == replace2)
        assertEquals(true, value)
    }
}

