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
import com.app2.core.data.local.entity.ProductEntity;
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
public final class ProductDao_Impl implements ProductDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ProductEntity> __insertionAdapterOfProductEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public ProductDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfProductEntity = new EntityInsertionAdapter<ProductEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `products` (`id`,`name`,`description`,`sku`,`barcode`,`quantity`,`min_quantity`,`max_quantity`,`price`,`price_base`,`price_loyal`,`price_school`,`price_student`,`category`,`warehouse_id`,`location_id`,`is_deleted`,`purchase_price_avg`,`discount_category`,`margin_percent`,`is_liquidation`,`extra_prices`,`created_at`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ProductEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getDescription() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getDescription());
        }
        if (entity.getSku() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getSku());
        }
        if (entity.getBarcode() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getBarcode());
        }
        statement.bindLong(6, entity.getQuantity());
        statement.bindLong(7, entity.getMinQuantity());
        statement.bindLong(8, entity.getMaxQuantity());
        statement.bindDouble(9, entity.getPrice());
        if (entity.getPriceBase() == null) {
          statement.bindNull(10);
        } else {
          statement.bindDouble(10, entity.getPriceBase());
        }
        if (entity.getPriceLoyal() == null) {
          statement.bindNull(11);
        } else {
          statement.bindDouble(11, entity.getPriceLoyal());
        }
        if (entity.getPriceSchool() == null) {
          statement.bindNull(12);
        } else {
          statement.bindDouble(12, entity.getPriceSchool());
        }
        if (entity.getPriceStudent() == null) {
          statement.bindNull(13);
        } else {
          statement.bindDouble(13, entity.getPriceStudent());
        }
        if (entity.getCategory() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getCategory());
        }
        if (entity.getWarehouseId() == null) {
          statement.bindNull(15);
        } else {
          statement.bindLong(15, entity.getWarehouseId());
        }
        if (entity.getLocationId() == null) {
          statement.bindNull(16);
        } else {
          statement.bindLong(16, entity.getLocationId());
        }
        final int _tmp = entity.isDeleted() ? 1 : 0;
        statement.bindLong(17, _tmp);
        if (entity.getPurchasePriceAvg() == null) {
          statement.bindNull(18);
        } else {
          statement.bindDouble(18, entity.getPurchasePriceAvg());
        }
        if (entity.getDiscountCategory() == null) {
          statement.bindNull(19);
        } else {
          statement.bindString(19, entity.getDiscountCategory());
        }
        if (entity.getMarginPercent() == null) {
          statement.bindNull(20);
        } else {
          statement.bindDouble(20, entity.getMarginPercent());
        }
        final int _tmp_1 = entity.isLiquidation() ? 1 : 0;
        statement.bindLong(21, _tmp_1);
        if (entity.getExtraPrices() == null) {
          statement.bindNull(22);
        } else {
          statement.bindString(22, entity.getExtraPrices());
        }
        if (entity.getCreatedAt() == null) {
          statement.bindNull(23);
        } else {
          statement.bindString(23, entity.getCreatedAt());
        }
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM products";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<ProductEntity> products,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfProductEntity.insert(products);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insert(final ProductEntity product, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfProductEntity.insert(product);
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
  public Object getAllActive(final Continuation<? super List<ProductEntity>> $completion) {
    final String _sql = "SELECT * FROM products WHERE is_deleted = 0 ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ProductEntity>>() {
      @Override
      @NonNull
      public List<ProductEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfSku = CursorUtil.getColumnIndexOrThrow(_cursor, "sku");
          final int _cursorIndexOfBarcode = CursorUtil.getColumnIndexOrThrow(_cursor, "barcode");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfMinQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "min_quantity");
          final int _cursorIndexOfMaxQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "max_quantity");
          final int _cursorIndexOfPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "price");
          final int _cursorIndexOfPriceBase = CursorUtil.getColumnIndexOrThrow(_cursor, "price_base");
          final int _cursorIndexOfPriceLoyal = CursorUtil.getColumnIndexOrThrow(_cursor, "price_loyal");
          final int _cursorIndexOfPriceSchool = CursorUtil.getColumnIndexOrThrow(_cursor, "price_school");
          final int _cursorIndexOfPriceStudent = CursorUtil.getColumnIndexOrThrow(_cursor, "price_student");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfWarehouseId = CursorUtil.getColumnIndexOrThrow(_cursor, "warehouse_id");
          final int _cursorIndexOfLocationId = CursorUtil.getColumnIndexOrThrow(_cursor, "location_id");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "is_deleted");
          final int _cursorIndexOfPurchasePriceAvg = CursorUtil.getColumnIndexOrThrow(_cursor, "purchase_price_avg");
          final int _cursorIndexOfDiscountCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "discount_category");
          final int _cursorIndexOfMarginPercent = CursorUtil.getColumnIndexOrThrow(_cursor, "margin_percent");
          final int _cursorIndexOfIsLiquidation = CursorUtil.getColumnIndexOrThrow(_cursor, "is_liquidation");
          final int _cursorIndexOfExtraPrices = CursorUtil.getColumnIndexOrThrow(_cursor, "extra_prices");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<ProductEntity> _result = new ArrayList<ProductEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ProductEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpSku;
            if (_cursor.isNull(_cursorIndexOfSku)) {
              _tmpSku = null;
            } else {
              _tmpSku = _cursor.getString(_cursorIndexOfSku);
            }
            final String _tmpBarcode;
            if (_cursor.isNull(_cursorIndexOfBarcode)) {
              _tmpBarcode = null;
            } else {
              _tmpBarcode = _cursor.getString(_cursorIndexOfBarcode);
            }
            final int _tmpQuantity;
            _tmpQuantity = _cursor.getInt(_cursorIndexOfQuantity);
            final int _tmpMinQuantity;
            _tmpMinQuantity = _cursor.getInt(_cursorIndexOfMinQuantity);
            final int _tmpMaxQuantity;
            _tmpMaxQuantity = _cursor.getInt(_cursorIndexOfMaxQuantity);
            final double _tmpPrice;
            _tmpPrice = _cursor.getDouble(_cursorIndexOfPrice);
            final Double _tmpPriceBase;
            if (_cursor.isNull(_cursorIndexOfPriceBase)) {
              _tmpPriceBase = null;
            } else {
              _tmpPriceBase = _cursor.getDouble(_cursorIndexOfPriceBase);
            }
            final Double _tmpPriceLoyal;
            if (_cursor.isNull(_cursorIndexOfPriceLoyal)) {
              _tmpPriceLoyal = null;
            } else {
              _tmpPriceLoyal = _cursor.getDouble(_cursorIndexOfPriceLoyal);
            }
            final Double _tmpPriceSchool;
            if (_cursor.isNull(_cursorIndexOfPriceSchool)) {
              _tmpPriceSchool = null;
            } else {
              _tmpPriceSchool = _cursor.getDouble(_cursorIndexOfPriceSchool);
            }
            final Double _tmpPriceStudent;
            if (_cursor.isNull(_cursorIndexOfPriceStudent)) {
              _tmpPriceStudent = null;
            } else {
              _tmpPriceStudent = _cursor.getDouble(_cursorIndexOfPriceStudent);
            }
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final Integer _tmpWarehouseId;
            if (_cursor.isNull(_cursorIndexOfWarehouseId)) {
              _tmpWarehouseId = null;
            } else {
              _tmpWarehouseId = _cursor.getInt(_cursorIndexOfWarehouseId);
            }
            final Integer _tmpLocationId;
            if (_cursor.isNull(_cursorIndexOfLocationId)) {
              _tmpLocationId = null;
            } else {
              _tmpLocationId = _cursor.getInt(_cursorIndexOfLocationId);
            }
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final Double _tmpPurchasePriceAvg;
            if (_cursor.isNull(_cursorIndexOfPurchasePriceAvg)) {
              _tmpPurchasePriceAvg = null;
            } else {
              _tmpPurchasePriceAvg = _cursor.getDouble(_cursorIndexOfPurchasePriceAvg);
            }
            final String _tmpDiscountCategory;
            if (_cursor.isNull(_cursorIndexOfDiscountCategory)) {
              _tmpDiscountCategory = null;
            } else {
              _tmpDiscountCategory = _cursor.getString(_cursorIndexOfDiscountCategory);
            }
            final Double _tmpMarginPercent;
            if (_cursor.isNull(_cursorIndexOfMarginPercent)) {
              _tmpMarginPercent = null;
            } else {
              _tmpMarginPercent = _cursor.getDouble(_cursorIndexOfMarginPercent);
            }
            final boolean _tmpIsLiquidation;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsLiquidation);
            _tmpIsLiquidation = _tmp_1 != 0;
            final String _tmpExtraPrices;
            if (_cursor.isNull(_cursorIndexOfExtraPrices)) {
              _tmpExtraPrices = null;
            } else {
              _tmpExtraPrices = _cursor.getString(_cursorIndexOfExtraPrices);
            }
            final String _tmpCreatedAt;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmpCreatedAt = null;
            } else {
              _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            }
            _item = new ProductEntity(_tmpId,_tmpName,_tmpDescription,_tmpSku,_tmpBarcode,_tmpQuantity,_tmpMinQuantity,_tmpMaxQuantity,_tmpPrice,_tmpPriceBase,_tmpPriceLoyal,_tmpPriceSchool,_tmpPriceStudent,_tmpCategory,_tmpWarehouseId,_tmpLocationId,_tmpIsDeleted,_tmpPurchasePriceAvg,_tmpDiscountCategory,_tmpMarginPercent,_tmpIsLiquidation,_tmpExtraPrices,_tmpCreatedAt);
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
  public Object getById(final int id, final Continuation<? super ProductEntity> $completion) {
    final String _sql = "SELECT * FROM products WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ProductEntity>() {
      @Override
      @Nullable
      public ProductEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfSku = CursorUtil.getColumnIndexOrThrow(_cursor, "sku");
          final int _cursorIndexOfBarcode = CursorUtil.getColumnIndexOrThrow(_cursor, "barcode");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfMinQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "min_quantity");
          final int _cursorIndexOfMaxQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "max_quantity");
          final int _cursorIndexOfPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "price");
          final int _cursorIndexOfPriceBase = CursorUtil.getColumnIndexOrThrow(_cursor, "price_base");
          final int _cursorIndexOfPriceLoyal = CursorUtil.getColumnIndexOrThrow(_cursor, "price_loyal");
          final int _cursorIndexOfPriceSchool = CursorUtil.getColumnIndexOrThrow(_cursor, "price_school");
          final int _cursorIndexOfPriceStudent = CursorUtil.getColumnIndexOrThrow(_cursor, "price_student");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfWarehouseId = CursorUtil.getColumnIndexOrThrow(_cursor, "warehouse_id");
          final int _cursorIndexOfLocationId = CursorUtil.getColumnIndexOrThrow(_cursor, "location_id");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "is_deleted");
          final int _cursorIndexOfPurchasePriceAvg = CursorUtil.getColumnIndexOrThrow(_cursor, "purchase_price_avg");
          final int _cursorIndexOfDiscountCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "discount_category");
          final int _cursorIndexOfMarginPercent = CursorUtil.getColumnIndexOrThrow(_cursor, "margin_percent");
          final int _cursorIndexOfIsLiquidation = CursorUtil.getColumnIndexOrThrow(_cursor, "is_liquidation");
          final int _cursorIndexOfExtraPrices = CursorUtil.getColumnIndexOrThrow(_cursor, "extra_prices");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final ProductEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpSku;
            if (_cursor.isNull(_cursorIndexOfSku)) {
              _tmpSku = null;
            } else {
              _tmpSku = _cursor.getString(_cursorIndexOfSku);
            }
            final String _tmpBarcode;
            if (_cursor.isNull(_cursorIndexOfBarcode)) {
              _tmpBarcode = null;
            } else {
              _tmpBarcode = _cursor.getString(_cursorIndexOfBarcode);
            }
            final int _tmpQuantity;
            _tmpQuantity = _cursor.getInt(_cursorIndexOfQuantity);
            final int _tmpMinQuantity;
            _tmpMinQuantity = _cursor.getInt(_cursorIndexOfMinQuantity);
            final int _tmpMaxQuantity;
            _tmpMaxQuantity = _cursor.getInt(_cursorIndexOfMaxQuantity);
            final double _tmpPrice;
            _tmpPrice = _cursor.getDouble(_cursorIndexOfPrice);
            final Double _tmpPriceBase;
            if (_cursor.isNull(_cursorIndexOfPriceBase)) {
              _tmpPriceBase = null;
            } else {
              _tmpPriceBase = _cursor.getDouble(_cursorIndexOfPriceBase);
            }
            final Double _tmpPriceLoyal;
            if (_cursor.isNull(_cursorIndexOfPriceLoyal)) {
              _tmpPriceLoyal = null;
            } else {
              _tmpPriceLoyal = _cursor.getDouble(_cursorIndexOfPriceLoyal);
            }
            final Double _tmpPriceSchool;
            if (_cursor.isNull(_cursorIndexOfPriceSchool)) {
              _tmpPriceSchool = null;
            } else {
              _tmpPriceSchool = _cursor.getDouble(_cursorIndexOfPriceSchool);
            }
            final Double _tmpPriceStudent;
            if (_cursor.isNull(_cursorIndexOfPriceStudent)) {
              _tmpPriceStudent = null;
            } else {
              _tmpPriceStudent = _cursor.getDouble(_cursorIndexOfPriceStudent);
            }
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final Integer _tmpWarehouseId;
            if (_cursor.isNull(_cursorIndexOfWarehouseId)) {
              _tmpWarehouseId = null;
            } else {
              _tmpWarehouseId = _cursor.getInt(_cursorIndexOfWarehouseId);
            }
            final Integer _tmpLocationId;
            if (_cursor.isNull(_cursorIndexOfLocationId)) {
              _tmpLocationId = null;
            } else {
              _tmpLocationId = _cursor.getInt(_cursorIndexOfLocationId);
            }
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final Double _tmpPurchasePriceAvg;
            if (_cursor.isNull(_cursorIndexOfPurchasePriceAvg)) {
              _tmpPurchasePriceAvg = null;
            } else {
              _tmpPurchasePriceAvg = _cursor.getDouble(_cursorIndexOfPurchasePriceAvg);
            }
            final String _tmpDiscountCategory;
            if (_cursor.isNull(_cursorIndexOfDiscountCategory)) {
              _tmpDiscountCategory = null;
            } else {
              _tmpDiscountCategory = _cursor.getString(_cursorIndexOfDiscountCategory);
            }
            final Double _tmpMarginPercent;
            if (_cursor.isNull(_cursorIndexOfMarginPercent)) {
              _tmpMarginPercent = null;
            } else {
              _tmpMarginPercent = _cursor.getDouble(_cursorIndexOfMarginPercent);
            }
            final boolean _tmpIsLiquidation;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsLiquidation);
            _tmpIsLiquidation = _tmp_1 != 0;
            final String _tmpExtraPrices;
            if (_cursor.isNull(_cursorIndexOfExtraPrices)) {
              _tmpExtraPrices = null;
            } else {
              _tmpExtraPrices = _cursor.getString(_cursorIndexOfExtraPrices);
            }
            final String _tmpCreatedAt;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmpCreatedAt = null;
            } else {
              _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            }
            _result = new ProductEntity(_tmpId,_tmpName,_tmpDescription,_tmpSku,_tmpBarcode,_tmpQuantity,_tmpMinQuantity,_tmpMaxQuantity,_tmpPrice,_tmpPriceBase,_tmpPriceLoyal,_tmpPriceSchool,_tmpPriceStudent,_tmpCategory,_tmpWarehouseId,_tmpLocationId,_tmpIsDeleted,_tmpPurchasePriceAvg,_tmpDiscountCategory,_tmpMarginPercent,_tmpIsLiquidation,_tmpExtraPrices,_tmpCreatedAt);
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

  @Override
  public Object search(final String query,
      final Continuation<? super List<ProductEntity>> $completion) {
    final String _sql = "SELECT * FROM products WHERE is_deleted = 0 AND (name LIKE '%' || ? || '%' OR sku LIKE '%' || ? || '%')";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, query);
    _argIndex = 2;
    _statement.bindString(_argIndex, query);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ProductEntity>>() {
      @Override
      @NonNull
      public List<ProductEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfSku = CursorUtil.getColumnIndexOrThrow(_cursor, "sku");
          final int _cursorIndexOfBarcode = CursorUtil.getColumnIndexOrThrow(_cursor, "barcode");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfMinQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "min_quantity");
          final int _cursorIndexOfMaxQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "max_quantity");
          final int _cursorIndexOfPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "price");
          final int _cursorIndexOfPriceBase = CursorUtil.getColumnIndexOrThrow(_cursor, "price_base");
          final int _cursorIndexOfPriceLoyal = CursorUtil.getColumnIndexOrThrow(_cursor, "price_loyal");
          final int _cursorIndexOfPriceSchool = CursorUtil.getColumnIndexOrThrow(_cursor, "price_school");
          final int _cursorIndexOfPriceStudent = CursorUtil.getColumnIndexOrThrow(_cursor, "price_student");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfWarehouseId = CursorUtil.getColumnIndexOrThrow(_cursor, "warehouse_id");
          final int _cursorIndexOfLocationId = CursorUtil.getColumnIndexOrThrow(_cursor, "location_id");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "is_deleted");
          final int _cursorIndexOfPurchasePriceAvg = CursorUtil.getColumnIndexOrThrow(_cursor, "purchase_price_avg");
          final int _cursorIndexOfDiscountCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "discount_category");
          final int _cursorIndexOfMarginPercent = CursorUtil.getColumnIndexOrThrow(_cursor, "margin_percent");
          final int _cursorIndexOfIsLiquidation = CursorUtil.getColumnIndexOrThrow(_cursor, "is_liquidation");
          final int _cursorIndexOfExtraPrices = CursorUtil.getColumnIndexOrThrow(_cursor, "extra_prices");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<ProductEntity> _result = new ArrayList<ProductEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ProductEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpSku;
            if (_cursor.isNull(_cursorIndexOfSku)) {
              _tmpSku = null;
            } else {
              _tmpSku = _cursor.getString(_cursorIndexOfSku);
            }
            final String _tmpBarcode;
            if (_cursor.isNull(_cursorIndexOfBarcode)) {
              _tmpBarcode = null;
            } else {
              _tmpBarcode = _cursor.getString(_cursorIndexOfBarcode);
            }
            final int _tmpQuantity;
            _tmpQuantity = _cursor.getInt(_cursorIndexOfQuantity);
            final int _tmpMinQuantity;
            _tmpMinQuantity = _cursor.getInt(_cursorIndexOfMinQuantity);
            final int _tmpMaxQuantity;
            _tmpMaxQuantity = _cursor.getInt(_cursorIndexOfMaxQuantity);
            final double _tmpPrice;
            _tmpPrice = _cursor.getDouble(_cursorIndexOfPrice);
            final Double _tmpPriceBase;
            if (_cursor.isNull(_cursorIndexOfPriceBase)) {
              _tmpPriceBase = null;
            } else {
              _tmpPriceBase = _cursor.getDouble(_cursorIndexOfPriceBase);
            }
            final Double _tmpPriceLoyal;
            if (_cursor.isNull(_cursorIndexOfPriceLoyal)) {
              _tmpPriceLoyal = null;
            } else {
              _tmpPriceLoyal = _cursor.getDouble(_cursorIndexOfPriceLoyal);
            }
            final Double _tmpPriceSchool;
            if (_cursor.isNull(_cursorIndexOfPriceSchool)) {
              _tmpPriceSchool = null;
            } else {
              _tmpPriceSchool = _cursor.getDouble(_cursorIndexOfPriceSchool);
            }
            final Double _tmpPriceStudent;
            if (_cursor.isNull(_cursorIndexOfPriceStudent)) {
              _tmpPriceStudent = null;
            } else {
              _tmpPriceStudent = _cursor.getDouble(_cursorIndexOfPriceStudent);
            }
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final Integer _tmpWarehouseId;
            if (_cursor.isNull(_cursorIndexOfWarehouseId)) {
              _tmpWarehouseId = null;
            } else {
              _tmpWarehouseId = _cursor.getInt(_cursorIndexOfWarehouseId);
            }
            final Integer _tmpLocationId;
            if (_cursor.isNull(_cursorIndexOfLocationId)) {
              _tmpLocationId = null;
            } else {
              _tmpLocationId = _cursor.getInt(_cursorIndexOfLocationId);
            }
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final Double _tmpPurchasePriceAvg;
            if (_cursor.isNull(_cursorIndexOfPurchasePriceAvg)) {
              _tmpPurchasePriceAvg = null;
            } else {
              _tmpPurchasePriceAvg = _cursor.getDouble(_cursorIndexOfPurchasePriceAvg);
            }
            final String _tmpDiscountCategory;
            if (_cursor.isNull(_cursorIndexOfDiscountCategory)) {
              _tmpDiscountCategory = null;
            } else {
              _tmpDiscountCategory = _cursor.getString(_cursorIndexOfDiscountCategory);
            }
            final Double _tmpMarginPercent;
            if (_cursor.isNull(_cursorIndexOfMarginPercent)) {
              _tmpMarginPercent = null;
            } else {
              _tmpMarginPercent = _cursor.getDouble(_cursorIndexOfMarginPercent);
            }
            final boolean _tmpIsLiquidation;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsLiquidation);
            _tmpIsLiquidation = _tmp_1 != 0;
            final String _tmpExtraPrices;
            if (_cursor.isNull(_cursorIndexOfExtraPrices)) {
              _tmpExtraPrices = null;
            } else {
              _tmpExtraPrices = _cursor.getString(_cursorIndexOfExtraPrices);
            }
            final String _tmpCreatedAt;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmpCreatedAt = null;
            } else {
              _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            }
            _item = new ProductEntity(_tmpId,_tmpName,_tmpDescription,_tmpSku,_tmpBarcode,_tmpQuantity,_tmpMinQuantity,_tmpMaxQuantity,_tmpPrice,_tmpPriceBase,_tmpPriceLoyal,_tmpPriceSchool,_tmpPriceStudent,_tmpCategory,_tmpWarehouseId,_tmpLocationId,_tmpIsDeleted,_tmpPurchasePriceAvg,_tmpDiscountCategory,_tmpMarginPercent,_tmpIsLiquidation,_tmpExtraPrices,_tmpCreatedAt);
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
