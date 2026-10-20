package com.dermify.dermify.`data`

import androidx.lifecycle.LiveData
import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class SkinScanDao_Impl(
  __db: RoomDatabase,
) : SkinScanDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfSkinScan: EntityInsertAdapter<SkinScan>

  private val __deleteAdapterOfSkinScan: EntityDeleteOrUpdateAdapter<SkinScan>
  init {
    this.__db = __db
    this.__insertAdapterOfSkinScan = object : EntityInsertAdapter<SkinScan>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `skin_scans` (`id`,`imagePath`,`predictedDisease`,`confidence`,`severity`,`userCorrectedLabel`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SkinScan) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.imagePath)
        statement.bindText(3, entity.predictedDisease)
        statement.bindDouble(4, entity.confidence.toDouble())
        statement.bindText(5, entity.severity)
        val _tmpUserCorrectedLabel: String? = entity.userCorrectedLabel
        if (_tmpUserCorrectedLabel == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpUserCorrectedLabel)
        }
        statement.bindLong(7, entity.timestamp)
      }
    }
    this.__deleteAdapterOfSkinScan = object : EntityDeleteOrUpdateAdapter<SkinScan>() {
      protected override fun createQuery(): String = "DELETE FROM `skin_scans` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SkinScan) {
        statement.bindLong(1, entity.id)
      }
    }
  }

  public override suspend fun insert(scan: SkinScan): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfSkinScan.insertAndReturnId(_connection, scan)
    _result
  }

  public override suspend fun delete(scan: SkinScan): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfSkinScan.handle(_connection, scan)
  }

  public override fun getAllScans(): LiveData<List<SkinScan>> {
    val _sql: String = "SELECT * FROM skin_scans ORDER BY timestamp DESC"
    return __db.invalidationTracker.createLiveData(arrayOf("skin_scans"), false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfImagePath: Int = getColumnIndexOrThrow(_stmt, "imagePath")
        val _columnIndexOfPredictedDisease: Int = getColumnIndexOrThrow(_stmt, "predictedDisease")
        val _columnIndexOfConfidence: Int = getColumnIndexOrThrow(_stmt, "confidence")
        val _columnIndexOfSeverity: Int = getColumnIndexOrThrow(_stmt, "severity")
        val _columnIndexOfUserCorrectedLabel: Int = getColumnIndexOrThrow(_stmt, "userCorrectedLabel")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<SkinScan> = mutableListOf()
        while (_stmt.step()) {
          val _item: SkinScan
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpImagePath: String
          _tmpImagePath = _stmt.getText(_columnIndexOfImagePath)
          val _tmpPredictedDisease: String
          _tmpPredictedDisease = _stmt.getText(_columnIndexOfPredictedDisease)
          val _tmpConfidence: Float
          _tmpConfidence = _stmt.getDouble(_columnIndexOfConfidence).toFloat()
          val _tmpSeverity: String
          _tmpSeverity = _stmt.getText(_columnIndexOfSeverity)
          val _tmpUserCorrectedLabel: String?
          if (_stmt.isNull(_columnIndexOfUserCorrectedLabel)) {
            _tmpUserCorrectedLabel = null
          } else {
            _tmpUserCorrectedLabel = _stmt.getText(_columnIndexOfUserCorrectedLabel)
          }
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item = SkinScan(_tmpId,_tmpImagePath,_tmpPredictedDisease,_tmpConfidence,_tmpSeverity,_tmpUserCorrectedLabel,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAll() {
    val _sql: String = "DELETE FROM skin_scans"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
