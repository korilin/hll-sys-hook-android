# Android 15 线程挂起崩溃处理库使用指南

## 概述

这个库已经优化完成，现在可以全面处理 Android 15 上所有类型的线程挂起相关崩溃，包括：

1. **SuspendThreadByPeer** 相关崩溃（原有功能）
2. **ThreadList::SuspendAll** 相关崩溃（新增功能）

## 优化内容

### Android 15 专门优化

- 新增 `art::Thread::AbortInThis` hook 处理
- 增加 SuspendAll 相关错误消息检测
- 实现双重 hook 机制，提高成功率

### 兼容性保证

- 完全向后兼容 Android 5-14
- 不需要修改现有应用代码
- 自动根据系统版本选择最佳处理方案

## 使用方法

### 1. 基本使用

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化线程挂起安全助手
        SuspendThreadSafeHelper.getInstance()
            .suspendThreadSafe(object : SuspendThreadSafeHelper.SuspendThreadCallback {
                override fun suspendThreadTimeout(waitTime: Double) {
                    // 处理线程挂起超时
                    Log.i("ThreadSafe", "线程挂起超时，等待时间: ${waitTime}秒")
                    
                    // 可以在这里上报到崩溃监控系统
                    // CrashReporter.reportNonFatal("Thread suspend timeout: ${waitTime}s")
                }

                override fun onError(errorMsg: String) {
                    // 处理初始化错误
                    Log.e("ThreadSafe", "初始化失败: $errorMsg")
                }
            })
    }
}
```

### 2. 在 Activity 中使用

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 通常建议在 Application 中初始化，这里仅作示例
        SuspendThreadSafeHelper.getInstance()
            .suspendThreadSafe(object : SuspendThreadSafeHelper.SuspendThreadCallback {
                override fun suspendThreadTimeout(waitTime: Double) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, 
                            "检测到线程挂起超时", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(errorMsg: String) {
                    Log.e("MainActivity", "错误: $errorMsg")
                }
            })
    }
}
```

## 测试验证

### 自动测试场景

库内置了多种测试场景，会自动触发可能的崩溃情况：

1. **高频线程名称修改**：触发 SuspendThreadByPeer
2. **频繁获取线程堆栈**：触发 SuspendThreadByPeer  
3. **大量短生命周期线程**：可能触发 SuspendAll
4. **高频 GC 触发**：可能触发 SuspendAll

### 手动测试（可选）

如果想要手动测试，可以参考以下代码：

```kotlin
// 测试 Android 15 SuspendAll 场景
private fun testSuspendAllScenario() {
    repeat(50) { index ->
        thread(name = "TestThread-$index") {
            // 分配内存触发 GC
            val data = ByteArray(1024 * 1024)
            Thread.sleep(10)
        }
    }
    
    // 触发 GC
    System.gc()
}
```

## 监控和日志

### 成功日志

当 hook 成功设置时，会看到如下日志：

```
I/suspend_thread_safe_v15: Hook setup success - StringPrintf: YES, AbortInThis: YES
```

### 拦截日志

当成功拦截崩溃时：

```
I/suspend_thread_safe_v15: AbortInThis intercepted: [错误消息]
W/suspend_thread_safe_v15: SuspendAll timeout detected, preventing crash: [错误消息]
```

### 错误日志

如果 hook 失败：

```
E/suspend_thread_safe_v15: StringPrintf Hook setup failed: [错误信息]
E/suspend_thread_safe_v15: AbortInThis Hook setup failed: [错误信息]
```

## 性能影响

- **内存开销**：极小，只增加少量 hook 函数指针
- **性能影响**：仅在发生线程挂起时才执行代理逻辑
- **稳定性**：使用双重 hook 机制，即使部分 hook 失败也能提供保护

## 注意事项

1. **初始化时机**：建议在 Application.onCreate() 中尽早初始化
2. **权限要求**：无需特殊权限
3. **混淆配置**：已内置混淆规则，无需额外配置
4. **版本兼容**：支持 Android 5.0+ 所有版本

## 问题排查

### 常见问题

1. **hook 失败**
   - 检查设备架构是否支持（arm64/arm/x86/x86_64）
   - 确认 ShadowHook 初始化成功

2. **回调未触发**
   - 确认已正确设置回调
   - 检查是否在主线程中设置回调

3. **崩溃仍然发生**
   - 检查 logcat 中的 hook 成功日志
   - 确认崩溃确实是线程挂起相关

### 调试建议

启用详细日志：

```kotlin
// 在测试环境中可以这样设置更详细的日志过滤
adb logcat | grep -E "(suspend_thread_safe|ThreadSafe|ShadowHook)"
```
