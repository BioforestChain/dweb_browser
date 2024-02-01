import org.dweb_browser.js_backend.view_model.ViewModelState
import org.dweb_browser.js_backend.view_model.ViewModelStateRole
import org.dweb_browser.js_backend.view_model.viewModelMutableMapOf
import org.dweb_browser.test.runCommonTest

import kotlin.test.Test
import kotlin.test.assertEquals

class TestViewModelState(){
    // 测试
    // 1. 实例化
    // 2. 获取状态值
    @Test
    fun testGetValue() = runCommonTest{
        console.log("开始测试从ViewModelState中获取数据 start")
        val v = 10
        val instance = ViewModelState(viewModelMutableMapOf("count" to v))
        assertEquals(v,instance["count"], )
        console.log("开始测试从ViewModelState中获取数据 end")
    }

    // 测试
    // 1. 通过 instance[key] = value 实现以Server角色设置状态
    // 2. onUpdate() 监听通过 Sever 角色更新状态
    @Test
    fun testSetByServer() = runCommonTest {
        val newKey = "count"
        val newValue = "10"
        val instance = ViewModelState(viewModelMutableMapOf())
        var hasCallback = false
        instance.onUpdate(ViewModelStateRole.SERVER){ key, value ->
            assertEquals(key,newKey)
            assertEquals(value, newValue)
            hasCallback = true
        }
        instance[newKey] = newValue
        assertEquals(hasCallback, true, "testSetByServer失败")
        console.log("试以 SERVER 角色设置状态和监听状态成功")
    }

    // 测试
    // 1. 通过 instance.set(key, value, role = server)实现以Server角色设置状态
    // 2. onUpdate() 监听通过 Sever 角色更新状态
    @Test
    fun testSetByServerRole() = runCommonTest {
        val newKey = "count"
        val newValue = "10"
        val instance = ViewModelState(viewModelMutableMapOf())
        var hasCallback = false
        instance.onUpdate(ViewModelStateRole.SERVER){ key, value ->
            assertEquals(key,newKey)
            assertEquals(value, newValue)
            hasCallback = true
        }
        instance.set(newKey, newValue, ViewModelStateRole.SERVER)
        assertEquals(hasCallback, true, " instance.set(newKey, newValue,ViewModelStateRole.SERVER) 失败")
        console.log("testSetByServerRole 完成")
    }

    // 1. 通过 instance.set(key, value, role = client)实现以client角色设置状态
    // 2. onUpdate() 监听通过 client 角色更新状态
    @Test
    fun testSetByClientRole() = runCommonTest {
        val newKey = "count"
        val newValue = "10"
        val instance = ViewModelState(viewModelMutableMapOf())
        var hasCallback = false
        instance.onUpdate(ViewModelStateRole.CLIENT){ key, value ->
            assertEquals(key,newKey)
            assertEquals(value, newValue)
            hasCallback = true
        }
        instance.set(newKey, newValue, ViewModelStateRole.CLIENT)
        assertEquals(hasCallback, true, " instance.set(newKey, newValue,ViewModelStateRole.CLIENT) 失败")
        console.log("testSetByClientRole 完成")
    }
}


/**
 *

 class A()

 如何实现 A()[key]



 */