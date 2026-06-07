package com.app2;

import com.app2.core.data.di.NetworkModule;
import com.app2.core.data.local.database.DatabaseModule;
import com.app2.feature.auth.PinViewModel_HiltModules;
import com.app2.feature.customers.CustomerDetailViewModel_HiltModules;
import com.app2.feature.customers.CustomerFormViewModel_HiltModules;
import com.app2.feature.customers.CustomersViewModel_HiltModules;
import com.app2.feature.dashboard.DashboardViewModel_HiltModules;
import com.app2.feature.invoices.InvoiceDetailViewModel_HiltModules;
import com.app2.feature.invoices.InvoiceFormViewModel_HiltModules;
import com.app2.feature.invoices.InvoicesViewModel_HiltModules;
import com.app2.feature.notifications.NotificationsViewModel_HiltModules;
import com.app2.feature.orders.OrderDetailViewModel_HiltModules;
import com.app2.feature.orders.OrderFormViewModel_HiltModules;
import com.app2.feature.orders.OrdersViewModel_HiltModules;
import com.app2.feature.pos.POSRegisterViewModel_HiltModules;
import com.app2.feature.pos.POSSessionViewModel_HiltModules;
import com.app2.feature.products.ProductDetailViewModel_HiltModules;
import com.app2.feature.products.ProductsViewModel_HiltModules;
import com.app2.feature.settings.SettingsViewModel_HiltModules;
import com.app2.feature.suppliers.SupplierDetailViewModel_HiltModules;
import com.app2.feature.suppliers.SupplierFormViewModel_HiltModules;
import com.app2.feature.suppliers.SuppliersViewModel_HiltModules;
import com.app2.feature.warehouses.CreateMovementViewModel_HiltModules;
import com.app2.feature.warehouses.LocationFormViewModel_HiltModules;
import com.app2.feature.warehouses.LocationsViewModel_HiltModules;
import com.app2.feature.warehouses.MovementListViewModel_HiltModules;
import com.app2.feature.warehouses.WarehouseFormViewModel_HiltModules;
import com.app2.feature.warehouses.WarehousesViewModel_HiltModules;
import dagger.Binds;
import dagger.Component;
import dagger.Module;
import dagger.Subcomponent;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.components.ActivityRetainedComponent;
import dagger.hilt.android.components.FragmentComponent;
import dagger.hilt.android.components.ServiceComponent;
import dagger.hilt.android.components.ViewComponent;
import dagger.hilt.android.components.ViewModelComponent;
import dagger.hilt.android.components.ViewWithFragmentComponent;
import dagger.hilt.android.flags.FragmentGetContextFix;
import dagger.hilt.android.flags.HiltWrapper_FragmentGetContextFix_FragmentGetContextFixModule;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.HiltViewModelFactory;
import dagger.hilt.android.internal.lifecycle.HiltWrapper_DefaultViewModelFactories_ActivityModule;
import dagger.hilt.android.internal.lifecycle.HiltWrapper_HiltViewModelFactory_ActivityCreatorEntryPoint;
import dagger.hilt.android.internal.lifecycle.HiltWrapper_HiltViewModelFactory_ViewModelModule;
import dagger.hilt.android.internal.managers.ActivityComponentManager;
import dagger.hilt.android.internal.managers.FragmentComponentManager;
import dagger.hilt.android.internal.managers.HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedComponentBuilderEntryPoint;
import dagger.hilt.android.internal.managers.HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedLifecycleEntryPoint;
import dagger.hilt.android.internal.managers.HiltWrapper_ActivityRetainedComponentManager_LifecycleModule;
import dagger.hilt.android.internal.managers.HiltWrapper_SavedStateHandleModule;
import dagger.hilt.android.internal.managers.ServiceComponentManager;
import dagger.hilt.android.internal.managers.ViewComponentManager;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.HiltWrapper_ActivityModule;
import dagger.hilt.android.scopes.ActivityRetainedScoped;
import dagger.hilt.android.scopes.ActivityScoped;
import dagger.hilt.android.scopes.FragmentScoped;
import dagger.hilt.android.scopes.ServiceScoped;
import dagger.hilt.android.scopes.ViewModelScoped;
import dagger.hilt.android.scopes.ViewScoped;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.internal.GeneratedComponent;
import dagger.hilt.migration.DisableInstallInCheck;
import javax.annotation.processing.Generated;
import javax.inject.Singleton;

@Generated("dagger.hilt.processor.internal.root.RootProcessor")
public final class App_HiltComponents {
  private App_HiltComponents() {
  }

  @Module(
      subcomponents = ServiceC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ServiceCBuilderModule {
    @Binds
    ServiceComponentBuilder bind(ServiceC.Builder builder);
  }

  @Module(
      subcomponents = ActivityRetainedC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ActivityRetainedCBuilderModule {
    @Binds
    ActivityRetainedComponentBuilder bind(ActivityRetainedC.Builder builder);
  }

  @Module(
      subcomponents = ActivityC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ActivityCBuilderModule {
    @Binds
    ActivityComponentBuilder bind(ActivityC.Builder builder);
  }

  @Module(
      subcomponents = ViewModelC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ViewModelCBuilderModule {
    @Binds
    ViewModelComponentBuilder bind(ViewModelC.Builder builder);
  }

  @Module(
      subcomponents = ViewC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ViewCBuilderModule {
    @Binds
    ViewComponentBuilder bind(ViewC.Builder builder);
  }

  @Module(
      subcomponents = FragmentC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface FragmentCBuilderModule {
    @Binds
    FragmentComponentBuilder bind(FragmentC.Builder builder);
  }

  @Module(
      subcomponents = ViewWithFragmentC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ViewWithFragmentCBuilderModule {
    @Binds
    ViewWithFragmentComponentBuilder bind(ViewWithFragmentC.Builder builder);
  }

  @Component(
      modules = {
          ActivityRetainedCBuilderModule.class,
          ServiceCBuilderModule.class,
          ApplicationContextModule.class,
          DatabaseModule.class,
          HiltWrapper_FragmentGetContextFix_FragmentGetContextFixModule.class,
          NetworkModule.class
      }
  )
  @Singleton
  public abstract static class SingletonC implements App_GeneratedInjector,
      FragmentGetContextFix.FragmentGetContextFixEntryPoint,
      HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedComponentBuilderEntryPoint,
      ServiceComponentManager.ServiceComponentBuilderEntryPoint,
      SingletonComponent,
      GeneratedComponent {
  }

  @Subcomponent
  @ServiceScoped
  public abstract static class ServiceC implements ServiceComponent,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ServiceComponentBuilder {
    }
  }

  @Subcomponent(
      modules = {
          ActivityCBuilderModule.class,
          ViewModelCBuilderModule.class,
          CreateMovementViewModel_HiltModules.KeyModule.class,
          CustomerDetailViewModel_HiltModules.KeyModule.class,
          CustomerFormViewModel_HiltModules.KeyModule.class,
          CustomersViewModel_HiltModules.KeyModule.class,
          DashboardViewModel_HiltModules.KeyModule.class,
          HiltWrapper_ActivityRetainedComponentManager_LifecycleModule.class,
          HiltWrapper_SavedStateHandleModule.class,
          InvoiceDetailViewModel_HiltModules.KeyModule.class,
          InvoiceFormViewModel_HiltModules.KeyModule.class,
          InvoicesViewModel_HiltModules.KeyModule.class,
          LocationFormViewModel_HiltModules.KeyModule.class,
          LocationsViewModel_HiltModules.KeyModule.class,
          MovementListViewModel_HiltModules.KeyModule.class,
          NotificationsViewModel_HiltModules.KeyModule.class,
          OrderDetailViewModel_HiltModules.KeyModule.class,
          OrderFormViewModel_HiltModules.KeyModule.class,
          OrdersViewModel_HiltModules.KeyModule.class,
          POSRegisterViewModel_HiltModules.KeyModule.class,
          POSSessionViewModel_HiltModules.KeyModule.class,
          PinViewModel_HiltModules.KeyModule.class,
          ProductDetailViewModel_HiltModules.KeyModule.class,
          ProductsViewModel_HiltModules.KeyModule.class,
          SettingsViewModel_HiltModules.KeyModule.class,
          SupplierDetailViewModel_HiltModules.KeyModule.class,
          SupplierFormViewModel_HiltModules.KeyModule.class,
          SuppliersViewModel_HiltModules.KeyModule.class,
          WarehouseFormViewModel_HiltModules.KeyModule.class,
          WarehousesViewModel_HiltModules.KeyModule.class
      }
  )
  @ActivityRetainedScoped
  public abstract static class ActivityRetainedC implements ActivityRetainedComponent,
      ActivityComponentManager.ActivityComponentBuilderEntryPoint,
      HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedLifecycleEntryPoint,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ActivityRetainedComponentBuilder {
    }
  }

  @Subcomponent(
      modules = {
          FragmentCBuilderModule.class,
          ViewCBuilderModule.class,
          HiltWrapper_ActivityModule.class,
          HiltWrapper_DefaultViewModelFactories_ActivityModule.class
      }
  )
  @ActivityScoped
  public abstract static class ActivityC implements MainActivity_GeneratedInjector,
      ActivityComponent,
      DefaultViewModelFactories.ActivityEntryPoint,
      HiltWrapper_HiltViewModelFactory_ActivityCreatorEntryPoint,
      FragmentComponentManager.FragmentComponentBuilderEntryPoint,
      ViewComponentManager.ViewComponentBuilderEntryPoint,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ActivityComponentBuilder {
    }
  }

  @Subcomponent(
      modules = {
          CreateMovementViewModel_HiltModules.BindsModule.class,
          CustomerDetailViewModel_HiltModules.BindsModule.class,
          CustomerFormViewModel_HiltModules.BindsModule.class,
          CustomersViewModel_HiltModules.BindsModule.class,
          DashboardViewModel_HiltModules.BindsModule.class,
          HiltWrapper_HiltViewModelFactory_ViewModelModule.class,
          InvoiceDetailViewModel_HiltModules.BindsModule.class,
          InvoiceFormViewModel_HiltModules.BindsModule.class,
          InvoicesViewModel_HiltModules.BindsModule.class,
          LocationFormViewModel_HiltModules.BindsModule.class,
          LocationsViewModel_HiltModules.BindsModule.class,
          MovementListViewModel_HiltModules.BindsModule.class,
          NotificationsViewModel_HiltModules.BindsModule.class,
          OrderDetailViewModel_HiltModules.BindsModule.class,
          OrderFormViewModel_HiltModules.BindsModule.class,
          OrdersViewModel_HiltModules.BindsModule.class,
          POSRegisterViewModel_HiltModules.BindsModule.class,
          POSSessionViewModel_HiltModules.BindsModule.class,
          PinViewModel_HiltModules.BindsModule.class,
          ProductDetailViewModel_HiltModules.BindsModule.class,
          ProductsViewModel_HiltModules.BindsModule.class,
          SettingsViewModel_HiltModules.BindsModule.class,
          SupplierDetailViewModel_HiltModules.BindsModule.class,
          SupplierFormViewModel_HiltModules.BindsModule.class,
          SuppliersViewModel_HiltModules.BindsModule.class,
          WarehouseFormViewModel_HiltModules.BindsModule.class,
          WarehousesViewModel_HiltModules.BindsModule.class
      }
  )
  @ViewModelScoped
  public abstract static class ViewModelC implements ViewModelComponent,
      HiltViewModelFactory.ViewModelFactoriesEntryPoint,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ViewModelComponentBuilder {
    }
  }

  @Subcomponent
  @ViewScoped
  public abstract static class ViewC implements ViewComponent,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ViewComponentBuilder {
    }
  }

  @Subcomponent(
      modules = ViewWithFragmentCBuilderModule.class
  )
  @FragmentScoped
  public abstract static class FragmentC implements FragmentComponent,
      DefaultViewModelFactories.FragmentEntryPoint,
      ViewComponentManager.ViewWithFragmentComponentBuilderEntryPoint,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends FragmentComponentBuilder {
    }
  }

  @Subcomponent
  @ViewScoped
  public abstract static class ViewWithFragmentC implements ViewWithFragmentComponent,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ViewWithFragmentComponentBuilder {
    }
  }
}
