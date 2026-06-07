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
import com.app2.core.data.local.entity.InvoiceEntity;
import java.lang.Class;
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
public final class InvoiceDao_Impl implements InvoiceDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<InvoiceEntity> __insertionAdapterOfInvoiceEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public InvoiceDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfInvoiceEntity = new EntityInsertionAdapter<InvoiceEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `invoices` (`id`,`invoice_number`,`customer_id`,`warehouse_id`,`status`,`subtotal`,`discount_total`,`tax_amount`,`total`,`notes`,`due_date`,`paid_at`,`type`,`payment_method`,`created_at`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final InvoiceEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getInvoiceNumber());
        if (entity.getCustomerId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, entity.getCustomerId());
        }
        if (entity.getWarehouseId() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getWarehouseId());
        }
        statement.bindString(5, entity.getStatus());
        statement.bindDouble(6, entity.getSubtotal());
        statement.bindDouble(7, entity.getDiscountTotal());
        statement.bindDouble(8, entity.getTaxAmount());
        statement.bindDouble(9, entity.getTotal());
        if (entity.getNotes() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getNotes());
        }
        if (entity.getDueDate() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getDueDate());
        }
        if (entity.getPaidAt() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getPaidAt());
        }
        if (entity.getType() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getType());
        }
        if (entity.getPaymentMethod() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getPaymentMethod());
        }
        if (entity.getCreatedAt() == null) {
          statement.bindNull(15);
        } else {
          statement.bindString(15, entity.getCreatedAt());
        }
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM invoices";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<InvoiceEntity> invoices,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfInvoiceEntity.insert(invoices);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insert(final InvoiceEntity invoice, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfInvoiceEntity.insert(invoice);
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
  public Object getAll(final Continuation<? super List<InvoiceEntity>> $completion) {
    final String _sql = "SELECT * FROM invoices ORDER BY created_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<InvoiceEntity>>() {
      @Override
      @NonNull
      public List<InvoiceEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfInvoiceNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "invoice_number");
          final int _cursorIndexOfCustomerId = CursorUtil.getColumnIndexOrThrow(_cursor, "customer_id");
          final int _cursorIndexOfWarehouseId = CursorUtil.getColumnIndexOrThrow(_cursor, "warehouse_id");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfSubtotal = CursorUtil.getColumnIndexOrThrow(_cursor, "subtotal");
          final int _cursorIndexOfDiscountTotal = CursorUtil.getColumnIndexOrThrow(_cursor, "discount_total");
          final int _cursorIndexOfTaxAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "tax_amount");
          final int _cursorIndexOfTotal = CursorUtil.getColumnIndexOrThrow(_cursor, "total");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "due_date");
          final int _cursorIndexOfPaidAt = CursorUtil.getColumnIndexOrThrow(_cursor, "paid_at");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfPaymentMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "payment_method");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<InvoiceEntity> _result = new ArrayList<InvoiceEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final InvoiceEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpInvoiceNumber;
            _tmpInvoiceNumber = _cursor.getString(_cursorIndexOfInvoiceNumber);
            final Integer _tmpCustomerId;
            if (_cursor.isNull(_cursorIndexOfCustomerId)) {
              _tmpCustomerId = null;
            } else {
              _tmpCustomerId = _cursor.getInt(_cursorIndexOfCustomerId);
            }
            final Integer _tmpWarehouseId;
            if (_cursor.isNull(_cursorIndexOfWarehouseId)) {
              _tmpWarehouseId = null;
            } else {
              _tmpWarehouseId = _cursor.getInt(_cursorIndexOfWarehouseId);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final double _tmpSubtotal;
            _tmpSubtotal = _cursor.getDouble(_cursorIndexOfSubtotal);
            final double _tmpDiscountTotal;
            _tmpDiscountTotal = _cursor.getDouble(_cursorIndexOfDiscountTotal);
            final double _tmpTaxAmount;
            _tmpTaxAmount = _cursor.getDouble(_cursorIndexOfTaxAmount);
            final double _tmpTotal;
            _tmpTotal = _cursor.getDouble(_cursorIndexOfTotal);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpDueDate;
            if (_cursor.isNull(_cursorIndexOfDueDate)) {
              _tmpDueDate = null;
            } else {
              _tmpDueDate = _cursor.getString(_cursorIndexOfDueDate);
            }
            final String _tmpPaidAt;
            if (_cursor.isNull(_cursorIndexOfPaidAt)) {
              _tmpPaidAt = null;
            } else {
              _tmpPaidAt = _cursor.getString(_cursorIndexOfPaidAt);
            }
            final String _tmpType;
            if (_cursor.isNull(_cursorIndexOfType)) {
              _tmpType = null;
            } else {
              _tmpType = _cursor.getString(_cursorIndexOfType);
            }
            final String _tmpPaymentMethod;
            if (_cursor.isNull(_cursorIndexOfPaymentMethod)) {
              _tmpPaymentMethod = null;
            } else {
              _tmpPaymentMethod = _cursor.getString(_cursorIndexOfPaymentMethod);
            }
            final String _tmpCreatedAt;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmpCreatedAt = null;
            } else {
              _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            }
            _item = new InvoiceEntity(_tmpId,_tmpInvoiceNumber,_tmpCustomerId,_tmpWarehouseId,_tmpStatus,_tmpSubtotal,_tmpDiscountTotal,_tmpTaxAmount,_tmpTotal,_tmpNotes,_tmpDueDate,_tmpPaidAt,_tmpType,_tmpPaymentMethod,_tmpCreatedAt);
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
  public Object getById(final int id, final Continuation<? super InvoiceEntity> $completion) {
    final String _sql = "SELECT * FROM invoices WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<InvoiceEntity>() {
      @Override
      @Nullable
      public InvoiceEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfInvoiceNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "invoice_number");
          final int _cursorIndexOfCustomerId = CursorUtil.getColumnIndexOrThrow(_cursor, "customer_id");
          final int _cursorIndexOfWarehouseId = CursorUtil.getColumnIndexOrThrow(_cursor, "warehouse_id");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfSubtotal = CursorUtil.getColumnIndexOrThrow(_cursor, "subtotal");
          final int _cursorIndexOfDiscountTotal = CursorUtil.getColumnIndexOrThrow(_cursor, "discount_total");
          final int _cursorIndexOfTaxAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "tax_amount");
          final int _cursorIndexOfTotal = CursorUtil.getColumnIndexOrThrow(_cursor, "total");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "due_date");
          final int _cursorIndexOfPaidAt = CursorUtil.getColumnIndexOrThrow(_cursor, "paid_at");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfPaymentMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "payment_method");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final InvoiceEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpInvoiceNumber;
            _tmpInvoiceNumber = _cursor.getString(_cursorIndexOfInvoiceNumber);
            final Integer _tmpCustomerId;
            if (_cursor.isNull(_cursorIndexOfCustomerId)) {
              _tmpCustomerId = null;
            } else {
              _tmpCustomerId = _cursor.getInt(_cursorIndexOfCustomerId);
            }
            final Integer _tmpWarehouseId;
            if (_cursor.isNull(_cursorIndexOfWarehouseId)) {
              _tmpWarehouseId = null;
            } else {
              _tmpWarehouseId = _cursor.getInt(_cursorIndexOfWarehouseId);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final double _tmpSubtotal;
            _tmpSubtotal = _cursor.getDouble(_cursorIndexOfSubtotal);
            final double _tmpDiscountTotal;
            _tmpDiscountTotal = _cursor.getDouble(_cursorIndexOfDiscountTotal);
            final double _tmpTaxAmount;
            _tmpTaxAmount = _cursor.getDouble(_cursorIndexOfTaxAmount);
            final double _tmpTotal;
            _tmpTotal = _cursor.getDouble(_cursorIndexOfTotal);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpDueDate;
            if (_cursor.isNull(_cursorIndexOfDueDate)) {
              _tmpDueDate = null;
            } else {
              _tmpDueDate = _cursor.getString(_cursorIndexOfDueDate);
            }
            final String _tmpPaidAt;
            if (_cursor.isNull(_cursorIndexOfPaidAt)) {
              _tmpPaidAt = null;
            } else {
              _tmpPaidAt = _cursor.getString(_cursorIndexOfPaidAt);
            }
            final String _tmpType;
            if (_cursor.isNull(_cursorIndexOfType)) {
              _tmpType = null;
            } else {
              _tmpType = _cursor.getString(_cursorIndexOfType);
            }
            final String _tmpPaymentMethod;
            if (_cursor.isNull(_cursorIndexOfPaymentMethod)) {
              _tmpPaymentMethod = null;
            } else {
              _tmpPaymentMethod = _cursor.getString(_cursorIndexOfPaymentMethod);
            }
            final String _tmpCreatedAt;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmpCreatedAt = null;
            } else {
              _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            }
            _result = new InvoiceEntity(_tmpId,_tmpInvoiceNumber,_tmpCustomerId,_tmpWarehouseId,_tmpStatus,_tmpSubtotal,_tmpDiscountTotal,_tmpTaxAmount,_tmpTotal,_tmpNotes,_tmpDueDate,_tmpPaidAt,_tmpType,_tmpPaymentMethod,_tmpCreatedAt);
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
