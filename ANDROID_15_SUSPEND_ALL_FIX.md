# Android 15 ThreadList::SuspendAll 崩溃修复方案

## 问题描述

在 Android 15 上，除了现有的 `SuspendThreadByPeer` 崩溃场景外，还出现了新的 `ThreadList::SuspendAll` 相关的崩溃。崩溃堆栈如下：

```
#04 pc 00000000008b30fc /apex/com.android.art/lib64/libart.so (art::Thread::AbortInThis(...)+772)
#06 pc 00000000004191b0 /apex/com.android.art/lib64/libart.so (art::ThreadList::SuspendAll(char const*, bool)+140)
```

## 修复方案

### 1. 新增 Thread::AbortInThis Hook

在 `suspend_thread_timeout_v15.cpp` 中新增了对 `art::Thread::AbortInThis` 方法的 hook 处理：

- **符号名称**: `_ZN3art6Thread11AbortInThisERKNSt3__112basic_stringIcNS1_11char_traitsIcEENS1_9allocatorIcEEEE`
- **目标库**: `/apex/com.android.art/lib64/libart.so` (64位) 或 `/apex/com.android.art/lib/libart.so` (32位)

### 2. 错误消息检测

新增 `checkSuspendAllErrorMessage()` 函数来检测 SuspendAll 相关的错误消息：

```cpp
bool checkSuspendAllErrorMessage(const std::string &msg) {
    return msg.find("suspend all") != std::string::npos ||
           msg.find("SuspendAll") != std::string::npos ||
           msg.find("ThreadList::SuspendAll") != std::string::npos ||
           (msg.find("timed out") != std::string::npos && 
            msg.find("barrier") != std::string::npos) ||
           msg.find("Suspend1Barrier") != std::string::npos ||
           msg.find("suspend1_barrier") != std::string::npos;
}
```

### 3. 双重 Hook 机制

现在 Android 15 版本同时 hook 两个关键点：

1. **StringPrintf**: 处理 `SuspendThreadByPeer` 相关的超时崩溃（原有逻辑）
2. **AbortInThis**: 处理 `ThreadList::SuspendAll` 相关的崩溃（新增逻辑）

### 4. 容错处理

- 如果 StringPrintf hook 失败，但 AbortInThis hook 成功，仍然能提供部分保护
- 如果 AbortInThis hook 失败，但 StringPrintf hook 成功，仍然能处理原有的崩溃场景
- 只有当两个 hook 都失败时，才会报告错误

## 代码变更总结

### 新增变量
```cpp
void *abortInThisStubFunction = nullptr;
void *abortInThisOriginalFunction = nullptr;
typedef void (*AbortInThis_t)(void *self, const std::string &msg);
```

### 新增函数
```cpp
void proxyAbortInThisFunc(void *self, const std::string &msg);
bool checkSuspendAllErrorMessage(const std::string &msg);
```

### 修改的函数
- `initHookPoint()`: 增加了对 AbortInThis 的 hook 设置
- 错误处理逻辑更加健壮

## 测试方案

在 `MainActivity.kt` 中新增了 `testAndroid15SuspendAllScenario()` 函数来测试 Android 15 的特定场景：

1. 创建大量短生命周期线程
2. 高频触发 GC
3. 模拟可能导致 SuspendAll 被调用的场景

## 使用方法

使用方法与之前完全相同，无需修改应用代码：

```kotlin
SuspendThreadSafeHelper.getInstance()
    .suspendThreadSafe(object : SuspendThreadSafeHelper.SuspendThreadCallback {
        override fun suspendThreadTimeout(waitTime: Double) {
            Log.i("TAG", "等待线程挂起完成，耗时: $waitTime 秒")
        }

        override fun onError(errorMsg: String) {
            Log.e("TAG", "错误: $errorMsg")
        }
    })
```

## 兼容性

- 完全向后兼容，不影响 Android 5-14 的现有功能
- 专门针对 Android 15+ 进行了增强
- 使用双重 hook 机制提高成功率和覆盖范围

## 日志输出

当检测到 SuspendAll 相关崩溃时，会输出如下日志：

```
I/suspend_thread_safe_v15: AbortInThis intercepted: [错误消息]
W/suspend_thread_safe_v15: SuspendAll timeout detected, preventing crash: [错误消息]
```

当 hook 设置成功时：

```
I/suspend_thread_safe_v15: Hook setup success - StringPrintf: YES, AbortInThis: YES
```
