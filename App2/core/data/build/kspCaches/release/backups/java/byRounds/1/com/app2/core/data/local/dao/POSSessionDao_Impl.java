package com.app2.core.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.app2.core.data.local.entity.POSSessionEntity;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class POSSessionDao_Impl implements POSSessionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<POSSessionEntity> __insertionAdapterOfPOSSessionEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public POSSessionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPOSSessionEntity = new EntityInsertionAdapter<POSSessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `pos_sessions` (`id`,`session_number`,`warehouse_id`,`user_name`,`opening_cash`,`closing_cash`,`expected_cash`,`status`,`opened_at`,`closed_at`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final POSSessionEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getSessionNumber() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getSessionNumber());
        }
        if (entity.getWarehouseId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, entity.getWarehouseId());
        }
        if (entity.getUserName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getUserName());
        }
        statement.bindDouble(5, entity.getOpeningCash());
        if (entity.getClosingCash() == null) {
          statement.bindNull(6);
        } else {
          statement.bindDouble(6, entity.getClosingCash());
        }
        if (entity.getExpectedCash() == null) {
          statement.bindNull(7);
        } else {
          statement.bindDouble(7, entity.getExpectedCash());
        }
        statement.bindString(8, entity.getStatus());
        if (entity.getOpenedAt() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getOpenedAt());
        }
        if (entity.getClosedAt() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getClosedAt());
        }
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM pos_sessions";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final POSSessionEntity session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPOSSessionEntity.insert(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getAll(final Continuation<? super List<POSSessionEntity>> $completion) {
    final String _sql = "SELECT * FROM pos_sessions ORDER BY opened_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<POSSessionEntity>>() {
      @Override
      @NonNull
      public List<POSSessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "session_number");
          final int _cursorIndexOfWarehouseId = CursorUtil.getColumnIndexOrThrow(_cursor, "warehouse_id");
          final int _cursorIndexOfUserName = CursorUtil.getColumnIndexOrThrow(_cursor, "user_name");
          final int _cursorIndexOfOpeningCash = CursorUtil.getColumnIndexOrThrow(_cursor, "opening_cash");
          final int _cursorIndexOfClosingCash = CursorUtil.getColumnIndexOrThrow(_cursor, "closing_cash");
          final int _cursorIndexOfExpectedCash = CursorUtil.getColumnIndexOrThrow(_cursor, "expected_cash");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfOpenedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "opened_at");
          final int _cursorIndexOfClosedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "closed_at");
          final List<POSSessionEntity> _result = new ArrayList<POSSessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final POSSessionEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpSessionNumber;
            if (_cursor.isNull(_cursorIndexOfSessionNumber)) {
              _tmpSessionNumber = null;
            } else {
              _tmpSessionNumber = _cursor.getString(_cursorIndexOfSessionNumber);
            }
            final Integer _tmpWarehouseId;
            if (_cursor.isNull(_cursorIndexOfWarehouseId)) {
              _tmpWarehouseId = null;
            } else {
              _tmpWarehouseId = _cursor.getInt(_cursorIndexOfWarehouseId);
            }
            final String _tmpUserName;
            if (_cursor.isNull(_cursorIndexOfUserName)) {
              _tmpUserName = null;
            } else {
              _tmpUserName = _cursor.getString(_cursorIndexOfUserName);
            }
            final double _tmpOpeningCash;
            _tmpOpeningCash = _cursor.getDouble(_cursorIndexOfOpeningCash);
            final Double _tmpClosingCash;
            if (_cursor.isNull(_cursorIndexOfClosingCash)) {
              _tmpClosingCash = null;
            } else {
              _tmpClosingCash = _cursor.getDouble(_cursorIndexOfClosingCash);
            }
            final Double _tmpExpectedCash;
            if (_cursor.isNull(_cursorIndexOfExpectedCash)) {
              _tmpExpectedCash = null;
            } else {
              _tmpExpectedCash = _cursor.getDouble(_cursorIndexOfExpectedCash);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpOpenedAt;
            if (_cursor.isNull(_cursorIndexOfOpenedAt)) {
              _tmpOpenedAt = null;
            } else {
              _tmpOpenedAt = _cursor.getString(_cursorIndexOfOpenedAt);
            }
            final String _tmpClosedAt;
            if (_cursor.isNull(_cursorIndexOfClosedAt)) {
              _tmpClosedAt = null;
            } else {
              _tmpClosedAt = _cursor.getString(_cursorIndexOfClosedAt);
            }
            _item = new POSSessionEntity(_tmpId,_tmpSessionNumber,_tmpWarehouseId,_tmpUserName,_tmpOpeningCash,_tmpClosingCash,_tmpExpectedCash,_tmpStatus,_tmpOpenedAt,_tmpClosedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getOpenSession(final Continuation<? super POSSessionEntity> $completion) {
    final String _sql = "SELECT * FROM pos_sessions WHERE status = 'open' LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<POSSessionEntity>() {
      @Override
      @Nullable
      public POSSessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "session_number");
          final int _cursorIndexOfWarehouseId = CursorUtil.getColumnIndexOrThrow(_cursor, "warehouse_id");
          final int _cursorIndexOfUserName = CursorUtil.getColumnIndexOrThrow(_cursor, "user_name");
          final int _cursorIndexOfOpeningCash = CursorUtil.getColumnIndexOrThrow(_cursor, "opening_cash");
          final int _cursorIndexOfClosingCash = CursorUtil.getColumnIndexOrThrow(_cursor, "closing_cash");
          final int _cursorIndexOfExpectedCash = CursorUtil.getColumnIndexOrThrow(_cursor, "expected_cash");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfOpenedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "opened_at");
          final int _cursorIndexOfClosedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "closed_at");
          final POSSessionEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpSessionNumber;
            if (_cursor.isNull(_cursorIndexOfSessionNumber)) {
              _tmpSessionNumber = null;
            } else {
              _tmpSessionNumber = _cursor.getString(_cursorIndexOfSessionNumber);
            }
            final Integer _tmpWarehouseId;
            if (_cursor.isNull(_cursorIndexOfWarehouseId)) {
              _tmpWarehouseId = null;
            } else {
              _tmpWarehouseId = _cursor.getInt(_cursorIndexOfWarehouseId);
            }
            final String _tmpUserName;
            if (_cursor.isNull(_cursorIndexOfUserName)) {
              _tmpUserName = null;
            } else {
              _tmpUserName = _cursor.getString(_cursorIndexOfUserName);
            }
            final double _tmpOpeningCash;
            _tmpOpeningCash = _cursor.getDouble(_cursorIndexOfOpeningCash);
            final Double _tmpClosingCash;
            if (_cursor.isNull(_cursorIndexOfClosingCash)) {
              _tmpClosingCash = null;
            } else {
              _tmpClosingCash = _cursor.getDouble(_cursorIndexOfClosingCash);
            }
            final Double _tmpExpectedCash;
            if (_cursor.isNull(_cursorIndexOfExpectedCash)) {
              _tmpExpectedCash = null;
            } else {
              _tmpExpectedCash = _cursor.getDouble(_cursorIndexOfExpectedCash);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpOpenedAt;
            if (_cursor.isNull(_cursorIndexOfOpenedAt)) {
              _tmpOpenedAt = null;
            } else {
              _tmpOpenedAt = _cursor.getString(_cursorIndexOfOpenedAt);
            }
            final String _tmpClosedAt;
            if (_cursor.isNull(_cursorIndexOfClosedAt)) {
              _tmpClosedAt = null;
            } else {
              _tmpClosedAt = _cursor.getString(_cursorIndexOfClosedAt);
            }
            _result = new POSSessionEntity(_tmpId,_tmpSessionNumber,_tmpWarehouseId,_tmpUserName,_tmpOpeningCash,_tmpClosingCash,_tmpExpectedCash,_tmpStatus,_tmpOpenedAt,_tmpClosedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
