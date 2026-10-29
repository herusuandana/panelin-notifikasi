#!/usr/bin/env python3
"""Execute the actual refresh coroutine offline; excludes Compose/Android gesture rendering."""
import os
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
cache = Path.home() / '.gradle/caches/modules-2/files-2.1'
lib = next((Path.home() / '.gradle/wrapper/dists').glob('gradle-*-bin/*/gradle-*/lib'))
java = str(next((Path.home() / '.local/share/panelin-android-toolchain').glob('jdk-17*/bin/java')))
compiler = os.pathsep.join(str(p) for p in lib.glob('*.jar'))
cp = os.pathsep.join([str(next(lib.glob('kotlin-stdlib-*.jar'))), *[str(next((cache / 'org.jetbrains.kotlinx' / name / '1.7.3').glob('*/*.jar'))) for name in ['kotlinx-coroutines-core-jvm', 'kotlinx-coroutines-test-jvm']]])
source = (root / 'app/src/main/java/id/panelin/notifikasi/MainActivity.kt').read_text()
method = source.split('    private suspend fun refresh(page: Int)', 1)[1].split('    private fun mutate', 1)[0]
kotlin = '''import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.coroutines.resume
class Fixture {
    var refreshing=true; var reading=false; var busy=false
    var refreshCount=0; var reads=0; var apps=0; var fail=false
    lateinit var frame: CancellableContinuation<Unit>
    suspend fun withFrameNanos(block: () -> Unit) { suspendCancellableCoroutine<Unit> { frame=it }; block() }
    suspend fun reload() { reads++; if(fail) error("read failed") }
    suspend fun loadApps() { apps++ }
    suspend fun refresh(page: Int)''' + method + '''}
@OptIn(ExperimentalCoroutinesApi::class)
fun main() = runTest {
    val f=Fixture()
    repeat(12) { page ->
        f.refreshing=true
        val work=launch { f.refresh(page%4) }
        runCurrent()
        check(f.refreshing && f.reads==page) // No read before loading frame.
        f.frame.resume(Unit); work.join()
        check(!f.refreshing && f.refreshCount==page+1 && f.reads==page+1)
    }
    check(f.apps==3)
    val failed=Fixture().apply { fail=true }
    val attempt=async { runCatching { failed.refresh(0) } }
    runCurrent(); failed.frame.resume(Unit)
    check(attempt.await().isFailure && !failed.refreshing && failed.refreshCount==0)
    val waiting=Fixture().apply { busy=true }
    val cancelled=launch { waiting.refresh(0) }
    runCurrent(); waiting.frame.resume(Unit); runCurrent()
    check(waiting.refreshing && waiting.reads==0)
    cancelled.cancelAndJoin()
    check(!waiting.refreshing && waiting.reads==0)
    println("Refresh coroutine OK: 12 completions, deferred read, app reload, error and cancellation cleanup.")
}
'''
with tempfile.TemporaryDirectory(prefix='refresh-flow-', dir=Path.home() / '.hermes/cache/scratch') as directory:
    directory = Path(directory)
    file = directory / 'Refresh.kt'
    file.write_text(kotlin)
    classes = directory / 'classes'
    subprocess.run([java, '-cp', compiler, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect', '-classpath', cp, '-d', str(classes), str(file)], check=True)
    subprocess.run([java, '-cp', str(classes) + os.pathsep + cp, 'RefreshKt'], check=True)
