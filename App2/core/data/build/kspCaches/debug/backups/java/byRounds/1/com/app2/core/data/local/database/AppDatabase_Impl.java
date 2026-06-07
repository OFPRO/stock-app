package com.app2.core.data.local.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.app2.core.data.local.dao.CustomerDao;
import com.app2.core.data.local.dao.CustomerDao_Impl;
import com.app2.core.data.local.dao.InvoiceDao;
import com.app2.core.data.local.dao.InvoiceDao_Impl;
import com.app2.core.data.local.dao.NotificationDao;
import com.app2.core.data.local.dao.NotificationDao_Impl;
import com.app2.core.data.local.dao.POSSessionDao;
import com.app2.core.data.local.dao.POSSessionDao_Impl;
import com.app2.core.data.local.dao.ProductDao;
import com.app2.core.data.local.dao.ProductDao_Impl;
import com.app2.core.data.local.dao.SupplierDao;
import com.app2.core.data.local.dao.SupplierDao_Impl;
import com.app2.core.data.local.dao.WarehouseDao;
import com.app2.core.data.local.dao.WarehouseDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile ProductDao _productDao;

  private volatile CustomerDao _customerDao;

  private volatile SupplierDao _supplierDao;

  private volatile WarehouseDao _warehouseDao;

  private volatile InvoiceDao _invoiceDao;

  private volatile NotificationDao _notificationDao;

  private volatile POSSessionDao _pOSSessionDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `products` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `sku` TEXT, `barcode` TEXT, `quantity` INTEGER NOT NULL, `min_quantity` INTEGER NOT NULL, `max_quantity` INTEGER NOT NULL, `price` REAL NOT NULL, `price_base` REAL, `price_loyal` REAL, `price_school` REAL, `price_student` REAL, `category` TEXT, `warehouse_id` INTEGER, `location_id` INTEGER, `is_deleted` INTEGER NOT NULL, `purchase_price_avg` REAL, `discount_category` TEXT, `margin_percent` REAL, `is_liquidation` INTEGER NOT NULL, `extra_prices` TEXT, `created_at` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `customers` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `email` TEXT, `phone` TEXT, `address` TEXT, `client_code` TEXT, `discount_rate` REAL NOT NULL, `is_loyal` INTEGER NOT NULL, `is_active` INTEGER NOT NULL, `ice` TEXT, `notes` TEXT, `created_at` TEXT, `updated_at` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `suppliers` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `email` TEXT, `phone` TEXT, `address` TEXT, `contact_person` TEXT, `created_at` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `warehouses` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `address` TEXT, `manager` TEXT, `phone` TEXT, `ice` TEXT, `patente` TEXT, `rc` TEXT, `taxe_number` TEXT, `is_default` INTEGER NOT NULL, `created_at` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `locations` (`id` INTEGER NOT NULL, `warehouse_id` INTEGER NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `capacity` INTEGER, `created_at` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`warehouse_id`) REFERENCES `warehouses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_locations_warehouse_id` ON `locations` (`warehouse_id`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `invoices` (`id` INTEGER NOT NULL, `invoice_number` TEXT NOT NULL, `customer_id` INTEGER, `warehouse_id` INTEGER, `status` TEXT NOT NULL, `subtotal` REAL NOT NULL, `discount_total` REAL NOT NULL, `tax_amount` REAL NOT NULL, `total` REAL NOT NULL, `notes` TEXT, `due_date` TEXT, `paid_at` TEXT, `type` TEXT, `payment_method` TEXT, `created_at` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `invoice_items` (`id` INTEGER NOT NULL, `invoice_id` INTEGER NOT NULL, `product_id` INTEGER, `product_name` TEXT, `quantity` INTEGER NOT NULL, `unit_price` REAL NOT NULL, `discount_percent` REAL NOT NULL, `tax_rate` REAL NOT NULL, `line_total` REAL NOT NULL, `created_at` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`invoice_id`) REFERENCES `invoices`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_items_invoice_id` ON `invoice_items` (`invoice_id`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `stock_movements` (`id` INTEGER NOT NULL, `product_id` INTEGER, `type` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `source_location_id` INTEGER, `dest_location_id` INTEGER, `note` TEXT, `created_at` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `purchase_orders` (`id` INTEGER NOT NULL, `order_number` TEXT, `supplier_id` INTEGER, `warehouse_id` INTEGER, `status` TEXT NOT NULL, `total` REAL NOT NULL, `notes` TEXT, `sent_at` TEXT, `received_at` TEXT, `created_at` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `reordering_rules` (`id` INTEGER NOT NULL, `product_id` INTEGER NOT NULL, `warehouse_id` INTEGER, `min_quantity` INTEGER NOT NULL, `max_quantity` INTEGER NOT NULL, `trigger_type` TEXT, `supplier_id` INTEGER, `created_at` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `notifications` (`id` INTEGER NOT NULL, `type` TEXT NOT NULL, `title` TEXT NOT NULL, `message` TEXT, `product_id` INTEGER, `warehouse_id` INTEGER, `is_read` INTEGER NOT NULL, `created_at` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `pos_sessions` (`id` INTEGER NOT NULL, `session_number` TEXT, `warehouse_id` INTEGER, `user_name` TEXT, `opening_cash` REAL NOT NULL, `closing_cash` REAL, `expected_cash` REAL, `status` TEXT NOT NULL, `opened_at` TEXT, `closed_at` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `pos_transactions` (`id` INTEGER NOT NULL, `ticket_number` TEXT, `session_id` INTEGER NOT NULL, `customer_id` INTEGER, `payment_method` TEXT NOT NULL, `subtotal` REAL NOT NULL, `tax_amount` REAL NOT NULL, `discount_amount` REAL NOT NULL, `total` REAL NOT NULL, `tendered_amount` REAL NOT NULL, `change_amount` REAL NOT NULL, `status` TEXT, `invoice_id` INTEGER, `created_at` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '9b25db9a85dd37a31ff5eaad1cae2dae')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `products`");
        db.execSQL("DROP TABLE IF EXISTS `customers`");
        db.execSQL("DROP TABLE IF EXISTS `suppliers`");
        db.execSQL("DROP TABLE IF EXISTS `warehouses`");
        db.execSQL("DROP TABLE IF EXISTS `locations`");
        db.execSQL("DROP TABLE IF EXISTS `invoices`");
        db.execSQL("DROP TABLE IF EXISTS `invoice_items`");
        db.execSQL("DROP TABLE IF EXISTS `stock_movements`");
        db.execSQL("DROP TABLE IF EXISTS `purchase_orders`");
        db.execSQL("DROP TABLE IF EXISTS `reordering_rules`");
        db.execSQL("DROP TABLE IF EXISTS `notifications`");
        db.execSQL("DROP TABLE IF EXISTS `pos_sessions`");
        db.execSQL("DROP TABLE IF EXISTS `pos_transactions`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsProducts = new HashMap<String, TableInfo.Column>(23);
        _columnsProducts.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("sku", new TableInfo.Column("sku", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("barcode", new TableInfo.Column("barcode", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("quantity", new TableInfo.Column("quantity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("min_quantity", new TableInfo.Column("min_quantity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("max_quantity", new TableInfo.Column("max_quantity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("price", new TableInfo.Column("price", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("price_base", new TableInfo.Column("price_base", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("price_loyal", new TableInfo.Column("price_loyal", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("price_school", new TableInfo.Column("price_school", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("price_student", new TableInfo.Column("price_student", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("category", new TableInfo.Column("category", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("warehouse_id", new TableInfo.Column("warehouse_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("location_id", new TableInfo.Column("location_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("is_deleted", new TableInfo.Column("is_deleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("purchase_price_avg", new TableInfo.Column("purchase_price_avg", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("discount_category", new TableInfo.Column("discount_category", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("margin_percent", new TableInfo.Column("margin_percent", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("is_liquidation", new TableInfo.Column("is_liquidation", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("extra_prices", new TableInfo.Column("extra_prices", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProducts.put("created_at", new TableInfo.Column("created_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysProducts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesProducts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoProducts = new TableInfo("products", _columnsProducts, _foreignKeysProducts, _indicesProducts);
        final TableInfo _existingProducts = TableInfo.read(db, "products");
        if (!_infoProducts.equals(_existingProducts)) {
          return new RoomOpenHelper.ValidationResult(false, "products(com.app2.core.data.local.entity.ProductEntity).\n"
                  + " Expected:\n" + _infoProducts + "\n"
                  + " Found:\n" + _existingProducts);
        }
        final HashMap<String, TableInfo.Column> _columnsCustomers = new HashMap<String, TableInfo.Column>(14);
        _columnsCustomers.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("email", new TableInfo.Column("email", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("phone", new TableInfo.Column("phone", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("address", new TableInfo.Column("address", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("client_code", new TableInfo.Column("client_code", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("discount_rate", new TableInfo.Column("discount_rate", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("is_loyal", new TableInfo.Column("is_loyal", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("is_active", new TableInfo.Column("is_active", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("ice", new TableInfo.Column("ice", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("created_at", new TableInfo.Column("created_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("updated_at", new TableInfo.Column("updated_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCustomers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCustomers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCustomers = new TableInfo("customers", _columnsCustomers, _foreignKeysCustomers, _indicesCustomers);
        final TableInfo _existingCustomers = TableInfo.read(db, "customers");
        if (!_infoCustomers.equals(_existingCustomers)) {
          return new RoomOpenHelper.ValidationResult(false, "customers(com.app2.core.data.local.entity.CustomerEntity).\n"
                  + " Expected:\n" + _infoCustomers + "\n"
                  + " Found:\n" + _existingCustomers);
        }
        final HashMap<String, TableInfo.Column> _columnsSuppliers = new HashMap<String, TableInfo.Column>(7);
        _columnsSuppliers.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuppliers.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuppliers.put("email", new TableInfo.Column("email", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuppliers.put("phone", new TableInfo.Column("phone", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuppliers.put("address", new TableInfo.Column("address", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuppliers.put("contact_person", new TableInfo.Column("contact_person", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuppliers.put("created_at", new TableInfo.Column("created_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSuppliers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSuppliers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSuppliers = new TableInfo("suppliers", _columnsSuppliers, _foreignKeysSuppliers, _indicesSuppliers);
        final TableInfo _existingSuppliers = TableInfo.read(db, "suppliers");
        if (!_infoSuppliers.equals(_existingSuppliers)) {
          return new RoomOpenHelper.ValidationResult(false, "suppliers(com.app2.core.data.local.entity.SupplierEntity).\n"
                  + " Expected:\n" + _infoSuppliers + "\n"
                  + " Found:\n" + _existingSuppliers);
        }
        final HashMap<String, TableInfo.Column> _columnsWarehouses = new HashMap<String, TableInfo.Column>(11);
        _columnsWarehouses.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWarehouses.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWarehouses.put("address", new TableInfo.Column("address", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWarehouses.put("manager", new TableInfo.Column("manager", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWarehouses.put("phone", new TableInfo.Column("phone", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWarehouses.put("ice", new TableInfo.Column("ice", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWarehouses.put("patente", new TableInfo.Column("patente", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWarehouses.put("rc", new TableInfo.Column("rc", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWarehouses.put("taxe_number", new TableInfo.Column("taxe_number", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWarehouses.put("is_default", new TableInfo.Column("is_default", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWarehouses.put("created_at", new TableInfo.Column("created_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWarehouses = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWarehouses = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoWarehouses = new TableInfo("warehouses", _columnsWarehouses, _foreignKeysWarehouses, _indicesWarehouses);
        final TableInfo _existingWarehouses = TableInfo.read(db, "warehouses");
        if (!_infoWarehouses.equals(_existingWarehouses)) {
          return new RoomOpenHelper.ValidationResult(false, "warehouses(com.app2.core.data.local.entity.WarehouseEntity).\n"
                  + " Expected:\n" + _infoWarehouses + "\n"
                  + " Found:\n" + _existingWarehouses);
        }
        final HashMap<String, TableInfo.Column> _columnsLocations = new HashMap<String, TableInfo.Column>(6);
        _columnsLocations.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("warehouse_id", new TableInfo.Column("warehouse_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("capacity", new TableInfo.Column("capacity", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("created_at", new TableInfo.Column("created_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLocations = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysLocations.add(new TableInfo.ForeignKey("warehouses", "CASCADE", "NO ACTION", Arrays.asList("warehouse_id"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesLocations = new HashSet<TableInfo.Index>(1);
        _indicesLocations.add(new TableInfo.Index("index_locations_warehouse_id", false, Arrays.asList("warehouse_id"), Arrays.asList("ASC")));
        final TableInfo _infoLocations = new TableInfo("locations", _columnsLocations, _foreignKeysLocations, _indicesLocations);
        final TableInfo _existingLocations = TableInfo.read(db, "locations");
        if (!_infoLocations.equals(_existingLocations)) {
          return new RoomOpenHelper.ValidationResult(false, "locations(com.app2.core.data.local.entity.LocationEntity).\n"
                  + " Expected:\n" + _infoLocations + "\n"
                  + " Found:\n" + _existingLocations);
        }
        final HashMap<String, TableInfo.Column> _columnsInvoices = new HashMap<String, TableInfo.Column>(15);
        _columnsInvoices.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoices.put("invoice_number", new TableInfo.Column("invoice_number", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoices.put("customer_id", new TableInfo.Column("customer_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoices.put("warehouse_id", new TableInfo.Column("warehouse_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoices.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoices.put("subtotal", new TableInfo.Column("subtotal", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoices.put("discount_total", new TableInfo.Column("discount_total", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoices.put("tax_amount", new TableInfo.Column("tax_amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoices.put("total", new TableInfo.Column("total", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoices.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoices.put("due_date", new TableInfo.Column("due_date", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoices.put("paid_at", new TableInfo.Column("paid_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoices.put("type", new TableInfo.Column("type", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoices.put("payment_method", new TableInfo.Column("payment_method", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoices.put("created_at", new TableInfo.Column("created_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysInvoices = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesInvoices = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoInvoices = new TableInfo("invoices", _columnsInvoices, _foreignKeysInvoices, _indicesInvoices);
        final TableInfo _existingInvoices = TableInfo.read(db, "invoices");
        if (!_infoInvoices.equals(_existingInvoices)) {
          return new RoomOpenHelper.ValidationResult(false, "invoices(com.app2.core.data.local.entity.InvoiceEntity).\n"
                  + " Expected:\n" + _infoInvoices + "\n"
                  + " Found:\n" + _existingInvoices);
        }
        final HashMap<String, TableInfo.Column> _columnsInvoiceItems = new HashMap<String, TableInfo.Column>(10);
        _columnsInvoiceItems.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoiceItems.put("invoice_id", new TableInfo.Column("invoice_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoiceItems.put("product_id", new TableInfo.Column("product_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoiceItems.put("product_name", new TableInfo.Column("product_name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoiceItems.put("quantity", new TableInfo.Column("quantity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoiceItems.put("unit_price", new TableInfo.Column("unit_price", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoiceItems.put("discount_percent", new TableInfo.Column("discount_percent", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoiceItems.put("tax_rate", new TableInfo.Column("tax_rate", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoiceItems.put("line_total", new TableInfo.Column("line_total", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInvoiceItems.put("created_at", new TableInfo.Column("created_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysInvoiceItems = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysInvoiceItems.add(new TableInfo.ForeignKey("invoices", "CASCADE", "NO ACTION", Arrays.asList("invoice_id"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesInvoiceItems = new HashSet<TableInfo.Index>(1);
        _indicesInvoiceItems.add(new TableInfo.Index("index_invoice_items_invoice_id", false, Arrays.asList("invoice_id"), Arrays.asList("ASC")));
        final TableInfo _infoInvoiceItems = new TableInfo("invoice_items", _columnsInvoiceItems, _foreignKeysInvoiceItems, _indicesInvoiceItems);
        final TableInfo _existingInvoiceItems = TableInfo.read(db, "invoice_items");
        if (!_infoInvoiceItems.equals(_existingInvoiceItems)) {
          return new RoomOpenHelper.ValidationResult(false, "invoice_items(com.app2.core.data.local.entity.InvoiceItemEntity).\n"
                  + " Expected:\n" + _infoInvoiceItems + "\n"
                  + " Found:\n" + _existingInvoiceItems);
        }
        final HashMap<String, TableInfo.Column> _columnsStockMovements = new HashMap<String, TableInfo.Column>(8);
        _columnsStockMovements.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStockMovements.put("product_id", new TableInfo.Column("product_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStockMovements.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStockMovements.put("quantity", new TableInfo.Column("quantity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStockMovements.put("source_location_id", new TableInfo.Column("source_location_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStockMovements.put("dest_location_id", new TableInfo.Column("dest_location_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStockMovements.put("note", new TableInfo.Column("note", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStockMovements.put("created_at", new TableInfo.Column("created_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysStockMovements = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesStockMovements = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoStockMovements = new TableInfo("stock_movements", _columnsStockMovements, _foreignKeysStockMovements, _indicesStockMovements);
        final TableInfo _existingStockMovements = TableInfo.read(db, "stock_movements");
        if (!_infoStockMovements.equals(_existingStockMovements)) {
          return new RoomOpenHelper.ValidationResult(false, "stock_movements(com.app2.core.data.local.entity.StockMovementEntity).\n"
                  + " Expected:\n" + _infoStockMovements + "\n"
                  + " Found:\n" + _existingStockMovements);
        }
        final HashMap<String, TableInfo.Column> _columnsPurchaseOrders = new HashMap<String, TableInfo.Column>(10);
        _columnsPurchaseOrders.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPurchaseOrders.put("order_number", new TableInfo.Column("order_number", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPurchaseOrders.put("supplier_id", new TableInfo.Column("supplier_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPurchaseOrders.put("warehouse_id", new TableInfo.Column("warehouse_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPurchaseOrders.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPurchaseOrders.put("total", new TableInfo.Column("total", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPurchaseOrders.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPurchaseOrders.put("sent_at", new TableInfo.Column("sent_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPurchaseOrders.put("received_at", new TableInfo.Column("received_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPurchaseOrders.put("created_at", new TableInfo.Column("created_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPurchaseOrders = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPurchaseOrders = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPurchaseOrders = new TableInfo("purchase_orders", _columnsPurchaseOrders, _foreignKeysPurchaseOrders, _indicesPurchaseOrders);
        final TableInfo _existingPurchaseOrders = TableInfo.read(db, "purchase_orders");
        if (!_infoPurchaseOrders.equals(_existingPurchaseOrders)) {
          return new RoomOpenHelper.ValidationResult(false, "purchase_orders(com.app2.core.data.local.entity.PurchaseOrderEntity).\n"
                  + " Expected:\n" + _infoPurchaseOrders + "\n"
                  + " Found:\n" + _existingPurchaseOrders);
        }
        final HashMap<String, TableInfo.Column> _columnsReorderingRules = new HashMap<String, TableInfo.Column>(8);
        _columnsReorderingRules.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReorderingRules.put("product_id", new TableInfo.Column("product_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReorderingRules.put("warehouse_id", new TableInfo.Column("warehouse_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReorderingRules.put("min_quantity", new TableInfo.Column("min_quantity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReorderingRules.put("max_quantity", new TableInfo.Column("max_quantity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReorderingRules.put("trigger_type", new TableInfo.Column("trigger_type", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReorderingRules.put("supplier_id", new TableInfo.Column("supplier_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReorderingRules.put("created_at", new TableInfo.Column("created_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysReorderingRules = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesReorderingRules = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoReorderingRules = new TableInfo("reordering_rules", _columnsReorderingRules, _foreignKeysReorderingRules, _indicesReorderingRules);
        final TableInfo _existingReorderingRules = TableInfo.read(db, "reordering_rules");
        if (!_infoReorderingRules.equals(_existingReorderingRules)) {
          return new RoomOpenHelper.ValidationResult(false, "reordering_rules(com.app2.core.data.local.entity.ReorderRuleEntity).\n"
                  + " Expected:\n" + _infoReorderingRules + "\n"
                  + " Found:\n" + _existingReorderingRules);
        }
        final HashMap<String, TableInfo.Column> _columnsNotifications = new HashMap<String, TableInfo.Column>(8);
        _columnsNotifications.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotifications.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotifications.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotifications.put("message", new TableInfo.Column("message", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotifications.put("product_id", new TableInfo.Column("product_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotifications.put("warehouse_id", new TableInfo.Column("warehouse_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotifications.put("is_read", new TableInfo.Column("is_read", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotifications.put("created_at", new TableInfo.Column("created_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysNotifications = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesNotifications = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoNotifications = new TableInfo("notifications", _columnsNotifications, _foreignKeysNotifications, _indicesNotifications);
        final TableInfo _existingNotifications = TableInfo.read(db, "notifications");
        if (!_infoNotifications.equals(_existingNotifications)) {
          return new RoomOpenHelper.ValidationResult(false, "notifications(com.app2.core.data.local.entity.NotificationEntity).\n"
                  + " Expected:\n" + _infoNotifications + "\n"
                  + " Found:\n" + _existingNotifications);
        }
        final HashMap<String, TableInfo.Column> _columnsPosSessions = new HashMap<String, TableInfo.Column>(10);
        _columnsPosSessions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosSessions.put("session_number", new TableInfo.Column("session_number", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosSessions.put("warehouse_id", new TableInfo.Column("warehouse_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosSessions.put("user_name", new TableInfo.Column("user_name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosSessions.put("opening_cash", new TableInfo.Column("opening_cash", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosSessions.put("closing_cash", new TableInfo.Column("closing_cash", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosSessions.put("expected_cash", new TableInfo.Column("expected_cash", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosSessions.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosSessions.put("opened_at", new TableInfo.Column("opened_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosSessions.put("closed_at", new TableInfo.Column("closed_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPosSessions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPosSessions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPosSessions = new TableInfo("pos_sessions", _columnsPosSessions, _foreignKeysPosSessions, _indicesPosSessions);
        final TableInfo _existingPosSessions = TableInfo.read(db, "pos_sessions");
        if (!_infoPosSessions.equals(_existingPosSessions)) {
          return new RoomOpenHelper.ValidationResult(false, "pos_sessions(com.app2.core.data.local.entity.POSSessionEntity).\n"
                  + " Expected:\n" + _infoPosSessions + "\n"
                  + " Found:\n" + _existingPosSessions);
        }
        final HashMap<String, TableInfo.Column> _columnsPosTransactions = new HashMap<String, TableInfo.Column>(14);
        _columnsPosTransactions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosTransactions.put("ticket_number", new TableInfo.Column("ticket_number", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosTransactions.put("session_id", new TableInfo.Column("session_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosTransactions.put("customer_id", new TableInfo.Column("customer_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosTransactions.put("payment_method", new TableInfo.Column("payment_method", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosTransactions.put("subtotal", new TableInfo.Column("subtotal", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosTransactions.put("tax_amount", new TableInfo.Column("tax_amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosTransactions.put("discount_amount", new TableInfo.Column("discount_amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosTransactions.put("total", new TableInfo.Column("total", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosTransactions.put("tendered_amount", new TableInfo.Column("tendered_amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosTransactions.put("change_amount", new TableInfo.Column("change_amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosTransactions.put("status", new TableInfo.Column("status", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosTransactions.put("invoice_id", new TableInfo.Column("invoice_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosTransactions.put("created_at", new TableInfo.Column("created_at", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPosTransactions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPosTransactions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPosTransactions = new TableInfo("pos_transactions", _columnsPosTransactions, _foreignKeysPosTransactions, _indicesPosTransactions);
        final TableInfo _existingPosTransactions = TableInfo.read(db, "pos_transactions");
        if (!_infoPosTransactions.equals(_existingPosTransactions)) {
          return new RoomOpenHelper.ValidationResult(false, "pos_transactions(com.app2.core.data.local.entity.POSTransactionEntity).\n"
                  + " Expected:\n" + _infoPosTransactions + "\n"
                  + " Found:\n" + _existingPosTransactions);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "9b25db9a85dd37a31ff5eaad1cae2dae", "adceef1e5dbba0822c912eebf559c3db");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "products","customers","suppliers","warehouses","locations","invoices","invoice_items","stock_movements","purchase_orders","reordering_rules","notifications","pos_sessions","pos_transactions");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `products`");
      _db.execSQL("DELETE FROM `customers`");
      _db.execSQL("DELETE FROM `suppliers`");
      _db.execSQL("DELETE FROM `warehouses`");
      _db.execSQL("DELETE FROM `locations`");
      _db.execSQL("DELETE FROM `invoices`");
      _db.execSQL("DELETE FROM `invoice_items`");
      _db.execSQL("DELETE FROM `stock_movements`");
      _db.execSQL("DELETE FROM `purchase_orders`");
      _db.execSQL("DELETE FROM `reordering_rules`");
      _db.execSQL("DELETE FROM `notifications`");
      _db.execSQL("DELETE FROM `pos_sessions`");
      _db.execSQL("DELETE FROM `pos_transactions`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(ProductDao.class, ProductDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CustomerDao.class, CustomerDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SupplierDao.class, SupplierDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(WarehouseDao.class, WarehouseDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(InvoiceDao.class, InvoiceDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(NotificationDao.class, NotificationDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(POSSessionDao.class, POSSessionDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public ProductDao productDao() {
    if (_productDao != null) {
      return _productDao;
    } else {
      synchronized(this) {
        if(_productDao == null) {
          _productDao = new ProductDao_Impl(this);
        }
        return _productDao;
      }
    }
  }

  @Override
  public CustomerDao customerDao() {
    if (_customerDao != null) {
      return _customerDao;
    } else {
      synchronized(this) {
        if(_customerDao == null) {
          _customerDao = new CustomerDao_Impl(this);
        }
        return _customerDao;
      }
    }
  }

  @Override
  public SupplierDao supplierDao() {
    if (_supplierDao != null) {
      return _supplierDao;
    } else {
      synchronized(this) {
        if(_supplierDao == null) {
          _supplierDao = new SupplierDao_Impl(this);
        }
        return _supplierDao;
      }
    }
  }

  @Override
  public WarehouseDao warehouseDao() {
    if (_warehouseDao != null) {
      return _warehouseDao;
    } else {
      synchronized(this) {
        if(_warehouseDao == null) {
          _warehouseDao = new WarehouseDao_Impl(this);
        }
        return _warehouseDao;
      }
    }
  }

  @Override
  public InvoiceDao invoiceDao() {
    if (_invoiceDao != null) {
      return _invoiceDao;
    } else {
      synchronized(this) {
        if(_invoiceDao == null) {
          _invoiceDao = new InvoiceDao_Impl(this);
        }
        return _invoiceDao;
      }
    }
  }

  @Override
  public NotificationDao notificationDao() {
    if (_notificationDao != null) {
      return _notificationDao;
    } else {
      synchronized(this) {
        if(_notificationDao == null) {
          _notificationDao = new NotificationDao_Impl(this);
        }
        return _notificationDao;
      }
    }
  }

  @Override
  public POSSessionDao posSessionDao() {
    if (_pOSSessionDao != null) {
      return _pOSSessionDao;
    } else {
      synchronized(this) {
        if(_pOSSessionDao == null) {
          _pOSSessionDao = new POSSessionDao_Impl(this);
        }
        return _pOSSessionDao;
      }
    }
  }
}
