package com.dermify.dermify.`data`

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.dermify.dermify.curation.ContributionOutboxDao
import com.dermify.dermify.curation.ContributionOutboxDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _skinScanDao: Lazy<SkinScanDao> = lazy {
    SkinScanDao_Impl(this)
  }

  private val _contributionOutboxDao: Lazy<ContributionOutboxDao> = lazy {
    ContributionOutboxDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(2, "dede1c94340cf363dd0c08c1ad59682d", "c703da28662e4cc5850e5df384ef7150") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `skin_scans` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `imagePath` TEXT NOT NULL, `predictedDisease` TEXT NOT NULL, `confidence` REAL NOT NULL, `severity` TEXT NOT NULL, `userCorrectedLabel` TEXT, `timestamp` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `contribution_outbox` (`contributionId` TEXT NOT NULL, `imagePath` TEXT NOT NULL, `phash` TEXT NOT NULL, `predictedClass` TEXT NOT NULL, `confidence` REAL NOT NULL, `consentVersion` TEXT NOT NULL, `appVersion` TEXT NOT NULL, `createdAtDate` TEXT NOT NULL, `status` TEXT NOT NULL, `attemptCount` INTEGER NOT NULL, `lastError` TEXT, PRIMARY KEY(`contributionId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'dede1c94340cf363dd0c08c1ad59682d')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `skin_scans`")
        connection.execSQL("DROP TABLE IF EXISTS `contribution_outbox`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsSkinScans: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSkinScans.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSkinScans.put("imagePath", TableInfo.Column("imagePath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSkinScans.put("predictedDisease", TableInfo.Column("predictedDisease", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSkinScans.put("confidence", TableInfo.Column("confidence", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSkinScans.put("severity", TableInfo.Column("severity", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSkinScans.put("userCorrectedLabel", TableInfo.Column("userCorrectedLabel", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSkinScans.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSkinScans: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesSkinScans: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSkinScans: TableInfo = TableInfo("skin_scans", _columnsSkinScans, _foreignKeysSkinScans, _indicesSkinScans)
        val _existingSkinScans: TableInfo = read(connection, "skin_scans")
        if (!_infoSkinScans.equals(_existingSkinScans)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |skin_scans(com.dermify.dermify.data.SkinScan).
              | Expected:
              |""".trimMargin() + _infoSkinScans + """
              |
              | Found:
              |""".trimMargin() + _existingSkinScans)
        }
        val _columnsContributionOutbox: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsContributionOutbox.put("contributionId", TableInfo.Column("contributionId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsContributionOutbox.put("imagePath", TableInfo.Column("imagePath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsContributionOutbox.put("phash", TableInfo.Column("phash", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsContributionOutbox.put("predictedClass", TableInfo.Column("predictedClass", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsContributionOutbox.put("confidence", TableInfo.Column("confidence", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsContributionOutbox.put("consentVersion", TableInfo.Column("consentVersion", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsContributionOutbox.put("appVersion", TableInfo.Column("appVersion", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsContributionOutbox.put("createdAtDate", TableInfo.Column("createdAtDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsContributionOutbox.put("status", TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsContributionOutbox.put("attemptCount", TableInfo.Column("attemptCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsContributionOutbox.put("lastError", TableInfo.Column("lastError", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysContributionOutbox: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesContributionOutbox: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoContributionOutbox: TableInfo = TableInfo("contribution_outbox", _columnsContributionOutbox, _foreignKeysContributionOutbox, _indicesContributionOutbox)
        val _existingContributionOutbox: TableInfo = read(connection, "contribution_outbox")
        if (!_infoContributionOutbox.equals(_existingContributionOutbox)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |contribution_outbox(com.dermify.dermify.curation.ContributionOutbox).
              | Expected:
              |""".trimMargin() + _infoContributionOutbox + """
              |
              | Found:
              |""".trimMargin() + _existingContributionOutbox)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "skin_scans", "contribution_outbox")
  }

  public override fun clearAllTables() {
    super.performClear(false, "skin_scans", "contribution_outbox")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(SkinScanDao::class, SkinScanDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ContributionOutboxDao::class, ContributionOutboxDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun skinScanDao(): SkinScanDao = _skinScanDao.value

  public override fun contributionOutboxDao(): ContributionOutboxDao = _contributionOutboxDao.value
}
