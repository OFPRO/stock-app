package com.app2;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.app2.core.data.di.NetworkModule_ProvideAdminApiFactory;
import com.app2.core.data.di.NetworkModule_ProvideCustomerApiFactory;
import com.app2.core.data.di.NetworkModule_ProvideInvoiceApiFactory;
import com.app2.core.data.di.NetworkModule_ProvideJsonFactory;
import com.app2.core.data.di.NetworkModule_ProvideKPIApiFactory;
import com.app2.core.data.di.NetworkModule_ProvideLocationApiFactory;
import com.app2.core.data.di.NetworkModule_ProvideMovementApiFactory;
import com.app2.core.data.di.NetworkModule_ProvideNotificationApiFactory;
import com.app2.core.data.di.NetworkModule_ProvideOkHttpClientFactory;
import com.app2.core.data.di.NetworkModule_ProvideOrderApiFactory;
import com.app2.core.data.di.NetworkModule_ProvidePOSApiFactory;
import com.app2.core.data.di.NetworkModule_ProvideProductApiFactory;
import com.app2.core.data.di.NetworkModule_ProvideRetrofitFactory;
import com.app2.core.data.di.NetworkModule_ProvideSupplierApiFactory;
import com.app2.core.data.di.NetworkModule_ProvideWarehouseApiFactory;
import com.app2.core.data.remote.AdminApiService;
import com.app2.core.data.remote.CustomerApiService;
import com.app2.core.data.remote.InvoiceApiService;
import com.app2.core.data.remote.KPIApiService;
import com.app2.core.data.remote.LocationApiService;
import com.app2.core.data.remote.MovementApiService;
import com.app2.core.data.remote.NotificationApiService;
import com.app2.core.data.remote.OrderApiService;
import com.app2.core.data.remote.POSApiService;
import com.app2.core.data.remote.ProductApiService;
import com.app2.core.data.remote.SupplierApiService;
import com.app2.core.data.remote.WarehouseApiService;
import com.app2.feature.auth.PinManager;
import com.app2.feature.auth.PinViewModel;
import com.app2.feature.auth.PinViewModel_HiltModules;
import com.app2.feature.customers.CustomerDetailViewModel;
import com.app2.feature.customers.CustomerDetailViewModel_HiltModules;
import com.app2.feature.customers.CustomerFormViewModel;
import com.app2.feature.customers.CustomerFormViewModel_HiltModules;
import com.app2.feature.customers.CustomersViewModel;
import com.app2.feature.customers.CustomersViewModel_HiltModules;
import com.app2.feature.dashboard.DashboardViewModel;
import com.app2.feature.dashboard.DashboardViewModel_HiltModules;
import com.app2.feature.invoices.InvoiceDetailViewModel;
import com.app2.feature.invoices.InvoiceDetailViewModel_HiltModules;
import com.app2.feature.invoices.InvoiceFormViewModel;
import com.app2.feature.invoices.InvoiceFormViewModel_HiltModules;
import com.app2.feature.invoices.InvoicesViewModel;
import com.app2.feature.invoices.InvoicesViewModel_HiltModules;
import com.app2.feature.notifications.NotificationsViewModel;
import com.app2.feature.notifications.NotificationsViewModel_HiltModules;
import com.app2.feature.orders.OrderDetailViewModel;
import com.app2.feature.orders.OrderDetailViewModel_HiltModules;
import com.app2.feature.orders.OrderFormViewModel;
import com.app2.feature.orders.OrderFormViewModel_HiltModules;
import com.app2.feature.orders.OrdersViewModel;
import com.app2.feature.orders.OrdersViewModel_HiltModules;
import com.app2.feature.pos.POSRegisterViewModel;
import com.app2.feature.pos.POSRegisterViewModel_HiltModules;
import com.app2.feature.pos.POSSessionViewModel;
import com.app2.feature.pos.POSSessionViewModel_HiltModules;
import com.app2.feature.products.ProductDetailViewModel;
import com.app2.feature.products.ProductDetailViewModel_HiltModules;
import com.app2.feature.products.ProductsViewModel;
import com.app2.feature.products.ProductsViewModel_HiltModules;
import com.app2.feature.settings.SettingsViewModel;
import com.app2.feature.settings.SettingsViewModel_HiltModules;
import com.app2.feature.suppliers.SupplierDetailViewModel;
import com.app2.feature.suppliers.SupplierDetailViewModel_HiltModules;
import com.app2.feature.suppliers.SupplierFormViewModel;
import com.app2.feature.suppliers.SupplierFormViewModel_HiltModules;
import com.app2.feature.suppliers.SuppliersViewModel;
import com.app2.feature.suppliers.SuppliersViewModel_HiltModules;
import com.app2.feature.warehouses.CreateMovementViewModel;
import com.app2.feature.warehouses.CreateMovementViewModel_HiltModules;
import com.app2.feature.warehouses.LocationFormViewModel;
import com.app2.feature.warehouses.LocationFormViewModel_HiltModules;
import com.app2.feature.warehouses.LocationsViewModel;
import com.app2.feature.warehouses.LocationsViewModel_HiltModules;
import com.app2.feature.warehouses.MovementListViewModel;
import com.app2.feature.warehouses.MovementListViewModel_HiltModules;
import com.app2.feature.warehouses.WarehouseFormViewModel;
import com.app2.feature.warehouses.WarehouseFormViewModel_HiltModules;
import com.app2.feature.warehouses.WarehousesViewModel;
import com.app2.feature.warehouses.WarehousesViewModel_HiltModules;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class DaggerApp_HiltComponents_SingletonC {
  private DaggerApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public App_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements App_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public App_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements App_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public App_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements App_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public App_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements App_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public App_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements App_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public App_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements App_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public App_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements App_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public App_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends App_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends App_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends App_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends App_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity arg0) {
      injectMainActivity2(arg0);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(26).put(LazyClassKeyProvider.com_app2_feature_warehouses_CreateMovementViewModel, CreateMovementViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_customers_CustomerDetailViewModel, CustomerDetailViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_customers_CustomerFormViewModel, CustomerFormViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_customers_CustomersViewModel, CustomersViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_dashboard_DashboardViewModel, DashboardViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_invoices_InvoiceDetailViewModel, InvoiceDetailViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_invoices_InvoiceFormViewModel, InvoiceFormViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_invoices_InvoicesViewModel, InvoicesViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_warehouses_LocationFormViewModel, LocationFormViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_warehouses_LocationsViewModel, LocationsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_warehouses_MovementListViewModel, MovementListViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_notifications_NotificationsViewModel, NotificationsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_orders_OrderDetailViewModel, OrderDetailViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_orders_OrderFormViewModel, OrderFormViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_orders_OrdersViewModel, OrdersViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_pos_POSRegisterViewModel, POSRegisterViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_pos_POSSessionViewModel, POSSessionViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_auth_PinViewModel, PinViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_products_ProductDetailViewModel, ProductDetailViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_products_ProductsViewModel, ProductsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_settings_SettingsViewModel, SettingsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_suppliers_SupplierDetailViewModel, SupplierDetailViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_suppliers_SupplierFormViewModel, SupplierFormViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_suppliers_SuppliersViewModel, SuppliersViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_warehouses_WarehouseFormViewModel, WarehouseFormViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_app2_feature_warehouses_WarehousesViewModel, WarehousesViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    private MainActivity injectMainActivity2(MainActivity instance) {
      MainActivity_MembersInjector.injectPinManager(instance, singletonCImpl.pinManagerProvider.get());
      return instance;
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_app2_feature_notifications_NotificationsViewModel = "com.app2.feature.notifications.NotificationsViewModel";

      static String com_app2_feature_warehouses_CreateMovementViewModel = "com.app2.feature.warehouses.CreateMovementViewModel";

      static String com_app2_feature_invoices_InvoiceDetailViewModel = "com.app2.feature.invoices.InvoiceDetailViewModel";

      static String com_app2_feature_products_ProductsViewModel = "com.app2.feature.products.ProductsViewModel";

      static String com_app2_feature_suppliers_SuppliersViewModel = "com.app2.feature.suppliers.SuppliersViewModel";

      static String com_app2_feature_customers_CustomerFormViewModel = "com.app2.feature.customers.CustomerFormViewModel";

      static String com_app2_feature_suppliers_SupplierDetailViewModel = "com.app2.feature.suppliers.SupplierDetailViewModel";

      static String com_app2_feature_pos_POSRegisterViewModel = "com.app2.feature.pos.POSRegisterViewModel";

      static String com_app2_feature_dashboard_DashboardViewModel = "com.app2.feature.dashboard.DashboardViewModel";

      static String com_app2_feature_warehouses_WarehousesViewModel = "com.app2.feature.warehouses.WarehousesViewModel";

      static String com_app2_feature_orders_OrdersViewModel = "com.app2.feature.orders.OrdersViewModel";

      static String com_app2_feature_warehouses_LocationFormViewModel = "com.app2.feature.warehouses.LocationFormViewModel";

      static String com_app2_feature_products_ProductDetailViewModel = "com.app2.feature.products.ProductDetailViewModel";

      static String com_app2_feature_auth_PinViewModel = "com.app2.feature.auth.PinViewModel";

      static String com_app2_feature_orders_OrderDetailViewModel = "com.app2.feature.orders.OrderDetailViewModel";

      static String com_app2_feature_pos_POSSessionViewModel = "com.app2.feature.pos.POSSessionViewModel";

      static String com_app2_feature_suppliers_SupplierFormViewModel = "com.app2.feature.suppliers.SupplierFormViewModel";

      static String com_app2_feature_settings_SettingsViewModel = "com.app2.feature.settings.SettingsViewModel";

      static String com_app2_feature_warehouses_MovementListViewModel = "com.app2.feature.warehouses.MovementListViewModel";

      static String com_app2_feature_invoices_InvoiceFormViewModel = "com.app2.feature.invoices.InvoiceFormViewModel";

      static String com_app2_feature_warehouses_WarehouseFormViewModel = "com.app2.feature.warehouses.WarehouseFormViewModel";

      static String com_app2_feature_invoices_InvoicesViewModel = "com.app2.feature.invoices.InvoicesViewModel";

      static String com_app2_feature_orders_OrderFormViewModel = "com.app2.feature.orders.OrderFormViewModel";

      static String com_app2_feature_customers_CustomersViewModel = "com.app2.feature.customers.CustomersViewModel";

      static String com_app2_feature_warehouses_LocationsViewModel = "com.app2.feature.warehouses.LocationsViewModel";

      static String com_app2_feature_customers_CustomerDetailViewModel = "com.app2.feature.customers.CustomerDetailViewModel";

      @KeepFieldType
      NotificationsViewModel com_app2_feature_notifications_NotificationsViewModel2;

      @KeepFieldType
      CreateMovementViewModel com_app2_feature_warehouses_CreateMovementViewModel2;

      @KeepFieldType
      InvoiceDetailViewModel com_app2_feature_invoices_InvoiceDetailViewModel2;

      @KeepFieldType
      ProductsViewModel com_app2_feature_products_ProductsViewModel2;

      @KeepFieldType
      SuppliersViewModel com_app2_feature_suppliers_SuppliersViewModel2;

      @KeepFieldType
      CustomerFormViewModel com_app2_feature_customers_CustomerFormViewModel2;

      @KeepFieldType
      SupplierDetailViewModel com_app2_feature_suppliers_SupplierDetailViewModel2;

      @KeepFieldType
      POSRegisterViewModel com_app2_feature_pos_POSRegisterViewModel2;

      @KeepFieldType
      DashboardViewModel com_app2_feature_dashboard_DashboardViewModel2;

      @KeepFieldType
      WarehousesViewModel com_app2_feature_warehouses_WarehousesViewModel2;

      @KeepFieldType
      OrdersViewModel com_app2_feature_orders_OrdersViewModel2;

      @KeepFieldType
      LocationFormViewModel com_app2_feature_warehouses_LocationFormViewModel2;

      @KeepFieldType
      ProductDetailViewModel com_app2_feature_products_ProductDetailViewModel2;

      @KeepFieldType
      PinViewModel com_app2_feature_auth_PinViewModel2;

      @KeepFieldType
      OrderDetailViewModel com_app2_feature_orders_OrderDetailViewModel2;

      @KeepFieldType
      POSSessionViewModel com_app2_feature_pos_POSSessionViewModel2;

      @KeepFieldType
      SupplierFormViewModel com_app2_feature_suppliers_SupplierFormViewModel2;

      @KeepFieldType
      SettingsViewModel com_app2_feature_settings_SettingsViewModel2;

      @KeepFieldType
      MovementListViewModel com_app2_feature_warehouses_MovementListViewModel2;

      @KeepFieldType
      InvoiceFormViewModel com_app2_feature_invoices_InvoiceFormViewModel2;

      @KeepFieldType
      WarehouseFormViewModel com_app2_feature_warehouses_WarehouseFormViewModel2;

      @KeepFieldType
      InvoicesViewModel com_app2_feature_invoices_InvoicesViewModel2;

      @KeepFieldType
      OrderFormViewModel com_app2_feature_orders_OrderFormViewModel2;

      @KeepFieldType
      CustomersViewModel com_app2_feature_customers_CustomersViewModel2;

      @KeepFieldType
      LocationsViewModel com_app2_feature_warehouses_LocationsViewModel2;

      @KeepFieldType
      CustomerDetailViewModel com_app2_feature_customers_CustomerDetailViewModel2;
    }
  }

  private static final class ViewModelCImpl extends App_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<CreateMovementViewModel> createMovementViewModelProvider;

    private Provider<CustomerDetailViewModel> customerDetailViewModelProvider;

    private Provider<CustomerFormViewModel> customerFormViewModelProvider;

    private Provider<CustomersViewModel> customersViewModelProvider;

    private Provider<DashboardViewModel> dashboardViewModelProvider;

    private Provider<InvoiceDetailViewModel> invoiceDetailViewModelProvider;

    private Provider<InvoiceFormViewModel> invoiceFormViewModelProvider;

    private Provider<InvoicesViewModel> invoicesViewModelProvider;

    private Provider<LocationFormViewModel> locationFormViewModelProvider;

    private Provider<LocationsViewModel> locationsViewModelProvider;

    private Provider<MovementListViewModel> movementListViewModelProvider;

    private Provider<NotificationsViewModel> notificationsViewModelProvider;

    private Provider<OrderDetailViewModel> orderDetailViewModelProvider;

    private Provider<OrderFormViewModel> orderFormViewModelProvider;

    private Provider<OrdersViewModel> ordersViewModelProvider;

    private Provider<POSRegisterViewModel> pOSRegisterViewModelProvider;

    private Provider<POSSessionViewModel> pOSSessionViewModelProvider;

    private Provider<PinViewModel> pinViewModelProvider;

    private Provider<ProductDetailViewModel> productDetailViewModelProvider;

    private Provider<ProductsViewModel> productsViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private Provider<SupplierDetailViewModel> supplierDetailViewModelProvider;

    private Provider<SupplierFormViewModel> supplierFormViewModelProvider;

    private Provider<SuppliersViewModel> suppliersViewModelProvider;

    private Provider<WarehouseFormViewModel> warehouseFormViewModelProvider;

    private Provider<WarehousesViewModel> warehousesViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);
      initialize2(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.createMovementViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.customerDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.customerFormViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.customersViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.dashboardViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.invoiceDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.invoiceFormViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.invoicesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.locationFormViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
      this.locationsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 9);
      this.movementListViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 10);
      this.notificationsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 11);
      this.orderDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 12);
      this.orderFormViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 13);
      this.ordersViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 14);
      this.pOSRegisterViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 15);
      this.pOSSessionViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 16);
      this.pinViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 17);
      this.productDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 18);
      this.productsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 19);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 20);
      this.supplierDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 21);
      this.supplierFormViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 22);
      this.suppliersViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 23);
      this.warehouseFormViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 24);
    }

    @SuppressWarnings("unchecked")
    private void initialize2(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.warehousesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 25);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(26).put(LazyClassKeyProvider.com_app2_feature_warehouses_CreateMovementViewModel, ((Provider) createMovementViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_customers_CustomerDetailViewModel, ((Provider) customerDetailViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_customers_CustomerFormViewModel, ((Provider) customerFormViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_customers_CustomersViewModel, ((Provider) customersViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_dashboard_DashboardViewModel, ((Provider) dashboardViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_invoices_InvoiceDetailViewModel, ((Provider) invoiceDetailViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_invoices_InvoiceFormViewModel, ((Provider) invoiceFormViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_invoices_InvoicesViewModel, ((Provider) invoicesViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_warehouses_LocationFormViewModel, ((Provider) locationFormViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_warehouses_LocationsViewModel, ((Provider) locationsViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_warehouses_MovementListViewModel, ((Provider) movementListViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_notifications_NotificationsViewModel, ((Provider) notificationsViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_orders_OrderDetailViewModel, ((Provider) orderDetailViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_orders_OrderFormViewModel, ((Provider) orderFormViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_orders_OrdersViewModel, ((Provider) ordersViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_pos_POSRegisterViewModel, ((Provider) pOSRegisterViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_pos_POSSessionViewModel, ((Provider) pOSSessionViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_auth_PinViewModel, ((Provider) pinViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_products_ProductDetailViewModel, ((Provider) productDetailViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_products_ProductsViewModel, ((Provider) productsViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_settings_SettingsViewModel, ((Provider) settingsViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_suppliers_SupplierDetailViewModel, ((Provider) supplierDetailViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_suppliers_SupplierFormViewModel, ((Provider) supplierFormViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_suppliers_SuppliersViewModel, ((Provider) suppliersViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_warehouses_WarehouseFormViewModel, ((Provider) warehouseFormViewModelProvider)).put(LazyClassKeyProvider.com_app2_feature_warehouses_WarehousesViewModel, ((Provider) warehousesViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_app2_feature_invoices_InvoiceFormViewModel = "com.app2.feature.invoices.InvoiceFormViewModel";

      static String com_app2_feature_suppliers_SupplierDetailViewModel = "com.app2.feature.suppliers.SupplierDetailViewModel";

      static String com_app2_feature_invoices_InvoicesViewModel = "com.app2.feature.invoices.InvoicesViewModel";

      static String com_app2_feature_orders_OrdersViewModel = "com.app2.feature.orders.OrdersViewModel";

      static String com_app2_feature_warehouses_WarehouseFormViewModel = "com.app2.feature.warehouses.WarehouseFormViewModel";

      static String com_app2_feature_products_ProductsViewModel = "com.app2.feature.products.ProductsViewModel";

      static String com_app2_feature_warehouses_WarehousesViewModel = "com.app2.feature.warehouses.WarehousesViewModel";

      static String com_app2_feature_auth_PinViewModel = "com.app2.feature.auth.PinViewModel";

      static String com_app2_feature_warehouses_MovementListViewModel = "com.app2.feature.warehouses.MovementListViewModel";

      static String com_app2_feature_pos_POSRegisterViewModel = "com.app2.feature.pos.POSRegisterViewModel";

      static String com_app2_feature_customers_CustomerDetailViewModel = "com.app2.feature.customers.CustomerDetailViewModel";

      static String com_app2_feature_warehouses_CreateMovementViewModel = "com.app2.feature.warehouses.CreateMovementViewModel";

      static String com_app2_feature_dashboard_DashboardViewModel = "com.app2.feature.dashboard.DashboardViewModel";

      static String com_app2_feature_pos_POSSessionViewModel = "com.app2.feature.pos.POSSessionViewModel";

      static String com_app2_feature_warehouses_LocationsViewModel = "com.app2.feature.warehouses.LocationsViewModel";

      static String com_app2_feature_customers_CustomerFormViewModel = "com.app2.feature.customers.CustomerFormViewModel";

      static String com_app2_feature_customers_CustomersViewModel = "com.app2.feature.customers.CustomersViewModel";

      static String com_app2_feature_suppliers_SuppliersViewModel = "com.app2.feature.suppliers.SuppliersViewModel";

      static String com_app2_feature_warehouses_LocationFormViewModel = "com.app2.feature.warehouses.LocationFormViewModel";

      static String com_app2_feature_notifications_NotificationsViewModel = "com.app2.feature.notifications.NotificationsViewModel";

      static String com_app2_feature_suppliers_SupplierFormViewModel = "com.app2.feature.suppliers.SupplierFormViewModel";

      static String com_app2_feature_orders_OrderDetailViewModel = "com.app2.feature.orders.OrderDetailViewModel";

      static String com_app2_feature_settings_SettingsViewModel = "com.app2.feature.settings.SettingsViewModel";

      static String com_app2_feature_orders_OrderFormViewModel = "com.app2.feature.orders.OrderFormViewModel";

      static String com_app2_feature_products_ProductDetailViewModel = "com.app2.feature.products.ProductDetailViewModel";

      static String com_app2_feature_invoices_InvoiceDetailViewModel = "com.app2.feature.invoices.InvoiceDetailViewModel";

      @KeepFieldType
      InvoiceFormViewModel com_app2_feature_invoices_InvoiceFormViewModel2;

      @KeepFieldType
      SupplierDetailViewModel com_app2_feature_suppliers_SupplierDetailViewModel2;

      @KeepFieldType
      InvoicesViewModel com_app2_feature_invoices_InvoicesViewModel2;

      @KeepFieldType
      OrdersViewModel com_app2_feature_orders_OrdersViewModel2;

      @KeepFieldType
      WarehouseFormViewModel com_app2_feature_warehouses_WarehouseFormViewModel2;

      @KeepFieldType
      ProductsViewModel com_app2_feature_products_ProductsViewModel2;

      @KeepFieldType
      WarehousesViewModel com_app2_feature_warehouses_WarehousesViewModel2;

      @KeepFieldType
      PinViewModel com_app2_feature_auth_PinViewModel2;

      @KeepFieldType
      MovementListViewModel com_app2_feature_warehouses_MovementListViewModel2;

      @KeepFieldType
      POSRegisterViewModel com_app2_feature_pos_POSRegisterViewModel2;

      @KeepFieldType
      CustomerDetailViewModel com_app2_feature_customers_CustomerDetailViewModel2;

      @KeepFieldType
      CreateMovementViewModel com_app2_feature_warehouses_CreateMovementViewModel2;

      @KeepFieldType
      DashboardViewModel com_app2_feature_dashboard_DashboardViewModel2;

      @KeepFieldType
      POSSessionViewModel com_app2_feature_pos_POSSessionViewModel2;

      @KeepFieldType
      LocationsViewModel com_app2_feature_warehouses_LocationsViewModel2;

      @KeepFieldType
      CustomerFormViewModel com_app2_feature_customers_CustomerFormViewModel2;

      @KeepFieldType
      CustomersViewModel com_app2_feature_customers_CustomersViewModel2;

      @KeepFieldType
      SuppliersViewModel com_app2_feature_suppliers_SuppliersViewModel2;

      @KeepFieldType
      LocationFormViewModel com_app2_feature_warehouses_LocationFormViewModel2;

      @KeepFieldType
      NotificationsViewModel com_app2_feature_notifications_NotificationsViewModel2;

      @KeepFieldType
      SupplierFormViewModel com_app2_feature_suppliers_SupplierFormViewModel2;

      @KeepFieldType
      OrderDetailViewModel com_app2_feature_orders_OrderDetailViewModel2;

      @KeepFieldType
      SettingsViewModel com_app2_feature_settings_SettingsViewModel2;

      @KeepFieldType
      OrderFormViewModel com_app2_feature_orders_OrderFormViewModel2;

      @KeepFieldType
      ProductDetailViewModel com_app2_feature_products_ProductDetailViewModel2;

      @KeepFieldType
      InvoiceDetailViewModel com_app2_feature_invoices_InvoiceDetailViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.app2.feature.warehouses.CreateMovementViewModel 
          return (T) new CreateMovementViewModel(singletonCImpl.provideMovementApiProvider.get(), singletonCImpl.provideProductApiProvider.get(), singletonCImpl.provideLocationApiProvider.get(), singletonCImpl.provideWarehouseApiProvider.get());

          case 1: // com.app2.feature.customers.CustomerDetailViewModel 
          return (T) new CustomerDetailViewModel(singletonCImpl.provideCustomerApiProvider.get());

          case 2: // com.app2.feature.customers.CustomerFormViewModel 
          return (T) new CustomerFormViewModel(singletonCImpl.provideCustomerApiProvider.get());

          case 3: // com.app2.feature.customers.CustomersViewModel 
          return (T) new CustomersViewModel(singletonCImpl.provideCustomerApiProvider.get());

          case 4: // com.app2.feature.dashboard.DashboardViewModel 
          return (T) new DashboardViewModel(singletonCImpl.provideKPIApiProvider.get());

          case 5: // com.app2.feature.invoices.InvoiceDetailViewModel 
          return (T) new InvoiceDetailViewModel(singletonCImpl.provideInvoiceApiProvider.get());

          case 6: // com.app2.feature.invoices.InvoiceFormViewModel 
          return (T) new InvoiceFormViewModel(singletonCImpl.provideInvoiceApiProvider.get(), singletonCImpl.provideCustomerApiProvider.get(), singletonCImpl.provideProductApiProvider.get());

          case 7: // com.app2.feature.invoices.InvoicesViewModel 
          return (T) new InvoicesViewModel(singletonCImpl.provideInvoiceApiProvider.get());

          case 8: // com.app2.feature.warehouses.LocationFormViewModel 
          return (T) new LocationFormViewModel(singletonCImpl.provideLocationApiProvider.get());

          case 9: // com.app2.feature.warehouses.LocationsViewModel 
          return (T) new LocationsViewModel(singletonCImpl.provideLocationApiProvider.get());

          case 10: // com.app2.feature.warehouses.MovementListViewModel 
          return (T) new MovementListViewModel(singletonCImpl.provideMovementApiProvider.get());

          case 11: // com.app2.feature.notifications.NotificationsViewModel 
          return (T) new NotificationsViewModel(singletonCImpl.provideNotificationApiProvider.get());

          case 12: // com.app2.feature.orders.OrderDetailViewModel 
          return (T) new OrderDetailViewModel(singletonCImpl.provideOrderApiProvider.get(), singletonCImpl.provideSupplierApiProvider.get());

          case 13: // com.app2.feature.orders.OrderFormViewModel 
          return (T) new OrderFormViewModel(singletonCImpl.provideOrderApiProvider.get(), singletonCImpl.provideSupplierApiProvider.get(), singletonCImpl.provideWarehouseApiProvider.get());

          case 14: // com.app2.feature.orders.OrdersViewModel 
          return (T) new OrdersViewModel(singletonCImpl.provideOrderApiProvider.get());

          case 15: // com.app2.feature.pos.POSRegisterViewModel 
          return (T) new POSRegisterViewModel(singletonCImpl.providePOSApiProvider.get(), singletonCImpl.provideProductApiProvider.get());

          case 16: // com.app2.feature.pos.POSSessionViewModel 
          return (T) new POSSessionViewModel(singletonCImpl.providePOSApiProvider.get());

          case 17: // com.app2.feature.auth.PinViewModel 
          return (T) new PinViewModel(singletonCImpl.pinManagerProvider.get());

          case 18: // com.app2.feature.products.ProductDetailViewModel 
          return (T) new ProductDetailViewModel(singletonCImpl.provideProductApiProvider.get(), singletonCImpl.provideMovementApiProvider.get());

          case 19: // com.app2.feature.products.ProductsViewModel 
          return (T) new ProductsViewModel(singletonCImpl.provideProductApiProvider.get());

          case 20: // com.app2.feature.settings.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.provideAdminApiProvider.get());

          case 21: // com.app2.feature.suppliers.SupplierDetailViewModel 
          return (T) new SupplierDetailViewModel(singletonCImpl.provideSupplierApiProvider.get());

          case 22: // com.app2.feature.suppliers.SupplierFormViewModel 
          return (T) new SupplierFormViewModel(singletonCImpl.provideSupplierApiProvider.get());

          case 23: // com.app2.feature.suppliers.SuppliersViewModel 
          return (T) new SuppliersViewModel(singletonCImpl.provideSupplierApiProvider.get());

          case 24: // com.app2.feature.warehouses.WarehouseFormViewModel 
          return (T) new WarehouseFormViewModel(singletonCImpl.provideWarehouseApiProvider.get());

          case 25: // com.app2.feature.warehouses.WarehousesViewModel 
          return (T) new WarehousesViewModel(singletonCImpl.provideWarehouseApiProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends App_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends App_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends App_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<PinManager> pinManagerProvider;

    private Provider<OkHttpClient> provideOkHttpClientProvider;

    private Provider<Json> provideJsonProvider;

    private Provider<Retrofit> provideRetrofitProvider;

    private Provider<MovementApiService> provideMovementApiProvider;

    private Provider<ProductApiService> provideProductApiProvider;

    private Provider<LocationApiService> provideLocationApiProvider;

    private Provider<WarehouseApiService> provideWarehouseApiProvider;

    private Provider<CustomerApiService> provideCustomerApiProvider;

    private Provider<KPIApiService> provideKPIApiProvider;

    private Provider<InvoiceApiService> provideInvoiceApiProvider;

    private Provider<NotificationApiService> provideNotificationApiProvider;

    private Provider<OrderApiService> provideOrderApiProvider;

    private Provider<SupplierApiService> provideSupplierApiProvider;

    private Provider<POSApiService> providePOSApiProvider;

    private Provider<AdminApiService> provideAdminApiProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.pinManagerProvider = DoubleCheck.provider(new SwitchingProvider<PinManager>(singletonCImpl, 0));
      this.provideOkHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 3));
      this.provideJsonProvider = DoubleCheck.provider(new SwitchingProvider<Json>(singletonCImpl, 4));
      this.provideRetrofitProvider = DoubleCheck.provider(new SwitchingProvider<Retrofit>(singletonCImpl, 2));
      this.provideMovementApiProvider = DoubleCheck.provider(new SwitchingProvider<MovementApiService>(singletonCImpl, 1));
      this.provideProductApiProvider = DoubleCheck.provider(new SwitchingProvider<ProductApiService>(singletonCImpl, 5));
      this.provideLocationApiProvider = DoubleCheck.provider(new SwitchingProvider<LocationApiService>(singletonCImpl, 6));
      this.provideWarehouseApiProvider = DoubleCheck.provider(new SwitchingProvider<WarehouseApiService>(singletonCImpl, 7));
      this.provideCustomerApiProvider = DoubleCheck.provider(new SwitchingProvider<CustomerApiService>(singletonCImpl, 8));
      this.provideKPIApiProvider = DoubleCheck.provider(new SwitchingProvider<KPIApiService>(singletonCImpl, 9));
      this.provideInvoiceApiProvider = DoubleCheck.provider(new SwitchingProvider<InvoiceApiService>(singletonCImpl, 10));
      this.provideNotificationApiProvider = DoubleCheck.provider(new SwitchingProvider<NotificationApiService>(singletonCImpl, 11));
      this.provideOrderApiProvider = DoubleCheck.provider(new SwitchingProvider<OrderApiService>(singletonCImpl, 12));
      this.provideSupplierApiProvider = DoubleCheck.provider(new SwitchingProvider<SupplierApiService>(singletonCImpl, 13));
      this.providePOSApiProvider = DoubleCheck.provider(new SwitchingProvider<POSApiService>(singletonCImpl, 14));
      this.provideAdminApiProvider = DoubleCheck.provider(new SwitchingProvider<AdminApiService>(singletonCImpl, 15));
    }

    @Override
    public void injectApp(App app) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.app2.feature.auth.PinManager 
          return (T) new PinManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 1: // com.app2.core.data.remote.MovementApiService 
          return (T) NetworkModule_ProvideMovementApiFactory.provideMovementApi(singletonCImpl.provideRetrofitProvider.get());

          case 2: // retrofit2.Retrofit 
          return (T) NetworkModule_ProvideRetrofitFactory.provideRetrofit(singletonCImpl.provideOkHttpClientProvider.get(), singletonCImpl.provideJsonProvider.get());

          case 3: // okhttp3.OkHttpClient 
          return (T) NetworkModule_ProvideOkHttpClientFactory.provideOkHttpClient();

          case 4: // kotlinx.serialization.json.Json 
          return (T) NetworkModule_ProvideJsonFactory.provideJson();

          case 5: // com.app2.core.data.remote.ProductApiService 
          return (T) NetworkModule_ProvideProductApiFactory.provideProductApi(singletonCImpl.provideRetrofitProvider.get());

          case 6: // com.app2.core.data.remote.LocationApiService 
          return (T) NetworkModule_ProvideLocationApiFactory.provideLocationApi(singletonCImpl.provideRetrofitProvider.get());

          case 7: // com.app2.core.data.remote.WarehouseApiService 
          return (T) NetworkModule_ProvideWarehouseApiFactory.provideWarehouseApi(singletonCImpl.provideRetrofitProvider.get());

          case 8: // com.app2.core.data.remote.CustomerApiService 
          return (T) NetworkModule_ProvideCustomerApiFactory.provideCustomerApi(singletonCImpl.provideRetrofitProvider.get());

          case 9: // com.app2.core.data.remote.KPIApiService 
          return (T) NetworkModule_ProvideKPIApiFactory.provideKPIApi(singletonCImpl.provideRetrofitProvider.get());

          case 10: // com.app2.core.data.remote.InvoiceApiService 
          return (T) NetworkModule_ProvideInvoiceApiFactory.provideInvoiceApi(singletonCImpl.provideRetrofitProvider.get());

          case 11: // com.app2.core.data.remote.NotificationApiService 
          return (T) NetworkModule_ProvideNotificationApiFactory.provideNotificationApi(singletonCImpl.provideRetrofitProvider.get());

          case 12: // com.app2.core.data.remote.OrderApiService 
          return (T) NetworkModule_ProvideOrderApiFactory.provideOrderApi(singletonCImpl.provideRetrofitProvider.get());

          case 13: // com.app2.core.data.remote.SupplierApiService 
          return (T) NetworkModule_ProvideSupplierApiFactory.provideSupplierApi(singletonCImpl.provideRetrofitProvider.get());

          case 14: // com.app2.core.data.remote.POSApiService 
          return (T) NetworkModule_ProvidePOSApiFactory.providePOSApi(singletonCImpl.provideRetrofitProvider.get());

          case 15: // com.app2.core.data.remote.AdminApiService 
          return (T) NetworkModule_ProvideAdminApiFactory.provideAdminApi(singletonCImpl.provideRetrofitProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
