package com.dermify.dermify.curation

import androidx.lifecycle.LiveData
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.getTotalChangedRows
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Float
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ContributionOutboxDao_Impl(
  __db: RoomDatabase,
) : ContributionOutboxDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfContributionOutbox: EntityInsertAdapter<ContributionOutbox>
  init {
    this.__db = __db
    this.__insertAdapterOfContributionOutbox = object : EntityInsertAdapter<ContributionOutbox>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `contribution_outbox` (`contributionId`,`imagePath`,`phash`,`predictedClass`,`confidence`,`consentVersion`,`appVersion`,`createdAtDate`,`status`,`attemptCount`,`lastError`) VALUES (?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ContributionOutbox) {
        statement.bindText(1, entity.contributionId)
        statement.bindText(2, entity.imagePath)
        statement.bindText(3, entity.phash)
        statement.bindText(4, entity.predictedClass)
        statement.bindDouble(5, entity.confidence.toDouble())
        statement.bindText(6, entity.consentVersion)
        statement.bindText(7, entity.appVersion)
        statement.bindText(8, entity.createdAtDate)
        statement.bindText(9, entity.status)
        statement.bindLong(10, entity.attemptCount.toLong())
        val _tmpLastError: String? = entity.lastError
        if (_tmpLastError == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpLastError)
        }
      }
    }
  }

  public override suspend fun insert(row: ContributionOutbox): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfContributionOutbox.insert(_connection, row)
  }

  public override suspend fun byStatus(status: String): List<ContributionOutbox> {
    val _sql: String = "SELECT * FROM contribution_outbox WHERE status = ? ORDER BY createdAtDate ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, status)
        val _columnIndexOfContributionId: Int = getColumnIndexOrThrow(_stmt, "contributionId")
        val _columnIndexOfImagePath: Int = getColumnIndexOrThrow(_stmt, "imagePath")
        val _columnIndexOfPhash: Int = getColumnIndexOrThrow(_stmt, "phash")
        val _columnIndexOfPredictedClass: Int = getColumnIndexOrThrow(_stmt, "predictedClass")
        val _columnIndexOfConfidence: Int = getColumnIndexOrThrow(_stmt, "confidence")
        val _columnIndexOfConsentVersion: Int = getColumnIndexOrThrow(_stmt, "consentVersion")
        val _columnIndexOfAppVersion: Int = getColumnIndexOrThrow(_stmt, "appVersion")
        val _columnIndexOfCreatedAtDate: Int = getColumnIndexOrThrow(_stmt, "createdAtDate")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfAttemptCount: Int = getColumnIndexOrThrow(_stmt, "attemptCount")
        val _columnIndexOfLastError: Int = getColumnIndexOrThrow(_stmt, "lastError")
        val _result: MutableList<ContributionOutbox> = mutableListOf()
        while (_stmt.step()) {
          val _item: ContributionOutbox
          val _tmpContributionId: String
          _tmpContributionId = _stmt.getText(_columnIndexOfContributionId)
          val _tmpImagePath: String
          _tmpImagePath = _stmt.getText(_columnIndexOfImagePath)
          val _tmpPhash: String
          _tmpPhash = _stmt.getText(_columnIndexOfPhash)
          val _tmpPredictedClass: String
          _tmpPredictedClass = _stmt.getText(_columnIndexOfPredictedClass)
          val _tmpConfidence: Float
          _tmpConfidence = _stmt.getDouble(_columnIndexOfConfidence).toFloat()
          val _tmpConsentVersion: String
          _tmpConsentVersion = _stmt.getText(_columnIndexOfConsentVersion)
          val _tmpAppVersion: String
          _tmpAppVersion = _stmt.getText(_columnIndexOfAppVersion)
          val _tmpCreatedAtDate: String
          _tmpCreatedAtDate = _stmt.getText(_columnIndexOfCreatedAtDate)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpAttemptCount: Int
          _tmpAttemptCount = _stmt.getLong(_columnIndexOfAttemptCount).toInt()
          val _tmpLastError: String?
          if (_stmt.isNull(_columnIndexOfLastError)) {
            _tmpLastError = null
          } else {
            _tmpLastError = _stmt.getText(_columnIndexOfLastError)
          }
          _item = ContributionOutbox(_tmpContributionId,_tmpImagePath,_tmpPhash,_tmpPredictedClass,_tmpConfidence,_tmpConsentVersion,_tmpAppVersion,_tmpCreatedAtDate,_tmpStatus,_tmpAttemptCount,_tmpLastError)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun byId(id: String): ContributionOutbox? {
    val _sql: String = "SELECT * FROM contribution_outbox WHERE contributionId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfContributionId: Int = getColumnIndexOrThrow(_stmt, "contributionId")
        val _columnIndexOfImagePath: Int = getColumnIndexOrThrow(_stmt, "imagePath")
        val _columnIndexOfPhash: Int = getColumnIndexOrThrow(_stmt, "phash")
        val _columnIndexOfPredictedClass: Int = getColumnIndexOrThrow(_stmt, "predictedClass")
        val _columnIndexOfConfidence: Int = getColumnIndexOrThrow(_stmt, "confidence")
        val _columnIndexOfConsentVersion: Int = getColumnIndexOrThrow(_stmt, "consentVersion")
        val _columnIndexOfAppVersion: Int = getColumnIndexOrThrow(_stmt, "appVersion")
        val _columnIndexOfCreatedAtDate: Int = getColumnIndexOrThrow(_stmt, "createdAtDate")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfAttemptCount: Int = getColumnIndexOrThrow(_stmt, "attemptCount")
        val _columnIndexOfLastError: Int = getColumnIndexOrThrow(_stmt, "lastError")
        val _result: ContributionOutbox?
        if (_stmt.step()) {
          val _tmpContributionId: String
          _tmpContributionId = _stmt.getText(_columnIndexOfContributionId)
          val _tmpImagePath: String
          _tmpImagePath = _stmt.getText(_columnIndexOfImagePath)
          val _tmpPhash: String
          _tmpPhash = _stmt.getText(_columnIndexOfPhash)
          val _tmpPredictedClass: String
          _tmpPredictedClass = _stmt.getText(_columnIndexOfPredictedClass)
          val _tmpConfidence: Float
          _tmpConfidence = _stmt.getDouble(_columnIndexOfConfidence).toFloat()
          val _tmpConsentVersion: String
          _tmpConsentVersion = _stmt.getText(_columnIndexOfConsentVersion)
          val _tmpAppVersion: String
          _tmpAppVersion = _stmt.getText(_columnIndexOfAppVersion)
          val _tmpCreatedAtDate: String
          _tmpCreatedAtDate = _stmt.getText(_columnIndexOfCreatedAtDate)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpAttemptCount: Int
          _tmpAttemptCount = _stmt.getLong(_columnIndexOfAttemptCount).toInt()
          val _tmpLastError: String?
          if (_stmt.isNull(_columnIndexOfLastError)) {
            _tmpLastError = null
          } else {
            _tmpLastError = _stmt.getText(_columnIndexOfLastError)
          }
          _result = ContributionOutbox(_tmpContributionId,_tmpImagePath,_tmpPhash,_tmpPredictedClass,_tmpConfidence,_tmpConsentVersion,_tmpAppVersion,_tmpCreatedAtDate,_tmpStatus,_tmpAttemptCount,_tmpLastError)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun pendingCount(): Int {
    val _sql: String = "SELECT COUNT(*) FROM contribution_outbox WHERE status IN ('PENDING', 'UPLOADING')"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun failedCount(): Int {
    val _sql: String = "SELECT COUNT(*) FROM contribution_outbox WHERE status = 'FAILED'"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeAll(): LiveData<List<ContributionOutbox>> {
    val _sql: String = "SELECT * FROM contribution_outbox ORDER BY createdAtDate DESC"
    return __db.invalidationTracker.createLiveData(arrayOf("contribution_outbox"), false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfContributionId: Int = getColumnIndexOrThrow(_stmt, "contributionId")
        val _columnIndexOfImagePath: Int = getColumnIndexOrThrow(_stmt, "imagePath")
        val _columnIndexOfPhash: Int = getColumnIndexOrThrow(_stmt, "phash")
        val _columnIndexOfPredictedClass: Int = getColumnIndexOrThrow(_stmt, "predictedClass")
        val _columnIndexOfConfidence: Int = getColumnIndexOrThrow(_stmt, "confidence")
        val _columnIndexOfConsentVersion: Int = getColumnIndexOrThrow(_stmt, "consentVersion")
        val _columnIndexOfAppVersion: Int = getColumnIndexOrThrow(_stmt, "appVersion")
        val _columnIndexOfCreatedAtDate: Int = getColumnIndexOrThrow(_stmt, "createdAtDate")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfAttemptCount: Int = getColumnIndexOrThrow(_stmt, "attemptCount")
        val _columnIndexOfLastError: Int = getColumnIndexOrThrow(_stmt, "lastError")
        val _result: MutableList<ContributionOutbox> = mutableListOf()
        while (_stmt.step()) {
          val _item: ContributionOutbox
          val _tmpContributionId: String
          _tmpContributionId = _stmt.getText(_columnIndexOfContributionId)
          val _tmpImagePath: String
          _tmpImagePath = _stmt.getText(_columnIndexOfImagePath)
          val _tmpPhash: String
          _tmpPhash = _stmt.getText(_columnIndexOfPhash)
          val _tmpPredictedClass: String
          _tmpPredictedClass = _stmt.getText(_columnIndexOfPredictedClass)
          val _tmpConfidence: Float
          _tmpConfidence = _stmt.getDouble(_columnIndexOfConfidence).toFloat()
          val _tmpConsentVersion: String
          _tmpConsentVersion = _stmt.getText(_columnIndexOfConsentVersion)
          val _tmpAppVersion: String
          _tmpAppVersion = _stmt.getText(_columnIndexOfAppVersion)
          val _tmpCreatedAtDate: String
          _tmpCreatedAtDate = _stmt.getText(_columnIndexOfCreatedAtDate)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpAttemptCount: Int
          _tmpAttemptCount = _stmt.getLong(_columnIndexOfAttemptCount).toInt()
          val _tmpLastError: String?
          if (_stmt.isNull(_columnIndexOfLastError)) {
            _tmpLastError = null
          } else {
            _tmpLastError = _stmt.getText(_columnIndexOfLastError)
          }
          _item = ContributionOutbox(_tmpContributionId,_tmpImagePath,_tmpPhash,_tmpPredictedClass,_tmpConfidence,_tmpConsentVersion,_tmpAppVersion,_tmpCreatedAtDate,_tmpStatus,_tmpAttemptCount,_tmpLastError)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun requeueStalled(): Int {
    val _sql: String = "UPDATE contribution_outbox SET status = 'PENDING' WHERE status = 'UPLOADING'"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateStatus(
    id: String,
    status: String,
    attempts: Int,
    error: String?,
  ) {
    val _sql: String = "UPDATE contribution_outbox SET status = ?, attemptCount = ?, lastError = ? WHERE contributionId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, status)
        _argIndex = 2
        _stmt.bindLong(_argIndex, attempts.toLong())
        _argIndex = 3
        if (error == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, error)
        }
        _argIndex = 4
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun retryFailed(): Int {
    val _sql: String = "UPDATE contribution_outbox SET status = 'PENDING', attemptCount = 0, lastError = NULL WHERE status = 'FAILED'"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: String) {
    val _sql: String = "DELETE FROM contribution_outbox WHERE contributionId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
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
