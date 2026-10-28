package id.panelin.notifikasi

import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class TraceMigrationTest {
    @Test fun versionOneUpgradeKeepsQueueAndDedupRows() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = context.getDatabasePath("panelin-secure.db")
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file,null).use { db ->
            db.execSQL("CREATE TABLE destinations(route TEXT PRIMARY KEY, payload BLOB NOT NULL)")
            db.execSQL("CREATE TABLE seen(event TEXT PRIMARY KEY)")
            db.execSQL("CREATE TABLE queue(id INTEGER PRIMARY KEY AUTOINCREMENT, event TEXT UNIQUE NOT NULL, route TEXT NOT NULL, payload BLOB NOT NULL, created INTEGER NOT NULL, state TEXT NOT NULL, outcome TEXT NOT NULL, attempts INTEGER NOT NULL, detail TEXT NOT NULL, next_at INTEGER NOT NULL, lease_until INTEGER NOT NULL, sent_at INTEGER NOT NULL)")
            db.execSQL("CREATE INDEX queue_due ON queue(state,next_at)")
            db.execSQL("INSERT INTO seen VALUES ('keep-seen')")
            db.execSQL("INSERT INTO queue VALUES (1,'keep-event','keep-route',X'010203',1,'held','',0,'keep-detail',0,0,0)")
            db.version=1
        }
        AppStore(context).destinations()
        SQLiteDatabase.openDatabase(file.path,null,SQLiteDatabase.OPEN_READONLY).use { db ->
            assertEquals(2,db.version)
            db.rawQuery("SELECT event,payload,state FROM queue",null).use {
                assertTrue(it.moveToFirst())
                assertEquals("keep-event",it.getString(0))
                assertArrayEquals(byteArrayOf(1,2,3),it.getBlob(1))
                assertEquals("held",it.getString(2))
            }
            db.rawQuery("SELECT event FROM seen",null).use { assertTrue(it.moveToFirst()); assertEquals("keep-seen",it.getString(0)) }
            db.rawQuery("SELECT COUNT(*) FROM trace",null).use { assertTrue(it.moveToFirst()); assertEquals(0,it.getInt(0)) }
        }
    }
}
