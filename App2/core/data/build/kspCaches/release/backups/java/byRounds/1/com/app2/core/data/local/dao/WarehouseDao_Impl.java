package com.app2.core.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.app2.core.data.local.entity.WarehouseEntity;
import java.lang.Class;
import java.lang.Exception;
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
public final class WarehouseDao_Impl implements WarehouseDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WarehouseEntity> __insertionAdapterOfWarehouseEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public WarehouseDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWarehouseEntity = new EntityInsertionAdapter<WarehouseEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `warehouses` (`id`,`name`,`address`,`manager`,`phone`,`ice`,`patente`,`rc`,`taxe_number`,`is_default`,`created_at`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WarehouseEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getAddress() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getAddress());
        }
        if (entity.getManager() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getManager());
        }
        if (entity.getPhone() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getPhone());
        }
        if (entity.getIce() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getIce());
        }
        if (entity.getPatente() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getPatente());
        }
        if (entity.getRc() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getRc());
        }
        if (entity.getTaxeNumber() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getTaxeNumber());
        }
        final int _tmp = entity.isDefault() ? 1 : 0;
        statement.bindLong(10, _tmp);
        if (entity.getCreatedAt() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getCreatedAt());
        }
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM warehouses";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<WarehouseEntity> warehouses,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfWarehouseEntity.insert(warehouses);
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
  public Object getAll(final Continuation<? super List<WarehouseEntity>> $completion) {
    final String _sql = "SELECT * FROM warehouses ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<WarehouseEntity>>() {
      @Override
      @NonNull
      public List<WarehouseEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfManager = CursorUtil.getColumnIndexOrThrow(_cursor, "manager");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfIce = CursorUtil.getColumnIndexOrThrow(_cursor, "ice");
          final int _cursorIndexOfPatente = CursorUtil.getColumnIndexOrThrow(_cursor, "patente");
          final int _cursorIndexOfRc = CursorUtil.getColumnIndexOrThrow(_cursor, "rc");
          final int _cursorIndexOfTaxeNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "taxe_number");
          final int _cursorIndexOfIsDefault = CursorUtil.getColumnIndexOrThrow(_cursor, "is_default");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<WarehouseEntity> _result = new ArrayList<WarehouseEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WarehouseEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpAddress;
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _tmpAddress = null;
            } else {
              _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            }
            final String _tmpManager;
            if (_cursor.isNull(_cursorIndexOfManager)) {
              _tmpManager = null;
            } else {
              _tmpManager = _cursor.getString(_cursorIndexOfManager);
            }
            final String _tmpPhone;
            if (_cursor.isNull(_cursorIndexOfPhone)) {
              _tmpPhone = null;
            } else {
              _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            }
            final String _tmpIce;
            if (_cursor.isNull(_cursorIndexOfIce)) {
              _tmpIce = null;
            } else {
              _tmpIce = _cursor.getString(_cursorIndexOfIce);
            }
            final String _tmpPatente;
            if (_cursor.isNull(_cursorIndexOfPatente)) {
              _tmpPatente = null;
            } else {
              _tmpPatente = _cursor.getString(_cursorIndexOfPatente);
            }
            final String _tmpRc;
            if (_cursor.isNull(_cursorIndexOfRc)) {
              _tmpRc = null;
            } else {
              _tmpRc = _cursor.getString(_cursorIndexOfRc);
            }
            final String _tmpTaxeNumber;
            if (_cursor.isNull(_cursorIndexOfTaxeNumber)) {
              _tmpTaxeNumber = null;
            } else {
              _tmpTaxeNumber = _cursor.getString(_cursorIndexOfTaxeNumber);
            }
            final boolean _tmpIsDefault;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDefault);
            _tmpIsDefault = _tmp != 0;
            final String _tmpCreatedAt;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmpCreatedAt = null;
            } else {
              _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            }
            _item = new WarehouseEntity(_tmpId,_tmpName,_tmpAddress,_tmpManager,_tmpPhone,_tmpIce,_tmpPatente,_tmpRc,_tmpTaxeNumber,_tmpIsDefault,_tmpCreatedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
